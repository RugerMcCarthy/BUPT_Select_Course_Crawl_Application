package com.example.couseselector

interface TryFindAvailableServerListener {
    fun success(serverId: Int)
    fun fail()
}