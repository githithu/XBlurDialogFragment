package com.mahsoud.android.libs.xblurdialogfragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.ViewGroup
import android.util.TypedValue
import android.annotation.TargetApi
import android.view.animation.LinearInterpolator
import android.graphics.Bitmap
import android.os.AsyncTask

/**
 * Encapsulate the whole behaviour to provide a blur effect on a DialogFragment.
 * <p/>
 * All the screen behind the dialog will be blurred except the action bar.
 * <p/>
 * Simply linked all methods to the matching lifecycle ones.
 */
class XBlurDialogEngine {

    /**
     * Image view used to display blurred background.
     */
    private var mBlurredBackgroundView: ImageView? = null

    /**
     * Layout params used to add blurred background.
     */
    private var mBlurredBackgroundLayoutParams: FrameLayout.LayoutParams? = null

    /**
     * Task used to capture screen and blur it.
     */
    private var mBluringTask: XBlurAsyncTask? = null

    /**
     * Used to enable or disable debug mod.
     */
    private var mDebugEnable = false

    /**
     * Factor used to down scale background. High quality isn't necessary
     * since the background will be blurred.
     */
    private var mDownScaleFactor = DEFAULT_BLUR_DOWN_SCALE_FACTOR

    /**
     * Radius used for fast blur algorithm.
     */
    private var mBlurRadius = DEFAULT_BLUR_RADIUS

    /**
     * Holding activity.
     */
    private var mHoldingActivity: Activity? = null

    /**
     * Allow to use a toolbar without set it as action bar.
     */
    private var mToolbar: Toolbar? = null

    /**
     * Duration used to animate in and out the blurred image.
     *
     *
     * In milli.
     */
    private var mAnimationDuration: Int = 0

    /**
     * Boolean used to know if the actionBar should be blurred.
     */
    private var mBlurredActionBar: Boolean = false

    /**
     * Boolean used to know if RenderScript should be used
     */
    private var mUseRenderScript: Boolean = false

    /**
     * Constructor.
     *
     * @param holdingActivity activity which holds the DialogFragment.
     */
    constructor(holdingActivity: Activity) {
        mHoldingActivity = holdingActivity
        mAnimationDuration = holdingActivity.resources.getInteger(R.integer.xblur_dialog_animation_duration)
    }

    /**
     * Must be linked to the original lifecycle.
     *
     * @param activity holding activity.
     */
    fun onAttach(activity: Activity) {
        mHoldingActivity = activity
    }

    /**
     * Resume the engine.
     *
     * @param retainedInstance use getRetainInstance.
     */
    fun onResume(retainedInstance: Boolean) {
        if (mBlurredBackgroundView == null || retainedInstance) {
            if (mHoldingActivity!!.window.decorView.isShown) {
                mBluringTask = XBlurAsyncTask()
                mBluringTask?.execute()
            } else {
                mHoldingActivity!!.window.decorView.viewTreeObserver.addOnPreDrawListener(
                    object : ViewTreeObserver.OnPreDrawListener {
                        override fun onPreDraw(): Boolean {
                            // dialog can have been closed before being drawn
                            if (mHoldingActivity != null) {
                                mHoldingActivity!!.window.decorView
                                    .viewTreeObserver.removeOnPreDrawListener(this)
                                mBluringTask = XBlurAsyncTask()
                                mBluringTask?.execute()
                            }
                            return true
                        }
                    }
                )
            }
        }
    }

