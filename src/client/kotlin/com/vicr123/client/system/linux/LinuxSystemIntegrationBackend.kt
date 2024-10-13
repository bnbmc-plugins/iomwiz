package com.vicr123.client.system.linux

import com.vicr123.client.system.SystemIntegrationBackend
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.types.Variant
import java.util.concurrent.CompletableFuture

class LinuxSystemIntegrationBackend : SystemIntegrationBackend {
    override fun canOpenFilePicker(): Boolean {
        return true;
    }

    override fun openFilePicker(): CompletableFuture<Array<String>> {
        return CompletableFuture.supplyAsync {
            val sessionBus = DBusConnectionBuilder.forSessionBus().build()
            var fileChooser = sessionBus.getRemoteObject("org.freedesktop.Portal.Desktop", "/org/freedesktop/Portal/Desktop", DBusFileChooserInterface::class.java)

            var options = HashMap<String, Variant<Any>>()
            options.put("handle_build", Variant<Any>("iomwiz"))

            var handle = fileChooser.OpenFile("", "Title Goes Here", options);
            arrayOf()
        }
    }
}

@DBusInterfaceName("org.freedesktop.portal.FileChooser")
public interface DBusFileChooserInterface : DBusInterface {
    fun OpenFile(parentWindow: String, title: String, options: HashMap<String, Variant<Any>>): String;
}