package com.sladematthew.apm

class Constants{
    object SharedPrefs {
        const val ACCESS_TOKEN = "apm.accessToken"
        const val MASTER_PASSWORD_HASH = "apm.masterPasswordHash"
    }

    object IntentKey{
        const val PASSWORD = "password"
    }

    object Misc {
        const val DEFAULT_LENGTH = 10
        const val DEFAULT_PREFIX = "P."
        const val LOCAL_FILENAME = "passwords.json"
        const val ALGORITHM = "SHA256"
    }
}

