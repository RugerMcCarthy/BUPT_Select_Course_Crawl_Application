package com.example.couseselector

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.jsoup.Jsoup
import java.io.IOException


class MainActivity : AppCompatActivity() {
    companion object {
        var LOGIN_URL = "http://auth.bupt.edu.cn/authserver/login?service=http%3a%2f%2fyjxt.bupt.edu.cn%2fULogin.aspx"
    }
    private var getInitParamIsOk = false;
    private lateinit var cookie: String
    private lateinit var lt: String
    private lateinit var execution: String
    private lateinit var _eventId: String
    private lateinit var rmShown: String
    private lateinit var client: OkHttpClient
    private fun getInitParams() {
        client = OkHttpClient.Builder().followRedirects(false).build()
        var request = Request.Builder().url(LOGIN_URL).get().build()
        client.newCall(request).enqueue(object: Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "网络连接错误，请检查网络！",
                        Toast.LENGTH_SHORT
                        ).show()
                }
            }

            override fun onResponse(
                call: Call,
                response: Response
            ) {
                if (response.code() == 200) {
                    var html = response.body()?.string()
                    Log.d("gzz", response.headers().get("Set-Cookie"))
                    cookie =
                        response.headers().get("Set-Cookie")?.split(";")?.get(0)?.split("=")?.get(1)!!
                    var document = Jsoup.parse(html)
                    lt = document.select("#casLoginForm > input[type=hidden]:nth-child(5)").first().attr("value")
                    execution = document.select("#casLoginForm > input[type=hidden]:nth-child(6)").first().attr("value")
                    _eventId = document.select("#casLoginForm > input[type=hidden]:nth-child(7)").first().attr("value")
                    rmShown = document.select("#casLoginForm > input[type=hidden]:nth-child(11)").first().attr("value")
                    getInitParamIsOk = true
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "获取初始化请求参数成功",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "获取初始化请求参数失败，错误代码：" + response.code(),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    private fun checkInitParam():Boolean {
        return (this::lt.isInitialized && this::execution.isInitialized && this::_eventId.isInitialized && this::cookie.isInitialized && this::rmShown.isInitialized)
    }
    private fun checkUserInfo(username: String, password: String) {
        val formBodyBuilder = FormBody.Builder()
        formBodyBuilder.add("username", username)
        formBodyBuilder.add("password", password)
        formBodyBuilder.add("lt", lt)
        formBodyBuilder.add("execution", execution)
        formBodyBuilder.add("_eventId", _eventId)
        formBodyBuilder.add("rmShown", rmShown)

        var headersBuilder = Headers.Builder()
        headersBuilder.add("Host", "auth.bupt.edu.cn")
        headersBuilder.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:81.0) Gecko/20100101 Firefox/81.0")
        headersBuilder.add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
        headersBuilder.add("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2")
        headersBuilder.add("Content-Type", "application/x-www-form-urlencoded")
        headersBuilder.add("Origin", "https://auth.bupt.edu.cn")
        headersBuilder.add("Connection", "keep-alive")
        headersBuilder.add("Upgrade-Insecure-Requests", "1")
        headersBuilder.add("Cookie", "JSESSIONID=$cookie")
        var request = Request.Builder().url(LOGIN_URL)
            .headers(headersBuilder.build())
            .post(formBodyBuilder.build())
            .build()
        client.newCall(request).enqueue(object: okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "网络连接错误", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                var text = response.body()?.string()
                if (text?.indexOf("密码有误") == -1) {
                    runOnUiThread{
                        Toast.makeText(this@MainActivity, "登录成功", Toast.LENGTH_SHORT).show()
                    }
                    getLoginCookie(response.headers().get("Location")!!)
                } else {
                    runOnUiThread{
                        Toast.makeText(this@MainActivity, "您提供的用户名或者密码有误", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun getLoginCookie(loginUrl: String) {
        var request = Request.Builder().url(loginUrl).get().build()
        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "网络连接错误", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                var loginCookie = response.headers().get("Set-Cookie")?.split(";")?.get(0)
                Log.d("gzz", response.headers().get("Set-Cookie"))
                var intent = Intent("com.example.couseselector.SELECT")
                intent.putExtra("loginCookie", loginCookie)
                runOnUiThread {
                    startActivity(intent)
                }
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getInitParams()
        findViewById<Button>(R.id.login).setOnClickListener {
            var usernameText = findViewById<EditText>(R.id.username).text.toString()
            var passwordText = findViewById<EditText>(R.id.password).text.toString()
            if (getInitParamIsOk and checkInitParam()) {
                checkUserInfo(usernameText, passwordText)
            } else {
                Toast.makeText(this@MainActivity, "获取初始化请求参数失败, 请检查网络", Toast.LENGTH_SHORT)
            }
        }
    }
}
