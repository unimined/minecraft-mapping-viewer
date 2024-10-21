package xyz.wagyourtail.site.minecraft_mapping_viewer.resolver

import xyz.wagyourtail.unimined.mapping.EnvType
import xyz.wagyourtail.unimined.mapping.Namespace
import xyz.wagyourtail.unimined.mapping.propogator.Propagator
import xyz.wagyourtail.unimined.mapping.resolver.MappingResolver
import xyz.wagyourtail.unimined.mapping.tree.MemoryMappingTree
import java.nio.file.Path

class MappingResolverImpl(
    name: String, val minecraft: Path
): MappingResolver<MappingResolverImpl>(name) {

    override fun createForPostProcess(key: String): MappingResolverImpl {
        return MappingResolverImpl("$name/$key", minecraft)
    }

    override suspend fun propogator(tree: MemoryMappingTree): MemoryMappingTree {
        Propagator(tree, Namespace("official"), setOf(minecraft)).propagate(tree.namespaces.toSet() - Namespace("official"))
        return tree
    }

}
