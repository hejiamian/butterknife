package butterknife.compiler

import butterknife.*
import butterknife.Optional
import butterknife.internal.Constants
import butterknife.internal.ListenerClass
import butterknife.internal.ListenerMethod
import com.google.auto.common.SuperficialValidation
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.squareup.javapoet.TypeName
import com.sun.source.util.Trees
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Symbol.VarSymbol
import com.sun.tools.javac.code.Type.JCPrimitiveType
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.JCTree.*
import com.sun.tools.javac.tree.TreeScanner
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.Field
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.*
import javax.lang.model.util.Types
import javax.tools.Diagnostic

class ButterKnifeProcessor : AbstractProcessor() {
    private var typeUtils: Types? = null
    private var filer: Filer? = null
    private var trees: Trees? = null

    private var sdk = 1
    private var debuggable = true

    private val rScanner: RScanner = RScanner()

    companion object {
        private const val OPTION_SDK_INT = "butterknife.minSdk"
        private const val OPTION_DEBUGGABLE = "butterknife.debuggable"
        val NO_ID = Id(Constants.NO_RES_ID)
        const val VIEW_TYPE = "android.view.View"
        const val ACTIVITY_TYPE = "android.app.Activity"
        const val DIALOG_TYPE = "android.app.Dialog"
        private const val COLOR_STATE_LIST_TYPE = "android.content.res.ColorStateList"
        private const val BITMAP_TYPE = "android.graphics.Bitmap"
        private const val ANIMATION_TYPE = "android.view.animation.Animation"
        private const val DRAWABLE_TYPE = "android.graphics.drawable.Drawable"
        private const val TYPED_ARRAY_TYPE = "android.content.res.TypedArray"
        private const val TYPEFACE_TYPE = "android.graphics.Typeface"
        private const val NULLABLE_ANNOTATION_NAME = "Nullable"
        private const val STRING_TYPE = "java.lang.String"
        private val LIST_TYPE = MutableList::class.java.canonicalName
        private val LISTENERS = listOf( //
                OnCheckedChanged::class.java,  //
                OnClick::class.java,  //
                OnEditorAction::class.java,  //
                OnFocusChange::class.java,  //
                OnItemClick::class.java,  //
                OnItemLongClick::class.java,  //
                OnItemSelected::class.java,  //
                OnLongClick::class.java,  //
                OnPageChange::class.java,  //
                OnTextChanged::class.java,  //
                OnTouch::class.java //
        )

        fun isSubtypeOfType(typeMirror: TypeMirror, otherType: String): Boolean {
            if (isTypeEqual(typeMirror, otherType)) {
                return true
            }
            if (typeMirror.kind != TypeKind.DECLARED) {
                return false
            }
            val declaredType = typeMirror as DeclaredType
            val typeArguments = declaredType.typeArguments
            if (typeArguments.size > 0) {
                val typeString = StringBuilder(declaredType.asElement().toString())
                typeString.append('<')
                for (i in typeArguments.indices) {
                    if (i > 0) {
                        typeString.append(',')
                    }
                    typeString.append('?')
                }
                typeString.append('>')
                if (typeString.toString() == otherType) {
                    return true
                }
            }
            val element = declaredType.asElement() as? TypeElement ?: return false
            val typeElement = element
            val superType = typeElement.superclass
            if (isSubtypeOfType(superType, otherType)) {
                return true
            }
            for (interfaceType in typeElement.interfaces) {
                if (isSubtypeOfType(interfaceType, otherType)) {
                    return true
                }
            }
            return false
        }

        private fun isTypeEqual(typeMirror: TypeMirror, otherType: String): Boolean {
            return otherType == typeMirror.toString()
        }

        /**
         * Returns a method name from the `android.content.res.Resources` class for array resource
         * binding, null if the element type is not supported.
         */
        private fun getArrayResourceMethodName(element: Element): FieldResourceBinding.Type? {
            val typeMirror = element.asType()
            if (TYPED_ARRAY_TYPE == typeMirror.toString()) {
                return FieldResourceBinding.Type.TYPED_ARRAY
            }
            if (TypeKind.ARRAY == typeMirror.kind) {
                val arrayType = typeMirror as ArrayType
                val componentType = arrayType.componentType.toString()
                if (STRING_TYPE == componentType) {
                    return FieldResourceBinding.Type.STRING_ARRAY
                } else if ("int" == componentType) {
                    return FieldResourceBinding.Type.INT_ARRAY
                } else if ("java.lang.CharSequence" == componentType) {
                    return FieldResourceBinding.Type.TEXT_ARRAY
                }
            }
            return null
        }

        /** Returns the first duplicate element inside an array, null if there are no duplicates.  */
        private fun findDuplicate(array: IntArray): Int? {
            val seenElements: MutableSet<Int> = LinkedHashSet()
            for (element in array) {
                if (!seenElements.add(element)) {
                    return element
                }
            }
            return null
        }

        private fun hasAnnotationWithName(element: Element, simpleName: String): Boolean {
            for (mirror in element.annotationMirrors) {
                val annotationName = mirror.annotationType.asElement().simpleName.toString()
                if (simpleName == annotationName) {
                    return true
                }
            }
            return false
        }

        private fun isFieldRequired(element: Element): Boolean {
            return !hasAnnotationWithName(element, NULLABLE_ANNOTATION_NAME)
        }

        private fun isListenerRequired(element: ExecutableElement): Boolean {
            return element.getAnnotation(Optional::class.java) == null
        }

        private fun getMirror(element: Element,
                              annotation: Class<out Annotation?>): AnnotationMirror? {
            for (annotationMirror in element.annotationMirrors) {
                if (annotationMirror.annotationType.toString() == annotation.canonicalName) {
                    return annotationMirror
                }
            }
            return null
        }
    }

    @Synchronized
    override fun init(env: ProcessingEnvironment) {
        super.init(env)
        env.options[OPTION_SDK_INT]?.let {
            try {
                this.sdk = it.toInt()
            } catch (e: NumberFormatException) {
                env.messager.printMessage(Diagnostic.Kind.WARNING, "Unable to parse supplied minSdk option $it. Falling back to API 1 support.")
            }
        }
        debuggable = "false" != env.options[ButterKnifeProcessor.OPTION_DEBUGGABLE]
        typeUtils = env.typeUtils
        filer = env.filer
        try {
            trees = Trees.instance(processingEnv)
        } catch (ignored: IllegalArgumentException) {
            try {
                // Get original ProcessingEnvironment from Gradle-wrapped one or KAPT-wrapped one.
                for (field in processingEnv.javaClass.declaredFields) {
                    if (field.name == "delegate" || field.name == "processingEnv") {
                        field.isAccessible = true
                        val javacEnv = field[processingEnv] as ProcessingEnvironment
                        trees = Trees.instance(javacEnv)
                        break
                    }
                }
            } catch (th: Throwable) {
                th.printStackTrace()
            }
        }
    }

