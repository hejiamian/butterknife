package butterknife

import androidx.annotation.IdRes
import butterknife.internal.ListenerClass
import butterknife.internal.ListenerMethod
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Bind a method to an [OnItemSelectedListener] on the view for each
 * ID specified.
 * <pre>`
 * @OnItemSelected(R.id.example_list) void onItemSelected(int position) {
 * Toast.makeText(this, "Selected position " + position + "!", Toast.LENGTH_SHORT).show();
 * }
`</pre> *
 * Any number of parameters from
 * [onItemSelected][OnItemSelectedListener.onItemSelected] may be used on the method.
 *
 *
 * To bind to methods other than `onItemSelected`, specify a different `callback`.
 * <pre>`
 * @OnItemSelected(value = R.id.example_list, callback = NOTHING_SELECTED)
 * void onNothingSelected() {
 * Toast.makeText(this, "Nothing selected!", Toast.LENGTH_SHORT).show();
 * }
`</pre> *
 *
 * @see OnItemSelectedListener
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(RetentionPolicy.CLASS)
@ListenerClass(targetType = "android.widget.AdapterView<?>", setter = "setOnItemSelectedListener", type = "android.widget.AdapterView.OnItemSelectedListener", callbacks = OnItemSelected.Callback::class)
annotation class OnItemSelected(
        /** View IDs to which the method will be bound.  */
        @IdRes vararg val value: Int = [-1],
        /** Listener callback to which the method will be bound.  */
        val callback: Callback = Callback.ITEM_SELECTED) {
    /** [OnItemSelectedListener] callback methods.  */
    enum class Callback {
        /**
         * [OnItemSelectedListener.onItemSelected]
         */
        @ListenerMethod(name = "onItemSelected", parameters = ["android.widget.AdapterView<?>", "android.view.View", "Int", "Long"])
        ITEM_SELECTED,

        /** [OnItemSelectedListener.onNothingSelected]  */
        @ListenerMethod(name = "onNothingSelected", parameters = ["android.widget.AdapterView<?>"])
        NOTHING_SELECTED
    }
}