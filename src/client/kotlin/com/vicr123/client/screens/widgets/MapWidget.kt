package com.vicr123.client.screens.widgets

import com.vicr123.client.iom.IOMClient
import com.vicr123.client.iom.IOMMap
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.text.Text


class MapWidget(x: Int, y: Int, width: Int, height: Int, private val map: IOMMap, private val client: IOMClient) :
    PressableWidget(x, y, width, height, Text.literal("")) {

    override fun renderWidget(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderWidget(context, mouseX, mouseY, delta)

        context!!.fill(
            x + 3,
            y + 3, x + this.width - 3, y + this.height - 15, 0x32000000
        )

        val downloadedImage = client.imageTexture(map.pictureResource);
        if (downloadedImage != null) {
            val maxWidth = this.width - 6
            val maxHeight = this.height - 18

            var newWidth = downloadedImage.width
            var newHeight = downloadedImage.height
            if (newWidth > maxWidth) {
                // Resize the image proportionally
                newWidth = maxWidth
                newHeight = (newWidth.toFloat() / downloadedImage.width * downloadedImage.height).toInt()
            }
            if (newHeight > maxHeight) {
                // Resize the image proportionally
                newHeight = maxHeight
                newWidth = (newHeight.toFloat() / downloadedImage.height * downloadedImage.width).toInt()
            }

            val left = x + 3 + (maxWidth - newWidth) / 2
            val top = y + 3 + (maxHeight - newHeight) / 2

            context.drawTexture(downloadedImage.identifier,
                left, top, 1f, 1f,
                newWidth, newHeight,
                newWidth, newHeight)
        }

        drawScrollableText(context, MinecraftClient.getInstance().textRenderer, Text.literal(map.name), x + 2, y + this.height - 15, x + width - 2, y + height, 0xffffff);
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder?) {

    }

    override fun onPress() {
    }

}