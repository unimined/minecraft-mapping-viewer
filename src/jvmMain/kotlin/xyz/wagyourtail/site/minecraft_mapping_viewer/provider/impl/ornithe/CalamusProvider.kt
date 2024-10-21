package xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.ornithe

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.buffer
import okio.source
import xyz.wagyourtail.site.minecraft_mapping_viewer.CACHE_DIR
import xyz.wagyourtail.site.minecraft_mapping_viewer.MMV_HTTP_CLIENT
import xyz.wagyourtail.site.minecraft_mapping_viewer.provider.MappingPatchProvider
import xyz.wagyourtail.site.minecraft_mapping_viewer.resolver.MappingResolverImpl
import xyz.wagyourtail.site.minecraft_mapping_viewer.util.ExpiringDelegate
import xyz.wagyourtail.unimined.mapping.EnvType
import xyz.wagyourtail.unimined.mapping.resolver.ContentProvider
import xyz.wagyourtail.unimined.mapping.resolver.MappingResolver
import kotlin.io.path.*
import kotlin.time.measureTime

object CalamusProvider : MappingPatchProvider("calamus") {
    val LOGGER = KotlinLogging.logger {  }

    override val srcNs: String = "official"
    override val dstNs: List<String> = listOf("calamus")

    val availableVersions by ExpiringDelegate {
        runBlocking {
            val resp = MMV_HTTP_CLIENT.get("https://meta.ornithemc.net/v3/versions/intermediary")
            if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get versions")
            Json.parseToJsonElement(resp.bodyAsText()).jsonArray.map { it.jsonObject["version"]!!.jsonPrimitive.content }
        }
    }

    override fun availableVersions(mcVersion: String, env: EnvType): List<String>? {
        return if (availableVersions.contains(mcVersion) || availableVersions.contains("$mcVersion-${env.name.lowercase()}")) null else emptyList()
    }

    override fun getDataVersion(mcVersion: String, env: EnvType, version: String?, into: MappingResolverImpl) {
        if (version != null) throw IllegalArgumentException("Invalid version")
        val realMcVersion = availableVersions.firstOrNull { it == mcVersion } ?: availableVersions.firstOrNull { it == "$mcVersion-${env.name.lowercase()}" } ?: throw IllegalArgumentException("Invalid mcVersion")

        LOGGER.info { "Getting calamus for $realMcVersion" }

        val cacheFile = CACHE_DIR.resolve("providers/net/ornithemc/calamus-intermediary/$realMcVersion/calamus-intermediary-$realMcVersion-v2.jar")

        if (!cacheFile.exists()) {
            measureTime {
                runBlocking {
                    val resp =
                        MMV_HTTP_CLIENT.get("https://maven.ornithemc.net/releases/net/ornithemc/calamus-intermediary/$realMcVersion/calamus-intermediary-$realMcVersion-v2.jar")
                    if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get version info")
                    // save to cache file
                    cacheFile.createParentDirectories()
                    cacheFile.writeBytes(resp.bodyAsChannel().toByteArray())
                }
            }.also { LOGGER.info { "Downloaded calamus for $realMcVersion in $it" } }
        } else {
            LOGGER.info { "Using cached calamus for $realMcVersion" }
        }

        // read cache file
        into.addDependency("calamus", into.MappingEntry(
            ContentProvider.of(
                "calamus-intermediary-$realMcVersion-v2.jar",
                cacheFile.inputStream().source().buffer()
            ),
            "calamus"
        ).apply {
            provides("calamus" to false)

            mapNamespace("intermediary" to "calamus")
        })
    }

}