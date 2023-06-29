package com.minhyuuk.footviewer.util

import android.annotation.TargetApi
import android.app.Activity
import android.os.Build
import android.view.View
import android.view.View.OnSystemUiVisibilityChangeListener
import android.view.WindowManager


/**
 * An API 11+ implementation of [SystemUiHider]. Uses APIs available in
 * Honeycomb and later (specifically [View.setSystemUiVisibility]) to
 * show and hide the system UI.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
open class SystemUiHiderHoneycomb(
    activity: Activity?,
    anchorView: View?,
    flags: Int
) :
    SystemUiHiderBase(activity, anchorView, flags) {
    /**
     * Flags for [View.setSystemUiVisibility] to use when showing the
     * system UI.
     */
    private var mShowFlags: Int = 0

    /**
     * Flags for [View.setSystemUiVisibility] to use when hiding the
     * system UI.
     */
    private var mHideFlags: Int

    /**
     * Flags to test against the first parameter in
     * [android.view.View.OnSystemUiVisibilityChangeListener.onSystemUiVisibilityChange]
     * to determine the system UI visibility state.
     */
    private var mTestFlags: Int = 1
    /**
     * {@inheritDoc}
     */
    /**
     * Whether or not the system UI is currently visible. This is cached from
     * [android.view.View.OnSystemUiVisibilityChangeListener].
     */
    override var isVisible = true
        set(isVisible) {
            super.isVisible = isVisible
        }

    /**
     * {@inheritDoc}
     */
    override fun setup() {
        mAnchorView.setOnSystemUiVisibilityChangeListener(mSystemUiVisibilityChangeListener)
    }

    /**
     * {@inheritDoc}
     */
    override fun hide() {
        mAnchorView.systemUiVisibility = mHideFlags
    }

    /**
     * {@inheritDoc}
     */
    override fun show() {
        mAnchorView.systemUiVisibility = mShowFlags
    }

    private val mSystemUiVisibilityChangeListener: OnSystemUiVisibilityChangeListener =
        object : OnSystemUiVisibilityChangeListener {
            override fun onSystemUiVisibilityChange(vis: Int) {
                // Test against mTestFlags to see if the system UI is visible.
                if (vis and mTestFlags != 0) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        // Pre-Jelly Bean, we must manually hide the action bar
                        // and use the old window flags API.
                        mActivity.actionBar!!.hide()
                        mActivity.window.setFlags(
                            WindowManager.LayoutParams.FLAG_FULLSCREEN,
                            WindowManager.LayoutParams.FLAG_FULLSCREEN
                        )
                    }

                    // Trigger the registered listener and cache the visibility
                    // state.
                    mOnVisibilityChangeListener!!.onVisibilityChange(false)
                    isVisible = false
                } else {
                    mAnchorView.systemUiVisibility = this@SystemUiHiderHoneycomb.mShowFlags
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        // Pre-Jelly Bean, we must manually show the action bar
                        // and use the old window flags API.
                        mActivity.actionBar!!.show()
                        mActivity.window.setFlags(
                            0,
                            WindowManager.LayoutParams.FLAG_FULLSCREEN
                        )
                    }

                    // Trigger the registered listener and cache the visibility
                    // state.
                    mOnVisibilityChangeListener!!.onVisibilityChange(true)
                    isVisible = true
                }
            }
        }

    /**
     * Constructor not intended to be called by clients. Use
     * [SystemUiHider.getInstance] to obtain an instance.
     */
    init {
        mShowFlags = View.SYSTEM_UI_FLAG_VISIBLE
        mHideFlags = View.SYSTEM_UI_FLAG_LOW_PROFILE
        mTestFlags = View.SYSTEM_UI_FLAG_LOW_PROFILE
        if (mFlags and FLAG_FULLSCREEN !== 0) {
            // If the client requested fullscreen, add flags relevant to hiding
            // the status bar. Note that some of these constants are new as of
            // API 16 (Jelly Bean). It is safe to use them, as they are inlined
            // at compile-time and do nothing on pre-Jelly Bean devices.
            mShowFlags = mShowFlags or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            mHideFlags = mHideFlags or (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
        if (mFlags and FLAG_HIDE_NAVIGATION !== 0) {
            // If the client requested hiding navigation, add relevant flags.
            mShowFlags = mShowFlags or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            mHideFlags = mHideFlags or (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            mTestFlags = mTestFlags or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
    }
}
