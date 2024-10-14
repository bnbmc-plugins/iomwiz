package com.vicr123.client.system.linux

import com.vicr123.client.system.SystemIntegrationBackend
import net.minecraft.text.Text
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.interfaces.DBusSigHandler
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.Variant
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val PORTAL_SERVICE = "org.freedesktop.portal.Desktop"

class LinuxSystemIntegrationBackend : SystemIntegrationBackend {
    lateinit var sessionBus: DBusConnection

    override fun canOpenFilePicker(): Boolean {
        return true;
    }

    override suspend fun openFilePicker(): List<String> {
        return suspendCoroutine { continuation ->
            if (!this::sessionBus.isInitialized) {
                sessionBus = DBusConnectionBuilder.forSessionBus().build()
            }
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
        }
    }
}

@DBusInterfaceName("org.freedesktop.portal.FileChooser")
public interface DBusFileChooserInterface : DBusInterface {
    fun OpenFile(parentWindow: String, title: String, options: MutableMap<String, Variant<*>>): DBusPath;
}

@DBusInterfaceName("org.freedesktop.portal.Request")
public interface DBusRequestInterface : DBusInterface {
    fun Close()

    class Response(path: String, val response: UInt32, val results: MutableMap<String, Variant<*>>) : DBusSignal(path, response, results)
}