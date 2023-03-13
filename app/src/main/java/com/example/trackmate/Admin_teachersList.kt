package com.example.trackmate

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

private var teachers = mutableListOf<JSONObject>()

class Admin_teachersList : DialogFragment() {
    private lateinit var fragmentView: View
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentView = inflater.inflate(R.layout.admin_teachers_list, container, false)
        return fragmentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Utils.print("Launching teachers list")
        getList()
    }

    private fun getList() {
        val callback = object : HttpCallback {
            override fun onComplete(result: HttpResult?) {
                if (result?.data != null && result.statusCode == 200) {
                    val json = JSONObject(result.data).getJSONObject("teachers")
                    Utils.print(json)
                    for (teacher in json.keys()) {
                        teachers.add(json.getJSONObject(teacher))
                    }
                    if (teachers.size > 0) {
                        setUI()
                    } else {
                        Utils.print("No teachers")
                        val msg = fragmentView.findViewById<TextView>(R.id.t_list_msg)
                        msg.visibility = View.VISIBLE
                    }
                }
            }
        }
        Server(requireContext(),"/admin/teacher/list", "GET", null, callback).execute()
    }

    private fun setUI() {
        Utils.print("setUI()")
        val recyclerView: RecyclerView = fragmentView.findViewById(R.id.t_list_list)
        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager
        val adapter = AdapterTeachersList(teachers,requireContext())
        recyclerView.adapter = adapter
    }

    class AdapterTeachersList(private val items: MutableList<JSONObject>,private val con: Context) :
        RecyclerView.Adapter<AdapterTeachersList.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val username: TextView = view.findViewById(R.id.t_list_username)
            val name: TextView = view.findViewById(R.id.t_list_name)
            val remove: Button = view.findViewById(R.id.t_remove)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_t_list, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.username.text = item.get("username").toString()
            holder.name.text = item.get("name").toString()
            holder.remove.setOnClickListener {
                Utils.print("removed")
                val pos = holder.adapterPosition
                val json = teachers[pos]
                val callback = object : HttpCallback {
                    override fun onComplete(result: HttpResult?) {
                        if (result != null && result.statusCode == 200) {
                            Utils.print("teacher removed successfully")
                        }
                    }
                }
                Server(con,"/admin/teacher/remove", "POST", json.toString(), callback).execute()
                teachers.removeAt(pos)
                notifyItemRemoved(pos)
            }
        }

        override fun getItemCount() = items.size
    }
}