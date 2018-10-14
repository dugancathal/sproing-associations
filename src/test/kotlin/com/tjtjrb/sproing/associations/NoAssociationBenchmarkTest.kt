package com.tjtjrb.sproing.associations

import com.carrotsearch.junitbenchmarks.BenchmarkOptions
import com.carrotsearch.junitbenchmarks.BenchmarkRule
import com.tjtjrb.sproing.associations.testbooks.Author
import com.tjtjrb.sproing.associations.testbooks.BookApp
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [BookApp::class])
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = ["classpath:/test-sql/createdb.sql"])
class NoAssociationBenchmarkTest {
    @get:Rule val benchmarkRule = BenchmarkRule()

    @Autowired
    lateinit var db: NamedParameterJdbcTemplate

    @WithPrefix(enabled = false)
    data class FlattenedRow(val id: Long, val name: String)

    @Before
    fun setUp() {
        db.update("INSERT INTO authors(name) VALUES (:name)", mapOf("name" to "Ursula Le Guin"))
        db.update("INSERT INTO authors(name) VALUES (:name)", mapOf("name" to "Terry Pratchett"))
        db.update("INSERT INTO authors(name) VALUES (:name)", mapOf("name" to "Terry Brooks"))
    }

    @Test
    @BenchmarkOptions(benchmarkRounds = 100)
    fun reflectiveTest() {
        db.query("SELECT * FROM authors", extract(FlattenedRow::class, into = Author::class))
    }

    @Test
    @BenchmarkOptions(benchmarkRounds = 100)
    fun hardCodedAlgorithm() {
        db.query("SELECT * FROM authors") { rs, _ ->
            Author(rs.getLong("id"), rs.getString("name"), emptyList())
        }
    }
}