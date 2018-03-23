@file:Suppress("MemberVisibilityCanBePrivate")

package net.samystudio.beaver.ui.base.controller

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.annotation.LayoutRes
import android.view.*
import butterknife.ButterKnife
import butterknife.Unbinder
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.archlifecycle.LifecycleController
import com.evernote.android.state.Bundler
import com.evernote.android.state.State
import com.evernote.android.state.StateSaver
import com.google.firebase.analytics.FirebaseAnalytics

abstract class BaseController : LifecycleController(),
                                DialogInterface.OnCancelListener,
                                DialogInterface.OnDismissListener
{
    private var unBinder: Unbinder? = null
    private var finished: Boolean = false
    private var dialogDismissed: Boolean = false
    private var dialogView: View? = null
    @get:LayoutRes
    protected abstract val layoutViewRes: Int
    protected open lateinit var firebaseAnalytics: FirebaseAnalytics
    @State
    protected var resultCode: Int = Activity.RESULT_CANCELED
    @State
    protected var resultIntent: Intent? = null
    @State
    protected var targetRequestCode: Int = 0
    protected var dialog: Dialog? = null
    @State(DialogStyle.BundleHelper::class)
    var dialogStyle: DialogStyle = DialogStyle.STYLE_NORMAL
    @State
    var dialogTheme = 0
    @State
    var dialogCancelable = true
        set(value)
        {
            field = value
            dialog?.setCancelable(value)
        }
    @State
    var isDialog = false

    override fun onRestoreInstanceState(savedInstanceState: Bundle)
    {
        super.onRestoreInstanceState(savedInstanceState)

        StateSaver.restoreInstanceState(this, savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View
    {
        if (isDialog)
        {
            if (layoutViewRes > 0)
            {
                dialogView = inflater.inflate(layoutViewRes, container, false).also {
                    unBinder = ButterKnife.bind(this, it)
                }
            }
            val view = View(activity)
            onViewCreated(view)
            return view
        }

        if (layoutViewRes > 0)
        {
            val view = inflater.inflate(layoutViewRes, container, false)
            unBinder = ButterKnife.bind(this, view)
            onViewCreated(view)
            return view
        }

        return View(activity)
    }

    override fun onContextAvailable(context: Context)
    {
        super.onContextAvailable(context)

        if (!::firebaseAnalytics.isInitialized)
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    }

    @CallSuper
    open fun onNewIntent(intent: Intent)
    {
        childRouters.forEach { router ->
            router.backstack.forEach {
                (it.controller() as? BaseController)?.onNewIntent(intent)
            }
        }
    }

    @CallSuper
    open fun onViewCreated(view: View)
    {
        if (!isDialog)
            return

        dialog = onCreateDialog().also {

            dialogDismissed = false

            when (dialogStyle)
            {
                DialogStyle.STYLE_NO_INPUT ->
                {
                    it.window?.addFlags(
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    it.requestWindowFeature(Window.FEATURE_NO_TITLE)
                }

                DialogStyle.STYLE_NO_FRAME,
                DialogStyle.STYLE_NO_TITLE ->
                    it.requestWindowFeature(Window.FEATURE_NO_TITLE)
                else                       ->
                {
                }
            }

            if (dialogView != null)
                it.setContentView(dialogView)

            it.ownerActivity = activity
            it.setCancelable(dialogCancelable)
            it.setOnCancelListener(this)
            it.setOnDismissListener(this)
        }
    }

    override fun onRestoreViewState(view: View, savedViewState: Bundle)
    {
        super.onRestoreViewState(view, savedViewState)

        val dialogState = savedViewState.getBundle(KEY_SAVED_DIALOG_STATE)
        dialogState?.let { dialog?.onRestoreInstanceState(it) }
    }

    override fun onAttach(view: View)
    {
        super.onAttach(view)

        dialog?.show()
        firebaseAnalytics.setCurrentScreen(activity!!, javaClass.simpleName, javaClass.simpleName)
    }

    override fun onSaveViewState(view: View, outState: Bundle)
    {
        super.onSaveViewState(view, outState)

        val dialogState = dialog?.onSaveInstanceState()
        dialogState?.let { outState.putBundle(KEY_SAVED_DIALOG_STATE, it) }
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        super.onSaveInstanceState(outState)

        StateSaver.saveInstanceState(this, outState)
    }

    override fun onDetach(view: View)
    {
        dialog?.hide()
    }

    override fun onDestroyView(view: View)
    {
        super.onDestroyView(view)

        dialog?.dismiss()
        dialog = null
        dialogDismissed = true

        unBinder?.unbind()
        unBinder = null
    }

    fun setTargetController(controller: Controller?, requestCode: Int = 0)
    {
        targetController = controller
        targetRequestCode = requestCode
    }

    /**
     * @see [android.app.Activity.setResult]
     */
    fun setResult(code: Int, intent: Intent?)
    {
        resultCode = code
        resultIntent = intent
    }

    fun setDialogStyle(style: DialogStyle, theme: Int = 0)
    {
        dialogStyle = style

        if (dialogStyle == DialogStyle.STYLE_NO_FRAME ||
            dialogStyle == DialogStyle.STYLE_NO_INPUT)
            dialogTheme = android.R.style.Theme_Panel

        if (theme != 0)
            dialogTheme = theme
    }

    open fun onCreateDialog(): Dialog
    {
        return Dialog(activity, dialogTheme)
    }

    fun show(router: Router, transaction: RouterTransaction = RouterTransaction.with(this))
    {
        if (transaction.controller() != this)
            throw IllegalArgumentException(
                "Transaction controller must be ${javaClass.name}, found ${transaction.controller().javaClass.name}")

        if (transaction.pushChangeHandler() == null) transaction.pushChangeHandler(
            dialogPushChangeHandler)

        if (targetController == null && !router.backstack.isEmpty()) targetController =
                router.backstack.last().controller()

        isDialog = true
        router.pushController(transaction)
    }

    fun dismiss()
    {
        dialog?.dismiss()
        dialog = null
        dialogDismissed = true

        finish()
    }

    fun finish()
    {
        if (finished) return

        if (isDialog && !dialogDismissed) dismiss()
        else
        {
            targetController?.onActivityResult(targetRequestCode, resultCode, resultIntent)

            router.popController(this)
        }

        finished = true
    }

    override fun onCancel(dialog: DialogInterface)
    {
    }

    @CallSuper
    override fun onDismiss(dialog: DialogInterface)
    {
        if (!dialogDismissed)
            dismiss()
    }

    companion object
    {
        private const val KEY_SAVED_DIALOG_STATE = "BaseSimpleController.savedDialogState"
        private val dialogPushChangeHandler: ControllerChangeHandler = DialogPushTransition()
    }

    enum class DialogStyle
    {
        STYLE_NORMAL, STYLE_NO_TITLE, STYLE_NO_FRAME, STYLE_NO_INPUT;

        /**
         * @hide
         */
        class BundleHelper : Bundler<DialogStyle>
        {
            override fun put(key: String, value: DialogStyle, bundle: Bundle) =
                bundle.putString(key, value.name)

            override fun get(key: String, bundle: Bundle): DialogStyle? =
                valueOf(bundle.getString(key))
        }
    }

    private class DialogPushTransition : ControllerChangeHandler()
    {
        override fun performChange(container: ViewGroup, from: View?, to: View?, isPush: Boolean,
                                   changeListener: ControllerChangeCompletedListener)
        {
            container.addView(to)
            changeListener.onChangeCompleted()
        }
    }
}