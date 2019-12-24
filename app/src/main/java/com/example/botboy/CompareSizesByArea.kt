package com.example.botboy

import android.util.Size
import kotlin.math.sign

internal class CompareSizesByArea : Comparator<Size> {

    // We cast here to ensure the multiplications won't overflow
    override fun compare(lhs: Size, rhs: Size) : Int {
        var v = lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height
        if (v == 0.toLong()) {
            return 0.toInt()
        } else if (v > 0) {
            return 1.toInt()
        }
        return -1.toInt()

    }
}