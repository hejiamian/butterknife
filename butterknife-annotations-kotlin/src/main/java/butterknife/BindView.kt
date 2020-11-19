package butterknife

import androidx.annotation.IdRes
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Bind a field to the view for the specified ID. The view will automatically be cast to the field
 * type.
 * <pre>`
 * @BindView(R.id.title) TextView title;
`</pre> *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class BindView(
        /** View ID to which the field will be bound.  */
        @IdRes val value: Int)