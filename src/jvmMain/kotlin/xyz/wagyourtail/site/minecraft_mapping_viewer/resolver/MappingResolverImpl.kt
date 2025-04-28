package xyz.wagyourtail.site.minecraft_mapping_viewer.resolver

import xyz.wagyourtail.unimined.mapping.Namespace
import xyz.wagyourtail.unimined.mapping.propagator.InheritanceTree
import xyz.wagyourtail.unimined.mapping.resolver.MappingResolver
import xyz.wagyourtail.unimined.mapping.tree.AbstractMappingTree
import xyz.wagyourtail.unimined.mapping.tree.MemoryMappingTree

class MappingResolverImpl(
    name: String, val inheritanceTreeProvider: (AbstractMappingTree, Namespace) -> InheritanceTree
): MappingResolver<MappingResolverImpl>(name) {

    override fun createForPostProcess(key: String, process: MemoryMappingTree.() -> Unit): MappingResolverImpl {
        return MappingResolverImpl("$name/$key", inheritanceTreeProvider)
    }

    override suspend fun applyInheritanceTree(tree: MemoryMappingTree, apply: suspend (InheritanceTree) -> Unit) {
        apply(inheritanceTreeProvider(tree, Namespace("official")))
    }

}
