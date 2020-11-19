package butterknife

import androidx.annotation.FontRes
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Bind a field to the specified font resource ID.
 * <pre>`
 * @BindFont(R.font.comic_sans) Typeface comicSans;
`</pre> *
 */
@Target(AnnotationTarget.FIELD)
@Retention(RetentionPolicy.RUNTIME)
annotation class BindFont(
        /** Font resource ID to which the field will be bound.  */
        @FontRes val value: Int, @BindFont.TypefaceStyle val style: Int = 0) {
    @IntDef(0, 1, 2, 3)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    annotation class TypefaceStyle
}