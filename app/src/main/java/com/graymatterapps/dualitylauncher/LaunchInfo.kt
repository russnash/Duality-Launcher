package com.graymatterapps.dualitylauncher

class LaunchInfo() {
    private var activityName: String
    private var packageName: String
    private var userSerial: Long

    init {
        activityName = ""
        packageName = ""
        userSerial = 0
    }

    fun setActivityName(activity: String) {
        activityName = activity
    }

    fun getActivityName() : String {
        return activityName
    }

    fun setPackageName(packageNameInfo: String) {
        packageName = packageNameInfo
    }

    fun getPackageName() : String {
        return packageName
    }

    fun setUserSerial(serial: Long) {
        userSerial = serial
    }

    fun getUserSerial() : Long {
        return userSerial
    }
}