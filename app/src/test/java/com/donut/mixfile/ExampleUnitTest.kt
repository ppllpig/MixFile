package com.donut.mixfile

import io.ktor.http.URLBuilder
import org.junit.Test
import java.util.Date

//appScope.launch(Dispatchers.IO) {
//    repeat(100) {
//        favorites += List(1000) {
//            FileDataLog(
//                genRandomString(32),
//                "test-data",
//                1,
//                category = "test"
//            )
//        }
//    }
//}

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

class ExampleUnitTest {

    data class User(
        val age: Int,
        val date: Date = Date()
    ) {
        init {
            println("init")
        }
    }

    val map = mapOf(1 to "aa", 2 to "bb")


    @Test
    fun main() {


    }


}