package xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.classes

import io.kvision.core.Overflow
import io.kvision.core.onClick
import io.kvision.html.div
import io.kvision.panel.*
import io.kvision.state.ObservableValue
import io.kvision.utils.perc
import kotlinx.browser.window
import xyz.wagyourtail.site.minecraft_mapping_viewer.improved.BetterTable
import xyz.wagyourtail.site.minecraft_mapping_viewer.isMobile
import xyz.wagyourtail.site.minecraft_mapping_viewer.improved.BetterTabPanel
import xyz.wagyourtail.site.minecraft_mapping_viewer.improved.FasterSplitPanel
import xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.ClassViewer
import xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.info.InfoViewer
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
        setStyle("word-break", "break-word")
        height = 100.perc
    }

    val fieldList = BetterTable(className = "field-table").apply {
        setStyle("word-break", "break-word")
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
                        removeAll()
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

                    selectedField.subscribe { fieldNode ->
                        leftTab.apply {
                            getTabs().withIndex().reversed().forEach { (i, it) ->
                                if (it.label == "Field Info") {
                                    removeTab(i)
                                }
                            }

                            if (fieldNode != null) {
                                if (leftTab.getTabs().isEmpty()) {
                                    addBoth()
                                }

                                tab("Field Info") {
                                    InfoViewer(fieldNode)
                                }
                            }
                        }
                    }

                    selectedMethod.subscribe { methodNode ->
                        selectedParamOrLocal.setState(null)
                        rightTab.apply {

                            getTabs().withIndex().reversed().forEach { (i, it) ->
                                if (it.label == "Method Info") {
                                    removeTab(i)
                                }
                                if (it.label == "Parameters") {
                                    removeTab(i)
                                }
                                if (it.label == "Local Variables") {
                                    removeTab(i)
                                }
                            }

                            if (methodNode != null) {
                                if (rightTab.getTabs().isEmpty()) {
                                    addBoth()
                                }

                                tab("Method Info") {
                                    InfoViewer(methodNode)
                                }
                                tab("Parameters") {
                                    div("todo")
                                }
                                tab("Local Variables") {
                                    div("todo")
                                }
                            }
                        }
                    }

                    selectedParamOrLocal.subscribe { memberNode ->
                        leftTab.apply {
                            getTabs().withIndex().reversed().forEach { (i, it) ->
                                if (it.label == "Parameter Info") {
                                    removeTab(i)
                                }
                                if (it.label == "Local Info") {
                                    removeTab(i)
                                }
                            }

                            if (memberNode != null) {
                                val varName = if (memberNode is MethodNode.ParameterNode) "Parameter" else "Local"

                                tab("$varName Info") {
                                    InfoViewer(memberNode)
                                }
                            }
                        }
                    }
                }
            }
        }

    }


}