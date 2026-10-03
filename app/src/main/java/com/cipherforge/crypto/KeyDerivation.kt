package com.cipherforge.crypto

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Вывод ключа шифрования из мастер-пароля пользователя (PBKDF2-HMAC-SHA256).
 *
 * Почему PBKDF2, а не Argon2: Argon2 не входит в стандартную библиотеку JVM/Android
 * и требует внешней зависимости (Bouncy Castle или нативную argon2-jvm), что усложняет
 * сборку. PBKDF2 — часть javax.crypto "из коробки", хорошо поддерживается на Android
 * и остаётся отраслевым стандартом. Позже можно заменить только эту функцию на Argon2,
 * не трогая остальной код (шифрование заметок через AES не изменится).
 */
object KeyDerivation {
    private const val ALGO = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 210_000 // рекомендация OWASP (2023) для PBKDF2-SHA256
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_SIZE = 16

    fun generateSalt(): ByteArray = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }

    fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGO)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    fun saltToString(salt: ByteArray): String = Base64.getEncoder().encodeToString(salt)
    fun saltFromString(s: String): ByteArray = Base64.getDecoder().decode(s)
}