    override fun getSupportedOptions(): Set<String>? {
        val builder = ImmutableSet.builder<String>()
        builder.add(OPTION_SDK_INT, OPTION_DEBUGGABLE)
        if (trees != null) builder.add(IncrementalAnnotationProcessorType.ISOLATING.processorOption)
        return builder.build()
    }

    override fun getSupportedAnnotationTypes(): Set<String>? {
        val types: MutableSet<String> = mutableSetOf()
        for (annotation in getSupportedAnnotations()) {
            types.add(annotation.canonicalName)
        }
        return types
    }

    private fun getSupportedAnnotations(): MutableSet<Class<out Annotation?>> {
        val annotations: MutableSet<Class<out Annotation?>> = mutableSetOf()
        annotations.add(BindAnim::class.java)
        annotations.add(BindArray::class.java)
        annotations.add(BindBitmap::class.java)
        annotations.add(BindBool::class.java)
        annotations.add(BindColor::class.java)
        annotations.add(BindDimen::class.java)
        annotations.add(BindDrawable::class.java)
        annotations.add(BindFloat::class.java)
        annotations.add(BindFont::class.java)
        annotations.add(BindInt::class.java)
        annotations.add(BindString::class.java)
        annotations.add(BindView::class.java)
        annotations.add(BindViews::class.java)
        annotations.addAll(LISTENERS)
        return annotations
    }

    override fun process(elements: Set<TypeElement?>?, env: RoundEnvironment): Boolean {
        val bindingMap: Map<TypeElement, BindingSet> = findAndParseTargets(env)
        for ((typeElement, binding) in bindingMap) {
            val javaFile = binding.brewJava(sdk, debuggable)
            try {
                javaFile!!.writeTo(filer)
            } catch (e: IOException) {
                error(typeElement, "Unable to write binding for type %s: %s", typeElement, e.message)
            }
        }
        return false
    }

    private fun findAndParseTargets(env: RoundEnvironment): MutableMap<TypeElement, BindingSet> {
        val builderMap: MutableMap<TypeElement, BindingSet.Builder> = mutableMapOf()
        val erasedTargetNames: MutableSet<TypeElement> = mutableSetOf()

        // Process each @BindAnim element.
        for (element in env.getElementsAnnotatedWith(BindAnim::class.java)) {
            if (!SuperficialValidation.validateElement(element)) continue
            try {
                parseResourceAnimation(element, builderMap, erasedTargetNames)
            } catch (e: Exception) {
                logParsingError(element, BindAnim::class.java, e)
            }
        }

        // Process each @BindArray element.
        for (element in env.getElementsAnnotatedWith(BindArray::class.java)) {
            if (!SuperficialValidation.validateElement(element)) continue
            try {
                parseResourceArray(element, builderMap, erasedTargetNames)
            } catch (e: Exception) {
                logParsingError(element, BindArray::class.java, e)
            }
        }

        // Process each @BindBitmap element.
        for (element in env.getElementsAnnotatedWith(BindBitmap::class.java)) {
            if (!SuperficialValidation.validateElement(element)) continue
            try {
                parseResourceBitmap(element, builderMap, erasedTargetNames)
            } catch (e: Exception) {
                logParsingError(element, BindBitmap::class.java, e)
            }
        }

        // Process each @BindBool element.
        for (element in env.getElementsAnnotatedWith(BindBool::class.java)) {
            if (!SuperficialValidation.validateElement(element)) continue
            try {
                parseResourceBool(element, builderMap, erasedTargetNames)
            } catch (e: Exception) {
                logParsingError(element, BindBool::class.java, e)
            }
        }

        // Process each @BindColor element.
        for (element in env.getElementsAnnotatedWith(BindColor::class.java)) {
            if (!SuperficialValidation.validateElement(element)) continue
            try {
                parseResourceColor(element, builderMap, erasedTargetNames)
            } catch (e: Exception) {
                logParsingError(element, BindColor::class.java, e)
            }
        }

        // Process each @BindDimen element.
        for (element in env.getElementsAnnotatedWith(BindDimen::class.java)) {
            if (!SuperficialValidation.validateElement(element)) continue
            try {
                parseResourceDimen(element, builderMap, erasedTargetNames)
            } catch (e: Exception) {
                logParsingError(element, BindDimen::class.java, e)
            }
        }

        // Process each @BindDrawable element.
        for (element in env.getElementsAnnotatedWith(BindDrawable::class.java)) {
            if (!SuperficialValidation.validateElement(element)) continue
            try {
                parseResourceDrawable(element, builderMap, erasedTargetNames)
            } catch (e: Exception) {
                logParsingError(element, BindDrawable::class.java, e)
            }
        }

        // Process each @BindFloat element.
        for (element in env.getElementsAnnotatedWith(BindFloat::class.java)) {
            if (!SuperficialValidation.validateElement(element)) continue
            try {
                parseResourceFloat(element, builderMap, erasedTargetNames)
            } catch (e: Exception) {
                logParsingError(element, BindFloat::class.java, e)
            }
        }

        // Process each @BindFont element.
        for (element in env.getElementsAnnotatedWith(BindFont::class.java)) {
            if (!SuperficialValidation.validateElement(element)) continue
            try {
                parseResourceFont(element, builderMap, erasedTargetNames)
            } catch (e: Exception) {
                logParsingError(element, BindFont::class.java, e)
            }
        }

        // Process each @BindInt element.
        for (element in env.getElementsAnnotatedWith(BindInt::class.java)) {
            if (!SuperficialValidation.validateElement(element)) continue
            try {
                parseResourceInt(element, builderMap, erasedTargetNames)
            } catch (e: Exception) {
                logParsingError(element, BindInt::class.java, e)
            }
        }

        // Process each @BindString element.
        for (element in env.getElementsAnnotatedWith(BindString::class.java)) {
            if (!SuperficialValidation.validateElement(element)) continue
            try {
                parseResourceString(element, builderMap, erasedTargetNames)
            } catch (e: Exception) {
                logParsingError(element, BindString::class.java, e)
            }
        }

        // Process each @BindView element.
        for (element in env.getElementsAnnotatedWith(BindView::class.java)) {
            // we don't SuperficialValidation.validateElement(element)
            // so that an unresolved View type can be generated by later processing rounds
            try {
                parseBindView(element, builderMap, erasedTargetNames)
            } catch (e: Exception) {
                logParsingError(element, BindView::class.java, e)
            }
        }

        // Process each @BindViews element.
        for (element in env.getElementsAnnotatedWith(BindViews::class.java)) {
            // we don't SuperficialValidation.validateElement(element)
            // so that an unresolved View type can be generated by later processing rounds
            try {
                parseBindViews(element, builderMap, erasedTargetNames)
            } catch (e: Exception) {
                logParsingError(element, BindViews::class.java, e)
            }
        }

        // Process each annotation that corresponds to a listener.
        for (listener in LISTENERS) {
            findAndParseListener(env, listener, builderMap, erasedTargetNames)
        }
        val classpathBindings: MutableMap<TypeElement, ClasspathBindingSet> = findAllSupertypeBindings(builderMap, erasedTargetNames)

        // Associate superclass binders with their subclass binders. This is a queue-based tree walk
        // which starts at the roots (superclasses) and walks to the leafs (subclasses).
        val entries: Deque<Map.Entry<TypeElement, BindingSet.Builder>> = ArrayDeque(builderMap.entries)
        val bindingMap: MutableMap<TypeElement, BindingSet> = LinkedHashMap()
        while (entries.isNotEmpty()) {
            val entry = entries.removeFirst()
            val type = entry.key
            val builder = entry.value
            val parentType: TypeElement? = findParentType(type, erasedTargetNames, classpathBindings.keys)
            if (parentType == null) {
                bindingMap[type] = builder.build()
            } else {
                var parentBinding: BindingInformationProvider? = bindingMap[parentType]
                if (parentBinding == null) parentBinding = classpathBindings[parentType]

                if (parentBinding != null) {
                    builder.setParent(parentBinding)
                    bindingMap[type] = builder.build()
                } else {
                    // Has a superclass binding but we haven't built it yet. Re-enqueue for later.
                    entries.addLast(entry)
                }
            }
        }
        return bindingMap
    }

