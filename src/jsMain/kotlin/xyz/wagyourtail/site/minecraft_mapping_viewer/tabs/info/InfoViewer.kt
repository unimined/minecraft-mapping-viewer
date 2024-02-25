package xyz.wagyourtail.site.minecraft_mapping_viewer.tabs.info

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.logger
import io.kvision.core.*
import io.kvision.html.*
import io.kvision.panel.*
import io.kvision.utils.auto
import io.kvision.utils.perc
import io.kvision.utils.px
import kotlinx.browser.window
import org.w3c.dom.Element
import xyz.wagyourtail.site.minecraft_mapping_viewer.improved.BetterTabPanel
import xyz.wagyourtail.site.minecraft_mapping_viewer.improved.BetterTable
import xyz.wagyourtail.site.minecraft_mapping_viewer.isMobile
import xyz.wagyourtail.unimined.mapping.Namespace
import xyz.wagyourtail.unimined.mapping.jvms.ext.FieldOrMethodDescriptor
import xyz.wagyourtail.unimined.mapping.jvms.ext.FullyQualifiedName
import xyz.wagyourtail.unimined.mapping.jvms.ext.NameAndDescriptor
import xyz.wagyourtail.unimined.mapping.jvms.ext.annotation.Annotation
import xyz.wagyourtail.unimined.mapping.jvms.four.AccessFlag
import xyz.wagyourtail.unimined.mapping.jvms.four.three.two.FieldDescriptor
import xyz.wagyourtail.unimined.mapping.jvms.four.three.two.FieldType
import xyz.wagyourtail.unimined.mapping.jvms.four.three.two.ObjectType
import xyz.wagyourtail.unimined.mapping.jvms.four.two.one.InternalName
import xyz.wagyourtail.unimined.mapping.jvms.four.two.two.UnqualifiedName
import xyz.wagyourtail.unimined.mapping.tree.MappingTree
import xyz.wagyourtail.unimined.mapping.tree.node.*
import xyz.wagyourtail.unimined.mapping.util.defaultedMapOf
import xyz.wagyourtail.unimined.mapping.visitor.*
import xyz.wagyourtail.unimined.mapping.visitor.delegate.*

@JsModule("sanitize-html")
@JsNonModule
external fun sanitizeHtml(input: String, options: dynamic): String

class InfoViewer(val baseNode: BaseNode<*, *>) : VPanel(className = "info-viewer") {
    val LOGGER by KotlinLogging.logger()

