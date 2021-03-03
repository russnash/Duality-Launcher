package us.graymatterapps.dualitylauncher.appdb

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import us.graymatterapps.dualitylauncher.LaunchInfo
import us.graymatterapps.dualitylauncher.R
import java.io.ByteArrayOutputStream

class AppDB(var context: Context) : SQLiteOpenHelper(context, DATABASENAME, null, VERSION) {
    val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    var db = this.writableDatabase
    val TAG: String = javaClass.simpleName

    override fun onCreate(sqldb: SQLiteDatabase?) {
        sqldb?.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLENAME (" +
                    "name TEXT NOT NULL," +
                    "icon BLOB," +
                    "activityName TEXT NOT NULL," +
                    "packageName TEXT NOT NULL," +
                    "userSerial INTEGER NOT NULL," +
                    "manualWork INTEGER NOT NULL DEFAULT 0," +
                    "PRIMARY KEY (packageName, activityName, userSerial)" +
                    ");"
        )

    }

    fun openDB() {
        if(!db.isOpen) {
            db = this.writableDatabase
        }
    }

    fun closeDB() {
        db.close()
    }

    fun updateWorkApps(manualWorkApps: ArrayList<LaunchInfo>) {
        openDB()
        manualWorkApps.forEach {
            db.execSQL("update $TABLENAME set manualWork = 1 where activityName='${it.getActivityName()}' and packageName='${it.getPackageName()}'")
        }
    }

    fun setAsManualWorkApp(info: LaunchInfo) {
        openDB()
        db.execSQL("update $TABLENAME set manualWork = 1 where activityName='${info.getActivityName()}' and packageName='${info.getPackageName()}'")
    }

    fun desetAsManualWorkApp(info: LaunchInfo) {
        openDB()
        db.execSQL("update $TABLENAME set manualWork = 0 where activityName='${info.getActivityName()}' and packageName='${info.getPackageName()}'")
    }

    fun clearDB() {
        openDB()
        db.execSQL("delete from $TABLENAME")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // DO nothing
    }

    fun add(app: AppList.AppListDataType) {
        openDB()
        var cv = ContentValues()
        cv.put("name", app.name)
        cv.put("icon", bitmapToBytes(app.icon.toBitmap()))
        cv.put("activityName", app.activityName)
        cv.put("packageName", app.packageName)
        cv.put("userSerial", app.userSerial)
        db.insert(TABLENAME, null, cv)
    }

    fun update(app: AppList.AppListDataType) {
        openDB()
        var cv = ContentValues()
        cv.put("name", app.name)
        cv.put("icon", bitmapToBytes(app.icon.toBitmap()))
        cv.put("activityName", app.activityName)
        cv.put("packageName", app.packageName)
        cv.put("userSerial", app.userSerial)
        db.replace(TABLENAME, null, cv)
    }

    fun getRecordCount(): Long {
        openDB()
        return DatabaseUtils.queryNumEntries(db, TABLENAME)
    }

    fun getAllPackages(): Cursor {
        openDB()
        val cursor = db.rawQuery("select packageName, userSerial from $TABLENAME", null)
        return cursor
    }

    fun isAppInDB(packageName: String, activityName: String): Boolean {
        openDB()
        val count = DatabaseUtils.queryNumEntries(db, TABLENAME, "packageName='$packageName'and activityName='$activityName'")
        return count > 0L
    }

    fun getIcon(activityName: String, packageName: String, serial: Long): Drawable {
        openDB()
        val cursor = db.rawQuery("select icon from $TABLENAME where activityName='$activityName' and packageName='$packageName' and userSerial = $serial", null)
        if(cursor.count != 0) {
            cursor.moveToFirst()
            val bitmap = bytesToBitmap(cursor.getBlob(0))
            return BitmapDrawable(context.resources, bitmap)
        }
        cursor.close()
        return ContextCompat.getDrawable(context, R.drawable.ic_error)!!
    }

    fun getName(activityName: String, packageName: String, serial: Long): String {
        openDB()
        var name = "?"
        val cursor = db.rawQuery("select name from $TABLENAME where activityName='$activityName' and packageName='$packageName' and userSerial = $serial", null)
        if(cursor.count != 0) {
            cursor.moveToFirst()
            name = cursor.getString(0)
        }
        cursor.close()
        return name
    }

    fun removePackage(packageName: String, userSerial: Long) {
        openDB()
        db.execSQL("delete from $TABLENAME where packageName='$packageName' and userSerial=$userSerial")
    }

    fun get8Apps(): ArrayList<AppList.AppListDataType> {
        openDB()
        val cursor = db.rawQuery("select * from $TABLENAME order by name asc limit 8", null)
        val results: ArrayList<AppList.AppListDataType> = ArrayList()
        for(n in 0..7) {
            cursor.moveToPosition(n)
            val bitmap = bytesToBitmap(cursor.getBlob(1))
            val icon = BitmapDrawable(context.resources, bitmap)
            val handle = userManager.getUserForSerialNumber(cursor.getLong(4))

            results.add(
                AppList.AppListDataType(
                    cursor.getString(0),
                    icon,
                    cursor.getString(2),
                    cursor.getString(3),
                    handle,
                    cursor.getLong(4)
                )
            )
        }
        cursor.close()
        return results
    }

    private fun bitmapToBytes(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()

        if(bitmap.width > 256 || bitmap.height > 256) {
            Log.d(TAG, "bitmapToBytes() resizing icon")
            val resizedBitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
            val resizedCanvas = Canvas(resizedBitmap)
            val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
            val dstRect = Rect(0, 0, 255, 255)
            resizedCanvas.drawBitmap(bitmap, srcRect, dstRect, null)
            resizedBitmap.compress(Bitmap.CompressFormat.PNG, 0, stream)
        } else {
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream)
        }
        return stream.toByteArray()
    }

    private fun bytesToBitmap(bytes: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    companion object {
        const val DATABASENAME = "DL"
        const val TABLENAME = "appDB"
        const val VERSION = 1
    }
}