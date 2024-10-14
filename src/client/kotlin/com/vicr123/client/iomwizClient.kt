package com.vicr123.client

import com.vicr123.client.iom.IOMClient
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

class iomwizClient : ClientModInitializer {
    private lateinit var mapButton: KeyBinding
    private var mapButtonPressed = false;

    private var iom: IOMClient? = null;

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
            handleInit(minecraftClient)
        }
        ClientPlayConnectionEvents.JOIN.register { clientPlayNetworkHandler, packetSender, minecraftClient ->
            handleJoin()
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

        if (iom!!.ready == null) {
            // IOM is not available
            client.setScreen(IOMWaitScreen(Text.translatable("iomwiz.wait.unavailable")))
            return
        } else if (iom!!.ready == false) {
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

        client.setScreen(IOMScreen(iom!!))
    }

    fun handleInit(client: MinecraftClient) {
        iom = IOMClient();
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
                iom!!.ready = true
                client.toastManager.add(SystemToast(SystemToast.Type.PERIODIC_NOTIFICATION, Text.translatable("iomwiz.ready.title"), Text.translatable("iomwiz.ready.message", "INTERNET EXPLORER")))
            } else if (message == "false") {
                iom!!.ready = false
            }
            println("Ready: ${iom!!.ready}")
            showIomScreenIfWaiting(client)
        }
    }

    fun updateServerRoot() {
        if (iom!!.serverRoot == null) {
            sendMessage("root")
        }
    }

    fun updateReady() {
        if (iom!!.ready != true) {
            sendMessage("ready")
        }
    }

    fun updateToken() {
        sendMessage("token")
    }
}
