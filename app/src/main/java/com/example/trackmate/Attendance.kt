package com.example.trackmate

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

class Attendance : DialogFragment() {
    private var students = mutableListOf<JSONObject>()
    private lateinit var selectedClass:String
    private lateinit var fragmentView: View
    private lateinit var classesList: Spinner
    private var classes = mutableListOf<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Dialog)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentView = inflater.inflate(R.layout.attendance, container, false)
        return fragmentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Utils.print("Launching attendance list")
        getClasses()
    }

    private fun getClasses() {
        val callback = object : HttpCallback {
            override fun onComplete(result: HttpResult?) {
                if (result?.data != null && result.statusCode == 200) {
                    val json = JSONObject(result.data).getJSONArray("classes")
                    Utils.print(json)
                    for (className in 0 until json.length()) {
                        classes.add(json.get(className).toString())
                    }
                    if (classes.size > 0) {
                        setupSpinner()
                    } else {
                        Utils.print("No classes")
                        val msg = fragmentView.findViewById<TextView>(R.id.a_no_class)
                        msg.visibility = View.VISIBLE
                    }
                }
            }
        }
        Server(requireContext(),"/admin/class/list", "GET", null, callback).execute()
    }

    private fun setupSpinner() {
        Utils.print("setupSpinner()")
        classesList=fragmentView.findViewById(R.id.a_c_list)
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, classes)
        classesList.adapter = adapter
        classesList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                Utils.print("class selected")
                val item = parent.getItemAtPosition(position).toString()
                selectedClass=item
                val msg = fragmentView.findViewById<TextView>(R.id.a_no_students)
                msg.visibility = View.GONE
                students= mutableListOf()
                val attList=fragmentView.findViewById<RecyclerView>(R.id.a_list_list)
                attList.visibility=View.GONE
                getStudents(item)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Another interface callback
            }
        }
    }

    private fun getStudents(className: String) {
        val data= JSONObject()
        data.put("class",className)
        val callback=object :HttpCallback{
            override fun onComplete(result: HttpResult?) {
                if(result?.data!=null && result.statusCode==200){
                    val classAttendance= JSONObject(result.data)
                    for(name in classAttendance.keys()){
                        val item=JSONObject()
                        item.put("username",name.toString())
                        item.put("attendance",classAttendance.getJSONArray(name))
                        students.add(item)
                    }
                    if(students.size >0){
                        Utils.print("setting up students list")
                        val recyclerView: RecyclerView = fragmentView.findViewById(R.id.a_list_list)
                        recyclerView.visibility=View.VISIBLE
                        val layoutManager = LinearLayoutManager(context)
                        recyclerView.layoutManager = layoutManager
                        val adapter = AdapterReportingList(students,requireContext())
                        recyclerView.adapter = adapter
                    }
                    else{
                        Utils.print("No students in this class")
                        val msg = fragmentView.findViewById<TextView>(R.id.a_no_students)
                        msg.visibility = View.VISIBLE
                    }
                }
            }
        }
        Server(requireContext(),"/admin/attendance","POST",data.toString(),callback).execute()
    }

    inner class AdapterReportingList(private val items: MutableList<JSONObject>, private val con: Context) :
        RecyclerView.Adapter<AdapterReportingList.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val username: TextView = view.findViewById(R.id.a_username)
            val percentage:TextView=view.findViewById(R.id.a_percentage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_a, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.username.text = item.get("username").toString()
            val stuAtt:JSONArray=item.getJSONArray("attendance")
            var ab = 0;
            for (i in 0 until stuAtt.length()) {
                if (stuAtt.getInt(i) == 0)
                    ab += 1
            }
            holder.percentage.text="${(((stuAtt.length() - ab).toDouble() / stuAtt.length()) * 100).roundToInt()}%"
        }

        override fun getItemCount() = items.size
    }
}