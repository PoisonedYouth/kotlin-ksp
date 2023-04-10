package com.poisonedyouth

import com.poisonedyouth.annotation.GenerateTable
import java.time.LocalDate

@GenerateTable
data class User(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate
)