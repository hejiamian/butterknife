package butterknife

import androidx.annotation.IdRes
import butterknife.internal.ListenerClass
import butterknife.internal.ListenerMethod
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Bind a method to a [TextWatcher] on the view for each ID specified.
 * <pre>`
 * @OnTextChanged(R.id.example) void onTextChanged(CharSequence text) {
 * Toast.makeText(this, "Text changed: " + text, Toast.LENGTH_SHORT).show();
 * }
`</pre> *
 * Any number of parameters from [ onTextChanged][TextWatcher.onTextChanged] may be used on the method.
 *
 *
 * To bind to methods other than `onTextChanged`, specify a different `callback`.
 * <pre>`
 * @OnTextChanged(value = R.id.example, callback = BEFORE_TEXT_CHANGED)
 * void onBeforeTextChanged(CharSequence text) {
 * Toast.makeText(this, "Before text changed: " + text, Toast.LENGTH_SHORT).show();
 * }
`</pre> *
 *
 * @see TextWatcher
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(RetentionPolicy.RUNTIME)
@ListenerClass(targetType = "android.widget.TextView", setter = "addTextChangedListener", remover = "removeTextChangedListener", type = "android.text.TextWatcher", callbacks = OnTextChanged.Callback::class)
annotation class OnTextChanged(
        /** View IDs to which the method will be bound.  */
        @IdRes vararg val value: Int = [-1],
        /** Listener callback to which the method will be bound.  */
        val callback: Callback = Callback.TEXT_CHANGED) {
    /** [TextWatcher] callback methods.  */
    enum class Callback {
        /** [TextWatcher.onTextChanged]  */
        @ListenerMethod(name = "onTextChanged", parameters = ["java.lang.CharSequence", "Int", "Int", "Int"])
        TEXT_CHANGED,

        /** [TextWatcher.beforeTextChanged]  */
        @ListenerMethod(name = "beforeTextChanged", parameters = ["java.lang.CharSequence", "Int", "Int", "Int"])
        BEFORE_TEXT_CHANGED,

        /** [TextWatcher.afterTextChanged]  */
        @ListenerMethod(name = "afterTextChanged", parameters = ["android.text.Editable"])
        AFTER_TEXT_CHANGED
    }
}