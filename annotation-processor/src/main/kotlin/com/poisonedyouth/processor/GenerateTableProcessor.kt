package com.poisonedyouth.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.poisonedyouth.annotation.GenerateTable
import com.poisonedyouth.annotation.PrimaryKey
import com.poisonedyouth.annotation.UniqueIndex
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import java.time.LocalDate

private const val TABLE_NAME_POSTFIX = "Table"

private val tableNameRegex = Regex.fromLiteral("^[A-Za-z_]*\$")

@OptIn(KspExperimental::class)
class GenerateTableProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Filter the classes with the matching annotation
        val annotatedClasses = resolver.getSymbolsWithAnnotation(GenerateTable::class.java.name)
            .filterIsInstance<KSClassDeclaration>()

        val customTypes = resolver.getSymbolsWithAnnotation(GenerateTable::class.java.name)
            .filterIsInstance<KSClassDeclaration>()
            .map { it.toClassName() }.toList()

        // For all annotated classes do the generation process
        for (annotatedClass in annotatedClasses) {
            // Check if required primary key annotation is set
            require(annotatedClass.getAllProperties().any { it.isAnnotationPresent(PrimaryKey::class) }) {
                "Required '@PrimaryKey' annotation is missing for specifying primary key property."
            }

            // Specify class name
            val className = annotatedClass.simpleName.asString() + TABLE_NAME_POSTFIX

            // Create an object builder
            val typeBuilder = TypeSpec.objectBuilder(className)
                .superclass(
                    LongIdTable::class.asClassName(),
                )
                .addSuperclassConstructorParameter(
                    CodeBlock.of(
                        format = "%1S,%2S",
                        getTableName(annotatedClass),
                        getIdProperty(annotatedClass)
                    )
                )

            // Create property specs
            val propertySpecs = annotatedClass.getAllProperties()
                .filter { it.getAnnotationsByType(PrimaryKey::class).firstOrNull() == null }
                .map {
                    PropertySpec.builder(
                        name = it.simpleName.asString(),
                        type = getType(
                            type = it.type.resolve(),
                            customTypes = customTypes
                        ),
                        modifiers = emptyList()
                    )
                        .initializer(
                            getInitValue(
                                annotatedClass = annotatedClass,
                                type = it.type.resolve(),
                                customTypes = customTypes,
                                columnName = it.simpleName.asString()
                            )
                        )
                        .build()
                }
            typeBuilder.addProperties(propertySpecs.asIterable())


            // Create the file spec builder
            val fileSpec = FileSpec.builder(annotatedClass.packageName.asString(), className)
                .addType(typeBuilder.build())
                .build()
            codeGenerator.createNewFile(
                Dependencies(false, annotatedClass.containingFile ?: error("No containing file available!")),
                fileSpec.packageName,
                fileSpec.name
            )
                .writer()
                .use { fileSpec.writeTo(it) }
        }
        // There are no additional processing rounds necessary
        return emptyList()
    }

    private fun getIdProperty(annotatedClass: KSClassDeclaration) = annotatedClass.getAllProperties()
        .filter { it.getAnnotationsByType(PrimaryKey::class).firstOrNull() != null }
        .map { it.simpleName.getShortName() }
        .single()

    private fun getTableName(annotatedClass: KSClassDeclaration): String {
        val annotation = annotatedClass.getAnnotationsByType(GenerateTable::class)
            .first()
        val tableName = annotation.tableName.ifEmpty {
            annotatedClass.simpleName.asString().let {
                if (annotation.lowerCase) {
                    it.lowercase()
                } else {
                    it
                }
            }
        }
        require(!tableNameRegex.matches(tableName)) {
            "Table name '$tableName' does not comply with naming requriements."
        }
        return tableName
    }

    private fun getInitValue(annotatedClass: KSClassDeclaration, type: KSType, customTypes: List<ClassName>, columnName: String): CodeBlock {
        val date = MemberName("org.jetbrains.exposed.sql.javatime", "date")
        val varchar = MemberName("", "varchar")
        val long = MemberName("", "long")
        val int = MemberName("", "integer")
        val reference = MemberName("", "reference")
        val uniqueIndex = MemberName("", "uniqueIndex")

        val annotation = annotatedClass.getAllProperties()
            .filter { it.simpleName.asString() == columnName }
            .mapNotNull { it.getAnnotationsByType(UniqueIndex::class).firstOrNull() }.firstOrNull()

        val className = customTypes.firstOrNull { it.simpleName == type.toString() }
        if (className != null) {
            if(annotation != null) {
                return CodeBlock.of("%1M(\"$columnName\", ${type}$TABLE_NAME_POSTFIX).%2M(%3S)", reference, uniqueIndex, annotation.uniqueKey)
            }else {
                return CodeBlock.of("%M(\"$columnName\", ${type}$TABLE_NAME_POSTFIX)", reference)
            }
        }

        return if (annotation != null) {
            when (type.toClassName()) {
                String::class.asClassName() -> CodeBlock.of("%1M(\"$columnName\", 255).%2M(%3S)", varchar, uniqueIndex, annotation.uniqueKey)
                Long::class.asClassName() -> CodeBlock.of("%M(\"$columnName\").%2M(%3S)", long, uniqueIndex, annotation.uniqueKey)
                Int::class.asClassName() -> CodeBlock.of("%M(\"$columnName\").%2M(%3S)", int, uniqueIndex, annotation.uniqueKey)
                LocalDate::class.asClassName() -> CodeBlock.of("%M(\"$columnName\").%2M(%3S)", date, uniqueIndex, annotation.uniqueKey)
                else -> error("Invalid column type '${type.toClassName().simpleName}'.")
            }
        } else {
            when (type.toClassName()) {
                String::class.asClassName() -> CodeBlock.of("%M(\"$columnName\", 255)", varchar)
                Long::class.asClassName() -> CodeBlock.of("%M(\"$columnName\")", long)
                Int::class.asClassName() -> CodeBlock.of("%M(\"$columnName\")", int)
                LocalDate::class.asClassName() -> CodeBlock.of("%M(\"$columnName\")", date)
                else -> error("Invalid column type '${type.toClassName().simpleName}'.")
            }
        }

    }

    private fun getType(type: KSType, customTypes: List<ClassName>): TypeName {
        val className = customTypes.firstOrNull { it.simpleName == type.toString() }

        if (className != null) {
            return Column::class.asTypeName().plusParameter(EntityID::class.asTypeName().plusParameter(Long::class.asTypeName()))
        }
        return when (type.toClassName()) {
            String::class.asClassName() -> Column::class.asTypeName().plusParameter(String::class.asTypeName())
            Long::class.asClassName() -> Column::class.asTypeName().plusParameter(Long::class.asTypeName())
            Int::class.asClassName() -> Column::class.asTypeName().plusParameter(Int::class.asTypeName())
            LocalDate::class.asClassName() -> Column::class.asTypeName().plusParameter(LocalDate::class.asTypeName())
            // Need to add additional types

            else -> error("Invalid column type '${type.toClassName().simpleName}' not part of $customTypes.")
        }
    }
}
