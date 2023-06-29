package com.minhyuuk.footviewer

import android.opengl.GLES20
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer


class Mesh(ply_file: InputStream?) {
  private var mMVPMatrixHandle = 0
  private var mPositionHandle = 0
  private var mColorHandle = 0
  private lateinit var bufferIdx: IntArray
  private var vertexCount = 0
  private var facesCount = 0
  var vertexBuffer: FloatBuffer? = null
    private set
  private var colorBuffer: FloatBuffer? = null
  private val facesBuffer: IntBuffer? = null
  private val parser: PlyParser
  private var mProgram = 0
  private val stride = (COORDS_PER_COLOR + COORDS_PER_VERTEX) * BYTES_PER_FLOAT

  fun loadShader(type: Int, shaderCode: String?): Int {
    var shader = GLES20.glCreateShader(type)
    // add the source code to the shader and compile it
    GLES20.glShaderSource(shader, shaderCode)
    GLES20.glCompileShader(shader)
    val compiled = IntArray(1)
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
    if (compiled[0] == 0) {
      Log.e("Shader", "Could not compile shader $type:")
      Log.e("Shader", GLES20.glGetShaderInfoLog(shader))
      GLES20.glDeleteShader(shader)
      shader = 0
    }
    return shader
  }

  protected val vertexShaderCode: String
    protected get() = ("uniform mat4 uMVPMatrix;\n" +
            "attribute vec3 vPosition;\n" +
            "attribute vec4 vColor;\n" +
            "varying vec4 fColor;\n" +
            "void main() {\n" +
            "  gl_Position = uMVPMatrix * vec4(vPosition, 1);\n" +
            "  fColor = vColor;\n" +
            "}\n")
  protected val fragmentShaderCode: String
    protected get() {
      return ("precision mediump float;\n" +
              "varying vec4 fColor;\n" +
              "void main() {\n" +
              "  gl_FragColor = fColor;\n" +
              "}\n")
    }

  protected fun checkGlError(TAG: String?, op: String) {
    var error: Int
    while (GLES20.glGetError().also { error = it } != GLES20.GL_NO_ERROR) {
      Log.e(TAG, "$op: glError $error")
      throw RuntimeException("$op: glError $error")
    }
  }

  // Riffed off of the Android OpenGL ES 2.0 How-to page
  // http://bit.ly/1KVYlAx
  init {
    parser = PlyParser(ply_file)
  }

  @Throws(IOException::class)
  fun createProgram(): Boolean {
    if (!parser.ParsePly()) {
      // It's not a PLY, so don't go any farther
      return false
    }
    vertexCount = parser.vertexCount
    facesCount = parser.faceCount
    bufferIdx = IntArray(3)
    GLES20.glGenBuffers(3, bufferIdx, 0)
    vertexBuffer = ByteBuffer.allocateDirect(parser.vertices!!.size * BYTES_PER_FLOAT)
      .order(ByteOrder.nativeOrder())
      .asFloatBuffer()
    vertexBuffer!!.put(parser.vertices).position(0)
    colorBuffer = ByteBuffer.allocateDirect(parser.colors!!.size * BYTES_PER_FLOAT)
      .order(ByteOrder.nativeOrder())
      .asFloatBuffer()
    colorBuffer!!.put(parser.colors).position(0)
    // TODO(bminortx): check size of int
//    facesBuffer = ByteBuffer.allocateDirect(parser.getFaces().length * BYTES_PER_FLOAT)
//            .order(ByteOrder.nativeOrder())
//            .asIntBuffer();
//    facesBuffer.put(parser.getFaces()).position(0);
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferIdx[0])
    GLES20.glBufferData(
      GLES20.GL_ARRAY_BUFFER,
      vertexBuffer!!.capacity() * BYTES_PER_FLOAT,
      vertexBuffer,
      GLES20.GL_STATIC_DRAW
    )
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferIdx[1])
    GLES20.glBufferData(
      GLES20.GL_ARRAY_BUFFER,
      colorBuffer!!.capacity() * BYTES_PER_FLOAT,
      colorBuffer,
      GLES20.GL_STATIC_DRAW
    )
    //    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferIdx[2]);
//    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
//            facesBuffer.capacity() * BYTES_PER_FLOAT,
//            facesBuffer,
//            GLES20.GL_STATIC_DRAW);
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

    // Get our shaders ready
    val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
    val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
    mProgram = GLES20.glCreateProgram()
    // add the vertex shader to program
    GLES20.glAttachShader(mProgram, vertexShader)
    checkGlError("Cube", "glAttachShader")
    // add the fragment shader to program
    GLES20.glAttachShader(mProgram, fragmentShader)
    checkGlError("Cube", "glAttachShader")
    GLES20.glLinkProgram(mProgram)
    return true
  }

  fun draw(mvpMatrix: FloatArray?) {
    // Add program to OpenGL ES environment
    GLES20.glUseProgram(mProgram)
    // Position Buffer, passed into shader
    mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferIdx[0])
    GLES20.glEnableVertexAttribArray(mPositionHandle)
    GLES20.glVertexAttribPointer(
      mPositionHandle, COORDS_PER_VERTEX,
      GLES20.GL_FLOAT, false, 0, 0
    )
    // Color Buffer, passed into shader
    mColorHandle = GLES20.glGetAttribLocation(mProgram, "vColor")
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferIdx[1])
    GLES20.glEnableVertexAttribArray(mColorHandle)
    GLES20.glVertexAttribPointer(
      mColorHandle, COORDS_PER_COLOR,
      GLES20.GL_FLOAT, false, 0, 0
    )
    // get handle to shape's transformation matrix
    mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
    // Pass the projection and view transformation to the shader
    GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0)
    // Draw the triangle
//    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, bufferIdx[2])
    //    GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, facesCount * 3, GLES20.GL_UNSIGNED_INT, 0);
    GLES20.glDrawArrays(GLES20.GL_POINTS, 0, vertexCount)
    // Disable vertex array
    GLES20.glDisableVertexAttribArray(mPositionHandle)
    GLES20.glDisableVertexAttribArray(mColorHandle)
    GLES20.glFlush()
  }

  companion object {
    // number of coordinates per vertex in this array
    const val BYTES_PER_FLOAT = 4
    const val BYTES_PER_SHORT = 2
    const val COORDS_PER_VERTEX = 3
    const val COORDS_PER_COLOR = 4
  }
}
