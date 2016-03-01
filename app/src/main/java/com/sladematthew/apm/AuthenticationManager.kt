package com.sladematthew.apm

import android.content.Context
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.dropbox.client2.DropboxAPI
import com.dropbox.client2.android.AndroidAuthSession
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

class AuthenticationManager(var context: android.content.Context) {

    private var mDBApi: DropboxAPI<AndroidAuthSession>? = null

    private var passwordList:PasswordList?=null

    init
    {
        val appKeys = AppKeyPair(Constants.APP_KEY, Constants.APP_SECRET)
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
                PreferenceManager.getDefaultSharedPreferences(context).edit().putString(Constants.ACCESS_TOKEN, accessToken).apply()
                return true

            } catch (e: IllegalStateException) {

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
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(Constants.MASTER_PASSWORD_HASH,password).apply()
    }

    fun checkMasterPassword(password: String):Boolean
    {
        var md5Password = getMD5EncryptedString(password)
        if(PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.MASTER_PASSWORD_HASH,"").equals(md5Password))
            return true
        return false
    }

    fun generatePassword(password: Password):String
    {
        if(PreferenceManager.getDefaultSharedPreferences(context).contains(Constants.MASTER_PASSWORD_HASH))
            return getMD5EncryptedString(password.label+password.version+PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.MASTER_PASSWORD_HASH,""))
        return ""
    }

    fun getPasswordList(): PasswordList
    {
        if(passwordList==null) {
            var file = File(Constants.FILENAME)

            var outputStream = FileOutputStream(file);

            if (PreferenceManager.getDefaultSharedPreferences(context).contains(Constants.ACCESS_TOKEN)) {
                var info = mDBApi?.getFile(Constants.FILENAME, null, outputStream, null);
            }

            var reader = JsonReader(FileReader(file));
            passwordList = Gson().fromJson<PasswordList>(reader, PasswordList::class.java);
        }
        return passwordList!!

    }

    fun addorUpdatePassword(password: Password)
    {

        for(p in getPasswordList().passwords)
        {
            if (password.label.equals(p.label)
            {
                p.version = password.version
            }
        }


        var passwordFileString = Gson().toJson(passwordList)

        try {
            var file = File(Constants.FILENAME);
            file.createNewFile();
            var fOut = FileOutputStream(file);
            var myOutWriter = OutputStreamWriter(fOut);
            myOutWriter.append(passwordFileString);
            myOutWriter.close();
            fOut.close();

            if (PreferenceManager.getDefaultSharedPreferences(context).contains(Constants.ACCESS_TOKEN)) {
                var inputStream = FileInputStream(file);
                var response = mDBApi?.putFile("/magnum-opus.txt", inputStream, file.length(), null, null);
            }

        } catch (e:Exception) {

        }
    }
}