package com.donut.mixfile

import com.alibaba.fastjson2.to
import com.alibaba.fastjson2.toJSONString
import com.donut.mixfile.server.core.aes.decryptAES
import com.donut.mixfile.server.core.utils.bean.MixShareInfo
import com.donut.mixfile.server.core.utils.bean.MixShareInfo.Companion.ENCODER
import com.donut.mixfile.server.core.utils.encodeHex
import com.donut.mixfile.server.core.utils.hashMD5
import com.donut.mixfile.server.core.utils.registerJson
import com.donut.mixfile.server.core.utils.resolveMixShareInfo
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

    }


}