package com.poisonedyouth

import com.poisonedyouth.annotation.GenerateTable
import com.poisonedyouth.annotation.PrimaryKey
import com.poisonedyouth.annotation.UniqueIndex
import java.time.LocalDate

@GenerateTable(lowerCase = false)
data class User(
    @PrimaryKey
    val id: Long,
    @UniqueIndex(uniqueKey = "name")
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate,
    val address: Address
)

@GenerateTable
data class Address(
    @PrimaryKey
    val id: Long,
    val street: String,
    val streetNumber: String,
    val zipCode: Int,
    val city: String
)