    private fun logParsingError(element: Element, annotation: Class<out Annotation?>,
                                e: java.lang.Exception) {
        val stackTrace = StringWriter()
        e.printStackTrace(PrintWriter(stackTrace))
        error(element, "Unable to parse @%s binding.\n\n%s", annotation.simpleName, stackTrace)
    }

    private fun isInaccessibleViaGeneratedCode(annotationClass: Class<out Annotation?>,
                                               targetThing: String, element: Element): Boolean {
        var hasError = false
        val enclosingElement = element.enclosingElement as TypeElement

        // Verify field or method modifiers.
        val modifiers = element.modifiers
        if (modifiers.contains(Modifier.PRIVATE) || modifiers.contains(Modifier.STATIC)) {
            error(element, "@%s %s must not be private or static. (%s.%s)",
                    annotationClass.simpleName, targetThing, enclosingElement.qualifiedName,
                    element.simpleName)
            hasError = true
        }

        // Verify containing type.
        if (enclosingElement.kind != ElementKind.CLASS) {
            error(enclosingElement, "@%s %s may only be contained in classes. (%s.%s)",
                    annotationClass.simpleName, targetThing, enclosingElement.qualifiedName,
                    element.simpleName)
            hasError = true
        }

        // Verify containing class visibility is not private.
        if (enclosingElement.modifiers.contains(Modifier.PRIVATE)) {
            error(enclosingElement, "@%s %s may not be contained in private classes. (%s.%s)",
                    annotationClass.simpleName, targetThing, enclosingElement.qualifiedName,
                    element.simpleName)
            hasError = true
        }
        return hasError
    }

    private fun isBindingInWrongPackage(annotationClass: Class<out Annotation?>,
                                        element: Element): Boolean {
        val enclosingElement = element.enclosingElement as TypeElement
        val qualifiedName = enclosingElement.qualifiedName.toString()
        if (qualifiedName.startsWith("android.")) {
            error(element, "@%s-annotated class incorrectly in Android framework package. (%s)",
                    annotationClass.simpleName, qualifiedName)
            return true
        }
        if (qualifiedName.startsWith("java.")) {
            error(element, "@%s-annotated class incorrectly in Java framework package. (%s)",
                    annotationClass.simpleName, qualifiedName)
            return true
        }
        return false
    }

    private fun parseBindView(element: Element, builderMap: MutableMap<TypeElement, BindingSet.Builder>,
                              erasedTargetNames: MutableSet<TypeElement>) {
        val enclosingElement = element.enclosingElement as TypeElement

        // Start by verifying common generated code restrictions.
        var hasError = (isInaccessibleViaGeneratedCode(BindView::class.java, "fields", element)
                || isBindingInWrongPackage(BindView::class.java, element))

        // Verify that the target type extends from View.
        var elementType = element.asType()
        if (elementType.kind == TypeKind.TYPEVAR) {
            val typeVariable = elementType as TypeVariable
            elementType = typeVariable.upperBound
        }
        val qualifiedName = enclosingElement.qualifiedName
        val simpleName = element.simpleName
        if (!ButterKnifeProcessor.isSubtypeOfType(elementType, VIEW_TYPE) && !isInterface(elementType)) {
            if (elementType.kind == TypeKind.ERROR) {
                note(element, "@%s field with unresolved type (%s) "
                        + "must elsewhere be generated as a View or interface. (%s.%s)",
                        BindView::class.java.simpleName, elementType, qualifiedName, simpleName)
            } else {
                error(element, "@%s fields must extend from View or be an interface. (%s.%s)",
                        BindView::class.java.simpleName, qualifiedName, simpleName)
                hasError = true
            }
        }
        if (hasError) {
            return
        }

        // Assemble information on the field.
        val id: Int = element.getAnnotation(BindView::class.java).value
        var builder = builderMap[enclosingElement]
        val resourceId = elementToId(element, BindView::class.java, id)
        if (builder != null) {
            val existingBindingName = builder.findExistingBindingName(resourceId)
            if (existingBindingName != null) {
                error(element, "Attempt to use @%s for an already bound ID %d on '%s'. (%s.%s)",
                        BindView::class.java.simpleName, id, existingBindingName,
                        enclosingElement.qualifiedName, element.simpleName)
                return
            }
        } else {
            builder = getOrCreateBindingBuilder(builderMap, enclosingElement)
        }
        val name = simpleName.toString()
        val type = TypeName.get(elementType)
        val required: Boolean = ButterKnifeProcessor.isFieldRequired(element)
        builder!!.addField(resourceId!!, FieldViewBinding(name, type, required))

        // Add the type-erased version to the valid binding targets set.
        erasedTargetNames.add(enclosingElement)
    }

