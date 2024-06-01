package xyz.wagyourtail.site.minecraft_mapping_viewer

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.dom.Window
import xyz.wagyourtail.unimined.mapping.Namespace
import xyz.wagyourtail.unimined.mapping.jvms.four.three.three.MethodDescriptor
import xyz.wagyourtail.unimined.mapping.jvms.four.three.two.FieldDescriptor
import xyz.wagyourtail.unimined.mapping.jvms.four.two.one.InternalName
import xyz.wagyourtail.unimined.mapping.tree.AbstractMappingTree
import xyz.wagyourtail.unimined.mapping.tree.node._class.ClassNode
import xyz.wagyourtail.unimined.mapping.visitor.*
import xyz.wagyourtail.unimined.mapping.visitor.delegate.NullDelegator
import xyz.wagyourtail.unimined.mapping.visitor.delegate.delegator

fun Window.isMobile() = innerWidth < innerHeight

fun AbstractMappingTree.filterClassByQuery(query: String, type: SearchType = SearchType.KEYWORD): List<Pair<Map<Namespace, InternalName>, () -> ClassNode>> {
    val q = query.trim()
    if (q.isEmpty()) return classList().map { it.first to it.second }

    return classList().filter { cls ->
        if (type == SearchType.KEYWORD || type == SearchType.CLASS) {
            if (cls.first.values.any { it.value.contains(query) }) {
                return@filter true
            }
        }
        if (type != SearchType.CLASS) {
            var ret = false
            cls.third(EmptyMappingVisitor().delegator(object : NullDelegator() {
                override fun visitClass(delegate: MappingVisitor, names: Map<Namespace, InternalName>): ClassVisitor? {
                    return default.visitClass(delegate, names)
                }

                override fun visitField(
                    delegate: ClassVisitor,
                    names: Map<Namespace, Pair<String, FieldDescriptor?>>
                ): FieldVisitor? {
                    if (type == SearchType.KEYWORD || type == SearchType.FIELD) {
                        if (ret || names.values.any { it.first.contains(q) }) {
                            ret = true
                            return null
                        }
                    }
                    return super.visitField(delegate, names)
                }

                override fun visitMethod(
                    delegate: ClassVisitor,
                    names: Map<Namespace, Pair<String, MethodDescriptor?>>
                ): MethodVisitor? {
                    if (type == SearchType.KEYWORD || type == SearchType.METHOD) {
                        if (ret || names.values.any { it.first.contains(q) }) {
                            ret = true
                            return null
                        }
                    }
                    return super.visitMethod(delegate, names)
                }

            }), namespaces)
            if (ret) return@filter true
        }
        false
    }.map { it.first to it.second }
}

//fun MappingTree.getAutocompleteResults(query: String, type: SearchType = SearchType.KEYWORD): Set<Pair<SearchType, FullyQualifiedName>> {
//    val q = query.trim()
//    val results = mutableSetOf<Pair<SearchType, FullyQualifiedName>>()
//
//    if (type == SearchType.KEYWORD || type == SearchType.CLASS) {
//        for (cls in this.classesIter()) {
//            cls.first.values.firstOrNull { it.value.lowercase().contains(q.lowercase()) }?.let {
//                results.add(SearchType.CLASS to FullyQualifiedName(ObjectType(it), null))
//            }
//        }
//    }
//
//    if (type != SearchType.CLASS) {
//        for (cls in this.classesIter()) {
//            val clsInfo = cls.second()
//            if (type == SearchType.KEYWORD || type == SearchType.METHOD) {
//                for (method in clsInfo.methods.resolve()) {
//                    method.names.entries.firstOrNull { it.value.lowercase().contains(q.lowercase()) }
//                        ?.let { (ns, name) ->
//                            results.add(
//                                SearchType.METHOD to FullyQualifiedName(
//                                    ObjectType(clsInfo.getName(ns) ?: return@let),
//                                    NameAndDescriptor(UnqualifiedName.unchecked(name), method.getDescriptor(ns))
//                                )
//                            )
//                        }
//                }
//            }
//            if (type == SearchType.KEYWORD || type == SearchType.FIELD) {
//                for (field in clsInfo.fields.resolve()) {
//                    field.names.entries.firstOrNull { it.value.lowercase().contains(q.lowercase()) }?.let { (ns, name) ->
//                        results.add(
//                            SearchType.FIELD to FullyQualifiedName(
//                                ObjectType(clsInfo.getName(ns) ?: return@let),
//                                NameAndDescriptor(UnqualifiedName.unchecked(name), field.getDescriptor(ns))
//                            )
//                        )
//                    }
//                }
//            }
//        }
//    }
//
//    return results
//}

fun <T> emptyIterator() = emptyList<T>().iterator()

fun ArrayBuffer.toByteArray(): ByteArray = Int8Array(this).unsafeCast<ByteArray>()
fun ArrayBuffer?.toByteArray(): ByteArray? = this?.run { Int8Array(this).unsafeCast<ByteArray>() }