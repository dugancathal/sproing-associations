package com.tjtjrb.sproing.associations

import kotlin.reflect.KClass

annotation class WithRootEntity(
    val value: KClass<*>,
    val nested: Array<WithRootEntity> = []
)
