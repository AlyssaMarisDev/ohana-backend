package com.ohana.auth.utils

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

class Hasher {
    companion object {
        fun generateSalt(): ByteArray {
            val salt = ByteArray(16)
            SecureRandom().nextBytes(salt)
            return salt
        }

        fun hashPassword(
            password: String,
            salt: ByteArray,
        ): String {
            val md = MessageDigest.getInstance("SHA-512")
            md.update(salt)
            val hashedPassword = md.digest(password.toByteArray())
            return Base64.getEncoder().encodeToString(hashedPassword)
        }
    }
}
