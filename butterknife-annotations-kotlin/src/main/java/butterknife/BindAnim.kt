package butterknife

import androidx.annotation.AnimRes
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Bind a field to the specified animation resource ID.
 * <pre>`
 * @BindAnim(R.anim.fade_in) Animation fadeIn;
`</pre> *
 */
@Target(AnnotationTarget.FIELD)
@Retention(RetentionPolicy.RUNTIME)
annotation class BindAnim(@AnimRes val value: Int)