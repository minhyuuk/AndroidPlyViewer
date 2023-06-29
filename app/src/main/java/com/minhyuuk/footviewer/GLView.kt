package com.minhyuuk.footviewer

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import java.io.IOException
import java.io.InputStream
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class GLView(context: Context, attributeSet: AttributeSet?) :
    GLSurfaceView(context, attributeSet) {
    private val renderer: Renderer?
    var SGD: ScaleGestureDetector
    private var mPreviousX = 0f
    private var mPreviousY = 0f

    //  Constructor
    init {
        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2)
        // Set the Renderer for drawing on the GLSurfaceView
        renderer = Renderer(context)
        setRenderer(renderer)
        SGD = ScaleGestureDetector(context, ScaleListener())
    }

    private inner class ScaleListener : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            renderer!!.scale *= detector.scaleFactor
            return true
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.
        val x = e.x
        val y = e.y
        val motionaction = e.action and MotionEvent.ACTION_MASK
        when (motionaction) {
            MotionEvent.ACTION_DOWN -> {
                // Prevent jumping around.
                mPreviousX = x
                mPreviousY = y
            }
            MotionEvent.ACTION_MOVE -> {
                if (renderer != null) {
                    // deltaX,Y 로 터치시 이동속도 조절
                    val deltaX = (x - mPreviousX) / 7f // 원래 2 , 빨라서 6으로 변경
                    val deltaY = (y - mPreviousY) / 7f
                    renderer.mDeltaX += deltaX
                    renderer.mDeltaY += deltaY
                    //          Log.e("renderer.mDeltaX",""+renderer.mDeltaX);
//          Log.e("renderer.mDeltaY",""+renderer.mDeltaY);
                }
                mPreviousX = x
                mPreviousY = y
            }
        }
        SGD.onTouchEvent(e)
        return true
    }

    // Called when reset button is pressed.
    fun Reset() {
        renderer!!.scale = 1f
        Matrix.setIdentityM(renderer.mAccumulatedRotation, 0)
    }

    class Renderer(var current_context: Context) : GLSurfaceView.Renderer {
        private var cube: Cube? = null
        private var mesh: Mesh? = null

        // Intrinsic Matrices
        private val mModelMatrix = FloatArray(16)
        private val mViewMatrix = FloatArray(16)

        // Projection matrix is set in onSurfaceChanged()
        private val mProjectionMatrix = FloatArray(16)
        private val mMVPMatrix = FloatArray(16)

        // Rotations for our touch movements
        val mAccumulatedRotation = FloatArray(16)
        private val mCurrentRotation = FloatArray(16)

        @Volatile
        var mDeltaX = 0f

        @Volatile
        var mDeltaY = 0f

        @Volatile
        var scale = 1f
        private val plyInput: InputStream
        var averageX = 0f
        var averageY = 0f
        var averageZ = 0f
        var sumX = 0f
        var sumY = 0f
        var sumZ = 0f

        init {
            // Just in case we don't use the PLY in the future,
            // we need to give the user the option of switching out.
            plyInput = current_context.resources.openRawResource(R.raw.controller_ascii)
        }

        override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
            // Initialize the accumulated rotation matrix
            Matrix.setIdentityM(mAccumulatedRotation, 0)
            // TODO(bminortx): put in loader for mesh
            cube = Cube()
            try {
                mesh = Mesh(plyInput)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            try {
                mesh!!.createProgram()
                val floatBuffer: FloatBuffer = mesh!!.vertexBuffer!!
                val capacity = floatBuffer.capacity()
                for (i in 0 until capacity / 3) {
                    sumX += floatBuffer[i * 3]
                    sumY += floatBuffer[i * 3 + 1]
                    sumZ += floatBuffer[i * 3 + 2]
                }
                averageX = sumX / capacity / 3
                averageY = sumY / capacity / 3
                averageZ = sumZ / capacity / 3
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        override fun onDrawFrame(gl: GL10) {
            val mTemporaryMatrix = FloatArray(16)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            // Model, View, and Projection
            Matrix.setIdentityM(mModelMatrix, 0)
            Matrix.scaleM(mModelMatrix, 0, scale, scale, scale)


//      Matrix.setLookAtM(mViewMatrix, 0, 1, 1, -10, 0, 0, 0, 0.0f, -1.0f, 0.0f);
            Matrix.setLookAtM(
                mViewMatrix,
                0,
                -0.2f,
                0.7f,
                -3f,
                averageX,
                averageY,
                averageZ,
                0.0f,
                -1.0f,
                0.0f
            )
            // Set a matrix that contains the current rotation.
            // Code below adapted from http://www.learnopengles.com/rotating-an-object-with-touch-events/
            Matrix.setIdentityM(mCurrentRotation, 0)
            Matrix.rotateM(mCurrentRotation, 0, mDeltaX, 0.0f, 1.0f, 0.0f)
            Matrix.rotateM(mCurrentRotation, 0, mDeltaY, -1.0f, 0.0f, 0.0f)
            mDeltaX = 0.0f
            mDeltaY = 0.0f
            // Multiply the current rotation by the accumulated rotation,
            // and then set the accumulated rotation to the result.
            Matrix.multiplyMM(
                mTemporaryMatrix, 0,
                mCurrentRotation, 0,
                mAccumulatedRotation, 0
            )
            System.arraycopy(
                mTemporaryMatrix, 0,
                mAccumulatedRotation, 0, 16
            )
            // Rotate the cube taking the overall rotation into account.
            Matrix.multiplyMM(
                mTemporaryMatrix, 0,
                mModelMatrix, 0,
                mAccumulatedRotation, 0
            )
            System.arraycopy(mTemporaryMatrix, 0, mModelMatrix, 0, 16)
            // Calculate the projection and view transformation
            Matrix.multiplyMM(mTemporaryMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mTemporaryMatrix, 0)
            // Draw shape
            mesh!!.draw(mMVPMatrix)
        }

        override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            val ratio = width.toFloat() / height
            // this projection matrix is applied to object coordinates
            // in the onDrawFrame() method
//      Log.e("ratio",""+-ratio);
//      Log.e("ratio",""+ratio);
//      Matrix.perspectiveM(mProjectionMatrix, 0, 100f, 0.5f, 1f, 100f);
//      Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 0.7f, 100);
            Matrix.orthoM(mProjectionMatrix, 0, -ratio, ratio, -1f, 1f, 0f, 100f)
        }
    }
}
