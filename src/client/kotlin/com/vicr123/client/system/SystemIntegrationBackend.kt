package com.vicr123.client.system

import java.net.URL

interface SystemIntegrationBackend {
    fun canOpenFilePicker(): Boolean
    suspend fun openFilePicker(): List<String>
    fun canOpenUrl(): Boolean
    fun openUrl(url: URL)
}