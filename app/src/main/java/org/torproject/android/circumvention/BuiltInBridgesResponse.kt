package org.torproject.android.circumvention

import com.google.gson.annotations.SerializedName

data class BuiltInBridgesResponse(
    @SerializedName("meek-azure")
    val meek_azure: List<String>,
    val obfs4: List<String>,
    val snowflake: List<String>
)