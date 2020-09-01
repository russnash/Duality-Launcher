package com.graymatterapps.dualitylauncher

data class LaunchInfo(private var activityName: String = "", private var packageName: String = "", private var userSerial: Long = 0) {
    val TAG = javaClass.simpleName

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