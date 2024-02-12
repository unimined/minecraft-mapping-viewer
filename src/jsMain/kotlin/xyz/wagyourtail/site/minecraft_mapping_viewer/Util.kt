package xyz.wagyourtail.site.minecraft_mapping_viewer

import org.w3c.dom.Window
import xyz.wagyourtail.unimined.mapping.jvms.ext.FullyQualifiedName
import xyz.wagyourtail.unimined.mapping.jvms.ext.NameAndDescriptor
import xyz.wagyourtail.unimined.mapping.jvms.four.ElementType
import xyz.wagyourtail.unimined.mapping.jvms.four.three.two.ObjectType
import xyz.wagyourtail.unimined.mapping.jvms.four.two.two.UnqualifiedName
import xyz.wagyourtail.unimined.mapping.tree.MappingTree
import xyz.wagyourtail.unimined.mapping.tree.node.ClassNode

fun Window.isMobile() = innerWidth < innerHeight

fun MappingTree.filterByQuery(query: String, type: SearchType = SearchType.KEYWORD): Set<ClassNode> {
    val q = query.trim()
    if (q.isEmpty()) return this.classes
    val nodes = mutableSetOf<ClassNode>()

    outer@for (cls in this.classes) {
        if (type == SearchType.KEYWORD || type == SearchType.CLASS) {
            if (cls.names.values.any { it?.value?.lowercase()?.contains(q.lowercase()) == true }) {
                nodes.add(cls)
                continue@outer
            }
        }
    }

    if (type == SearchType.KEYWORD || type == SearchType.METHOD) {
        outer@for (cls in this.classes) {
            for (method in cls.methods.resolve()) {
                if (method.names.values.any { it.lowercase().contains(q.lowercase()) }) {
                    nodes.add(cls)
                    continue@outer
                }
            }
        }
    }

    if (type == SearchType.KEYWORD || type == SearchType.FIELD) {
        outer@for (cls in this.classes) {
            for (field in cls.fields.resolve()) {
                if (field.names.values.any { it.lowercase().contains(q.lowercase()) }) {
                    nodes.add(cls)
                    continue@outer
                }
            }
        }
    }

    return nodes
}

fun MappingTree.getAutocompleteResults(query: String, type: SearchType = SearchType.KEYWORD): Set<Pair<SearchType, FullyQualifiedName>> {
    val q = query.trim()
    val results = mutableSetOf<Pair<SearchType, FullyQualifiedName>>()

    if (type == SearchType.KEYWORD || type == SearchType.CLASS) {
        for (cls in this.classes) {
            cls.names.values.firstOrNull { it?.value?.lowercase()?.contains(q.lowercase()) == true }?.let {
                results.add(SearchType.CLASS to FullyQualifiedName(ObjectType(it), null))
            }
        }
    }

    if (type == SearchType.KEYWORD || type == SearchType.METHOD) {
        for (cls in this.classes) {
            for (method in cls.methods.resolve()) {
                method.names.entries.firstOrNull { it.value.lowercase().contains(q.lowercase()) }?.let { (ns, name) ->
                    results.add(SearchType.METHOD to FullyQualifiedName(
                        ObjectType(cls.getName(ns) ?: return@let),
                        NameAndDescriptor(UnqualifiedName.unchecked(name), method.getDescriptor(ns))
                    ))
                }
            }
        }
    }

    if (type == SearchType.KEYWORD || type == SearchType.FIELD) {
        for (cls in this.classes) {
            for (field in cls.fields.resolve()) {
                field.names.entries.firstOrNull { it.value.lowercase().contains(q.lowercase()) }?.let { (ns, name) ->
                    results.add(
                        SearchType.FIELD to FullyQualifiedName(
                            ObjectType(cls.getName(ns) ?: return@let),
                            NameAndDescriptor(UnqualifiedName.unchecked(name), field.getDescriptor(ns))
                        )
                    )
                }
            }
        }
    }

    return results
}