package butterknife

import butterknife.internal.ListenerClass
import butterknife.internal.ListenerMethod
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Bind a method to an [OnItemClickListener] on the view for each ID
 * specified.
 * <pre>`
 * @OnItemClick(R.id.example_list) void onItemClick(int position) {
 * Toast.makeText(this, "Clicked position " + position + "!", Toast.LENGTH_SHORT).show();
 * }
`</pre> *
 * Any number of parameters from [onItemClick][OnItemClickListener.onItemClick] may be used on the method.
 *
 * @see OnItemClickListener
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(RetentionPolicy.RUNTIME)
@ListenerClass(targetType = "android.widget.AdapterView<?>", setter = "setOnItemClickListener", type = "android.widget.AdapterView.OnItemClickListener", method = [ListenerMethod(name = "onItemClick", parameters = ["android.widget.AdapterView<?>", "android.view.View", "Int", "Long"])])
annotation class OnItemClick(
        /** View IDs to which the method will be bound.  */
        vararg val value: Int = [-1])