    companion object {
        init {
            style(".info-viewer > .container-fluid") {
                height = auto
                overflow = Overflow.INHERIT
            }
        }

        fun Container.className(name: InternalName) {
            hPanel {
                div("Type: ") {
                    paddingRight = 5.px
                    whiteSpace = WhiteSpace.NOWRAP
                }

                val javaName = name.toString().replace("/", ".").replace("$", ".")
                hPanel {
                    overflow = Overflow.HIDDEN
                    style(pClass = PClass.HOVER) {
                        textDecoration = TextDecoration(TextDecorationLine.UNDERLINE, null, null)
                        cursor = Cursor.POINTER
                    }

                    div(className = "small-horizontal-scrollbar") {
                        overflowX = Overflow.AUTO
                        overflowY = Overflow.HIDDEN
                        whiteSpace = WhiteSpace.NOWRAP

                        fun scroll(deltaY: Double) {
                            val el = getElement()
                            if (el != null) {
                                el.scrollLeft += deltaY
                            }
                        }

                        div(className = BsBgColor.DARKSUBTLE.className) {
                            setStyle("user-select", "none")

                            code(javaName, className = BsBgColor.DARKSUBTLE.className) {
                                setStyle("user-select", "all")
                                padding = 2.px

                                onEvent {
                                    mousewheel = { event ->
                                        event.preventDefault()
                                        event.stopPropagation()
                                        scroll(event.deltaY)
                                    }
                                }
                            }

                            onEvent {
                                mousewheel = { event ->
                                    event.preventDefault()
                                    event.stopPropagation()
                                    scroll(event.deltaY)
                                }
                            }
                        }

                        onEvent {
                            mousewheel = { event ->
                                event.preventDefault()
                                event.stopPropagation()
                                scroll(event.deltaY)
                            }
                        }
                    }
                    icon("fas fa-copy") {
                        paddingLeft = 5.px
                        paddingRight = 5.px
                    }
                    onClick {
                        @Suppress("UNNECESSARY_SAFE_CALL")
                        window.navigator.clipboard?.writeText(javaName)
                    }
                }
            }
        }

        fun Container.descriptor(name: FieldOrMethodDescriptor) {
            hPanel {
                div("Descriptor: ") {
                    paddingRight = 5.px
                    whiteSpace = WhiteSpace.NOWRAP
                }

                hPanel {
                    overflow = Overflow.HIDDEN
                    style(pClass = PClass.HOVER) {
                        textDecoration = TextDecoration(TextDecorationLine.UNDERLINE, null, null)
                        cursor = Cursor.POINTER
                    }

                    div(className = "small-horizontal-scrollbar") {
                        overflowX = Overflow.AUTO
                        overflowY = Overflow.HIDDEN
                        whiteSpace = WhiteSpace.NOWRAP

                        fun scroll(deltaY: Double) {
                            val el = getElement()
                            if (el != null) {
                                el.scrollLeft += deltaY
                            }
                        }

                        div(className = BsBgColor.DARKSUBTLE.className) {
                            setStyle("user-select", "none")

                            code(name.toString(), className = BsBgColor.DARKSUBTLE.className) {
                                setStyle("user-select", "all")
                                padding = 2.px


                                onEvent {
                                    mousewheel = { event ->
                                        event.preventDefault()
                                        event.stopPropagation()
                                        scroll(event.deltaY)
                                    }
                                }
                            }

                            onEvent {
                                mousewheel = { event ->
                                    event.preventDefault()
                                    event.stopPropagation()
                                    scroll(event.deltaY)
                                }
                            }
                        }

                        onEvent {
                            mousewheel = { event ->
                                event.preventDefault()
                                event.stopPropagation()
                                scroll(event.deltaY)
                            }
                        }
                    }
                    icon("fas fa-copy") {
                        paddingLeft = 5.px
                        paddingRight = 5.px
                    }
                    onClick {
                        @Suppress("UNNECESSARY_SAFE_CALL")
                        window.navigator.clipboard?.writeText(name.toString())
                    }
                }
            }
        }

        fun Container.mixinTarget(name: FullyQualifiedName) {
            val (type, methodOrField) = name.getParts()
            val str =
                if (methodOrField == null) {
                    type.toString()
                } else {
                    val (name, desc) = methodOrField.getParts()
                    if (desc == null) {
                        null
                    } else if (desc.isMethodDescriptor()) {
                        "${type}${name}${desc}"
                    } else {
                        "${type}${name}:${desc}"
                    }
                }
            hPanel {
                div("Mixin Target: ") {
                    paddingRight = 5.px
                    whiteSpace = WhiteSpace.NOWRAP
                }

                if (str == null) {
                    span(className = BsColor.WARNING.className) {
                        overflowX = Overflow.AUTO
                        setStyle("user-select", "none")

                        code("Can't Generate Mixin Target, Missing Descriptor") {
                            setStyle("user-select", "all")
                            padding = 2.px
                        }
                    }
                } else {
                    hPanel {
                        overflow = Overflow.HIDDEN
                        style(pClass = PClass.HOVER) {
                            textDecoration = TextDecoration(TextDecorationLine.UNDERLINE, null, null)
                            cursor = Cursor.POINTER
                        }


                        div(className = "small-horizontal-scrollbar") {
                            overflowX = Overflow.AUTO
                            overflowY = Overflow.HIDDEN
                            whiteSpace = WhiteSpace.NOWRAP

                            fun scroll(deltaY: Double) {
                                val el = getElement()
                                if (el != null) {
                                    el.scrollLeft += deltaY
                                }
                            }

                            div(className = BsBgColor.DARKSUBTLE.className) {
                                setStyle("user-select", "none")

                                code(str, className = BsBgColor.DARKSUBTLE.className) {
                                    setStyle("user-select", "all")
                                    padding = 2.px

                                    onEvent {
                                        mousewheel = { event ->
                                            event.preventDefault()
                                            event.stopPropagation()
                                            scroll(event.deltaY)
                                        }
                                    }
                                }

                                onEvent {
                                    mousewheel = { event ->
                                        event.preventDefault()
                                        event.stopPropagation()
                                        scroll(event.deltaY)
                                    }
                                }
                            }

                            onEvent {
                                mousewheel = { event ->
                                    event.preventDefault()
                                    event.stopPropagation()
                                    scroll(event.deltaY)
                                }
                            }
                        }
                        icon("fas fa-copy") {
                            paddingLeft = 5.px
                            paddingRight = 5.px
                        }
                        onClick {
                            @Suppress("UNNECESSARY_SAFE_CALL")
                            window.navigator.clipboard?.writeText(str)
                        }
                    }
                }
            }
        }

        fun Container.accessWidener(name: FullyQualifiedName, isMethod: Boolean) {
            val (type, methodOrField) = name.getParts()
            val str =
                if (methodOrField == null) {
                    "accessible\tclass\t${type.getInternalName()}"
                } else {
                    val (name, desc) = methodOrField.getParts()
                    if (desc == null) {
                        null
                    } else if (isMethod) {
                        "accessible\tmethod\t${type.getInternalName()}\t${name}\t${desc}"
                    } else {
                        "accessible\tfield\t${type.getInternalName()}\t${name}\t${desc}"
                    }
                }
            hPanel {
                div("AW: ") {
                    paddingRight = 5.px
                    whiteSpace = WhiteSpace.NOWRAP
                }
                if (str == null) {
                    span(className = BsColor.WARNING.className) {
                        overflowX = Overflow.AUTO
                        setStyle("user-select", "none")

                        code("Can't Generate AW, Missing Descriptor") {
                            setStyle("user-select", "all")
                            padding = 2.px
                        }
                    }
                } else {
                    hPanel {
                        overflow = Overflow.HIDDEN
                        style(pClass = PClass.HOVER) {
                            textDecoration = TextDecoration(TextDecorationLine.UNDERLINE, null, null)
                            cursor = Cursor.POINTER
                        }


                        div(className = "small-horizontal-scrollbar") {
                            overflowX = Overflow.AUTO
                            overflowY = Overflow.HIDDEN
                            whiteSpace = WhiteSpace.NOWRAP

                            fun scroll(deltaY: Double) {
                                val el = getElement()
                                if (el != null) {
                                    el.scrollLeft += deltaY
                                }
                            }

                            div(className = BsBgColor.DARKSUBTLE.className) {
                                setStyle("user-select", "none")

                                code(str, className = BsBgColor.DARKSUBTLE.className) {
                                    setStyle("user-select", "all")
                                    padding = 2.px

                                    onEvent {
                                        mousewheel = { event ->
                                            event.preventDefault()
                                            event.stopPropagation()
                                            scroll(event.deltaY)
                                        }
                                    }
                                }

                                onEvent {
                                    mousewheel = { event ->
                                        event.preventDefault()
                                        event.stopPropagation()
                                        scroll(event.deltaY)
                                    }
                                }
                            }

                            onEvent {
                                mousewheel = { event ->
                                    event.preventDefault()
                                    event.stopPropagation()
                                    scroll(event.deltaY)
                                }
                            }
                        }
                        icon("fas fa-copy") {
                            paddingLeft = 5.px
                            paddingRight = 5.px
                        }
                        onClick {
                            @Suppress("UNNECESSARY_SAFE_CALL")
                            window.navigator.clipboard?.writeText(str)
                        }
                    }
                }
            }
        }

        fun Container.accessTransform(name: FullyQualifiedName, isMethod: Boolean) {
            hPanel {
                div("AT: ") {
                    paddingRight = 5.px
                    whiteSpace = WhiteSpace.NOWRAP
                }

                val (type, methodOrField) = name.getParts()
                val typeName = type.getInternalName().toString().replace("/", ".")
                val str =
                    if (methodOrField == null) {
                        "public\t${typeName}"
                    } else {
                        val (methodName, desc) = methodOrField.getParts()
                        if (desc == null && isMethod) {
                            null
                        } else if (isMethod) {
                            "public\t${typeName}\t${methodName}${desc}"
                        } else {
                            "public\t${typeName}\t${methodName}"
                        }
                    }
                if (str == null) {
                    span(className = BsColor.WARNING.className) {
                        overflowX = Overflow.AUTO
                        setStyle("user-select", "none")

                        code("Can't Generate Method AT, Missing Descriptor") {
                            setStyle("user-select", "all")
                            padding = 2.px
                        }
                    }
                } else {
                    hPanel {
                        overflow = Overflow.HIDDEN
                        style(pClass = PClass.HOVER) {
                            textDecoration = TextDecoration(TextDecorationLine.UNDERLINE, null, null)
                            cursor = Cursor.POINTER
                        }


                        div(className = "small-horizontal-scrollbar") {
                            overflowX = Overflow.AUTO
                            overflowY = Overflow.HIDDEN
                            whiteSpace = WhiteSpace.NOWRAP

                            fun scroll(deltaY: Double) {
                                val el = getElement()
                                if (el != null) {
                                    el.scrollLeft += deltaY
                                }
                            }

                            div(className = BsBgColor.DARKSUBTLE.className) {
                                setStyle("user-select", "none")

                                code(str, className = BsBgColor.DARKSUBTLE.className) {
                                    setStyle("user-select", "all")
                                    padding = 2.px

                                    onEvent {
                                        mousewheel = { event ->
                                            event.preventDefault()
                                            event.stopPropagation()
                                            scroll(event.deltaY)
                                        }
                                    }
                                }

                                onEvent {
                                    mousewheel = { event ->
                                        event.preventDefault()
                                        event.stopPropagation()
                                        scroll(event.deltaY)
                                    }
                                }
                            }

                            onEvent {
                                mousewheel = { event ->
                                    event.preventDefault()
                                    event.stopPropagation()
                                    scroll(event.deltaY)
                                }
                            }
                        }
                        icon("fas fa-copy") {
                            paddingLeft = 5.px
                            paddingRight = 5.px
                        }
                        onClick {
                            @Suppress("UNNECESSARY_SAFE_CALL")
                            window.navigator.clipboard?.writeText(str)
                        }
                    }
                }
            }
        }

    }

