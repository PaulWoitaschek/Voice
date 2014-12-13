#include <jni.h>
#include <android/log.h>
#include <stdlib.h>
#include "sonic.h"
#include "sonicjni.h"

// For debug messages:
#if SONIC_DEBUG
#define APPNAME "Sonic"
#define LOGV(...) _android_log_printNative(ANDROID_LOG_VERBOSE, APPNAME, __VA_ARGS__);
#else
#define LOGV(...)
#endif

struct sonicInstStruct {
    sonicStream stream;
    short *byteBuf;
    int byteBufSize;
};

typedef struct sonicInstStruct *sonicInst;

#define getInst(sonicID) ((sonicInst)((char *)NULL + (sonicID)))

/* Initialize the C data structure */
jlong Java_org_vinuxproject_sonic_Sonic_initNative(
    JNIEnv *env,
    jobject thiz,
    jint sampleRate,
    jint channels)
{
    sonicInst inst = (sonicInst)calloc(1, sizeof(struct sonicInstStruct));

    if(inst == NULL) {
        return 0;
    }
    LOGV("Creating sonic stream");
    inst->stream = sonicCreateStream(sampleRate, channels);
    if(inst->stream == NULL) {
        return 0;
    }
    inst->byteBufSize = 100;
    inst->byteBuf = (short *)calloc(inst->byteBufSize, sizeof(short));
    if(inst->byteBuf == NULL) {
        return 0;
    }
    return (jlong)((char *)inst - (char *)NULL);
}


// Teardown the C data structure.
void Java_org_vinuxproject_sonic_Sonic_closeNative(
    JNIEnv *env,
    jobject thiz,
    jlong sonicID)
{
    sonicInst inst = getInst(sonicID);
    sonicStream stream = inst->stream;

    LOGV("Destroying stream");
    sonicDestroyStream(stream);
    free(inst->byteBuf);
    free(inst);
}

/* Put bytes into the input buffer of the sound alteration object
   lenBytes bytes will be read from buffer into the sound alteration object
   buffer is not guaranteed not to change after this function is called,
   so data should be copied from it */
jboolean Java_org_vinuxproject_sonic_Sonic_putBytesNative(
    JNIEnv *env,
    jobject thiz,
    jlong sonicID,
    jbyteArray buffer,
    jint lenBytes)
{
    sonicInst inst = getInst(sonicID);
    sonicStream stream = inst->stream;
    int samples = lenBytes/(sizeof(short)*sonicGetNumChannels(stream));
    int remainingBytes = lenBytes - samples*sizeof(short)*sonicGetNumChannels(stream);

// TODO: deal with case where remainingBytes is not 0.
if(remainingBytes != 0) {
    LOGV("Remaining bytes == %d!!!", remainingBytes);
}
    if(lenBytes > inst->byteBufSize*sizeof(short)) {
        inst->byteBufSize = lenBytes*(2/sizeof(short));
        inst->byteBuf = (short *)realloc(inst->byteBuf, inst->byteBufSize*sizeof(short));
        if(inst->byteBuf == NULL) {
            return 0;
        }
    }
    LOGV("Writing %d bytes to stream", lenBytes);
    (*env)->GetByteArrayRegion(env, buffer, 0, lenBytes, (jbyte *)inst->byteBuf);
    return sonicWriteShortToStream(stream, inst->byteBuf, samples);
}

// Get bytes representing sped up/slowed down sound and put up to lenBytes
// into ret.
// Returns number of bytes read, or -1 if we run out of memory.
jint Java_org_vinuxproject_sonic_Sonic_receiveBytesNative(
    JNIEnv *env,
    jobject thiz,
    jlong sonicID,
    jbyteArray ret,
    jint lenBytes)
{
    sonicInst inst = getInst(sonicID);
    sonicStream stream = inst->stream;
    int available = sonicSamplesAvailable(stream)*sizeof(short)*sonicGetNumChannels(stream);
    int samplesRead, bytesRead;

    LOGV("Reading %d bytes from stream", lenBytes);
    if(lenBytes > available) {
        lenBytes = available;
    }
    if(lenBytes > inst->byteBufSize*sizeof(short)) {
        inst->byteBufSize = lenBytes*(2/sizeof(short));
        inst->byteBuf = (short *)realloc(inst->byteBuf, inst->byteBufSize*sizeof(short));
        if(inst->byteBuf == NULL) {
            return -1;
        }
    }
    //LOGV("Doing read %d", lenBytes);
    samplesRead = sonicReadShortFromStream(stream, inst->byteBuf,
	lenBytes/(sizeof(short)*sonicGetNumChannels(stream)));
    bytesRead = samplesRead*sizeof(short)*sonicGetNumChannels(stream); 
    //LOGV("Returning %d", samplesRead);
    (*env)->SetByteArrayRegion(env, ret, 0, bytesRead, (jbyte *)inst->byteBuf);
    return bytesRead;
}

// Set pitch in sound alteration object
void Java_org_vinuxproject_sonic_Sonic_setPitchNative(
    JNIEnv *env,
    jobject thiz,
    jlong sonicID,
    jfloat newPitch)
{
    sonicStream stream = getInst(sonicID)->stream;
    LOGV("Set pitch to %f", newPitch);
    sonicSetPitch(stream, newPitch);
}

