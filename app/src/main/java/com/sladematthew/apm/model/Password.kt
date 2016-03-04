package com.sladematthew.apm.model

import com.sladematthew.apm.Constants
import java.io.Serializable

data class Password ( var label:String,var version:Int, var length:Int,var prefix:String,var username:String):Serializable
