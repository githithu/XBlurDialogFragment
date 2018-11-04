package com.mahsoud.android.libs.xblurdialogfragment

import androidx.fragment.app.DialogFragment
import android.app.Activity
import androidx.appcompat.widget.Toolbar
import android.content.DialogInterface
import android.view.WindowManager
import android.os.Bundle

/**
 * Encapsulate dialog behavior with blur effect for
 * app using {@link android.support.v4.app.DialogFragment}.
 * <p/>
 * All the screen behind the dialog will be blurred except the action bar.
 */
abstract class SupportXBlurDialogFragment : DialogFragment() {

    /**
     * Engine used to blur.
     */
    private lateinit var mBlurEngine: XBlurDialogEngine

    /**
     * Allow to set a Toolbar which isn't set as actionbar.
     */
    private var mToolbar: Toolbar? = null

    /**
     * Dimming policy.
     */
    private var mDimmingEffect: Boolean = false

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        if (mBlurEngine != null) {
            mBlurEngine.onAttach(activity!!) // re attached
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBlurEngine = XBlurDialogEngine(activity!!)

        if (mToolbar != null) {
            mBlurEngine.setToolbar(mToolbar!!)
        }

        val radius = getBlurRadius()
        if (radius <= 0) {
            throw IllegalArgumentException("Blur radius must be strictly positive. Found : $radius")
        }
        mBlurEngine.setBlurRadius(radius)

        val factor = getDownScaleFactor()
        if (factor <= 1.0) {
            throw IllegalArgumentException("Down scale must be strictly greater than 1.0. Found : $factor")
        }
        mBlurEngine.setDownScaleFactor(factor)

        mBlurEngine.setUseRenderScript(isRenderScriptEnable())

        mBlurEngine.debug(isDebugEnable())

        mBlurEngine.setBlurActionBar(isActionBarBlurred())

        mDimmingEffect = isDimmingEnable()
    }

    override fun onStart() {
        val dialog = dialog
        if (dialog != null) {

            // enable or disable dimming effect.
            if (!mDimmingEffect) {
                dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }

            // add default fade to the dialog if no window animation has been set.
            val currentAnimation = dialog.window!!.attributes.windowAnimations
            if (currentAnimation == 0) {
                dialog.window!!.attributes.windowAnimations = R.style.XBlurDialogFragment_Default_Animation
            }
        }
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        mBlurEngine?.onResume(retainInstance)
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        mBlurEngine?.onDismiss()
    }

    override fun onDetach() {
        super.onDetach()
        mBlurEngine?.onDetach()
    }

    override fun onDestroyView() {
        if (dialog != null) {
            dialog.setDismissMessage(null)
        }
        super.onDestroyView()
    }

    /**
     * Allow to set a Toolbar which isn't set as ActionBar.
     *
     *
     * Must be called before onCreate.
     *
     * @param toolBar toolBar
     */
    fun setToolbar(toolBar: Toolbar) {
        mToolbar = toolBar
        mBlurEngine?.setToolbar(toolBar)
    }

    /**
     * For inheritance purpose.
     *
     *
     * Enable or disable debug mode.
     *
     * @return true if debug mode should be enabled.
     */
    protected fun isDebugEnable(): Boolean {
        return XBlurDialogEngine.DEFAULT_DEBUG_POLICY
    }

    /**
     * For inheritance purpose.
     *
     *
     * Allow to customize the down scale factor.
     *
     *
     * The factor down scaled factor used to reduce the size of the source image.
     * Range :  ]1.0,infinity)
     *
     * @return customized down scaled factor.
     */
    protected fun getDownScaleFactor(): Float {
        return XBlurDialogEngine.DEFAULT_BLUR_DOWN_SCALE_FACTOR
    }

    /**
     * For inheritance purpose.
     *
     *
     * Allow to customize the blur radius factor.
     *
     *
     * radius down scaled factor used to reduce the size of the source image.
     * Range :  [1,infinity)
     *
     * @return customized blur radius.
     */
    protected fun getBlurRadius(): Int {
        return XBlurDialogEngine.DEFAULT_BLUR_RADIUS
    }

    /**
     * For inheritance purpose.
     *
     *
     * Enable or disable the dimming effect.
     *
     *
     * Disabled by default.
     *
     * @return enable true to enable the dimming effect.
     */
    protected fun isDimmingEnable(): Boolean {
        return XBlurDialogEngine.DEFAULT_DIMMING_POLICY
    }

    /**
     * For inheritance purpose.
     *
     *
     * Enable or disable the blur effect on the action bar.
     *
     *
     * Disable by default.
     *
     * @return true to enable the blur effect on the action bar.
     */
    protected fun isActionBarBlurred(): Boolean {
        return XBlurDialogEngine.DEFAULT_ACTION_BAR_BLUR
    }

    /**
     * For inheritance purpose.
     *
     *
     * Enable or disable RenderScript.
     *
     *
     * Disable by default.
     *
     *
     * Don't forget to add those lines to your build.gradle if your are using Renderscript
     * <pre>
     * defaultConfig {
     * ...
     * renderscriptTargetApi 22
     * renderscriptSupportModeEnabled true
     * ...
     * }
    </pre> *
     *
     * @return true to enable RenderScript.
     */
    protected fun isRenderScriptEnable(): Boolean {
        return XBlurDialogEngine.DEFAULT_USE_RENDERSCRIPT
    }
}