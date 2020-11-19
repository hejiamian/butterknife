package butterknife

import androidx.annotation.StringRes
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Bind a field to the specified string resource ID.
 * <pre>`
 * @BindString(R.string.username_error) String usernameErrorText;
`</pre> *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class BindString(
        /** String resource ID to which the field will be bound.  */
        @StringRes val value: Int)