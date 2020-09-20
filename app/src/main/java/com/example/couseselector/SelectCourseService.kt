package com.example.couseselector

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import okhttp3.*
import org.jsoup.Jsoup
import java.io.IOException


class SelectCourseService: Service() {

    companion object {
        var COURSE_URL = "http://10.3.255.3%d/Lesson/PlanCourseOnlineSel.aspx"
        var COURSE_SEL_URL = "http://10.3.255.3%d/Gstudent/Course/PlanSelClass.aspx"

        var CHANNEL_ONE_ID = "crawl_server"
        var CHANNEL_ONE_NAME = "选课爬虫服务"
    }

    private lateinit var courseUrlClient: OkHttpClient
    private lateinit var courseUrlSelClient: OkHttpClient

    private lateinit var expectCourseNameList: ArrayList<String>
    private var coursePosMap = HashMap<String, Int>()
    private lateinit var loginCookie: String

    fun sendToast(toastMsg: String) {
        var intent = Intent("com.example.couseselector.toast")
        intent.putExtra("msg", toastMsg)
        sendBroadcast(intent)
    }

    fun sendRecyclerChange(recyclerMsg: String, pos: Int, remain: Int) {
        var intent = Intent("com.example.couseselector.recycler")
        intent.putExtra("msg", recyclerMsg)
        intent.putExtra("pos", pos)
        intent.putExtra("remain", remain)
        sendBroadcast(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        courseUrlClient = OkHttpClient.Builder().followRedirects(false).build()
        courseUrlSelClient = OkHttpClient.Builder().followRedirects(false).build()
        expectCourseNameList = intent?.getStringArrayListExtra("expectCourseNameList")!!
        for (i in expectCourseNameList.indices) {
            coursePosMap[expectCourseNameList[i]] = i
        }

        loginCookie = intent?.getStringExtra("loginCookie")

        Thread {
            findAvailableServerId(0, object: TryFindAvailableServerListener {
                override fun success(serverId: Int) {
                    sendToast("成功连接${serverId}号服务器")
                    startSelectCourse(serverId)
                }
                override fun fail() {
                    sendToast("四台服务器均连接失败")
                }
            })
        }.start()
        toForegroundService()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun findAvailableServerId(serverId: Int, listener: TryFindAvailableServerListener) {
        if (serverId == 4) {
            listener.fail()
            return
        }
        var headersBuilder = Headers.Builder()
        headersBuilder.add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
        headersBuilder.add("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2")
        headersBuilder.add( "User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko")
        headersBuilder.add("Connection", "keep-alive")
        headersBuilder.add("Pragma", "no-cache")
        headersBuilder.add("Cache-Control", "max-age=0")
        headersBuilder.add("Upgrade-Insecure-Requests", "1")
        headersBuilder.add("Cookie", loginCookie)
        var request = Request.Builder()
            .headers(headersBuilder.build())
            .url(String.format(COURSE_URL, serverId))
            .get()
            .build()
        courseUrlClient.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("gzz", "网络连接错误")
            }

            override fun onResponse(call: Call, response: Response) {
                var resp = response.body()?.string()
                if (resp?.indexOf("ReLogin.aspx") != -1) {
                    findAvailableServerId(serverId + 1, listener)
                    return
                }
                listener.success(serverId)
            }
        })
    }

    private fun startSelectCourse(serverId: Int) {
        while (true) {
            var headersBuilder = Headers.Builder()
            headersBuilder.add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            headersBuilder.add("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2")
            headersBuilder.add( "User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko")
            headersBuilder.add("Connection", "keep-alive")
            headersBuilder.add("Pragma", "no-cache")
            headersBuilder.add("Cache-Control", "max-age=0")
            headersBuilder.add("Upgrade-Insecure-Requests", "1")
            headersBuilder.add("Cookie", loginCookie)
            var request = Request.Builder()
                .headers(headersBuilder.build())
                .url(String.format(COURSE_URL, serverId))
                .get()
                .build()
            courseUrlClient.newCall(request).enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d("gzz", "网络连接错误")
                }
                override fun onResponse(call: Call, response: Response) {
                    var resp = response.body()?.string()
                    if (resp?.indexOf("ReLogin.aspx") != -1) {
                        sendToast("请切换服务器")
                        return
                    }
                    toSelectCourse(resp, serverId)
                }
            })
        }
    }


    private fun formalSelectCourse(html: String, url: String, courseName: String) {
        var courseHeaderBuilder = Headers.Builder()
            .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:81.0) Gecko/20100101 Firefox/81.0")
            .add("Accept", "*/*")
            .add("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2")
            .add("X-Requested-With", "XMLHttpRequest")
            .add("X-MicrosoftAjax", "Delta=true")
            .add("Cache-Control", "no-cache")
            .add("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
            .add("Cookie", loginCookie)

        var domParser = Jsoup.parse(html)
        var trs = domParser.getElementsByTag("tr")
        for (tr in trs) {
            var tds = tr.getElementsByTag("td")
            if (tds.size == 6 || tds.size == 7) {
                var str_partctl00 = tds[tds.size - 1].getElementsByTag("input").attr("name")
                var ctl00 = str_partctl00.substring(0, str_partctl00.indexOf("\$dgData")) + "\$UpdatePanel2|ctl00\$contentParent" + str_partctl00.substring(str_partctl00.indexOf("\$dgData"))
                var __VIEWSTATE = domParser.select("#__VIEWSTATE").attr("value")
                var __VIEWSTATEGENERATOR = domParser.select("#__VIEWSTATEGENERATOR").attr("value")
                var __EVENTVALIDATION = domParser.select("#__EVENTVALIDATION").attr("value")

                var formBodyBuilder = FormBody.Builder()
                    .add("ctl00\$ScriptManager1", ctl00)
                    .add("__EVENTTARGET", "")
                    .add("__EVENTARGUMENT", "")
                    .add("__LASTFOCUS", "")
                    .add("__VIEWSTATE", __VIEWSTATE)
                    .add("__VIEWSTATEGENERATOR", __VIEWSTATEGENERATOR)
                    .add("__VIEWSTATEENCRYPTED", "")
                    .add("__EVENTVALIDATION", __EVENTVALIDATION)
                    .add("ctl00\$contentParent\$drpXqu\$drpXqu", "")
                    .add("__ASYNCPOST", "true")
                    .add(str_partctl00+".x", "7")
                    .add(str_partctl00+".y", "8")
                courseHeaderBuilder.add("Referer", url)
                var request = Request.Builder().url(url).headers(courseHeaderBuilder.build()).post(formBodyBuilder.build()).build()
                courseUrlSelClient.newCall(request).enqueue(object: Callback {
                    override fun onFailure(call: Call, e: IOException) {

                    }

                    override fun onResponse(call: Call, response: Response) {
                        var html = response.body()?.string()
                        if (html?.indexOf("上课时间冲突") != -1) {
                            sendRecyclerChange("上课时间冲突,正在重试..", coursePosMap[courseName]!!, expectCourseNameList.size)
                        } else {
                            expectCourseNameList.remove(courseName)
                            sendRecyclerChange("选课成功", coursePosMap[courseName]!!, expectCourseNameList.size)
                        }
                    }

                })
            }
        }
    }

    private fun toSelectCourse(html: String, serverId: Int) {
        var courseListHeaderBuilder = Headers.Builder()
            .add("Accept-Language", "zh-CN")
            .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko")
            .add("Connection", "Keep-Alive")
            .add("Pragma", "no-cache")
            .add("Host", "yjxt.bupt.edu.cn")
            .add("Cookie", loginCookie)

        var domParser = Jsoup.parse(html)
        var trs = domParser.getElementsByTag("tr")
        for (tr in trs) {
            var tds = tr.getElementsByTag("td")
            if (tds.size == 11 && tds[7].text().replace(" ", "").replace("\n", "") == "选择上课班级") {
                var curCourse = tds[1].text().replace(" ","").replace("\n", "").replace("\r", "")
                if (expectCourseNameList.contains(curCourse)) {
                    var payload = tds[7].getElementsByTag("a").attr("onclick")
                    payload = payload.substring(10, payload.length - 14)
                    var url = String.format(COURSE_SEL_URL, serverId) + payload
                    var request = Request.Builder()
                        .headers(courseListHeaderBuilder.build())
                        .url(url)
                        .get()
                        .build()
                    courseUrlSelClient = OkHttpClient()
                    courseUrlSelClient.newCall(request).enqueue(object: Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            Log.d("gzz", "网络连接错误")
                        }

                        override fun onResponse(call: Call, response: Response) {
                            var str_course = response.body()?.string()
                            formalSelectCourse(str_course!!, String.format(COURSE_SEL_URL, serverId) + payload, curCourse)
                        }
                    })
                }
            } else if (tds.size == 11){
                for (course in expectCourseNameList) {
                    if (course == tds[7].text().replace(" ", "").replace("\n", "")) {
                        expectCourseNameList.remove(course)
                        sendRecyclerChange("课程已选", coursePosMap[course]!!, expectCourseNameList.size)
                    }
                }
            }
        }
    }

    fun toForegroundService() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            var notificationChannel = NotificationChannel(
                CHANNEL_ONE_ID,
                CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.enableLights(true)
            notificationChannel.setLightColor(Color.RED)
            notificationChannel.setShowBadge(true)
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC)
            val manager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(notificationChannel)
            var intent = Intent()
            intent.setAction("com.example.couseselector.SELECT")
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
            val notification: Notification =
                Notification.Builder(this).setChannelId(CHANNEL_ONE_ID)
                    .setTicker("Nature")
                    .setSmallIcon(R.drawable.favicon)
                    .setContentTitle("自动选课进行中")
                    .setContentIntent(pendingIntent)
                    .setContentText("正在选课中..")
                    .build()

            notification.flags = notification.flags or Notification.FLAG_NO_CLEAR
            startForeground(1, notification);
        } else {
            val notification: Notification =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    Notification.Builder(this)
                        .setTicker("Nature")
                        .setSmallIcon(R.drawable.favicon)
                        .setContentTitle("选课爬虫")
                        .setContentText("正在选课中..").build()
                } else {
                    TODO("VERSION.SDK_INT < JELLY_BEAN")
                }

            notification.flags = notification.flags or Notification.FLAG_NO_CLEAR
            startForeground(1, notification);
        }
    }
}