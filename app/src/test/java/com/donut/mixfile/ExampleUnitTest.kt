package com.donut.mixfile

import com.alibaba.fastjson2.to
import com.alibaba.fastjson2.toJSONString
import com.donut.mixfile.server.core.utils.registerJson
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.Date


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

class ExampleUnitTest {

    data class User(
        val age: Int,
        val date: Date = Date()
    )

    @Test
    fun main() {
        println("程序继续执行")
        registerJson()
        runBlocking {
            println(User(1).toJSONString())
            println("{\"age\":1,\"date\":1742876318663}".to<User>())
        }
    }


}