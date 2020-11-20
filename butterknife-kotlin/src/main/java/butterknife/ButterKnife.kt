package butterknife

import android.app.Activity
import android.app.Dialog
import android.util.Log
import android.view.View
import androidx.annotation.CheckResult
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.util.*

/**
 * Field and method binding for Android views. Use this class to simplify finding views and
 * attaching listeners by binding them with annotations.
 *
 *
 * Finding views from your activity is as easy as:
 * <pre>`
 * public class ExampleActivity extends Activity {
 * @BindView(R.id.title) EditText titleView;
 * @BindView(R.id.subtitle) EditText subtitleView;
 *
 * @Override protected void onCreate(Bundle savedInstanceState) {
 * super.onCreate(savedInstanceState);
 * setContentView(R.layout.example_activity);
 * ButterKnife.bind(this);
 * }
 * }
`</pre> *
 * Binding can be performed directly on an [activity][.bind], a
 * [view][.bind], or a [dialog][.bind]. Alternate objects to
 * bind can be specified along with an [activity][.bind],
 * [view][.bind], or
 * [dialog][.bind].
 *
 *
 * Group multiple views together into a [List] or array.
 * <pre>`
 * @BindView({R.id.first_name, R.id.middle_name, R.id.last_name})
 * List<EditText> nameViews;
</EditText>`</pre> *
 *
 *
 * To bind listeners to your views you can annotate your methods:
 * <pre>`
 * @OnClick(R.id.submit) void onSubmit() {
 * // React to button click.
 * }
`</pre> *
 * Any number of parameters from the listener may be used on the method.
 * <pre>`
 * @OnItemClick(R.id.tweet_list) void onTweetClicked(int position) {
 * // React to tweet click.
 * }
`</pre> *
 *
 *
 * Be default, views are required to be present in the layout for both field and method bindings.
 * If a view is optional add a `@Nullable` annotation for fields (such as the one in the
 * [support-annotations](http://tools.android.com/tech-docs/support-annotations) library)
 * or the `@Optional` annotation for methods.
 * <pre>`
 * @Nullable @BindView(R.id.title) TextView subtitleView;
`</pre> *
 * Resources can also be bound to fields to simplify programmatically working with views:
 * <pre>`
 * @BindBool(R.bool.is_tablet) boolean isTablet;
 * @BindInt(R.integer.columns) int columns;
 * @BindColor(R.color.error_red) int errorRed;
`</pre> *
 */
class ButterKnife private constructor() {
    companion object {
        private const val TAG = "ButterKnife"
        private var debug = false

        @VisibleForTesting
        val BINDINGS: MutableMap<Class<*>, Constructor<out Unbinder?>?> = mutableMapOf()

        /** Control whether debug logging is enabled.  */
        fun setDebug(debug: Boolean) {
            Companion.debug = debug
        }

        /**
         * BindView annotated fields and methods in the specified [Activity]. The current content
         * view is used as the view root.
         *
         * @param target Target activity for view binding.
         */
        @UiThread
        fun bind(target: Activity): Unbinder {
            val sourceView = target.window.decorView
            return bind(target, sourceView)
        }

        /**
         * BindView annotated fields and methods in the specified [View]. The view and its children
         * are used as the view root.
         *
         * @param target Target view for view binding.
         */
        @UiThread
        fun bind(target: View): Unbinder {
            return bind(target, target)
        }

        /**
         * BindView annotated fields and methods in the specified [Dialog]. The current content
         * view is used as the view root.
         *
         * @param target Target dialog for view binding.
         */
        @UiThread
        fun bind(target: Dialog): Unbinder {
            val sourceView = target.window!!.decorView
            return bind(target, sourceView)
        }

        /**
         * BindView annotated fields and methods in the specified `target` using the `source`
         * [Activity] as the view root.
         *
         * @param target Target class for view binding.
         * @param source Activity on which IDs will be looked up.
         */
        @UiThread
        fun bind(target: Any, source: Activity): Unbinder {
            val sourceView = source.window.decorView
            return bind(target, sourceView)
        }

        /**
         * BindView annotated fields and methods in the specified `target` using the `source`
         * [Dialog] as the view root.
         *
         * @param target Target class for view binding.
         * @param source Dialog on which IDs will be looked up.
         */
        @UiThread
        fun bind(target: Any, source: Dialog): Unbinder {
            val sourceView = source.window!!.decorView
            return bind(target, sourceView)
        }

        /**
         * BindView annotated fields and methods in the specified `target` using the `source`
         * [View] as the view root.
         *
         * @param target Target class for view binding.
         * @param source View root on which IDs will be looked up.
         */
        @UiThread
        fun bind(target: Any, source: View): Unbinder {
            val targetClass: Class<*> = target.javaClass
            if (debug) Log.d(TAG, "Looking up binding for " + targetClass.name)
            return try {
                val constructor = findBindingConstructorForClass(targetClass)
                constructor?.newInstance(target, source)!!
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
                throw e
            } catch (e: InstantiationException) {
                e.printStackTrace()
                throw e
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
                throw e
            }
        }

        @CheckResult
        @UiThread
        private fun findBindingConstructorForClass(cls: Class<*>): Constructor<out Unbinder?>? {
            var bindingCtor = BINDINGS[cls]
            if (bindingCtor != null || BINDINGS.containsKey(cls)) {
                if (debug) Log.d(TAG, "HIT: Cached in binding map.")
                return bindingCtor
            }
            val clsName = cls.name
            if (clsName.startsWith("android.") || clsName.startsWith("java.")
                    || clsName.startsWith("androidx.")) {
                if (debug) Log.d(TAG, "MISS: Reached framework class. Abandoning search.")
                return null
            }
            try {
                val bindingClass = cls.classLoader.loadClass(clsName + "_ViewBinding")
                bindingCtor = bindingClass.getConstructor(cls, View::class.java) as Constructor<out Unbinder?>
                if (debug) Log.d(TAG, "HIT: Loaded binding class and constructor.")
            } catch (e: ClassNotFoundException) {
                if (debug) Log.d(TAG, "Not found. Trying superclass " + cls.superclass.name)
                bindingCtor = findBindingConstructorForClass(cls.superclass)
            } catch (e: NoSuchMethodException) {
                throw RuntimeException("Unable to find binding constructor for $clsName", e)
            }
            BINDINGS[cls] = bindingCtor
            return bindingCtor
        }
    }

    init {
        throw AssertionError("No instances.")
    }
}