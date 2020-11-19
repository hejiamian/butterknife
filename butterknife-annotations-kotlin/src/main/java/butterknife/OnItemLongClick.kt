package butterknife

import butterknife.internal.ListenerClass
import butterknife.internal.ListenerMethod
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Bind a method to an [OnItemLongClickListener] on the view for each
 * ID specified.
 * <pre>`
 * @OnItemLongClick(R.id.example_list) boolean onItemLongClick(int position) {
 * Toast.makeText(this, "Long clicked position " + position + "!", Toast.LENGTH_SHORT).show();
 * return true;
 * }
`</pre> *
 * Any number of parameters from
 * [onItemLongClick][OnItemLongClickListener.onItemLongClick] may be used on the method.
 *
 *
 * If the return type of the method is `void`, true will be returned from the listener.
 *
 * @see OnItemLongClickListener
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(RetentionPolicy.RUNTIME)
@ListenerClass(targetType = "android.widget.AdapterView<?>", setter = "setOnItemLongClickListener", type = "android.widget.AdapterView.OnItemLongClickListener", method = [ListenerMethod(name = "onItemLongClick", parameters = ["android.widget.AdapterView<?>", "android.view.View", "Int", "Long"], returnType = "Boolean", defaultReturn = "true")])
annotation class OnItemLongClick(
        /** View IDs to which the method will be bound.  */
        vararg val value: Int = [-1])