package com.tjtjrb.sproing.associations

import com.tjtjrb.sproing.associations.testbooks.Author
import com.tjtjrb.sproing.associations.testbooks.AuthorRepo
import com.tjtjrb.sproing.associations.testbooks.BookApp
import org.assertj.core.api.Assertions.assertThat
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
class CanMakeQueryTests {
    @Autowired
    lateinit var authorRepo: AuthorRepo

    @Autowired
    lateinit var db: NamedParameterJdbcTemplate

    @Test
    fun `can create a single item in the database`() {
        val created = authorRepo.create(Author(null, "Ursula Le Guin"))

        assertThat(created.id).isNotNull()
        assertThat(created.name).isEqualTo("Ursula Le Guin")
    }

    @Test
    fun `can return single items from the database`() {
        val created = authorRepo.create(Author(null, "Ursula Le Guin"))

        val found = authorRepo.findById(created.id!!)!!

        assertThat(found.id).isEqualTo(created.id)
        assertThat(found.name).isEqualTo("Ursula Le Guin")
    }

    @Test
    fun `can return all items from the database`() {
        authorRepo.create(Author(null, "Ursula Le Guin"))
        authorRepo.create(Author(null, "Terry Pratchett"))

        val found = authorRepo.findAll()

        assertThat(found.size).isEqualTo(2)
        assertThat(found.map { it.name }).containsExactlyInAnyOrder("Ursula Le Guin", "Terry Pratchett")
    }

    @Test
    fun `can return all of an entity and map an association`() {
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

        val hydratedUrsula = authorRepo.findByIdHydrated(ursula.id!!)

        assertThat(hydratedUrsula.books).hasSize(2)
        assertThat(hydratedUrsula.books.map { it.name }).containsExactlyInAnyOrder(
            "A Wizard of Earthsea",
            "The Tombs of Atuan"
        )
    }

    @Test
    fun `does not require all roots to have children`() {
        val nonAuthor = authorRepo.create(Author(null, "TJ Taylor"))
        val found = authorRepo.findByIdHydrated(nonAuthor.id!!)

        assertThat(found.books).isEmpty()
    }
}
