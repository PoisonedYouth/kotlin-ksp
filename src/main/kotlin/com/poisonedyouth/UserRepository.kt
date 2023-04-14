package com.poisonedyouth

import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

interface UserRepository {
    fun save(user: User): Long

    fun findById(userId: Long): User?
}

class UserRepositoryImpl : UserRepository {
    override fun save(user: User): Long = transaction {
        UserTable.insertAndGetId {
            it[firstName] = user.firstName
            it[lastName] = user.lastName
            it[birthDate] = user.birthDate
        }.value
    }

    override fun findById(userId: Long): User? = transaction {
        UserTable.select { UserTable.id eq userId }.firstOrNull()?.let { userResultRow ->
            User(
                id = userResultRow[UserTable.id].value,
                firstName = userResultRow[UserTable.firstName],
                lastName = userResultRow[UserTable.lastName],
                birthDate = userResultRow[UserTable.birthDate],
                address = AddressTable.select { AddressTable.id eq userResultRow[UserTable.address].value }.first().let {
                    Address(
                        id = it[AddressTable.id].value,
                        street = it[AddressTable.street],
                        streetNumber = it[AddressTable.streetNumber],
                        zipCode = it[AddressTable.zipCode],
                        city = it[AddressTable.city]
                    )
                }
            )
        }
    }

}