package xyz.jadonfowler.sounds.structure

import java.security.MessageDigest

private val HEX_CHARS = "0123456789abcdef".toCharArray()

fun ByteArray.toHex(): String {
    val result = StringBuffer()

    forEach {
        val octet = it.toInt()
        val firstIndex = (octet and 0xF0).ushr(4)
        val secondIndex = octet and 0x0F
        result.append(HEX_CHARS[firstIndex])
        result.append(HEX_CHARS[secondIndex])
    }

    return result.toString()
}

fun ByteArray.md5Hash(): String {
    val md = MessageDigest.getInstance("MD5")
    md.update(this)
    val digest = md.digest()
    return digest.toHex()
}

fun String.md5Hash(): String {
    return toByteArray().md5Hash()
}
