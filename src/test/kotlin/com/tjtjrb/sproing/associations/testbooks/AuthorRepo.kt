package com.tjtjrb.sproing.associations.testbooks

import com.tjtjrb.sproing.associations.ResultsWithAssociationsExtractor
import com.tjtjrb.sproing.associations.WithPrefix
import com.tjtjrb.sproing.associations.WithRootEntity
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Service
import javax.persistence.Table
import javax.sql.DataSource

@Table(name = "notable_quotes")
data class NotableQuote(val id: Long?, val content: String, val bookId: Long?)

@Table(name = "books")
data class Book(val id: Long?, val name: String, val authorId: Long?, val notableQuotes: List<NotableQuote> = emptyList())

@Table(name = "authors")
data class Author(val id: Long?, val name: String, val books: List<Book> = emptyList())

const val FIND_BY_ID_QUERY = "SELECT id, name FROM authors WHERE id = :id"
const val FIND_ALL_QUERY = "SELECT id, name FROM authors"
const val FIND_ALL_HYDRATED_QUERY = """
    SELECT
        authors.id as id
        , authors.name as name
        , books.id as book_id
        , books.name as book_name
        , books.author_id as book_author_id
    FROM authors
    LEFT JOIN books ON books.author_id = authors.id
    WHERE authors.id = :id
"""

@Service
class AuthorRepo(private val dataSource: DataSource) {
    private val db = NamedParameterJdbcTemplate(dataSource)

    fun create(author: Author): Author {
        val insert = SimpleJdbcInsert(dataSource).withTableName("authors").usingGeneratedKeyColumns("id")
        val id = insert.executeAndReturnKey(mapOf(
            "name" to author.name
        ))
        return author.copy(id = id.toLong())
    }

    fun findById(id: Long): Author? {
        return db.queryForObject(FIND_BY_ID_QUERY, paramMap("id" to id)) { rs, _ ->
            Author(id = rs.getLong("id"), name = rs.getString("name"))
        }
    }

    fun findAll(): List<Author> {
        return db.query(FIND_ALL_QUERY, paramMap()) { rs, _ ->
            Author(id = rs.getLong("id"), name = rs.getString("name"))
        }
    }

    @WithRootEntity(Author::class, nested = [WithRootEntity(Book::class)])
    data class HydratedAuthorRow(
        @WithPrefix(enabled = false) val author: Author,
        @WithPrefix("book_") val book: Book?
    )

    fun findByIdHydrated(id: Long): Author {
        return db.query(FIND_ALL_HYDRATED_QUERY, paramMap("id" to id), ResultsWithAssociationsExtractor(Author::class, HydratedAuthorRow::class))!!.first()
    }
}

fun paramMap(vararg pairs: Pair<String, Any>) = MapSqlParameterSource(mapOf(*pairs))
