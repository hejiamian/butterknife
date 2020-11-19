package butterknife.internal

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Retention(RetentionPolicy.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ListenerMethod(
        /** Name of the listener method for which this annotation applies.  */
        val name: String,
        /** List of method parameters. If the type is not a primitive it must be fully-qualified.  */
        val parameters: Array<String> = [],
        /** Primitive or fully-qualified return type of the listener method. May also be `void`.  */
        val returnType: String = "void",
        /** If [.returnType] is not `void` this value is returned when no binding exists.  */
        val defaultReturn: String = "null") 