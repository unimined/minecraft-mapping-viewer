package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import io.github.oshai.kotlinlogging.Level
import io.github.oshai.kotlinlogging.logger
import io.kvision.*
import io.kvision.core.*
import io.kvision.html.customTag
import io.kvision.html.div
import io.kvision.html.icon
import io.kvision.html.link
import io.kvision.pace.Pace
import io.kvision.panel.*
import io.kvision.state.ObservableValue
import io.kvision.theme.ThemeManager
import io.kvision.utils.*
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okio.Buffer
import okio.use
import xyz.wagyourtail.unimined.mapping.formats.umf.UMFReader
import xyz.wagyourtail.unimined.mapping.formats.umf.UMFWriter
import xyz.wagyourtail.unimined.mapping.tree.LazyMappingTree
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

val ENABLE_DEBUG = false

fun main() {
    startApplication(
        ::MinecraftMappingViewer,
        module.hot,
        BootstrapModule,
        BootstrapCssModule,
        TomSelectModule,
        BootstrapUploadModule,
        ToastifyModule,
        FontAwesomeModule,
        TabulatorModule,
        TabulatorCssBootstrapModule,
        CoreModule
    )
}

class MinecraftMappingViewer : Application() {
    val LOGGER by KotlinLogging.logger()

    init {
        KotlinLoggingConfiguration.logLevel = if (ENABLE_DEBUG) {
            Level.DEBUG
        } else {
            Level.INFO
        }
        KotlinLoggingConfiguration.formatter = MessageFormatterOverride()
        ThemeManager.init(remember = true)
        Pace.init()
        Pace.show()
        Pace.start()
        UMFReader.uncheckedReading = true
    }

    val titlebar = TitleBar(this)
    val settings = Settings(this)
    val mappingViewer = MappingViewer(this)

    val windowSize = ObservableValue(0 to 0)

