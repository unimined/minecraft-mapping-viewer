package xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.classes

import io.kvision.core.Overflow
import io.kvision.core.onClick
import io.kvision.html.Div
import io.kvision.html.div
import io.kvision.html.header
import io.kvision.panel.Direction
import io.kvision.panel.TabPosition
import io.kvision.panel.tab
import io.kvision.state.ObservableValue
import io.kvision.utils.perc
import kotlinx.browser.window
import xyz.wagyourtail.site.minecraft_mapping_viewer.improved.BetterTabPanel
import xyz.wagyourtail.site.minecraft_mapping_viewer.improved.BetterTable
import xyz.wagyourtail.site.minecraft_mapping_viewer.improved.FasterSplitPanel
import xyz.wagyourtail.site.minecraft_mapping_viewer.isMobile
import xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.info.InfoViewer
import xyz.wagyourtail.unimined.mapping.jvms.ext.NameAndDescriptor
import xyz.wagyourtail.unimined.mapping.jvms.four.two.two.UnqualifiedName
import xyz.wagyourtail.unimined.mapping.tree.node.ClassNode
import xyz.wagyourtail.unimined.mapping.tree.node.FieldNode
import xyz.wagyourtail.unimined.mapping.tree.node.MethodNode

class ClassContentViewer(classNode: ClassNode) : Div() {

    init {
        height = 100.perc
    }

    val methodList = BetterTable(className = "method-table").apply {
        setStyle("word-break", "break-word")
    }

    val fieldList = BetterTable(className = "field-table").apply {
        setStyle("word-break", "break-word")
    }

    val hasFields = classNode.fields.resolve().isNotEmpty()
    val hasMethods = classNode.methods.resolve().isNotEmpty()

    val mobileFlag = window.isMobile()

    val leftTabs = BetterTabPanel(tabPosition = TabPosition.LEFT) {
        height = 100.perc
    }

    val rightTabs by lazy {
        if (mobileFlag) leftTabs
        else BetterTabPanel(tabPosition = TabPosition.RIGHT) {
            height = 100.perc
        }
    }

    val selectedField = ObservableValue<FieldNode?>(null)
    val selectedMethod = ObservableValue(MethodData(null))

    fun updateContent() {
        singleRender {
            removeAll()
            if (leftTabs.getTabs().isNotEmpty() && rightTabs.getTabs().isNotEmpty() && !mobileFlag) {
                add(FasterSplitPanel(direction = Direction.VERTICAL).apply {
                    height = 100.perc

                    add(leftTabs)
                    add(rightTabs)
                })
            } else if (leftTabs.getTabs().isNotEmpty()) {
                add(leftTabs)
            } else if (rightTabs.getTabs().isNotEmpty()) {
                add(rightTabs)
            } else {
                add(Div("No content"))
            }
        }
    }

    fun selectField(field: FieldNode?) {
        singleRender {
            val activeTabName = leftTabs.activeTab?.label
            leftTabs.getTabs().withIndex().reversed().forEach { (i, it) ->
                if (it.label == "Field Info") {
                    leftTabs.removeTab(i)
                }
            }
            if (field != null) {
            leftTabs.tab("Field Info") {
                add(InfoViewer(field))
            }
                }
            selectedField.setState(field)
            leftTabs.activeTab = leftTabs.getTabs().firstOrNull { it.label == activeTabName } ?: leftTabs.activeTab
        }
    }

    fun selectMethod(method: MethodNode?) {
        singleRender {
            val activeTabName = rightTabs.activeTab?.label
            rightTabs.getTabs().withIndex().reversed().forEach { (i, it) ->
                if (it.label == "Method Info") {
                    rightTabs.removeTab(i)
                }
            }
            if (method != null) {
                rightTabs.tab("Method Info") {
                    add(InfoViewer(method))
                }
            }
            selectedMethod.setState(MethodData(method))
            rightTabs.activeTab = rightTabs.getTabs().firstOrNull { it.label == activeTabName } ?: rightTabs.activeTab
        }
    }

