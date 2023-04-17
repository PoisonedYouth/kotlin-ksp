package com.poisonedyouth.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.kspWithCompilation
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.io.File

class GenerateTableProcessorTest {

    @Test
    fun `should generate valid table object for valid input`() {
        // given
        val source = """
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
           """

        // when
        val compilation = KotlinCompilation().apply {
            inheritClassPath = true
            kspWithCompilation = true


            sources = listOf(
                SourceFile.kotlin("User.kt", source)
            )
            symbolProcessorProviders = listOf(
                GenerateTableProcessorProvider()
            )
        }
        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        // then
        val expectedResult = """
                import java.time.LocalDate
                import kotlin.String
                import org.jetbrains.exposed.dao.id.LongIdTable
                import org.jetbrains.exposed.sql.Column
                import org.jetbrains.exposed.sql.javatime.date
                
                public object UserTable : LongIdTable("User","id") {
                  public val firstName: Column<String> = varchar("firstName", 255)
                
                  public val lastName: Column<String> = varchar("lastName", 255)
                
                  public val birthDate: Column<LocalDate> = date("birthDate")
                }
           """

        val generated = File(
            compilation.kspSourcesDir,
            "kotlin/UserTable.kt"
        )
        assertEquals(
            expectedResult.trimIndent(),
            generated.readText().trimIndent()
        )
    }


    @Test
    fun `should generate valid table object with lowercase true`() {
        // given
        val source = """
                import com.poisonedyouth.annotation.GenerateTable
                import com.poisonedyouth.annotation.PrimaryKey
                import java.time.LocalDate
                
                @GenerateTable(lowerCase = true)
                data class User(
                    @PrimaryKey
                    val id: Long,
                    val firstName: String,
                    val lastName: String,
                    val birthDate: LocalDate
                )
           """

        // when
        val compilation = KotlinCompilation().apply {
            inheritClassPath = true
            kspWithCompilation = true


            sources = listOf(
                SourceFile.kotlin("User.kt", source)
            )
            symbolProcessorProviders = listOf(
                GenerateTableProcessorProvider()
            )
        }
        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        // then
        val expectedResult = """
                import java.time.LocalDate
                import kotlin.String
                import org.jetbrains.exposed.dao.id.LongIdTable
                import org.jetbrains.exposed.sql.Column
                import org.jetbrains.exposed.sql.javatime.date
                
                public object UserTable : LongIdTable("user","id") {
                  public val firstName: Column<String> = varchar("firstName", 255)
                
                  public val lastName: Column<String> = varchar("lastName", 255)
                
                  public val birthDate: Column<LocalDate> = date("birthDate")
                }
           """

        val generated = File(
            compilation.kspSourcesDir,
            "kotlin/UserTable.kt"
        )
        assertEquals(
            expectedResult.trimIndent(),
            generated.readText().trimIndent()
        )
    }

    @Test
    fun `should generate valid table object with unique index`() {
        // given
        val source = """
                import com.poisonedyouth.annotation.GenerateTable
                import com.poisonedyouth.annotation.PrimaryKey
                import com.poisonedyouth.annotation.UniqueIndex
                import java.time.LocalDate
                
                @GenerateTable(lowerCase = true)
                data class User(
                    @PrimaryKey
                    val id: Long,
                    @UniqueIndex(uniqueKey="name")
                    val firstName: String,
                    val lastName: String,
                    val birthDate: LocalDate
                )
           """

        // when
        val compilation = KotlinCompilation().apply {
            inheritClassPath = true
            kspWithCompilation = true


            sources = listOf(
                SourceFile.kotlin("User.kt", source)
            )
            symbolProcessorProviders = listOf(
                GenerateTableProcessorProvider()
            )
        }
        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        // then
        val expectedResult = """
                import java.time.LocalDate
                import kotlin.String
                import org.jetbrains.exposed.dao.id.LongIdTable
                import org.jetbrains.exposed.sql.Column
                import org.jetbrains.exposed.sql.javatime.date
                
                public object UserTable : LongIdTable("user","id") {
                  public val firstName: Column<String> = varchar("firstName", 255).uniqueIndex("name")
                
                  public val lastName: Column<String> = varchar("lastName", 255)
                
                  public val birthDate: Column<LocalDate> = date("birthDate")
                }
           """

        val generated = File(
            compilation.kspSourcesDir,
            "kotlin/UserTable.kt"
        )
        assertEquals(
            expectedResult.trimIndent(),
            generated.readText().trimIndent()
        )
    }

