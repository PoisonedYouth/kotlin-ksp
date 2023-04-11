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
        UserTable.select { UserTable.id eq userId }.firstOrNull()?.let {
            User(
                id = it[UserTable.id].value,
                firstName = it[UserTable.firstName],
                lastName = it[UserTable.lastName],
                birthDate = it[UserTable.birthDate]
            )
        }
    }

}