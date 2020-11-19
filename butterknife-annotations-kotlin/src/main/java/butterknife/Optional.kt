package butterknife

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Denote that the view specified by the injection is not required to be present.
 * <pre>`
 * @Optional @OnClick(R.id.subtitle) void onSubtitleClick() {}
`</pre> *
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(RetentionPolicy.RUNTIME)
annotation class Optional 