package com.example.projectmanager.util

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
    object Loading : Resource<Nothing>()

    companion object {
        fun <T> success(data: T): Resource<T> = Success(data)
        fun error(message: String): Resource<Nothing> = Error(message)
        fun loading(): Resource<Nothing> = Loading
    }
} 