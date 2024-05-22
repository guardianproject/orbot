/* Copyright (c) 2020, Benjamin Erhart, Orbot / The Guardian Project - https://guardianproject.info */
/* See LICENSE for licensing information */
package org.torproject.android.ui.onboarding

import android.util.Log

import com.android.volley.toolbox.HurlStack

import java.io.IOException
import java.net.*

class ProxiedHurlStack(
    private val mHost: String,
    private val mPort: Int,
    private val mUsername: String,
    private val mPassword: String
) : HurlStack() {

    @Throws(IOException::class)
    override fun createConnection(url: URL): HttpURLConnection {
        val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved(mHost, mPort))

        val authenticator = object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                Log.d(this::class.java.simpleName, "getPasswordAuthentication!")
                return PasswordAuthentication(mUsername, mPassword.toCharArray())
            }
        }

        Authenticator.setDefault(authenticator)

        return url.openConnection(proxy) as HttpURLConnection
    }
}
