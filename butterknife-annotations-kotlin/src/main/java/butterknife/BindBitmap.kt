package butterknife

import androidx.annotation.DrawableRes
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Bind a field to a [@link Bitmap] from the specified drawable resource ID.
 * <pre>`
 * @BindBitmap(R.drawable.logo) Bitmap logo;
`</pre> *
 */
@Target(AnnotationTarget.FIELD)
@Retention(RetentionPolicy.RUNTIME)
annotation class BindBitmap(@DrawableRes val value: Int)