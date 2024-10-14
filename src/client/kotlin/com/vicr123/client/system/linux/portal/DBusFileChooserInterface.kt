package com.vicr123.client.system.linux.portal

import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.types.Variant

@DBusInterfaceName("org.freedesktop.portal.FileChooser")
public interface DBusFileChooserInterface : DBusInterface {
    fun OpenFile(parentWindow: String, title: String, options: MutableMap<String, Variant<*>>): DBusPath;
}

