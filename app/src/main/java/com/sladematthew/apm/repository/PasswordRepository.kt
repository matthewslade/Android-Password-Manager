package com.sladematthew.apm.repository

import android.content.Context
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.sladematthew.apm.AuthRepository
import com.sladematthew.apm.Constants
import com.sladematthew.apm.DrivePasswords
import com.sladematthew.apm.model.Password
import com.sladematthew.apm.model.PasswordList
import com.sladematthew.apm.security.PasswordGenerator
import com.sladematthew.apm.security.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PasswordRepository(context: Context) {

    private val authRepository = AuthRepository(context)
    private val drivePasswords = DrivePasswords(context)
    private val secureStorage = SecureStorage(context)
    private val passwordGenerator = PasswordGenerator()

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

    fun clearMasterPassword() {
        masterPassword = ""
    }

    fun setMasterPassword(password: String) {
        masterPassword = password
        // Store hash using SHA-256 for master password verification
        val hash = passwordGenerator.generatePassword(
            PasswordGenerator.ALGORITHM_SHA256,
            "",
            0,
            32,
            "",
            password
        )
        secureStorage.putString(Constants.SharedPrefs.MASTER_PASSWORD_HASH, hash.substring(2))
    }

    fun checkMasterPassword(password: String): Boolean {
        val storedHash = secureStorage.getString(Constants.SharedPrefs.MASTER_PASSWORD_HASH, "")
        val inputHash = passwordGenerator.generatePassword(
            PasswordGenerator.ALGORITHM_SHA256,
            "",
            0,
            32,
            "",
            password
        ).substring(2)

        if (storedHash == inputHash) {
            masterPassword = password
            return true
        }
        return false
    }

    /**
     * Generate password using the algorithm specified in the Password object
     * Supports backward compatibility with MD5 and SHA-256, and modern Argon2id
     */
    fun generatePasswordFromModel(password: Password): String {
        if (!hasMasterPassword() || masterPassword == null) return ""

        val algorithm = password.algorithm ?: Constants.Misc.ALGORITHM_LEGACY_SHA256
        val label = password.label ?: return ""
        val version = password.version ?: 1
        val length = password.length ?: Constants.Misc.DEFAULT_LENGTH
        val prefix = password.prefix ?: ""

        return passwordGenerator.generatePassword(
            algorithm,
            label,
            version,
            length,
            prefix,
            masterPassword!!
        )
    }

    /**
     * LEGACY: Generate password using MD5 hash
     * Preserved for backward compatibility with existing passwords
     * @deprecated Use generatePasswordFromModel instead
     */
    @Deprecated("Use generatePasswordFromModel instead")
    fun generatePassword(password: Password): String {
        if (!hasMasterPassword() || masterPassword == null) return ""

        val label = password.label ?: return ""
        val version = password.version ?: 1
        val length = password.length ?: Constants.Misc.DEFAULT_LENGTH
        val prefix = password.prefix ?: ""

        return passwordGenerator.generatePassword(
            PasswordGenerator.ALGORITHM_MD5,
            label,
            version,
            length,
            prefix,
            masterPassword!!
        )
    }

    /**
     * LEGACY: Generate password using SHA-256 hash
     * Preserved for backward compatibility
     * @deprecated Use generatePasswordFromModel instead
     */
    @Deprecated("Use generatePasswordFromModel instead")
    fun generatePassword2(password: Password): String {
        if (!hasMasterPassword() || masterPassword == null) return ""

        val label = password.label ?: return ""
        val version = password.version ?: 1
        val length = password.length ?: Constants.Misc.DEFAULT_LENGTH
        val prefix = password.prefix ?: ""

        return passwordGenerator.generatePassword(
            PasswordGenerator.ALGORITHM_SHA256,
            label,
            version,
            length,
            prefix,
            masterPassword!!
        )
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
            val label = password.label ?: return@withContext
            syncPasswordList {
                passwordListMutex.withLock {
                    _passwordList!!.passwords[label] = password
                }
            }
        }
    }

    suspend fun deletePassword(password: Password) {
        withContext(Dispatchers.IO) {
            val label = password.label ?: return@withContext
            syncPasswordList {
                passwordListMutex.withLock {
                    if (_passwordList!!.passwords.containsKey(label)) {
                        _passwordList!!.passwords.remove(label)
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
        return secureStorage.contains(Constants.SharedPrefs.MASTER_PASSWORD_HASH)
    }

    suspend fun hasDriveConnection(): Boolean {
        // First check if we have the drive object
        if (drivePasswords.drive != null) return true

        // If not, check if Firebase has a signed-in user and try to load drive
        val isSignedIn = authRepository.isSignedIn()
        if (isSignedIn) {
            drivePasswords.drive = authRepository.getGoogleDrive()
            return drivePasswords.drive != null
        }

        return false
    }
}
