package com.anwesh.uiprojects.jumpinghammerview

/**
 * Created by anweshmishra on 05/04/20.
 */

import android.view.View
import android.view.MotionEvent
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.content.Context
import android.app.Activity

val nodes : Int = 5
val bars : Int = 5
val sizeFactor : Float = 1.5f
val strokeFactor : Int = 90
val delay : Long = 20
val foreColor : Int = Color.parseColor("#673AB7")
val backColor : Int = Color.parseColor("#BDBDBD")
val barSizeFactor : Float = 4f
val scGap : Float = 0.02f

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.sinify() : Float = Math.sin(this * Math.PI).toFloat()

fun Canvas.drawJumpingHammer(i : Int, scale : Float, size : Float, w : Float, paint : Paint) {
    val sci : Float = scale.divideScale(i, bars)
    val sf : Float = sci.sinify()
    val barSize : Float = size / barSizeFactor
    val barW : Float = w / bars
    val y : Float = (size - barSize) * sf
    save()
    translate(size / 2, -y)
    drawRect(RectF(-barW / 2, -barSize, barW / 2, 0f), paint)
    drawLine(0f, 0f, 0f, y, paint)
    restore()
}

fun Canvas.drawJumpingHammers(scale : Float, size : Float, w : Float, paint : Paint) {
    for (j in 0..(bars - 1)) {
        drawJumpingHammer(j, scale, size, w, paint)
    }
}

fun Canvas.drawJHTNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    paint.color = foreColor
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.strokeCap = Paint.Cap.ROUND
    save()
    translate(0f, gap * (i + 1))
    drawJumpingHammers(scale, size, w, paint)
    restore()
}

class JumpingHammerView(ctx : Context) : View(ctx) {

    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scGap * dir
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * dir
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class JHTNode(var i : Int, val state : State = State()) {

        private var next : JHTNode? = null
        private var prev : JHTNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = JHTNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawJHTNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Float) -> Unit) {
            state.update(cb)
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : JHTNode {
            var curr : JHTNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class JumpingHammer(var i : Int) {

        private var root : JHTNode = JHTNode(0)
        private var curr : JHTNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Float) -> Unit) {
            curr.update {
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : JumpingHammerView) {

        private val jh : JumpingHammer = JumpingHammer(0)
        private var animator : Animator = Animator(view)
        private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun render(canvas : Canvas) {
            canvas.drawColor(backColor)
            jh.draw(canvas, paint)
            animator.animate {
                jh.update {
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            jh.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : JumpingHammerView {
            val view : JumpingHammerView = JumpingHammerView(activity)
            activity.setContentView(view)
            return view
        }
    }
}