    /**
     * Must be linked to the original lifecycle.
     */
    @SuppressLint("NewApi")
    fun onDismiss() {
        //remove blurred background and clear memory, could be null if dismissed before blur effect
        //processing ends
        //cancel async task
        if (mBluringTask != null) {
            mBluringTask?.cancel(true)
        }
        if (mBlurredBackgroundView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                mBlurredBackgroundView!!
                    .animate()
                    .alpha(0f)
                    .setDuration(mAnimationDuration.toLong())
                    .setInterpolator(AccelerateInterpolator())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            removeBlurredView()
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            super.onAnimationCancel(animation)
                            removeBlurredView()
                        }
                    }).start()
            } else {
                removeBlurredView()
            }
        }
    }

    /**
     * Must be linked to the original lifecycle.
     */
    fun onDetach() {
        if (mBluringTask != null) {
            mBluringTask?.cancel(true)
        }
        mBluringTask = null
        mHoldingActivity = null
    }

    /**
     * Enable / disable debug mode.
     *
     *
     * LogCat and graphical information directly on blurred screen.
     *
     * @param enable true to display log in LogCat.
     */
    fun debug(enable: Boolean) {
        mDebugEnable = enable
    }

    /**
     * Apply custom down scale factor.
     *
     *
     * By default down scale factor is set to
     * [XBlurDialogEngine.DEFAULT_BLUR_DOWN_SCALE_FACTOR]
     *
     *
     * Higher down scale factor will increase blurring speed but reduce final rendering quality.
     *
     * @param factor customized down scale factor, must be at least 1.0 ( no down scale applied )
     */
    fun setDownScaleFactor(factor: Float) {
        if (factor >= 1.0f) {
            mDownScaleFactor = factor
        } else {
            mDownScaleFactor = 1.0f
        }
    }

    /**
     * Apply custom blur radius.
     *
     *
     * By default blur radius is set to
     * [XBlurDialogEngine.DEFAULT_BLUR_RADIUS]
     *
     * @param radius custom radius used to blur.
     */
    fun setBlurRadius(radius: Int) {
        if (radius >= 0) {
            mBlurRadius = radius
        } else {
            mBlurRadius = 0
        }
    }

    /**
     * Set use of RenderScript
     *
     *
     * By default RenderScript is set to
     * [XBlurDialogEngine.DEFAULT_USE_RENDERSCRIPT]
     *
     *
     * Don't forget to add those lines to your build.gradle
     * <pre>
     * defaultConfig {
     * ...
     * renderscriptTargetApi 22
     * renderscriptSupportModeEnabled true
     * ...
     * }
    </pre> *
     *
     * @param useRenderScript use of RenderScript
     */
    fun setUseRenderScript(useRenderScript: Boolean) {
        mUseRenderScript = useRenderScript
    }

    /**
     * Enable / disable blurred action bar.
     *
     *
     * When enabled, the action bar is blurred in addition of the content.
     *
     * @param enable true to blur the action bar.
     */
    fun setBlurActionBar(enable: Boolean) {
        mBlurredActionBar = enable
    }

    /**
     * Set a toolbar which isn't set as action bar.
     *
     * @param toolbar toolbar.
     */
    fun setToolbar(toolbar: Toolbar) {
        mToolbar = toolbar
    }

    /**
     * Blur the given bitmap and add it to the activity.
     *
     * @param bkg  should be a bitmap of the background.
     * @param view background view.
     */
    private fun blur(bkg: Bitmap, view: View) {
        val startMs = System.currentTimeMillis()
        //define layout params to the previous imageView in order to match its parent
        mBlurredBackgroundLayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        //overlay used to build scaled preview and blur background
        var overlay: Bitmap? = null

        //evaluate top offset due to action bar, 0 if the actionBar should be blurred.
        val actionBarHeight: Int
        if (mBlurredActionBar) {
            actionBarHeight = 0
        } else {
            actionBarHeight = getActionBarHeight()
        }

        //evaluate top offset due to status bar
        var statusBarHeight = 0
        if (mHoldingActivity!!.window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN === 0) {
            //not in fullscreen mode
            statusBarHeight = getStatusBarHeight()
        }

        // check if status bar is translucent to remove status bar offset in order to provide blur
        // on content bellow the status.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && isStatusBarTranslucent()) {
            statusBarHeight = 0
        }

        val topOffset = actionBarHeight + statusBarHeight
        // evaluate bottom or right offset due to navigation bar.
        var bottomOffset = 0
        var rightOffset = 0
        val navBarSize = getNavigationBarOffset()

        if (mHoldingActivity!!.resources.getBoolean(R.bool.blur_dialog_has_bottom_navigation_bar)) {
            bottomOffset = navBarSize
        } else {
            rightOffset = navBarSize
        }

        //add offset to the source boundaries since we don't want to blur actionBar pixels
        val srcRect = Rect(
            0,
            topOffset,
            bkg.width - rightOffset,
            bkg.height - bottomOffset
        )

        //in order to keep the same ratio as the one which will be used for rendering, also
        //add the offset to the overlay.
        val height = Math.ceil((view.height - topOffset - bottomOffset).toDouble() / mDownScaleFactor)
        val width = Math.ceil((view.width - rightOffset) * height / (view.height - topOffset - bottomOffset))

        // Render script doesn't work with RGB_565
        if (mUseRenderScript) {
            overlay = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
        } else {
            overlay = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.RGB_565)
        }
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
                //|| mHoldingActivity is ActionBarActivity
                || mHoldingActivity is AppCompatActivity
            ) {
                //add offset as top margin since actionBar height must also considered when we display
                // the blurred background. Don't want to draw on the actionBar.
                mBlurredBackgroundLayoutParams?.setMargins(0, actionBarHeight, 0, 0)
                mBlurredBackgroundLayoutParams?.gravity = Gravity.TOP
            }
        } catch (e: NoClassDefFoundError) {
            // no dependency to appcompat, that means no additional top offset due to actionBar.
            mBlurredBackgroundLayoutParams?.setMargins(0, 0, 0, 0)
        }

        //scale and draw background view on the canvas overlay
        val canvas = Canvas(overlay!!)
        val paint = Paint()
        paint.flags = Paint.FILTER_BITMAP_FLAG

        //build drawing destination boundaries
        val destRect = RectF(0f, 0f, overlay.width.toFloat(), overlay.height.toFloat())

        //draw background from source area in source background to the destination area on the overlay
        canvas.drawBitmap(bkg, srcRect, destRect, paint)

        //apply fast blur on overlay
        if (mUseRenderScript) {
            overlay = XRenderScriptBlurHelper.doBlur(overlay, mBlurRadius, true, mHoldingActivity!!)
        } else {
            overlay = XFastBlurHelper.doBlur(overlay, mBlurRadius, true)
        }
        if (mDebugEnable) {
            val blurTime = (System.currentTimeMillis() - startMs).toString() + " ms"
            Log.d(TAG, "Blur method : " + if (mUseRenderScript) "RenderScript" else "FastBlur")
            Log.d(TAG, "Radius : $mBlurRadius")
            Log.d(TAG, "Down Scale Factor : $mDownScaleFactor")
            Log.d(TAG, "Blurred achieved in : $blurTime")
            Log.d(
                TAG, "Allocation : " + bkg.rowBytes + "ko (screen capture) + "
                        + overlay!!.rowBytes + "ko (blurred bitmap)"
                        + if (!mUseRenderScript) " + temp buff " + overlay.rowBytes + "ko." else "."
            )
            val bounds = Rect()
            val canvas1 = Canvas(overlay)
            paint.color = Color.BLACK
            paint.isAntiAlias = true
            paint.textSize = 20.0f
            paint.getTextBounds(blurTime, 0, blurTime.length, bounds)
            canvas1.drawText(blurTime, 2.toFloat(), bounds.height().toFloat(), paint)
        }
        //set bitmap in an image view for final rendering
        mBlurredBackgroundView = ImageView(mHoldingActivity)
        mBlurredBackgroundView?.scaleType = ImageView.ScaleType.CENTER_CROP
        mBlurredBackgroundView!!.setImageDrawable(BitmapDrawable(mHoldingActivity!!.resources, overlay))
    }

    /**
     * Retrieve action bar height.
     *
     * @return action bar height in px.
     */
    private fun getActionBarHeight(): Int {
        var actionBarHeight = 0

        try {
            if (mToolbar != null) {
                actionBarHeight = mToolbar!!.height
            } /*else if (mHoldingActivity is ActionBarActivity) {
                val supportActionBar = (mHoldingActivity as ActionBarActivity).getSupportActionBar()
                if (supportActionBar != null) {
                    actionBarHeight = supportActionBar!!.getHeight()
                }
            }*/ else if (mHoldingActivity is AppCompatActivity) {
                val supportActionBar = (mHoldingActivity as AppCompatActivity).supportActionBar
                if (supportActionBar != null) {
                    actionBarHeight = supportActionBar.height
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                val actionBar = mHoldingActivity!!.actionBar
                if (actionBar != null) {
                    actionBarHeight = actionBar.height
                }
            }
        } catch (e: NoClassDefFoundError) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                val actionBar = mHoldingActivity!!.actionBar
                if (actionBar != null) {
                    actionBarHeight = actionBar.height
                }
            }
        }

        return actionBarHeight
    }

    /**
     * retrieve status bar height in px
     *
     * @return status bar height in px
     */
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = mHoldingActivity!!.resources
            .getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = mHoldingActivity!!.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    /**
     * Retrieve offset introduce by the navigation bar.
     *
     * @return bottom offset due to navigation bar.
     */
    private fun getNavigationBarOffset(): Int {
        var result = 0
        val resources = mHoldingActivity!!.resources
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            if (resourceId > 0) {
                result = resources.getDimensionPixelSize(resourceId)
            }
        }
        return result
    }

    /**
     * Used to check if the status bar is translucent.
     *
     * @return true if the status bar is translucent.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun isStatusBarTranslucent(): Boolean {
        val typedValue = TypedValue()
        val attribute = intArrayOf(android.R.attr.windowTranslucentStatus)
        val array = mHoldingActivity!!.obtainStyledAttributes(typedValue.resourceId, attribute)
        val isStatusBarTranslucent = array.getBoolean(0, false)
        array.recycle()
        return isStatusBarTranslucent
    }

    /**
     * Removed the blurred view from the view hierarchy.
     */
    private fun removeBlurredView() {
        if (mBlurredBackgroundView != null) {
            val parent = mBlurredBackgroundView!!.parent as ViewGroup
            parent.removeView(mBlurredBackgroundView)
            mBlurredBackgroundView = null
        }
    }

    /**
     * Async task used to process blur out of ui thread
     */
    @SuppressLint("StaticFieldLeak")
    private inner class XBlurAsyncTask : AsyncTask<Void, Void, Void>() {

        private var mBackground: Bitmap? = null
        private var mBackgroundView: View? = null

        override fun onPreExecute() {
            super.onPreExecute()

            mBackgroundView = mHoldingActivity!!.window.decorView

            //retrieve background view, must be achieved on ui thread since
            //only the original thread that created a view hierarchy can touch its views.

            val rect = Rect()
            mBackgroundView!!.getWindowVisibleDisplayFrame(rect)
            mBackgroundView!!.destroyDrawingCache()
            mBackgroundView!!.isDrawingCacheEnabled = true
            mBackgroundView!!.buildDrawingCache(true)
            mBackground = mBackgroundView!!.getDrawingCache(true)

            /**
             * After rotation, the DecorView has no height and no width. Therefore
             * .getDrawingCache() return null. That's why we  have to force measure and layout.
             */
            if (mBackground == null) {
                mBackgroundView!!.measure(
                    View.MeasureSpec.makeMeasureSpec(rect.width(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(rect.height(), View.MeasureSpec.EXACTLY)
                )
                mBackgroundView!!.layout(
                    0, 0, mBackgroundView!!.measuredWidth,
                    mBackgroundView!!.measuredHeight
                )
                mBackgroundView!!.destroyDrawingCache()
                mBackgroundView!!.isDrawingCacheEnabled = true
                mBackgroundView!!.buildDrawingCache(true)
                mBackground = mBackgroundView!!.getDrawingCache(true)
            }
        }

        override fun doInBackground(vararg params: Void): Void? {
            //process to the blue
            if (!isCancelled) {
                blur(mBackground!!, mBackgroundView!!)
            } else {
                return null
            }
            //clear memory
            mBackground!!.recycle()
            return null
        }

        @SuppressLint("NewApi")
        override fun onPostExecute(aVoid: Void) {
            super.onPostExecute(aVoid)

            mBackgroundView!!.destroyDrawingCache()
            mBackgroundView!!.isDrawingCacheEnabled = false

            mHoldingActivity!!.window.addContentView(
                mBlurredBackgroundView,
                mBlurredBackgroundLayoutParams
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                mBlurredBackgroundView?.alpha = 0f
                mBlurredBackgroundView!!
                    .animate()
                    .alpha(1f)
                    .setDuration(mAnimationDuration.toLong())
                    .setInterpolator(LinearInterpolator())
                    .start()
            }
            mBackgroundView = null
            mBackground = null
        }
    }

    companion object {
        /**
         * Since image is going to be blurred, we don't care about resolution.
         * Down scale factor to reduce blurring time and memory allocation.
         */
        val DEFAULT_BLUR_DOWN_SCALE_FACTOR = 4.0f

        /**
         * Radius used to blur the background
         */
        val DEFAULT_BLUR_RADIUS = 8

        /**
         * Default dimming policy.
         */
        val DEFAULT_DIMMING_POLICY = false

        /**
         * Default debug policy.
         */
        val DEFAULT_DEBUG_POLICY = false

        /**
         * Default action bar blurred policy.
         */
        val DEFAULT_ACTION_BAR_BLUR = false

        /**
         * Default use of RenderScript.
         */
        val DEFAULT_USE_RENDERSCRIPT = false

        /**
         * Log cat
         */
        val TAG = XBlurDialogEngine::class.java.simpleName
    }
}