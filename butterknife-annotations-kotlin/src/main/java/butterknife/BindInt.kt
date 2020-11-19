package butterknife

import androidx.annotation.IntegerRes
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Bind a field to the specified integer resource ID.
 * <pre>`
 * @BindInt(R.int.columns) int columns;
`</pre> *
 */
@Target(AnnotationTarget.FIELD)
@Retention(RetentionPolicy.RUNTIME)
annotation class BindInt(
        /** Integer resource ID to which the field will be bound.  */
        @IntegerRes val value: Int)