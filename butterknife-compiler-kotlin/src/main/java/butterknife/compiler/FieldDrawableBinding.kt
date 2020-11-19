package butterknife.compiler

import butterknife.internal.Constants.NO_RES_ID
import com.squareup.javapoet.CodeBlock

internal class FieldDrawableBinding(private val id: Id, private val name: String, private val tintAttributeId: Id) : ResourceBinding {
    override fun id(): Id? {
        return id
    }

    override fun requiresResources(sdk: Int): Boolean {
        return false
    }

    override fun render(sdk: Int): CodeBlock? {
        if (tintAttributeId.value != NO_RES_ID) {
            return CodeBlock.of("target.\$L = \$T.getTintedDrawable(context, \$L, \$L)", name, BindingSet.UTILS, id.code,
                    tintAttributeId.code)
        }
        return if (sdk >= 21) {
            CodeBlock.of("target.\$L = context.getDrawable(\$L)", name, id.code)
        } else CodeBlock.of("target.\$L = \$T.getDrawable(context, \$L)", name, BindingSet.CONTEXT_COMPAT, id.code)
    }

}