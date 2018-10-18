package com.tjtjrb.sproing.associations

import com.tjtjrb.sproing.associations.testbooks.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.junit4.SpringRunner


@RunWith(SpringRunner::class)
@SpringBootTest(classes = [BookApp::class])
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = ["classpath:/test-sql/createdb.sql"])
class NestedAssociationsTest {
    @Autowired
    lateinit var authorRepo: AuthorRepo
    @Autowired
    lateinit var db: NamedParameterJdbcTemplate

    lateinit var ursula: Author

    @Before
    fun setUp() {
        val terry = authorRepo.create(Author(null, "Terry Pratchett"))
        db.update("INSERT INTO books(name, author_id) VALUES (:name, :authorId)", mapOf(
            "name" to "Discworld",
            "authorId" to terry.id
        ))

        ursula = authorRepo.create(Author(null, "Ursula Le Guin"))
        db.update("INSERT INTO books(name, author_id) VALUES (:name, :authorId)", mapOf(
            "name" to "A Wizard of Earthsea",
            "authorId" to ursula.id
        ))

        val bookId = GeneratedKeyHolder().also {
            db.update("INSERT INTO books(name, author_id) VALUES (:name, :authorId)", paramMap(
                "name" to "The Tombs of Atuan",
                "authorId" to ursula.id!!
            ), it)
        }.key
        db.update("INSERT INTO notable_quotes(content, book_id) VALUES (:content, :bookId)", mapOf(
            "content" to "I don't even know",
            "bookId" to bookId
        ))
    }

    @WithRootEntity(
        Author::class,
        nested = [
            WithRootEntity(Book::class, nested = [
                WithRootEntity(NotableQuote::class)
            ])
        ]
    )
    data class AuthorWithBookAliasRow(
        @WithPrefix(enabled = false) val author: Author,
        @WithPrefix("b_") val book: Book?,
        @WithPrefix("q_") val quote: NotableQuote?
    )

    @Test
    fun `can return nested entities`() {
        val query = """
            SELECT a.id as a_id, a.name as a_name, b.id as b_id, b.name as b_name, b.author_id as b_author_id, q.id as q_id, q.content as q_content, q.book_id as q_book_id
            FROM authors a
            INNER JOIN books b ON b.author_id = a.id
            LEFT JOIN notable_quotes q ON q.book_id = b.id
            WHERE a.id = :authorId
        """
        val rows = db.query(query, paramMap("authorId" to ursula.id!!), extract(into=Author::class, from=AuthorWithBookAliasRow::class))!!

        assertThat(rows.size).isEqualTo(1)
        val ursula = rows.first()
        assertThat(ursula.name).isEqualTo("Ursula Le Guin")
        assertThat(ursula.books.map { it.name }).containsExactlyInAnyOrder("A Wizard of Earthsea", "The Tombs of Atuan")

        val tombs = ursula.books.find { it.name == "The Tombs of Atuan" }!!

        assertThat(tombs.notableQuotes).hasSize(1)
        assertThat(tombs.notableQuotes[0].content).isEqualTo("I don't even know")
    }
}