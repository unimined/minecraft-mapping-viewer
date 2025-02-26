package xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.ornithe

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
import kotlin.io.path.*
import kotlin.time.measureTime


enum class AvailableVersions(val creator: () -> List<String>) {
    GEN_1({
        runBlocking {
            val resp = MMV_HTTP_CLIENT.get("https://meta.ornithemc.net/v3/versions/gen1/intermediary")
            if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get versions")
            Json.parseToJsonElement(resp.bodyAsText()).jsonArray.map { it.jsonObject["version"]!!.jsonPrimitive.content }
        }
    }),
    GEN_2({
        runBlocking {
            val resp = MMV_HTTP_CLIENT.get("https://meta.ornithemc.net/v3/versions/gen2/intermediary")
            if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get versions")
            Json.parseToJsonElement(resp.bodyAsText()).jsonArray.map { it.jsonObject["version"]!!.jsonPrimitive.content }
        }
    });

    companion object {
        val byName = entries.associateBy { it.toString() }
    }

    override fun toString() = super.name.lowercase().replace("_", "")

    val versions: List<String> by ExpiringDelegate(refCreator = creator)

    fun resolve(mcVersion: String, env: EnvType): String? {
        return versions.find { it == mcVersion || it == "$mcVersion-${env.name.lowercase()}" }
    }
}

object CalamusProvider : MappingPatchProvider("calamus") {
    val LOGGER = KotlinLogging.logger {  }

    override val srcNs: String = "official"
    override val dstNs: List<String> = listOf("calamus")

    override fun availableVersions(mcVersion: String, env: EnvType): List<String> {
        return buildList {
            for (version in AvailableVersions.entries.reversed()) {
                if (version.resolve(mcVersion, env) != null) add(version.toString())
            }
        }
    }

    override fun getDataVersion(mcVersion: String, env: EnvType, version: String?, into: MappingResolverImpl) {
        if (version == null) throw IllegalArgumentException("Invalid version")
        if (version !in availableVersions(mcVersion, env)) throw IllegalArgumentException("Invalid version")

        val mappingName = if (version == AvailableVersions.GEN_1.toString()) "calamus-intermediary" else "calamus-intermediary-${version}"

        val availableVersions = AvailableVersions.byName.getValue(version)
        val realMcVersion = availableVersions.resolve(mcVersion, env) ?: throw IllegalArgumentException("Invalid mcVersion")

        LOGGER.info { "Getting calamus for $realMcVersion" }

        val cacheFile = CACHE_DIR.resolve("providers/net/ornithemc/$mappingName/$realMcVersion/$mappingName-$realMcVersion-v2.jar")

        if (!cacheFile.exists()) {
            measureTime {
                runBlocking {
                    val resp =
                        MMV_HTTP_CLIENT.get("https://maven.ornithemc.net/releases/net/ornithemc/$mappingName/$realMcVersion/$mappingName-$realMcVersion-v2.jar")
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
                "$mappingName-$realMcVersion-v2.jar",
                cacheFile.inputStream().source().buffer()
            ),
            "calamus"
        ).apply {
            provides("calamus" to false)

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

            mapNamespace("intermediary" to "calamus")
        })
    }

}