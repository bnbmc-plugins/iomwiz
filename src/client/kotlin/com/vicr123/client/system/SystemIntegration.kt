package com.vicr123.client.system

import com.vicr123.client.system.linux.LinuxSystemIntegrationBackend
import java.net.URL

class SystemIntegration {
    companion object {
        private val backend = currentSystemBackend()

        private fun currentSystemBackend(): LinuxSystemIntegrationBackend? {
            if (System.getProperty("os.name").lowercase() == "linux") {
                return LinuxSystemIntegrationBackend()
            }
            return null
        }

        fun canOpenFilePicker(): Boolean {
            if (backend != null) return backend.canOpenFilePicker();
            return false;
        }

        suspend fun openFilePicker(): List<String> {
            if (backend != null) return backend.openFilePicker()
            return emptyList()
        }

        fun canOpenUrl(): Boolean {
            if (backend != null) return backend.canOpenUrl()
            return false;
        }

        fun openUrl(url: URL) {
            if (backend != null) return backend.openUrl(url)
        }
    }
}