package com.example.couseselector

import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.jsoup.Jsoup
import java.io.IOException


class SelectCourseActivity: AppCompatActivity() {

    private var selectCourseRecyclerReceiver = SelectCourseRecyclerReceiver()
    private var selectCourseToastReceiver = SelectCourseToastReceiver()
    private lateinit var startSelectCourseServiceIntent: Intent
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewAdapter: CourseAdapter

    private lateinit var addCourseName: EditText
    private lateinit var addCourseButton: Button
    private lateinit var startSelectCourseButton: Button
    private lateinit var stopSelectCourseButton: Button

    private lateinit var prepareStatus: LinearLayout
    private lateinit var courseList: ArrayList<Course>
    private lateinit var innerCourseList: ArrayList<String>
    private lateinit var loginCookie: String

    inner class SelectCourseToastReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var msg = intent?.getStringExtra("msg")
            Toast.makeText(this@SelectCourseActivity, msg, Toast.LENGTH_SHORT).show()
        }
    }

    inner class SelectCourseRecyclerReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var msg = intent?.getStringExtra("msg")
            var pos = intent?.getIntExtra("pos", 0)
            var remain = intent?.getIntExtra("remain", 0)
            courseList[pos!!].selectStatus = msg
            recyclerViewAdapter.notifyDataSetChanged()
            if (remain == 0) {
                stopSelectCourseButton.performClick()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select)
        registerReceiver(selectCourseRecyclerReceiver, IntentFilter("com.example.couseselector.recycler"))
        registerReceiver(selectCourseToastReceiver, IntentFilter("com.example.couseselector.toast"))
        loginCookie = intent.getStringExtra("loginCookie")
        recyclerView = findViewById(R.id.recycler_view)

        addCourseName = findViewById<EditText>(R.id.add_course_name)
        addCourseButton = findViewById<Button>(R.id.add_course)
        startSelectCourseButton = findViewById<Button>(R.id.start_select)
        stopSelectCourseButton = findViewById<Button>(R.id.stop_select)
        prepareStatus = findViewById<LinearLayout>(R.id.prepareStatus)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        courseList = ArrayList<Course>()
        innerCourseList = ArrayList<String>()
        recyclerViewAdapter = CourseAdapter(courseList)
        recyclerView.adapter = recyclerViewAdapter

        addCourseButton.setOnClickListener {
            var courseName = addCourseName.text.toString()
            if (courseName.equals("")) {
                Toast.makeText(this@SelectCourseActivity, "输入课程名称为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            courseList.add(Course(courseName, "待选课.."))
            innerCourseList.add(courseName)
            recyclerViewAdapter.notifyDataSetChanged()
        }
        startSelectCourseButton.setOnClickListener {
            prepareStatus.visibility = View.GONE
            stopSelectCourseButton.visibility = View.VISIBLE
            for (course in courseList) {
                if (course.selectStatus == "待选课..") {
                    course.selectStatus = "正在选课.."
                }
            }
            recyclerViewAdapter.notifyDataSetChanged()
            startSelectCourseServiceIntent = Intent(this, SelectCourseService::class.java)
            startSelectCourseServiceIntent.putExtra("loginCookie", loginCookie)
            startSelectCourseServiceIntent.putExtra("expectCourseNameList", innerCourseList)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(startSelectCourseServiceIntent)
            }
        }
        stopSelectCourseButton.setOnClickListener {
            prepareStatus.visibility = View.VISIBLE
            stopSelectCourseButton.visibility = View.GONE
            for (course in courseList) {
                if (course.selectStatus == "正在选课..") {
                    course.selectStatus = "待选课.."
                }
            }
            recyclerViewAdapter.notifyDataSetChanged()
            stopService(startSelectCourseServiceIntent)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("gzz", "singleTop")
    }

    override fun onDestroy() {
        unregisterReceiver(selectCourseRecyclerReceiver)
        unregisterReceiver(selectCourseToastReceiver)
        super.onDestroy()
    }
}