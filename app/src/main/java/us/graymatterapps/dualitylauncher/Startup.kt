package us.graymatterapps.dualitylauncher

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_startup.*

@SuppressLint("NewApi")
class Startup : AppCompatActivity(), AppManager.AppManagerInterface {
    lateinit var textView:TextView
    val TAG = "!Startup!"

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "Startup onCreate() display:${this.display!!.displayId}")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_startup)
        textView = findViewById(R.id.startupTextView)
        appManagerListener = this
        if(dualityLauncherApplication.isAppManagerInitialized()) {
            startMain()
        }
    }

    fun startMain() {
        Log.d(TAG, "Startup startMain() display:${this.display!!.displayId}")
        startupTextView.text = ""
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        Log.d(TAG, "Startup onResume() display:${this.display!!.displayId}")
        super.onResume()
        if(dualityLauncherApplication.isAppManagerInitialized()) {
            startMain()
        }
    }

    override fun onStart() {
        Log.d(TAG, "Startup onStart() display:${this.display!!.displayId}")
        super.onStart()
        if(dualityLauncherApplication.isAppManagerInitialized()) {
            startMain()
        }
    }

    override fun onStarted() {
        Log.d(TAG, "Startup onStarted() display:${this.display!!.displayId}")
        startMain()
    }
}