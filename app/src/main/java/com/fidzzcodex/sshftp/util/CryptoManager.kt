package com.fidzzcodex.sshftp.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import android.util.Base64

object CryptoManager {
    private const val ALIAS       = "sshftp_master_key"
    private const val PROVIDER    = "AndroidKeyStore"
    private const val ALGORITHM   = KeyProperties.KEY_ALGORITHM_AES
    private const val BLOCK_MODE  = KeyProperties.BLOCK_MODE_CBC
    private const val PADDING     = KeyProperties.ENCRYPTION_PADDING_PKCS7
    private const val TRANSFORM   = "$ALGORITHM/$BLOCK_MODE/$PADDING"

    private val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

    private fun getOrCreateKey(): SecretKey {
        keyStore.getKey(ALIAS, null)?.let { return it as SecretKey }
        val spec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()
        return KeyGenerator.getInstance(ALGORITHM, PROVIDER)
            .apply { init(spec) }
            .generateKey()
    }

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv         = cipher.iv
        val encrypted  = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined   = iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String {
        val combined  = Base64.decode(encoded, Base64.NO_WRAP)
        val iv        = combined.copyOfRange(0, 16)
        val encrypted = combined.copyOfRange(16, combined.size)
        val cipher    = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), IvParameterSpec(iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    /** Generate a stable DB passphrase tied to this device keystore */
    fun getDbPassphrase(): ByteArray {
        val key = "sshftp_db_passphrase"
        return encrypt(key).toByteArray(Charsets.UTF_8).copyOf(32)
    }
}
