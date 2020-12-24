package us.graymatterapps.dualitylauncher

import android.content.Context
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.Xml
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class IconPackManager(val context: Context) {
    val iconPacks: ArrayList<IconPack> = ArrayList()
    val appFilter: ArrayList<AppFilter> = ArrayList()
    var iconBack: Drawable? = null
    var iconMask: Drawable? = null
    var scale: Float = 0f
    var iconSize = 0
    var binary = false
    val packageManager = context.packageManager
    val TAG = javaClass.simpleName

    fun updateIconPacks() {
        iconPacks.clear()
        val packages = packageManager.getInstalledPackages(0)
        packages.forEach {
            val iconPackResId = checkIfIconPack(it.packageName)
            if(iconPackResId != 0) {
                iconPacks.add(
                    IconPack(
                        it.packageName,
                        it.applicationInfo.loadLabel(packageManager).toString(),
                        iconPackResId
                    )
                )
            }
        }
    }

    fun checkIfIconPack(packageName: String): Int {
        try {
            /*
            val apkResources = packageManager.getResourcesForApplication(packageName)
            val resId = apkResources.getIdentifier("appfilter", "xml", packageName)
             */
            val pkgContext = appContext.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)
            val resources = pkgContext.resources
            var resId = resources.getIdentifier("appfilter", "xml", packageName)
            if(resId == 0) {
                resId = resources.getIdentifier("appfilter", "raw", packageName)
            }
            return resId
        } catch (e: Exception) {
            //Log.d(TAG, "checkIfIconPack($packageName): ${e.printStackTrace()}")
            return 0
        }
    }

    fun initializeIconPack(iconPack: String) {
        val packageName = getPackageNameForIconPack(iconPack)
        val resId = getResIdFromIconPacks(packageName)
        appFilter.clear()
        scale = 0f
        iconMask = null
        iconBack = null
        iconSize = 0
        val pkgContext = appContext.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)
        val apkResources = pkgContext.resources
        var xmlPullParser: XmlPullParser
        try {
            xmlPullParser = apkResources.getXml(resId)
        } catch (e: Exception) {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val stream = apkResources.openRawResource(resId)
            xmlPullParser = factory.newPullParser()
            xmlPullParser.setInput(stream, null)
        }

        var eventType = xmlPullParser.eventType
        while(eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_DOCUMENT -> {
                    Log.d(TAG, "START_DOCUMENT")
                }
                XmlPullParser.START_TAG -> {
                    Log.d(TAG, "START_TAG name:${xmlPullParser.name}")
                    if(xmlPullParser.name == "item") {
                        appFilter.add(AppFilter(
                            xmlPullParser.getAttributeValue(null, "component"),
                            xmlPullParser.getAttributeValue(null, "drawable")
                        ))
                    }
                    if(xmlPullParser.name == "iconback") {
                        val resources = packageManager.getResourcesForApplication(packageName)
                        val resId = resources.getIdentifier(xmlPullParser.getAttributeValue(null, "img1"), "drawable", packageName)
                        iconBack = resources.getDrawable(resId)
                    }
                    if(xmlPullParser.name == "iconmask") {
                        val resources = packageManager.getResourcesForApplication(packageName)
                        val resId = resources.getIdentifier(xmlPullParser.getAttributeValue(null, "img1"), "drawable", packageName)
                        iconMask = resources.getDrawable(resId)
                    }
                    if(xmlPullParser.name == "scale") {
                        val scaleString = xmlPullParser.getAttributeValue(null, "factor")
                        scale = scaleString.toFloat()
                    }
                }
                XmlPullParser.TEXT -> {
                    Log.d(TAG, "TEXT name:${xmlPullParser.name}")
                }
                XmlPullParser.END_TAG -> {
                    Log.d(TAG, "END_TAG name:${xmlPullParser.name}")
                }
            }
            eventType = xmlPullParser.next()
        }

        if(appFilter.size > 0) {
            val iconPackageName = getPackageNameForIconPack(iconPack)
            val resources = packageManager.getResourcesForApplication(iconPackageName)
            var resId = 0
            for(index in 0 until appFilter.size) {
                resId = resources.getIdentifier(appFilter[index].drawable, "drawable", iconPackageName)
                if(resId != 0) {
                    break
                }
            }
            val icon = resources.getDrawable(resId)
            iconSize = icon.intrinsicWidth
        } else {
            iconSize = 108
        }
    }

    fun getResIdFromIconPacks(packageName: String) : Int {
        iconPacks.forEach{
            if(it.packageName == packageName) {
                return it.resId
            }
        }
        return 0
    }

    fun getPackageNameForIconPack(iconPack: String) : String {
        iconPacks.forEach {
            if(it.name == iconPack) {
                return it.packageName
            }
        }
        return ""
    }

    fun getIconPackDrawable(iconPack: String, packageName: String, activityName: String, standardIcon: Drawable): Drawable? {
        val search = "ComponentInfo{$packageName/$activityName}"
        try {
            appFilter.forEach {
                if (it.componentInfo == search) {
                    val iconPackageName = getPackageNameForIconPack(iconPack)
                    val resources = packageManager.getResourcesForApplication(iconPackageName)
                    val resId = resources.getIdentifier(it.drawable, "drawable", iconPackageName)
                    val icon = resources.getDrawable(resId)
                    return icon
                }
            }
        } catch (e: Exception) {
            // Do nothing and return modded icon
        }

        if(iconBack != null && iconMask != null) {
            return makeAdaptedIcon(standardIcon)
        } else {
            return null
        }
    }

    fun makeAdaptedIcon(standardIcon: Drawable): Drawable {
        if(iconSize == 0) {
            iconSize = 108
        }
        var foreground = standardIcon
        val bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        var srcRect = Rect(0, 0, iconBack!!.intrinsicWidth, iconBack!!.intrinsicHeight)
        var dstRect = Rect(0, 0, iconSize, iconSize)
        canvas.drawBitmap(iconBack!!.toBitmap(), srcRect, dstRect, null)

        srcRect = Rect(0, 0, foreground.intrinsicWidth, foreground.intrinsicHeight)
        dstRect = Rect(0, 0, iconSize, iconSize)
        canvas.drawBitmap(foreground.toBitmap(), srcRect, dstRect, null)

        val paint = Paint()
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        srcRect = Rect(0, 0, iconMask!!.intrinsicWidth, iconMask!!.intrinsicHeight)
        dstRect = Rect(0, 0, iconSize, iconSize)
        canvas.drawBitmap(iconMask!!.toBitmap(), srcRect, dstRect, paint)

        /*
        if(scale > 1f) {
            val percentageScale = 100 * scale
            val onePercent = iconSize / percentageScale
            val oneHundredPercent = 100 * onePercent
            val bitmapResult = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
            val canvasResult = Canvas(bitmapResult)
            val start = (iconSize - oneHundredPercent) / 2
            srcRect = Rect(start.toInt(), start.toInt(), iconSize-start.toInt(), iconSize-start.toInt())
            dstRect = Rect(0, 0, iconSize, iconSize)
            canvasResult.drawBitmap(bitmap, srcRect, dstRect, null)
            return BitmapDrawable(context.resources, bitmapResult)
        }

         */
        return BitmapDrawable(context.resources, bitmap)
    }

    data class IconPack(
        var packageName: String,
        var name: String,
        var resId: Int
    )

    data class AppFilter(
        var componentInfo: String,
        var drawable: String
    )
}