package xyz.wagyourtail.site.minecraft_mapping_viewer.provider.impl.mcp

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import okio.buffer
import okio.source
import xyz.wagyourtail.commonskt.utils.mutliAssociate
import xyz.wagyourtail.site.minecraft_mapping_viewer.CACHE_DIR
import xyz.wagyourtail.site.minecraft_mapping_viewer.MMV_HTTP_CLIENT
import xyz.wagyourtail.site.minecraft_mapping_viewer.MappingService
import xyz.wagyourtail.site.minecraft_mapping_viewer.provider.MappingPatchProvider
import xyz.wagyourtail.site.minecraft_mapping_viewer.resolver.MappingResolverImpl
import xyz.wagyourtail.site.minecraft_mapping_viewer.util.ExpiringDelegate
import xyz.wagyourtail.unimined.mapping.EnvType
import xyz.wagyourtail.unimined.mapping.resolver.ContentProvider
import xyz.wagyourtail.unimined.mapping.resolver.MappingResolver
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.*
import kotlin.time.measureTime

object SeargeProvider : MappingPatchProvider("searge") {

    val LOGGER = KotlinLogging.logger {  }

    override val srcNs: String = "official"
    override val dstNs: List<String> = listOf("searge")

    val availableVersions: Map<String, Map<VersionType, List<String>>> by ExpiringDelegate {
        runBlocking {
            val allVersions = mutableMapOf<String, MutableMap<VersionType, MutableList<String>>>()
            val mcp_config = MMV_HTTP_CLIENT.get("https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/maven-metadata.xml")
            if (mcp_config.status != HttpStatusCode.OK) throw Exception("Failed to get versions")
            run {
                val text = mcp_config.bodyAsText()
                val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(text.byteInputStream())
                val items = document.getElementsByTagName("versions").item(0)?.childNodes ?: error("Invalid response from parchment")
                val versions = mutableListOf<String>()
                for (i in 0 until items.length) {
                    val item = items.item(i)
                    if (item.nodeName != "version") continue
                    versions.add(item.textContent)
                }
                versions.mutliAssociate {  vers ->
                    (MappingService.minecraftVersions.versions.firstOrNull { vers.startsWith(it.id) }?.id ?: error("Failed to find mc version for $vers")) to vers
                }.forEach {
                    allVersions.getOrPut(it.key) { mutableMapOf() }.getOrPut(VersionType.MCP_CONFIG) { mutableListOf() } += it.value
                }
            }
            val mcp_srg = MMV_HTTP_CLIENT.get("https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp/maven-metadata.xml")
            if (mcp_srg.status != HttpStatusCode.OK) throw Exception("Failed to get versions")
            run {
                val text = mcp_srg.bodyAsText()
                val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(text.byteInputStream())
                val items = document.getElementsByTagName("versions").item(0)?.childNodes ?: error("Invalid response from parchment")
                val versions = mutableListOf<String>()
                for (i in 0 until items.length) {
                    val item = items.item(i)
                    if (item.nodeName != "version") continue
                    versions.add(item.textContent)
                }
                versions.mutliAssociate {  vers ->
                    (MappingService.minecraftVersions.versions.firstOrNull { vers.startsWith(it.id) }?.id ?: error("Failed to find mc version for $vers")) to vers
                }.forEach {
                    allVersions.getOrPut(it.key) { mutableMapOf() }.getOrPut(VersionType.MCP_SRG) { mutableListOf() } += it.value
                }
            }
            allVersions
        }
    }

    override fun availableVersions(mcVersion: String, env: EnvType): List<String>? {
        if (env != EnvType.JOINED) return emptyList()
        return availableVersions.getOrDefault(mcVersion, emptyMap())
            .flatMap { it.value }
            .map { if (it == mcVersion) it else it.substringAfter("$mcVersion-") }
            .toSet().toList()
    }

    override fun getDataVersion(mcVersion: String, env: EnvType, version: String?, into: MappingResolverImpl) {
        val correctedVersion = if (version == mcVersion) version else "$mcVersion-$version"
        val versions = availableVersions[mcVersion] ?: throw IllegalArgumentException("Invalid mcVersion $mcVersion")
        val versionType = versions.entries.firstOrNull { it.value.contains(correctedVersion) }?.key ?: throw IllegalArgumentException("Invalid version $correctedVersion")

        LOGGER.info { "Getting searge for $mcVersion/$correctedVersion" }
        val cacheFile = CACHE_DIR.resolve("providers/de/oceanlabs/mcp/mcp_config/${mcVersion}/${correctedVersion}/mcp_config-${mcVersion}-${correctedVersion}.zip")

        if (!cacheFile.exists()) {
            measureTime {
                runBlocking {
                    val resp = MMV_HTTP_CLIENT.get(when(versionType) {
                        VersionType.MCP_SRG -> {
                            "https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp/${correctedVersion}/mcp-${correctedVersion}-srg.zip"
                        }
                        VersionType.MCP_CONFIG -> {
                            "https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/${correctedVersion}/mcp_config-${correctedVersion}.zip"
                        }
                    })
                    if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get version info")
                    // save to cache file
                    cacheFile.createParentDirectories()
                    cacheFile.writeBytes(resp.bodyAsChannel().toByteArray())
                }
            }.apply { LOGGER.info { "Downloaded searge for $mcVersion/$correctedVersion in $this" } }
        } else {
            LOGGER.info { "Using cached searge for $mcVersion/$correctedVersion" }
        }

        // read cache file
        into.addDependency("searge", into.MappingEntry(
            ContentProvider.of(
                "mcp_config-${mcVersion}-${correctedVersion}.zip",
                cacheFile.inputStream().source().buffer()
            ),
            "searge"
        ).apply {

            requires("official")
            provides("searge" to false)

            mapNamespace(
                "source" to "official",
                "obf" to "official",
                "srg" to "searge",
                "target" to "searge"
            )
        })
    }

    enum class VersionType {
        MCP_CONFIG,
        MCP_SRG
    }

}