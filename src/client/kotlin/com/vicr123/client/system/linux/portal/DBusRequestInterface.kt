package com.vicr123.client.system.linux.portal

import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.Variant

@DBusInterfaceName("org.freedesktop.portal.Request")
public interface DBusRequestInterface : DBusInterface {
    fun Close()

    class Response(path: String, val response: UInt32, val results: MutableMap<String, Variant<*>>) : DBusSignal(path, response, results)
}