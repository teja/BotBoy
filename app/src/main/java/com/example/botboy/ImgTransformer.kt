package com.example.botboy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.media.Image
import android.util.Log
import android.view.WindowManager
import java.io.File
import java.io.FileOutputStream


class ImageTransformer {
  private lateinit var rgbBytes: IntArray
  private lateinit var rgbFrameBitmap : Bitmap
  private var srcWidth: Int
  private var dstWidth: Int
  private var srcHeight: Int
  private var dstHeight: Int
  constructor(
      srcWidth: Int, srcHeight: Int,
      dstWidth: Int, dstHeight: Int) {
    this.srcWidth = srcWidth
    this.srcHeight =srcHeight 
    this.dstWidth = dstWidth 
    this.dstHeight = dstHeight
    rgbBytes = IntArray(srcWidth * srcHeight)
    rgbFrameBitmap = Bitmap.createBitmap(
       srcWidth, srcHeight, Bitmap.Config.ARGB_8888)
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

    private var yRowStride: Int = 0
    fun transformationMatrix(srcWidth : Int, srcHeight: Int,
                             dstWidth: Int, dstHeight: Int,
			     angleToRotate: Int) : Matrix {
        val matrix = Matrix()
	var srcW = srcWidth
	var srcH = srcHeight
	// Swap the dimensions if we need to rotate.
	if (angleToRotate == 90 || angleToRotate == 270) {
		srcW = srcHeight
		srcH = srcWidth
	}
	var widthRatio = dstWidth.toFloat() / srcW
	var heightRatio = dstHeight.toFloat() / srcH
	var scale : Float = Math.max(widthRatio, heightRatio)
	with (matrix) {
		postScale(scale, scale, dstWidth/2.0f, dstHeight/2.0f)
		postRotate((angleToRotate % 360).toFloat(),
	                   dstWidth/2.0f, dstHeight/2.0f)
	}
        return matrix
    }
    private val yuvBytes = arrayOfNulls<ByteArray>(3)
    protected fun fillBytes(planes: Array<Image.Plane>, yuvBytes: Array<ByteArray?>) {
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
    private fun convertToRgbBytes(img: Image) {
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
    }
    var externaloutputDir : String? = null
    fun getDirectoryToSaveFile(ctx: Context) : String? {
        if (externaloutputDir == null) {
            initExternalDir(ctx)
        }
        return externaloutputDir
    }
    fun initExternalDir(ctx: Context) {
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
    fun setBitmap(inp : IntArray, mp : Bitmap, width: Int, height: Int) {
        mp.setPixels(inp, 0, width, 0, 0, width, height)
    }
    public fun transformImage(img : Image, outbmp : Bitmap,  ctx: Context) {
        val rotation = (ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
        Log.i("TEJA", "RRR" + rotation)
        convertToRgbBytes(img)
        Log.i("TEJAG", img.width.toString() + ":" + img.height.toString())
        setBitmap(rgbBytes!!, rgbFrameBitmap, img.width, img.height)
        var canvas = Canvas(outbmp)
        canvas.drawBitmap(rgbFrameBitmap,
            transformationMatrix(img.width, img.height, 320, 320, rotation),
	    null)

        outbmp.compress(
            Bitmap.CompressFormat.JPEG, 85,
            FileOutputStream(File(getDirectoryToSaveFile(ctx), "croppedimg.jpg"))
        )
        rgbFrameBitmap.compress(
            Bitmap.CompressFormat.JPEG, 85,
            FileOutputStream(File(getDirectoryToSaveFile(ctx), "original.jpg")))
    }
}

