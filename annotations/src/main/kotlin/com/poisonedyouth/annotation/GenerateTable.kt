package com.poisonedyouth.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class GenerateTable(
    val lowerCase: Boolean = true,
    val tableName: String = ""
)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FIELD)
annotation class PrimaryKey
