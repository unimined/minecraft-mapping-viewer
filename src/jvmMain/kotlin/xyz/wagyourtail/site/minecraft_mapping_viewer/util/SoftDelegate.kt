package xyz.wagyourtail.site.minecraft_mapping_viewer.util

import java.lang.ref.SoftReference
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class SoftDelegate<T>(private val refCreator: () -> T) : ReadOnlyProperty<Any?, T> {

    private var soft = SoftReference<T>(null)

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return soft.get() ?: refCreator().also {
            soft = SoftReference(it)
        }
    }

}