    private fun parseBindViews(element: Element, builderMap: MutableMap<TypeElement, BindingSet.Builder>,
                               erasedTargetNames: MutableSet<TypeElement>) {
        val enclosingElement = element.enclosingElement as TypeElement

        // Start by verifying common generated code restrictions.
        var hasError = (isInaccessibleViaGeneratedCode(BindViews::class.java, "fields", element)
                || isBindingInWrongPackage(BindViews::class.java, element))

        // Verify that the type is a List or an array.
        val elementType = element.asType()
        val erasedType: String? = doubleErasure(elementType)
        var viewType: TypeMirror? = null

        var kind = FieldCollectionViewBinding.Kind.LIST
        when {
            elementType.kind == TypeKind.ARRAY -> {
                val arrayType = elementType as ArrayType
                viewType = arrayType.componentType
                kind = FieldCollectionViewBinding.Kind.ARRAY
            }
            LIST_TYPE == erasedType -> {
                val declaredType = elementType as DeclaredType
                val typeArguments = declaredType.typeArguments
                if (typeArguments.size != 1) {
                    error(element, "@%s List must have a generic component. (%s.%s)",
                            BindViews::class.java.simpleName, enclosingElement.qualifiedName,
                            element.simpleName)
                    hasError = true
                } else {
                    viewType = typeArguments[0]
                }
                kind = FieldCollectionViewBinding.Kind.LIST
            }
            else -> {
                error(element, "@%s must be a List or array. (%s.%s)", BindViews::class.java.simpleName,
                        enclosingElement.qualifiedName, element.simpleName)
                hasError = true
            }
        }
        if (viewType != null && viewType.kind == TypeKind.TYPEVAR) {
            val typeVariable = viewType as TypeVariable
            viewType = typeVariable.upperBound
        }

        // Verify that the target type extends from View.
        if (viewType != null && !isSubtypeOfType(viewType, VIEW_TYPE) && !isInterface(viewType)) {
            if (viewType.kind == TypeKind.ERROR) {
                note(element, "@%s List or array with unresolved type (%s) "
                        + "must elsewhere be generated as a View or interface. (%s.%s)",
                        BindViews::class.java.simpleName, viewType, enclosingElement.qualifiedName,
                        element.simpleName)
            } else {
                error(element, "@%s List or array type must extend from View or be an interface. (%s.%s)",
                        BindViews::class.java.simpleName, enclosingElement.qualifiedName,
                        element.simpleName)
                hasError = true
            }
        }

        // Assemble information on the field.
        val name = element.simpleName.toString()
        val ids: IntArray = element.getAnnotation(BindViews::class.java).value
        if (ids.isEmpty()) {
            error(element, "@%s must specify at least one ID. (%s.%s)", BindViews::class.java.simpleName,
                    enclosingElement.qualifiedName, element.simpleName)
            hasError = true
        }
        val duplicateId: Int? = findDuplicate(ids)
        duplicateId?.let {
            error(element, "@%s annotation contains duplicate ID %d. (%s.%s)",
                    BindViews::class.java.simpleName, duplicateId, enclosingElement.qualifiedName,
                    element.simpleName)
            hasError = true
        }
        if (!hasError) {
            val type = TypeName.get(Objects.requireNonNull(viewType))
            val required: Boolean = isFieldRequired(element)
            val builder: BindingSet.Builder = getOrCreateBindingBuilder(builderMap, enclosingElement)
            builder.addFieldCollection(FieldCollectionViewBinding(name, type, kind,
                    ArrayList(elementToIds(element, BindViews::class.java, ids).values), required))
            erasedTargetNames.add(enclosingElement)
        }

    }

    private fun parseResourceAnimation(element: Element,
                                       builderMap: MutableMap<TypeElement, BindingSet.Builder>, erasedTargetNames: MutableSet<TypeElement>) {
        var hasError = false
        val enclosingElement = element.enclosingElement as TypeElement

        // Verify that the target type is Animation.
        if (ANIMATION_TYPE != element.asType().toString()) {
            error(element, "@%s field type must be 'Animation'. (%s.%s)",
                    BindAnim::class.java.simpleName, enclosingElement.qualifiedName,
                    element.simpleName)
            hasError = true
        }

        // Verify common generated code restrictions.
        hasError = hasError or isInaccessibleViaGeneratedCode(BindAnim::class.java, "fields", element)
        hasError = hasError or isBindingInWrongPackage(BindAnim::class.java, element)
        if (hasError) {
            return
        }

        // Assemble information on the field.
        val name = element.simpleName.toString()
        val id: Int = element.getAnnotation(BindAnim::class.java).value
        val resourceId = elementToId(element, BindAnim::class.java, id)
        val builder: BindingSet.Builder = getOrCreateBindingBuilder(builderMap, enclosingElement)
        builder.addResource(FieldAnimationBinding(resourceId!!, name))
        erasedTargetNames.add(enclosingElement)
    }

    private fun parseResourceBool(element: Element,
                                  builderMap: MutableMap<TypeElement, BindingSet.Builder>, erasedTargetNames: MutableSet<TypeElement>) {
        var hasError = false
        val enclosingElement = element.enclosingElement as TypeElement

        // Verify that the target type is bool.
        if (element.asType().kind != TypeKind.BOOLEAN) {
            error(element, "@%s field type must be 'boolean'. (%s.%s)",
                    BindBool::class.java.simpleName, enclosingElement.qualifiedName,
                    element.simpleName)
            hasError = true
        }

        // Verify common generated code restrictions.
        hasError = hasError or isInaccessibleViaGeneratedCode(BindBool::class.java, "fields", element)
        hasError = hasError or isBindingInWrongPackage(BindBool::class.java, element)
        if (hasError) {
            return
        }

        // Assemble information on the field.
        val name = element.simpleName.toString()
        val id: Int = element.getAnnotation(BindBool::class.java).value
        val resourceId = elementToId(element, BindBool::class.java, id)
        val builder: BindingSet.Builder = getOrCreateBindingBuilder(builderMap, enclosingElement)
        builder.addResource(
                FieldResourceBinding(resourceId!!, name, FieldResourceBinding.Type.BOOL))
        erasedTargetNames.add(enclosingElement)
    }

    private fun parseResourceColor(element: Element,
                                   builderMap: MutableMap<TypeElement, BindingSet.Builder>, erasedTargetNames: MutableSet<TypeElement>) {
        var hasError = false
        val enclosingElement = element.enclosingElement as TypeElement

        // Verify that the target type is int or ColorStateList.
        var isColorStateList = false
        val elementType = element.asType()
        if (COLOR_STATE_LIST_TYPE == elementType.toString()) {
            isColorStateList = true
        } else if (elementType.kind != TypeKind.INT) {
            error(element, "@%s field type must be 'int' or 'ColorStateList'. (%s.%s)",
                    BindColor::class.java.simpleName, enclosingElement.qualifiedName,
                    element.simpleName)
            hasError = true
        }

        // Verify common generated code restrictions.
        hasError = hasError or isInaccessibleViaGeneratedCode(BindColor::class.java, "fields", element)
        hasError = hasError or isBindingInWrongPackage(BindColor::class.java, element)
        if (hasError) {
            return
        }

        // Assemble information on the field.
        val name = element.simpleName.toString()
        val id: Int = element.getAnnotation(BindColor::class.java).value
        val resourceId = elementToId(element, BindColor::class.java, id)
        val builder: BindingSet.Builder = getOrCreateBindingBuilder(builderMap, enclosingElement)
        val colorStateList = FieldResourceBinding.Type.COLOR_STATE_LIST
        val color = FieldResourceBinding.Type.COLOR
        builder.addResource(FieldResourceBinding(
                resourceId!!,
                name,
                if (isColorStateList) colorStateList else color))
        erasedTargetNames.add(enclosingElement)
    }

