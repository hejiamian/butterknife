package butterknife

import androidx.annotation.IdRes
import butterknife.internal.ListenerClass
import butterknife.internal.ListenerMethod
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Bind a method to an [OnTouchListener] on the view for each ID specified.
 * <pre>`
 * @OnTouch(R.id.example) boolean onTouch() {
 * Toast.makeText(this, "Touched!", Toast.LENGTH_SHORT).show();
 * return false;
 * }
`</pre> *
 * Any number of parameters from
 * [onTouch][OnTouchListener.onTouch] may be used
 * on the method.
 *
 *
 * If the return type of the method is `void`, true will be returned from the listener.
 *
 * @see OnTouchListener
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(RetentionPolicy.RUNTIME)
@ListenerClass(targetType = "android.view.View", setter = "setOnTouchListener", type = "android.view.View.OnTouchListener", method = [ListenerMethod(name = "onTouch", parameters = ["android.view.View", "android.view.MotionEvent"], returnType = "Boolean", defaultReturn = "true")])
annotation class OnTouch(
        /** View IDs to which the method will be bound.  */
        @IdRes vararg val value: Int = [-1])