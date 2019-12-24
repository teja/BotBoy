package com.example.botboy

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Size
import android.view.View

class OverlayDrawView : View {

    constructor(ctx: Context) : super(ctx) {
        init()
    }

    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs) {
        init()
    }

    fun init()
    {
        myPaint.setColor(Color.rgb(255, 0, 0));
        myPaint.setStrokeWidth(10.toFloat())
        myPaint.style = Paint.Style.STROKE
        imagePaint.setColor(Color.RED);
    }

    var myPaint = Paint();
    var imagePaint = Paint();
    var imgbitmap : Bitmap? = null
    private var boxes : ArrayList<Pair<Point, Size>> = ArrayList()
    private var ratioHeight = 0
    override fun onDraw(canvas: Canvas?) {
        if (imgbitmap != null) {
            canvas?.drawBitmap(imgbitmap!!, 0f, 0f, imagePaint)
        }
        for ( box in boxes) {
            canvas?.drawRect(box.first.x.toFloat(), box.first.y.toFloat(),
                box.second.width.toFloat(), box.second.height.toFloat(), myPaint)
        }
        canvas?.drawRect(0f, 0f, 100f, 100f, myPaint)
    }

    public fun AddRect(st : Point, sz : Size) {
        boxes.add(Pair(st, sz))
        invalidate()
    }

    public fun ResetImg(mp : Bitmap) {
        imgbitmap = mp
        invalidate()
    }
}