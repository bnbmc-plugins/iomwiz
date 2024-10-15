package com.vicr123.client.screens

import com.vicr123.client.iom.IOMClient
import com.vicr123.client.iom.MapsChangedListener
import com.vicr123.client.screens.widgets.MapWidget
import com.vicr123.client.system.SystemIntegration
import kotlinx.coroutines.runBlocking
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text
import java.net.URL
import java.util.concurrent.CompletableFuture

private const val MAP_SIZE = 80
private const val MAP_SPACING = 10

@Environment(EnvType.CLIENT)
class IOMScreen(private val client: MinecraftClient, private val iomClient: IOMClient) : Screen(Text.literal("IOM")) {
    private val chooseFileButton = ButtonWidget.builder(Text.literal("Choose a file"), ::chooseFile).build()
    private val openInBrowserButton = ButtonWidget.builder(Text.translatable("iomwiz.browser.open"), ::openInBrowser).build()
    private lateinit var catgList: CatgList
    private val shownMapButtons: MutableList<MapWidget> = mutableListOf()

    class CatgList(private val parentScreen: IOMScreen, minecraftClient: MinecraftClient?, i: Int, j: Int, k: Int) :
        AlwaysSelectedEntryListWidget<CatgEntry>(
            minecraftClient,
            i,
            j,
            k,
            20
        ) {

        init {
            setRenderBackground(false)

            parentScreen.iomClient.mapsChangedListeners.add(object : MapsChangedListener {
                override fun onMapsChanged() {
                    updateCategories()
                }
            })
            updateCategories()
        }

        fun updateCategories() {
            while (entryCount > 0) {
                this.remove(0)
            }

            for (category in parentScreen.iomClient.maps?.groupBy { it.category }?.keys ?: emptyList()) {
                this.addEntry(CatgEntry(parentScreen, category))
            }
            if (entryCount > 0 && selectedOrNull == null) {
                setSelected(getEntry(0))
            }
        }

        override fun onClick(mouseX: Double, mouseY: Double) {
            super.onClick(mouseX, mouseY)
            val entry = getEntryAtPosition(mouseX, mouseY)
            if (entry != null) {
                setSelected(entry)
            }
        }

        override fun setSelected(entry: CatgEntry?) {
            super.setSelected(entry)
            parentScreen.updateShownMaps()
        }
    }

    class CatgEntry(private val parentScreen: IOMScreen, val category: String) : AlwaysSelectedEntryListWidget.Entry<CatgEntry>() {
        override fun render(
            context: DrawContext?,
            index: Int,
            y: Int,
            x: Int,
            entryWidth: Int,
            entryHeight: Int,
            mouseX: Int,
            mouseY: Int,
            hovered: Boolean,
            tickDelta: Float
        ) {
            context?.drawCenteredTextWithShadow(parentScreen.client!!.textRenderer,
                if (category.isEmpty()) Text.translatable("iomwiz.category.uncategorised") else Text.literal(category), x + entryWidth / 2, y + entryHeight / 2, 0xffffff)
        }

        override fun getNarration(): Text {
            return if (category.isEmpty()) Text.translatable("iomwiz.category.uncategorised") else Text.literal(category)
        }

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            return true
        }
    }

    override fun init() {
        super.init()

        chooseFileButton.setPosition(width / 2, height - 10)
        chooseFileButton.active = SystemIntegration.canOpenFilePicker()
        addDrawableChild(chooseFileButton)

        openInBrowserButton.setPosition(10, height - 40)
        openInBrowserButton.active = SystemIntegration.canOpenUrl()
        addDrawableChild(openInBrowserButton)

        catgList = CatgList(this, client, 100, height - 80, 10)
        catgList.setPosition(10, 10)
        catgList.setDimensions(150, height - 80)
        catgList.active = true
        addDrawableChild(catgList)
    }

    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
    }

    fun updateShownMaps() {
        for (button in shownMapButtons) {
            remove(button)
        }
        shownMapButtons.clear()

        if (!this::catgList.isInitialized) return
        if (catgList.selectedOrNull == null) return

        val firstX = 170
        val maxX = width - 10
        val firstY = 10
        val maxY = height - 40

        val mapsHorizontally = (maxX - firstX) / (MAP_SIZE + MAP_SPACING)
        val mapsVertically = (maxY - firstY) / (MAP_SIZE + MAP_SPACING)

        val validMaps = iomClient.maps?.groupBy { it.category }?.get(catgList.selectedOrNull!!.category)
        if (validMaps == null) return;

        val firstItem = 0;
        var currentOffset = 0;
        while (firstItem + currentOffset < validMaps.size && currentOffset < mapsHorizontally * mapsVertically) {
            val thisX = firstX + (currentOffset % mapsHorizontally) * (MAP_SIZE + MAP_SPACING)
            val thisY = firstY + (currentOffset / mapsHorizontally) * (MAP_SIZE + MAP_SPACING)

            val mapButton = MapWidget(thisX, thisY, MAP_SIZE, MAP_SIZE, validMaps[firstItem + currentOffset], iomClient)
            addDrawableChild(mapButton)
            shownMapButtons.add(mapButton)

            currentOffset++
        }
    }

    override fun resize(client: MinecraftClient?, width: Int, height: Int) {
        super.resize(client, width, height)
        updateShownMaps()
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