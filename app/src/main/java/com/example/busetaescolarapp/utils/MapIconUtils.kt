package com.example.busetaescolarapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

object MapIconUtils {
    /** Los marcadores de Google Maps solo aceptan bitmaps, así que convertimos el vector a uno. */
    fun vectorToBitmapDescriptor(context: Context, drawableResId: Int): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(context, drawableResId)
            ?: return BitmapDescriptorFactory.defaultMarker()

        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        drawable.setBounds(0, 0, width, height)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    /** Genera un marcador circular con un número adentro, para indicar el orden de las paradas. */
    fun numberedMarker(numero: Int, colorFondo: Int = Color.parseColor("#FFA000")): BitmapDescriptor {
        val size = 90
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val centro = size / 2f
        val radio = centro - 4f

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorFondo
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centro, centro, radio, circlePaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawCircle(centro, centro, radio, borderPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val textY = centro - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(numero.toString(), centro, textY, textPaint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    /** Marcador circular con un visto bueno, para indicar que ya se recorrió esa parada. */
    fun checkmarkMarker(colorFondo: Int = Color.parseColor("#43A047")): BitmapDescriptor {
        val size = 90
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val centro = size / 2f
        val radio = centro - 4f

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorFondo
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centro, centro, radio, circlePaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawCircle(centro, centro, radio, borderPaint)

        val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 8f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val path = android.graphics.Path().apply {
            moveTo(size * 0.26f, size * 0.52f)
            lineTo(size * 0.42f, size * 0.68f)
            lineTo(size * 0.76f, size * 0.32f)
        }
        canvas.drawPath(path, checkPaint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
