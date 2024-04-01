package xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.mojmap

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okio.buffer
import okio.source
import xyz.wagyourtail.site.minecraft_mapping_viewer.CACHE_DIR
import xyz.wagyourtail.site.minecraft_mapping_viewer.MMV_HTTP_CLIENT
import xyz.wagyourtail.site.minecraft_mapping_viewer.provider.MappingPatchProvider
import xyz.wagyourtail.site.minecraft_mapping_viewer.util.ExpiringDelegate
import xyz.wagyourtail.unimined.mapping.EnvType
import xyz.wagyourtail.unimined.mapping.resolver.ContentProvider
import xyz.wagyourtail.unimined.mapping.resolver.MappingResolver
import xyz.wagyourtail.unimined.mapping.util.defaultedMapOf
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.*
import kotlin.time.measureTime

object ParchmentProvider : MappingPatchProvider("parchment") {
    val LOGGER = KotlinLogging.logger {  }

    override val requires: List<Pair<MappingPatchProvider, (String, String?) -> String?>> = listOf(
        MojmapProvider to { _, _ -> null }
    )
    override val srcNs: String = "mojmap"
    override val dstNs: List<String> = listOf("mojmap")

    val availableMcVersions by ExpiringDelegate {
        runBlocking {
            val resp = MMV_HTTP_CLIENT.post("https://ldtteam.jfrog.io/ui/api/v1/ui/treebrowser?compacted=false") {
                contentType(ContentType.Application.Json)
                headers {
                    append("X-Requested-With", "XMLHttpRequest")
                }
                setBody(buildJsonObject {
                    put("projectKey", "")
                    put("repoType", "virtual")
                    put("repoKey", "parchmentmc-public")
                    put("path", "org/parchmentmc/data")
                    put("text", "data")
                    put("trashcan", false)
                    put("type", "junction")
                }.toString())
            }
            if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get versions: " + resp.bodyAsText())
            Json.parseToJsonElement(resp.bodyAsText()).jsonObject["data"]?.jsonArray?.mapNotNull {
                val text = it.jsonObject["text"]?.jsonPrimitive?.content ?: error("Invalid response from parchment")
                if (text.startsWith("parchment-")) text.substring(10) else null
            } ?: error("Invalid response from parchment")
        }
    }

    val availableVersions = defaultedMapOf<String, List<String>> { mcVersion ->
        runBlocking {
            val resp = MMV_HTTP_CLIENT.get("https://maven.parchmentmc.org/org/parchmentmc/data/parchment-${mcVersion}/maven-metadata.xml")
            if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get versions for $mcVersion")
            val xmlText = resp.bodyAsText()
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlText.byteInputStream())
            val items = document.getElementsByTagName("versions").item(0)?.childNodes ?: error("Invalid response from parchment")
            val versions = mutableListOf<String>()
            for (i in 0 until items.length) {
                val item = items.item(i)
                if (item.nodeName != "version") continue
                versions.add(item.textContent)
            }
            versions.reversed()
        }
    }

    override fun availableVersions(mcVersion: String, env: EnvType): List<String>? {
        return if (availableMcVersions.contains(mcVersion)) availableVersions[mcVersion] else emptyList()
    }

    override fun getDataVersion(mcVersion: String, env: EnvType, version: String?, into: MappingResolver) {
        if (!availableMcVersions.contains(mcVersion)) throw IllegalArgumentException("Invalid mcVersion")
        if (!availableVersions[mcVersion].contains(version)) throw IllegalArgumentException("Invalid version")

        LOGGER.info { "Getting parchment for $mcVersion/$version" }

        val cacheFile = CACHE_DIR.resolve("providers/org/parchmentmc/data/parchment-${mcVersion}/${version}/parchment-${mcVersion}-${version}.zip")

        // file exists, or bleeding is more than a day old
        if (!cacheFile.exists() || (version == "BLEEDING-SNAPSHOT" && cacheFile.getLastModifiedTime().toMillis() < System.currentTimeMillis() - 1000 * 60 * 60 * 24)) {
            measureTime {
                runBlocking {
                    val resp = MMV_HTTP_CLIENT.get("https://maven.parchmentmc.org/org/parchmentmc/data/parchment-${mcVersion}/${version}/parchment-${mcVersion}-${version}.zip")
                    if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get version info")
                    // save to cache file
                    cacheFile.createParentDirectories()
                    cacheFile.writeBytes(resp.bodyAsChannel().toByteArray())
                }
            }.also { LOGGER.info { "Downloaded parchment for $mcVersion in $it" } }
        } else {
            LOGGER.info { "Using cached parchment for $mcVersion" }
        }

        // read cache file
        into.addDependency("parchment", into.MappingEntry(
            ContentProvider.of(
                "parchment-${mcVersion}-${version}.zip",
                cacheFile.inputStream().source().buffer()
            )
        ).apply {
            mapNamespace("source", "mojmap")
            requires("mojmap")
            provides("mojmap" to true)
        })
    }

}