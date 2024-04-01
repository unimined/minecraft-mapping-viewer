package xyz.wagyourtail.site.minecraft_mapping_viewer.improved

import io.kvision.state.ObservableValue

class ClearableObservableValue<T>(initial: T) : ObservableValue<T>(initial) {

    fun clearSubscribers() {
        observers.clear()
    }

}