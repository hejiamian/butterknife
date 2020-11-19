package butterknife.compiler

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName

class FieldViewBinding(val name: String, val type: TypeName, val isRequired: Boolean) : MemberViewBinding {

    val rawType: ClassName
        get() = if (type is ParameterizedTypeName) {
            type.rawType
        } else type as ClassName

    override fun description() = "field '$name'"

}