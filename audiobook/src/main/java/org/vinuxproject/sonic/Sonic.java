/**
 * Copyright: Bill Cox
 * License: LGPL
 * Modified: Paul Woitaschek
 */
package org.vinuxproject.sonic;

@SuppressWarnings("ALL")
public class Sonic {
    static {
        System.loadLibrary("sonic");
    }

    // Sonic is thread-safe, but to have multiple instances of it, we have to
    // store a pointer to it's data. We store that here as a long, just in case
    // someone wants to port this JNI wapper to a 64-bit JVM.
    private long sonicID = 0;

    // Create a sonic stream. Return false only if we are out of memory and cannot
    // allocate the stream. Set numChannels to 1 for mono, and 2 for stereo.
    public Sonic(int sampleRate, int numChannels) {
        close();
        sonicID = initNative(sampleRate, numChannels);
    }

    // Call this to clean up memory after you're done processing sound.
    void close() {
        if (sonicID != 0) {
            closeNative(sonicID);
            sonicID = 0;
        }
    }

    private native long initNative(int sampleRate, int channels);

    // When done with sound processing, it's best to call this method to clean up memory.
    private native void closeNative(long sonicID);

    // Just insure that close gets called, in case the user forgot.
    protected void finalize() {
        // It is safe to call this twice, in case the user already did.
        close();
    }

    // Force the sonic stream to generate output using whatever data it currently
    // has. No extra delay will be added to the output, but flushing in the middle of
    // words could introduce distortion.
    public void flush() {
        flushNative(sonicID);
    }

    private native void flushNative(long sonicID);

    // Get the sample rate of the stream.
    public int getSampleRate() {
        return getSampleRateNative(sonicID);
    }

    // Set the sample rate of the stream. This will drop any samples that have not been read.
    public void setSampleRate(int newSampleRate) {
        setSampleRateNative(sonicID, newSampleRate);
    }

    // Note that changing the sample rate or num channels will cause a flush.
    private native void setSampleRateNative(long sonicID, int newSampleRate);

    private native int getSampleRateNative(long sonicID);

    // Get the number of channels.
    public int getNumChannels() {
        return getNumChannelsNative(sonicID);
    }

    // Set the number of channels. This will drop any samples that have not been read.
    public void setNumChannels(int newNumChannels) {
        setNumChannelsNative(sonicID, newNumChannels);
    }

    private native void setNumChannelsNative(long sonicID, int newNumChannels);

    private native int getNumChannelsNative(long sonicID);

    // Get the pitch of the stream.
    public float getPitch() {
        return getPitchNative(sonicID);
    }

    // Set the pitch of the stream.
    public void setPitch(float newPitch) {
        setPitchNative(sonicID, newPitch);
    }

    private native void setPitchNative(long sonicID, float newPitch);

    private native float getPitchNative(long sonicID);

    // Get the speed of the stream.
    public float getSpeed() {
        return getSpeedNative(sonicID);
    }

    //Set the speed of the stream.
    public void setSpeed(float newSpeed) {
        setSpeedNative(sonicID, newSpeed);
    }

    private native void setSpeedNative(long sonicID, float newSpeed);

    private native float getSpeedNative(long sonicID);

    // Get the rate of the stream.
    public float getRate() {
        return getRateNative(sonicID);
    }

    // Set the rate of the stream. Rate means how fast we play, without pitch correction
// You probably just want to use setSpeed and setPitch instead.
    public void setRate(float newRate) {
        setRateNative(sonicID, newRate);
    }

    private native void setRateNative(long sonicID, float newRate);

    private native float getRateNative(long sonicID);

    // Get the chord pitch setting.
    public boolean getChordPitch() {
        return getChordPitchNative(sonicID);
    }

    // Set chord pitch mode on or off. Default is off. See the documentation
    // page for a description of this feature.
    public void setChordPitch(boolean useChordPitch) {
        setChordPitchNative(sonicID, useChordPitch);
    }

    private native void setChordPitchNative(long sonicID, boolean useChordPitch);

    private native boolean getChordPitchNative(long sonicID);

    // Use this to write 16-bit data to be speed up or down into the stream.
    // Return false if memory realloc failed, otherwise true.
    public boolean putBytes(byte[] buffer, int lenBytes) {
        return putBytesNative(sonicID, buffer, lenBytes);
    }

    private native boolean putBytesNative(long sonicID, byte[] buffer, int lenBytes);

    // Use this to read 16-bit data out of the stream. Sometimes no data will
    // be available, and zero is returned, which is not an error condition.
    public int receiveBytes(byte[] ret, int lenBytes) {
        return receiveBytesNative(sonicID, ret, lenBytes);
    }

    private native int receiveBytesNative(long sonicID, byte[] ret, int lenBytes);

    // Return the number of samples in the output buffer
    public int availableBytes() {
        return availableBytesNative(sonicID);
    }

    private native int availableBytesNative(long sonicID);

    // Get the scaling factor of the stream.
    public float getVolume() {
        return getVolumeNative(sonicID);
    }

    // Set the scaling factor of the stream.
    public void setVolume(float newVolume) {
        setVolumeNative(sonicID, newVolume);
    }

    private native void setVolumeNative(long sonicID, float newVolume);

    private native float getVolumeNative(long sonicID);
}