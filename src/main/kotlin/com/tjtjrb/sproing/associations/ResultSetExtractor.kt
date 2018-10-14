package com.tjtjrb.sproing.associations

import com.tjtjrb.sproing.associations.util.isList
import com.tjtjrb.sproing.associations.util.isPrimitive
import com.tjtjrb.sproing.associations.util.toSnake
import java.sql.ResultSet
import javax.persistence.Table
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

fun <T : Any> ResultSet.extract(klass: KClass<T>, columnPrefix: String = getEntityAliasPrefix(klass)): T? {
    val params = klass.primaryConstructor!!.parameters.map { param ->
        val paramClass = param.type.classifier!! as KClass<*>
        when {
            paramClass.isPrimitive() -> getField(prefixedColumn(columnPrefix, param), paramClass)
            paramClass.isList() -> ListParam<T>()
            else -> ValidParam(extract(paramClass, getEntityAliasPrefix(param)))
        }
    }

    if (params.any() && params.all { !it.isValid() })
        return null

    val args = params.map { it.unwrap() }.toTypedArray()
    return klass.primaryConstructor!!.call(*args)
}

private fun <T : Any> ResultSet.getField(columnName: String, param: KClass<T>): ParamWrapper<T> {
    val field: Any? = when (param) {
        String::class -> getString(columnName)
        Long::class -> getLong(columnName)
        Int::class -> getInt(columnName)
        Boolean::class -> getBoolean(columnName)
        else -> null
    }

    if (wasNull()) {
        return UninitializedParam()
    }

    @Suppress("UNCHECKED_CAST")
    return ValidParam(field as T)
}

private fun getEntityAliasPrefix(param: KAnnotatedElement): String {
    val prefix = param.findAnnotation<WithPrefix>()
    if (prefix != null) {
        return if (prefix.enabled) prefix.value else ""
    }

    return getTableName(param)
}

private fun getTableName(klass: KAnnotatedElement): String =
    klass.findAnnotation<Table>()?.name ?: klass.javaClass.simpleName.toString()

private fun prefixedColumn(columnPrefix: String, param: KParameter) =
    "${columnPrefix}${param.name!!.toSnake()}"