    private fun parseResourceDimen(element: Element,
                                   builderMap: MutableMap<TypeElement, BindingSet.Builder>, erasedTargetNames: MutableSet<TypeElement>) {
        var hasError = false
        val enclosingElement = element.enclosingElement as TypeElement

        // Verify that the target type is int or ColorStateList.
        var isInt = false
        val elementType = element.asType()
        if (elementType.kind == TypeKind.INT) {
            isInt = true
        } else if (elementType.kind != TypeKind.FLOAT) {
            error(element, "@%s field type must be 'int' or 'float'. (%s.%s)",
                    BindDimen::class.java.simpleName, enclosingElement.qualifiedName,
                    element.simpleName)
            hasError = true
        }

        // Verify common generated code restrictions.
        hasError = hasError or isInaccessibleViaGeneratedCode(BindDimen::class.java, "fields", element)
        hasError = hasError or isBindingInWrongPackage(BindDimen::class.java, element)
        if (hasError) {
            return
        }

        // Assemble information on the field.
        val name = element.simpleName.toString()
        val id: Int = element.getAnnotation(BindDimen::class.java).value
        val resourceId = elementToId(element, BindDimen::class.java, id)
        val builder: BindingSet.Builder = getOrCreateBindingBuilder(builderMap, enclosingElement)
        builder.addResource(FieldResourceBinding(resourceId!!, name,
                if (isInt) FieldResourceBinding.Type.DIMEN_AS_INT else FieldResourceBinding.Type.DIMEN_AS_FLOAT))
        erasedTargetNames.add(enclosingElement)
    }

    private fun parseResourceBitmap(element: Element,
                                    builderMap: MutableMap<TypeElement, BindingSet.Builder>, erasedTargetNames: MutableSet<TypeElement>) {
        var hasError = false
        val enclosingElement = element.enclosingElement as TypeElement

        // Verify that the target type is Bitmap.
        if (BITMAP_TYPE != element.asType().toString()) {
            error(element, "@%s field type must be 'Bitmap'. (%s.%s)",
                    BindBitmap::class.java.simpleName, enclosingElement.qualifiedName,
                    element.simpleName)
            hasError = true
        }

        // Verify common generated code restrictions.
        hasError = hasError or isInaccessibleViaGeneratedCode(BindBitmap::class.java, "fields", element)
        hasError = hasError or isBindingInWrongPackage(BindBitmap::class.java, element)
        if (hasError) {
            return
        }

        // Assemble information on the field.
        val name = element.simpleName.toString()
        val id: Int = element.getAnnotation(BindBitmap::class.java).value
        val resourceId = elementToId(element, BindBitmap::class.java, id)
        val builder: BindingSet.Builder = getOrCreateBindingBuilder(builderMap, enclosingElement)
        builder.addResource(
                FieldResourceBinding(resourceId!!, name, FieldResourceBinding.Type.BITMAP))
        erasedTargetNames.add(enclosingElement)
    }

    private fun parseResourceDrawable(element: Element, builderMap: MutableMap<TypeElement,
            BindingSet.Builder>, erasedTargetNames: MutableSet<TypeElement>) {
        var hasError = false
        val enclosingElement = element.enclosingElement as TypeElement

        // Verify that the target type is Drawable.
        if (DRAWABLE_TYPE != element.asType().toString()) {
            error(element, "@%s field type must be 'Drawable'. (%s.%s)",
                    BindDrawable::class.java.simpleName, enclosingElement.qualifiedName,
                    element.simpleName)
            hasError = true
        }

        // Verify common generated code restrictions.
        hasError = hasError or isInaccessibleViaGeneratedCode(BindDrawable::class.java, "fields", element)
        hasError = hasError or isBindingInWrongPackage(BindDrawable::class.java, element)
        if (hasError) {
            return
        }

        // Assemble information on the field.
        val name = element.simpleName.toString()
        val id: Int = element.getAnnotation(BindDrawable::class.java).value
        val tint: Int = element.getAnnotation(BindDrawable::class.java).tint
        val resourceIds = elementToIds(element, BindDrawable::class.java, intArrayOf(id, tint))
        val builder: BindingSet.Builder = getOrCreateBindingBuilder(builderMap, enclosingElement)
        resourceIds[id]?.let { it ->
            resourceIds[tint]?.let { tintId ->
                builder.addResource(FieldDrawableBinding(it, name, tintId))
            }
        }

        erasedTargetNames.add(enclosingElement)
    }

    private fun parseResourceFloat(element: Element,
                                   builderMap: MutableMap<TypeElement, BindingSet.Builder>, erasedTargetNames: MutableSet<TypeElement>) {
        var hasError = false
        val enclosingElement = element.enclosingElement as TypeElement

        // Verify that the target type is float.
        if (element.asType().kind != TypeKind.FLOAT) {
            error(element, "@%s field type must be 'float'. (%s.%s)",
                    BindFloat::class.java.simpleName, enclosingElement.qualifiedName,
                    element.simpleName)
            hasError = true
        }

        // Verify common generated code restrictions.
        hasError = hasError or isInaccessibleViaGeneratedCode(BindFloat::class.java, "fields", element)
        hasError = hasError or isBindingInWrongPackage(BindFloat::class.java, element)
        if (hasError) {
            return
        }

        // Assemble information on the field.
        val name = element.simpleName.toString()
        val id: Int = element.getAnnotation(BindFloat::class.java).value
        val resourceId = elementToId(element, BindFloat::class.java, id)
        val builder: BindingSet.Builder = getOrCreateBindingBuilder(builderMap, enclosingElement)
        builder.addResource(
                FieldResourceBinding(resourceId!!, name, FieldResourceBinding.Type.FLOAT))
        erasedTargetNames.add(enclosingElement)
    }

