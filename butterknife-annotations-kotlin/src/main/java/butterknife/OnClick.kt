package butterknife

import androidx.annotation.IdRes
import butterknife.internal.ListenerClass
import butterknife.internal.ListenerMethod
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Bind a method to an [OnClickListener] on the view for each ID specified.
 * <pre>`
 * @OnClick(R.id.example) void onClick() {
 * Toast.makeText(this, "Clicked!", Toast.LENGTH_SHORT).show();
 * }
`</pre> *
 * Any number of parameters from
 * [onClick][OnClickListener.onClick] may be used on the
 * method.
 *
 * @see OnClickListener
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(RetentionPolicy.RUNTIME)
@ListenerClass(targetType = "android.view.View", setter = "setOnClickListener", type = "butterknife.internal.DebouncingOnClickListener", method = [ListenerMethod(name = "doClick", parameters = ["android.view.View"])])
annotation class OnClick(
        /** View IDs to which the method will be bound.  */
        @IdRes vararg val value: Int = [-1])