package com.cipherforge.crypto

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec
import javax.crypto.KeyAgreement
import javax.crypto.spec.SecretKeySpec

/**
 * ECDH (Elliptic Curve Diffie-Hellman) — обмен ключами на эллиптических кривых.
 *
 * Важно: сама ECC не шифрует данные напрямую (в отличие от RSA). Два участника
 * обмениваются публичными ключами по открытому каналу, и каждый независимо
 * вычисляет один и тот же общий секрет — он никогда не передаётся по сети.
 * Из этого секрета выводится симметричный AES-ключ, которым уже шифруется
 * реальное сообщение (через уже готовый AesCipher).
 */
object EcdhKeyExchange {
    private const val CURVE = "secp256r1"

    fun generateKeyPair(): KeyPair {
        val gen = KeyPairGenerator.getInstance("EC")
        gen.initialize(ECGenParameterSpec(CURVE))
        return gen.generateKeyPair()
    }

    /** Каждая сторона вызывает это со своим приватным ключом и публичным ключом собеседника. */
    fun deriveSharedAesKey(myPrivateKey: PrivateKey, theirPublicKey: PublicKey): SecretKeySpec {
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(myPrivateKey)
        agreement.doPhase(theirPublicKey, true)
        val sharedSecret = agreement.generateSecret()

        // Общий секрет ECDH — не готовый ключ, а просто число.
        // SHA-256 превращает его в равномерный 256-битный AES-ключ (упрощённый KDF).
        val keyBytes = MessageDigest.getInstance("SHA-256").digest(sharedSecret)
        return SecretKeySpec(keyBytes, "AES")
    }
}
