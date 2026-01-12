package com.sladematthew.apm.repository

import android.content.Context
import android.content.SharedPreferences
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.sladematthew.apm.AuthRepository
import com.sladematthew.apm.Constants
import com.sladematthew.apm.DrivePasswords
import com.sladematthew.apm.model.Password
import com.sladematthew.apm.model.PasswordList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class PasswordRepository(context: Context) {

    private val authRepository = AuthRepository(context)
    private val drivePasswords = DrivePasswords(context)
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("com.sladematthew.apm", Context.MODE_PRIVATE)

    // Thread-safe password list access
    private val passwordListMutex = Mutex()
    private var _passwordList: PasswordList? = null

    private var masterPassword: String? = null

    // Thread-safe access to password list
    suspend fun getPasswordList(): PasswordList? = passwordListMutex.withLock {
        _passwordList
    }

    private suspend fun setPasswordList(list: PasswordList?) = passwordListMutex.withLock {
        _passwordList = list
    }

    /**
     * LEGACY: MD5 hash function - preserved for backward compatibility
     * DO NOT use for new password generation
     */
    private fun getMD5Hash(encTarget: String): String {
        var mdEnc: MessageDigest? = null
        try {
            mdEnc = MessageDigest.getInstance("MD5")
        } catch (e: NoSuchAlgorithmException) {
        }

        if (mdEnc == null)
            return ""

        mdEnc.update(encTarget.toByteArray(), 0, encTarget.length)
        var md5 = BigInteger(1, mdEnc.digest()).toString(34)
        while (md5.length < 24) {
            md5 += "0"
        }
        return md5
    }

    /**
     * SHA-256 hash function for password generation
     */
    private fun getSHA256Hash(encTarget: String): String {
        var mdEnc: MessageDigest? = null
        try {
            mdEnc = MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
        }

        if (mdEnc == null)
            return ""

        mdEnc.update(encTarget.toByteArray(), 0, encTarget.length)
        var md5 = BigInteger(1, mdEnc.digest()).toString(34)
        while (md5.length < 24) {
            md5 += "0"
        }
        return md5
    }

    fun clearMasterPassword() {
        masterPassword = ""
    }

    fun setMasterPassword(password: String) {
        masterPassword = password
        sharedPreferences
            .edit()
            .putString(
                Constants.SharedPrefs.MASTER_PASSWORD_HASH,
                getSHA256Hash(password).substring(2)
            )
            .apply()
    }

    fun checkMasterPassword(password: String): Boolean {
        if (sharedPreferences.getString(Constants.SharedPrefs.MASTER_PASSWORD_HASH, "") ==
            getSHA256Hash(password).substring(2)
        ) {
            masterPassword = password
            return true
        }
        return false
    }

    /**
     * LEGACY: Generate password using MD5 hash
     * Preserved for backward compatibility with existing passwords
     */
    fun generatePassword(password: Password): String {
        if (sharedPreferences.contains(Constants.SharedPrefs.MASTER_PASSWORD_HASH))
            return password.prefix.trim() + getMD5Hash(
                password
                    .label
                    .toLowerCase()
                    .trim()
                    .replace(" ", "")
                        + password.version
                        + masterPassword!!.trim()
            ).substring(0, password.length)
        return ""
    }

    /**
     * Generate password using SHA-256 hash
     */
    fun generatePassword2(password: Password): String {
        if (sharedPreferences.contains(Constants.SharedPrefs.MASTER_PASSWORD_HASH))
            return password.prefix.trim() + getSHA256Hash(
                password
                    .label
                    .toLowerCase()
                    .trim()
                    .replace(" ", "")
                        + password.version
                        + masterPassword!!.trim()
            ).substring(0, password.length)
        return ""
    }

    suspend fun loadDrive(): Boolean {
        drivePasswords.drive = authRepository.getGoogleDrive()
        return drivePasswords.drive != null
    }

    suspend fun loadPasswordList() {
        withContext(Dispatchers.IO) {
            val passwordString = drivePasswords.restore() ?: "{\"passwords\":{}}"
            val list = Gson().fromJson<PasswordList>(passwordString)
            setPasswordList(list)
        }
    }

    suspend fun addOrUpdatePassword(password: Password) {
        withContext(Dispatchers.IO) {
            syncPasswordList {
                passwordListMutex.withLock {
                    _passwordList!!.passwords[password.label] = password
                }
            }
        }
    }

    suspend fun deletePassword(password: Password) {
        withContext(Dispatchers.IO) {
            syncPasswordList {
                passwordListMutex.withLock {
                    if (_passwordList!!.passwords.containsKey(password.label)) {
                        _passwordList!!.passwords.remove(password.label)
                    }
                }
            }
        }
    }

    private suspend fun syncPasswordList(mutation: suspend () -> Unit) {
        withContext(Dispatchers.IO) {
            loadPasswordList()
            mutation()
            drivePasswords.backup(Gson().toJson(_passwordList))
        }
    }

    fun hasMasterPassword(): Boolean {
        return sharedPreferences.contains(Constants.SharedPrefs.MASTER_PASSWORD_HASH)
    }

    fun hasDriveConnection(): Boolean {
        return drivePasswords.drive != null
    }
}
