package butterknife

import android.view.View
import androidx.annotation.NonNull
import androidx.annotation.UiThread

/** An action that can be applied to a list of views.  */
interface Action<T : View?> {
    /** Apply the action on the `view` which is at `index` in the list.  */
    @UiThread
    fun apply(@NonNull view: T, index: Int)
}