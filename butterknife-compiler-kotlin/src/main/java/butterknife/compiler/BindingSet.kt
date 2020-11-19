package butterknife.compiler

import butterknife.OnTouch
import butterknife.internal.ListenerClass
import butterknife.internal.ListenerMethod
import com.google.auto.common.MoreElements
import com.google.common.collect.ImmutableList
import com.squareup.javapoet.*
import java.lang.Deprecated
import java.util.*
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import kotlin.Any
import kotlin.Array
import kotlin.AssertionError
import kotlin.Boolean
import kotlin.Enum
import kotlin.IllegalStateException
import kotlin.Int
import kotlin.String
import kotlin.let
import kotlin.reflect.KClass
import kotlin.toString

class BindingSet(private val targetTypeName: TypeName?, private val bindingClassName: ClassName?,
                 private val enclosingElement: TypeElement?, private val isFinal: Boolean,
                 private val isView: Boolean, private val isActivity: Boolean, private val isDialog: Boolean,
                 private val viewBindings: ImmutableList<ViewBinding>,
                 private val collectionBindings: ImmutableList<FieldCollectionViewBinding>,
                 private val resourceBindings: ImmutableList<ResourceBinding>,
                 private val parentBinding: BindingInformationProvider?) : BindingInformationProvider {
    companion object {
        val UTILS = ClassName.get("butterknife.internal", "Utils")
        private val VIEW = ClassName.get("android.view", "View")
        private val CONTEXT = ClassName.get("android.content", "Context")
        private val RESOURCES = ClassName.get("android.content.res", "Resources")
        private val UI_THREAD = ClassName.get("androidx.annotation", "UiThread")
        private val CALL_SUPER = ClassName.get("androidx.annotation", "CallSuper")
        private val SUPPRESS_LINT = ClassName.get("android.annotation", "SuppressLint")
        private val UNBINDER = ClassName.get("butterknife", "Unbinder")
        val BITMAP_FACTORY = ClassName.get("android.graphics", "BitmapFactory")
        val CONTEXT_COMPAT = ClassName.get("androidx.core.content", "ContextCompat")
        val ANIMATION_UTILS = ClassName.get("android.view.animation", "AnimationUtils")

        fun asHumanDescription(bindings: Collection<MemberViewBinding>): String {
            val iterator = bindings.iterator()
            return when (bindings.size) {
                1 -> iterator.next().description()
                2 -> iterator.next().description() + " and " + iterator.next().description()
                else -> {
                    val builder = StringBuilder()
                    var i = 0
                    val count = bindings.size
                    while (iterator.hasNext()) {
                        if (i != 0) {
                            builder.append(", ")
                        }
                        if (i == count - 1) {
                            builder.append("and ")
                        }
                        builder.append(iterator.next().description())
                        i++
                    }
                    builder.toString()
                }
            }
        }

        private fun bestGuess(type: String): TypeName {
            return when (type) {
                "void" -> TypeName.VOID
                "boolean" -> TypeName.BOOLEAN
                "byte" -> TypeName.BYTE
                "char" -> TypeName.CHAR
                "double" -> TypeName.DOUBLE
                "float" -> TypeName.FLOAT
                "int" -> TypeName.INT
                "long" -> TypeName.LONG
                "short" -> TypeName.SHORT
                else -> {
                    var left = type.indexOf('<')
                    if (left != -1) {
                        val typeClassName = ClassName.bestGuess(type.substring(0, left))
                        val typeArguments: MutableList<TypeName> = ArrayList()
                        do {
                            typeArguments.add(WildcardTypeName.subtypeOf(Any::class.java))
                            left = type.indexOf('<', left + 1)
                        } while (left != -1)
                        return ParameterizedTypeName.get(typeClassName,
                                *typeArguments.toTypedArray())
                    }
                    ClassName.bestGuess(type)
                }
            }
        }

        fun requiresCast(type: TypeName): Boolean {
            return ButterKnifeProcessor.VIEW_TYPE != type.toString()
        }

        fun newBuilder(enclosingElement: TypeElement): Builder {
            val typeMirror = enclosingElement.asType()
            val isView = ButterKnifeProcessor.isSubtypeOfType(typeMirror, ButterKnifeProcessor.VIEW_TYPE)
            val isActivity = ButterKnifeProcessor.isSubtypeOfType(typeMirror, ButterKnifeProcessor.ACTIVITY_TYPE)
            val isDialog = ButterKnifeProcessor.isSubtypeOfType(typeMirror, ButterKnifeProcessor.DIALOG_TYPE)
            var targetType = TypeName.get(typeMirror)
            if (targetType is ParameterizedTypeName) {
                targetType = targetType.rawType
            }
            val bindingClassName = getBindingClassName(enclosingElement)
            val isFinal = enclosingElement.modifiers.contains(Modifier.FINAL)
            return Builder(targetType, bindingClassName, enclosingElement, isFinal, isView, isActivity,
                    isDialog)
        }

        fun getBindingClassName(typeElement: TypeElement): ClassName {
            val packageName = MoreElements.getPackage(typeElement).qualifiedName.toString()
            val className = typeElement.qualifiedName.toString().substring(
                    packageName.length + 1).replace('.', '$')
            return ClassName.get(packageName, className + "_ViewBinding")
        }
    }

    override fun getBindingClassName(): ClassName? {
        return bindingClassName
    }

    fun brewJava(sdk: Int, debuggable: Boolean): JavaFile? {
        val bindingConfiguration = createType(sdk, debuggable)
        return JavaFile.builder(bindingClassName!!.packageName(), bindingConfiguration)
                .addFileComment("Generated code from Butter Knife. Do not modify!")
                .build()
    }

    private fun createType(sdk: Int, debuggable: Boolean): TypeSpec {
        val result = TypeSpec.classBuilder(bindingClassName!!.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .addOriginatingElement(enclosingElement)
        if (isFinal) result.addModifiers(Modifier.FINAL)

        if (parentBinding != null) {
            result.superclass(parentBinding.getBindingClassName())
        } else {
            result.addSuperinterface(UNBINDER)
        }
        if (hasTargetField()) result.addField(targetTypeName, "target", Modifier.PRIVATE)
        // Add a delegating constructor with a target type + view signature for reflective use.
        when {
            isView -> {
                result.addMethod(createBindingConstructorForView())
            }
            isActivity -> {
                result.addMethod(createBindingConstructorForActivity())
            }
            isDialog -> {
                result.addMethod(createBindingConstructorForDialog())
            }
        }
        if (!constructorNeedsView()) {
            // Add a delegating constructor with a target type + view signature for reflective use.
            result.addMethod(createBindingViewDelegateConstructor())
        }
        result.addMethod(createBindingConstructor(sdk, debuggable))
        if (hasViewBindings() || parentBinding == null) {
            result.addMethod(createBindingUnbindMethod(result))
        }
        return result.build()
    }

    private fun createBindingViewDelegateConstructor(): MethodSpec? {
        return MethodSpec.constructorBuilder()
                .addJavadoc("""@deprecated Use {@link #${"$"}T(${"$"}T, ${"$"}T)} for direct creation.
    Only present for runtime invocation through {@code ButterKnife.bind()}.
""",
                        bindingClassName, targetTypeName, CONTEXT)
                .addAnnotation(Deprecated::class.java)
                .addAnnotation(UI_THREAD)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(targetTypeName, "target")
                .addParameter(VIEW, "source")
                .addStatement("this(target, source.getContext())")
                .build()
    }

    private fun createBindingConstructorForView(): MethodSpec? {
        val builder = MethodSpec.constructorBuilder()
                .addAnnotation(UI_THREAD)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(targetTypeName, "target")
        if (constructorNeedsView()) {
            builder.addStatement("this(target, target)")
        } else {
            builder.addStatement("this(target, target.getContext())")
        }
        return builder.build()
    }

    private fun createBindingConstructorForActivity(): MethodSpec? {
        val builder = MethodSpec.constructorBuilder()
                .addAnnotation(UI_THREAD)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(targetTypeName, "target")
        if (constructorNeedsView()) {
            builder.addStatement("this(target, target.getWindow().getDecorView())")
        } else {
            builder.addStatement("this(target, target)")
        }
        return builder.build()
    }

    private fun createBindingConstructorForDialog(): MethodSpec? {
        val builder = MethodSpec.constructorBuilder()
                .addAnnotation(UI_THREAD)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(targetTypeName, "target")
        if (constructorNeedsView()) {
            builder.addStatement("this(target, target.getWindow().getDecorView())")
        } else {
            builder.addStatement("this(target, target.getContext())")
        }
        return builder.build()
    }

    private fun createBindingConstructor(sdk: Int, debuggable: Boolean): MethodSpec? {
        val constructor = MethodSpec.constructorBuilder()
                .addAnnotation(UI_THREAD)
                .addModifiers(Modifier.PUBLIC)
        if (hasMethodBindings()) {
            constructor.addParameter(targetTypeName, "target", Modifier.FINAL)
        } else {
            constructor.addParameter(targetTypeName, "target")
        }
        if (constructorNeedsView()) {
            constructor.addParameter(VIEW, "source")
        } else {
            constructor.addParameter(CONTEXT, "context")
        }
        if (hasUnqualifiedResourceBindings()) {
            // Aapt can change IDs out from underneath us, just suppress since all will work at runtime.
            constructor.addAnnotation(AnnotationSpec.builder(SuppressWarnings::class.java)
                    .addMember("value", "\$S", "ResourceType")
                    .build())
        }
        if (hasOnTouchMethodBindings()) {
            constructor.addAnnotation(AnnotationSpec.builder(SUPPRESS_LINT)
                    .addMember("value", "\$S", "ClickableViewAccessibility")
                    .build())
        }
        parentBinding?.let {
            when {
                it.constructorNeedsView() -> {
                    constructor.addStatement("super(target, source)")
                }
                constructorNeedsView() -> {
                    constructor.addStatement("super(target, source.getContext())")
                }
                else -> {
                    constructor.addStatement("super(target, context)")
                }
            }
            constructor.addCode("\n")
        }
        if (hasTargetField()) {
            constructor.addStatement("this.target = target")
            constructor.addCode("\n")
        }
        if (hasViewBindings()) {
            if (hasViewLocal()) {
                // Local variable in which all views will be temporarily stored.
                constructor.addStatement("\$T view", VIEW)
            }
            for (binding in viewBindings) {
                addViewBinding(constructor, binding, debuggable)
            }
            for (binding in collectionBindings) {
                constructor.addStatement("\$L", binding.render(debuggable))
            }
            if (resourceBindings.isNotEmpty()) constructor.addCode("\n")
        }
        if (resourceBindings.isNotEmpty()) {
            if (constructorNeedsView()) {
                constructor.addStatement("\$T context = source.getContext()", CONTEXT)
            }
            if (hasResourceBindingsNeedingResource(sdk)) {
                constructor.addStatement("\$T res = context.getResources()", RESOURCES)
            }
            for (binding in resourceBindings) {
                constructor.addStatement("\$L", binding.render(sdk))
            }
        }
        return constructor.build()
    }

    private fun createBindingUnbindMethod(bindingClass: TypeSpec.Builder): MethodSpec? {
        val result = MethodSpec.methodBuilder("unbind")
                .addAnnotation(Override::class.java)
                .addModifiers(Modifier.PUBLIC)
        if (!isFinal && parentBinding == null) result.addAnnotation(CALL_SUPER)
        if (hasTargetField()) {
            if (hasFieldBindings()) result.addStatement("\$T target = this.target", targetTypeName)
            result.addStatement("if (target == null) throw new \$T(\$S)", IllegalStateException::class.java,
                    "Bindings already cleared.")
            result.addStatement("\$N = null", if (hasFieldBindings()) "this.target" else "target")
            result.addCode("\n")

            for (binding in viewBindings) {
                binding.fieldBinding?.let {
                    result.addStatement("target.\$L = null", it.name)
                }
            }
            for (binding in collectionBindings) {
                result.addStatement("target.\$L = null", binding.name)
            }
        }
        if (hasMethodBindings()) {
            result.addCode("\n")
            for (binding in viewBindings) {
                addFieldAndUnbindStatement(bindingClass, result, binding)
            }
        }
        if (parentBinding != null) {
            result.addCode("\n")
            result.addStatement("super.unbind()")
        }
        return result.build()
    }

    private fun addFieldAndUnbindStatement(result: TypeSpec.Builder, unbindMethod: MethodSpec.Builder,
                                           bindings: ViewBinding) {
        // Only add fields to the binding if there are method bindings.
        val classMethodBindings: Map<ListenerClass, Map<ListenerMethod, Set<MethodViewBinding>>> = bindings.methodBindings
        if (classMethodBindings.isEmpty()) return
        val fieldName = if (bindings.isBoundToRoot) "viewSource" else "view" + bindings.id.value.toString(16)
        result.addField(VIEW, fieldName, Modifier.PRIVATE)

        // We only need to emit the null check if there are zero required bindings.
        val needsNullChecked = bindings.requiredBindings.isEmpty()
        if (needsNullChecked) unbindMethod.beginControlFlow("if (\$N != null)", fieldName)

        for (listenerClass in classMethodBindings.keys) {
            // We need to keep a reference to the listener
            // in case we need to unbind it via a remove method.
            val requiresRemoval = listenerClass.remover.isNotEmpty()
            var listenerField = "null"
            if (requiresRemoval) {
                val listenerClassName = bestGuess(listenerClass.type)
                listenerField = fieldName + (listenerClassName as ClassName).simpleName()
                result.addField(listenerClassName, listenerField, Modifier.PRIVATE)
            }
            val targetType: String = listenerClass.targetType
            if (ButterKnifeProcessor.VIEW_TYPE != targetType) {
                unbindMethod.addStatement("((\$T) \$N).\$N(\$N)", bestGuess(targetType),
                        fieldName, removerOrSetter(listenerClass, requiresRemoval), listenerField)
            } else {
                unbindMethod.addStatement("\$N.\$N(\$N)", fieldName,
                        removerOrSetter(listenerClass, requiresRemoval), listenerField)
            }
            if (requiresRemoval) unbindMethod.addStatement("\$N = null", listenerField)
        }
        unbindMethod.addStatement("\$N = null", fieldName)
        if (needsNullChecked) {
            unbindMethod.endControlFlow()
        }
    }

    private fun removerOrSetter(listenerClass: ListenerClass, requiresRemoval: Boolean): String? {
        return if (requiresRemoval) listenerClass.remover else listenerClass.setter
    }

    private fun addViewBinding(result: MethodSpec.Builder, binding: ViewBinding, debuggable: Boolean) {
        if (binding.isSingleFieldBinding) {
            // Optimize the common case where there's a single binding directly to a field.
            val builder = CodeBlock.builder()
            binding.fieldBinding?.let {
                builder.add("target.\$L = ", it.name)
                val requiresCast: Boolean = requiresCast(it.type)
                if (!debuggable || !requiresCast && !it.isRequired) {
                    if (requiresCast) builder.add("(\$T) ", it.type)
                    builder.add("source.findViewById(\$L)", binding.id.code)
                } else {
                    builder.add("\$T.find", UTILS)
                    builder.add(if (it.isRequired) "RequiredView" else "OptionalView")
                    if (requiresCast) builder.add("AsType")
                    builder.add("(source, \$L", binding.id.code)

                    if (it.isRequired || requiresCast) builder.add(", \$S", asHumanDescription(listOf(it)))
                    if (requiresCast) builder.add(", \$T.class", it.rawType)
                    builder.add(")")
                }
            }

            result.addStatement("\$L", builder.build())
            return
        }
        val requiredBindings: List<MemberViewBinding> = binding.requiredBindings
        if (!debuggable || requiredBindings.isEmpty()) {
            result.addStatement("view = source.findViewById(\$L)", binding.id.code)
        } else if (!binding.isBoundToRoot) {
            result.addStatement("view = \$T.findRequiredView(source, \$L, \$S)", UTILS,
                    binding.id.code, asHumanDescription(requiredBindings))
        }
        addFieldBinding(result, binding, debuggable)
        addMethodBindings(result, binding, debuggable)
    }

    private fun addFieldBinding(result: MethodSpec.Builder, binding: ViewBinding, debuggable: Boolean) {
        binding.fieldBinding?.let {
            if (requiresCast(it.type)) {
                if (debuggable) {
                    result.addStatement("target.\$L = \$T.castView(view, \$L, \$S, \$T.class)",
                            it.name, UTILS, binding.id.code,
                            asHumanDescription(listOf(it)), it.rawType)
                } else {
                    result.addStatement("target.\$L = (\$T) view", it.name, it.type)
                }
            } else {
                result.addStatement("target.\$L = view", it.name)
            }
        }
    }

    private fun addMethodBindings(result: MethodSpec.Builder, binding: ViewBinding, debuggable: Boolean) {
        val classMethodBindings: Map<ListenerClass, Map<ListenerMethod, Set<MethodViewBinding>>> = binding.methodBindings
        if (classMethodBindings.isEmpty()) return

        // We only need to emit the null check if there are zero required bindings.
        val needsNullChecked = binding.requiredBindings.isEmpty()
        if (needsNullChecked) result.beginControlFlow("if (view != null)")

        // Add the view reference to the binding.
        var fieldName = if (binding.isBoundToRoot) "viewSource" else "view" + binding.id.value.toString(16)
        var bindName = if (binding.isBoundToRoot) "source" else "view"
        result.addStatement("\$L = \$N", fieldName, bindName)

        for ((listener, methodBindings) in classMethodBindings) {
            val callback = TypeSpec.anonymousClassBuilder("")
                    .superclass(ClassName.bestGuess(listener.type))
            for (method in getListenerMethods(listener)) {
                val callbackMethod: MethodSpec.Builder = MethodSpec.methodBuilder(method.name)
                        .addAnnotation(Override::class.java)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(bestGuess(method.returnType))

                val parameterTypes: Array<String> = method.parameters
                for (i in parameterTypes.indices) {
                    callbackMethod.addParameter(bestGuess(parameterTypes[i]), "p$i")
                }

                var hasReturnValue = false
                val builder = CodeBlock.builder()
                val methodViewBindings = methodBindings[method]
                if (methodViewBindings != null) {
                    for (methodBinding in methodViewBindings) {
                        if (methodBinding.hasReturnValue) {
                            hasReturnValue = true
                            builder.add("return ") // TODO what about multiple methods?
                        }
                        builder.add("target.\$L(", methodBinding.name)
                        val parameters = methodBinding.parameters
                        val listenerParameters: Array<String> = method.parameters
                        var i = 0

                        parameters?.forEach {
                            if (i > 0) {
                                builder.add(", ")
                            }
                            val listenerPosition: Int = it.listenerPosition
                            if (it.requiresCast(listenerParameters[listenerPosition])) {
                                if (debuggable) {
                                    builder.add("\$T.castParam(p\$L, \$S, \$L, \$S, \$L, \$T.class)", UTILS,
                                            listenerPosition, method.name, listenerPosition, methodBinding.name, i,
                                            it.type)
                                } else {
                                    builder.add("(\$T) p\$L", it.type, listenerPosition)
                                }
                            } else {
                                builder.add("p\$L", listenerPosition)
                            }
                            i++
                        }

                        builder.add(");\n")
                    }
                }
                if ("void" != method.returnType && !hasReturnValue) {
                    builder.add("return \$L;\n", method.defaultReturn)
                }
                callbackMethod.addCode(builder.build())
                callback.addMethod(callbackMethod.build())
            }
            val requiresRemoval = listener.remover.isNotEmpty()
            var listenerField: String? = null
            if (requiresRemoval) {
                val listenerClassName: TypeName? = bestGuess(listener.type)
                listenerField = fieldName + (listenerClassName as ClassName).simpleName()
                result.addStatement("\$L = \$L", listenerField, callback.build())
            }
            val targetType: String = listener.targetType
            if (ButterKnifeProcessor.VIEW_TYPE != targetType) {
                result.addStatement("((\$T) \$N).\$L(\$L)", bestGuess(targetType), bindName,
                        listener.setter, if (requiresRemoval) listenerField else callback.build())
            } else {
                result.addStatement("\$N.\$L(\$L)", bindName, listener.setter,
                        if (requiresRemoval) listenerField else callback.build())
            }
        }
        if (needsNullChecked) result.endControlFlow()
    }

    private fun getListenerMethods(listener: ListenerClass): List<ListenerMethod> {
        return if (listener.method.size == 1) {
            mutableListOf(*listener.method)
        } else try {
            val methods: MutableList<ListenerMethod> = mutableListOf()
            val callbacks = listener.callbacks
            for (callbackMethod in callbacks.java.enumConstants) {
                val callbackField = callbacks.java.getField(callbackMethod.javaClass.simpleName)
                val method = callbackField.getAnnotation(ListenerMethod::class.java)
                        ?: throw java.lang.IllegalStateException(String.format("@%s's %s.%s missing @%s annotation.",
                                callbacks.java.simpleName, callbacks.java.name,
                                callbackMethod.name, ListenerMethod::class.java.simpleName))
                methods.add(method)
            }
            methods
        } catch (e: NoSuchFieldException) {
            throw AssertionError(e)
        }
    }

    /** True when this type's bindings require a view hierarchy.  */
    private fun hasViewBindings(): Boolean {
        return viewBindings.isNotEmpty() || collectionBindings.isNotEmpty()
    }

    /** True when this type's bindings use raw integer values instead of `R` references.  */
    private fun hasUnqualifiedResourceBindings(): Boolean {
        resourceBindings.forEach {
            if (it.id()?.qualifed == true) return true
        }
        return false
    }

    /** True when this type's bindings use Resource directly instead of Context.  */
    private fun hasResourceBindingsNeedingResource(sdk: Int): Boolean {
        resourceBindings.forEach {
            if (it.requiresResources(sdk)) return true
        }
        return false
    }

    private fun hasMethodBindings(): Boolean {
        viewBindings.forEach {
            if (it.methodBindings.isNotEmpty()) return true
        }
        return false
    }

    private fun hasOnTouchMethodBindings(): Boolean {
        viewBindings.forEach {
            if (it.methodBindings.containsKey(OnTouch::class.java.getAnnotation(ListenerClass::class.java))) return true
        }
        return false
    }

    private fun hasFieldBindings(): Boolean {
        viewBindings.forEach {
            if (it.fieldBinding != null) return true
        }
        return collectionBindings.isNotEmpty()
    }

    private fun hasTargetField(): Boolean {
        return hasFieldBindings() || hasMethodBindings()
    }

    private fun hasViewLocal(): Boolean {
        viewBindings.forEach {
            if (it.requiresLocal()) return true
        }
        return false
    }

    override fun constructorNeedsView(): Boolean {
        return (hasViewBindings() || parentBinding?.constructorNeedsView() ?: false)
    }

    override fun toString(): String {
        return bindingClassName.toString()
    }

    class Builder(private val targetTypeName: TypeName?, private val bindingClassName: ClassName?,
                  private val enclosingElement: TypeElement?, private val isFinal: Boolean,
                  private val isView: Boolean, private val isActivity: Boolean, private val isDialog: Boolean) {

        private var parentBinding: BindingInformationProvider? = null

        private val viewIdMap: MutableMap<Id, ViewBinding.Builder> = mutableMapOf()
        private val collectionBindings = ImmutableList.builder<FieldCollectionViewBinding>()
        private val resourceBindings = ImmutableList.builder<ResourceBinding>()

        fun addField(id: Id, binding: FieldViewBinding?) {
            getOrCreateViewBindings(id).setFieldBinding(binding)
        }

        fun addFieldCollection(binding: FieldCollectionViewBinding) {
            collectionBindings.add(binding)
        }

        fun addMethod(id: Id, listener: ListenerClass?, method: ListenerMethod, binding: MethodViewBinding?): Boolean {
            val viewBinding = getOrCreateViewBindings(id)
            if (viewBinding.hasMethodBinding(listener, method) && "void" != method.returnType) return false
            viewBinding.addMethodBinding(listener!!, method, binding!!)
            return true
        }

        fun addResource(binding: ResourceBinding) {
            resourceBindings.add(binding)
        }

        fun setParent(parent: BindingInformationProvider?) {
            parentBinding = parent
        }

        fun findExistingBindingName(id: Id?): String? {
            val builder = viewIdMap[id] ?: return null
            val fieldBinding = builder.fieldBinding ?: return null
            return fieldBinding.name
        }

        private fun getOrCreateViewBindings(id: Id): ViewBinding.Builder {
            var viewId = viewIdMap[id]
            if (viewId == null) {
                viewId = ViewBinding.Builder(id)
                viewIdMap[id] = viewId
            }
            return viewId
        }

        fun build(): BindingSet {
            val viewBindings = ImmutableList.builder<ViewBinding>()
            for (builder in viewIdMap.values) {
                viewBindings.add(builder.build())
            }
            return BindingSet(targetTypeName, bindingClassName, enclosingElement, isFinal, isView,
                    isActivity, isDialog, viewBindings.build(), collectionBindings.build(),
                    resourceBindings.build(), parentBinding)
        }
    }
}