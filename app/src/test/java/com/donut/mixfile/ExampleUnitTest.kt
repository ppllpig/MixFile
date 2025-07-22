package com.donut.mixfile

import org.junit.Test
import org.mozilla.javascript.Context


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

class ExampleUnitTest {


    @Test
    fun main() {
        val context = Context.enter()
        try {
            val scope = context.initStandardObjects()
            val result = context.evaluateString(scope, "1 + 2", "script", 1, null)
            println("结果: $result") // 输出 3
        } finally {
            Context.exit()
        }
    }


}