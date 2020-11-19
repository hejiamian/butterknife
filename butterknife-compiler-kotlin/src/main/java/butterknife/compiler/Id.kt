package butterknife.compiler

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.sun.tools.javac.code.Symbol

/**
 * Represents an ID of an Android resource.
 */
class Id @JvmOverloads constructor(var value: Int, rSymbol: Symbol? = null) {
    @JvmField
    var code: CodeBlock? = null
    @JvmField
    var qualifed = false
    override fun equals(o: Any?): Boolean {
        return o is Id && value == o.value
    }

    override fun hashCode(): Int {
        return value
    }

    override fun toString(): String {
        throw UnsupportedOperationException("Please use value or code explicitly")
    }

    companion object {
        private val ANDROID_R = ClassName.get("android", "R")
        private const val R = "R"
    }

    init {
        if (rSymbol != null) {
            val className = ClassName.get(rSymbol.packge().qualifiedName.toString(), R,
                    rSymbol.enclClass().name.toString())
            val resourceName = rSymbol.name.toString()
            code = if (className.topLevelClassName() == ANDROID_R) CodeBlock.of("\$L.\$N", className, resourceName) else CodeBlock.of("\$T.\$N", className, resourceName)
            qualifed = true
        } else {
            code = CodeBlock.of("\$L", value)
            qualifed = false
        }
    }
}