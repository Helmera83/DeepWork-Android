package com.example.data.crypto

import android.content.Context
import android.util.Base64
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CryptoManager(context: Context) {

  private val prefs = context.getSharedPreferences("taskbreak_e2ee_prefs", Context.MODE_PRIVATE)
  private val secureRandom = SecureRandom()

  private val _isVaultUnlocked = MutableStateFlow(false)
  val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

  private val _hasCustomPassphrase = MutableStateFlow(prefs.contains(KEY_SALT))
  val hasCustomPassphrase: StateFlow<Boolean> = _hasCustomPassphrase.asStateFlow()

  private var activeKey: SecretKey? = null

  init {
    // If no custom password was configured yet, generate a default device-local session key
    if (!prefs.contains(KEY_SALT)) {
      val defaultSalt = ByteArray(SALT_LENGTH_BYTES)
      secureRandom.nextBytes(defaultSalt)
      prefs.edit().putString(KEY_SALT, Base64.encodeToString(defaultSalt, Base64.NO_WRAP)).apply()
    }
  }

  fun setMasterPassphrase(passphrase: String): Boolean {
    if (passphrase.length < 4) return false
    try {
      val salt = getOrCreateSalt()
      val key = deriveKey(passphrase.toCharArray(), salt)
      activeKey = key
      _isVaultUnlocked.value = true
      _hasCustomPassphrase.value = true

      // Store a verification hash to validate password on subsequent unlocks
      val verifier = encryptWithKey("E2EE_VERIFIED_CHECK", key)
      prefs.edit()
        .putString(KEY_VERIFIER, verifier)
        .putBoolean(KEY_HAS_CUSTOM_PASSWORD, true)
        .apply()
      return true
    } catch (e: Exception) {
      e.printStackTrace()
      return false
    }
  }

  fun unlockVault(passphrase: String): Boolean {
    try {
      val salt = getOrCreateSalt()
      val key = deriveKey(passphrase.toCharArray(), salt)
      val verifier = prefs.getString(KEY_VERIFIER, null)
      if (verifier != null) {
        val decrypted = decryptWithKey(verifier, key)
        if (decrypted == "E2EE_VERIFIED_CHECK") {
          activeKey = key
          _isVaultUnlocked.value = true
          return true
        } else {
          return false
        }
      } else {
        // First time initialization with this passphrase
        activeKey = key
        _isVaultUnlocked.value = true
        val check = encryptWithKey("E2EE_VERIFIED_CHECK", key)
        prefs.edit().putString(KEY_VERIFIER, check).apply()
        return true
      }
    } catch (e: Exception) {
      return false
    }
  }

  fun lockVault() {
    activeKey = null
    _isVaultUnlocked.value = false
  }

  fun encryptText(plainText: String): String {
    val key = activeKey ?: deriveDefaultDeviceKey()
    return encryptWithKey(plainText, key)
  }

  fun decryptText(cipherTextWithEnvelope: String): String {
    val key = activeKey ?: deriveDefaultDeviceKey()
    return try {
      decryptWithKey(cipherTextWithEnvelope, key)
    } catch (e: Exception) {
      // Return original or error indicator if not matching
      cipherTextWithEnvelope
    }
  }

  private fun encryptWithKey(plainText: String, secretKey: SecretKey): String {
    val iv = ByteArray(GCM_IV_LENGTH_BYTES)
    secureRandom.nextBytes(iv)
    val cipher = Cipher.getInstance(TRANSFORMATION)
    val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

    val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
    val buffer = ByteBuffer.allocate(iv.size + encryptedBytes.size)
    buffer.put(iv)
    buffer.put(encryptedBytes)

    return Base64.encodeToString(buffer.array(), Base64.NO_WRAP)
  }

  private fun decryptWithKey(cipherTextWithEnvelope: String, secretKey: SecretKey): String {
    val decoded = Base64.decode(cipherTextWithEnvelope, Base64.NO_WRAP)
    if (decoded.size < GCM_IV_LENGTH_BYTES) {
      return cipherTextWithEnvelope
    }
    val buffer = ByteBuffer.wrap(decoded)
    val iv = ByteArray(GCM_IV_LENGTH_BYTES)
    buffer.get(iv)

    val encryptedBytes = ByteArray(buffer.remaining())
    buffer.get(encryptedBytes)

    val cipher = Cipher.getInstance(TRANSFORMATION)
    val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

    val decryptedBytes = cipher.doFinal(encryptedBytes)
    return String(decryptedBytes, Charsets.UTF_8)
  }

  private fun getOrCreateSalt(): ByteArray {
    val saltStr = prefs.getString(KEY_SALT, null)
    return if (saltStr != null) {
      Base64.decode(saltStr, Base64.NO_WRAP)
    } else {
      val salt = ByteArray(SALT_LENGTH_BYTES)
      secureRandom.nextBytes(salt)
      prefs.edit().putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP)).apply()
      salt
    }
  }

  private fun deriveDefaultDeviceKey(): SecretKey {
    val salt = getOrCreateSalt()
    return deriveKey("TASKBREAK_DEFAULT_LOCAL_VAULT_KEY_2026".toCharArray(), salt)
  }

  private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKey {
    val spec = PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val keyBytes = factory.generateSecret(spec).encoded
    return SecretKeySpec(keyBytes, "AES")
  }

  companion object {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val SALT_LENGTH_BYTES = 16
    private const val PBKDF2_ITERATIONS = 12000
    private const val KEY_LENGTH_BITS = 256

    private const val KEY_SALT = "e2ee_salt"
    private const val KEY_VERIFIER = "e2ee_verifier"
    private const val KEY_HAS_CUSTOM_PASSWORD = "e2ee_has_custom_password"
  }
}
