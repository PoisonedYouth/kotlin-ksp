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
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import com.squareup.kotlinpoet.ksp.toClassName
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import java.time.LocalDate
import java.util.regex.Pattern

private const val TABLE_NAME_POSTFIX = "Table"

class GenerateTableProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Filter the classes with the matching annotation
        val annotatedClasses = resolver.getSymbolsWithAnnotation(GenerateTable::class.java.name)
            .filterIsInstance<KSClassDeclaration>()

        // For all annotated classes do the generation process
        for (annotatedClass in annotatedClasses) {

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
                        getTablename(annotatedClass),
                        getIdProperty(annotatedClass)
                    )
                )

            // Create property specs
            val propertySpecs = annotatedClass.getAllProperties()
                .filter { !it.simpleName.asString().contains("id") }
                .map {
                    PropertySpec.builder(
                        name = it.simpleName.asString(),
                        type = getType(it.type.resolve()),
                        modifiers = emptyList()
                    )
                        .initializer(getInitValue(it.type.resolve(), it.simpleName.asString()))
                        .build()
                }
            typeBuilder.addProperties(propertySpecs.asIterable())


            // Create the file spec builder
            val fileSpec = FileSpec.builder(annotatedClass.packageName.asString(), className)
                .addType(typeBuilder.build())
                .build()
            codeGenerator.createNewFile(
                Dependencies(false, annotatedClass.containingFile!!),
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
        .filter { it.simpleName.asString().contains("id") }
        .map { it.simpleName.getShortName() }
        .first()

    @OptIn(KspExperimental::class)
    private fun getTablename(annotatedClass: KSClassDeclaration): String {
        val annotation = annotatedClass.getAnnotationsByType(GenerateTable::class)
            .first()
        val tableName = annotatedClass.simpleName.asString().let {
            if (annotation.lowerCase) {
                it.lowercase()
            } else {
                it
            }
        }
        val regex = Regex.fromLiteral("^[A-Za-z_]*\$")
        require(!regex.matches(tableName)) {
            "Table name '$tableName' does not comply with naming requriements."
        }
        return tableName
    }
}

private fun getInitValue(type: KSType, columnName: String): CodeBlock {
    val date = MemberName("org.jetbrains.exposed.sql.javatime", "date")
    val varchar = MemberName("", "varchar")
    val long = MemberName("", "long")

    return when (type.toClassName().simpleName) {
        "String" -> CodeBlock.of("%M(\"$columnName\", 255)", varchar)
        "Long" -> CodeBlock.of("%M(\"$columnName\")", long)
        "LocalDate" -> CodeBlock.of("%M(\"$columnName\")", date)
        else -> error("Invalid column type '${type.toClassName().simpleName}'.")
    }
}

private fun getType(type: KSType): TypeName {
    return when (type.toClassName().simpleName) {
        "String" -> Column::class.asTypeName().plusParameter(String::class.asTypeName())
        "Long" -> Column::class.asTypeName().plusParameter(Long::class.asTypeName())
        "LocalDate" -> Column::class.asTypeName().plusParameter(LocalDate::class.asTypeName())
        else -> error("Invalid column type '${type.toClassName().simpleName}'.")
    }
}
