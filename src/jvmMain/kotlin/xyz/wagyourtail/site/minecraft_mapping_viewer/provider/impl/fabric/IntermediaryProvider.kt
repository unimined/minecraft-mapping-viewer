package xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.fabric

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

object IntermediaryProvider : MappingPatchProvider("intermediary") {
    val LOGGER = KotlinLogging.logger {  }

    override val srcNs: String = "official"
    override val dstNs: List<String> = listOf("intermediary")

    val availableVersions by ExpiringDelegate {
        runBlocking {
            val resp = MMV_HTTP_CLIENT.get("https://meta.fabricmc.net/v2/versions/intermediary")
            if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get versions")
            Json.parseToJsonElement(resp.bodyAsText()).jsonArray.map { it.jsonObject["version"]!!.jsonPrimitive.content }
        }
    }

    override fun availableVersions(mcVersion: String, env: EnvType): List<String>? {
        if (env != EnvType.JOINED) return emptyList()
        return if (availableVersions.contains(mcVersion)) null else emptyList()
    }

    override fun getDataVersion(mcVersion: String, env: EnvType, version: String?, into: MappingResolverImpl) {
        if (!availableVersions.contains(mcVersion)) throw IllegalArgumentException("Invalid mcVersion")
        if (version != null) throw IllegalArgumentException("Invalid version")

        LOGGER.info { "Getting intermediary for $mcVersion" }

        val cacheFile = CACHE_DIR.resolve("providers/net/fabricmc/intermediary/$mcVersion/intermediary-$mcVersion-v2.jar")

        if (!cacheFile.exists()) {
            measureTime {
                runBlocking {
                    val resp =
                        MMV_HTTP_CLIENT.get("https://maven.fabricmc.net/net/fabricmc/intermediary/$mcVersion/intermediary-$mcVersion-v2.jar")
                    if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get version info")
                    // save to cache file
                    cacheFile.createParentDirectories()
                    cacheFile.writeBytes(resp.bodyAsChannel().toByteArray())
                }
            }.also { LOGGER.info { "Downloaded intermediary for $mcVersion in $it" } }
        } else {
            LOGGER.info { "Using cached intermediary for $mcVersion" }
        }

        // read cache file
        into.addDependency("intermediary", into.MappingEntry(
            ContentProvider.of(
                "intermediary-$mcVersion-v2.jar",
                cacheFile.inputStream().source().buffer()
            ),
            "intermediary"
        ).apply {
            provides("intermediary" to false)
        })
    }

}