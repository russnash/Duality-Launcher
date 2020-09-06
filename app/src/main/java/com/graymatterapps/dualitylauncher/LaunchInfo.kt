package com.graymatterapps.dualitylauncher

import kotlinx.serialization.Serializable

@Serializable
data class LaunchInfo(
    private var activityName: String = "",
    private var packageName: String = "",
    private var userSerial: Long = 0,
    private var type: Int = 0,
    private var folderName: String = "",
    private var folderUniqueId: Long = 0L
) {
    val TAG = javaClass.simpleName

    fun setActivityName(activity: String) {
        activityName = activity
    }

    fun getActivityName(): String {
        return activityName
    }

    fun setPackageName(packageNameInfo: String) {
        packageName = packageNameInfo
    }

    fun getPackageName(): String {
        return packageName
    }

    fun setUserSerial(serial: Long) {
        userSerial = serial
    }

    fun getUserSerial(): Long {
        return userSerial
    }

    fun setType(launchInfoType: Int) {
        type = launchInfoType
    }

    fun getType(): Int {
        return type
    }

    fun setFolderName(name: String) {
        folderName = name
    }

    fun getFolderName(): String {
        return folderName
    }

    fun setFolderUniqueId(id: Long) {
        folderUniqueId = id
    }

    fun getFolderUniqueId() : Long {
        return folderUniqueId
    }

    companion object {
        const val ICON = 0
        const val FOLDER = 1
    }
}