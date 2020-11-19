package butterknife.compiler

import com.squareup.javapoet.ClassName

interface BindingInformationProvider {
    fun constructorNeedsView(): Boolean
    fun getBindingClassName(): ClassName?
}