    @Test
    fun `should generate valid table object with tablename`() {
        // given
        val source = """
                import com.poisonedyouth.annotation.GenerateTable
                import com.poisonedyouth.annotation.PrimaryKey
                import java.time.LocalDate
                
                @GenerateTable(lowerCase = false, tableName = "MyUser")
                data class User(
                    @PrimaryKey
                    val id: Long,
                    val firstName: String,
                    val lastName: String,
                    val birthDate: LocalDate
                )
           """

        // when
        val compilation = KotlinCompilation().apply {
            inheritClassPath = true
            kspWithCompilation = true


            sources = listOf(
                SourceFile.kotlin("User.kt", source)
            )
            symbolProcessorProviders = listOf(
                GenerateTableProcessorProvider()
            )
        }
        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        // then
        val expectedResult = """
                import java.time.LocalDate
                import kotlin.String
                import org.jetbrains.exposed.dao.id.LongIdTable
                import org.jetbrains.exposed.sql.Column
                import org.jetbrains.exposed.sql.javatime.date
                
                public object UserTable : LongIdTable("MyUser","id") {
                  public val firstName: Column<String> = varchar("firstName", 255)
                
                  public val lastName: Column<String> = varchar("lastName", 255)
                
                  public val birthDate: Column<LocalDate> = date("birthDate")
                }
           """

        val generated = File(
            compilation.kspSourcesDir,
            "kotlin/UserTable.kt"
        )
        assertEquals(
            expectedResult.trimIndent(),
            generated.readText().trimIndent()
        )
    }

    @Test
    fun `should fail with missing @PrimaryKey annotation`() {
        // given
        val source = """
                import com.poisonedyouth.annotation.GenerateTable
                import com.poisonedyouth.annotation.PrimaryKey
                import java.time.LocalDate
                
                @GenerateTable(lowerCase = false, tableName = "MyUser")
                data class User(
                    val id: Long,
                    val firstName: String,
                    val lastName: String,
                    val birthDate: LocalDate
                )
           """

        // when
        val compilation = KotlinCompilation().apply {
            inheritClassPath = true
            kspWithCompilation = true


            sources = listOf(
                SourceFile.kotlin("User.kt", source)
            )
            symbolProcessorProviders = listOf(
                GenerateTableProcessorProvider()
            )
        }
        val result = compilation.compile()

        // then
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)

        val generated = File(
            compilation.kspSourcesDir,
            "kotlin/UserTable.kt"
        )
        assertFalse(generated.exists())
    }

    @Test
    fun `should fail with multiple @PrimaryKey annotation`() {
        // given
        val source = """
                import com.poisonedyouth.annotation.GenerateTable
                import com.poisonedyouth.annotation.PrimaryKey
                import java.time.LocalDate
                
                @GenerateTable(lowerCase = false, tableName = "MyUser")
                data class User(
                    @PrimaryKey
                    val id: Long,
                    @PrimaryKey
                    val userId: Long,
                    val firstName: String,
                    val lastName: String,
                    val birthDate: LocalDate
                )
           """

        // when
        val compilation = KotlinCompilation().apply {
            inheritClassPath = true
            kspWithCompilation = true


            sources = listOf(
                SourceFile.kotlin("User.kt", source)
            )
            symbolProcessorProviders = listOf(
                GenerateTableProcessorProvider()
            )
        }
        val result = compilation.compile()

        // then
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)

        val generated = File(
            compilation.kspSourcesDir,
            "kotlin/UserTable.kt"
        )
        assertFalse(generated.exists())
    }

}