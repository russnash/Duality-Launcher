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
    var dualLaunchNames = arrayOf(
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", ""),
        arrayOf("", "", "", "", "", "", "", "")
    )
    var dualLaunchUniqueIds = arrayOf(
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
        dualLaunchNames[row][column] = launchInfo.getDualLaunchName()
        dualLaunchUniqueIds[row][column] = launchInfo.getDualLaunchUniqueId()
    }

    fun getLaunchInfo(row: Int, column: Int): LaunchInfo {
        var launchInfo = LaunchInfo()
        launchInfo.setActivityName(activities[row][column])
        launchInfo.setPackageName(packages[row][column])
        launchInfo.setUserSerial(userSerials[row][column])
        launchInfo.setType(types[row][column])
        launchInfo.setFolderName(folderNames[row][column])
        launchInfo.setFolderUniqueId(folderUniqueIds[row][column])
        launchInfo.setDualLaunchName(dualLaunchNames[row][column])
        launchInfo.setDualLaunchUniqueId(dualLaunchUniqueIds[row][column])
        return launchInfo
    }
}
