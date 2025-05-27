package xyz.wagyourtail.site.minecraft_mapping_viewer.storage

import xyz.wagyourtail.site.minecraft_mapping_viewer.improved.ClearableObservableValue

class RetainedObservableValue<T> @PublishedApi internal constructor(initial: T, val writer: (T) -> Unit) : ClearableObservableValue<T>(initial) {

    init {
        subscribe(writer)
    }

    companion object {

        inline fun <reified T> fromLocalStorage(key: String, initialValue: () -> T) =
            RetainedObservableValue(readStorageValue(key) ?: initialValue()) { writeStorageValue(key, it) }

        inline fun <reified T> fromParams(key: String, initialValue: () -> T) =
            RetainedObservableValue(readParamValue(key) ?: initialValue()) { writeParamValue(key, it) }

    }


    override fun clearSubscribers() {
        super.clearSubscribers()
        subscribe(writer)
    }

}