package com.vicr123.client.screens

import com.vicr123.client.iom.IOMClient
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

@Environment(EnvType.CLIENT)
class IOMScreen(private val iomClient: IOMClient) : Screen(Text.literal("IOM")) {
    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        context?.drawCenteredTextWithShadow(textRenderer, "IOM stuff goes here", width / 2, height / 2, 0xffffff)
    }
}