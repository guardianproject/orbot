package org.torproject.android

import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class Bridges(val type: String, val source: String, val bridge_strings: List<String>)
data class Error(val code: Int, val detail: String)
data class Errors(val errors: List<Error>)

data class SettingsRequest(val country: String? = null, val transports: List<String>? = null)
data class SettingsResponse(val settings: List<Bridges>?)

interface CircumventionEndpoints {
    @POST("settings")
    fun getSettings(@Body data: SettingsRequest): Call<SettingsResponse>

    @GET("countries")
    fun getCountries(): Call<List<String>>


}

object ServiceBuilder {
    private val client = OkHttpClient.Builder().build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://bridges.torproject.org/moat/circumvention/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()

    fun<T> buildService(service: Class<T>): T = retrofit.create(service)
}

class CircumventionApiManager {
    private val retrofit = ServiceBuilder.buildService(CircumventionEndpoints::class.java)
    fun getSettings(request: SettingsRequest, onResult: (SettingsResponse?) -> Unit) {
        retrofit.getSettings(request).enqueue(
            object: Callback<SettingsResponse> {
                override fun onResponse(
                    call: Call<SettingsResponse>,
                    response: Response<SettingsResponse>
                ) {
                    onResult(response.body())
                }

                override fun onFailure(call: Call<SettingsResponse>, t: Throwable) {
                    onResult(null)
                }

            })
    }
    fun getCountries(onResult: (List<String>?) -> Unit) {
        retrofit.getCountries().enqueue(object: Callback<List<String>> {
            override fun onResponse(call: Call<List<String>>, response: Response<List<String>>) {
                onResult(response.body())
            }

            override fun onFailure(call: Call<List<String>>, t: Throwable) {
                onResult(null)
            }

        })
    }
}