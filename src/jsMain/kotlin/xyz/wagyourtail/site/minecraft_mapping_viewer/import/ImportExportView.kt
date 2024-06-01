package xyz.wagyourtail.site.minecraft_mapping_viewer.import

import io.kvision.core.*
import io.kvision.form.select.Select
import io.kvision.form.select.SelectInput
import io.kvision.form.select.select
import io.kvision.form.select.selectInput
import io.kvision.form.text.TextInput
import io.kvision.html.*
import io.kvision.panel.*
import io.kvision.utils.perc
import io.kvision.utils.px
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import okio.Buffer
import okio.use
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.get
import xyz.wagyourtail.site.minecraft_mapping_viewer.AppScope
import xyz.wagyourtail.site.minecraft_mapping_viewer.MappingViewer
import xyz.wagyourtail.site.minecraft_mapping_viewer.isMobile
import xyz.wagyourtail.site.minecraft_mapping_viewer.toByteArray
import xyz.wagyourtail.unimined.mapping.Namespace
import xyz.wagyourtail.unimined.mapping.formats.FormatProvider
import xyz.wagyourtail.unimined.mapping.formats.FormatRegistry
import xyz.wagyourtail.unimined.mapping.formats.unsupported.UnsupportedWriter
import xyz.wagyourtail.unimined.mapping.formats.zip.ZipReader
import xyz.wagyourtail.unimined.mapping.util.escape
import xyz.wagyourtail.unimined.mapping.visitor.delegate.mapNs
import xyz.wagyourtail.unimined.mapping.visitor.delegate.nsFiltered
import xyz.wagyourtail.unimined.mapping.visitor.delegate.recordNamespaces
import kotlin.js.Promise

class ImportExportView(mappingViewer: MappingViewer) : Div() {

    init {
//        FormatRegistry.registerFormat(FormatProvider(ZipReader))
        height = 100.perc
        display = Display.GRID
        gridTemplateColumns = if (window.isMobile()) {
            "1fr"
        } else {
            "1fr 1fr"
        }
    }

    val importPanel = vPanel {
        borderRight = Border(1.px, BorderStyle.SOLID, Color.hex(0x000000))
        height = 100.perc
        padding = 10.px

        val import = input {
            type = InputType.FILE
            name = "importFile"
        }

        h5 {
            paddingTop = 10.px
            +"Namespace Mappings"
        }
        val mapNamespace = table {
        }

        h5 {
            paddingTop = 10.px
            +"Add:"
        }
        val maps = mutableMapOf<TextInput, TextInput>()
        val from = TextInput {
            placeholder = "From"
            marginRight = 5.px
        }
        val to = TextInput {
            placeholder = "To"
            marginRight = 5.px
        }
        hPanel {
            div {
                hPanel {
                    add(from)
                    add(to)
                }
                flexPanel {
                    justifyContent = JustifyContent.CENTER
                    button("Import") {
                        marginTop = 10.px
                        onClick {
                            AppScope.launch {
                                val fileData = (import.getElement() as HTMLInputElement).files?.get(0)
                                if (fileData == null) {
                                    window.alert("No file selected")
                                    return@launch
                                }
                                Buffer().use { buf ->
                                    buf.write((fileData.asDynamic().arrayBuffer() as Promise<ArrayBuffer>).await().toByteArray())
                                    val env = mappingViewer.app.settings.getEnv()
                                    val format = FormatRegistry.autodetectFormat(env, fileData.name, buf)
                                    if (format == null) {
                                        if (ZipReader.isFormat(env, fileData.name, buf)) {
                                            window.alert("Zip/Jar files are not currently supported")
                                        } else {
                                            window.alert("Unknown format")
                                        }
                                        return@launch
                                    }

                                    if (mappingViewer.mappings.value == null) {
                                        throw IllegalStateException("No mappings loaded")
                                    }
                                    val ns = mutableSetOf<Namespace>()
                                    format.reader.read(
                                        env,
                                        buf,
                                        mappingViewer.mappings.value!!,
                                        mappingViewer.mappings.value!!.nsFiltered("", inverted = true).recordNamespaces { ns.addAll(it) },
                                        maps.map { (it.key.value ?: "") to (it.value.value ?: "") }.toMap()
                                    )
                                    mappingViewer.app.settings.selectedMappingNs.setState((mappingViewer.app.settings.selectedMappingNs.value + ns).toSet().toList())
                                    mappingViewer.mappings.setState(mappingViewer.mappings.value)
                                }
                            }
                        }
                    }
                }
            }
            div {
                button("+") {
                    marginLeft = 5.px
                    onClick {
                        mapNamespace.tr {
                            val fromInp = TextInput {
                                value = from.value
                                marginRight = 5.px
                            }
                            from.value = ""
                            val toInp = TextInput {
                                value = to.value
                                marginRight = 5.px
                            }
                            to.value = ""
                            maps[fromInp] = toInp
                            td {
                                add(fromInp)
                            }
                            td {
                                add(toInp)
                            }
                        }
                    }
                }
            }
        }
    }

