package butterknife

import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import butterknife.internal.Constants
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Bind a field to the specified drawable resource ID.
 * <pre>`
 * @BindDrawable(R.drawable.placeholder)
 * Drawable placeholder;
 * @BindDrawable(value = R.drawable.placeholder, tint = R.attr.colorAccent)
 * Drawable tintedPlaceholder;
`</pre> *
 */
@Target(AnnotationTarget.FIELD)
@Retention(RetentionPolicy.RUNTIME)
annotation class BindDrawable(
        /** Drawable resource ID to which the field will be bound.  */
        @DrawableRes val value: Int,
        /** Color attribute resource ID that is used to tint the drawable.  */
        @AttrRes val tint: Int = Constants.NO_RES_ID)