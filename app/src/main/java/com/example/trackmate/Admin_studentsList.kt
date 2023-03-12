package com.example.trackmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

private var students = mutableListOf<JSONObject>()
private lateinit var selectedClass:String

class Admin_studentsList : DialogFragment() {
    private lateinit var fragmentView: View
    private lateinit var classesList: Spinner
    private var classes = mutableListOf<String>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentView = inflater.inflate(R.layout.admin_student_list, container, false)
        return fragmentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Utils.print("Launching students list")
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
                        val msg = fragmentView.findViewById<TextView>(R.id.text_no_class2)
                        msg.visibility = View.VISIBLE
                    }
                }
            }
        }
        Server(Admin_studentsList().requireContext(),"/admin/class/list", "GET", null, callback).execute()
    }

    private fun setupSpinner() {
        Utils.print("setupSpinner()")
        classesList=fragmentView.findViewById(R.id.s_c_list)
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
                students= mutableListOf()
                getStudents(item)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Another interface callback
            }
        }
    }

    private fun getStudents(className: String) {
        val data=JSONObject()
        data.put("class",className)
        val callback=object :HttpCallback{
            override fun onComplete(result: HttpResult?) {
                if(result?.data!=null && result.statusCode==200){
                    val json=JSONObject(result.data)
                    for(student in json.keys()){
                        students.add(json.getJSONObject(student))
                    }
                    if(students.size>0){
                        Utils.print("setting up students list")
                        val recyclerView: RecyclerView = fragmentView.findViewById(R.id.s_list_list)
                        val layoutManager = LinearLayoutManager(context)
                        recyclerView.layoutManager = layoutManager
                        val adapter = AdapterStudentsList(students)
                        recyclerView.adapter = adapter
                    }
                    else{
                        Utils.print("No students in this class")
                        val msg = fragmentView.findViewById<TextView>(R.id.text_no_student)
                        msg.visibility = View.VISIBLE
                    }
                }
            }
        }
        Server(Admin_studentsList().requireContext(),"/admin/class/view","POST",data.toString(),callback).execute()
    }

    class AdapterStudentsList(private val items: MutableList<JSONObject>) :
        RecyclerView.Adapter<AdapterStudentsList.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.s_list_name)
            val username: TextView = view.findViewById(R.id.s_list_username)
            val remove: Button = view.findViewById(R.id.button_s_remove)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_s_list, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.name.text = item.get("name").toString()
            holder.username.text = item.get("username").toString()
            holder.remove.setOnClickListener {
                Utils.print("removed")
                val pos = holder.adapterPosition
                val json = JSONObject()
                json.put("class", selectedClass)
                json.put("username", students[pos].get("username").toString())
                Utils.print(json)
                val callback = object : HttpCallback {
                    override fun onComplete(result: HttpResult?) {
                        if (result != null && result.statusCode == 200) {
                            Utils.print("student removed successfully")
                        }
                    }
                }
                Server(Admin_studentsList().requireContext(),"/admin/student/remove", "POST", json.toString(), callback).execute()
                students.removeAt(pos)
                notifyItemRemoved(pos)
            }
        }

        override fun getItemCount() = items.size
    }
}