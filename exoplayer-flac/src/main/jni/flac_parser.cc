/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "include/flac_parser.h"

#include <jni.h>

#include <android/log.h>

#include <cassert>
#include <cstdlib>
#include <cstring>

#define LOG_TAG "FLACParser"
#define ALOGE(...) \
  ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))
#define ALOGV(...) \
  ((void)__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__))

#define LOG_ALWAYS_FATAL(...) \
  (__android_log_assert(NULL, LOG_TAG, ##__VA_ARGS__))

#define LITERAL_TO_STRING_INTERNAL(x) #x
#define LITERAL_TO_STRING(x) LITERAL_TO_STRING_INTERNAL(x)

#define TRESPASS()          \
  LOG_ALWAYS_FATAL(__FILE__ \
                   ":" LITERAL_TO_STRING(__LINE__) " Should not be here.");
#define CHECK(x) \
  if (!(x)) ALOGE("Check failed: %s ", #x)

// The FLAC parser calls our C++ static callbacks using C calling conventions,
// inside FLAC__stream_decoder_process_until_end_of_metadata
// and FLAC__stream_decoder_process_single.
// We immediately then call our corresponding C++ instance methods
// with the same parameter list, but discard redundant information.

FLAC__StreamDecoderReadStatus FLACParser::read_callback(
    const FLAC__StreamDecoder * /* decoder */, FLAC__byte buffer[],
    size_t *bytes, void *client_data) {
  return reinterpret_cast<FLACParser *>(client_data)
      ->readCallback(buffer, bytes);
}

FLAC__StreamDecoderSeekStatus FLACParser::seek_callback(
    const FLAC__StreamDecoder * /* decoder */,
    FLAC__uint64 absolute_byte_offset, void *client_data) {
  return reinterpret_cast<FLACParser *>(client_data)
      ->seekCallback(absolute_byte_offset);
}

FLAC__StreamDecoderTellStatus FLACParser::tell_callback(
    const FLAC__StreamDecoder * /* decoder */,
    FLAC__uint64 *absolute_byte_offset, void *client_data) {
  return reinterpret_cast<FLACParser *>(client_data)
      ->tellCallback(absolute_byte_offset);
}

FLAC__StreamDecoderLengthStatus FLACParser::length_callback(
    const FLAC__StreamDecoder * /* decoder */, FLAC__uint64 *stream_length,
    void *client_data) {
  return reinterpret_cast<FLACParser *>(client_data)
      ->lengthCallback(stream_length);
}

FLAC__bool FLACParser::eof_callback(const FLAC__StreamDecoder * /* decoder */,
                                    void *client_data) {
  return reinterpret_cast<FLACParser *>(client_data)->eofCallback();
}

FLAC__StreamDecoderWriteStatus FLACParser::write_callback(
    const FLAC__StreamDecoder * /* decoder */, const FLAC__Frame *frame,
    const FLAC__int32 *const buffer[], void *client_data) {
  return reinterpret_cast<FLACParser *>(client_data)
      ->writeCallback(frame, buffer);
}

void FLACParser::metadata_callback(const FLAC__StreamDecoder * /* decoder */,
                                   const FLAC__StreamMetadata *metadata,
                                   void *client_data) {
  reinterpret_cast<FLACParser *>(client_data)->metadataCallback(metadata);
}

void FLACParser::error_callback(const FLAC__StreamDecoder * /* decoder */,
                                FLAC__StreamDecoderErrorStatus status,
                                void *client_data) {
  reinterpret_cast<FLACParser *>(client_data)->errorCallback(status);
}

// These are the corresponding callbacks with C++ calling conventions

FLAC__StreamDecoderReadStatus FLACParser::readCallback(FLAC__byte buffer[],
                                                       size_t *bytes) {
  size_t requested = *bytes;
  ssize_t actual = mDataSource->readAt(mCurrentPos, buffer, requested);
  if (0 > actual) {
    *bytes = 0;
    return FLAC__STREAM_DECODER_READ_STATUS_ABORT;
  } else if (0 == actual) {
    *bytes = 0;
    mEOF = true;
    return FLAC__STREAM_DECODER_READ_STATUS_END_OF_STREAM;
  } else {
    assert(actual <= requested);
    *bytes = actual;
    mCurrentPos += actual;
    return FLAC__STREAM_DECODER_READ_STATUS_CONTINUE;
  }
}

FLAC__StreamDecoderSeekStatus FLACParser::seekCallback(
    FLAC__uint64 absolute_byte_offset) {
  mCurrentPos = absolute_byte_offset;
  mEOF = false;
  return FLAC__STREAM_DECODER_SEEK_STATUS_OK;
}

FLAC__StreamDecoderTellStatus FLACParser::tellCallback(
    FLAC__uint64 *absolute_byte_offset) {
  *absolute_byte_offset = mCurrentPos;
  return FLAC__STREAM_DECODER_TELL_STATUS_OK;
}

FLAC__StreamDecoderLengthStatus FLACParser::lengthCallback(
    FLAC__uint64 *stream_length) {
  return FLAC__STREAM_DECODER_LENGTH_STATUS_UNSUPPORTED;
}

FLAC__bool FLACParser::eofCallback() { return mEOF; }

FLAC__StreamDecoderWriteStatus FLACParser::writeCallback(
    const FLAC__Frame *frame, const FLAC__int32 *const buffer[]) {
  if (mWriteRequested) {
    mWriteRequested = false;
    // FLAC parser doesn't free or realloc buffer until next frame or finish
    mWriteHeader = frame->header;
    mWriteBuffer = buffer;
    mWriteCompleted = true;
    return FLAC__STREAM_DECODER_WRITE_STATUS_CONTINUE;
  } else {
    ALOGE("FLACParser::writeCallback unexpected");
    return FLAC__STREAM_DECODER_WRITE_STATUS_ABORT;
  }
}

void FLACParser::metadataCallback(const FLAC__StreamMetadata *metadata) {
  switch (metadata->type) {
    case FLAC__METADATA_TYPE_STREAMINFO:
      if (!mStreamInfoValid) {
        mStreamInfo = metadata->data.stream_info;
        mStreamInfoValid = true;
      } else {
        ALOGE("FLACParser::metadataCallback unexpected STREAMINFO");
      }
      break;
    case FLAC__METADATA_TYPE_SEEKTABLE:
      mSeekTable = &metadata->data.seek_table;
      break;
    default:
      ALOGE("FLACParser::metadataCallback unexpected type %u", metadata->type);
      break;
  }
}

void FLACParser::errorCallback(FLAC__StreamDecoderErrorStatus status) {
  ALOGE("FLACParser::errorCallback status=%d", status);
  mErrorStatus = status;
}

// Copy samples from FLAC native 32-bit non-interleaved to 16-bit interleaved.
// These are candidates for optimization if needed.

static void copyMono8(int16_t *dst, const int *const *src, unsigned nSamples,
                      unsigned /* nChannels */) {
  for (unsigned i = 0; i < nSamples; ++i) {
    *dst++ = src[0][i] << 8;
  }
}

static void copyStereo8(int16_t *dst, const int *const *src, unsigned nSamples,
                        unsigned /* nChannels */) {
  for (unsigned i = 0; i < nSamples; ++i) {
    *dst++ = src[0][i] << 8;
    *dst++ = src[1][i] << 8;
  }
}

static void copyMultiCh8(int16_t *dst, const int *const *src, unsigned nSamples,
                         unsigned nChannels) {
  for (unsigned i = 0; i < nSamples; ++i) {
    for (unsigned c = 0; c < nChannels; ++c) {
      *dst++ = src[c][i] << 8;
    }
  }
}

static void copyMono16(int16_t *dst, const int *const *src, unsigned nSamples,
                       unsigned /* nChannels */) {
  for (unsigned i = 0; i < nSamples; ++i) {
    *dst++ = src[0][i];
  }
}

static void copyStereo16(int16_t *dst, const int *const *src, unsigned nSamples,
                         unsigned /* nChannels */) {
  for (unsigned i = 0; i < nSamples; ++i) {
    *dst++ = src[0][i];
    *dst++ = src[1][i];
  }
}

static void copyMultiCh16(int16_t *dst, const int *const *src,
                          unsigned nSamples, unsigned nChannels) {
  for (unsigned i = 0; i < nSamples; ++i) {
    for (unsigned c = 0; c < nChannels; ++c) {
      *dst++ = src[c][i];
    }
  }
}

// 24-bit versions should do dithering or noise-shaping, here or in AudioFlinger

static void copyMono24(int16_t *dst, const int *const *src, unsigned nSamples,
                       unsigned /* nChannels */) {
  for (unsigned i = 0; i < nSamples; ++i) {
    *dst++ = src[0][i] >> 8;
  }
}

static void copyStereo24(int16_t *dst, const int *const *src, unsigned nSamples,
                         unsigned /* nChannels */) {
  for (unsigned i = 0; i < nSamples; ++i) {
    *dst++ = src[0][i] >> 8;
    *dst++ = src[1][i] >> 8;
  }
}

static void copyMultiCh24(int16_t *dst, const int *const *src,
                          unsigned nSamples, unsigned nChannels) {
  for (unsigned i = 0; i < nSamples; ++i) {
    for (unsigned c = 0; c < nChannels; ++c) {
      *dst++ = src[c][i] >> 8;
    }
  }
}

static void copyTrespass(int16_t * /* dst */, const int *const * /* src */,
                         unsigned /* nSamples */, unsigned /* nChannels */) {
  TRESPASS();
}

// FLACParser

FLACParser::FLACParser(DataSource *source)
    : mDataSource(source),
      mCopy(copyTrespass),
      mDecoder(NULL),
      mSeekTable(NULL),
      firstFrameOffset(0LL),
      mCurrentPos(0LL),
      mEOF(false),
      mStreamInfoValid(false),
      mWriteRequested(false),
      mWriteCompleted(false),
      mWriteBuffer(NULL),
      mErrorStatus((FLAC__StreamDecoderErrorStatus)-1) {
  ALOGV("FLACParser::FLACParser");
  memset(&mStreamInfo, 0, sizeof(mStreamInfo));
  memset(&mWriteHeader, 0, sizeof(mWriteHeader));
}

FLACParser::~FLACParser() {
  ALOGV("FLACParser::~FLACParser");
  if (mDecoder != NULL) {
    FLAC__stream_decoder_delete(mDecoder);
    mDecoder = NULL;
  }
}

bool FLACParser::init() {
  // setup libFLAC parser
  mDecoder = FLAC__stream_decoder_new();
  if (mDecoder == NULL) {
    // The new should succeed, since probably all it does is a malloc
    // that always succeeds in Android.  But to avoid dependence on the
    // libFLAC internals, we check and log here.
    ALOGE("new failed");
    return false;
  }
  FLAC__stream_decoder_set_md5_checking(mDecoder, false);
  FLAC__stream_decoder_set_metadata_ignore_all(mDecoder);
  FLAC__stream_decoder_set_metadata_respond(mDecoder,
                                            FLAC__METADATA_TYPE_STREAMINFO);
  FLAC__stream_decoder_set_metadata_respond(mDecoder,
                                            FLAC__METADATA_TYPE_SEEKTABLE);
  FLAC__StreamDecoderInitStatus initStatus;
  initStatus = FLAC__stream_decoder_init_stream(
      mDecoder, read_callback, seek_callback, tell_callback, length_callback,
      eof_callback, write_callback, metadata_callback, error_callback,
      reinterpret_cast<void *>(this));
  if (initStatus != FLAC__STREAM_DECODER_INIT_STATUS_OK) {
    // A failure here probably indicates a programming error and so is
    // unlikely to happen. But we check and log here similarly to above.
    ALOGE("init_stream failed %d", initStatus);
    return false;
  }
  return true;
}

bool FLACParser::decodeMetadata() {
  // parse all metadata
  if (!FLAC__stream_decoder_process_until_end_of_metadata(mDecoder)) {
    ALOGE("metadata decoding failed");
    return false;
  }
  // store first frame offset
  FLAC__stream_decoder_get_decode_position(mDecoder, &firstFrameOffset);

  if (mStreamInfoValid) {
    // check channel count
    if (getChannels() == 0 || getChannels() > 8) {
      ALOGE("unsupported channel count %u", getChannels());
      return false;
    }
    // check bit depth
    switch (getBitsPerSample()) {
      case 8:
      case 16:
      case 24:
        break;
      default:
        ALOGE("unsupported bits per sample %u", getBitsPerSample());
        return false;
    }
    // check sample rate
    switch (getSampleRate()) {
      case 8000:
      case 11025:
      case 12000:
      case 16000:
      case 22050:
      case 24000:
      case 32000:
      case 44100:
      case 48000:
      case 88200:
      case 96000:
        break;
      default:
        ALOGE("unsupported sample rate %u", getSampleRate());
        return false;
    }
    // configure the appropriate copy function, defaulting to trespass
    static const struct {
      unsigned mChannels;
      unsigned mBitsPerSample;
      void (*mCopy)(int16_t *dst, const int *const *src, unsigned nSamples,
                    unsigned nChannels);
    } table[] = {
        {1, 8, copyMono8},   {2, 8, copyStereo8},   {8, 8, copyMultiCh8},
        {1, 16, copyMono16}, {2, 16, copyStereo16}, {8, 16, copyMultiCh16},
        {1, 24, copyMono24}, {2, 24, copyStereo24}, {8, 24, copyMultiCh24},
    };
    for (unsigned i = 0; i < sizeof(table) / sizeof(table[0]); ++i) {
      if (table[i].mChannels >= getChannels() &&
          table[i].mBitsPerSample == getBitsPerSample()) {
        mCopy = table[i].mCopy;
        break;
      }
    }
  } else {
    ALOGE("missing STREAMINFO");
    return false;
  }
  return true;
}

size_t FLACParser::readBuffer(void *output, size_t output_size) {
  mWriteRequested = true;
  mWriteCompleted = false;

  if (!FLAC__stream_decoder_process_single(mDecoder)) {
    ALOGE("FLACParser::readBuffer process_single failed. Status: %s",
          getDecoderStateString());
    return -1;
  }
  if (!mWriteCompleted) {
    if (FLAC__stream_decoder_get_state(mDecoder) !=
        FLAC__STREAM_DECODER_END_OF_STREAM) {
      ALOGE("FLACParser::readBuffer write did not complete. Status: %s",
            getDecoderStateString());
    }
    return -1;
  }

  // verify that block header keeps the promises made by STREAMINFO
  unsigned blocksize = mWriteHeader.blocksize;
  if (blocksize == 0 || blocksize > getMaxBlockSize()) {
    ALOGE("FLACParser::readBuffer write invalid blocksize %u", blocksize);
    return -1;
  }
  if (mWriteHeader.sample_rate != getSampleRate() ||
      mWriteHeader.channels != getChannels() ||
      mWriteHeader.bits_per_sample != getBitsPerSample()) {
    ALOGE(
        "FLACParser::readBuffer write changed parameters mid-stream: %d/%d/%d "
        "-> %d/%d/%d",
        getSampleRate(), getChannels(), getBitsPerSample(),
        mWriteHeader.sample_rate, mWriteHeader.channels,
        mWriteHeader.bits_per_sample);
    return -1;
  }

  size_t bufferSize = blocksize * getChannels() * sizeof(int16_t);
  if (bufferSize > output_size) {
    ALOGE(
        "FLACParser::readBuffer not enough space in output buffer "
        "%zu < %zu",
        output_size, bufferSize);
    return -1;
  }

  // copy PCM from FLAC write buffer to our media buffer, with interleaving.
  (*mCopy)(reinterpret_cast<int16_t *>(output), mWriteBuffer, blocksize,
           getChannels());

  // fill in buffer metadata
  CHECK(mWriteHeader.number_type == FLAC__FRAME_NUMBER_TYPE_SAMPLE_NUMBER);

  return bufferSize;
}

int64_t FLACParser::getSeekPosition(int64_t timeUs) {
  if (!mSeekTable) {
    return -1;
  }

  int64_t sample = (timeUs * getSampleRate()) / 1000000LL;
  if (sample >= getTotalSamples()) {
      sample = getTotalSamples();
  }

  FLAC__StreamMetadata_SeekPoint* points = mSeekTable->points;
  for (unsigned i = mSeekTable->num_points; i > 0; ) {
    i--;
    if (points[i].sample_number <= sample) {
      return firstFrameOffset + points[i].stream_offset;
    }
  }
  return firstFrameOffset;
}
