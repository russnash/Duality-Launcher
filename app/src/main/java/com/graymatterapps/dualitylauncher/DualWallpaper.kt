package com.graymatterapps.dualitylauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream


class DualWallpaper(con: Context){
    private lateinit var wallpaper: Drawable
    private var context: Context = con
    val TAG = javaClass.simpleName

    init{

        var file = context.filesDir
        file = File(file, "dualwallpaper.jpg")

        if (file.exists()) {
            depersist()
        } else {
            wallpaper = ColorDrawable(Color.BLACK)
        }
    }

    private fun depersist(){
        var file = context.filesDir
        file = File(file, "dualwallpaper.jpg")

        wallpaper = Drawable.createFromPath(file.absolutePath)!!
    }

    private fun persist(){
        val bitmap = (wallpaper as BitmapDrawable).bitmap
        var file = context.filesDir
        file = File(file, "dualwallpaper.jpg")

        try {
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException){
            e.printStackTrace()
        }
    }

    fun get(): Drawable{
        return wallpaper
    }

    fun set(drawable: Drawable){
        wallpaper = drawable
        persist()
    }
}