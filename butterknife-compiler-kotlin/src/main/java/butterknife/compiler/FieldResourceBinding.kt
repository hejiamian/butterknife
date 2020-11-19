package butterknife.compiler

import com.google.common.collect.ImmutableList
import com.google.errorprone.annotations.Immutable
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import java.util.*

internal class FieldResourceBinding(private val id: Id, private val name: String, private val type: Type) : ResourceBinding {
    internal enum class Type {
        BITMAP(ResourceMethod(BindingSet.BITMAP_FACTORY, "decodeResource", true, 1)), BOOL("getBoolean"), COLOR(ResourceMethod(BindingSet.CONTEXT_COMPAT, "getColor", false, 1),
                ResourceMethod(null, "getColor", false, 23)),
        COLOR_STATE_LIST(ResourceMethod(BindingSet.CONTEXT_COMPAT,
                "getColorStateList", false, 1),
                ResourceMethod(null, "getColorStateList", false, 23)),
        DIMEN_AS_INT("getDimensionPixelSize"), DIMEN_AS_FLOAT("getDimension"), FLOAT(ResourceMethod(BindingSet.UTILS, "getFloat", false, 1)), INT("getInteger"), INT_ARRAY("getIntArray"), STRING("getString"), STRING_ARRAY("getStringArray"), TEXT_ARRAY("getTextArray"), TYPED_ARRAY("obtainTypedArray");

        private var methods: ImmutableList<ResourceMethod>? = null

        constructor(vararg methods: ResourceMethod?) {
            val methodList = mutableListOf<ResourceMethod>()
            methods.forEach {
                it?.let { method -> methodList.add(method) }
            }
            methodList.sorted()
            methodList.reverse()
            this.methods = ImmutableList.copyOf(methodList)

        }

        constructor(methodName: String?) {
            methods = ImmutableList.of(ResourceMethod(null, methodName, true, 1))
        }

        fun methodForSdk(sdk: Int): ResourceMethod? {
//            val size = methods?.size ?: 0
            methods?.forEach {
                if (it.sdk <= sdk) {
                    return it
                }
            }
            throw AssertionError()
        }
    }

    @Immutable
    internal class ResourceMethod(val typeName: ClassName?, val name: String?, val requiresResources: Boolean, val sdk: Int) : Comparable<ResourceMethod> {
        override fun compareTo(other: ResourceMethod): Int {
            return Integer.compare(sdk, other.sdk)
        }

    }

    override fun id(): Id {
        return id
    }

    override fun requiresResources(sdk: Int): Boolean {
        return type.methodForSdk(sdk)!!.requiresResources
    }

    override fun render(sdk: Int): CodeBlock? {
        val method = type.methodForSdk(sdk)
        if (method!!.typeName == null) {
            return if (method.requiresResources) {
                CodeBlock.of("target.\$L = res.\$L(\$L)", name, method.name, id.code)
            } else CodeBlock.of("target.\$L = context.\$L(\$L)", name, method.name, id.code)
        }
        return if (method.requiresResources) {
            CodeBlock.of("target.\$L = \$T.\$L(res, \$L)", name, method.typeName, method.name,
                    id.code)
        } else CodeBlock.of("target.\$L = \$T.\$L(context, \$L)", name, method.typeName, method.name,
                id.code)
    }

}