package com.graymatterapps.graymatterutils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.DisplayMetrics
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.graymatterapps.dualitylauncher.MainActivity

object GrayMatterUtils {
    fun getVersionCode(con: Context): Int {
        try {
            val pm = con.packageManager
            val pkgInfo = pm.getPackageInfo(con.packageName, 0)
            return pkgInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return 0
    }

    fun getVersionName(con: Context): String {
        try {
            val pm = con.packageManager
            val pkgInfo = pm.getPackageInfo(con.packageName, 0)
            return pkgInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return "Unknown"
    }

    fun colorPrefToColor(color: String?): Int {
        when (color) {
            "Black" -> return Color.BLACK
            "White" -> return Color.WHITE
            "Green" -> return Color.GREEN
            "Blue" -> return Color.BLUE
            "Cyan" -> return Color.CYAN
            "Dark Gray" -> return Color.DKGRAY
            "Gray" -> return Color.GRAY
            "Light Gray" -> return Color.LTGRAY
            "Magenta" -> return Color.MAGENTA
            "Red" -> return Color.RED
            "Yellow" -> return Color.YELLOW
            else -> return Color.TRANSPARENT
        }
    }

    fun colorToColorPref(color: Int): String {
        when (color) {
            Color.BLACK -> return "Black"
            Color.WHITE -> return "White"
            Color.GREEN -> return "Green"
            Color.BLUE -> return "Blue"
            Color.CYAN -> return "Cyan"
            Color.DKGRAY -> return "Dark Gray"
            Color.GRAY -> return "Gray"
            Color.LTGRAY -> return "Light Gray"
            Color.MAGENTA -> return "Magenta"
            Color.RED -> return "Red"
            Color.YELLOW -> return "Yellow"
            else -> return "Custom"
        }
    }

    fun vibrate(con: Context, millis: Long) {
        val vibe = con.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibe.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun showOkCancelDialog(
        con: Context,
        message: String,
        okListener: DialogInterface.OnClickListener,
        cancelListener: DialogInterface.OnClickListener? = null
    ) {
        AlertDialog.Builder(con)
            .setMessage(message)
            .setPositiveButton("Ok", okListener)
            .setNegativeButton("Cancel", cancelListener)
            .create()
            .show()
    }

    fun showOkDialog(
        con: Context,
        message: String,
        okListener: DialogInterface.OnClickListener? = null
    ) {
        AlertDialog.Builder(con)
            .setMessage(message)
            .setPositiveButton("Ok", okListener)
            .create()
            .show()
    }

    fun longToast(con: Context, message: String) {
        Toast.makeText(con, message, Toast.LENGTH_LONG).show()
    }

    fun shortToast(con: Context, message: String) {
        Toast.makeText(con, message, Toast.LENGTH_SHORT).show()
    }

    fun hideKeyboardFrom(con: Context, view: View) {
        val imm = con.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun isLandscape(parent: MainActivity): Boolean {
        val displayManager = parent.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val displays = displayManager.displays
        val currentDisplay = parent.windowManager.getDefaultDisplay().displayId
        val display = displays.find { it.displayId == currentDisplay }
        var realMetrics = DisplayMetrics()
        display!!.getRealMetrics(realMetrics)
        return realMetrics.widthPixels > realMetrics.heightPixels
    }

    fun getDistance(x1: Float, y1: Float, x2: Float, y2: Float) : Double {
        return Math.sqrt(Math.pow(x2.toDouble() - x1.toDouble(), 2.0) + Math.pow(y2.toDouble() - y1.toDouble(),
            2.0
        ))
    }
}