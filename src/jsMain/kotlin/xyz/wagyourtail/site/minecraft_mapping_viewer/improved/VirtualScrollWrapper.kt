package xyz.wagyourtail.site.minecraft_mapping_viewer.improved

import io.kvision.core.*
import io.kvision.panel.SimplePanel
import io.kvision.utils.perc

class VirtualScrollWrapper<T: SimplePanel>(
    private val scrollElement: SimplePanel,
    private val insertElement: T,
    private val above: SimplePanel,
    private val below: SimplePanel,
    private val intrinsicRowHeight: Float,
    private val totalRows: Int,
    private val rowSupplier: T.(Int) -> Container
) {

    val rowHeights = FloatArray(totalRows) { intrinsicRowHeight }

    var firstIndex = 0
        private set

    init {
        scrollElement.height = 100.perc
        scrollElement.overflow = Overflow.AUTO
    }

    private fun updateContent(scrollHeight: Float, visibleHeight: Float) {
        insertElement.singleRender {
            insertElement.removeAll()
            above.height = CssSize(scrollHeight, UNIT.px)
            insertElement.add(above)
            var h = 0f
            var i0 = totalRows
            for (i in firstIndex until totalRows) {
                val row = insertElement.rowSupplier(i)
                row.addAfterInsertHook {
                    row.getElement()?.let {
                        rowHeights[i] = it.getBoundingClientRect().height.toFloat()
                    }
                }
                insertElement.add(row)
                h += rowHeights[i]
                if (h >= visibleHeight * 2) {
                    i0 = i
                    break
                }
            }
            // calculate below height
            var bh = 0f
            for (i in i0 until totalRows) {
                bh += rowHeights[i]
            }
            below.height = CssSize(bh, UNIT.px)
            insertElement.add(below)
        }
    }

    init {
        scrollElement.onEvent {
            scroll = {
                // calculate the first visible row with real row heights
                scrollElement.getElement()?.apply {
                    var i = 0
                    var h = 0f
                    while (i < totalRows && h + rowHeights[i] < scrollTop) {
                        h += rowHeights[i]
                        i++
                    }
                    firstIndex = i
                    updateContent(h, offsetHeight.toFloat())
                }
            }
        }

        updateContent(0f, 100f)
    }

}