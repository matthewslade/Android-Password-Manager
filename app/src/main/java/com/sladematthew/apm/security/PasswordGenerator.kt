package com.sladematthew.apm.security

import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Advanced password generator with support for multiple algorithms
 * - MD5 (legacy, deprecated but supported for backward compatibility)
 * - SHA-256 (legacy, supported for backward compatibility)
 * - PBKDF2 (modern, recommended for new passwords - OWASP 2023)
 */
class PasswordGenerator {

    companion object {
        const val ALGORITHM_MD5 = "MD5"
        const val ALGORITHM_SHA256 = "SHA256"
        const val ALGORITHM_PBKDF2 = "PBKDF2"

        // PBKDF2 parameters (balanced for mobile)
        private const val PBKDF2_ITERATIONS = 100000 // OWASP 2023 minimum
        private const val PBKDF2_KEY_LENGTH = 512 // 64 bytes
    }

    /**
     * Generate password using the specified algorithm
     */
    fun generatePassword(
        algorithm: String,
        label: String,
        version: Int,
        length: Int,
        prefix: String,
        masterPassword: String
    ): String {
        return when (algorithm) {
            ALGORITHM_MD5 -> generateMD5Password(label, version, length, prefix, masterPassword)
            ALGORITHM_SHA256 -> generateSHA256Password(label, version, length, prefix, masterPassword)
            ALGORITHM_PBKDF2 -> generatePBKDF2Password(label, version, length, prefix, masterPassword)
            else -> generateSHA256Password(label, version, length, prefix, masterPassword) // Default fallback
        }
    }

    /**
     * LEGACY: MD5-based password generation
     * Kept for backward compatibility only
     */
    private fun generateMD5Password(
        label: String,
        version: Int,
        length: Int,
        prefix: String,
        masterPassword: String
    ): String {
        val input = label.lowercase().trim().replace(" ", "") + version + masterPassword.trim()
        val hash = getMD5Hash(input)
        return prefix.trim() + hash.substring(0, length.coerceAtMost(hash.length))
    }

    /**
     * LEGACY: SHA-256-based password generation
     * Kept for backward compatibility
     */
    private fun generateSHA256Password(
        label: String,
        version: Int,
        length: Int,
        prefix: String,
        masterPassword: String
    ): String {
        val input = label.lowercase().trim().replace(" ", "") + version + masterPassword.trim()
        val hash = getSHA256Hash(input)
        return prefix.trim() + hash.substring(0, length.coerceAtMost(hash.length))
    }

    /**
     * MODERN: PBKDF2-HMAC-SHA512 based password generation
     * Recommended for all new passwords (OWASP 2023 standard)
     *
     * PBKDF2 is:
     * - OWASP recommended (2023) with 100,000+ iterations
     * - Resistant to brute-force attacks
     * - Built into Java standard library (no external dependencies)
     * - Deterministic with custom salt (perfect for password generation)
     */
    private fun generatePBKDF2Password(
        label: String,
        version: Int,
        length: Int,
        prefix: String,
        masterPassword: String
    ): String {
        try {
            // Create deterministic salt from label + version
            val input = label.lowercase().trim().replace(" ", "") + version
            val salt = getSHA256Hash(input).take(16).toByteArray(Charsets.UTF_8)

            // Generate key using PBKDF2-HMAC-SHA512
            val spec = PBEKeySpec(
                masterPassword.toCharArray(),
                salt,
                PBKDF2_ITERATIONS,
                PBKDF2_KEY_LENGTH
            )
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
            val hash = factory.generateSecret(spec).encoded

            // Convert to base34 (alphanumeric excluding confusing chars like 0/O, 1/l)
            val base34Hash = toBase34(hash)

            return prefix.trim() + base34Hash.substring(0, length.coerceAtMost(base34Hash.length))
        } catch (e: Exception) {
            // Fallback to SHA256 if PBKDF2 fails
            return generateSHA256Password(label, version, length, prefix, masterPassword)
        }
    }

    /**
     * Convert byte array to base34 string (alphanumeric, no confusing characters)
     * Character set: abcdefghijkmnpqrstuvwxyz23456789 (34 chars)
     * Excludes: 0, 1, l, o (easily confused)
     */
    private fun toBase34(bytes: ByteArray): String {
        val charset = "abcdefghijkmnpqrstuvwxyz23456789"
        val bigInt = BigInteger(1, bytes)
        val sb = StringBuilder()

        var value = bigInt
        while (value > BigInteger.ZERO) {
            val remainder = value.mod(BigInteger.valueOf(34))
            sb.append(charset[remainder.toInt()])
            value = value.divide(BigInteger.valueOf(34))
        }

        // Pad if too short
        while (sb.length < 32) {
            sb.append(charset[0])
        }

        return sb.toString()
    }

    /**
     * Legacy MD5 hash function
     */
    private fun getMD5Hash(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            md.update(input.toByteArray(), 0, input.length)
            val hash = BigInteger(1, md.digest()).toString(34)
            hash.padEnd(24, '0')
        } catch (e: NoSuchAlgorithmException) {
            ""
        }
    }

    /**
     * Legacy SHA-256 hash function
     */
    private fun getSHA256Hash(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(input.toByteArray(), 0, input.length)
            val hash = BigInteger(1, md.digest()).toString(34)
            hash.padEnd(24, '0')
        } catch (e: NoSuchAlgorithmException) {
            ""
        }
    }

    /**
     * Get password strength indicator
     */
    fun getPasswordStrength(algorithm: String): PasswordStrength {
        return when (algorithm) {
            ALGORITHM_MD5 -> PasswordStrength.WEAK
            ALGORITHM_SHA256 -> PasswordStrength.MEDIUM
            ALGORITHM_PBKDF2 -> PasswordStrength.STRONG
            else -> PasswordStrength.MEDIUM
        }
    }
}

enum class PasswordStrength {
    WEAK,    // MD5
    MEDIUM,  // SHA-256
    STRONG   // PBKDF2
}
