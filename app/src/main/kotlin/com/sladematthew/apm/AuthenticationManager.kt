package com.sladematthew.apm

import android.os.AsyncTask
import android.os.Environment
import android.preference.PreferenceManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.widget.Toast
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.http.OkHttp3Requestor
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.WriteMode
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.sladematthew.apm.model.Password
import com.sladematthew.apm.model.PasswordList
import java.io.*
import java.math.BigInteger
import java.security.KeyStore
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class AuthenticationManager(var context: android.content.Context) {

    var dropboxClient:DbxClientV2?=null

    var passwordList:PasswordList?=null

    private var masterPassword:String?=null

    private val KEY_NAME = "APM_FINGERPRINT"

    private fun initDropboxClient()
    {
        val accessToken = PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(Constants.SharedPrefs.ACCESS_TOKEN, null)
                ?:Auth.getOAuth2Token()

        PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putString(Constants.SharedPrefs.ACCESS_TOKEN, accessToken)
                .apply()

        if (dropboxClient == null)
            dropboxClient = DbxClientV2(
                    DbxRequestConfig
                            .newBuilder("AndroidPasswordManager")
                            .withHttpRequestor(
                                    OkHttp3Requestor(
                                            OkHttp3Requestor.defaultOkHttpClient()
                                    )
                            )
                            .build(),
                    accessToken
            )
    }

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
        PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putString(
                        Constants.SharedPrefs.MASTER_PASSWORD_HASH,
                        getSHA256Hash(password).substring(2)
                )
                .apply()
    }

    fun checkMasterPassword(password: String):Boolean {
        if(PreferenceManager
                        .getDefaultSharedPreferences(context)
                        .getString(Constants.SharedPrefs.MASTER_PASSWORD_HASH,"") ==
                getSHA256Hash(password).substring(2)) {
            masterPassword = password
            return true
        }
        return false
    }

    fun generatePassword(password: Password):String {
        if(PreferenceManager.getDefaultSharedPreferences(context).contains(Constants.SharedPrefs.MASTER_PASSWORD_HASH))
            return password.prefix.trim() + getMD5Hash(
                    password
                            .label
                            .toLowerCase()
                            .trim()
                            .replace(" ","")
                            + password.version
                            + masterPassword!!.trim()
            ).substring(0,password.length)
        return ""
    }

    fun generatePassword2(password: Password):String {
        if(PreferenceManager.getDefaultSharedPreferences(context).contains(Constants.SharedPrefs.MASTER_PASSWORD_HASH))
            return password.prefix.trim() + getSHA256Hash(
                    password
                            .label
                            .toLowerCase()
                            .trim()
                            .replace(" ","")
                            + password.version
                            + masterPassword!!.trim()
            ).substring(0,password.length)
        return ""
    }

    fun loadPasswordList(callback: ()-> Unit) {
        initDropboxClient()
        class GetPaswordListTask: AsyncTask<Void, Void, Void>()
        {
            override fun doInBackground(vararg p0: Void?): Void?
            {
                val file = File(Environment.getExternalStorageDirectory().toString()+Constants.Misc.LOCAL_FILENAME)

                val outputStream = FileOutputStream(file)

                if (PreferenceManager.getDefaultSharedPreferences(context).contains(Constants.SharedPrefs.ACCESS_TOKEN)) {
                    val downloader = dropboxClient?.files()?.download(Constants.Misc.DROPBOX_FILENAME)
                    downloader?.download(outputStream)
                }
                passwordList = Gson().fromJson(JsonReader(FileReader(file)))
                return null
            }

            override fun onPostExecute(result: Void?) {
                callback()
            }
        }

        if(passwordList==null)
            GetPaswordListTask().execute()
        else
            callback()

    }

    inner class PutPaswordListTask(private var callback:(()->Unit)?): AsyncTask<Void, Void, Boolean>() {
        override fun doInBackground(vararg p0: Void?): Boolean {
            try {
                val passwordFileString = Gson().toJson(passwordList)
                val file = File(Environment.getExternalStorageDirectory().toString()+Constants.Misc.LOCAL_FILENAME)
                file.createNewFile()
                val fOut = FileOutputStream(file)
                val outWriter = OutputStreamWriter(fOut)
                outWriter.append(passwordFileString)
                outWriter.close()
                fOut.close()

                if (PreferenceManager.getDefaultSharedPreferences(context).contains(Constants.SharedPrefs.ACCESS_TOKEN)) {
                    dropboxClient
                            ?.files()
                            ?.uploadBuilder(Constants.Misc.DROPBOX_FILENAME)
                            ?.withMode(WriteMode.OVERWRITE)
                            ?.uploadAndFinish(FileInputStream(file))
                }
                return true

            } catch (e:Exception) {
            }
            return false
        }

        override fun onPostExecute(result: Boolean) {
            if(result) {
                Toast.makeText(context, R.string.toast_save_success, Toast.LENGTH_LONG).show()
                callback?.invoke()
            }
            else
                Toast.makeText(context,R.string.toast_save_error,Toast.LENGTH_LONG).show()
        }
    }

    fun addOrUpdatePassword(password: Password, callback:(()-> Unit)? = null) {
        initDropboxClient()
        loadPasswordList {
            passwordList!!.passwords[password.label] = password
            PutPaswordListTask(callback).execute()
        }
    }

    fun deletePassword(password: Password,callback: () -> Unit) {
        initDropboxClient()
        loadPasswordList{
            if(passwordList!!.passwords.containsKey(password.label)) {
                passwordList!!.passwords.remove(password.label)
                PutPaswordListTask(callback).execute()
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