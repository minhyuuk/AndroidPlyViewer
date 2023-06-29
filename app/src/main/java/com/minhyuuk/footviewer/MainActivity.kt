package com.minhyuuk.footviewer

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import android.view.View


class MainActivity : Activity() {
    var view: GLView? = null
    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.activity_main)
        view = findViewById<View>(R.id.gl_view) as GLView
    }

    override fun onPause() {
        super.onPause()
        view?.onPause()
    }

    override fun onResume() {
        super.onResume()
        view?.onResume()
    }

    override fun onConfigurationChanged(conf: Configuration) {
        super.onConfigurationChanged(conf)
    }

    fun Reset(v: View?) {
        view?.Reset()
    }
}
