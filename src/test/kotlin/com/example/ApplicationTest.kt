package com.example

import org.junit.jupiter.api.Test


class ApplicationTest {
    @Test
    fun basicTest() {
        assert(2==2)
    }

    @Test
    fun basicTest2() {
        assert(2==2)
    }

    @Test
    fun basicTest3() {
        assert(2==2)
    }
}


//    @Test
//    fun testRoot() = testApplication {
//        application {
//            configureRouting()
//        }
//        client.get("/").apply {
//            assertEquals(HttpStatusCode.OK, status)
//            assertEquals("Hello World!", bodyAsText())
//        }
//    }
