package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.logger
import io.kvision.core.AlignItems
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
import xyz.wagyourtail.unimined.mapping.EnvType
import xyz.wagyourtail.unimined.mapping.Namespace

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

    val availableMappings = ObservableValue<Map<String, MappingInfo>?>(null)

    val updating = ObservableValue(false)
    private val selectedMappings = ObservableValue<Map<String, String?>>(emptyMap())
    val selectedMappingNs = ObservableValue<List<Namespace>>(emptyList())

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
                    for ((mapping, info) in avail) {
                        if (info.versions?.isEmpty() != true) {
                            mappingSelectList.hPanel(alignItems = AlignItems.CENTER) {
                                val check = checkBox(label = mapping, value = defaultSelected.contains(mapping)) {
                                    margin = 10.px
                                    subscribe {
                                        if (it) {
                                            defaultSelected.add(mapping)
                                        } else {
                                            defaultSelected.remove(mapping)
                                        }
                                    }
                                }
                                val version = if (info.versions != null) {
                                    selectInput(
                                        options = info.versions.map { it to it },
                                        value = info.versions.first()
                                    ) {
                                        margin = 10.px
                                        paddingRight = 15.px
                                    }
                                } else {
                                    ObservableValue(null)
                                }

                                version.subscribe {
                                    if (check.value) {
                                        selectedMappings.setState(selectedMappings.value + (mapping to it))
                                    }
                                }

                                check.subscribe {
                                    val vers = version.getState()
                                    if (it) {
                                        selectedMappings.setState(selectedMappings.value + (mapping to vers))
                                    } else {
                                        selectedMappings.setState(selectedMappings.value - mapping)
                                    }
                                }
                            }
                        }
                    }
                }
                updating.setState(false)
                selectedMappings.setState(selectedMappings.value)
            }
        }
        var changed = false
        selectedMappings.subscribe { map ->
            selectedMappingNs.setState(listOf(Namespace("official")) + map.keys.flatMap { availableMappings.value!![it]!!.dstNs }.map { Namespace(it) })
            if (updating.value) return@subscribe
            if (app.titlebar.settingsVisible.value && window.isMobile()) {
                if (!changed) {
                    changed = true
                    value.setState(Triple(getEnv(), mcVersion.value ?: return@subscribe, emptyMap()))
                }
                return@subscribe
            }
            value.setState(Triple(getEnv(), mcVersion.value ?: return@subscribe, map))
        }
        app.titlebar.settingsVisible.subscribe {
            if (!it && window.isMobile() && changed) {
                value.setState(Triple(getEnv(), mcVersion.value ?: return@subscribe, selectedMappings.value))
            }
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