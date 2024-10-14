package com.vicr123.client.system

interface SystemIntegrationBackend {
    fun canOpenFilePicker(): Boolean
    suspend fun openFilePicker(): List<String>
}