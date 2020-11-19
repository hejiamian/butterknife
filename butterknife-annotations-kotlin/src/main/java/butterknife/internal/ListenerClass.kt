package butterknife.internal

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import kotlin.reflect.KClass

@Retention(RetentionPolicy.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class ListenerClass(val targetType: String,
                               /** Name of the setter method on the [target type][.targetType] for the listener.  */
                               val setter: String,
                               /**
                                * Name of the method on the [target type][.targetType] to remove the listener. If
                                * empty [.setter] will be used by default.
                                */
                               val remover: String = "",
                               /** Fully-qualified class name of the listener type.  */
                               val type: String,
                               /** Enum which declares the listener callback methods. Mutually exclusive to [.method].  */
                               val callbacks: KClass<out Enum<*>> = NONE::class,
                               /**
                                * Method data for single-method listener callbacks. Mutually exclusive with [.callbacks]
                                * and an error to specify more than one value.
                                */
                               val method: Array<ListenerMethod> = []) {
    /** Default value for [.callbacks].  */
    enum class NONE
}