package com.vicr123.client.screens

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.MutableText
import net.minecraft.text.Text


@Environment(EnvType.CLIENT)
class IOMWaitScreen(private val text: MutableText) : Screen(Text.literal("IOM")) {
    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        context?.drawCenteredTextWithShadow(textRenderer, text, width / 2, height / 2, 0xffffff)
    }
}