package butterknife.compiler

import butterknife.internal.ListenerClass
import butterknife.internal.ListenerMethod
import java.util.*

class ViewBinding(val id: Id, val methodBindings: Map<ListenerClass, MutableMap<ListenerMethod, MutableSet<MethodViewBinding>>>,
                           val fieldBinding: FieldViewBinding?) {

    val requiredBindings: List<MemberViewBinding>
        get() {
            val requiredBindings: MutableList<MemberViewBinding> = ArrayList()
            if (fieldBinding != null && fieldBinding.isRequired) {
                requiredBindings.add(fieldBinding)
            }
            for (methodBinding in methodBindings.values) {
                for (set in methodBinding.values) {
                    for (binding in set) {
                        if (binding.required) {
                            requiredBindings.add(binding)
                        }
                    }
                }
            }
            return requiredBindings
        }

    val isSingleFieldBinding: Boolean
        get() = methodBindings.isEmpty() && fieldBinding != null

    fun requiresLocal(): Boolean {
        if (isBoundToRoot) {
            return false
        }
        return !isSingleFieldBinding
    }

    val isBoundToRoot: Boolean
        get() = ButterKnifeProcessor.NO_ID.equals(id)

    class Builder internal constructor(private val id: Id) {
        private val methodBindings: MutableMap<ListenerClass, MutableMap<ListenerMethod, MutableSet<MethodViewBinding>>> = LinkedHashMap()

        @JvmField
        var fieldBinding: FieldViewBinding? = null
        fun hasMethodBinding(listener: ListenerClass?, method: ListenerMethod): Boolean {
            val methods: Map<ListenerMethod, Set<MethodViewBinding>>? = methodBindings[listener]
            return methods != null && methods.containsKey(method)
        }

        fun addMethodBinding(listener: ListenerClass, method: ListenerMethod,
                             binding: MethodViewBinding) {
            var methods = methodBindings[listener]
            var set: MutableSet<MethodViewBinding>? = null
            if (methods == null) {
                methods = LinkedHashMap()
                methodBindings[listener] = methods
            } else {
                set = methods[method]
            }
            if (set == null) {
                set = LinkedHashSet()
                methods[method] = set
            }
            set.add(binding)
        }

        fun setFieldBinding(fieldBinding: FieldViewBinding?) {
            if (this.fieldBinding != null) {
                throw AssertionError()
            }
            this.fieldBinding = fieldBinding
        }

        fun build(): ViewBinding {
            return ViewBinding(id, methodBindings, fieldBinding)
        }

    }

}