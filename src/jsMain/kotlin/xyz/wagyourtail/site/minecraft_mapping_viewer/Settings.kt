package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.logger
import io.kvision.core.BsBgColor
import io.kvision.core.WhiteSpace
import io.kvision.form.check.checkBox
import io.kvision.form.select.select
import io.kvision.form.select.selectInput
import io.kvision.html.Div
import io.kvision.html.div
import io.kvision.panel.hPanel
import io.kvision.panel.vPanel
import io.kvision.state.ObservableSetWrapper
import io.kvision.state.ObservableValue
import io.kvision.utils.perc
import io.kvision.utils.px
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import xyz.wagyourtail.site.minecraft_mapping_viewer.AppScope
import xyz.wagyourtail.site.minecraft_mapping_viewer.MinecraftMappingViewer
import xyz.wagyourtail.site.minecraft_mapping_viewer.Model
import xyz.wagyourtail.unimined.mapping.EnvType

class Settings(val app: MinecraftMappingViewer) : Div(className = BsBgColor.BODYSECONDARY.className) {
    val LOGGER by KotlinLogging.logger()
    init {
        whiteSpace = WhiteSpace.NOWRAP
        +("Settings")
    }

    private val versions = ObservableValue<List<Pair<String, Boolean>>?>(null)

    init {
        AppScope.launch {
            LOGGER.info { "requesting versions..." }
            versions.setState(Model.requestVersions())
        }
    }

    private val envType = select(label = "Environment", options = EnvType.entries.map { it.name to it.name }, value = EnvType.JOINED.name) {
        margin = 10.px
    }

    private val mcVersion = select(label = "Minecraft Version") {
        margin = 10.px
    }

    private val snapshots = checkBox(label = "Snapshots") {
        margin = 10.px
    }

    init {
        div(className = BsBgColor.BODYTERTIARY.className) {
            width = 100.perc
            height = 2.px
        }
    }

    private val mappingSelectList = vPanel {

    }

    fun getEnv() = EnvType.valueOf(envType.value ?: EnvType.JOINED.name)

    private val defaultSelected = ObservableSetWrapper<String>()
    private val availableMappings = ObservableValue<Map<String, List<String>>?>(null)

    val updating = ObservableValue(false)
    private val selectedMappings = ObservableValue<Map<String, String?>>(emptyMap())

    val value = ObservableValue<Triple<EnvType, String, Map<String, String?>>?>(null)

    init {
        localStorage.getItem("settings.defaultSelected")?.let { data ->
            Json.parseToJsonElement(data).jsonArray.map { it.jsonPrimitive.content }.forEach {
                defaultSelected.add(it)
            }
        }
        defaultSelected.subscribe {
            val obj = buildJsonArray {
                it.forEach {
                    this.add(JsonPrimitive(it))
                }
            }
            localStorage.setItem("settings.defaultSelected", obj.toString())
        }
        versions.subscribe {
            updateVersions()
        }
        snapshots.subscribe {
            updateVersions()
        }
        mcVersion.subscribe {
            updateAvailableMappings()
        }
        envType.subscribe {
            updateAvailableMappings()
        }
        availableMappings.subscribe { avail ->
            mappingSelectList.removeAll()
            if (avail != null) {
                singleRender {
                    updating.setState(true)
                    for ((mapping, versions) in avail) {
                        mappingSelectList.hPanel {
                            val check = checkBox(label = mapping, value = defaultSelected.contains(mapping)) {
                                margin = 10.px
                                subscribe {
                                    println("checkbox: $mapping")
                                    if (it) {
                                        defaultSelected.add(mapping)
                                    } else {
                                        defaultSelected.remove(mapping)
                                    }
                                }
                            }
                            val version = if (versions.size > 1) {
                                selectInput {
                                    options = versions.map { it to it }
                                    value = defaultSelected.firstOrNull { it in versions }
                                    margin = 10.px
                                }
                            } else {
                                ObservableValue(versions.firstOrNull())
                            }

                            version.subscribe {
                                if (it == null) {
                                    check.setState(false)
                                } else {
                                    selectedMappings.setState(selectedMappings.value + (mapping to it))
                                }
                            }
                            check.subscribe {
                                val vers = version.getState()
                                if (it && vers != null) {
                                    selectedMappings.setState(selectedMappings.value + (mapping to vers))
                                } else {
                                    selectedMappings.setState(selectedMappings.value - mapping)
                                }
                            }
                        }
                    }
                    updating.setState(false)
                    selectedMappings.setState(selectedMappings.value)
                }
            }
        }
        selectedMappings.subscribe {
            if (updating.value) return@subscribe
            window.setTimeout({
                value.setState(Triple(getEnv(), mcVersion.value ?: return@setTimeout, it))
            }, 0)
        }
    }

    fun updateVersions() {
        mcVersion.options = versions.value?.filter { it.second || snapshots.value }?.map { it.first to it.first }
        if (mcVersion.value == null) {
            mcVersion.value = mcVersion.options?.firstOrNull()?.first
        }
    }

    fun updateAvailableMappings() {
        AppScope.launch {
            LOGGER.info { "requesting available mappings ${mcVersion.value}..." }
            availableMappings.setState(Model.availableMappings(mcVersion.value ?: return@launch, getEnv()))
        }
    }

}