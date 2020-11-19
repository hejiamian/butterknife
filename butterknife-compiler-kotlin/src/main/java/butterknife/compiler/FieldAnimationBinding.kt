package butterknife.compiler

import com.squareup.javapoet.CodeBlock

internal class FieldAnimationBinding(private val id: Id, private val name: String) : ResourceBinding {
    override fun id(): Id? {
        return id
    }

    override fun requiresResources(sdk: Int): Boolean {
        return false
    }

    override fun render(sdk: Int): CodeBlock? {
        return CodeBlock.of("target.\$L = \$T.loadAnimation(context, \$L)", name, BindingSet.ANIMATION_UTILS,
                id.code)
    }

}