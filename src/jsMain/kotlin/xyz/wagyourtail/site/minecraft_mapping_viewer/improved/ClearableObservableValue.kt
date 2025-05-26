package xyz.wagyourtail.site.minecraft_mapping_viewer.improved

import io.kvision.state.ObservableValue

open class ClearableObservableValue<T>(initial: T) : ObservableValue<T>(initial) {

    open fun clearSubscribers() {
        observers.clear()
    }

}