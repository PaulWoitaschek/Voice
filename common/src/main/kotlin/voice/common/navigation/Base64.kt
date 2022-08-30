package voice.common.navigation

import java.util.Base64

internal fun String.base64Encoded(): String = Base64.getEncoder().encodeToString(toByteArray())

internal fun String.base64Decoded(): String = Base64.getDecoder().decode(this).decodeToString()
