package com.mahsoud.android.apps.xblurdialog

import android.annotation.TargetApi
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mahsoud.android.libs.xblurdialogfragment.XBlurDialogFragment

/**
 * Bundle key used to start the blur dialog with a given scale factor (float).
 */
private const val BUNDLE_KEY_DOWN_SCALE_FACTOR = "bundle_key_down_scale_factor"

/**
 * Bundle key used to start the blur dialog with a given blur radius (int).
 */
private const val BUNDLE_KEY_BLUR_RADIUS = "bundle_key_blur_radius"

/**
 * Bundle key used to start the blur dialog with a given dimming effect policy.
 */
private const val BUNDLE_KEY_DIMMING = "bundle_key_dimming_effect"

/**
 * Bundle key used to start the blur dialog with a given debug policy.
 */
private const val BUNDLE_KEY_DEBUG = "bundle_key_debug_effect"

/**
 * A simple [XBlurDialogFragment] subclass.
 * Activities that contain this fragment must implement the
 * [SampleDialogFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [SampleDialogFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
/**
 * Simple fragment with blur effect behind.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
class SampleDialogFragment : XBlurDialogFragment() {
    private var mRadius: Int = 0
    private var mDownScaleFactor: Float = 0.toFloat()
    private var mDimming: Boolean = false
    private var mDebug: Boolean = false

    private var listener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

            mRadius = it.getInt(BUNDLE_KEY_BLUR_RADIUS);
            mDownScaleFactor = it.getFloat(BUNDLE_KEY_DOWN_SCALE_FACTOR);
            mDimming = it.getBoolean(BUNDLE_KEY_DIMMING);
            mDebug = it.getBoolean(BUNDLE_KEY_DEBUG);
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sample_dialog, container, false)
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun isDebugEnable(): Boolean {
        return mDebug
    }

    override fun isDimmingEnable(): Boolean {
        return mDimming
    }

    override fun isActionBarBlurred(): Boolean {
        return true
    }

    override fun getDownScaleFactor(): Float {
        return mDownScaleFactor
    }

    override fun getBlurRadius(): Int {
        return mRadius
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        var TAG = SampleDialogFragment::class.java.simpleName

        /**
         * Retrieve a new instance of the sample fragment.
         *
         * @param radius          blur radius.
         * @param downScaleFactor down scale factor.
         * @param dimming         dimming effect.
         * @param debug           debug policy.
         * @return well instantiated fragment.
         */
        @JvmStatic
        fun newInstance(radius: Int,
                        downScaleFactor: Float,
                        dimming: Boolean,
                        debug: Boolean) =
            SampleDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(BUNDLE_KEY_BLUR_RADIUS, radius)
                    putFloat(BUNDLE_KEY_DOWN_SCALE_FACTOR, downScaleFactor)
                    putBoolean(BUNDLE_KEY_DIMMING, dimming)
                    putBoolean(BUNDLE_KEY_DEBUG, debug)
                }
            }
    }
}
