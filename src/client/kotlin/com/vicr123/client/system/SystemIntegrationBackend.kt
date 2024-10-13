package com.vicr123.client.system

import java.util.concurrent.CompletableFuture

interface SystemIntegrationBackend {
    fun canOpenFilePicker(): Boolean
    fun openFilePicker(): CompletableFuture<Array<String>>
}