    private fun parseResourceFont(element: Element,
                                  builderMap: MutableMap<TypeElement, BindingSet.Builder>, erasedTargetNames: MutableSet<TypeElement>) {
        var hasError = false
        val enclosingElement = element.enclosingElement as TypeElement

        // Verify that the target type is a Typeface.
        if (TYPEFACE_TYPE != element.asType().toString()) {
            error(element, "@%s field type must be 'Typeface'. (%s.%s)",
                    BindFont::class.java.simpleName, enclosingElement.qualifiedName,
                    element.simpleName)
            hasError = true
        }

        // Verify common generated code restrictions.
        hasError = hasError or isInaccessibleViaGeneratedCode(BindFont::class.java, "fields", element)
        hasError = hasError or isBindingInWrongPackage(BindFont::class.java, element)

        // Assemble information on the field.
        val name = element.simpleName.toString()
        val bindFont = element.getAnnotation(BindFont::class.java)
        val styleValue: Int = bindFont.style
        val style = FieldTypefaceBinding.TypefaceStyles.fromValue(styleValue)
        if (style == null) {
            error(element, "@%s style must be NORMAL, BOLD, ITALIC, or BOLD_ITALIC. (%s.%s)",
                    BindFont::class.java.simpleName, enclosingElement.qualifiedName, name)
            hasError = true
        }
        if (hasError) {
            return
        }
        val builder: BindingSet.Builder = getOrCreateBindingBuilder(builderMap, enclosingElement)
        val resourceId = elementToId(element, BindFont::class.java, bindFont.value)
        builder.addResource(FieldTypefaceBinding(resourceId!!, name, style!!))
        erasedTargetNames.add(enclosingElement)
    }

    private fun parseResourceInt(element: Element,
                                 builderMap: MutableMap<TypeElement, BindingSet.Builder>, erasedTargetNames: MutableSet<TypeElement>) {
        var hasError = false
        val enclosingElement = element.enclosingElement as TypeElement

        // Verify that the target type is int.
        if (element.asType().kind != TypeKind.INT) {
            error(element, "@%s field type must be 'int'. (%s.%s)", BindInt::class.java.simpleName,
                    enclosingElement.qualifiedName, element.simpleName)
            hasError = true
        }

        // Verify common generated code restrictions.
        hasError = hasError or isInaccessibleViaGeneratedCode(BindInt::class.java, "fields", element)
        hasError = hasError or isBindingInWrongPackage(BindInt::class.java, element)
        if (hasError) {
            return
        }

        // Assemble information on the field.
        val name = element.simpleName.toString()
        val id: Int = element.getAnnotation(BindInt::class.java).value
        val resourceId = elementToId(element, BindInt::class.java, id)
        val builder: BindingSet.Builder = getOrCreateBindingBuilder(builderMap, enclosingElement)
        builder.addResource(
                FieldResourceBinding(resourceId!!, name, FieldResourceBinding.Type.INT))
        erasedTargetNames.add(enclosingElement)
    }

    private fun parseResourceString(element: Element,
                                    builderMap: MutableMap<TypeElement, BindingSet.Builder>, erasedTargetNames: MutableSet<TypeElement>) {
        var hasError = false
        val enclosingElement = element.enclosingElement as TypeElement

        // Verify that the target type is String.
        if (STRING_TYPE != element.asType().toString()) {
            error(element, "@%s field type must be 'String'. (%s.%s)",
                    BindString::class.java.simpleName, enclosingElement.qualifiedName,
                    element.simpleName)
            hasError = true
        }

        // Verify common generated code restrictions.
        hasError = hasError or isInaccessibleViaGeneratedCode(BindString::class.java, "fields", element)
        hasError = hasError or isBindingInWrongPackage(BindString::class.java, element)
        if (hasError) {
            return
        }

        // Assemble information on the field.
        val name = element.simpleName.toString()
        val id: Int = element.getAnnotation(BindString::class.java).value
        val resourceId = elementToId(element, BindString::class.java, id)
        val builder: BindingSet.Builder = getOrCreateBindingBuilder(builderMap, enclosingElement)
        builder.addResource(
                FieldResourceBinding(resourceId!!, name, FieldResourceBinding.Type.STRING))
        erasedTargetNames.add(enclosingElement)
    }

    private fun parseResourceArray(element: Element,
                                   builderMap: MutableMap<TypeElement, BindingSet.Builder>, erasedTargetNames: MutableSet<TypeElement>) {
        var hasError = false
        val enclosingElement = element.enclosingElement as TypeElement

        // Verify that the target type is supported.
        val type = getArrayResourceMethodName(element)
        if (type == null) {
            error(element,
                    "@%s field type must be one of: String[], int[], CharSequence[], %s. (%s.%s)",
                    BindArray::class.java.simpleName, TYPED_ARRAY_TYPE, enclosingElement.qualifiedName,
                    element.simpleName)
            hasError = true
        }

        // Verify common generated code restrictions.
        hasError = hasError or isInaccessibleViaGeneratedCode(BindArray::class.java, "fields", element)
        hasError = hasError or isBindingInWrongPackage(BindArray::class.java, element)
        if (hasError) {
            return
        }

        // Assemble information on the field.
        type?.let {
            val name = element.simpleName.toString()
            val id: Int = element.getAnnotation(BindArray::class.java).value
            val resourceId = elementToId(element, BindArray::class.java, id)
            val builder: BindingSet.Builder = getOrCreateBindingBuilder(builderMap, enclosingElement)
            builder.addResource(FieldResourceBinding(resourceId!!, name, it))
            erasedTargetNames.add(enclosingElement)
        }
    }

    /** Uses both [Types.erasure] and string manipulation to strip any generic types.  */
    private fun doubleErasure(elementType: TypeMirror): String? {
        var name = typeUtils?.erasure(elementType).toString()
        val typeParamStart = name.indexOf('<')
        if (typeParamStart != -1) {
            name = name.substring(0, typeParamStart)
        }
        return name
    }

    private fun findAndParseListener(env: RoundEnvironment,
                                     annotationClass: Class<out Annotation?>,
                                     builderMap: MutableMap<TypeElement, BindingSet.Builder>, erasedTargetNames: MutableSet<TypeElement>) {
        for (element in env.getElementsAnnotatedWith(annotationClass)) {
            if (!SuperficialValidation.validateElement(element)) continue
            try {
                parseListenerAnnotation(annotationClass, element, builderMap, erasedTargetNames)
            } catch (e: java.lang.Exception) {
                val stackTrace = StringWriter()
                e.printStackTrace(PrintWriter(stackTrace))
                error(element, "Unable to generate view binder for @%s.\n\n%s",
                        annotationClass.simpleName, stackTrace.toString())
            }
        }
    }

