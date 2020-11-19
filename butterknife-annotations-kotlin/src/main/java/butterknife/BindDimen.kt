package butterknife

import androidx.annotation.DimenRes
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Bind a field to the specified dimension resource ID. Type can be `int` for pixel size or
 * `float` for exact amount.
 * <pre>`
 * @BindDimen(R.dimen.horizontal_gap) int gapPx;
 * @BindDimen(R.dimen.horizontal_gap) float gap;
`</pre> *
 */
@Target(AnnotationTarget.FIELD)
@Retention(RetentionPolicy.RUNTIME)
annotation class BindDimen(
        /** Dimension resource ID to which the field will be bound.  */
        @DimenRes val value: Int)