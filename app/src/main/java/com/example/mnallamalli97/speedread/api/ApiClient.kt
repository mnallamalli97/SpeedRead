package com.example.mnallamalli97.speedread.api

import okhttp3.OkHttpClient
import okhttp3.OkHttpClient.Builder
import okhttp3.Request
import retrofit2.Retrofit
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.jvm.Throws

object ApiClient {
  const val BASE_URL = "https://news67.p.rapidapi.com/trending?limit=10&langs=en&skip=5"

  val apiClient: Request
    get() {
      val client = OkHttpClient()
      return Request.Builder()
          .url("https://news67.p.rapidapi.com/trending?limit=10&langs=en&skip=5")
          .get()
          .addHeader("x-rapidapi-key", "f46e5c8c90msh9778f06fc8c5a49p16d051jsn87608541b961")
          .addHeader("x-rapidapi-host", "news67.p.rapidapi.com")
          .build()
    }

  // Create a trust manager that does not validate certificate chains
  val unsafeOkHttpClient: Builder
    get() = try {
      // Create a trust manager that does not validate certificate chains
      val trustAllCerts =
        arrayOf<TrustManager>(
            object : X509TrustManager {
              @Throws(
                  CertificateException::class
              ) override fun checkClientTrusted(
                chain: Array<X509Certificate>,
                authType: String
              ) {
              }

              @Throws(
                  CertificateException::class
              ) override fun checkServerTrusted(
                chain: Array<X509Certificate>,
                authType: String
              ) {
              }

              override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
              }
            }
        )

      // Install the all-trusting trust manager
      val sslContext = SSLContext.getInstance("SSL")
      sslContext.init(null, trustAllCerts, SecureRandom())

      // Create an ssl socket factory with our all-trusting manager
      val sslSocketFactory = sslContext.socketFactory
      val builder = Builder()
      builder.sslSocketFactory(
          sslSocketFactory, (trustAllCerts[0] as X509TrustManager)
      )
      builder.hostnameVerifier(HostnameVerifier { hostname, session -> true })
      builder
    } catch (e: Exception) {
      throw RuntimeException(e)
    }
}