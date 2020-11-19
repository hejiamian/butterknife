package butterknife

import androidx.annotation.BoolRes
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Bind a field to the specified boolean resource ID.
 * <pre>`
 * @BindBool(R.bool.is_tablet) boolean isTablet;
`</pre> *
 */
@Target(AnnotationTarget.FIELD)
@Retention(RetentionPolicy.RUNTIME)
annotation class BindBool(
        /** Boolean resource ID to which the field will be bound.  */
        @BoolRes val value: Int)