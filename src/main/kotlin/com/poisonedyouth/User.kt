package com.poisonedyouth

import com.poisonedyouth.annotation.GenerateTable
import com.poisonedyouth.annotation.PrimaryKey
import java.time.LocalDate

@GenerateTable(lowerCase = false)
data class User(
    @PrimaryKey
    val id: Long,
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate
)