package xyz.wagyourtail.site.minecraft_mapping_viewer.mmv2

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import io.github.oshai.kotlinlogging.Level
import io.github.oshai.kotlinlogging.logger
import io.kvision.Application
import io.kvision.core.*
import io.kvision.html.div
import io.kvision.html.h5
import io.kvision.html.icon
import io.kvision.html.link
import io.kvision.pace.Pace
import io.kvision.panel.*
import io.kvision.require
import io.kvision.state.ObservableValue
import io.kvision.state.sub
import io.kvision.theme.ThemeManager
import io.kvision.utils.auto
import io.kvision.utils.perc
import io.kvision.utils.px
import io.kvision.utils.vh
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okio.Buffer
import okio.use
import xyz.wagyourtail.site.minecraft_mapping_viewer.Model
import xyz.wagyourtail.site.minecraft_mapping_viewer.isMobile
import xyz.wagyourtail.unimined.mapping.EnvType
import xyz.wagyourtail.unimined.mapping.formats.umf.UMFReader
import xyz.wagyourtail.unimined.mapping.formats.umf.UMFWriter
import xyz.wagyourtail.unimined.mapping.tree.MappingTree
import kotlin.time.measureTime

val AppScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

class App : Application() {
    val LOGGER by KotlinLogging.logger()

    init {
        KotlinLoggingConfiguration.logLevel = Level.INFO
        KotlinLoggingConfiguration.formatter
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

    var baseMappings: Pair<Set<String>, MappingTree>? = null
    var patches = mutableMapOf<Pair<String, String>, MappingTree>()

    override fun start(state: Map<String, Any>) {
        window.addEventListener("resize", {
            windowSize.setState(window.innerWidth to window.innerHeight)
        })
        require("css/overrides.css")

        root("kvapp") {
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
                            gridTemplateColumns = "auto auto"

                            div {
                                paddingRight = 5.px

                                link("", url = "https://github.com/unimined/minecraft-mapping-viewer") {
                                    target = "_blank"
                                    icon("fa-brands fa-github")
                                }
                            }
                        }

                        h5("Wagyourtail 2024") {
                            marginTop = 5.px
                            marginBottom = 5.px
                        }
                    }
                }
            }
        }
        var prevMc: Pair<EnvType, String>? = null
        var inc = 0
        settings.value.subscribe { data ->
            if (data == null) return@subscribe
            val (env, mcVers, selected) = data
            inc++
            AppScope.launch {
                LOGGER.info { "selected mappings changed ($inc): $selected" }
                val base = baseMappings
                val required = selected.filter { it.value == null }.keys
                val mc = env to mcVers
                if (prevMc != mc || base == null || !base.first.containsAll(required)) {
                    measureTime {
                        prevMc = mc
                        LOGGER.info { "requesting base mappings $env $mcVers (${required})..." }
                        Buffer().use { buf ->
                            buf.writeUtf8(
                                Model.requestBaseMappings(
                                    mcVers,
                                    env,
                                    required.toList()
                                )
                            )
                            baseMappings = required to UMFReader.read(buf)
                        }
                    }.also {
                        LOGGER.info { "base mappings received in $it" }
                    }
                    LOGGER.info { "clearing existing patches" }
                    patches.clear()
                }

                LOGGER.info { "requesting patches..." }
                measureTime {
                    val newPatches = selected.filter { it.value != null }.mapNotNull { entry ->
                        val mappings = entry.key
                        val version = entry.value ?: return@mapNotNull null
                        if (mappings to version in patches) return@mapNotNull null
                        Buffer().use {
                            it.writeUtf8(
                                Model.requestMappingPatch(
                                    mcVers,
                                    env,
                                    mappings,
                                    version
                                )
                            )
                            mappings to version to UMFReader.read(it)
                        }
                    }
                    for (entry in newPatches) {
                        patches[entry.first] = entry.second
                    }
                }.also {
                    LOGGER.info { "new patches received in $it" }
                }

                val mergedMappings = MappingTree()
                LOGGER.info { "applying base mappings" }
                measureTime {
                    baseMappings?.second?.accept(mergedMappings) ?: error("Failed to apply, base mappings not found")
                }.also {
                    LOGGER.info { "base mappings applied in $it" }
                }
                for (patchId in selected.filter { it.value != null }.map { it.key to it.value }) {
                    LOGGER.info { "applying patch \"$patchId\"" }
                    measureTime {
                        patches[patchId]?.accept(mergedMappings) ?: error("Failed to apply, patch $patchId not found")
                        LOGGER.debug {
                            Buffer().use {
                                patches[patchId]?.accept(UMFWriter.write(it))
                                it.readUtf8()
                            }
                        }
                    }.also {
                        LOGGER.info { "patch \"$patchId\" applied in $it" }
                    }
                }
                LOGGER.debug {
                    Buffer().use {
                        mergedMappings.accept(UMFWriter.write(it))
                        it.readUtf8()
                    }
                }

                mappingViewer.mappings.setState(mergedMappings)
            }
        }
    }

}