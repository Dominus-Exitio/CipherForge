package com.cipherforge.crypto

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM — основной рабочий алгоритм симметричного шифрования.
 * GCM даёт и конфиденциальность, и проверку целостности (authentication tag) "из коробки".
 * Логика перенесена из проверенного прототипа CipherCore.kt без изменений.
 */
object AesCipher {
    private const val ALGO = "AES/GCM/NoPadding"
    private const val KEY_SIZE = 256
    private const val IV_SIZE = 12       // рекомендуемый размер IV для GCM
    private const val TAG_LENGTH = 128   // бит

    fun generateKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(KEY_SIZE)
        return keyGen.generateKey()
    }

    /** Результат: base64(iv + ciphertext+tag) — удобно для отображения, хранения и передачи одной строкой. */
    fun encrypt(plainText: String, key: SecretKey): String {
        val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(ALGO)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH, iv))
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(iv + cipherText)
    }

    fun decrypt(encoded: String, key: SecretKey): String {
        val data = Base64.getDecoder().decode(encoded)
        val iv = data.copyOfRange(0, IV_SIZE)
        val cipherText = data.copyOfRange(IV_SIZE, data.size)
        val cipher = Cipher.getInstance(ALGO)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH, iv))
        val plain = cipher.doFinal(cipherText)
        return String(plain, Charsets.UTF_8)
    }

    fun keyToString(key: SecretKey): String = Base64.getEncoder().encodeToString(key.encoded)
    fun keyFromString(s: String): SecretKey =
        SecretKeySpec(Base64.getDecoder().decode(s), "AES")
}
