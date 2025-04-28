package xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.ornithe

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
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
import xyz.wagyourtail.unimined.mapping.EnvType
import xyz.wagyourtail.unimined.mapping.resolver.ContentProvider
import xyz.wagyourtail.unimined.mapping.visitor.fixes.renest
import java.util.concurrent.TimeUnit
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.writeBytes
import kotlin.time.measureTime

object FeatherProvider : MappingPatchProvider("feather") {

    val LOGGER = KotlinLogging.logger {  }

    override val requires: List<Pair<MappingPatchProvider, (String, String?) -> String?>> = listOf(
        CalamusProvider to { mcVersion, version ->
            version?.split("-", limit = 2)?.get(0)
        }
    )

    override val srcNs: String = "calamus"
    override val dstNs: List<String> = listOf("feather")

    val availableVersions = Caffeine.newBuilder()
        .expireAfterWrite(6, TimeUnit.HOURS)
        .softValues()
        .build<Pair<AvailableVersions, String>, List<String>> {
            runBlocking {
                val resp = MMV_HTTP_CLIENT.get("https://meta.ornithemc.net/v3/versions/${it.first}/feather/${it.second}")
                if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get ${it.first} versions")
                Json.parseToJsonElement(resp.bodyAsText()).jsonArray.map { it.jsonObject["build"]!!.jsonPrimitive.content }
            }
        }

    override fun availableVersions(mcVersion: String, env: EnvType): List<String> {
        return buildList {
            for (version in AvailableVersions.entries.reversed()) {
                val realMcVersion = version.resolve(mcVersion, env) ?: continue
                val versions = availableVersions.get(version to realMcVersion) ?: continue
                addAll(versions.map { "$version-$it" })
            }
        }
    }

    override fun getDataVersion(mcVersion: String, env: EnvType, version: String?, into: MappingResolverImpl) {
        val (gen, vers) = version?.split("-", limit = 2) ?: throw IllegalArgumentException("Invalid version")
        val genVal = AvailableVersions.byName.getValue(gen)
        val realMcVersion = genVal.resolve(mcVersion, env) ?: throw IllegalArgumentException("Invalid mcVersion")
        val versions = availableVersions.get(genVal to realMcVersion) ?: throw IllegalArgumentException("Invalid mcVersion")
        if (!versions.contains(vers)) throw IllegalArgumentException("Invalid version")

        val mappingName = if (genVal == AvailableVersions.GEN_1) "feather" else "feather-$genVal"

        LOGGER.info { "Getting $mappingName for $realMcVersion+build.$version" }
        val cacheFile = CACHE_DIR.resolve("providers/net/ornithemc/$mappingName/$realMcVersion+build.${vers}/$mappingName-$realMcVersion+build.${vers}-v2.jar")

        if (!cacheFile.exists()) {
            measureTime {
                runBlocking {
                    val resp =
                        MMV_HTTP_CLIENT.get("https://maven.ornithemc.net/releases/net/ornithemc/$mappingName/$realMcVersion+build.${vers}/$mappingName-$realMcVersion+build.${vers}-v2.jar")
                    if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get version info")
                    // save to cache file
                    cacheFile.createParentDirectories()
                    cacheFile.writeBytes(resp.bodyAsChannel().toByteArray())
                }
            }.apply { LOGGER.info { "Downloaded $mappingName for $realMcVersion+build.$vers in $this" } }
        } else {
            LOGGER.info { "Using cached $mappingName for $realMcVersion+build.$vers" }
        }

        // read cache file
        into.addDependency("feather", into.MappingEntry(
            ContentProvider.of(
                "$mappingName-$realMcVersion+build.${vers}-v2.jar",
                cacheFile.inputStream().source().buffer()
            ),
            "feather"
        ).apply {

            requires("calamus")
            provides("feather" to true)

            mapNamespace(
                "intermediary" to "calamus",
                "named" to "feather",
                "source" to "feather",
            )

            renest()

        })
    }

}