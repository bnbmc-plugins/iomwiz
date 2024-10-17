package com.vicr123.client.iom

import com.google.gson.Gson
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CompletableFuture

class IOMHttp {
    var authToken: String? = null
    private var gson = Gson();

    fun <T> get(url: URL, clazz: Class<T>): CompletableFuture<T> {
        return http("GET", url, null, clazz);
    }

    fun <T> post(url: URL, payload: String, clazz: Class<T>): CompletableFuture<T> {
        return http("POST", url, payload, clazz);
    }

    fun <T> put(url: URL, payload: String, clazz: Class<T>): CompletableFuture<T> {
        return http("PUT", url, payload, clazz);
    }

    private fun <T> http(verb: String, url: URL, payload: String?, clazz: Class<T>): CompletableFuture<T> {
        return CompletableFuture.supplyAsync {
            return@supplyAsync (url.openConnection() as HttpURLConnection).run {
                requestMethod = verb
                setRequestProperty("Authorization", "Bearer $authToken")

                if (payload != null) {
                    val payloadBytes = payload.toByteArray()
                    setRequestProperty("Content-Type", "application/json")
                    setFixedLengthStreamingMode(payloadBytes.size)
                    doOutput = true
                    outputStream.write(payloadBytes)
                }

                if (responseCode in 200..299) {
                    if (clazz == InputStream::class.java) return@run inputStream as T
                    if (clazz == Unit::class.java) return@run Unit as T
                    return@run gson.fromJson(inputStream.bufferedReader().readText(), clazz)
                } else {
                    throw IOMHttpException(responseCode, inputStream.bufferedReader());
                }
            }
        }
    }
}

