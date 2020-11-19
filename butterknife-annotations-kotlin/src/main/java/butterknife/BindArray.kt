package butterknife

import androidx.annotation.ArrayRes
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Bind a field to the specified array resource ID. The type of array will be inferred from the
 * annotated element.
 *
 * String array:
 * <pre>`
 * @BindArray(R.array.countries) String[] countries;
`</pre> *
 *
 * Int array:
 * <pre>`
 * @BindArray(R.array.phones) int[] phones;
`</pre> *
 *
 * Text array:
 * <pre>`
 * @BindArray(R.array.options) CharSequence[] options;
`</pre> *
 *
 * [android.content.res.TypedArray]:
 * <pre>`
 * @BindArray(R.array.icons) TypedArray icons;
`</pre> *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(AnnotationTarget.FIELD)
/** Array resource ID to which the field will be bound.  */
annotation class BindArray(@ArrayRes val value: Int)