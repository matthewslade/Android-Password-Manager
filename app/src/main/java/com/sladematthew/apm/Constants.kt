package com.sladematthew.apm

class Constants{
    object SharedPrefs {
        val ACCESS_TOKEN = "apm.accessToken"
        val MASTER_PASSWORD_HASH = "apm.masterPasswordHash"
        val LAST_ACTIVITY = "apm.lastActivity"

    }

    object IntentKey{
        val PASSWORD = "password"
    }

    object Credentials{
        val APP_KEY = "w72uu1b53wlewpr"
        val APP_SECRET = "a0ccibz0ijb9q0p"
    }

    object Misc {
        val DEFAULT_LENGTH = 10
        val DEFAULT_PREFIX = "P_"
        val LOCAL_FILENAME = "/passwords.json"
        val DROPBOX_FILENAME = "/passwords.json"
    }
}

