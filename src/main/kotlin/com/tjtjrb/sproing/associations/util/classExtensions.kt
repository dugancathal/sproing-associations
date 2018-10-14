package com.tjtjrb.sproing.associations.util

import kotlin.reflect.KClass
import kotlin.reflect.KParameter

fun KParameter.isList(): Boolean = this.type.classifier == List::class
fun KClass<*>.isList(): Boolean = this == List::class
fun KClass<*>.isPrimitive(): Boolean =
    this.javaPrimitiveType != null || this.javaObjectType == String::class.java