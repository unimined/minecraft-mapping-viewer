package xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.constants

import io.kvision.core.Overflow
import io.kvision.html.Div
import io.kvision.html.div
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
import xyz.wagyourtail.unimined.mapping.Namespace
import xyz.wagyourtail.unimined.mapping.tree.node._constant.ConstantGroupNode
import xyz.wagyourtail.unimined.mapping.tree.node._constant.ConstantNode
import xyz.wagyourtail.unimined.mapping.tree.node._constant.TargetNode

class ConstantContentViewer(val namespaces: List<Namespace>, constantGroupNode: ConstantGroupNode) : Div() {

    init {
        height = 100.perc
    }

    val mobileFlag = window.isMobile()

    val constantList = BetterTable(className = "constant-table").apply {
        if (!this@ConstantContentViewer.mobileFlag) {
            setStyle("word-break", "break-word")
        }
    }

    val targetList = BetterTable(className = "target-table").apply {
        if (!this@ConstantContentViewer.mobileFlag) {
            setStyle("word-break", "break-word")
        }
    }

    val hasConstants = constantGroupNode.constants.isNotEmpty()
    val hasTargets = constantGroupNode.targets.isNotEmpty()

    val leftTabs = BetterTabPanel(tabPosition = TabPosition.LEFT) {
        height = 100.perc
    }

    val rightTabs by lazy {
        if (mobileFlag) leftTabs
        else BetterTabPanel(tabPosition = TabPosition.RIGHT) {
            height = 100.perc
        }
    }

    val selectedConstant = ObservableValue<ConstantNode?>(null)
    val selectedTarget = ObservableValue<TargetNode?>(null)

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

    fun selectConstant(constant: ConstantNode?) {
        singleRender {
            val activeTabName = rightTabs.activeTab?.label
            rightTabs.getTabs().withIndex().reversed().forEach { (i, it) ->
                if (it.label == "Constant Info") {
                    rightTabs.removeTab(i)
                }
            }
            if (constant != null) {
                rightTabs.tab("Constant Info") {
                    add(InfoViewer(this@ConstantContentViewer.namespaces, constant))
                }
            }
            selectedConstant.setState(constant)
            rightTabs.activeTab = rightTabs.getTabs().firstOrNull { it.label == activeTabName } ?: rightTabs.activeTab
        }
    }

    fun selectTarget(target: TargetNode?) {
        singleRender {
            val activeTabName = leftTabs.activeTab?.label
            leftTabs.getTabs().withIndex().reversed().forEach { (i, it) ->
                if (it.label == "Target Info") {
                    leftTabs.removeTab(i)
                }
            }
            if (target != null) {
                leftTabs.tab("Target Info") {
                    add(InfoViewer(this@ConstantContentViewer.namespaces, target))
                }
            }
            selectedTarget.setState(target)
            leftTabs.activeTab = leftTabs.getTabs().firstOrNull { it.label == activeTabName } ?: leftTabs.activeTab
        }
    }

    init {

        selectedConstant.subscribe {
            updateContent()
        }

        selectedTarget.subscribe {
            updateContent()
        }


        targetList.head.row {
            header(constantGroupNode.baseNs.name)
            header("Parameter")
        }

        val targetBody = targetList.body {}
        for (target in constantGroupNode.targets) {
            targetBody.row(data = target) {
                cell(target.target?.value ?: "-")
                cell(target.paramIdx?.toString() ?: "-")
            }
        }
        targetList.activeRow.subscribe {
            if (it is TargetNode?) {
                selectTarget(it)
            }
        }

        constantList.head.row {
            header("Class")
            header("Field")
            header("Type")
        }

        val constantBody = constantList.body {}
        for (constant in constantGroupNode.constants) {
            constantBody.row(data = constant) {
                cell(constant.constClass.value)
                cell(constant.constName.value)
                cell(constant.fieldDesc?.toString() ?: "-")
            }
        }
        constantList.activeRow.subscribe {
            if (it is ConstantNode?) {
                selectConstant(it)
            }
        }

        if (hasTargets) {
            rightTabs.tab("Targets") {
                div {
                    height = 100.perc
                    overflow = Overflow.AUTO

                    add(this@ConstantContentViewer.targetList)
                }
            }
        }

        if (hasConstants) {
            leftTabs.tab("Constants") {
                div {
                    height = 100.perc
                    overflow = Overflow.AUTO

                    add(this@ConstantContentViewer.constantList)
                }
            }
        }

        updateContent()
    }

}