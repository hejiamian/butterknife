package butterknife

import androidx.annotation.ColorRes
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Bind a field to the specified color resource ID. Type can be `int` or
 * [android.content.res.ColorStateList].
 * <pre>`
 * @BindColor(R.color.background_green) int green;
 * @BindColor(R.color.background_green_selector) ColorStateList greenSelector;
`</pre> *
 */
@Target(AnnotationTarget.FIELD)
@Retention(RetentionPolicy.RUNTIME)
annotation class BindColor(
        /** Color resource ID to which the field will be bound.  */
        @ColorRes val value: Int)