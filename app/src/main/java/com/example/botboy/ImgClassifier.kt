package com.example.botboy

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.lang.Exception
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class TfDetectResult(val id : String?, val title: String?,
                     val confidence : Float?, val locn : RectF?) {
    override fun toString(): String {
        return String.format("Object num: %s\nType: %s\nprob: %f\nLocation: %s",
            id!!, title!!, confidence!!, locn.toString())
    }
}

class ImgClassifier constructor(private val assetManager: AssetManager){
    val INPUT_SIZE = 320
    val DIM_IMG_SIZE_X = 320
    val DIM_IMG_SIZE_Y = 320
    private var interpreter: Interpreter? = null

    private lateinit var labelProb: Array<ByteArray>
    private var outputLocations = Array(1) {Array(20) {FloatArray(4)} }
    private var outputClasses = Array(1){FloatArray(20)}
    private var outputProbs = Array(1){FloatArray(20)}
    private var numDetectinos = FloatArray(1)
    private var outputMap = HashMap<Int, Any>()
    private val labels = Vector<String>()
    private val intValues by lazy { IntArray(INPUT_SIZE * INPUT_SIZE) }
    private lateinit var imgData: ByteBuffer
    private var ctx : Context? = null
    init {
        try {
            labels.add("background")
            labels.add("tball")
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        outputMap[0] = outputLocations
        outputMap[1] = outputClasses
        outputMap[2] = outputProbs
        outputMap[3] = numDetectinos
        labelProb = Array(1) {ByteArray(labels.size)}
        imgData = ByteBuffer.allocateDirect(1 * INPUT_SIZE  * INPUT_SIZE * 3)
        imgData!!.order(ByteOrder.nativeOrder())
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
        if (imgData == null) return
        imgData!!.rewind()
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until DIM_IMG_SIZE_X) {
            for (j in 0 until DIM_IMG_SIZE_Y) {
                val value = intValues!![pixel++]
                imgData!!.put((value shr 16 and 0xFF).toByte())
                imgData!!.put((value shr 8 and 0xFF).toByte())
                imgData!!.put((value and 0xFF).toByte())
            }
        }
        try {
            Log.i("TEJA", "Initializing interpreter")
            if (interpreter == null) {
                interpreter = Interpreter(loadModelFile())
            }
            Log.i("TEJA", "Interpreter initialized")
        } catch (e : Exception) {
            throw RuntimeException(e)
        }
    }

    public fun recognizeImage(bitmap: Bitmap) : List<TfDetectResult> {
        Log.i("TEJA", "Recognize image called")
        convertBitmapToByteBuffer(bitmap)
        Log.i("TEJA", "Calling interpreter")
        var inputObj = Array<Any>(1){}
        inputObj[0] = imgData
        interpreter!!.runForMultipleInputsOutputs(inputObj, outputMap)
        var numObjsFoundn = numDetectinos[0].toInt()
        var output = ArrayList<TfDetectResult>(numObjsFoundn)
        Log.i("TEJA", "Got output" + output.size)
        Log.i("TEJA", "NumFound" + numObjsFoundn)
        var numObjsFound : Int = 0
        for (i in 0..numObjsFoundn-1) {
            // Log.i("TEJA", "WHAA" + outputClasses[0][i].toString())
            if (outputClasses[0][i].toInt() == 0) {
                // 0th is background. We are interested in 1, i.e, tball.
                continue
            }
            numObjsFound++
            output.add(TfDetectResult(i.toString(),
                "ball",
                outputProbs[0][i],
                RectF(
                    outputLocations[0][i][1] * 320,
                    outputLocations[0][i][0] * 320,
                    outputLocations[0][i][3] * 320,
                    outputLocations[0][i][2] * 320
                )
            ))
            Log.i("TEJASWRP", output.last().toString())
        }
        output.trimToSize()
        return output
    }

    fun loadModelFile() : MappedByteBuffer {
        var fd = assetManager.openFd("tennisballmodel.tflite")
        var inputStream = FileInputStream(fd!!.fileDescriptor)
        var fileChannel = inputStream.channel
        Log.i("TEJA", "final channel acquired")
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fd!!.startOffset, fd.declaredLength)
    }
}
