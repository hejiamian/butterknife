package butterknife

import androidx.annotation.IdRes
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Bind a field to the view for the specified ID. The view will automatically be cast to the field
 * type.
 * <pre>`
 * @BindViews({ R.id.title, R.id.subtitle })
 * List<TextView> titles;
`</pre> *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class BindViews(
        /** View IDs to which the field will be bound.  */
        @IdRes vararg val value: Int)