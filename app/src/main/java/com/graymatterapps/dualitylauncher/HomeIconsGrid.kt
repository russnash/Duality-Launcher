package com.graymatterapps.dualitylauncher

import kotlinx.serialization.Serializable

@Serializable
class HomeIconsGrid {
    var activities = arrayOf(
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", "")
    )
    var packages = arrayOf(
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", "")
    )
    var userSerials = arrayOf(
        longArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        longArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        longArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        longArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        longArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        longArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        longArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        longArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
    )
    val TAG = javaClass.simpleName

    fun changeLaunchInfo(row: Int, column: Int, launchInfo: LaunchInfo){
        activities[row][column] = launchInfo.getActivityName()
        packages[row][column] = launchInfo.getPackageName()
        userSerials[row][column] = launchInfo.getUserSerial()
    }

    fun getLaunchInfo(row: Int, column: Int): LaunchInfo {
        var launchInfo = LaunchInfo()
        launchInfo.setActivityName(activities[row][column])
        launchInfo.setPackageName(packages[row][column])
        launchInfo.setUserSerial(userSerials[row][column])
        return launchInfo
    }
}
