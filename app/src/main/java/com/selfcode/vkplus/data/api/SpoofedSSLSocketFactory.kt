package com.selfcode.vkplus.data.api

import java.net.InetAddress
import java.net.Socket
import java.security.KeyStore
import javax.net.ssl.*

class SpoofedSSLSocketFactory(
    private val delegate: SSLSocketFactory,
    private val fakeSni: String
) : SSLSocketFactory() {

    override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites
    override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites

    override fun createSocket(): Socket = applySni(delegate.createSocket() as SSLSocket)
    override fun createSocket(host: String, port: Int): Socket = applySni(delegate.createSocket(host, port) as SSLSocket)
    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket =
        applySni(delegate.createSocket(host, port, localHost, localPort) as SSLSocket)
    override fun createSocket(host: InetAddress, port: Int): Socket =
        applySni(delegate.createSocket(host, port) as SSLSocket)
    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket =
        applySni(delegate.createSocket(address, port, localAddress, localPort) as SSLSocket)

    override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket =
        applySni(delegate.createSocket(s, host, port, autoClose) as SSLSocket)

    private fun applySni(socket: SSLSocket): SSLSocket {
        try {
            val params = socket.sslParameters
            params.serverNames = listOf(javax.net.ssl.SNIHostName(fakeSni))
            socket.sslParameters = params
        } catch (e: Exception) {
            // Fallback: if SNI setting fails, proceed without it
        }
        return socket
    }

    companion object {
        // Domains that look legitimate to DPI systems
        val SPOOF_TARGETS = listOf(
            "google.com",
            "cloudflare.com",
            "cdn.jsdelivr.net",
            "ajax.googleapis.com"
        )

        fun create(fakeSni: String = "cloudflare.com"): Pair<SpoofedSSLSocketFactory, X509TrustManager> {
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(null as KeyStore?)
            val trustManager = trustManagerFactory.trustManagers
                .filterIsInstance<X509TrustManager>().first()
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(trustManager), null)
            return Pair(SpoofedSSLSocketFactory(sslContext.socketFactory, fakeSni), trustManager)
        }
    }
}
