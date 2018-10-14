package com.tjtjrb.sproing.associations

sealed class ParamWrapper<T> {
    abstract fun isValid(): Boolean
    abstract fun unwrap(): T
}

data class ValidParam<T>(val param: T) : ParamWrapper<T>() {
    override fun isValid(): Boolean = param != null
    override fun unwrap(): T = param
}

class UninitializedParam<T> : ParamWrapper<T>() {
    override fun isValid(): Boolean = false
    override fun unwrap(): T = throw RuntimeException("Cannot unwrap")
}

class ListParam<T> : ParamWrapper<List<T>>() {
    override fun isValid(): Boolean = false
    override fun unwrap(): List<T> = emptyList()
}