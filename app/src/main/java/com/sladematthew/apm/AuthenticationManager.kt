package com.sladematthew.apm

import android.content.Context
import android.os.AsyncTask
import android.os.Environment
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.http.OkHttp3Requestor
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.WriteMode
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.sladematthew.apm.model.Password
import com.sladematthew.apm.model.PasswordList
import kotlinx.android.synthetic.main.activity_start.*
import java.io.*
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

class AuthenticationManager(var context: android.content.Context) {

    var dropboxClient:DbxClientV2?=null

    var passwordList:PasswordList?=null

    private var masterPassword:String?=null

    private var log = Log.getLogger(AuthenticationManager::class.java)

    fun initDropboxClient()
    {
        var accessToken = PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.SharedPrefs.ACCESS_TOKEN, null)
        if (accessToken == null)
        {
            accessToken = Auth.getOAuth2Token()

        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(Constants.SharedPrefs.ACCESS_TOKEN, accessToken).apply()
        if (dropboxClient == null)
        {
            val requestConfig = DbxRequestConfig.newBuilder("AndroidPasswordManager").withHttpRequestor(OkHttp3Requestor.INSTANCE).build()
            dropboxClient = DbxClientV2(requestConfig, accessToken)
        }
    }

    fun getMD5EncryptedString(encTarget: String): String {
        var mdEnc: MessageDigest? = null
        try {
            mdEnc = MessageDigest.getInstance("MD5")
        } catch (e: NoSuchAlgorithmException) {
            println("Exception while encrypting to md5")
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

    fun clearMasterPassword()
    {
        masterPassword = ""
    }

    fun setMasterPassword(password: String){
        masterPassword = password
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(Constants.SharedPrefs.MASTER_PASSWORD_HASH,getMD5EncryptedString(password).substring(2)).apply()
    }

    fun checkMasterPassword(password: String):Boolean
    {
        var md5Password = getMD5EncryptedString(password).substring(2)
        if(PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.SharedPrefs.MASTER_PASSWORD_HASH,"").equals(md5Password)) {
            log.debug(PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.SharedPrefs.MASTER_PASSWORD_HASH,""))
            masterPassword = password
            return true
        }
        return false
    }

    fun generatePassword(password: Password):String
    {
        if(PreferenceManager.getDefaultSharedPreferences(context).contains(Constants.SharedPrefs.MASTER_PASSWORD_HASH))
            return password.prefix.trim()+getMD5EncryptedString(password.label.toLowerCase().trim().replace(" ","")+password.version+masterPassword!!.trim()).substring(0,password.length)
        return ""
    }

    fun loadPasswordList(callback: ()-> Unit)
    {
        initDropboxClient()
        class GetPaswordListTask: AsyncTask<Void, Void, Void>()
        {
            override fun doInBackground(vararg p0: Void?): Void?
            {
                var file = File(Environment.getExternalStorageDirectory().toString()+Constants.Misc.LOCAL_FILENAME)

                var outputStream = FileOutputStream(file)

                if (PreferenceManager.getDefaultSharedPreferences(context).contains(Constants.SharedPrefs.ACCESS_TOKEN)) {
                    var downloader = dropboxClient?.files()?.download(Constants.Misc.DROPBOX_FILENAME)
                    downloader?.download(outputStream)
                }

                var reader = JsonReader(FileReader(file))
                passwordList = Gson().fromJson<PasswordList>(reader, PasswordList::class.java)
                if(passwordList==null)
                    passwordList = PasswordList(HashMap<String,Password>())
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

    inner class PutPaswordListTask(var callback:()->Unit): AsyncTask<Void, Void, Boolean>()
    {
        override fun doInBackground(vararg p0: Void?): Boolean {
            try {
                val passwordFileString = Gson().toJson(passwordList)
                val file = File(Environment.getExternalStorageDirectory().toString()+Constants.Misc.LOCAL_FILENAME);
                file.createNewFile()
                val fOut = FileOutputStream(file)
                val outWriter = OutputStreamWriter(fOut)
                outWriter.append(passwordFileString)
                outWriter.close()
                fOut.close()

                if (PreferenceManager.getDefaultSharedPreferences(context).contains(Constants.SharedPrefs.ACCESS_TOKEN)) {
                    val response = dropboxClient?.files()?.uploadBuilder(Constants.Misc.DROPBOX_FILENAME)?.withMode(WriteMode.OVERWRITE)?.uploadAndFinish(FileInputStream(file))
                    log.debug("response is {}",response?.rev)
                }
                return true

            } catch (e:Exception) {
                log.warn("unable to save password file",e)
            }
            return false
        }

        override fun onPostExecute(result: Boolean) {
            if(result) {
                Toast.makeText(context, R.string.toast_save_success, Toast.LENGTH_LONG).show()
                callback()
            }
            else
                Toast.makeText(context,R.string.toast_save_error,Toast.LENGTH_LONG).show()
        }
    }

    fun addorUpdatePassword(password: Password,callback:()-> Unit)
    {
        initDropboxClient()
        fun addOrUpdatePasswordCallback()
        {
            passwordList!!.passwords.put(password.label,password)
            PutPaswordListTask(callback).execute()
        }
        loadPasswordList(::addOrUpdatePasswordCallback)
    }

    fun deletePassword(password: Password,callback: () -> Unit)
    {
        initDropboxClient()
        fun deletePasswordCallback()
        {
            if(passwordList!!.passwords.containsKey(password.label))
            {
                passwordList!!.passwords.remove(password.label)

                PutPaswordListTask(callback).execute()
            }
        }
        loadPasswordList(::deletePasswordCallback)
    }
}