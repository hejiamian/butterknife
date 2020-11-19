package butterknife.compiler

import com.squareup.javapoet.ClassName
import javax.lang.model.element.TypeElement

internal class ClasspathBindingSet(private val constructorNeedsView: Boolean, classElement: TypeElement) : BindingInformationProvider {
    private val className: ClassName = BindingSet.getBindingClassName(classElement)
    override fun getBindingClassName(): ClassName {
        return className
    }

    override fun constructorNeedsView(): Boolean {
        return constructorNeedsView
    }

}