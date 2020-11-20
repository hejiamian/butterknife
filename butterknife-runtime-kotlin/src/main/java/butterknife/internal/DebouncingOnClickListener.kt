package butterknife.internal

import android.os.Handler
import android.os.Looper
import android.view.View

/**
 * A [click listener][View.OnClickListener] that debounces multiple clicks posted in the
 * same frame. A click on one button disables all buttons for that frame.
 */
abstract class DebouncingOnClickListener : View.OnClickListener {
    override fun onClick(v: View) {
        if (enabled) {
            enabled = false

            // Post to the main looper directly rather than going through the view.
            // Ensure that ENABLE_AGAIN will be executed, avoid static field {@link #enabled}
            // staying in false state.
            MAIN.post(ENABLE_AGAIN)
            doClick(v)
        }
    }

    abstract fun doClick(v: View?)

    companion object {
        private val ENABLE_AGAIN = Runnable { enabled = true }
        private val MAIN = Handler(Looper.getMainLooper())
        var enabled = true
    }
}