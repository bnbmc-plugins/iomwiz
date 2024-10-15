package com.vicr123.client.system.linux

import com.vicr123.client.system.SystemIntegrationBackend
import com.vicr123.client.system.linux.portal.DBusFileChooserInterface
import com.vicr123.client.system.linux.portal.DBusOpenURIInterface
import com.vicr123.client.system.linux.portal.DBusRequestInterface
import kotlinx.coroutines.suspendCancellableCoroutine
import net.minecraft.text.Text
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusSigHandler
import org.freedesktop.dbus.types.Variant
import java.net.URL
import kotlin.coroutines.resume

private const val PORTAL_SERVICE = "org.freedesktop.portal.Desktop"

class LinuxSystemIntegrationBackend : SystemIntegrationBackend {
    lateinit var sessionBus: DBusConnection

    private fun initialseSessionBus() {
        if (!this::sessionBus.isInitialized) {
            sessionBus = DBusConnectionBuilder.forSessionBus().build()
        }
    }

    override fun canOpenFilePicker(): Boolean {
        return true;
    }

    override suspend fun openFilePicker(): List<String> {
        return suspendCancellableCoroutine { continuation ->
            initialseSessionBus()
            val fileChooser = sessionBus.getRemoteObject(PORTAL_SERVICE, "/org/freedesktop/portal/desktop", DBusFileChooserInterface::class.java)

            val handle = fileChooser.OpenFile("", Text.translatable("iomwiz.filepicker.title").string, mutableMapOf(
                "handle_token" to Variant("iomwiz"),
                "multiple" to Variant(true),
                "accept_label" to Variant(Text.translatable("iomwiz.filepicker.accept").string)
            ))
            val request = sessionBus.getRemoteObject(PORTAL_SERVICE, handle.path, DBusRequestInterface::class.java)
            sessionBus.addSigHandler(DBusRequestInterface.Response::class.java, request, object : DBusSigHandler<DBusRequestInterface.Response> {
                override fun handle(signal: DBusRequestInterface.Response) {
                    sessionBus.removeSigHandler(DBusRequestInterface.Response::class.java, request, this)

                    if (signal.response.toLong() != 0L) {
                        // The interaction was ended abnormally
                        continuation.resume(emptyList())
                        return
                    }

                    val uris = signal.results["uris"]?.value as List<String>
                    continuation.resume(uris);
                }
            })
            continuation.invokeOnCancellation {
                request.Close()
            }
        }
    }

    override fun canOpenUrl(): Boolean {
        return true;
    }

    override fun openUrl(url: URL) {
        initialseSessionBus()
        val openUrl = sessionBus.getRemoteObject(PORTAL_SERVICE, "/org/freedesktop/portal/desktop", DBusOpenURIInterface::class.java)
        openUrl.OpenURI(
            "",
            url.toString(),
            mutableMapOf(
                "handle_token" to Variant("iomwiz"),
                "ask" to Variant(true)
            )
        )
    }
}