    init {
        selectedMethod.subscribe {
            updateContent()
        }

        selectedField.subscribe {
            updateContent()
        }

        val classNode = classNode
        fieldList.head.row {
            for (ns in classNode.root.namespaces) {
                header(ns.name)
            }
        }


        val fieldBody = fieldList.body {}
        for (field in classNode.fields.resolve()) {
            fieldBody.row(data = field) {
                for (name in classNode.root.namespaces) {
                    cell(field.getName(name)?.let { NameAndDescriptor(UnqualifiedName.unchecked(it), field.getDescriptor(name)).value } ?: "-")
                }
            }
        }
        fieldList.activeRow.subscribe {
            selectField(it as FieldNode?)
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
            selectMethod(it as MethodNode?)
        }

        if (hasFields) {
            rightTabs.tab("Fields") {
                div {
                    height = 100.perc
                    overflow = Overflow.AUTO

                    add(this@ClassContentViewer.fieldList)
                }
            }
        }

        if (hasMethods) {
            leftTabs.tab("Methods") {
                div {
                    height = 100.perc
                    overflow = Overflow.AUTO

                    add(this@ClassContentViewer.methodList)
                }
            }
        }

        updateContent()
    }


    inner class MethodData(val method: MethodNode?) {

        val paramList = BetterTable(className = "params-table").apply {
            setStyle("word-break", "break-word")
        }

        val localList = BetterTable(className = "locals-table").apply {
            setStyle("word-break", "break-word")
        }

        val selectedParam = ObservableValue<MethodNode.ParameterNode?>(null)
        val selectedLocal = ObservableValue<MethodNode.LocalNode?>(null)

        val hasParams = method?.params?.isNotEmpty() ?: false
        val hasLocals = method?.locals?.isNotEmpty() ?: false

        fun selectParam(param: MethodNode.ParameterNode?) {
            singleRender {
                val activeTabName = leftTabs.activeTab?.label
                leftTabs.getTabs().withIndex().reversed().forEach { (i, it) ->
                    if (it.label == "Param Info") {
                        leftTabs.removeTab(i)
                    }
                }
                if (param != null) {
                    leftTabs.tab("Param Info") {
                        add(InfoViewer(param))
                    }
                }
                selectedParam.setState(param)
                leftTabs.activeTab = leftTabs.getTabs().firstOrNull { it.label == activeTabName } ?: leftTabs.activeTab
            }
        }

        fun selectLocal(local: MethodNode.LocalNode?) {
            singleRender {
                val activeTabName = rightTabs.activeTab?.label
                leftTabs.getTabs().withIndex().reversed().forEach { (i, it) ->
                    if (it.label == "Local Info") {
                        leftTabs.removeTab(i)
                    }
                }
                if (local != null) {
                    leftTabs.tab("Local Info") {
                        add(InfoViewer(local))
                    }
                }
                selectedLocal.setState(local)
                rightTabs.activeTab = rightTabs.getTabs().firstOrNull { it.label == activeTabName } ?: rightTabs.activeTab
            }
        }

        init {
            selectParam(null)
            selectLocal(null)

            rightTabs.getTabs().withIndex().reversed().forEach { (i, it) ->
                if (it.label == "Parameters") {
                    rightTabs.removeTab(i)
                }
                if (it.label == "Local Vars") {
                    rightTabs.removeTab(i)
                }
            }

            val method = method
            if (method != null && (hasParams || hasLocals)) properInit()
        }

        private fun properInit() {
            val method = method!!

            paramList.head.row {
                header("Index")
                header("LvOrd")
                for (ns in method.root.namespaces) {
                    header(ns.name)
                }
            }

            val paramBody = paramList.body {}
            for (param in method.params) {
                paramBody.row(data = param) {
                    cell(param.index?.toString() ?: "-")
                    cell(param.lvOrd?.toString() ?: "-")
                    for (name in method.root.namespaces) {
                        cell(param.names[name] ?: "-")
                    }
                }
            }
            paramList.activeRow.subscribe {
                selectParam(it as MethodNode.ParameterNode?)
            }

            localList.head.row {
                header("LvOrd")
                header("StartOp")
                for (ns in method.root.namespaces) {
                    header(ns.name)
                }
            }
            val localBody = localList.body {}
            for (local in method.locals) {
                localBody.row(data = local) {
                    cell(local.lvOrd?.toString() ?: "-")
                    cell(local.startOp?.toString() ?: "-")
                    for (name in method.root.namespaces) {
                        cell(local.names[name] ?: "-")
                    }
                }
            }

            localList.activeRow.subscribe {
                selectLocal(it as MethodNode.LocalNode?)
            }

            if (hasParams) {
                rightTabs.tab("Parameters") {
                    add(paramList)
                }
            }

            if (hasLocals) {
                rightTabs.tab("Local Vars") {
                    add(localList)
                }
            }

            updateContent()
        }

    }

}