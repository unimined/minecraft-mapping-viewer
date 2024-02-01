package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.ktor.client.*
import okio.BufferedSource

abstract class MappingSource {

    abstract suspend fun getMappings(mcVersion: String, httpClient: HttpClient): Triple<String, BufferedSource, List<String>>?

}
