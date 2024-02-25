package xyz.wagyourtail.site.minecraft_mapping_viewer.sources.meta

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class LauncherMeta(
    val latest: Latest,
    val versions: List<MCVersion>
) {

    constructor(json: JsonObject) : this(
        Latest(json["latest"]!!.jsonObject),
        json["versions"]!!.jsonArray.map { MCVersion(it.jsonObject) }
    )


}

@Serializable
data class Latest(
    val release: String,
    val snapshot: String
) {
    constructor(json: JsonObject) : this(
        json["release"]!!.jsonPrimitive.content,
        json["snapshot"]!!.jsonPrimitive.content
    )

}

@Serializable
data class MCVersion(
    val id: String,
    val type: String,
    val url: String,
    val time: String,
    val releaseTime: String,
    val sha1: String,
    val complianceLevel: Int
) {

    constructor(json: JsonObject) : this(
        json["id"]!!.jsonPrimitive.content,
        json["type"]!!.jsonPrimitive.content,
        json["url"]!!.jsonPrimitive.content,
        json["time"]!!.jsonPrimitive.content,
        json["releaseTime"]!!.jsonPrimitive.content,
        json["sha1"]!!.jsonPrimitive.content,
        json["complianceLevel"]!!.jsonPrimitive.int
    )

}