package butterknife.compiler

import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName

class FieldCollectionViewBinding(var name: String, private val type: TypeName, private val kind: Kind, private val ids: List<Id>,
                                          private val required: Boolean) {
    enum class Kind(val factoryName: String) {
        ARRAY("arrayFilteringNull"), LIST("listFilteringNull");

    }

    fun render(debuggable: Boolean): CodeBlock {
        val builder = CodeBlock.builder()
                .add("target.\$L = \$T.\$L(", name, BindingSet.UTILS, kind.factoryName)
        for (i in ids.indices) {
            if (i > 0) {
                builder.add(", ")
            }
            builder.add("\n")
            val id = ids[i]
            val requiresCast = BindingSet.requiresCast(type)
            if (!debuggable) {
                if (requiresCast) {
                    builder.add("(\$T) ", type)
                }
                builder.add("source.findViewById(\$L)", id.code)
            } else if (!requiresCast && !required) {
                builder.add("source.findViewById(\$L)", id.code)
            } else {
                builder.add("\$T.find", BindingSet.UTILS)
                builder.add(if (required) "RequiredView" else "OptionalView")
                if (requiresCast) {
                    builder.add("AsType")
                }
                builder.add("(source, \$L, \"field '\$L'\"", id.code, name)
                if (requiresCast) {
                    var rawType: TypeName? = type
                    if (rawType is ParameterizedTypeName) {
                        rawType = rawType.rawType
                    }
                    builder.add(", \$T.class", rawType)
                }
                builder.add(")")
            }
        }
        return builder.add(")").build()
    }

}