    @OptIn(ExperimentalTime::class)
    override fun start(state: Map<String, Any>) {
        window.addEventListener("resize", {
            windowSize.setState(window.innerWidth to window.innerHeight)
        })
        require("css/overrides.css")

        val root = root("kvapp") {
            singleRender {
                vPanel {
                    height = 100.vh
                    div {
                        add(titlebar)
                    }

                    div {
                        flexGrow = 1
                        flexShrink = 1
                        flexBasis = auto
                        height = 100.perc
                        overflow = Overflow.HIDDEN

                        val mobile = StackPanel(className = "mobile") {
                            height = 100.perc
                            overflow = Overflow.HIDDEN

                            titlebar.settingsVisible.subscribe {
                                activeChild = if (it) settings else mappingViewer
                            }

                        }

                        val desktop = hPanel(className = "desktop") {
                            height = 100.perc
                            overflow = Overflow.HIDDEN
                        }

                        windowSize.subscribe {
                            singleRender {
                                if (window.isMobile()) {
                                    removeAll()
                                    add(mobile.apply {
                                        add(settings)
                                        add(mappingViewer)
                                    })
                                } else {
                                    removeAll()
                                    add(desktop.apply {
                                        add(settings)
                                        add(mappingViewer)
                                    })
                                }
                            }
                        }

                    }

                    flexPanel(justify = JustifyContent.SPACEBETWEEN) {
                        flexGrow = 0
                        flexShrink = 1
                        flexBasis = auto

                        borderTop = Border(2.px, BorderStyle.SOLID, Color.name(Col.LIGHTGRAY))

                        gridPanel(alignItems = AlignItems.CENTER) {
                            height = 100.perc
                            gridTemplateColumns = "auto auto auto"

                            div {
                                marginLeft = 10.px
                                paddingRight = 5.px

                                link("", url = "https://github.com/unimined/minecraft-mapping-viewer") {
                                    setAttribute("aria-label", "Github")

                                    target = "_blank"
                                    icon("fa-brands fa-github")
                                }

                                link("", url = "https://discord.gg/P6W58J8") {
                                    marginLeft = 5.px
                                    setAttribute("aria-label", "Discord")

                                    target = "_blank"
                                    icon("fa-brands fa-discord")
                                }
                            }

                            div {
                                height = 90.perc
                                width = 1.px
                                background = Background(Color.name(Col.GRAY))
                                marginLeft = 5.px
                                marginRight = 5.px
                            }

                            div {
                                link("", url = "https://ko-fi.com/wagyourtail") {
                                    marginLeft = 5.px
                                    setAttribute("aria-label", "Ko-fi")

                                    target = "_blank"
//                                    image("/image/ko-fi.svg", alt = "Ko-fi") {
//                                        height = 24.px
//                                        paddingBottom = 4.px
//                                    }
                                    customTag("svg") {
                                        setAttribute("height", "20")
                                        setAttribute("width", "20")
                                        setAttribute("viewBox", "0 0 24 24")
                                        setAttribute("role", "img")
                                        setAttribute("xmlns", "http://www.w3.org/2000/svg")
                                        setStyle("fill", "rgba(var(--bs-link-color-rgb)")
                                        marginBottom = 4.px
                                        style(pClass = PClass.HOVER) {
                                            setStyle("fill", "rgba(var(--bs-link-hover-color-rgb)")
                                        }
                                        customTag("path") {
                                            setAttribute("d", "M23.881 8.948c-.773-4.085-4.859-4.593-4.859-4.593H.723c-.604 0-.679.798-.679.798s-.082 7.324-.022 11.822c.164 2.424 2.586 2.672 2.586 2.672s8.267-.023 11.966-.049c2.438-.426 2.683-2.566 2.658-3.734 4.352.24 7.422-2.831 6.649-6.916zm-11.062 3.511c-1.246 1.453-4.011 3.976-4.011 3.976s-.121.119-.31.023c-.076-.057-.108-.09-.108-.09-.443-.441-3.368-3.049-4.034-3.954-.709-.965-1.041-2.7-.091-3.71.951-1.01 3.005-1.086 4.363.407 0 0 1.565-1.782 3.468-.963 1.904.82 1.832 3.011.723 4.311zm6.173.478c-.928.116-1.682.028-1.682.028V7.284h1.77s1.971.551 1.971 2.638c0 1.913-.985 2.667-2.059 3.015z")
                                        }
                                    }
                                }

                                link("", url = "https://patreon.com/wagyourtail") {
                                    marginLeft = 5.px
                                    setAttribute("aria-label", "Patreon")

                                    target = "_blank"
                                    icon("fa-brands fa-patreon")
                                }
                            }
                        }

                        div("Wagyourtail 2024") {
                            fontSize = 1.rem
                            marginTop = 5.px
                            marginBottom = 5.px
                            marginRight = 10.px
                        }
                    }
                }
            }
        }

        var inc = 0
        settings.value.subscribe { data ->
            if (data == null) return@subscribe
            val (env, mcVers, selected) = data
            inc++

            AppScope.launch {
                mappingViewer.mappings.setState(null)
                mappingViewer.loading.setState(true)

                LOGGER.info { "selected mappings changed ($inc): $selected" }
                val required = selected.filter { it.value == null }.keys
                val mc = env to mcVers
                var baseMappings: String

                mappingViewer.loadingMessage.value = "Requesting Mappings"
                var count = 0
                var patches: Map<Pair<String, String?>, String>
                measureTime {
                    val newPatches = selected.map { entry ->
                        try {
                            val mappings = entry.key
                            val version = entry.value
                            mappingViewer.loadingMessage.value =
                                "Requesting Mapping $mappings${if (version == null) "" else "-$version"} (${++count} / ${selected.size})"
                            mappings to version to Model.requestMappingPatch(
                                mcVers,
                                env,
                                mappings,
                                version
                            )
                        } catch (t: Throwable) {
                            mappingViewer.showError("Error requesting mappings", t)
                            throw t
                        }
                    }
                    patches = newPatches.toMap()
                }.also {
                    mappingViewer.loadingMessage.value = "$count new patches received in $it"
                }

                val mergedMappings = LazyMappingTree()

                count = 0
                measureTime {
                    for ((patchId, patch) in patches.entries) {
                        mappingViewer.loadingMessage.value =
                            "applying patch \"$patchId\" (${++count} / ${patches.size})"
                        measureTime {
                            try {
                                UMFReader.read(patch, mergedMappings)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                LOGGER.error { "Failed to apply patch \"$patchId\"" }
                            }
                        }.also {
                            LOGGER.info { "patch \"$patchId\" applied in $it" }
                        }
                    }
                }.also {
                    mappingViewer.loadingMessage.value = "Loaded ${patches.size} patches in $it"
                }

                LOGGER.debug {
                    Buffer().use {
                        mergedMappings.accept(UMFWriter.write(it))
                        it.readUtf8()
                    }
                }

                mappingViewer.mappings.setState(mergedMappings)
                mappingViewer.loading.setState(false)
            }
        }
    }

}

val AppScope = CoroutineScope(Dispatchers.Default + SupervisorJob())