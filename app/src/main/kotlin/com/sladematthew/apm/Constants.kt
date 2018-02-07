package com.sladematthew.apm

class Constants{
    object SharedPrefs {
        const val ACCESS_TOKEN = "apm.accessToken"
        const val MASTER_PASSWORD_HASH = "apm.masterPasswordHash"
        const val LAST_ACTIVITY = "apm.lastActivity"
    }

    object IntentKey{
        const val PASSWORD = "password"
    }

    object Credentials{
        const val APP_KEY = "w72uu1b53wlewpr"
    }

    object Misc {
        const val DEFAULT_LENGTH = 10
        const val DEFAULT_PREFIX = "P_"
        const val LOCAL_FILENAME = "/passwords.json"
        const val DROPBOX_FILENAME = "/passwords.json"
    }
}

