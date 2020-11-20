package butterknife.internal

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View
import androidx.annotation.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import java.util.*

// Used by generated code.
class Utils private constructor() {
    companion object {
        private val VALUE = TypedValue()

        @UiThread // Implicit synchronization for use of shared resource VALUE.
        fun getTintedDrawable(context: Context,
                              @DrawableRes id: Int, @AttrRes tintAttrId: Int): Drawable? {
            val attributeFound = context.theme.resolveAttribute(tintAttrId, VALUE, true)
            if (!attributeFound) {
                throw Resources.NotFoundException("Required tint color attribute with name "
                        + context.resources.getResourceEntryName(tintAttrId)
                        + " and attribute ID "
                        + tintAttrId
                        + " was not found.")
            }
            var drawable = ContextCompat.getDrawable(context, id)
            drawable = DrawableCompat.wrap(drawable!!.mutate())
            val color = ContextCompat.getColor(context, VALUE.resourceId)
            DrawableCompat.setTint(drawable, color)
            return drawable
        }

        @UiThread // Implicit synchronization for use of shared resource VALUE.
        fun getFloat(context: Context, @DimenRes id: Int): Float {
            val value = VALUE
            context.resources.getValue(id, value, true)
            if (value.type == TypedValue.TYPE_FLOAT) {
                return value.float
            }
            throw Resources.NotFoundException("Resource ID #0x" + Integer.toHexString(id)
                    + " type #0x" + Integer.toHexString(value.type) + " is not valid")
        }

        fun <T> findOptionalViewAsType(source: View, @IdRes id: Int, who: String,
                                       cls: Class<T>): T {
            val view = source.findViewById<View>(id)
            return castView(view, id, who, cls)
        }

        fun findRequiredView(source: View, @IdRes id: Int, who: String): View {
            val view = source.findViewById<View>(id)
            if (view != null) {
                return view
            }
            val name = getResourceEntryName(source, id)
            throw IllegalStateException("Required view '"
                    + name
                    + "' with ID "
                    + id
                    + " for "
                    + who
                    + " was not found. If this view is optional add '@Nullable' (fields) or '@Optional'"
                    + " (methods) annotation.")
        }

        fun <T> findRequiredViewAsType(source: View, @IdRes id: Int, who: String,
                                       cls: Class<T>): T {
            val view = findRequiredView(source, id, who)
            return castView(view, id, who, cls)
        }

        fun <T> castView(view: View, @IdRes id: Int, who: String, cls: Class<T>): T {
            return try {
                cls.cast(view)
            } catch (e: ClassCastException) {
                val name = getResourceEntryName(view, id)
                throw IllegalStateException("View '"
                        + name
                        + "' with ID "
                        + id
                        + " for "
                        + who
                        + " was of the wrong type. See cause for more info.", e)
            }
        }

        fun <T> castParam(value: Any?, from: String, fromPos: Int, to: String, toPos: Int,
                          cls: Class<T>): T {
            return try {
                cls.cast(value)
            } catch (e: ClassCastException) {
                throw IllegalStateException("Parameter #"
                        + (fromPos + 1)
                        + " of method '"
                        + from
                        + "' was of the wrong type for parameter #"
                        + (toPos + 1)
                        + " of method '"
                        + to
                        + "'. See cause for more info.", e)
            }
        }

        private fun getResourceEntryName(view: View, @IdRes id: Int): String {
            return if (view.isInEditMode) {
                "<unavailable while editing>"
            } else view.context.resources.getResourceEntryName(id)
        }

    }

    init {
        throw AssertionError("No instances.")
    }
}