// Get the current pitch.
jfloat Java_org_vinuxproject_sonic_Sonic_getPitchNative(
    JNIEnv *env,
    jobject thiz,
    jlong sonicID)
{
    sonicStream stream = getInst(sonicID)->stream;
    LOGV("Reading pitch");
    return sonicGetPitch(stream);
}

// Speed up the sound and increase the pitch, or slow down the sound and
// decrease the pitch.
void Java_org_vinuxproject_sonic_Sonic_setRateNative(
    JNIEnv *env,
    jobject thiz,
    jlong sonicID,
    jfloat newRate)
{
    sonicStream stream = getInst(sonicID)->stream;
    LOGV("Set rate to %f", newRate);
    sonicSetRate(stream, newRate);
}

// Return the current playback rate.
jfloat Java_org_vinuxproject_sonic_Sonic_getRateNative(
    JNIEnv *env,
    jobject thiz,
    jlong sonicID)
{
    sonicStream stream = getInst(sonicID)->stream;
    LOGV("Reading rate");
    return sonicGetRate(stream);
}

// Get the current sample rate.
jint Java_org_vinuxproject_sonic_Sonic_getSampleRateNative(
    JNIEnv *env,
    jobject thiz,
    jlong sonicID)
{
    sonicStream stream = getInst(sonicID)->stream;
    LOGV("Reading sample rate");
    return sonicGetSampleRate(stream);
}

// Set the sample rate.
void Java_org_vinuxproject_sonic_Sonic_setSampleRateNative(
    JNIEnv *env,
    jobject thiz,
    jlong sonicID,
    jint newSampleRate)
{
    sonicStream stream = getInst(sonicID)->stream;
    LOGV("Set sample rate to %d", newSampleRate);
    sonicSetSampleRate(stream, newSampleRate);
}

// Get the current number of channels.
jint Java_org_vinuxproject_sonic_Sonic_getNumChannelsNative(
    JNIEnv *env,
    jobject thiz,
    jlong sonicID)
{
    sonicStream stream = getInst(sonicID)->stream;
    LOGV("Reading num channels");
    return sonicGetNumChannels(stream);
}

// Set the number of channels.
void Java_org_vinuxproject_sonic_Sonic_setNumChannelsNative(
    JNIEnv *env,
    jobject thiz,
    jlong sonicID,
    jint newNumChannels)
{
    sonicStream stream = getInst(sonicID)->stream;
    LOGV("Set sample rate to %d", newNumChannels);
    sonicSetNumChannels(stream, newNumChannels);
}

// Get the current speed.
jfloat Java_org_vinuxproject_sonic_Sonic_getSpeedNative(
    JNIEnv *env,
    jobject thiz,
    jlong sonicID)
{
    sonicStream stream = getInst(sonicID)->stream;
    LOGV("Reading speed");
    return sonicGetSpeed(stream);
}

// Change the speed.
void Java_org_vinuxproject_sonic_Sonic_setSpeedNative(
    JNIEnv *env,
    jobject thiz,
    jlong sonicID,
    jfloat newSpeed)
{
    sonicStream stream = getInst(sonicID)->stream;
    LOGV("Set speed to %f", newSpeed);
    sonicSetSpeed(stream, newSpeed);
}

// Get the current volume.
jfloat Java_org_vinuxproject_sonic_Sonic_getVolumeNative(
    JNIEnv *env,
    jobject thiz,
    jlong sonicID)
{
    sonicStream stream = getInst(sonicID)->stream;
    LOGV("Reading volume");
    return sonicGetVolume(stream);
}

// Change the volume.
void Java_org_vinuxproject_sonic_Sonic_setVolumeNative(
    JNIEnv *env,
    jobject thiz,
    jlong sonicID,
    jfloat newVolume)
{
    sonicStream stream = getInst(sonicID)->stream;
    LOGV("Set volume to %f", newVolume);
    sonicSetVolume(stream, newVolume);
}

// Get the current chord pitch setting.
jboolean Java_org_vinuxproject_sonic_Sonic_getChordPitchNative(
    JNIEnv *env,
    jobject thiz,
    jlong sonicID)
{
    sonicStream stream = getInst(sonicID)->stream;
    LOGV("Reading chord pitch");
    return sonicGetChordPitch(stream);
}

// Set chord pitch mode on or off.  Default is off.
void Java_org_vinuxproject_sonic_Sonic_setChordPitchNative(
    JNIEnv *env,
    jobject thiz,
    jlong sonicID,
    jboolean useChordPitch)
{
    sonicStream stream = getInst(sonicID)->stream;
    LOGV("Set chord pitch to %d", useChordPitch);
    sonicSetChordPitch(stream, useChordPitch);
}

// Returns the number of bytes that can be read from the speed alteration
// object
jint Java_org_vinuxproject_sonic_Sonic_availableBytesNative(
    JNIEnv *env,
    jobject thiz,
    jlong sonicID)
{
    sonicStream stream = getInst(sonicID)->stream;
    LOGV("Reading samples available = %d", sonicSamplesAvailable(stream)*sizeof(short)*sonicGetNumChannels(stream));

    return sonicSamplesAvailable(stream)*sizeof(short)*sonicGetNumChannels(stream);
}

// Process any samples still in a sonic buffer.
void Java_org_vinuxproject_sonic_Sonic_flushNative(
    JNIEnv *env,
    jobject thiz,
    jlong sonicID)
{
    sonicStream stream = getInst(sonicID)->stream;
    LOGV("Flushing stream");
    sonicFlushStream(stream);
}
