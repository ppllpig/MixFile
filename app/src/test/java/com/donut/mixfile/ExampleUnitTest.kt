package com.donut.mixfile

import com.alibaba.fastjson2.annotation.JSONField
import com.alibaba.fastjson2.to
import com.alibaba.fastjson2.toJSONString
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
    class User(
        age: Int,
        val date: Date = Date()
    ) {
        @JSONField
        var age2: Int = age
            private set


        init {
            println("init")
        }
    }


    @Test
    fun main() {

    }


}