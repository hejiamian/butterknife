package butterknife

import androidx.annotation.IdRes
import butterknife.internal.ListenerClass
import butterknife.internal.ListenerMethod
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Bind a method to an [OnLongClickListener] on the view for each ID
 * specified.
 * <pre>`
 * @OnLongClick(R.id.example) boolean onLongClick() {
 * Toast.makeText(this, "Long clicked!", Toast.LENGTH_SHORT).show();
 * return true;
 * }
`</pre> *
 * Any number of parameters from [OnLongClickListener.onLongClick] may be
 * used on the method.
 *
 *
 * If the return type of the method is `void`, true will be returned from the listener.
 *
 * @see OnLongClickListener
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(RetentionPolicy.RUNTIME)
@ListenerClass(targetType = "android.view.View", setter = "setOnLongClickListener", type = "android.view.View.OnLongClickListener", method = [ListenerMethod(name = "onLongClick", parameters = ["android.view.View"], returnType = "Boolean", defaultReturn = "true")])
annotation class OnLongClick(
        /** View IDs to which the method will be bound.  */
        @IdRes vararg val value: Int = [-1])