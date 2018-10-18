package com.tjtjrb.sproing.associations

import com.tjtjrb.sproing.associations.util.isList
import org.springframework.jdbc.core.ResultSetExtractor
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

class ResultsWithAssociationsExtractor<Result : Any, RowClass : Any>(
    private val resultClass: KClass<Result>,
    private val rowClass: KClass<RowClass>,
    private val rootPrefix: String
) : ResultSetExtractor<List<Result>> {
    override fun extractData(rs: ResultSet): List<Result> {
        val preAssociationMappings = mutableListOf<RowClass>()

        while (rs.next()) {
            val flatRow = rs.extract(rowClass, rootPrefix)
            preAssociationMappings.add(flatRow!!)
        }

        val rootEntityAnnotation = rowClass.findAnnotation<WithRootEntity>()
        return if (rootEntityAnnotation == null) {
            val associations = emptyMap<KClass<*>, List<Any>>().withDefault { emptyList() }
            preAssociationMappings.map { constructClass(resultClass, associations, it) }
        } else {
            associateChildEntities(preAssociationMappings, rootEntityAnnotation)
        }
    }

    private fun <T : Any> associateChildEntities(flattenedRows: List<RowClass>, entitySpec: WithRootEntity): List<T> {
        val rootClass = entitySpec.value as KClass<T>
        val nestedAssociations = entitySpec.nested
        return flattenedRows.asSequence()
            .groupBy { row -> row.propOfType(rootClass) }
            .filter { it.key != null }
            .map { (root, associatedRows) -> toHydratedEntity(rootClass, root, nestedAssociations, associatedRows) }
    }

    private fun <T : Any> toHydratedEntity(rootClass: KClass<T>, root: T?, nestedAssociations: Array<WithRootEntity>, associatedRows: List<RowClass>): T {
        val associationLists = nestedAssociations.map { it.value to associateChildEntities<Any>(associatedRows, it) }
            .toMap().withDefault { emptyList<T>() }

        return constructClass(rootClass, associationLists, root)
    }

    private fun <T : Any> constructClass(clazz: KClass<T>, nested: Map<KClass<*>, List<Any?>>, primaryFields: Any?): T {
        val args = clazz.primaryConstructor!!.parameters.map {
            if (it.isList()) {
                nested.getValue(genericTypeOf(it))
            } else {
                primaryFields!!.valueAt(it.name!!, it.type.classifier as KClass<*>)
            }
        }.toTypedArray()

        return clazz.primaryConstructor!!.call(*args)
    }

    private fun genericTypeOf(param: KParameter) = param.type.arguments[0].type!!.classifier as KClass<*>
}

fun <Result : Any, RowClass : Any> extract(
    into: KClass<Result>,
    from: KClass<RowClass>,
    rootPrefix: String = ""
) = ResultsWithAssociationsExtractor(into, from, rootPrefix)

fun <Result : Any> extract(
    into: KClass<Result>,
    rootPrefix: String = ""
) = extract(into, into, rootPrefix)

private fun <T : Any> Any.propOfType(primaryEntityClass: KClass<T>): T? {
    val prop = this::class.memberProperties.find { it.returnType.classifier == primaryEntityClass }!!
    val propVal = prop.getter.call(this)
    return if (propVal != null) propVal as T else null
}

private fun <T : Any> Any.valueAt(descriptor: String, clazz: KClass<T>): T? {
    val prop = this::class.memberProperties.find { it.name == descriptor }!!
    val propVal = prop.getter.call(this)
    return if (propVal != null) propVal as T else null
}