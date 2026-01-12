package com.sladematthew.apm.model

import java.io.Serializable

data class Password(
        var algorithm: String? = "SHA256", // Nullable for backward compatibility
        var label: String? = null,
        var version: Int? = null,
        var length: Int? = null,
        var prefix: String? = null,
        var username: String? = ""
):Serializable
