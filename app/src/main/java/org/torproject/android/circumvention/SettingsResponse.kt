package org.torproject.android.circumvention

data class SettingsResponse(val settings: List<Bridges>?, val errors: List<Error>?)
data class Bridges(val bridges: Bridge)
data class Bridge(val type: String, val source: String, val bridge_strings: List<String>? = null)
data class Error(val code: Int, val detail: String)
