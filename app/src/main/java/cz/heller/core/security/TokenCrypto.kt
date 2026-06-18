package cz.heller.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Šifrování citlivých řetězců (Fio tokenů) klíčem uloženým v AndroidKeyStore.
 * Klíč je AES-256, hardware-backed (kde to zařízení umí) a **nikdy neopustí zařízení**
 * — nedá se vyexportovat ani přečíst, jen použít k (de)šifrování přes Keystore.
 *
 * Uložený tvar: `enc1:` + Base64( IV[12 B] ‖ ciphertext+GCM-tag ).
 * Vstup bez prefixu `enc1:` se bere jako legacy plaintext a vrací se beze změny
 * (zpětná kompatibilita — zašifruje se až při příštím zápisu).
 */
object TokenCrypto {

    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "heller_fio_token_key"
    private const val PREFIX = "enc1:"
    private const val IV_LEN = 12
    private const val TAG_BITS = 128
    private const val TRANSFORM = "AES/GCM/NoPadding"

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    /** Zašifruje [plain]; prázdný řetězec nechá být. */
    fun encrypt(plain: String): String {
        if (plain.isEmpty()) return plain
        val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        val iv = cipher.iv
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val packed = iv + ct
        return PREFIX + Base64.encodeToString(packed, Base64.NO_WRAP)
    }

    /**
     * Dešifruje [stored]. Legacy plaintext (bez prefixu) vrací beze změny.
     * Při nečitelnosti (poškozená data / zneplatněný klíč) vrací prázdný řetězec
     * — volající takové připojení zahodí a uživatel token zadá znovu.
     */
    fun decrypt(stored: String): String {
        if (!stored.startsWith(PREFIX)) return stored
        return runCatching {
            val raw = Base64.decode(stored.removePrefix(PREFIX), Base64.NO_WRAP)
            val iv = raw.copyOfRange(0, IV_LEN)
            val ct = raw.copyOfRange(IV_LEN, raw.size)
            val cipher = Cipher.getInstance(TRANSFORM).apply {
                init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
            }
            String(cipher.doFinal(ct), Charsets.UTF_8)
        }.getOrDefault("")
    }
}
