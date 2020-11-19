package butterknife.compiler

import com.squareup.javapoet.CodeBlock

interface ResourceBinding {
    fun id(): Id?

    /** True if the code for this binding requires a 'res' variable for `Resources` access.  */
    fun requiresResources(sdk: Int): Boolean
    fun render(sdk: Int): CodeBlock?
}