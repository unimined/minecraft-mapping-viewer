package xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.info

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.logger
import io.kvision.core.*
import io.kvision.html.*
import io.kvision.panel.*
import io.kvision.utils.perc
import io.kvision.utils.px
import kotlinx.browser.window
import xyz.wagyourtail.site.minecraft_mapping_viewer.improved.BetterTable
import xyz.wagyourtail.unimined.mapping.Namespace
import xyz.wagyourtail.unimined.mapping.jvms.ext.annotation.Annotation
import xyz.wagyourtail.unimined.mapping.jvms.four.AccessFlag
import xyz.wagyourtail.unimined.mapping.jvms.four.three.two.FieldType
import xyz.wagyourtail.unimined.mapping.jvms.four.three.two.ObjectType
import xyz.wagyourtail.unimined.mapping.jvms.four.two.one.InternalName
import xyz.wagyourtail.unimined.mapping.tree.MappingTree
import xyz.wagyourtail.unimined.mapping.tree.node.*
import xyz.wagyourtail.unimined.mapping.util.defaultedMapOf
import xyz.wagyourtail.unimined.mapping.visitor.*
import xyz.wagyourtail.unimined.mapping.visitor.delegate.*

@JsModule("sanitize-html")
@JsNonModule
external fun sanitizeHtml(input: String, options: dynamic): String

class InfoViewer(val baseNode: BaseNode<*, *>) : VPanel() {
    val LOGGER by KotlinLogging.logger()

    val nsTabs = tabPanel {}

    val byNamespace = defaultedMapOf<Namespace, ByNamespaceData> { ns ->
        ByNamespaceData().also {
            nsTabs.tab(ns.name) {
                add(it)
            }
        }
    }

    init {
        width = 100.perc
        height = 100.perc
        overflow = Overflow.AUTO

        when (baseNode) {
            is ClassNode -> {
                classExtra(baseNode)
            }
            is MethodNode -> {
                methodExtra(baseNode)
            }
            is FieldNode -> {
                fieldExtra(baseNode)
            }
        }
    }

    fun classExtra(classNode: ClassNode) {
        div {
            div {
                for (name in classNode.root.namespaces) {
                    val intName = classNode.getName(name) ?: continue
                    byNamespace[name].extra.apply {
                        div {
                            +"Internal Name: "
                            div(className = BsBgColor.DARKSUBTLE.className) {
                                display = Display.INLINE
                                style(pClass = PClass.HOVER) {
                                    textDecoration = TextDecoration(TextDecorationLine.UNDERLINE, null, null)
                                    cursor = Cursor.POINTER
                                }
                                span {
                                    overflowX = Overflow.AUTO
                                    code(intName.toString(), className = BsBgColor.DARKSUBTLE.className) {
                                        padding = 2.px
                                    }
                                }
                                icon("fas fa-copy")
                                onClick {
                                    window.navigator.clipboard.writeText(intName.toString())
                                }
                            }
                        }

                        div {
                            +"Type: "
                            val javaName = intName.toString().replace("/", ".").replace("$", ".")
                            div(className = BsBgColor.DARKSUBTLE.className) {
                                display = Display.INLINE
                                style(pClass = PClass.HOVER) {
                                    textDecoration = TextDecoration(TextDecorationLine.UNDERLINE, null, null)
                                    cursor = Cursor.POINTER
                                }
                                span {
                                    overflowX = Overflow.AUTO
                                    code(javaName) {
                                        padding = 2.px
                                    }
                                }
                                icon("fas fa-copy")
                                onClick {
                                    window.navigator.clipboard.writeText(javaName)
                                }
                            }
                        }

                        div {
                            +"AW: "
                            div(className = BsBgColor.DARKSUBTLE.className) {
                                display = Display.INLINE
                                style(pClass = PClass.HOVER) {
                                    textDecoration = TextDecoration(TextDecorationLine.UNDERLINE, null, null)
                                    cursor = Cursor.POINTER
                                }
                                span {
                                    overflowX = Overflow.AUTO
                                    code("accessible\tclass\t${intName}") {
                                        padding = 2.px
                                    }
                                }
                                icon("fas fa-copy")
                                onClick {
                                    window.navigator.clipboard.writeText("accessible\tclass\t${intName}")
                                }
                            }
                        }

                        div {
                            +"AT: "
                            val javaName = intName.toString().replace("/", ".")
                            div(className = BsBgColor.DARKSUBTLE.className) {
                                display = Display.INLINE
                                style(pClass = PClass.HOVER) {
                                    textDecoration = TextDecoration(TextDecorationLine.UNDERLINE, null, null)
                                    cursor = Cursor.POINTER
                                }
                                span {
                                    overflowX = Overflow.AUTO
                                    code("public\t$javaName") {
                                        padding = 2.px
                                    }
                                }
                                icon("fas fa-copy")
                                onClick {
                                    window.navigator.clipboard.writeText("public\t$javaName")
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    fun methodExtra(methodNode: MethodNode) {

    }

    fun fieldExtra(fieldNode: FieldNode) {

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
                byNamespace[ns].comments.add(p(sanitizeHtml(comment, null), rich = true))
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

    inner class ByNamespaceData : Div() {

        val comments = div(className = BsBorder.BORDERBOTTOM.className) {
            marginBottom = 5.px
        }

        val extra = div(className = BsBorder.BORDERBOTTOM.className) {
            marginBottom = 5.px
        }

    }

}