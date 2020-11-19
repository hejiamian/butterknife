package butterknife

import butterknife.internal.ListenerClass
import butterknife.internal.ListenerMethod
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Bind a method to an [OnEditorActionListener] on the view for each
 * ID specified.
 * <pre>`
 * @OnEditorAction(R.id.example) boolean onEditorAction(KeyEvent key) {
 * Toast.makeText(this, "Pressed: " + key, Toast.LENGTH_SHORT).show();
 * return true;
 * }
`</pre> *
 * Any number of parameters from
 * [ onEditorAction][OnEditorActionListener.onEditorAction] may be used on the method.
 *
 *
 * If the return type of the method is `void`, true will be returned from the listener.
 *
 * @see OnEditorActionListener
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(RetentionPolicy.RUNTIME)
@ListenerClass(targetType = "android.widget.TextView", setter = "setOnEditorActionListener", type = "android.widget.TextView.OnEditorActionListener", method = [ListenerMethod(name = "onEditorAction", parameters = ["android.widget.TextView", "Int", "android.view.KeyEvent"], returnType = "Boolean", defaultReturn = "true")])
annotation class OnEditorAction(
        /** View IDs to which the method will be bound.  */
        vararg val value: Int = [-1])