package butterknife.compiler

import com.squareup.javapoet.TypeName

/** Represents a parameter type and its position in the listener method.  */
class Parameter(val listenerPosition: Int, val type: TypeName) {

    fun requiresCast(toType: String): Boolean {
        return type.toString() != toType
    }

}