    @Throws(java.lang.Exception::class)
    private fun parseListenerAnnotation(annotationClass: Class<out Annotation?>, element: Element,
                                        builderMap: MutableMap<TypeElement, BindingSet.Builder>, erasedTargetNames: MutableSet<TypeElement>) {
        // This should be guarded by the annotation's @Target but it's worth a check for safe casting.
        check(!(element !is ExecutableElement || element.getKind() != ElementKind.METHOD)) { String.format("@%s annotation must be on a method.", annotationClass.simpleName) }
        val executableElement = element
        val enclosingElement = element.getEnclosingElement() as TypeElement

        // Assemble information on the method.
        val annotation: Annotation? = element.getAnnotation(annotationClass)
        val annotationValue = annotationClass.getDeclaredMethod("value")
        check(annotationValue.returnType == IntArray::class.java) { String.format("@%s annotation value() type not int[].", annotationClass) }
        val ids = annotationValue.invoke(annotation) as IntArray
        val name = executableElement.simpleName.toString()
        val required: Boolean = ButterKnifeProcessor.isListenerRequired(executableElement)

        // Verify that the method and its containing class are accessible via generated code.
        var hasError = isInaccessibleViaGeneratedCode(annotationClass, "methods", element)
        hasError = hasError or isBindingInWrongPackage(annotationClass, element)
        val duplicateId = findDuplicate(ids)
        if (duplicateId != null) {
            error(element, "@%s annotation for method contains duplicate ID %d. (%s.%s)",
                    annotationClass.simpleName, duplicateId, enclosingElement.qualifiedName,
                    element.getSimpleName())
            hasError = true
        }
        val listener = annotationClass.getAnnotation(ListenerClass::class.java)
                ?: throw IllegalStateException(String.format("No @%s defined on @%s.", ListenerClass::class.java.simpleName,
                        annotationClass.simpleName))
        for (id in ids) {
            if (id == NO_ID.value) {
                if (ids.size == 1) {
                    if (!required) {
                        error(element, "ID-free binding must not be annotated with @Optional. (%s.%s)",
                                enclosingElement.qualifiedName, element.getSimpleName())
                        hasError = true
                    }
                } else {
                    error(element, "@%s annotation contains invalid ID %d. (%s.%s)",
                            annotationClass.simpleName, id, enclosingElement.qualifiedName,
                            element.getSimpleName())
                    hasError = true
                }
            }
        }
        val method: ListenerMethod
        val methods: Array<ListenerMethod> = listener.method
        check(methods.size <= 1) {
            String.format("Multiple listener methods specified on @%s.",
                    annotationClass.simpleName)
        }
        if (methods.size == 1) {
            check(listener.callbacks == ListenerClass.NONE::class.java) {
                String.format("Both method() and callback() defined on @%s.",
                        annotationClass.simpleName)
            }
            method = methods[0]
        } else {
            val annotationCallback = annotationClass.getDeclaredMethod("callback")
            val callback = annotationCallback.invoke(annotation) as Enum<*>
            val callbackField: Field = callback.javaClass.getField(callback.name)
            method = callbackField.getAnnotation(ListenerMethod::class.java)
            method?.let {
                String.format("No @%s defined on @%s's %s.%s.", ListenerMethod::class.java.simpleName,
                        annotationClass.simpleName, callback.javaClass.simpleName,
                        callback.name)
            }
        }

        // Verify that the method has equal to or less than the number of parameters as the listener.
        val methodParameters = executableElement.parameters
        if (methodParameters.size > method.parameters.size) {
            error(element, "@%s methods can have at most %s parameter(s). (%s.%s)",
                    annotationClass.simpleName, method.parameters.size,
                    enclosingElement.qualifiedName, element.getSimpleName())
            hasError = true
        }

        // Verify method return type matches the listener.
        var returnType = executableElement.returnType
        if (returnType is TypeVariable) {
            returnType = returnType.upperBound
        }
        val returnTypeString = returnType.toString()
        val hasReturnValue = "void" != returnTypeString
        if (returnTypeString != method.returnType && hasReturnValue) {
            error(element, "@%s methods must have a '%s' return type. (%s.%s)",
                    annotationClass.simpleName, method.returnType,
                    enclosingElement.qualifiedName, element.getSimpleName())
            hasError = true
        }
        if (hasError) {
            return
        }
        var parameters = mutableListOf<Parameter>()
        if (methodParameters.isNotEmpty()) {
            val methodParameterUsed = BitSet(methodParameters.size)
            val parameterTypes: Array<String> = method.parameters
            for (i in methodParameters.indices) {
                val methodParameter = methodParameters[i]
                var methodParameterType = methodParameter.asType()
                if (methodParameterType is TypeVariable) {
                    methodParameterType = methodParameterType.upperBound
                }
                for (j in parameterTypes.indices) {
                    if (methodParameterUsed[j]) {
                        continue
                    }
                    if ((isSubtypeOfType(methodParameterType!!, parameterTypes[j])
                                    && isSubtypeOfType(methodParameterType, VIEW_TYPE))
                            || isTypeEqual(methodParameterType, parameterTypes[j])
                            || isInterface(methodParameterType)) {
                        parameters[i] = Parameter(j, TypeName.get(methodParameterType))
                        methodParameterUsed.set(j)
                        break
                    }
                }
                if (parameters[i] == null) {
                    val builder = java.lang.StringBuilder()
                    builder.append("Unable to match @")
                            .append(annotationClass.simpleName)
                            .append(" method arguments. (")
                            .append(enclosingElement.qualifiedName)
                            .append('.')
                            .append(element.getSimpleName())
                            .append(')')
                    for (j in parameters.indices) {
                        val parameter = parameters[j]
                        builder.append("\n\n  Parameter #")
                                .append(j + 1)
                                .append(": ")
                                .append(methodParameters[j].asType().toString())
                                .append("\n    ")
                        if (parameter == null) {
                            builder.append("did not match any listener parameters")
                        } else {
                            builder.append("matched listener parameter #")
                                    .append(parameter.listenerPosition + 1)
                                    .append(": ").append(parameter.type)
                        }
                    }
                    builder.append("\n\nMethods may have up to ")
                            .append(method.parameters.size)
                            .append(" parameter(s):\n")
                    for (parameterType in method.parameters) {
                        builder.append("\n  ").append(parameterType)
                    }
                    builder.append(
                            "\n\nThese may be listed in any order but will be searched for from top to bottom.")
                    error(executableElement, builder.toString())
                    return
                }
            }
        }
        val binding = MethodViewBinding(name, parameters, required, hasReturnValue)
        val builder: BindingSet.Builder = getOrCreateBindingBuilder(builderMap, enclosingElement)
        val resourceIds = elementToIds(element, annotationClass, ids)
        for ((key, value) in resourceIds!!) {
            if (!builder.addMethod(value, listener, method, binding)) {
                error(element, "Multiple listener methods with return value specified for ID %d. (%s.%s)",
                        key, enclosingElement.qualifiedName, element.getSimpleName())
                return
            }
        }

        // Add the type-erased version to the valid binding targets set.
        erasedTargetNames.add(enclosingElement)
    }

    private fun isInterface(typeMirror: TypeMirror): Boolean {
        return (typeMirror is DeclaredType
                && typeMirror.asElement().kind == ElementKind.INTERFACE)
    }

