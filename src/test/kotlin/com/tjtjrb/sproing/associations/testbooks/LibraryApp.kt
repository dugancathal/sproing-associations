package com.tjtjrb.sproing.associations.testbooks

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class BookApp

fun main(args: Array<String>) {
    SpringApplication.run(BookApp::class.java, *args)
}
