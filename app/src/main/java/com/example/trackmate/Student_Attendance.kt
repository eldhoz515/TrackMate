package com.example.trackmate

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

class Student_Attendance : DialogFragment() {
    private lateinit var className: String
    private lateinit var studentName: String
    private lateinit var fragmentView: View
    private lateinit var total: TextView
    private lateinit var present: TextView
    private lateinit var absent: TextView
    private lateinit var percentage: TextView
    private var attendance = mutableListOf<Int>()
    private lateinit var stuAtt: JSONArray

    companion object {
        fun newInstance(data: JSONObject): Student_Attendance {
            val dialogFragment = Student_Attendance()
            val args = Bundle()
            args.putString("class", data.getString("class"))
            args.putString("username", data.getString("username"))
            dialogFragment.arguments = args
            return dialogFragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        fragmentView = inflater.inflate(R.layout.student_attendance, container, false)
        return fragmentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        className = arguments?.getString("class").toString()
        studentName = arguments?.getString("username").toString()
        Utils.print("$className $studentName")
        Utils.print("Launching student attendance")
        getAttendance()
    }

    private fun getAttendance() {
        val data = JSONObject()
        data.put("class", className)
        val callback = object : HttpCallback {
            override fun onComplete(result: HttpResult?) {
                if (result?.data != null && result.statusCode == 200) {
                    val classAttendance = JSONObject(result.data)
                    Utils.print(classAttendance)
                    if (classAttendance.has(studentName)) {
                        stuAtt = classAttendance.getJSONArray(studentName)
                        Utils.print(stuAtt)
                        for (i in (0 until stuAtt.length()).reversed()) {
                            if (attendance.size > 4) break
                            attendance.add(stuAtt.getInt(i))
                        }
                        setUI()
                    } else {
                        Utils.print("No recent sessions")
                    }
                }
            }
        }
        Server(requireContext(), "/student/attendance", "POST", data.toString(), callback).execute()
    }

    private fun setUI() {
        Utils.print("setUI")
        total = fragmentView.findViewById(R.id.s_a_total)
        present = fragmentView.findViewById(R.id.s_a_present)
        absent = fragmentView.findViewById(R.id.s_a_absent)
        percentage = fragmentView.findViewById(R.id.s_a_percentage)
        var ab = 0;
        for (i in 0 until stuAtt.length()) {
            if (stuAtt.getInt(i) == 0) ab += 1
        }
        total.text = "Total Sessions : ${stuAtt.length()}"
        present.text = "Present : ${stuAtt.length() - ab}"
        absent.text = "Absent : ${ab}"
        percentage.text =
            "Percentage : ${(((stuAtt.length() - ab).toDouble() / stuAtt.length()) * 100).roundToInt()}%"
        Utils.print(attendance)
        val recyclerView: RecyclerView = fragmentView.findViewById(R.id.s_a_recent)
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.layoutManager = layoutManager
        val adapter = AdapterStudentAttendance(attendance, requireContext())
        recyclerView.adapter = adapter
    }

    inner class AdapterStudentAttendance(
        private val items: MutableList<Int>, private val con: Context
    ) : RecyclerView.Adapter<AdapterStudentAttendance.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val item: TextView = view.findViewById(R.id.s_a_recent_item)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_s_recent, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            if (item == 1) holder.item.setBackgroundResource(R.drawable.tick_button)
            else holder.item.setBackgroundResource(R.drawable.remove_button)
        }

        override fun getItemCount() = items.size

    }
}