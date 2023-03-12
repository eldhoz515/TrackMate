package com.example.trackmate

import android.content.Context
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.widget.ImageView
import org.json.JSONObject
import java.io.File

class Utils {
    companion object {
        fun print(msg: Any) {
            Log.d("tm", msg.toString())
        }

        fun start(obj: View) {
            obj.visibility = View.VISIBLE
            val animatedDrawable = obj.background as AnimatedVectorDrawable
            animatedDrawable.registerAnimationCallback(object : Animatable2.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    super.onAnimationEnd(drawable)
                    animatedDrawable.start()
                }
            })
            animatedDrawable.start()
        }

        fun end(obj: View) {
            val animatedDrawable = obj.background as AnimatedVectorDrawable
            animatedDrawable.stop()
            obj.visibility = View.GONE
        }

        fun readFile(context: Context, fname: String): JSONObject? {
            val file = File(context.filesDir, fname)
            return if (file.exists()) {
                val jsonString = file.readText()
                JSONObject(jsonString)
            } else {
                null
            }
        }

        fun writeFile(context: Context, fname: String, data: JSONObject) {
            val jsonString = data.toString()
            val file = File(context.filesDir, fname)
            file.writeText(jsonString)
        }
    }
}