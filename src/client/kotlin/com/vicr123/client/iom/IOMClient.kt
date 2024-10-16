package com.vicr123.client.iom

import com.google.gson.Gson
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.client.toast.SystemToast
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.io.File
import java.io.InputStream
import java.net.URL
import java.util.*

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

    fun giveMap(map: IOMMap) {
        http.get(URL("${serverRoot}maps/${map.id}/give"), Unit::class.java)
    }

    fun imageTexture(resource: String): DownloadedImage? {
        if (images.containsKey(resource)) {
            return images[resource]
        }

        images[resource] = null
        http.get(URL("${serverRoot}images/${resource}"), InputStream::class.java).thenAccept { stream ->
            try {
                val nativeImage = NativeImage.read(stream)
                val texture = NativeImageBackedTexture(nativeImage)
                val identifier = Identifier("iomwiz", "maps/$resource")
                texture.bindTexture()
                client.textureManager.registerTexture(identifier, texture)
                images[resource] = DownloadedImage(identifier, nativeImage.width, nativeImage.height)
            } catch (e: Exception) {
                // Give up on this image
                e.printStackTrace()
            }
        }

        return null
    }

    fun uploadMap(mapFile: File, category: String) {
        mapFile.inputStream().use { stream ->
            val gson = Gson()

            val payload = JsonObject()
            payload.addProperty("category", category)
            payload.addProperty("name", mapFile.nameWithoutExtension)
            payload.addProperty("image", Base64.getEncoder().encodeToString(stream.readAllBytes()))

            http.post(URL("${serverRoot}maps"), gson.toJson(payload), Unit::class.java).thenAccept({
                client.toastManager.add(SystemToast(SystemToast.Type.PERIODIC_NOTIFICATION, Text.translatable("iomwiz.uploadsuccess.title"), Text.translatable("iomwiz.uploadsuccess.message", mapFile.nameWithoutExtension)))
            }).exceptionally { e ->
                e.printStackTrace()

                if (e is IOMHttpException) {
                    println("IOM server responded with ${e.responseCode}")
                    println(e.bufferedReader.readText())
                }

                client.toastManager.add(SystemToast(SystemToast.Type.PERIODIC_NOTIFICATION, Text.translatable("iomwiz.uploadfail.title"), Text.translatable("iomwiz.uploadfail.message", mapFile.nameWithoutExtension)))
                null
            }
        }
    }
}

class DownloadedImage(public val identifier: Identifier, public val width: Int, public val height: Int)

public interface MapsChangedListener {
    fun onMapsChanged()
}

