package us.graymatterapps.dualitylauncher

import android.app.ActivityManager
import android.app.Application
import android.app.WallpaperManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.*
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.text.format.DateFormat
import android.view.View
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import org.acra.ACRA
import org.acra.annotation.AcraCore
import org.acra.annotation.AcraDialog
import org.acra.annotation.AcraMailSender
import org.acra.data.StringFormat
import us.graymatterapps.dualitylauncher.appdb.AppList
import us.graymatterapps.dualitylauncher.appdb.IconPackManager
import us.graymatterapps.dualitylauncher.components.widgets.WidgetDB
import us.graymatterapps.graymatterutils.GrayMatterUtils
import java.util.*


lateinit var appWidgetManager: AppWidgetManager
lateinit var appWidgetHost: AppWidgetHost
lateinit var settingsPreferences: SharedPreferences
lateinit var prefs: SharedPreferences
lateinit var appContext: Context
lateinit var replicator: Replicator
lateinit var dragAndDropData: DragAndDropData
lateinit var dualityLauncherApplication: DualityLauncherApplication
//lateinit var mainContext: Context
lateinit var homePagerDual: ViewPager2
lateinit var homePagerMain: ViewPager2
var isScrolling = false
val STANDARD_ICON_SIZE = 100

@AcraCore(buildConfigClass = org.acra.BuildConfig::class, reportFormat = StringFormat.JSON)
@AcraMailSender(mailTo = "russnash37@gmail.com", reportAsFile = true)
@AcraDialog(resText = R.string.acra_dialog_text)
class DualityLauncherApplication: Application() {
    private lateinit var appList: AppList
    private lateinit var widgetDB: WidgetDB
    private lateinit var dualWallpaper: DualWallpaper
    private lateinit var mainScreen: View
    private lateinit var dualScreen: View
    private lateinit var iconPackManager: IconPackManager
    val TAG = javaClass.simpleName

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)

        ACRA.DEV_LOGGING = true
        ACRA.init(this)
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        dualityLauncherApplication = this
        prefs = this.getSharedPreferences(PREFS_FILENAME, 0)
        settingsPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        iconPackManager = IconPackManager(applicationContext)
        appList = AppList(applicationContext)
        appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        appWidgetHost = AppWidgetHost(applicationContext, 1)
        appWidgetHost.startListening()
        widgetDB = WidgetDB(this)
        replicator = Replicator()
        dragAndDropData = DragAndDropData()
        dualWallpaper = DualWallpaper(this)
    }

    fun getAppListContext(): AppList {
        return appList
    }

    fun getWidgetDBContext(): WidgetDB{
        return widgetDB
    }

    fun getDualWallpaperContext(): DualWallpaper{
        return dualWallpaper
    }

    fun getIconPackManagerContext(): IconPackManager{
        return iconPackManager
    }

    fun setMainScreenView(view: View) {
        mainScreen = view
    }

    fun setDualScreenView(view: View) {
        dualScreen = view
    }

    override fun onTerminate() {
        super.onTerminate()
        appWidgetHost.stopListening()
    }

    fun arePagersInitialized() : Boolean {
        return ::homePagerDual.isInitialized && ::homePagerMain.isInitialized
    }

    fun areScreensInitialized(): Boolean {
        return ::mainScreen.isInitialized && ::dualScreen.isInitialized
    }

    fun wideShot() {
        if(!areScreensInitialized()) {
            displayOff()
        } else {
            val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val displays = displayManager.displays

            if (displays.size == 1) {
                displayOff()
            } else {
                if (displays[1].displayId != 1) {
                    displayOff()
                } else {
                    val wallpaperManager = WallpaperManager.getInstance(appContext)
                    val mainWall: BitmapDrawable = wallpaperManager.drawable as BitmapDrawable
                    val wallBitmap = mainWall.bitmap
                    val bitmapMain = GrayMatterUtils.getScreenshotOfRoot(mainScreen)
                    val bitmapDual = GrayMatterUtils.getScreenshotOfRoot(dualScreen)

                    val bitmap = Bitmap.createBitmap(
                        bitmapMain.width * 2,
                        bitmapMain.height,
                        Bitmap.Config.RGB_565
                    )
                    var canvas = Canvas(bitmap)
                    canvas.drawBitmap(bitmapDual, 0F, 0F, null)
                    canvas.drawBitmap(wallBitmap, bitmapDual.width.toFloat(), 0F, null)
                    canvas.drawBitmap(bitmapMain, bitmapDual.width.toFloat(), 0F, null)

                    val now = Date()
                    DateFormat.format("yyyy-MM-dd_hh:mm:ss", now)

                    val contentResolver = applicationContext.contentResolver
                    val contentValues = ContentValues()
                    contentValues.put(
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        "DualityLauncher" + now + ".png"
                    )
                    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    contentValues.put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES
                    )
                    val imageUri: Uri = contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )!!
                    val outputStream =
                        contentResolver.openOutputStream(Objects.requireNonNull(imageUri))
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    Objects.requireNonNull(outputStream)!!.close()
                    GrayMatterUtils.shortToast(
                        applicationContext,
                        "Wide screenshot saved to gallery"
                    )
                }
            }
        }
    }

    fun displayOff() {
        GrayMatterUtils.longToast(this, "Dual screen not detected!")
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}