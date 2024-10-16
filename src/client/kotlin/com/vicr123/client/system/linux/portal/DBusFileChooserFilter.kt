package com.vicr123.client.system.linux.portal

import org.freedesktop.dbus.Struct
import org.freedesktop.dbus.annotations.Position
import org.freedesktop.dbus.types.UInt32

public class DBusFileChooserFilter(
    @Position(0)
    val userVisibleName: String,

    @Position(1)
    val items: MutableList<DBusFileChooserFilterItem>) : Struct() {

}

public class DBusFileChooserFilterItem(
    @Position(0)
    val type: UInt32,

    @Position(1)
    val pattern: String) : Struct() {

}