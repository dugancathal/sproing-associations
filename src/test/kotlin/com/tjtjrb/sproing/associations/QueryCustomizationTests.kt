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
class QueryCustomizationTests {
    @Autowired
    lateinit var authorRepo: AuthorRepo
    @Autowired
    lateinit var db: NamedParameterJdbcTemplate

    @WithPrefix(enabled = false)
    data class JustAuthorId(val id: Long)

    @Test
    fun resultSetsDoNotRequireAllColumns() {
        val terry = authorRepo.create(Author(null, "Terry Pratchett"))

        val row = db.query("SELECT id FROM authors", extract(into = JustAuthorId::class))!!.first()
        assertThat(row.id).isEqualTo(terry.id)
    }
}