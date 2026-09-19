package com.cipherforge.crypto

import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/** Кодирование публичных/приватных ключей в base64-строки для хранения и передачи (QR-код, файл). */
object KeyCodec {
    fun encodePublicKey(key: PublicKey): String = Base64.getEncoder().encodeToString(key.encoded)
    fun encodePrivateKey(key: PrivateKey): String = Base64.getEncoder().encodeToString(key.encoded)

    fun decodePublicKey(base64: String, algorithm: String): PublicKey {
        val bytes = Base64.getDecoder().decode(base64)
        return KeyFactory.getInstance(algorithm).generatePublic(X509EncodedKeySpec(bytes))
    }

    fun decodePrivateKey(base64: String, algorithm: String): PrivateKey {
        val bytes = Base64.getDecoder().decode(base64)
        return KeyFactory.getInstance(algorithm).generatePrivate(PKCS8EncodedKeySpec(bytes))
    }
}
