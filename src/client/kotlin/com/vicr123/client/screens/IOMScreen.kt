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
import net.minecraft.client.gui.screen.ConfirmScreen
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.texture.NativeImage
import net.minecraft.text.Text
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

private const val MAP_SIZE = 80
private const val MAP_SPACING = 10

@Environment(EnvType.CLIENT)
class IOMScreen(private val client: MinecraftClient, private val iomClient: IOMClient) : Screen(Text.literal("IOM")) {
    private val chooseFileButton = ButtonWidget.builder(Text.translatable("iomwiz.upload.text"), ::chooseFile).build()
    private val openInBrowserButton = ButtonWidget.builder(Text.translatable("iomwiz.browser.open"), ::openInBrowser).build()
    private val previousPageButton = ButtonWidget.builder(Text.literal("<"), ::previousPage).build()
    private val nextPageButton = ButtonWidget.builder(Text.literal(">"), ::nextPage).build()
    private lateinit var catgList: CatgList
    private val shownMapButtons: MutableList<MapWidget> = mutableListOf()
    private var pageNumber = 0
    private var totalPages = 0

    val mapsChangedListener = object : MapsChangedListener {
        override fun onMapsChanged() {
            updatePage()
        }
    }

    companion object {
        var currentCategoryName = ""
    }

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
            val currentCategory = currentCategoryName
            while (entryCount > 0) {
                this.remove(0)
            }

            for (category in parentScreen.iomClient.maps?.groupBy { it.category }?.keys ?: emptyList()) {
                val entry = CatgEntry(parentScreen, category)
                this.addEntry(entry)
                if (category == currentCategory && selectedOrNull == null) {
                    setSelected(entry)
                }
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
            currentCategoryName = entry?.category ?: ""
            parentScreen.pageNumber = 0
            parentScreen.updatePage()
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

        chooseFileButton.setPosition(width / 2, height - 30)
        chooseFileButton.active = SystemIntegration.canOpenFilePicker()
        addDrawableChild(chooseFileButton)

        openInBrowserButton.setPosition(10, height - 30)
        openInBrowserButton.active = SystemIntegration.canOpenUrl()
        addDrawableChild(openInBrowserButton)

        previousPageButton.setPosition(170, height - 30)
        previousPageButton.setDimensions(20, 20)
        addDrawableChild(previousPageButton)

        nextPageButton.setPosition(250, height - 30)
        nextPageButton.setDimensions(20, 20)
        addDrawableChild(nextPageButton)

        catgList = CatgList(this, client, 100, height - 80, 10)
        catgList.setPosition(10, 10)
        catgList.setDimensions(150, height - 80)
        catgList.active = true
        addDrawableChild(catgList)

        iomClient.mapsChangedListeners.add(mapsChangedListener)

        updateShownMaps()
        updatePage()
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

        val firstItem = pageSize() * pageNumber;
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

    fun pageSize(): Int {
        val firstX = 170
        val maxX = width - 10
        val firstY = 10
        val maxY = height - 40

        val mapsHorizontally = (maxX - firstX) / (MAP_SIZE + MAP_SPACING)
        val mapsVertically = (maxY - firstY) / (MAP_SIZE + MAP_SPACING)

        return mapsHorizontally * mapsVertically
    }

    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        context?.drawCenteredTextWithShadow(client!!.textRenderer, Text.literal("${pageNumber + 1} / $totalPages"), 220, height - 25, 0xffffff)
    }

    override fun resize(client: MinecraftClient?, width: Int, height: Int) {
        super.resize(client, width, height)
        updatePage()
        updateShownMaps()
    }

    fun chooseFile(button: ButtonWidget) {
        val uploadCategory = catgList.selectedOrNull?.category ?: ""
        CompletableFuture.supplyAsync {
            runBlocking {
                return@runBlocking SystemIntegration.openFilePicker()
            }
        }.thenAccept { urls ->
            client?.send {
                processUpload(urls.map { Path.of(URI(it)) }, uploadCategory)
            }
        }
    }

    fun processUpload(paths: List<Path>, uploadCategory: String) {
        val nonCanonPaths = mutableSetOf<Path>()
        for (path in paths) {
            val nativeImage = path.toFile().inputStream().use {
                try {
                    return@use NativeImage.read(it);
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@use null;
                }
            }
            if (nativeImage != null && (nativeImage.height % 128 != 0 || nativeImage.width % 128 != 0)) {
                nonCanonPaths.add(path)
            }
        }

        if (nonCanonPaths.isNotEmpty()) {
            client?.setScreen(ConfirmScreen({ ok ->
                client?.setScreen(this)

                for (path in paths) {
                    iomClient.uploadMap(path.toFile(), uploadCategory)
                }
            }, Text.translatable("iomwiz.noncanon.title"), Text.translatable("iomwiz.noncanon.message"), Text.translatable("iomwiz.noncanon.yes"), Text.translatable("iomwiz.noncanon.no")))
            return
        }

        for (path in paths) {
            iomClient.uploadMap(path.toFile(), uploadCategory)
        }
    }

    override fun removed() {
        // TODO: Cancel any running coroutines
        iomClient.mapsChangedListeners.remove(mapsChangedListener)
        super.removed()
    }

    fun previousPage(button: ButtonWidget) {
        pageNumber--
        updatePage()
    }

    fun nextPage(button: ButtonWidget) {
        pageNumber++
        updatePage()
    }

    fun updatePage() {
        if (!this::catgList.isInitialized) return

        val validMaps = if (catgList.selectedOrNull == null) null else iomClient.maps?.groupBy { it.category }?.get(catgList.selectedOrNull!!.category)
        previousPageButton.active = pageNumber > 0
        nextPageButton.active = validMaps != null && (pageNumber + 1) * pageSize() < validMaps.size
        totalPages = (validMaps?.size ?: 1) / pageSize() + 1

        updateShownMaps()
    }

    override fun filesDragged(paths: MutableList<Path>?) {
        super.filesDragged(paths)
        if (paths != null) {
            processUpload(paths, catgList.selectedOrNull?.category ?: "")
        }
    }

    fun openInBrowser(button: ButtonWidget) {
        SystemIntegration.openUrl(URL("${iomClient.serverRoot}?auth=${iomClient.authToken}"))
    }
}