package com.example.botboy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.media.Image
import android.media.Image.Plane
import android.util.Log
import android.util.Size
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.concurrent.Semaphore


class BallDetectHelper {
    var ctx : Context? = null
    var imgClassifier : ImgClassifier? = null
    constructor(context: Context) {
        ctx = context
        if (ctx == null) {
            Log.i("TEJA", "Context is null")
            return
        }
        imgClassifier = ImgClassifier(ctx!!.assets)
    }


    private var rgbBytes: IntArray? = null
    private var rgbFrameBitmap : Bitmap? = null
    private var croppedBitmap : Bitmap? = null
    public fun setSize(s : Size) {
        rgbBytes = IntArray(s.height * s.width)
    }

    private val yuvBytes = arrayOfNulls<ByteArray>(3)

    protected fun fillBytes(planes: Array<Plane>, yuvBytes: Array<ByteArray?>) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (i in planes.indices) {
            val buffer = planes[i].buffer
            if (yuvBytes[i] == null) {
                yuvBytes[i] = ByteArray(buffer.capacity())
            }
            buffer.get(yuvBytes[i])
        }
    }
    val kMaxChannelValue = 262143

    private fun YUV2RGB(nY: Int, nU: Int, nV: Int): Int {
        var nY = nY
        var nU = nU
        var nV = nV
        nY -= 16
        nU -= 128
        nV -= 128
        if (nY < 0) nY = 0

        // This is the floating point equivalent. We do the conversion in integer
        // because some Android devices do not have floating point in hardware.
        // nR = (int)(1.164 * nY + 2.018 * nU);
        // nG = (int)(1.164 * nY - 0.813 * nV - 0.391 * nU);
        // nB = (int)(1.164 * nY + 1.596 * nV);

        var nR = (1192 * nY + 1634 * nV).toInt()
        var nG = (1192 * nY - 833 * nV - 400 * nU).toInt()
        var nB = (1192 * nY + 2066 * nU).toInt()

        nR = Math.min(kMaxChannelValue, Math.max(0, nR))
        nG = Math.min(kMaxChannelValue, Math.max(0, nG))
        nB = Math.min(kMaxChannelValue, Math.max(0, nB))

        nR = nR shr 10 and 0xff
        nG = nG shr 10 and 0xff
        nB = nB shr 10 and 0xff

        return 0xff000000.toInt() or (nR shl 16) or (nG shl 8) or nB
    }

    private fun convertYUV420ToARGB8888(yData: ByteArray, uData: ByteArray, vData: ByteArray, width: Int, height: Int,
                                        yRowStride: Int, uvRowStride: Int, uvPixelStride: Int, out: IntArray) {
        var i = 0
        for (y in 0..height - 1) {
            val pY = yRowStride * y
            val uv_row_start = uvRowStride * (y shr 1)
            val pU = uv_row_start
            val pV = uv_row_start

            for (x in 0..width - 1) {
                val uv_offset = (x shr 1) * uvPixelStride
                out[i++] = YUV2RGB(
                    convertByteToInt(yData, pY + x),
                    convertByteToInt(uData, pU + uv_offset),
                    convertByteToInt(vData, pV + uv_offset))
            }
        }
    }
    private fun convertByteToInt(arr: ByteArray, pos: Int): Int {
        return arr[pos].toInt() and 0xFF
    }
    var externaloutputDir : String? = null
    fun getDirectoryToSaveFile() : String? {
        if (externaloutputDir == null) {
            initExternalDir()
        }
        return externaloutputDir
    }

    fun initExternalDir() {
        for (fd in ctx!!.externalMediaDirs) {
            if (fd == null) {
                continue
            }
            val path = fd.absolutePath
            val m = path.indexOf("/Android/media/")
            if (m >= 0) {
                if (fd.isDirectory) {
                    externaloutputDir =  fd.absolutePath
                    return
                }
            }
        }
        if (externaloutputDir == null) {
            throw Exception("Didn't find directory to write file")
        }
    }


    private var yRowStride: Int = 0

    fun setBitmap(inp : IntArray, mp : Bitmap, width: Int, height: Int) {
        mp.setPixels(inp, 0, width, 0, 0, width, height)
    }

    fun transformationMatrix(srcWidth : Int, srcHeight: Int,
                             dstWidth: Int, dstHeight: Int) : Matrix {

        if (srcH == srcWidth && srcW == srcWidth && transformMatrix != null) {
            return transformMatrix!!
        }
        val matrix = Matrix()
        val scaleWidth = dstWidth.toFloat() / srcWidth.toFloat()
        val scaleHeight = dstHeight.toFloat() / srcHeight.toFloat()
        val scaleFactor = Math.max(scaleWidth.toFloat(), scaleHeight.toFloat())
        matrix.postScale(scaleFactor.toFloat(), scaleFactor.toFloat())
        srcW = srcWidth
        srcH = srcHeight
        transformMatrix = matrix
        return matrix
    }

    // Protect with lock
    var transformMatrix : Matrix? = null
    var srcW : Int = -1
    var srcH : Int = -1

    var intValues : IntArray? = null
    val NUM_DETECTIONS = 20
    var outputLocations = Array(1){Array(NUM_DETECTIONS){FloatArray(4)}}
    var outputClasses = Array(1){FloatArray(NUM_DETECTIONS)}
    var outputScores = Array(1){FloatArray(NUM_DETECTIONS)}
    var numDetections = FloatArray(1)

    var ballLockUsage = Semaphore(1)
    fun getBallLocs(img : Image) : List<RectF> {
        ballLockUsage.acquire()
        Log.i("TEJA", "Lock acquired")
        val planes = img.getPlanes()
        fillBytes(planes, yuvBytes)
        yRowStride = planes[0].getRowStride();
        val uvRowStride = planes[1].getRowStride();
        val uvPixelStride = planes[1].getPixelStride();
        if (rgbBytes == null || rgbBytes!!.size != img.height * img.width) {
            rgbBytes = IntArray(img.height * img.width)
        }

        convertYUV420ToARGB8888(yuvBytes[0]!!,
            yuvBytes[1]!!,
            yuvBytes[2]!!,
            img.width,
            img.height,
            yRowStride,
            uvRowStride,
            uvPixelStride,
            rgbBytes!!)
        if (rgbFrameBitmap == null) {
            rgbFrameBitmap = Bitmap.createBitmap(img.width, img.height, Bitmap.Config.ARGB_8888)
        }
        Log.i("TEJAG", img.width.toString() + ":" + img.height.toString())
        if (croppedBitmap == null) {
            croppedBitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)
        }
        setBitmap(rgbBytes!!, rgbFrameBitmap!!, img.width, img.height)
        var canvas = Canvas(croppedBitmap!!)
        canvas.drawBitmap(rgbFrameBitmap!!,
            transformationMatrix(img.width, img.height, 320, 320), null)

        croppedBitmap?.compress(
            Bitmap.CompressFormat.JPEG, 85,
            FileOutputStream(File(getDirectoryToSaveFile(), "croppedimg.jpg")))
        rgbFrameBitmap?.compress(
            Bitmap.CompressFormat.JPEG, 85,
            FileOutputStream(File(getDirectoryToSaveFile(), "original.jpg")))
        val results = imgClassifier!!.recognizeImage(croppedBitmap!!)
        var output = ArrayList<RectF>()
        for (result in results) {
            Log.i("TEJA", result.toString())
            if (result.id == "tball") {
                output.add(result.locn!!)
            }
        }
        ballLockUsage.release()
        return output
    }
}
