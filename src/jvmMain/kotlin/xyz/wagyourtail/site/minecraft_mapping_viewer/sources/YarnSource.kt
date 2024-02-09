package xyz.wagyourtail.site.minecraft_mapping_viewer.sources

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.Buffer
import okio.BufferedSource
import xyz.wagyourtail.site.minecraft_mapping_viewer.MappingSource

object YarnSource : MappingSource() {
    override suspend fun getMappings(mcVersion: String, httpClient: HttpClient): Triple<String, BufferedSource, List<String>>? {
        val resp = httpClient.get("https://meta.fabricmc.net/v2/versions/yarn/$mcVersion")
        if (resp.status.value != 200) return null
        val json = Json.parseToJsonElement(resp.bodyAsText())
        // get latest
        val latest = json.jsonArray.first().jsonObject["build"]!!.jsonPrimitive.content

        val resp2 = httpClient.get("https://maven.fabricmc.net/net/fabricmc/yarn/$mcVersion+build.$latest/yarn-$mcVersion+build.$latest-v2.jar")
        if (resp2.status.value != 200) return null
        return Triple("yarn", Buffer().readFrom(resp2.bodyAsChannel().toInputStream()), listOf("yarn-$mcVersion+build.$latest-v2.jar"))
    }
}
