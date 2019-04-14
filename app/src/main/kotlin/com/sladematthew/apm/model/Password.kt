package com.sladematthew.apm.model

import java.io.Serializable

data class Password(
        var algorithm: String,
        var label:String,
        var version:Int,
        var length:Int,
        var prefix:String,
        var username:String
):Serializable
