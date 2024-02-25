package xyz.wagyourtail.site.minecraft_mapping_viewer.resolver

import xyz.wagyourtail.unimined.mapping.resolver.MappingResolver
import xyz.wagyourtail.unimined.mapping.tree.MappingTree

class MappingResolverImpl(
    name: String,
    propogator: (MappingTree.() -> Unit)?
): MappingResolver(name, propogator) {

//    override val mojmapLocation: Location = Location("", object : Completer {
//        override suspend fun complete(baseUrl: String, coords: MavenCoords): String {
//            val mcVersion = minecraftVersions.versions.first { it.id == coords.version }
//            val resp = MMV_HTTP_CLIENT.get(mcVersion.url)
//            if (resp.status != HttpStatusCode.OK) throw Exception("Failed to get version info")
//            val versionInfo = Json.parseToJsonElement(resp.bodyAsText()).jsonObject
//            val downloads = versionInfo["downloads"]!!.jsonObject
//            if (coords.artifact.startsWith("client")) {
//                return downloads["client_mappings"]!!.jsonObject["url"]!!.jsonPrimitive.content
//            } else if (coords.artifact.startsWith("server")) {
//                return downloads["server_mappings"]!!.jsonObject["url"]!!.jsonPrimitive.content
//            }
//            throw IllegalArgumentException("Invalid artifact")
//        }
//    })

    override fun createForPostProcess(key: String): MappingResolver {
        return MappingResolverImpl(key, propogator)
    }

//    override fun mcVersionCompare(a: String, b: String): Int {
//        val aVer = minecraftVersions.versions.indexOfFirst { it.id == a }
//        val bVer = minecraftVersions.versions.indexOfFirst { it.id == b }
//        if (aVer == -1 || bVer == -1) throw IllegalArgumentException("Invalid Minecraft version")
//        return aVer.compareTo(bVer)
//    }



}
