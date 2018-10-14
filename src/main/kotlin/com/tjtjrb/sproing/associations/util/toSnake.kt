package com.tjtjrb.sproing.associations.util

fun String.toSnake(): String {
    return replace(Regex("^[A-Z]")) { m -> m.groupValues[0].toLowerCase() }
        .replace(Regex("[A-Z]")) { m -> "_${m.groupValues[0].toLowerCase()}" }
}