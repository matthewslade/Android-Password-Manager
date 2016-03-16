package com.sladematthew.apm

import android.content.Context
import android.os.AsyncTask
import android.os.Environment
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.dropbox.client2.DropboxAPI
import com.dropbox.client2.android.AndroidAuthSession
import com.dropbox.client2.exception.DropboxServerException
import com.dropbox.client2.session.AppKeyPair
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

    private var mDBApi: DropboxAPI<AndroidAuthSession>? = null

    var passwordList:PasswordList?=null

    private var masterPassword:String?=null

    private var log = Log.getLogger(AuthenticationManager::class.java)

    init
    {
        val appKeys = AppKeyPair(Constants.Credentials.APP_KEY, Constants.Credentials.APP_SECRET)
        val session = AndroidAuthSession(appKeys)
        if(PreferenceManager.getDefaultSharedPreferences(context).contains(Constants.SharedPrefs.ACCESS_TOKEN))
            session.oAuth2AccessToken = PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.SharedPrefs.ACCESS_TOKEN,null)
        mDBApi = DropboxAPI(session)
    }

    fun authWithDropbox(activity: StartActivity)
    {
        mDBApi?.session?.startOAuth2Authentication(activity)
    }

    fun authCompleted():Boolean
    {
        if (mDBApi?.session?.authenticationSuccessful()?:false) {
            try {
                // Required to complete auth, sets the access token on the session
                mDBApi?.session?.finishAuthentication()
                val accessToken = mDBApi?.session?.oAuth2AccessToken
                PreferenceManager.getDefaultSharedPreferences(context).edit().putString(Constants.SharedPrefs.ACCESS_TOKEN, accessToken).apply()
                return true

            } catch (e: IllegalStateException) {
                log.warn("unable to complete dropbox auth",e)
            }

        }
        return false
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
            md5 = md5 + "0"
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
        class GetPaswordListTask: AsyncTask<Void, Void, Void>()
        {
            override fun doInBackground(vararg p0: Void?): Void?
            {
                var file = File(Environment.getExternalStorageDirectory().toString()+Constants.Misc.LOCAL_FILENAME)

                var outputStream = FileOutputStream(file);

                if (PreferenceManager.getDefaultSharedPreferences(context).contains(Constants.SharedPrefs.ACCESS_TOKEN)) {
                    try {
                        var info = mDBApi?.getFile(Constants.Misc.DROPBOX_FILENAME, null, outputStream, null);
                    }
                    catch(e:DropboxServerException)
                    {
                        log.debug("dropbox server exception",e)
                    }
                }

                var reader = JsonReader(FileReader(file));
                passwordList = Gson().fromJson<PasswordList>(reader, PasswordList::class.java);
                if(passwordList==null)
                    passwordList = PasswordList(HashMap<String,Password>());
                return null;
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
                var passwordFileString = Gson().toJson(passwordList)
                var file = File(Environment.getExternalStorageDirectory().toString()+Constants.Misc.LOCAL_FILENAME);
                file.createNewFile()
                var fOut = FileOutputStream(file);
                var outWriter = OutputStreamWriter(fOut);
                outWriter.append(passwordFileString);
                outWriter.close();
                fOut.close();

                if (PreferenceManager.getDefaultSharedPreferences(context).contains(Constants.SharedPrefs.ACCESS_TOKEN)) {
                    var inputStream = FileInputStream(file);
                    var response = mDBApi?.putFileOverwrite(Constants.Misc.DROPBOX_FILENAME, inputStream, file.length(), null);
                    log.debug("response is {}",response?.rev)
                }
                return true

            } catch (e:Exception) {
                log.warn("unable to save password file",e)
            }
            return false;
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
        fun addOrUpdatePasswordCallback()
        {
            passwordList!!.passwords.put(password.label,password)
            PutPaswordListTask(callback).execute()
        }
        loadPasswordList(::addOrUpdatePasswordCallback)
    }

    fun deletePassword(password: Password,callback: () -> Unit)
    {
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