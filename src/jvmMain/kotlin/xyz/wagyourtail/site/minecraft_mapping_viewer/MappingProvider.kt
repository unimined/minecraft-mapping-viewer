package xyz.wagyourtail.site.minecraft_mapping_viewer

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okio.Buffer
import okio.source
import xyz.wagyourtail.site.minecraft_mapping_viewer.sources.YarnSource
import xyz.wagyourtail.unimined.mapping.EnvType
import xyz.wagyourtail.unimined.mapping.formats.FormatRegistry
import xyz.wagyourtail.unimined.mapping.formats.umf.UMFReader
import xyz.wagyourtail.unimined.mapping.formats.umf.UMFWriter
import xyz.wagyourtail.unimined.mapping.tree.MappingTree
import java.nio.file.Path
import kotlin.io.path.*

@Serializable
data class MCVersion(
    val id: String,
    val type: String,
    val url: String,
    val time: String,
    val releaseTime: String,
    val sha1: String,
    val complianceLevel: Int
)

class MappingProvider(updateTime: Long) {
    val httpClient = HttpClient().apply {
        headers {
            append(HttpHeaders.UserAgent, "MinecraftMappingViewer/1.0 <wagyourtail@wagyourtail.xyz>")
            append(HttpHeaders.Accept, "*")
        }
    }

    val mappingSources: List<MappingSource> = listOf(
        YarnSource
    )

    val versions: Map<String, MCVersion> = runBlocking {
        val resp = httpClient.get("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json")
        Json.parseToJsonElement(resp.bodyAsText()).jsonObject["versions"]!!.jsonArray.map {
            Json.decodeFromJsonElement(MCVersion.serializer(), it)
        }.associateBy { it.id }
    }

    suspend fun genMappings(mcVersion: String, cacheFile: Path) = withContext(Dispatchers.IO) {
        val version = versions[mcVersion] ?: throw IllegalArgumentException("Invalid Minecraft version")
        val mappings = MappingTree()
        for (source in mappingSources) {
            val map = source.getMappings(mcVersion, httpClient)
            if (map != null) {
                FormatRegistry.autodetectFormat(EnvType.JOINED, map.first, map.second.peek())!!.read(EnvType.JOINED, map.second, mappings, mappings, mapOf())
            }
        }
        val temp = cacheFile.resolveSibling(cacheFile.fileName.toString() + ".tmp")
        temp.parent?.createDirectories()
        Buffer().use {
            mappings.accept(UMFWriter.write(it))
            temp.outputStream().use { out ->
                it.copyTo(out)
            }
        }
        temp.moveTo(cacheFile)
        cacheFile.readText()
    }

    suspend fun getMappings(mcVersion: String): String {
        if (mcVersion !in versions) {
            throw IllegalArgumentException("Invalid Minecraft version")
        }
        val cachePath = Path.of("mmv_cache", "mappings").resolve(mcVersion).resolve("mappings.umf")
        if (cachePath.exists()) {
            Buffer().use {
                it.writeAll(cachePath.inputStream().source())
                val start = System.currentTimeMillis()
                UMFReader.read(it)
                val end = System.currentTimeMillis()
                println("parse time: ${end - start} ms")
            }
            return cachePath.readText()
        }
        return genMappings(mcVersion, cachePath)
    }



}