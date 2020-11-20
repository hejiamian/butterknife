package butterknife

import androidx.annotation.UiThread

/** An unbinder contract that will unbind views when called.  */
interface Unbinder {
    @UiThread
    fun unbind()
}