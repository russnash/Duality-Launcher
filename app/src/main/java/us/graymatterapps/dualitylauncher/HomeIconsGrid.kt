package us.graymatterapps.dualitylauncher

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
    var types = arrayOf(
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0)
    )
    var folderNames = arrayOf(
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", "")
    )
    var folderUniqueIds = arrayOf(
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

    fun changeLaunchInfo(row: Int, column: Int, launchInfo: LaunchInfo) {
        activities[row][column] = launchInfo.getActivityName()
        packages[row][column] = launchInfo.getPackageName()
        userSerials[row][column] = launchInfo.getUserSerial()
        types[row][column] = launchInfo.getType()
        folderNames[row][column] = launchInfo.getFolderName()
        folderUniqueIds[row][column] = launchInfo.getFolderUniqueId()
    }

    fun getLaunchInfo(row: Int, column: Int): LaunchInfo {
        var launchInfo = LaunchInfo()
        launchInfo.setActivityName(activities[row][column])
        launchInfo.setPackageName(packages[row][column])
        launchInfo.setUserSerial(userSerials[row][column])
        launchInfo.setType(types[row][column])
        launchInfo.setFolderName(folderNames[row][column])
        launchInfo.setFolderUniqueId(folderUniqueIds[row][column])
        return launchInfo
    }
}
