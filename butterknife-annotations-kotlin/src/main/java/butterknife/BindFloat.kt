package butterknife

import androidx.annotation.DimenRes
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Bind a field to the specified dimension resource ID whose type is explicitly defined as float.
 *
 *
 * This is different than simply reading a normal dimension as a float value which
 * [@BindDimen][BindDimen] supports. The resource must be defined as a float like
 * `<item name="whatever" format="float" type="dimen">1.1</item>`.
 * <pre>`
 * @BindFloat(R.dimen.image_ratio) float imageRatio;
`</pre> *
 */
@Target(AnnotationTarget.FIELD)
@Retention(RetentionPolicy.RUNTIME)
annotation class BindFloat(
        /** Float resource ID to which the field will be bound.  */
        @DimenRes val value: Int)