package xyz.wagyourtail.site.minecraft_mapping_viewer.util

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

class ExpiringDelegate<T>(private val expireAfter: Duration = 1.days, private val refCreator: () -> T) : ReadOnlyProperty<Any?, T> {

    private var lastUpdate = 0L
    private var value: T? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (value == null || lastUpdate + expireAfter.inWholeMilliseconds < System.currentTimeMillis()) {
            value = refCreator()
            lastUpdate = System.currentTimeMillis()
        }
        return value!!
    }

}