package us.graymatterapps.dualitylauncher

import android.util.Log

class Replicator {
    private val participants: ArrayList<Participants>
    val TAG = javaClass.simpleName

    init {
        participants = ArrayList<Participants>()
    }

    fun register(displayId: Int, listener: ReplicatorInterface) {
        deregister(displayId)
        participants.add(
            Participants(
                displayId,
                listener
            )
        )
        Log.d(TAG, "register() display:$displayId")
    }

    fun deregister(displayId: Int) {
        val indexes = ArrayList<Int>()

        for (i in 0 until participants.size) {
            if (participants[i].displayId == displayId) {
                indexes.add(i)
            }
        }

        indexes.forEach {
            participants.removeAt(it)
        }
        Log.d(TAG, "deregister() display:$displayId * ${indexes.size}")
    }

    fun addIcon(displayId: Int, launchInfo: LaunchInfo, page: Int, row: Int, column: Int) {
        participants.forEach {
            if (it.displayId != displayId) {
                Log.d(TAG, "addIcon() to display:${it.displayId} page:$page")
                it.listener.addIcon(launchInfo, page, row, column)
            }
        }
    }

    fun changeIcon(displayId: Int, launchInfo: LaunchInfo, page: Int, row: Int, column: Int) {
        participants.forEach {
            if (it.displayId != displayId) {
                Log.d(TAG, "changeIcon() on display:${it.displayId} page:$page")
                it.listener.changeIcon(launchInfo, page, row, column)
            }
        }
    }

    fun addFolder(displayId: Int, launchInfo: LaunchInfo, page: Int, row: Int, column: Int) {
        participants.forEach {
            if (it.displayId != displayId) {
                Log.d(TAG, "addFolder() to display:${it.displayId} page:$page")
                it.listener.addFolder(launchInfo, page, row, column)
            }
        }
    }

    fun addFolderAll(launchInfo: LaunchInfo, page: Int, row: Int, column: Int) {
        participants.forEach {
            Log.d(TAG, "addFolder() to display:${it.displayId} page:$page")
            it.listener.addFolder(launchInfo, page, row, column)
        }
    }

    fun changeFolder(displayId: Int, launchInfo: LaunchInfo, page: Int, row: Int, column: Int) {
        participants.forEach {
            if (it.displayId != displayId) {
                Log.d(TAG, "changeFolder() on display:${it.displayId} page:$page")
                it.listener.changeFolder(launchInfo, page, row, column)
            }
        }
    }

    fun addDualLaunch(displayId: Int, launchInfo: LaunchInfo, page: Int, row: Int, column: Int) {
        participants.forEach {
            if (it.displayId != displayId) {
                Log.d(TAG, "addDualLaunch() to display:${it.displayId} page:$page")
                it.listener.addDualLaunch(launchInfo, page, row, column)
            }
        }
    }

    fun changeDualLaunch(displayId: Int, launchInfo: LaunchInfo, page: Int, row: Int, column: Int) {
        participants.forEach {
            if (it.displayId != displayId) {
                Log.d(TAG, "changeDualLaunch() on display:${it.displayId} page:$page")
                it.listener.changeDualLaunch(launchInfo, page, row, column)
            }
        }
    }

    fun deleteViews(displayId: Int, page: Int, row: Int, column: Int) {
        participants.forEach {
            if (it.displayId != displayId) {
                Log.d(TAG, "deleteViews() on display:${it.displayId} page:$page")
                it.listener.deleteViews(page, row, column)
            }
        }
    }

    private data class Participants(
        var displayId: Int,
        var listener: ReplicatorInterface
    )

    interface ReplicatorInterface {
        fun deleteViews(page: Int, row: Int, column: Int)
        fun addIcon(launchInfo: LaunchInfo, page: Int, row: Int, column: Int)
        fun changeIcon(launchInfo: LaunchInfo, page: Int, row: Int, column: Int)
        fun addFolder(launchInfo: LaunchInfo, page: Int, row: Int, column: Int)
        fun changeFolder(launchInfo: LaunchInfo, page: Int, row: Int, column: Int)
        fun addDualLaunch(launchInfo: LaunchInfo, page: Int, row: Int, column: Int)
        fun changeDualLaunch(launchInfo: LaunchInfo, page: Int, row: Int, column: Int)
    }
}