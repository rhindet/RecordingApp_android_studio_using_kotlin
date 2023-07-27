package com.arrap.recordingapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max


//Clase para controlar los vectores de ondas audio
class WavefromView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var paint = Paint()
    private var  amplitudes = ArrayList<Float>()
    private var  spikes = ArrayList<RectF>()

    private var radius = 6f
    private var w = 9f
    private var d = 6f

    private var  sw = 0f
    private var sh = 400f

    private var maxSpikes = 0


    init{
        paint.color = Color.rgb(144, 50, 187)
        sw = resources.displayMetrics.widthPixels.toFloat()

        maxSpikes = (sw / (w + d)).toInt()

    }

    fun addAmplitude(amp:Float){

        val norm  = Math.min(amp.toInt() / 7 , 400 ).toFloat()

        amplitudes.add(norm)

        spikes.clear()
        var amps = amplitudes.takeLast(maxSpikes)
        for(i in amps.indices){

            var left = sw - i*(w +d)
            var top = sh / 2 - amps[i] / 2
            var right = left + w
            var bottom = top + amps[i]
            spikes.add(RectF(left,top,right,bottom))
        }

        invalidate()
    }

    fun clear() : ArrayList<Float>{
        var amps = amplitudes.clone() as ArrayList<Float>
        amplitudes.clear()
        spikes.clear()
        invalidate()

        return  amps
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
       spikes.forEach{
           canvas?.drawRoundRect(it,radius,radius,paint)
       }

    }

}