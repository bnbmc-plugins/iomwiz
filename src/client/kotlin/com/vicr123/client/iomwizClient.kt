package com.vicr123.client

import com.vicr123.client.iom.IOMClient
import com.vicr123.client.iom.IOMReadyState
import com.vicr123.client.iom.MapsChangedListener
import com.vicr123.client.screens.IOMScreen
import com.vicr123.client.screens.IOMWaitScreen
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.toast.SystemToast
import net.minecraft.client.util.InputUtil
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.lwjgl.glfw.GLFW

const val TICKS_PER_HOUR: Int = 20 * 60 * 60

class iomwizClient : ClientModInitializer {
    private lateinit var mapButton: KeyBinding
    private var mapButtonPressed = false;

    private var iom: IOMClient? = null;
    private var tickCounter = 0;
    private var isConnected = false;

    override fun onInitializeClient() {
        mapButton = KeyBindingHelper.registerKeyBinding(KeyBinding("iomwiz.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_PAUSE, "iomwiz.key.category"))

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (mapButton.isPressed && !mapButtonPressed) {
                mapButtonPressed = true

                updateServerRoot()
                updateReady()

                showIomScreen(client)
            } else if (!mapButton.isPressed && mapButtonPressed) {
                mapButtonPressed = false
            }
        }
        ClientPlayConnectionEvents.INIT.register { clientPlayNetworkHandler, minecraftClient ->
            tickCounter = 0
            handleInit(minecraftClient)
            isConnected = true;
        }
        ClientPlayConnectionEvents.JOIN.register { clientPlayNetworkHandler, packetSender, minecraftClient ->
            handleJoin()
        }
        ClientPlayConnectionEvents.DISCONNECT.register { clientPlayNetworkHandler, minecraftClient ->
            isConnected = false
        }
        ClientTickEvents.END_CLIENT_TICK.register { minecraftClient ->
            if (!isConnected) return@register

            tickCounter++
            // Update the token every hour
            if (tickCounter >= TICKS_PER_HOUR) {
                tickCounter = 0
                updateToken()
            }
        }

    }

    fun showIomScreenIfWaiting(client: MinecraftClient) {
        if (client.currentScreen is IOMWaitScreen) {
            showIomScreen(client)
        }
    }

    fun showIomScreen(client: MinecraftClient) {
        if (iom == null) {
            return;
        }

        if (iom!!.ready == IOMReadyState.Unavailable) {
            // IOM is not available
            client.setScreen(IOMWaitScreen(Text.translatable("iomwiz.wait.unavailable")))
            return
        } else if (iom!!.ready == IOMReadyState.NotReady) {
            client.setScreen(IOMWaitScreen(Text.translatable("iomwiz.wait.notready")))
            return
        }

        if (iom!!.authToken == null) {
            client.setScreen(IOMWaitScreen(Text.translatable("iomwiz.wait.server")))
            updateToken()
            return
        }

        if (iom!!.maps == null) {
            iom!!.updateMaps();
            client.setScreen(IOMWaitScreen(Text.translatable("iomwiz.wait.load")))
        }

        client.setScreen(IOMScreen(client, iom!!))
    }

    private fun handleInit(client: MinecraftClient) {
        iom = IOMClient(client)
        iom!!.mapsChangedListeners.add(object : MapsChangedListener {
            override fun onMapsChanged() {
                showIomScreenIfWaiting(client)
            }
        })

        ClientPlayNetworking.registerReceiver(Identifier("iomfrontend:wiz")) { minecraftClient, clientPlayNetworkHandler, packetByteBuf, packetSender ->
            minecraftClient.execute {
                receiveMessage(minecraftClient, packetByteBuf)
            }
        }
    }

    fun handleJoin() {
        updateServerRoot()
        updateToken()
        updateReady()
    }

    fun sendMessage(message: String) {
        val packetBuf = PacketByteBufs.create()
        packetBuf.writeString(message)
        ClientPlayNetworking.send(Identifier("iomfrontend:wiz"), packetBuf)
    }

    fun receiveMessage(client: MinecraftClient, packetByteBuf: PacketByteBuf) {
        val payload = packetByteBuf.readString()
        println(payload)

        val type = payload.substring(0, payload.indexOf(','))
        val message = payload.substring(payload.indexOf(',') + 1)

        if (type == "root") {
            iom!!.serverRoot = message
            println("Server Root: ${iom!!.serverRoot}")
        } else if (type == "token") {
            iom!!.authToken = message
            println("Auth Token: ${iom!!.authToken}")
            showIomScreenIfWaiting(client)
        } else if (type == "ready") {
            if (message == "true") {
                // Only show the toast if we weren't ready before
                if (iom!!.ready == IOMReadyState.NotReady) {
                    client.toastManager.add(SystemToast(SystemToast.Type.PERIODIC_NOTIFICATION, Text.translatable("iomwiz.ready.title"), Text.translatable("iomwiz.ready.message", mapButton.boundKeyLocalizedText)))
                }
                iom!!.ready = IOMReadyState.Ready
            } else if (message == "false") {
                iom!!.ready = IOMReadyState.NotReady
            }
            println("Ready: ${iom!!.ready}")
            showIomScreenIfWaiting(client)
        } else if (type == "mapsUpdated") {
            iom?.updateMaps()
        }
    }

    fun updateServerRoot() {
        if (iom!!.serverRoot == null) {
            sendMessage("root")
        }
    }

    fun updateReady() {
        if (iom!!.ready != IOMReadyState.Ready) {
            sendMessage("ready")
        }
    }

    fun updateToken() {
        sendMessage("token")
    }
}