    val nsTabs = BetterTabPanel(className = "info-ns-tabs " + BsBorder.BORDER.className, tabPosition = if (window.isMobile()) TabPosition.TOP else TabPosition.LEFT).also {
        add(it)
        it.marginBottom = 5.px
    }

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
        for (name in classNode.root.namespaces) {
            val intName = classNode.getName(name) ?: continue
            byNamespace[name].extra.apply {
                className(intName)
                descriptor(FieldOrMethodDescriptor.unchecked("L$intName;"))
                val fqn = FullyQualifiedName(ObjectType(intName), null)
                mixinTarget(fqn)
                accessWidener(fqn, false)
                accessTransform(fqn, false)
            }
        }
    }

    fun methodExtra(methodNode: MethodNode) {
        for (name in methodNode.root.namespaces) {
            val intName = methodNode.getName(name) ?: continue
            byNamespace[name].extra.apply {
                val cName = (methodNode.parent as ClassNode).getName(name) ?: return@apply
                val mName = methodNode.getName(name) ?: return@apply
                val mDesc = methodNode.getDescriptor(name)
                methodNode.getDescriptor(name)?.let { descriptor(it) }
                val fqn = FullyQualifiedName(ObjectType(cName), NameAndDescriptor(UnqualifiedName.unchecked(mName), mDesc))
                mixinTarget(fqn)
                accessWidener(fqn, true)
                accessTransform(fqn, true)
            }
        }
    }

    fun fieldExtra(fieldNode: FieldNode) {
        for (name in fieldNode.root.namespaces) {
            val intName = fieldNode.getName(name) ?: continue
            byNamespace[name].extra.apply {
                val cName = (fieldNode.parent as ClassNode).getName(name) ?: return@apply
                val fName = fieldNode.getName(name) ?: return@apply
                val fDesc = fieldNode.getDescriptor(name)
                fieldNode.getDescriptor(name)?.let { descriptor(it) }
                val fqn = FullyQualifiedName(ObjectType(cName), NameAndDescriptor(UnqualifiedName.unchecked(fName), fDesc))
                fDesc?.getFieldDescriptor()?.let { descriptor(FieldOrMethodDescriptor(it)) }
                mixinTarget(fqn)
                accessWidener(fqn, false)
                accessTransform(fqn, false)
            }
        }
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
                baseNode.acceptInner(DelegateClassVisitor(ClassNode(tree), delegator), tree.namespaces, false)
            }
            is MethodNode -> {
                baseNode.acceptInner(DelegateMethodVisitor(MethodNode(ClassNode(tree)), delegator), tree.namespaces, false)
            }
            is FieldNode -> {
                baseNode.acceptInner(DelegateFieldVisitor(FieldNode(ClassNode(tree)), delegator), tree.namespaces, false)
            }
            is MethodNode.ParameterNode -> {
                baseNode.acceptInner(DelegateParameterVisitor(MethodNode.ParameterNode(MethodNode(ClassNode(tree)), -1, -1), delegator), tree.namespaces, false)
            }
            is MethodNode.LocalNode -> {
                baseNode.acceptInner(DelegateLocalVariableVisitor(MethodNode.LocalNode(MethodNode(ClassNode(tree)), -1, -1), delegator), tree.namespaces, false)
            }
            is PackageNode -> {
                baseNode.acceptInner(DelegatePackageVisitor(PackageNode(tree), delegator), tree.namespaces, false)
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