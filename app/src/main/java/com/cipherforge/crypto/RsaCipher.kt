package com.cipherforge.crypto

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.MGF1ParameterSpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec

/**
 * RSA сам по себе умеет шифровать только короткие данные (для 2048 бит + OAEP-SHA256 —
 * максимум ~190 байт), поэтому для реальных сообщений используется гибридная схема:
 * сообщение шифруется AES-256-GCM случайным одноразовым ключом, а сам этот AES-ключ
 * шифруется RSA публичным ключом получателя. Расшифровать может только тот, у кого
 * есть соответствующий приватный RSA-ключ.
 */
object RsaCipher {
    private const val KEY_SIZE = 2048
    private const val TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"

    private val oaepParams = OAEPParameterSpec(
        "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT
    )

    fun generateKeyPair(): KeyPair {
        val gen = KeyPairGenerator.getInstance("RSA")
        gen.initialize(KEY_SIZE)
        return gen.generateKeyPair()
    }

    /** Формат результата: base64(RSA-зашифрованный AES-ключ) + "." + base64(AES-шифротекст). */
    fun encryptHybrid(plainText: String, recipientPublicKey: PublicKey): String {
        val aesKey = AesCipher.generateKey()
        val cipherText = AesCipher.encrypt(plainText, aesKey)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, recipientPublicKey, oaepParams)
        val encryptedKey = cipher.doFinal(aesKey.encoded)

        return Base64.getEncoder().encodeToString(encryptedKey) + "." + cipherText
    }

    fun decryptHybrid(payload: String, myPrivateKey: PrivateKey): String {
        val parts = payload.split(".", limit = 2)
        require(parts.size == 2) { "Некорректный формат зашифрованных данных" }
        val (encryptedKeyB64, cipherText) = parts

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, myPrivateKey, oaepParams)
        val aesKeyBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedKeyB64))

        return AesCipher.decrypt(cipherText, SecretKeySpec(aesKeyBytes, "AES"))
    }
}
