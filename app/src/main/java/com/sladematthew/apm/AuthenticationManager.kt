package com.sladematthew.apm

import android.content.Context
import android.preference.PreferenceManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.sladematthew.apm.model.Password
import com.sladematthew.apm.model.PasswordList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.security.KeyStore
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.KeyGenerator

class AuthenticationManager(var context: Context) {

    var authRepository = AuthRepository(context)
    var drivePasswords = DrivePasswords(context)
    var sharedPreferences = context.getSharedPreferences("com.sladematthew.apm", Context.MODE_PRIVATE)

    var passwordList:PasswordList?=null

    private var masterPassword:String?=null

    private val KEY_NAME = "APM_FINGERPRINT"

    private fun getMD5Hash(encTarget: String): String {
        var mdEnc: MessageDigest? = null
        try {
            mdEnc = MessageDigest.getInstance("MD5")
        } catch (e: NoSuchAlgorithmException) { }
        // Encryption algorithm

        if(mdEnc==null)
            return ""

        mdEnc.update(encTarget.toByteArray(), 0, encTarget.length)
        var md5 = BigInteger(1, mdEnc.digest()).toString(34)
        while (md5.length < 24) {
            md5 += "0"
        }
        return md5
    }

    private fun getSHA256Hash(encTarget: String): String {
        var mdEnc: MessageDigest? = null
        try {
            mdEnc = MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
        }
        // Encryption algorithm

        if(mdEnc==null)
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

    fun setMasterPassword(password: String){
        masterPassword = password
        sharedPreferences
                .edit()
                .putString(
                        Constants.SharedPrefs.MASTER_PASSWORD_HASH,
                        getSHA256Hash(password).substring(2)
                )
                .apply()
    }

    fun checkMasterPassword(password: String):Boolean {
        if(sharedPreferences.getString(Constants.SharedPrefs.MASTER_PASSWORD_HASH,"") ==
                getSHA256Hash(password).substring(2)) {
            masterPassword = password
            return true
        }
        return false
    }

    fun generatePassword(password: Password):String {
        if(sharedPreferences.contains(Constants.SharedPrefs.MASTER_PASSWORD_HASH)) {
            val label = password.label ?: return ""
            val version = password.version ?: 1
            val length = password.length ?: 10
            val prefix = password.prefix ?: ""

            return prefix.trim() + getMD5Hash(
                    label
                            .toLowerCase()
                            .trim()
                            .replace(" ","")
                            + version
                            + masterPassword!!.trim()
            ).substring(0,length)
        }
        return ""
    }

    fun generatePassword2(password: Password):String {
        if(sharedPreferences.contains(Constants.SharedPrefs.MASTER_PASSWORD_HASH)) {
            val label = password.label ?: return ""
            val version = password.version ?: 1
            val length = password.length ?: 10
            val prefix = password.prefix ?: ""

            return prefix.trim() + getSHA256Hash(
                    label
                            .toLowerCase()
                            .trim()
                            .replace(" ","")
                            + version
                            + masterPassword!!.trim()
            ).substring(0,length)
        }
        return ""
    }

    suspend fun loadDrive(){
        drivePasswords.drive = authRepository.getGoogleDrive()
    }

    suspend fun loadPasswordList(){
        val passwordString = drivePasswords.restore() ?: "{\"passwords\":{}}"
        passwordList = Gson().fromJson<PasswordList>(passwordString)
    }

    private fun syncPasswordList(callback: ()-> Unit) {
        GlobalScope.launch(Dispatchers.IO){
            loadPasswordList()
            callback()
            drivePasswords.backup(Gson().toJson(passwordList))
        }
    }

    fun addOrUpdatePassword(password: Password, callback:(()-> Unit)? = null) {
        val label = password.label ?: return
        syncPasswordList {
            passwordList!!.passwords[label] = password
        }
    }

    fun deletePassword(password: Password,callback: () -> Unit) {
        val label = password.label ?: return
        syncPasswordList{
            if(passwordList!!.passwords.containsKey(label)) {
                passwordList!!.passwords.remove(label)
            }
        }
    }

    fun createKey() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            if (keyStore.containsAlias(KEY_NAME))
                return

            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            keyGenerator.init(KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setUserAuthenticationValidityDurationSeconds(10)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build())
            keyGenerator.generateKey()
        } catch (e: Exception) { }
    }
}