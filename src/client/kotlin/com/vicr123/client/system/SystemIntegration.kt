package com.vicr123.client.system

import com.vicr123.client.system.linux.LinuxSystemIntegrationBackend
import io.netty.util.concurrent.Promise
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class SystemIntegration {
    companion object {
        private val backend = currentSystemBackend()

        private fun currentSystemBackend(): LinuxSystemIntegrationBackend? {
            if (System.getProperty("os.name").lowercase() == "linux") {
                return LinuxSystemIntegrationBackend();
            }
            return null
        }

        fun canOpenFilePicker(): Boolean {
            if (backend != null) return backend.canOpenFilePicker();
            return false;
        }

        fun openFilePicker(): CompletableFuture<Array<String>> {
            if (backend != null) return backend.openFilePicker();
            return CompletableFuture.completedFuture(arrayOf())
        }
    }
}