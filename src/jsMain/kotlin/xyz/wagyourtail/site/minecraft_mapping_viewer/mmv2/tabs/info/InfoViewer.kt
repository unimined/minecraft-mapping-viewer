package xyz.wagyourtail.site.minecraft_mapping_viewer.mmv2.tabs.info

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.logger
import io.kvision.core.Overflow
import io.kvision.html.Div
import io.kvision.html.div
import io.kvision.html.h4
import io.kvision.html.h5
import io.kvision.panel.VPanel
import io.kvision.utils.perc
import io.kvision.utils.px
import xyz.wagyourtail.site.minecraft_mapping_viewer.mmv2.improved.BetterTable
import xyz.wagyourtail.unimined.mapping.Namespace
import xyz.wagyourtail.unimined.mapping.jvms.ext.annotation.Annotation
import xyz.wagyourtail.unimined.mapping.jvms.four.AccessFlag
import xyz.wagyourtail.unimined.mapping.jvms.four.two.one.InternalName
import xyz.wagyourtail.unimined.mapping.tree.MappingTree
import xyz.wagyourtail.unimined.mapping.tree.node.*
import xyz.wagyourtail.unimined.mapping.visitor.*
import xyz.wagyourtail.unimined.mapping.visitor.delegate.*

class InfoViewer(val baseNode: BaseNode<*, *>) : VPanel() {
    val LOGGER by KotlinLogging.logger()

    init {
        width = 100.perc
        height = 100.perc
        overflow = Overflow.AUTO
    }

    val signatures by lazy {
        BetterTable("Signatures").also {
            it.head.row {
                for (ns in baseNode.root.namespaces) {
                    header(ns.name)
                }
            }
            div {
                marginBottom = 5.px
                h4("Signatures")
                add(it)
            }
        }
    }

    val annotations by lazy {
        BetterTable("Annotations").also {
            it.head.row {
                header("Type")
                header("Base Namespace")
                header("Annotation")
                header("Namespaces")
            }
            div {
                marginBottom = 5.px
                h4("Annotations")
                add(it)
            }
        }
    }

    val access by lazy {
        BetterTable("Access").also {
            it.head.row {
                header("Type")
                header("Value")
                header("Namespaces")
            }
            div {
                marginBottom = 5.px
                h4("Access")
                add(it)
            }
        }
    }

    val comments by lazy {
        div {
            marginBottom = 5.px
            h4("Comments")
        }
    }

    val exceptions by lazy {
        BetterTable("Exceptions").also {
            it.head.row {
                header("Type")
                header("Base Namespace")
                header("Exception")
                header("Namespaces")
            }
            div {
                marginBottom = 5.px
                h4("Exceptions")
                add(it)
            }
        }
    }


    val delegator = object : NullDelegator() {

        override fun visitSignature(
            delegate: SignatureParentVisitor<*>,
            values: Map<Namespace, String>
        ): SignatureVisitor? {
            val body = signatures.firstBody ?: signatures.body {}
            body.row {
                for (ns in baseNode.root.namespaces) {
                    cell(values[ns] ?: "-")
                }
            }
            return null
        }

        override fun visitAnnotation(
            delegate: AnnotationParentVisitor<*>,
            type: AnnotationType,
            baseNs: Namespace,
            annotation: Annotation,
            namespaces: Set<Namespace>
        ): AnnotationVisitor? {
            val body = annotations.firstBody ?: annotations.body {}
            body.row {
                cell(type.name)
                cell(baseNs.name)
                cell(annotation.toString())
                cell(namespaces.joinToString { it.name })
            }
            return null
        }

        override fun visitAccess(
            delegate: AccessParentVisitor<*>,
            type: AccessType,
            value: AccessFlag,
            namespaces: Set<Namespace>
        ): AccessVisitor? {
            val body = access.firstBody ?: access.body {}
            body.row {
                cell(type.name)
                cell(value.toString())
                cell(namespaces.joinToString { it.name })
            }
            return null
        }

        override fun visitComment(delegate: CommentParentVisitor<*>, values: Map<Namespace, String>): CommentVisitor? {
            for ((ns, comment) in values.entries.sortedBy { baseNode.root.namespaces.indexOf(it.key) }) {
                comments.add(Div {
                    marginBottom = 5.px
                    h5(ns.name)
                    div(comment)
                })
            }
            return null
        }

        override fun visitException(
            delegate: MethodVisitor,
            type: ExceptionType,
            exception: InternalName,
            baseNs: Namespace,
            namespaces: Set<Namespace>
        ): ExceptionVisitor? {
            val body = exceptions.firstBody ?: exceptions.body {}
            body.row {
                cell(type.name)
                cell(baseNs.name)
                cell(exception.toString())
                cell(namespaces.joinToString { it.name })
            }
            return null
        }

    }

    init {
        val tree = MappingTree()
        when (baseNode) {
            is ClassNode -> {
                baseNode.acceptInner(DelegateClassVisitor(ClassNode(tree), delegator), false)
            }
            is MethodNode -> {
                baseNode.acceptInner(DelegateMethodVisitor(MethodNode(ClassNode(tree)), delegator), false)
            }
            is FieldNode -> {
                baseNode.acceptInner(DelegateFieldVisitor(FieldNode(ClassNode(tree)), delegator), false)
            }
            is MethodNode.ParameterNode -> {
                baseNode.acceptInner(DelegateParameterVisitor(MethodNode.ParameterNode(MethodNode(ClassNode(tree)), -1, -1), delegator), false)
            }
            is MethodNode.LocalNode -> {
                baseNode.acceptInner(DelegateLocalVariableVisitor(MethodNode.LocalNode(MethodNode(ClassNode(tree)), -1, -1), delegator), false)
            }
            is PackageNode -> {
                baseNode.acceptInner(DelegatePackageVisitor(PackageNode(tree), delegator), false)
            }
            else -> {
                LOGGER.warn { "Unknown node type: ${baseNode::class.simpleName}" }
            }
        }
    }



}