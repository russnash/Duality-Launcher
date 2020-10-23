package us.graymatterapps.dualitylauncher

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_wallpaper.*
import java.io.InputStream

const val PICK_IMAGE = 1

class WallpaperActivity : AppCompatActivity() {
    lateinit var imageStream: InputStream
    lateinit var type: String
    lateinit var wallpaperManager: WallpaperManager
    val TAG = javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallpaper)

        var secondaryDisplayOK = true
        type = intent.dataString.toString()

        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        if (type == "main") {
            val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val displays = displayManager.displays
            val dispCon = createDisplayContext(displays[0])
            wallpaperManager = WallpaperManager.getInstance(dispCon)
        } else {
            val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val displays = displayManager.displays
            if (displays.size > 1) {
                val dispCon = createDisplayContext(displays[1])
                wallpaperManager = WallpaperManager.getInstance(dispCon)
            } else {
                secondaryDisplayOK = false
                displayOff()
            }
        }

        if (type == "wide") {
            hingeGap.visibility = View.VISIBLE
            hingeValue.visibility = View.VISIBLE
            hingeGap.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    hingeValue.text = p1.toString()
                    val editor = settingsPreferences.edit()
                    editor.putInt("hingeGap", p1)
                    editor.apply()
                    setWideImage()
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                    // Do nothing
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                    // Do nothing
                }
            })
            hingeGap.progress = settingsPreferences.getInt("hingeGap", 60)
            hingeLeft.setOnClickListener {
                hingeGap.progress--
            }
            hingeRight.setOnClickListener {
                hingeGap.progress++
            }
        } else {
            hingeGap.visibility = View.INVISIBLE
            hingeValue.visibility = View.INVISIBLE
        }

        if (secondaryDisplayOK) {
            buttonOk.setOnClickListener(object : View.OnClickListener {
                override fun onClick(view: View?) {
                    setWallpaper()
                }
            })

            buttonCancel.setOnClickListener(object : View.OnClickListener {
                override fun onClick(view: View?) {
                    finish()
                }
            })

            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE)
        }
    }

    fun displayOff() {
        val builder = AlertDialog.Builder(this@WallpaperActivity)
        builder.setTitle("Secondary display not detected!")
        builder.setMessage("Please either insert your device into the dual screen case, or enable the dual screen.")
        builder.setPositiveButton("Ok") { dialog, which ->
            dialog.dismiss()
            finish()
        }
        builder.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            imageStream = this.contentResolver.openInputStream(data?.data!!)!!
            val drawable = BitmapDrawable(imageStream)
            var bitmap: Bitmap

            if (drawable.intrinsicHeight > wallpaperManager.desiredMinimumHeight) {
                val ratio =
                    wallpaperManager.desiredMinimumHeight / drawable.intrinsicHeight.toDouble()
                val height = wallpaperManager.desiredMinimumHeight
                val width = drawable.intrinsicWidth * ratio
                bitmap = Bitmap.createScaledBitmap(
                    drawableToBitmap(drawable),
                    width.toInt(), height, false
                )
                image.setImageDrawable(BitmapDrawable(bitmap))
            } else {
                image.setImageDrawable(drawable)
            }
            image.scaleX = 1f
            image.scaleY = 1f
        }

        if (type == "wide") {
            setWideImage()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun setWideImage() {
        image.layoutParams.height = wallpaperManager.desiredMinimumHeight / 2 + hingeGap.progress
        image.layoutParams.width = wallpaperManager.desiredMinimumWidth / 2 + hingeGap.progress
        image.requestLayout()
    }

    private fun setWallpaper() {
        image.background = null
        val result: Bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.RGB_565)
        val canvas = Canvas(result)
        image.draw(canvas)

        if (type == "main") {
            wallpaperManager.setBitmap(result)
        }

        if (type == "dual") {
            dualWallpaper.set(BitmapDrawable(result))
            val editor = settingsPreferences.edit()
            editor.putString("update_wallpaper", System.currentTimeMillis().toString())
            editor.putBoolean("dual_wallpaper_hack", true)
            editor.apply()
        }

        if (type == "wide") {
            val right: Bitmap =
                Bitmap.createBitmap(result, result.width / 2 + (hingeGap.progress / 2), 0, (result.width - hingeGap.progress) / 2, result.height)
            val left: Bitmap = Bitmap.createBitmap(result, 0, 0, (result.width - hingeGap.progress) / 2, result.height)

            wallpaperManager.setBitmap(right)

            dualWallpaper.set(BitmapDrawable(left))
            val editor = settingsPreferences.edit()
            editor.putString("update_wallpaper", System.currentTimeMillis().toString())
            editor.putBoolean("dual_wallpaper_hack", true)
            editor.apply()
        }

        finish()
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}