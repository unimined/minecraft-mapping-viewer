package xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.babric

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.*
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

object BabricIntermediaryProvider : MappingPatchProvider("babric-intermediary") {
    val LOGGER = KotlinLogging.logger {  }

    override val srcNs: String = "official"
    override val dstNs: List<String> = listOf("babric-intermediary")

    override fun availableVersions(mcVersion: String, env: EnvType): List<String>? {
        if (mcVersion == "b1.7.3") return null
        return emptyList()
    }

    override fun getDataVersion(mcVersion: String, env: EnvType, version: String?, into: MappingResolverImpl) {
        if (mcVersion != "b1.7.3") throw IllegalArgumentException("Invalid mcVersion")
        if (version != null) throw IllegalArgumentException("Invalid version")

        LOGGER.info { "Getting babric-intermediary for $mcVersion" }

        val cacheFile = CACHE_DIR.resolve("providers/babric/intermediary/$mcVersion/intermediary-$mcVersion-v2.jar")

        if (!cacheFile.exists()) {
            measureTime {
                runBlocking {
                    val resp =
                        MMV_HTTP_CLIENT.get("https://maven.glass-launcher.net/babric/babric/intermediary/$mcVersion/intermediary-$mcVersion-v2.jar")
                    if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get version info")
                    // save to cache file
                    cacheFile.createParentDirectories()
                    cacheFile.writeBytes(resp.bodyAsChannel().toByteArray())
                }
            }.also { LOGGER.info { "Downloaded babric-intermediary for $mcVersion in $it" } }
        } else {
            LOGGER.info { "Using cached babric-intermediary for $mcVersion" }
        }

        // read cache file
        into.addDependency("babric-intermediary", into.MappingEntry(
            ContentProvider.of(
                "intermediary-$mcVersion-v2.jar",
                cacheFile.inputStream().source().buffer()
            ),
            "babric-intermediary"
        ).apply {
            provides("babric-intermediary" to false)
            mapNamespace("intermediary" to "babric-intermediary")

            when (env) {
                EnvType.CLIENT -> {
                    mapNamespace("client", "official")
                    mapNamespace("clientOfficial", "official")
                }
                EnvType.SERVER -> {
                    mapNamespace("server", "official")
                    mapNamespace("serverOfficial", "official")
                }
                else -> {}
            }
        })
    }

}