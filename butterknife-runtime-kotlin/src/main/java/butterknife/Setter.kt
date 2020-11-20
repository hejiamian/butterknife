package butterknife

import android.view.View
import androidx.annotation.UiThread

/** A setter that can apply a value to a list of views.  */
interface Setter<T : View?, V> {
    /** Set the `value` on the `view` which is at `index` in the list.  */
    @UiThread
    operator fun set(view: T, value: V?, index: Int)
}