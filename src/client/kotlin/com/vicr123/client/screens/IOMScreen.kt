package com.vicr123.client.screens

import com.vicr123.client.iom.IOMClient
import com.vicr123.client.system.SystemIntegration
import kotlinx.coroutines.runBlocking
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text
import java.net.URL
import java.util.concurrent.CompletableFuture

@Environment(EnvType.CLIENT)
class IOMScreen(private val iomClient: IOMClient) : Screen(Text.literal("IOM")) {
    private val chooseFileButton = ButtonWidget.builder(Text.literal("Choose a file"), ::chooseFile).build()
    private val openInBrowserButton = ButtonWidget.builder(Text.translatable("iomwiz.browser.open"), ::openInBrowser).build()

    override fun init() {
        super.init()

        chooseFileButton.setPosition(width / 2, height - 10)
        chooseFileButton.active = SystemIntegration.canOpenFilePicker()
        addDrawableChild(chooseFileButton)

        openInBrowserButton.setPosition(10, height - 40)
        openInBrowserButton.active = SystemIntegration.canOpenUrl()
        addDrawableChild(openInBrowserButton)
    }

    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        context?.drawCenteredTextWithShadow(textRenderer, "IOM stuff goes here", width / 2, height / 2, 0xffffff)
    }

    fun chooseFile(button: ButtonWidget) {
        CompletableFuture.supplyAsync {
            runBlocking {
                return@runBlocking SystemIntegration.openFilePicker()
            }
        }.thenAccept { urls ->
            println(urls)
        }
    }

    fun openInBrowser(button: ButtonWidget) {
        SystemIntegration.openUrl(URL("${iomClient.serverRoot}?auth=${iomClient.authToken}"))
    }
}