    val exportPanel = vPanel {
        borderLeft = Border(1.px, BorderStyle.SOLID, Color.hex(0x000000))
        height = 100.perc
        padding = 10.px

        h5 {
            paddingTop = 10.px
            +"Namespace Mappings"
        }
        val mapNamespace = table {
        }
        val maps = mutableMapOf<SelectInput, TextInput>()
        val from = SelectInput {
            mappingViewer.app.settings.selectedMappingNs.subscribe {
                options = it.map { it.name to it.name }
                value = it.firstOrNull()?.name
            }
            marginRight = 5.px
        }
        val to = TextInput {
            placeholder = "To"
            marginRight = 5.px
        }
        from.subscribe {
            to.value = from.value
        }
        hPanel {
            div {
                hPanel {
                    add(from)
                    add(to)
                }
                flexPanel {
                    justifyContent = JustifyContent.CENTER
                    val format = selectInput {
                        paddingTop = 5.px

                        options = FormatRegistry.formats.filter { it.writer != UnsupportedWriter }.map { it.name to it.name }
                        value = options!!.firstOrNull()?.first
                    }
                    button("Export") {
                        marginTop = 10.px
                        onClick {
                            AppScope.launch {
                                Buffer().use {
                                    val env = mappingViewer.app.settings.getEnv()
                                    val provider = FormatRegistry.formats.firstOrNull { it.name == format.value }
                                    if (provider == null) {
                                        window.alert("No Format Selected?")
                                        return@launch
                                    }
                                    val writer = provider.writer
                                    mappingViewer.mappings.value?.accept(writer.write(
                                        env,
                                        it
                                    ).mapNs(maps.map { Namespace(it.key.value!!) to Namespace(it.value.value!!) }.toMap()).nsFiltered(maps.keys.map { Namespace(it.value!!) }.toSet()))
                                    this@ImportExportView.download(Blob(arrayOf(Uint8Array(it.readByteArray().toTypedArray()).buffer), BlobPropertyBag("text/plain;charset=utf-8")), "mappings.${writer.name}")
                                }
                            }
                        }
                    }
                }
            }
            div {
                button("+") {
                    marginLeft = 5.px
                    onClick {
                        mapNamespace.tr {
                            val fromInp = SelectInput {
                                options = mappingViewer.app.settings.selectedMappingNs.value.map { it.name to it.name }
                                value = from.value
                                marginRight = 5.px
                            }
                            val toInp = TextInput {
                                value = to.value
                                marginRight = 5.px
                            }
                            to.value = from.value
                            maps[fromInp] = toInp
                            td {
                                add(fromInp)
                            }
                            td {
                                add(toInp)
                            }
                        }
                    }
                }
            }
        }
    }

    fun download(file: Blob, filename: String) {
        if (window.navigator.asDynamic().msSaveOrOpenBlob != null) { // IE10+
            window.navigator.asDynamic().msSaveOrOpenBlob(file, filename)
        } else { // Others
            val a = document.createElement("a")
            val url = URL.createObjectURL(file)
            a.setAttribute("href", url)
            a.setAttribute("download", filename)
            document.body?.appendChild(a)
            a.asDynamic().click()
            window.setTimeout({
                document.body?.removeChild(a)
                URL.revokeObjectURL(url)
            }, 0)
        }
    }
}