    private fun getOrCreateBindingBuilder(
            builderMap: MutableMap<TypeElement, BindingSet.Builder>, enclosingElement: TypeElement): BindingSet.Builder {
        var builder = builderMap[enclosingElement]
        if (builder == null) {
            builder = BindingSet.newBuilder(enclosingElement)
            builderMap[enclosingElement] = builder
        }
        return builder
    }

    /** Finds the parent binder type in the supplied sets, if any.  */
    private fun findParentType(typeElement: TypeElement, parents: MutableSet<TypeElement>,
                               classpathParents: MutableSet<TypeElement>): TypeElement? {
        var typeElement: TypeElement? = typeElement
        while (true) {
            typeElement = getSuperClass(typeElement)
            if (typeElement == null || parents.contains(typeElement)
                    || classpathParents.contains(typeElement)) {
                return typeElement
            }
        }
    }

    private fun findAllSupertypeBindings(builderMap: MutableMap<TypeElement, BindingSet.Builder>,
                                         processedInThisRound: MutableSet<TypeElement>): MutableMap<TypeElement, ClasspathBindingSet> {
        val classpathBindings: MutableMap<TypeElement, ClasspathBindingSet?> = mutableMapOf()
        val supportedAnnotations = getSupportedAnnotations()
        val requireViewInConstructor: Set<Class<out Annotation?>> = ImmutableSet.builder<Class<out Annotation?>>()
                .addAll(LISTENERS).add(BindView::class.java).add(BindViews::class.java).build()
        supportedAnnotations.removeAll(requireViewInConstructor)

        for (typeElement in builderMap.keys) {
            // Make sure to process superclass before subclass. This is because if there is a class that
            // requires a View in the constructor, all subclasses need it as well.
            val superClasses: Deque<TypeElement> = ArrayDeque()
            var superClass = getSuperClass(typeElement)
            while (superClass != null && !processedInThisRound.contains(superClass)
                    && !classpathBindings.containsKey(superClass)) {
                superClasses.addFirst(superClass)
                superClass = getSuperClass(superClass)
            }
            var parentHasConstructorWithView = false
            while (!superClasses.isEmpty()) {
                val superclass = superClasses.removeFirst()
                val classpathBinding = findBindingInfoForType(superclass, requireViewInConstructor, supportedAnnotations,
                        parentHasConstructorWithView)
                if (classpathBinding != null) {
                    parentHasConstructorWithView = parentHasConstructorWithView or classpathBinding.constructorNeedsView()
                    classpathBindings[superclass] = classpathBinding
                }
            }
        }
        return ImmutableMap.copyOf(classpathBindings)
    }

    private fun findBindingInfoForType(
            typeElement: TypeElement, requireConstructorWithView: Set<Class<out Annotation?>>,
            otherAnnotations: Set<Class<out Annotation?>>, needsConstructorWithView: Boolean): ClasspathBindingSet? {
        var foundSupportedAnnotation = false
        for (enclosedElement in typeElement.enclosedElements) {
            for (bindViewAnnotation in requireConstructorWithView) {
                if (enclosedElement.getAnnotation(bindViewAnnotation) != null) {
                    return ClasspathBindingSet(true, typeElement)
                }
            }
            for (supportedAnnotation in otherAnnotations) {
                if (enclosedElement.getAnnotation(supportedAnnotation) != null) {
                    if (needsConstructorWithView) {
                        return ClasspathBindingSet(true, typeElement)
                    }
                    foundSupportedAnnotation = true
                }
            }
        }
        return if (foundSupportedAnnotation) {
            ClasspathBindingSet(false, typeElement)
        } else {
            null
        }
    }

    private fun getSuperClass(typeElement: TypeElement?): TypeElement? {
        val type = typeElement!!.superclass
        return if (type.kind == TypeKind.NONE) {
            null
        } else (type as DeclaredType).asElement() as TypeElement
    }

    override fun getSupportedSourceVersion(): SourceVersion? {
        return SourceVersion.latestSupported()
    }

    private fun error(element: Element, message: String, vararg args: Any?) {
        printMessage(Diagnostic.Kind.ERROR, element, message, args)
    }

    private fun note(element: Element, message: String, vararg args: Any) {
        printMessage(Diagnostic.Kind.NOTE, element, message, args)
    }

    private fun printMessage(kind: Diagnostic.Kind, element: Element, message: String, vararg args: Any?) {
        var message: String = message
        if (!args.isNullOrEmpty()) {
            message = String.format(message, *args)
        }
        processingEnv.messager.printMessage(kind, message, element)
    }

    private fun elementToId(element: Element, annotation: Class<out Annotation?>, value: Int): Id? {
        val tree = trees?.getTree(element, getMirror(element, annotation)) as JCTree
        tree?.let { // tree can be null if the references are compiled types and not source
            rScanner.reset()
            it.accept(rScanner)
            if (!rScanner.resourceIds.isEmpty()) {
                return rScanner.resourceIds.values.iterator().next()
            }
        }
        return Id(value)
    }

    private fun elementToIds(element: Element, annotation: Class<out Annotation?>,
                             values: IntArray): MutableMap<Int, Id> {
        var resourceIds: MutableMap<Int, Id> = mutableMapOf()
        val tree = trees?.getTree(element, getMirror(element, annotation)) as JCTree
        if (tree != null) { // tree can be null if the references are compiled types and not source
            rScanner.reset()
            tree.accept(rScanner)
            resourceIds = rScanner.resourceIds
        }

        // Every value looked up should have an Id
        for (value in values) {
            resourceIds.putIfAbsent(value, Id(value))
        }
        return resourceIds
    }

    inner class RScanner : TreeScanner() {
        var resourceIds: MutableMap<Int, Id> = LinkedHashMap()
        override fun visitIdent(jcIdent: JCIdent) {
            super.visitIdent(jcIdent)
            val symbol = jcIdent.sym
            if (symbol.type is JCPrimitiveType) {
                val id = parseId(symbol)
                if (id != null) {
                    resourceIds[id.value] = id
                }
            }
        }

        override fun visitSelect(jcFieldAccess: JCFieldAccess) {
            val symbol = jcFieldAccess.sym
            val id = parseId(symbol)
            if (id != null) {
                resourceIds[id.value] = id
            }
        }

        private fun parseId(symbol: Symbol): Id? {
            var id: Id? = null
            if (symbol.enclosingElement != null && symbol.enclosingElement.enclosingElement != null && symbol.enclosingElement.enclosingElement.enclClass() != null) {
                try {
                    val value = Objects.requireNonNull((symbol as VarSymbol).constantValue) as Int
                    id = Id(value, symbol)
                } catch (ignored: java.lang.Exception) {
                }
            }
            return id
        }

        override fun visitLiteral(jcLiteral: JCLiteral) {
            try {
                val value = jcLiteral.value as Int
                resourceIds[value] = Id(value)
            } catch (ignored: java.lang.Exception) {
            }
        }

        fun reset() {
            resourceIds.clear()
        }
    }
}