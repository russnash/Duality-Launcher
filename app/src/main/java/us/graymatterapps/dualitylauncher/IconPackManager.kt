package us.graymatterapps.dualitylauncher

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.ScaleDrawable
import android.util.Log
import android.view.Gravity
import androidx.core.graphics.drawable.toBitmap
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class IconPackManager(val context: Context) {
    val iconPacks: ArrayList<IconPack> = ArrayList()
    val appFilter: ArrayList<AppFilter> = ArrayList()
    var iconBack: ArrayList<Drawable> = ArrayList()
    var iconMask: Drawable? = null
    var scale: Float = 0f
    var iconSize = 0
    var binary = false
    val packageManager = context.packageManager
    val BACK_ATTRS = arrayOf("img", "img1", "img2", "img3", "img4", "img5")
    val TAG = javaClass.simpleName

    fun updateIconPacks() {
        iconPacks.clear()
        val packages = packageManager.getInstalledPackages(0)
        packages.forEach {
            val iconPackResId = checkIfIconPack(it.packageName)
            if (iconPackResId != 0) {
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
            val pkgContext =
                appContext.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)
            val resources = pkgContext.resources
            var resId = resources.getIdentifier("appfilter", "xml", packageName)
            if (resId == 0) {
                resId = resources.getIdentifier("appfilter", "raw", packageName)
            }
            return resId
        } catch (e: Exception) {
            return 0
        }
    }

    fun initializeIconPack(iconPack: String) {
        val packageName = getPackageNameForIconPack(iconPack)
        val resId = getResIdFromIconPacks(packageName)
        appFilter.clear()
        scale = 0f
        iconMask = null
        iconBack.clear()
        iconSize = 0
        val pkgContext =
            appContext.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)
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
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_DOCUMENT -> {
                    Log.d(TAG, "START_DOCUMENT")
                }
                XmlPullParser.START_TAG -> {
                    Log.d(TAG, "START_TAG name:${xmlPullParser.name}")
                    if (xmlPullParser.name == "item") {
                        appFilter.add(
                            AppFilter(
                                xmlPullParser.getAttributeValue(null, "component"),
                                xmlPullParser.getAttributeValue(null, "drawable")
                            )
                        )
                    }
                    if (xmlPullParser.name == "iconback") {
                        val resources = packageManager.getResourcesForApplication(packageName)
                        BACK_ATTRS.forEach {
                            try {
                                val resId = resources.getIdentifier(
                                    xmlPullParser.getAttributeValue(
                                        null,
                                        it
                                    ), "drawable", packageName
                                )
                                if (resId != 0) {
                                    iconBack.add(resources.getDrawable(resId))
                                }
                            } catch (e: Exception) {
                                // Do nothing
                            }
                        }
                    }
                    if (xmlPullParser.name == "iconmask") {
                        try {
                            val resources = packageManager.getResourcesForApplication(packageName)
                            val resId = resources.getIdentifier(
                                xmlPullParser.getAttributeValue(
                                    null,
                                    "img1"
                                ), "drawable", packageName
                            )
                            iconMask = resources.getDrawable(resId)
                        } catch (e: Exception) {
                            iconMask = null
                        }
                        if (iconMask == null) {
                            try {
                                val resources =
                                    packageManager.getResourcesForApplication(packageName)
                                val resId = resources.getIdentifier(
                                    xmlPullParser.getAttributeValue(
                                        null,
                                        "img"
                                    ), "drawable", packageName
                                )
                                iconMask = resources.getDrawable(resId)
                            } catch (e: Exception) {
                                iconMask = null
                            }
                        }
                    }
                    if (xmlPullParser.name == "scale") {
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

        if (appFilter.size > 0) {
            val iconPackageName = getPackageNameForIconPack(iconPack)
            val resources = packageManager.getResourcesForApplication(iconPackageName)
            var resId = 0
            for (index in 0 until appFilter.size) {
                resId =
                    resources.getIdentifier(appFilter[index].drawable, "drawable", iconPackageName)
                if (resId != 0) {
                    break
                }
            }
            val icon = resources.getDrawable(resId)
            iconSize = icon.intrinsicWidth
        } else {
            iconSize = 108
        }
    }

    fun getResIdFromIconPacks(packageName: String): Int {
        iconPacks.forEach {
            if (it.packageName == packageName) {
                return it.resId
            }
        }
        return 0
    }

    fun getPackageNameForIconPack(iconPack: String): String {
        iconPacks.forEach {
            if (it.name == iconPack) {
                return it.packageName
            }
        }
        return ""
    }

    fun getIconPackDrawable(
        iconPack: String,
        packageName: String,
        activityName: String,
        standardIcon: Drawable
    ): Drawable? {
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

        if (iconBack.size > 0) {
            return makeAdaptedIcon(standardIcon)
        } else {
            return null
        }
    }

    fun makeAdaptedIcon(standardIcon: Drawable): Drawable {
        if (iconSize == 0) {
            iconSize = 192
        }

        var foreground = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)

        if(scale > 0) {
            var scaledForeground = Bitmap.createScaledBitmap(
                standardIcon.toBitmap(),
                (iconSize * scale).toInt(),
                (iconSize * scale).toInt(),
                true
            )
            val foregroundBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
            val foregroundCanvas = Canvas(foregroundBitmap)

            if (scale <= 1.0) {
                val offset = (iconSize - scaledForeground.width) / 2
                var srcRect = Rect(0, 0, scaledForeground.width, scaledForeground.height)
                var dstRect = Rect(offset, offset, iconSize - offset, iconSize - offset)
                foregroundCanvas.drawBitmap(scaledForeground, srcRect, dstRect, null)
            } else {
                val offset = (scaledForeground.width - iconSize) / 2
                var srcRect = Rect(
                    offset,
                    offset,
                    scaledForeground.width - offset,
                    scaledForeground.height - offset
                )
                var dstRect = Rect(0, 0, iconSize, iconSize)
                foregroundCanvas.drawBitmap(scaledForeground, srcRect, dstRect, null)
            }

            foreground = foregroundBitmap
        } else {
            foreground = standardIcon.toBitmap()
        }
        var background = iconBack[(0..iconBack.size - 1).random()].toBitmap()

        val bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        var srcRect = Rect(0, 0, background.width, background.height)
        var dstRect = Rect(0, 0, iconSize, iconSize)
        canvas.drawBitmap(background, srcRect, dstRect, null)

        srcRect = Rect(0, 0, foreground.width, foreground.height)
        dstRect = Rect(0, 0, iconSize, iconSize)
        canvas.drawBitmap(foreground, srcRect, dstRect, null)

        if (iconMask != null) {
            var mask = iconMask!!.toBitmap()
            val paint = Paint()
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            srcRect = Rect(0, 0, mask.width, mask.height)
            dstRect = Rect(0, 0, iconSize, iconSize)
            canvas.drawBitmap(mask, srcRect, dstRect, paint)
        }

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