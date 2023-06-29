package com.minhyuuk.footviewer

import android.annotation.SuppressLint
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader


class PlyParser(plyFile: InputStream?) {
    // Parser mechanisms
    private val bufferedReader: BufferedReader
    private val NO_INDEX = 100
    private var vertexIndex = NO_INDEX
    private var colorIndex = NO_INDEX
    private var normalIndex = NO_INDEX
    private var inHeader = true
    var currentElement = 0
    var currentFace = 0

    // Getters
    /* data fields to store points, colors, faces information read from PLY file */
    var vertices: FloatArray? = null
    var colors: FloatArray? = null
    var normals: FloatArray? = null
    var faces: IntArray? = null

    // Size of an individual element, in floats
    var vertexSize = 0
    var colorSize = 4
    var normalSize = 0
    var faceSize = 3

    // Normalizing constants
    var vertexMax = 0f
    var colorMax = 0f

    // Number of elements in the entire PLY
    var vertexCount = 0
    var faceCount = 0

    // Counter for header
    private var elementCount = 0

    init {
        bufferedReader = BufferedReader(InputStreamReader(plyFile))
    }

    @Throws(IOException::class)
    fun ParsePly(): Boolean {
        // Check if this is even a PLY file.
        var line = bufferedReader.readLine()
        if (line != "ply") {
            Log.e("ReadHeader", "File is not a PLY! Leave us.")
            return false
        }

        // Check for ASCII format
        line = bufferedReader.readLine()
        val words = line.split(" ").toTypedArray()
        if (words[1] != "ascii") {
            Log.e("ReadHeader", "File is not ASCII format! Cannot read.")
            return false
        }

        // Read the header
        line = bufferedReader.readLine()
        while (line != null && inHeader) {
            ReadHeader(line)
            line = bufferedReader.readLine()
        }

        // Populate the data
        if (vertexSize != 3) {
            Log.e("ParsePly", "Incorrect count of vertices! Expected 3.")
            return false
        }
        vertices = FloatArray(vertexCount * vertexSize)
        faces = IntArray(faceCount * faceSize)
        Log.e("*** vertexCount : ", "" + vertexCount)
        Log.e("*** colorSize : ", "" + colorSize)
        Log.e("*** vertexSize : ", "" + vertexSize)
        Log.e("*** vertices : ", "" + vertices)
        if (colorSize != 0) {
            colors = FloatArray(vertexCount * colorSize)
        }
        if (normalSize != 0) {
            normals = FloatArray(vertexCount * normalSize)
        }
        line = bufferedReader.readLine()
        while (line != null) {
            ReadData(line)
            line = bufferedReader.readLine()
        }
        ScaleData()
        return true
    }

    @SuppressLint("LongLogTag")
    fun ReadHeader(line: String) {
        // Make into a list of words, yo.
        val words = line.split(" ").toTypedArray()
        if (words[0] == "comment") {
            return
        }
        // Check if element or property
        if (words[0] == "element") {
            if (words[1] == "vertex") {
                vertexCount = words[2].toInt()
                Log.e("*** vertex count init : ", "" + vertexCount)
            } else if (words[1] == "face") {
                faceCount = words[2].toInt()
            }
        }
        if (words[0] == "property") {
            if (words[2] == "x" || words[2] == "y" || words[2] == "z") {
                if (vertexIndex > elementCount) {
                    vertexIndex = elementCount
                }
                vertexSize++
            } else if (words[2] == "nx" || words[2] == "ny" || words[2] == "nz") {
                if (normalIndex > elementCount) {
                    normalIndex = elementCount
                }
                normalSize++
            } else if (words[2] == "red" || words[2] == "green" || words[2] == "blue" || words[2] == "alpha") {
                if (colorIndex > elementCount) {
                    colorIndex = elementCount
                    Log.e("*** colorIndex", "" + colorIndex)
                }
                //        colorSize++;
            }
            elementCount++
        }
        if (words[0] == "end_header") {
            inHeader = false
            return
        }
    }

    @SuppressLint("LongLogTag")
    fun ReadData(line: String) {
        val words = line.split(" ").toTypedArray()
        // Compensate for extra line read with (vertexCount - 1)
        if (currentElement < vertexCount - 1) {
            for (i in 0 until vertexSize) {
                vertices!![currentElement * vertexSize + i] = words[vertexIndex + i].toFloat()
                if (vertexMax < Math.abs(vertices!![currentElement * vertexSize + i])) {
                    vertexMax = Math.abs(vertices!![currentElement * vertexSize + i])
                }
            }
            for (i in 0 until colorSize) {
                if (i == 3) {
                    colors!![currentElement * colorSize + i] = 1.0f
                } else {
                    colors!![currentElement * colorSize + i] = words[colorIndex + i].toFloat()
                }
                if (colorMax < colors!![currentElement * colorSize + i]) {
                    colorMax = colors!![currentElement * colorSize + i]
                }
            }
            for (i in 0 until normalSize) {
                normals!![currentElement * normalSize + i] = words[normalIndex + i].toFloat()
            }
            currentElement++
        }
    }

    fun ScaleData() {
        for (i in 0 until vertexCount * vertexSize) {
            vertices!![i] /= vertexMax
        }
        for (i in 0 until vertexCount * colorSize) {
            colors!![i] /= colorMax
        }
    }
}
