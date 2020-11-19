package butterknife

import androidx.annotation.IdRes
import butterknife.internal.ListenerClass
import butterknife.internal.ListenerMethod
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Bind a method to an [OnCheckedChangeListener] on the view for
 * each ID specified.
 * <pre>`
 * @OnCheckedChanged(R.id.example) void onChecked(boolean checked) {
 * Toast.makeText(this, checked ? "Checked!" : "Unchecked!", Toast.LENGTH_SHORT).show();
 * }
`</pre> *
 * Any number of parameters from
 * [ onCheckedChanged][OnCheckedChangeListener.onCheckedChanged] may be used on the method.
 *
 * @see OnCheckedChangeListener
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(RetentionPolicy.RUNTIME)
@ListenerClass(targetType = "android.widget.CompoundButton", setter = "setOnCheckedChangeListener", type = "android.widget.CompoundButton.OnCheckedChangeListener", method = [ListenerMethod(name = "onCheckedChanged", parameters = ["android.widget.CompoundButton", "Boolean"])])
annotation class OnCheckedChanged(
        /** View IDs to which the method will be bound.  */
        @IdRes vararg val value: Int = [-1])