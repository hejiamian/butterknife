package butterknife

import butterknife.internal.ListenerClass
import butterknife.internal.ListenerMethod
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Bind a method to an [OnFocusChangeListener] on the view for each ID
 * specified.
 * <pre>`
 * @OnFocusChange(R.id.example) void onFocusChanged(boolean focused) {
 * Toast.makeText(this, focused ? "Gained focus" : "Lost focus", Toast.LENGTH_SHORT).show();
 * }
`</pre> *
 * Any number of parameters from [onFocusChange][OnFocusChangeListener.onFocusChange] may be used on the method.
 *
 * @see OnFocusChangeListener
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(RetentionPolicy.RUNTIME)
@ListenerClass(targetType = "android.view.View", setter = "setOnFocusChangeListener", type = "android.view.View.OnFocusChangeListener", method = [ListenerMethod(name = "onFocusChange", parameters = ["android.view.View", "Boolean"])])
annotation class OnFocusChange(
        /** View IDs to which the method will be bound.  */
        vararg val value: Int = [-1])