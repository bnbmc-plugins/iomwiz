package com.vicr123.client.iom

import java.net.URL

class IOMClient {
    var serverRoot: String? = null
    var authToken: String?
        get() = http.authToken
        set(value) {
            http.authToken = value
        }

    var ready: Boolean? = null
    var maps: Array<Map>? = null
        set(value) {
            field = value
            for (listener in mapsChangedListeners) {
                listener.onMapsChanged()
            }
        }

    val http = IOMHttp()

    var mapsChangedListeners = mutableListOf<MapsChangedListener>()

    fun updateMaps() {
        http.get(URL("${serverRoot}maps"), Array<Map>::class.java).thenAccept { maps -> this.maps = maps}
    }
}

public interface MapsChangedListener {
    fun onMapsChanged()
}

class Map {
    lateinit var name: String
    lateinit var pictureResource: String
    var id: Int = 0
    lateinit var category: String
    var isOwner: Boolean = false
}

