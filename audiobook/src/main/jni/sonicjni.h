/*
 * Class:     org_vinuxproject_sonic_Sonic
 * Method:    _init
 * Signature: (II)J
 */
jlong Java_org_vinuxproject_sonic_Sonic_initNativeNative
  (JNIEnv *, jobject, jint, jint);

/*
 * Class:     org_vinuxproject_sonic_Sonic
 * Method:    _close
 * Signature: (J)V
 */
void Java_org_vinuxproject_sonic_Sonic_closeNative
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_vinuxproject_sonic_Sonic
 * Method:    _flush
 * Signature: (J)V
 */
void Java_org_vinuxproject_sonic_Sonic_flushNative
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_vinuxproject_sonic_Sonic
 * Method:    _setSampleRate
 * Signature: (JI)V
 */
void Java_org_vinuxproject_sonic_Sonic_setSampleRateNative
  (JNIEnv *, jobject, jlong, jint);

/*
 * Class:     org_vinuxproject_sonic_Sonic
 * Method:    _getSampleRate
 * Signature: (J)I
 */
jint Java_org_vinuxproject_sonic_Sonic_getSampleRateNative
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_vinuxproject_sonic_Sonic
 * Method:    _setNumChannels
 * Signature: (JI)V
 */
void Java_org_vinuxproject_sonic_Sonic_setNumChannelsNative
  (JNIEnv *, jobject, jlong, jint);

/*
 * Class:     org_vinuxproject_sonic_Sonic
 * Method:    _getNumChannels
 * Signature: (J)I
 */
jint Java_org_vinuxproject_sonic_Sonic_getNumChannelsNative
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_vinuxproject_sonic_Sonic
 * Method:    _setPitch
 * Signature: (JF)V
 */
void Java_org_vinuxproject_sonic_Sonic_setPitchNative
  (JNIEnv *, jobject, jlong, jfloat);

/*
 * Class:     org_vinuxproject_sonic_Sonic
 * Method:    _getPitch
 * Signature: (J)F
 */
jfloat Java_org_vinuxproject_sonic_Sonic_getPitchNative
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_vinuxproject_sonic_Sonic
 * Method:    _setSpeed
 * Signature: (JF)V
 */
void Java_org_vinuxproject_sonic_Sonic_setSpeedNative
  (JNIEnv *, jobject, jlong, jfloat);

/*
 * Class:     org_vinuxproject_sonic_Sonic
 * Method:    _getSpeed
 * Signature: (J)F
 */
jfloat Java_org_vinuxproject_sonic_Sonic_getSpeedNative
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_vinuxproject_sonic_Sonic
 * Method:    _setRate
 * Signature: (JF)V
 */
void Java_org_vinuxproject_sonic_Sonic_setRateNative
  (JNIEnv *, jobject, jlong, jfloat);

/*
 * Class:     org_vinuxproject_sonic_Sonic
 * Method:    _getRate
 * Signature: (J)F
 */
jfloat Java_org_vinuxproject_sonic_Sonic_getRateNative
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_vinuxproject_sonic_Sonic
 * Method:    _setChordPitch
 * Signature: (JZ)V
 */
void Java_org_vinuxproject_sonic_Sonic_setChordPitchNative
  (JNIEnv *, jobject, jlong, jboolean);

/*
 * Class:     org_vinuxproject_sonic_Sonic
 * Method:    _getChordPitch
 * Signature: (J)Z
 */
jboolean Java_org_vinuxproject_sonic_Sonic_getChordPitchNative
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_vinuxproject_sonic_Sonic
 * Method:    _putBytes
 * Signature: (J[BI)Z
 */
jboolean Java_org_vinuxproject_sonic_Sonic_putBytesNative
  (JNIEnv *, jobject, jlong, jbyteArray, jint);

/*
 * Class:     org_vinuxproject_sonic_Sonic
 * Method:    _receiveBytes
 * Signature: (J[BI)I
 */
jint Java_org_vinuxproject_sonic_Sonic_receiveBytesNative
  (JNIEnv *, jobject, jlong, jbyteArray, jint);

/*
 * Class:     org_vinuxproject_sonic_Sonic
 * Method:    _availableBytes
 * Signature: (J)I
 */
jint Java_org_vinuxproject_sonic_Sonic_availableBytesNative
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_vinuxproject_sonic_Sonic
 * Method:    _setVolume
 * Signature: (JF)V
 */
void Java_org_vinuxproject_sonic_Sonic_setVolumeNative
  (JNIEnv *, jobject, jlong, jfloat);

/*
 * Class:     org_vinuxproject_sonic_Sonic
 * Method:    _getVolume
 * Signature: (J)F
 */
jfloat Java_org_vinuxproject_sonic_Sonic_getVolumeNative
  (JNIEnv *, jobject, jlong);
