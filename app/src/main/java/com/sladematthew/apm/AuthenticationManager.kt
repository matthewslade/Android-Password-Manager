package com.sladematthew.apm

import android.content.Context
import android.os.AsyncTask
import android.os.Environment
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.view.View
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

    private var passwordList:PasswordList?=null

    private var log = Log.getLogger(AuthenticationManager::class.java)

    init
    {
        val appKeys = AppKeyPair(Constants.Credentials.APP_KEY, Constants.Credentials.APP_SECRET)
        val session = AndroidAuthSession(appKeys)
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
        var md5 = BigInteger(1, mdEnc.digest()).toString(16)
        while (md5.length < 32) {
            md5 = "0" + md5
        }
        return md5
    }

    fun setMasterPassword(password: String){
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(Constants.SharedPrefs.MASTER_PASSWORD_HASH,getMD5EncryptedString(password)).apply()
    }

    fun checkMasterPassword(password: String):Boolean
    {
        var md5Password = getMD5EncryptedString(password)
        if(PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.SharedPrefs.MASTER_PASSWORD_HASH,"").equals(md5Password))
            return true
        return false
    }

    fun generatePassword(password: Password):String
    {
        if(PreferenceManager.getDefaultSharedPreferences(context).contains(Constants.SharedPrefs.MASTER_PASSWORD_HASH))
            return getMD5EncryptedString(password.label+password.version+PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.SharedPrefs.MASTER_PASSWORD_HASH,""))
        return ""
    }

    fun getPasswordList(callback: (PasswordList)-> Unit)
    {
        class GetPaswordListTask: AsyncTask<Void, Void, Void>()
        {
            override fun doInBackground(vararg p0: Void?): Void?
            {
                var file = File(Environment.getExternalStorageDirectory().toString()+Constants.Names.LOCAL_FILENAME)

                var outputStream = FileOutputStream(file);

                if (PreferenceManager.getDefaultSharedPreferences(context).contains(Constants.SharedPrefs.ACCESS_TOKEN)) {
                    try {
                        var info = mDBApi?.getFile(Constants.Names.DROPBOX_FILENAME, null, outputStream, null);
                    }
                    catch(e:DropboxServerException)
                    {
                        log.debug("dropbox server exception",e)
                    }
                }

                var reader = JsonReader(FileReader(file));
                passwordList = Gson().fromJson<PasswordList>(reader, PasswordList::class.java);
                if(passwordList==null)
                    passwordList = PasswordList(ArrayList<Password>());
                return null;
            }

            override fun onPostExecute(result: Void?) {
                callback(passwordList!!)
            }
        }

        if(passwordList==null)
            GetPaswordListTask().execute()
        else
            callback(passwordList!!)

    }

    fun addorUpdatePassword(password: Password)
    {

        class PutPaswordListTask: AsyncTask<String, Void, Void>()
        {
            override fun doInBackground(vararg p0: String?): Void? {
                try {
                    var outPutString:String = p0[0]!!;
                    var file = File(Environment.getExternalStorageDirectory().toString()+Constants.Names.LOCAL_FILENAME);
                    file.createNewFile()
                    var fOut = FileOutputStream(file);
                    var outWriter = OutputStreamWriter(fOut);
                    outWriter.append(outPutString);
                    outWriter.close();
                    fOut.close();

                    if (PreferenceManager.getDefaultSharedPreferences(context).contains(Constants.SharedPrefs.ACCESS_TOKEN)) {
                        var inputStream = FileInputStream(file);
                        var response = mDBApi?.putFileOverwrite(Constants.Names.DROPBOX_FILENAME, inputStream, file.length(), null);
                        log.debug("response is {}",response?.rev)
                    }

                } catch (e:Exception) {
                    log.warn("unable to save password file",e)
                }
                return null;
            }
        }

        fun addOrUpdatePasswordCallback(pwList:PasswordList)
        {
            var exists:Boolean = false;
            for(p in pwList.passwords)
            {
                if (password.label.equals(p.label))
                {
                    p.version = password.version;
                    exists=true
                }
            }

            if(!exists)
            {
                pwList.passwords.add(password)
            }
            var passwordFileString = Gson().toJson(passwordList)
            PutPaswordListTask().execute(passwordFileString)
        }

        getPasswordList(::addOrUpdatePasswordCallback)
    }
}