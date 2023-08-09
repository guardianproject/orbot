package org.torproject.android.circumvention

import com.google.gson.annotations.SerializedName

data class BuiltInBridgesResponse(
    @SerializedName("meek-azure")
    val meekAzure: List<String>,
    val obfs4: List<String>,
    val snowflake: List<String>
)