package xyz.wagyourtail.site.minecraft_mapping_viewer.mmv2.tabs.classes

import io.kvision.core.Overflow
import io.kvision.core.onClick
import io.kvision.html.div
import io.kvision.panel.*
import io.kvision.state.ObservableValue
import io.kvision.utils.perc
import kotlinx.browser.window
import xyz.wagyourtail.site.minecraft_mapping_viewer.mmv2.improved.BetterTable
import xyz.wagyourtail.site.minecraft_mapping_viewer.isMobile
import xyz.wagyourtail.site.minecraft_mapping_viewer.mmv2.improved.BetterTabPanel
import xyz.wagyourtail.site.minecraft_mapping_viewer.mmv2.improved.FasterSplitPanel
import xyz.wagyourtail.site.minecraft_mapping_viewer.mmv2.tabs.ClassViewer
import xyz.wagyourtail.site.minecraft_mapping_viewer.mmv2.tabs.info.InfoViewer
import xyz.wagyourtail.unimined.mapping.jvms.ext.NameAndDescriptor
import xyz.wagyourtail.unimined.mapping.jvms.four.two.two.UnqualifiedName
import xyz.wagyourtail.unimined.mapping.tree.node.ClassNode
import xyz.wagyourtail.unimined.mapping.tree.node.FieldNode
import xyz.wagyourtail.unimined.mapping.tree.node.MemberNode
import xyz.wagyourtail.unimined.mapping.tree.node.MethodNode

class ClassContentViewer(val parentElement: ClassViewer, val classNode: ClassNode) : BetterTabPanel() {

    init {
        width = 100.perc
        height = 100.perc
    }

    val classInfoTab = tab("Info") {
        InfoViewer(classNode).also { add(it) }
    }

    val methodList = BetterTable(className = "method-table").apply {
        height = 100.perc
    }

    val fieldList = BetterTable(className = "field-table").apply {
        height = 100.perc
    }

     val selectedField = ObservableValue<FieldNode?>(null)
     val selectedMethod = ObservableValue<MethodNode?>(null)

     val selectedParamOrLocal = ObservableValue<MemberNode<*,*,*>?>(null)

    init {

        val hasFields = classNode.fields.resolve().isNotEmpty()
       val hasMethods = classNode.methods.resolve().isNotEmpty()

        fieldList.head.row {
            for (ns in classNode.root.namespaces) {
                header(ns.name)
            }
        }


        val fieldBody = fieldList.body {}
        for (field in classNode.fields.resolve()) {
            fieldBody.row(data = field) {
                for (name in classNode.root.namespaces) {
                    cell(field.getName(name)?.let { NameAndDescriptor(UnqualifiedName.unchecked(it), field.getDescriptor(name)).value } ?: "-") {
                        onClick {
                            selectedField.setState(field)
                        }
                    }
                }
            }
        }
        fieldList.activeRow.subscribe {
            selectedField.setState(it as FieldNode?)
        }

        methodList.head.row {
            for (ns in classNode.root.namespaces) {
                header(ns.name)
            }
        }

        val methodBody = methodList.body {}
        for (method in classNode.methods.resolve()) {
            methodBody.row(data = method) {
                for (name in classNode.root.namespaces) {
                    cell(method.getName(name)?.let { NameAndDescriptor(UnqualifiedName.unchecked(it), method.getDescriptor(name)).value } ?: "-")
                }
            }
        }
        methodList.activeRow.subscribe {
            selectedMethod.setState(it as MethodNode?)
        }

        if (hasFields || hasMethods) {
            tab("Content") {
                if (window.isMobile()) {
                    BetterTabPanel {
                        if (hasMethods) {
                            tab("Methods") {
                                div("methods") {
                                    width = 100.perc
                                }
                            }
                        }
                        if (hasFields) {
                            tab("Fields") {
                                div("fields") {
                                    width = 100.perc
                                }
                            }
                        }
                    }.also {
                        add(it)
                    }
                } else {

                    val leftTab = BetterTabPanel(tabPosition = TabPosition.LEFT) {
                        height = 100.perc

                        if (hasMethods) {
                            tab("Methods") {
                                div {
                                    height = 100.perc
                                    overflow = Overflow.AUTO

                                    add(methodList)
                                }
                            }
                        }
                    }

                    val mobile = window.isMobile()

                    val rightTab = (if (mobile) leftTab else BetterTabPanel(tabPosition = TabPosition.RIGHT)).apply {
                        height = 100.perc

                        if (hasFields) {
                            tab("Fields") {
                                div {
                                    height = 100.perc
                                    overflow = Overflow.AUTO

                                    add(fieldList)
                                }
                            }
                        }
                    }

                    fun addBoth() {
                        if (mobile) {
                            add(leftTab)
                        } else {
                            FasterSplitPanel(direction = Direction.VERTICAL, className = "class-content-split") {
                                height = 100.perc
                                minSize = 100

                                add(leftTab)
                                add(rightTab)
                            }.also {
                                add(it)
                            }
                        }
                    }

                    if (leftTab.getTabs().isEmpty()) {
                        add(rightTab)
                    } else if (rightTab.getTabs().isEmpty()) {
                        add(leftTab)
                    } else {
                        addBoth()
                    }

                    selectedField.subscribe {
                        if (it != null) {
                            if (leftTab.getTabs().isEmpty()) {
                                removeAll()
                                addBoth()
                            }
                            leftTab.apply {
                                tab("Field Info") {
                                    InfoViewer(it)
                                }
                            }
                        }
                    }

                    var methodInfo: Tab? = null
                    var parameters: Tab? = null
                    var localVariables: Tab? = null

                    selectedMethod.subscribe {
                        if (it != null) {
                            if (rightTab.getTabs().isEmpty()) {
                                removeAll()
                                addBoth()
                            }
                            rightTab.apply {
                                methodInfo?.let { removeTab(it) }
                                tab("Method Info") {
                                    InfoViewer(it)
                                    methodInfo = this
                                }
                                parameters?.let { removeTab(it) }
                                tab("Parameters") {
                                    div("todo")
                                    parameters = this
                                }
                                localVariables?.let { removeTab(it) }
                                tab("Local Variables") {
                                    div("todo")
                                    localVariables = this
                                }
                            }
                        }
                    }

                    var paramInfo: Tab? = null
                    selectedParamOrLocal.subscribe {
                        if (it != null) {
                            leftTab.apply {
                                val varName = if (it is MethodNode.ParameterNode) "Parameter" else "Local"
                                paramInfo?.let { removeTab(it) }
                                paramInfo = tab("$varName Info") {
                                    InfoViewer(it)
                                }
                            }
                        }
                    }
                }
            }
        }

    }


}