package butterknife

import androidx.annotation.IdRes
import butterknife.internal.ListenerClass
import butterknife.internal.ListenerMethod
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Bind a method to an `OnPageChangeListener` on the view for each ID specified.
 * <pre>`
 * @OnPageChange(R.id.example_pager) void onPageSelected(int position) {
 * Toast.makeText(this, "Selected " + position + "!", Toast.LENGTH_SHORT).show();
 * }
`</pre> *
 * Any number of parameters from `onPageSelected` may be used on the method.
 *
 *
 * To bind to methods other than `onPageSelected`, specify a different `callback`.
 * <pre>`
 * @OnPageChange(value = R.id.example_pager, callback = PAGE_SCROLL_STATE_CHANGED)
 * void onPageStateChanged(int state) {
 * Toast.makeText(this, "State changed: " + state + "!", Toast.LENGTH_SHORT).show();
 * }
`</pre> *
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(RetentionPolicy.RUNTIME)
@ListenerClass(targetType = "androidx.viewpager.widget.ViewPager", setter = "addOnPageChangeListener", remover = "removeOnPageChangeListener", type = "androidx.viewpager.widget.ViewPager.OnPageChangeListener", callbacks = OnPageChange.Callback::class)
annotation class OnPageChange(
        /** View IDs to which the method will be bound.  */
        @IdRes vararg val value: Int = [-1],
        /** Listener callback to which the method will be bound.  */
        val callback: Callback = Callback.PAGE_SELECTED) {
    /** `ViewPager.OnPageChangeListener` callback methods.  */
    enum class Callback {
        @ListenerMethod(name = "onPageSelected", parameters = ["Int"])
        PAGE_SELECTED,

        @ListenerMethod(name = "onPageScrolled", parameters = ["Int", "Float", "Int"])
        PAGE_SCROLLED,

        /** `onPageScrollStateChanged(int)`  */
        @ListenerMethod(name = "onPageScrollStateChanged", parameters = ["Int"])
        PAGE_SCROLL_STATE_CHANGED
    }
}