package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.kvision.*
import io.kvision.core.*
import io.kvision.html.*
import io.kvision.i18n.tr
import io.kvision.pace.Pace
import io.kvision.panel.*
import io.kvision.table.*
import io.kvision.theme.Theme
import io.kvision.theme.ThemeManager
import io.kvision.toast.Toast
import io.kvision.toast.ToastOptions
import io.kvision.utils.perc
import io.kvision.utils.px
import io.kvision.utils.vh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import xyz.wagyourtail.unimined.mapping.formats.umf.UMFReader
import xyz.wagyourtail.unimined.mapping.tree.MappingTree
import kotlin.time.measureTime

val AppScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

class App : Application() {
    init {
        ThemeManager.init(remember = true)
    }

    val main = MainUi()

    override fun start(state: Map<String, Any>) {
        style("thead") {
            top = 0.px
            position = Position.STICKY
        }

        style(".container-fluid") {
            height = 100.perc
        }

        Pace.init()
        Pace.show()
        Pace.start()

        val root = root("kvapp") {
            width = 100.perc
            height = 100.vh
            position = Position.FIXED
            top = 0.px
            bottom = 0.px
            fontSize = 12.px

            add(main)
        }

        AppScope.launch {
            println("requesting mappings...")
            val buf = Buffer().also {
                it.write((Model.requestMappings("1.16.5")).encodeUtf8())
            }
            println("done")
            println("parsing mappings...")
            var mappings: MappingTree
            val time = measureTime {
                try {
                    mappings = UMFReader.read(buf)
                } catch (e: Exception) {
                    Toast.danger("Failed to parse mappings:\n${e.message}", options = ToastOptions(
                        duration = 10 * 1000
                    ))
                    return@launch
                }
            }
            println("done in ${time.inWholeMilliseconds} ms")
            println("assemble table...")
            val columns: List<String> = mappings.namespaces.map { ns -> ns.name }
            // Table(headerNames, types, caption, responsiveType, tableColor, theadColor, tbodyDivider, className, init)
            val loaded = Div() {
                +"Loaded 0 / ${mappings.classes.size} classes"
            }
            val mappingView = main.center.mappingView
            val classes = mappingView.classes.classes
            val classesScroll = mappingView.classes.classScrollContainer
            classes.apply {
                maxHeight = 100.perc
                width = 100.perc
                overflow = Overflow.SCROLL

                this.removeHeaderCells()
                for (n in mappings.namespaces) {
                    this.addHeaderCell(HeaderCell(content = n.name))
                }

                val cList = mappings.classes.toList()

                style(".cls_row", pClass = PClass.HOVER) {
                    cursor = Cursor.POINTER
                }

                tr {
                    th {
                        height = 1.px
                        padding = 0.px
                        color = Color.name(Col.BLACK)
                        setAttribute("colspan", "${columns.size}")
                    }
                }

                val methods = mappingView.classes.content.contentLeft.methods
                val fields = mappingView.classes.content.contentRight.fields

                var i = 0

                fun dynamicLoad() {
                    for (cI in i*100..(++i)*100) {
                        if (cI >= cList.size) {
                            return
                        }
                        val c = cList[cI]
                        row("cls_row") {
                            for (n in mappings.namespaces) {
                                cell(c.getName(n)?.value ?: "-")
                            }
                            onClick {
                                methods.apply {
                                    maxHeight = 100.perc
                                    width = 100.perc
                                    overflow = Overflow.SCROLL

                                    removeHeaderCells()
                                    for (n in mappings.namespaces) {
                                        addHeaderCell(HeaderCell(content = n.name))
                                    }

                                    removeAll()

                                    for (m in c.methods.resolve()) {
                                        row {
                                            for (n in mappings.namespaces) {
                                                m.getName(n)?.let {
                                                    if (m.getDescriptor(n) != null) {
                                                        cell("${it};${m.getDescriptor(n)!!.value}")
                                                    } else {
                                                        cell(it)
                                                    }
                                                } ?: cell("-")
                                            }
                                        }
                                    }
                                }

                                fields.apply {
                                    maxHeight = 100.perc
                                    width = 100.perc
                                    overflow = Overflow.SCROLL

                                    removeHeaderCells()
                                    for (n in mappings.namespaces) {
                                        addHeaderCell(HeaderCell(content = n.name))
                                    }

                                    removeAll()

                                    for (f in c.fields.resolve()) {
                                        row {
                                            for (n in mappings.namespaces) {
                                                f.getName(n)?.let {
                                                    if (f.getDescriptor(n) != null) {
                                                        cell("${it};${f.getDescriptor(n)!!.value}")
                                                    } else {
                                                        cell(it)
                                                    }
                                                } ?: cell("-")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    loaded.content = "Loaded ${i*100} / ${cList.size} classes"
                }

                dynamicLoad()

                classesScroll.onEvent {
                    scroll = {
                        val dyn = it.asDynamic().target
                        val max = dyn.scrollHeight - dyn.clientHeight
                        println("scroll: ${dyn.scrollTop} / $max")
                        if (max - dyn.scrollTop < 100) {
                            dynamicLoad()
                        }
                    }
                }
            }
            classes.add(loaded)

            Pace.stop()
            Pace.hide()
            println("done")
        }
    }
}

fun main() {
    startApplication(
        ::App,
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
