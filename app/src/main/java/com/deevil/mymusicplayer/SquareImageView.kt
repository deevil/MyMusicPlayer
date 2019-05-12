package com.deevil.mymusicplayer

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory

class SquareImageView : ImageView {

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
//
//        val width = measuredWidth
//        setMeasuredDimension(width, width)
    }

    override fun setImageDrawable(drawable: Drawable) {
        val radius = 0.03f
        val bitmap = (drawable as BitmapDrawable).bitmap
        val rid = RoundedBitmapDrawableFactory.create(resources, bitmap)
        rid.cornerRadius = bitmap.width * radius
        super.setImageDrawable(rid)
    }
}
