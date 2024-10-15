package com.vicr123.client.iom

import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Identifier
import java.io.InputStream
import java.net.URL

class IOMClient(private val client: MinecraftClient) {
    var serverRoot: String? = null
    var authToken: String?
        get() = http.authToken
        set(value) {
            http.authToken = value
        }

    var ready: Boolean? = null
    var maps: Array<IOMMap>? = null
        set(value) {
            field = value
            for (listener in mapsChangedListeners) {
                listener.onMapsChanged()
            }
        }
    private var images: MutableMap<String, DownloadedImage?> = mutableMapOf("x" to null) //x means no image is saved so never return an image here

    val http = IOMHttp()

    var mapsChangedListeners = mutableListOf<MapsChangedListener>()

    fun updateMaps() {
        http.get(URL("${serverRoot}maps"), Array<IOMMap>::class.java).thenAccept { maps -> this.maps = maps}
    }

    fun imageTexture(resource: String): DownloadedImage? {
        if (images.containsKey(resource)) {
            return images[resource]
        }

        images[resource] = null
        http.get(URL("${serverRoot}images/${resource}"), InputStream::class.java).thenAccept { stream ->
            val nativeImage = NativeImage.read(stream)
            val texture = NativeImageBackedTexture(nativeImage)
            val identifier = Identifier("iomwiz", "maps/$resource")
            texture.bindTexture()
            client.textureManager.registerTexture(identifier, texture)
            images[resource] = DownloadedImage(identifier, nativeImage.width, nativeImage.height)
        }

        return null
    }
}

class DownloadedImage(public val identifier: Identifier, public val width: Int, public val height: Int)

public interface MapsChangedListener {
    fun onMapsChanged()
}

