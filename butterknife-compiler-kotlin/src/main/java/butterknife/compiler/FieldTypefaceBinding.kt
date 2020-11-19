package butterknife.compiler

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock

internal class FieldTypefaceBinding(private val id: Id, private val name: String, private val style: TypefaceStyles) : ResourceBinding {
    /** Keep in sync with [android.graphics.Typeface] constants.  */
    internal enum class TypefaceStyles(val value: Int) {
        NORMAL(0), BOLD(1), ITALIC(2), BOLD_ITALIC(3);

        companion object {
            @JvmStatic
            fun fromValue(value: Int): TypefaceStyles? {
                for (style in values()) {
                    if (style.value == value) {
                        return style
                    }
                }
                return null
            }
        }

    }

    override fun id(): Id? {
        return id
    }

    override fun requiresResources(sdk: Int): Boolean {
        return sdk >= 26
    }

    override fun render(sdk: Int): CodeBlock? {
        var typeface = if (sdk >= 26) CodeBlock.of("res.getFont(\$L)", id.code) else CodeBlock.of("\$T.getFont(context, \$L)", RESOURCES_COMPAT, id.code)
        if (style != TypefaceStyles.NORMAL) {
            typeface = CodeBlock.of("$1T.create($2L, $1T.$3L)", TYPEFACE, typeface, style)
        }
        return CodeBlock.of("target.\$L = \$L", name, typeface)
    }

    companion object {
        private val RESOURCES_COMPAT = ClassName.get("androidx.core.content.res", "ResourcesCompat")
        private val TYPEFACE = ClassName.get("android.graphics", "Typeface")
    }

}