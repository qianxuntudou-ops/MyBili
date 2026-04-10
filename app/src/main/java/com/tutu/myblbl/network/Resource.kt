package com.tutu.myblbl.network

sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    class Success<T>(data: T) : Resource<T>(data)
    
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    
    class Loading<T>(data: T? = null) : Resource<T>(data)
    
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading
    
    fun getOrThrow(): T {
        return when (this) {
            is Success -> data!!
            is Error -> throw IllegalStateException(message ?: "Unknown error")
            is Loading -> throw IllegalStateException("Still loading")
        }
    }
    
    fun getOrNull(): T? = data
    
    fun getMessageOrNull(): String? = message
    
    inline fun onSuccess(action: (T) -> Unit): Resource<T> {
        if (this is Success && data != null) {
            action(data)
        }
        return this
    }
    
    inline fun onError(action: (String) -> Unit): Resource<T> {
        if (this is Error) {
            action(message ?: "Unknown error")
        }
        return this
    }
    
    inline fun onLoading(action: () -> Unit): Resource<T> {
        if (this is Loading) {
            action()
        }
        return this
    }
    
    companion object {
        fun <T> success(data: T): Resource<T> = Success(data)
        
        fun <T> error(message: String, data: T? = null): Resource<T> = Error(message, data)
        
        fun <T> loading(data: T? = null): Resource<T> = Loading(data)
    }
}
