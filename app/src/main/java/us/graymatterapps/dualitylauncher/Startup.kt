package us.graymatterapps.dualitylauncher

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class Startup : AppCompatActivity(), AppManager.AppManagerInterface {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_startup)
        appManagerListener = this
        if(dualityLauncherApplication.isAppManagerInitialized()) {
            startMain()
        }
    }

    fun startMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if(dualityLauncherApplication.isAppManagerInitialized()) {
            startMain()
        }
    }

    override fun onStart() {
        super.onStart()
        if(dualityLauncherApplication.isAppManagerInitialized()) {
            startMain()
        }
    }

    override fun onStarted() {
        startMain()
    }
}