package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.github.oshai.kotlinlogging.Formatter
import io.github.oshai.kotlinlogging.KLoggingEvent
import kotlin.js.Date

class MessageFormatterOverride(private val includePrefix: Boolean = true) : Formatter {

    override fun formatMessage(loggingEvent: KLoggingEvent): String {
        return "[${Date().toLocaleTimeString()}] [${loggingEvent.level.name}] [${loggingEvent.loggerName}] ${loggingEvent.message}"
    }

}