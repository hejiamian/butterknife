package butterknife

import android.util.Property
import android.view.View
import androidx.annotation.UiThread

/** Convenience methods for working with view collections.  */
object ViewCollections {
    /** Apply the specified `actions` across the `list` of views.  */
    @UiThread
    @SafeVarargs
    fun <T : View?> run(list: List<T>,
                        vararg actions: Action<in T>) {
        var i = 0
        val count = list.size
        while (i < count) {
            for (action in actions) {
                action.apply(list[i], i)
            }
            i++
        }
    }

    /** Apply the specified `actions` across the `array` of views.  */
    @UiThread
    @SafeVarargs
    fun <T : View?> run(array: Array<T>,
                        vararg actions: Action<in T>) {
        var i = 0
        val count = array.size
        while (i < count) {
            for (action in actions) {
                action.apply(array[i], i)
            }
            i++
        }
    }

    /** Apply the specified `action` across the `list` of views.  */
    @UiThread
    fun <T : View?> run(list: List<T>,
                        action: Action<in T>) {
        var i = 0
        val count = list.size
        while (i < count) {
            action.apply(list[i], i)
            i++
        }
    }

    /** Apply the specified `action` across the `array` of views.  */
    @UiThread
    fun <T : View?> run(array: Array<T>, action: Action<in T>) {
        var i = 0
        val count = array.size
        while (i < count) {
            action.apply(array[i], i)
            i++
        }
    }

    /** Apply `actions` to `view`.  */
    @UiThread
    @SafeVarargs
    fun <T : View?> run(view: T,
                        vararg actions: Action<in T?>) {
        for (action in actions) {
            action.apply(view, 0)
        }
    }

    /** Apply `action` to `view`.  */
    @UiThread
    fun <T : View?> run(view: T, action: Action<in T?>) {
        action.apply(view, 0)
    }

    /** Set the `value` using the specified `setter` across the `list` of views.  */
    @UiThread
    operator fun <T : View?, V> set(list: List<T>,
                                    setter: Setter<in T, V>, value: V?) {
        var i = 0
        val count = list.size
        while (i < count) {
            setter[list[i], value] = i
            i++
        }
    }

    /** Set the `value` using the specified `setter` across the `array` of views.  */
    @UiThread
    operator fun <T : View?, V> set(array: Array<T>,
                                    setter: Setter<in T, V>, value: V?) {
        var i = 0
        val count = array.size
        while (i < count) {
            setter[array[i], value] = i
            i++
        }
    }

    /** Set `value` on `view` using `setter`.  */
    @UiThread
    operator fun <T : View?, V> set(view: T,
                                    setter: Setter<in T?, V>, value: V?) {
        setter[view, value] = 0
    }

    /**
     * Apply the specified `value` across the `list` of views using the `property`.
     */
    @UiThread
    operator fun <T : View?, V> set(list: List<T>,
                                    setter: Property<in T, V?>, value: V?) {
        var i = 0
        val count = list.size
        while (i < count) {
            setter[list[i]] = value
            i++
        }
    }

    /**
     * Apply the specified `value` across the `array` of views using the `property`.
     */
    @UiThread
    operator fun <T : View?, V> set(array: Array<T>,
                                    setter: Property<in T, V?>, value: V?) {
        var i = 0
        val count = array.size
        while (i < count) {
            setter[array[i]] = value
            i++
        }
    }

    /** Apply `value` to `view` using `property`.  */
    @UiThread
    operator fun <T : View?, V> set(view: T,
                                    setter: Property<in T?, V?>, value: V?) {
        setter[view] = value
    }
}