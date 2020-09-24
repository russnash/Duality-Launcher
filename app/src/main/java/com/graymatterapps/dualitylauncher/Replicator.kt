package com.graymatterapps.dualitylauncher

import android.util.Log

class Replicator {
    private val participants: ArrayList<Participants>
    val TAG = javaClass.simpleName

    init {
        participants = ArrayList<Participants>()
    }

    fun register(displayId: Int, listener: ReplicatorInterface) {
        deregister(displayId)
        participants.add(Participants(
            displayId,
            listener
        ))
        Log.d(TAG, "register() $displayId")
    }

    fun deregister(displayId: Int) {
        val indexes = ArrayList<Int>()

        for(i in 0 until participants.size) {
            if(participants[i].displayId == displayId) {
                indexes.add(i)
            }
        }

        indexes.forEach {
            participants.removeAt(it)
        }
        Log.d(TAG, "deregister() $displayId * ${indexes.size}")
    }

    fun addIcon(displayId: Int, launchInfo: LaunchInfo, page: Int, row: Int, column: Int) {
        participants.forEach {
            if(it.displayId != displayId) {
                Log.d(TAG, "addIcon() to ${it.displayId}")
                it.listener.addIcon(launchInfo, page, row, column)
            }
        }
    }

    fun changeIcon(displayId: Int, launchInfo: LaunchInfo, page: Int, row: Int, column: Int) {
        participants.forEach {
            if(it.displayId != displayId) {
                Log.d(TAG, "changeIcon() to ${it.displayId}")
                it.listener.changeIcon(launchInfo, page, row, column)
            }
        }
    }

    fun addFolder(displayId: Int, launchInfo: LaunchInfo, page: Int, row: Int, column: Int) {
        participants.forEach {
            if(it.displayId != displayId) {
                Log.d(TAG, "addFolder() to ${it.displayId}")
                it.listener.addFolder(launchInfo, page, row, column)
            }
        }
    }

    fun changeFolder(displayId: Int, launchInfo: LaunchInfo, page: Int, row: Int, column: Int) {
        participants.forEach {
            if(it.displayId != displayId) {
                Log.d(TAG, "changeFolder() to ${it.displayId}")
                it.listener.changeFolder(launchInfo, page, row, column)
            }
        }
    }

    fun deleteViews(displayId: Int, page: Int, row: Int, column: Int){
        participants.forEach {
            if(it.displayId != displayId) {
                Log.d(TAG, "deleteViews() to ${it.displayId}")
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
    }
}