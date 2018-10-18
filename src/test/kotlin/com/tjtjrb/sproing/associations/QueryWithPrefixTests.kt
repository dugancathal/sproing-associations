package com.tjtjrb.sproing.associations

import com.tjtjrb.sproing.associations.testbooks.Author
import com.tjtjrb.sproing.associations.testbooks.AuthorRepo
import com.tjtjrb.sproing.associations.testbooks.Book
import com.tjtjrb.sproing.associations.testbooks.BookApp
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
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
class QueryWithPrefixTests {
    @Autowired
    lateinit var authorRepo: AuthorRepo
    @Autowired
    lateinit var db: NamedParameterJdbcTemplate

    @Before
    fun setUp() {
        val terry = authorRepo.create(Author(null, "Terry Pratchett"))
        db.update("INSERT INTO books(name, author_id) VALUES (:name, :authorId)", mapOf(
            "name" to "Discworld",
            "authorId" to terry.id
        ))

        val ursula = authorRepo.create(Author(null, "Ursula Le Guin"))
        db.update("INSERT INTO books(name, author_id) VALUES (:name, :authorId)", mapOf(
            "name" to "A Wizard of Earthsea",
            "authorId" to ursula.id
        ))
        db.update("INSERT INTO books(name, author_id) VALUES (:name, :authorId)", mapOf(
            "name" to "The Tombs of Atuan",
            "authorId" to ursula.id
        ))
    }

    @WithRootEntity(Author::class, nested = [WithRootEntity(Book::class)])
    data class AuthorWithBookAliasRow(
        @WithPrefix("a_") val author: Author,
        @WithPrefix("b_") val book: Book
    )

    @Test
    fun `can query with custom aliases`() {
        val query = """
            SELECT a.id as a_id, a.name as a_name, b.id as b_id, b.name as b_name, b.author_id as b_author_id
            FROM authors a
            INNER JOIN books b ON b.author_id = a.id
        """
        val rows = db.query(query, extract(into = Author::class, from = AuthorWithBookAliasRow::class))!!

        assertThat(rows.size).isEqualTo(2)
        assertThat(rows.map { it.name }).containsExactlyInAnyOrder("Ursula Le Guin", "Terry Pratchett")
        assertThat(rows.map { it.books.map { it.name } }).containsExactlyInAnyOrder(
            listOf("A Wizard of Earthsea", "The Tombs of Atuan"),
            listOf("Discworld")
        )
    }
}
