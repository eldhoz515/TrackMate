package com.example.trackmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class Admin_teacherRequests : DialogFragment() {
    private val root = "http://192.168.1.5:8888"
    private var teacherRequests = mutableListOf<String>()
    private lateinit var fragmentView: View
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentView = inflater.inflate(R.layout.admin_teacher_requests, container, false)
        return fragmentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Utils.print("Launching teacher requests")
//        getRequests()
        setUI()
    }

//    private fun getRequests() {
//        val callback = object : HttpCallback {
//            override fun onComplete(result: HttpResult?) {
//                if (result?.data != null && result.statusCode == 200) {
//                    val json = JSONObject(result.data)
//                    val requests = json.getJSONArray("requests")
//                    for (i in 0 until requests.length()) {
//                        teacherRequests.add(requests.getJSONObject(i))
//                    }
//                    if (teacherRequests.size > 0) {
//                        setUI()
//                    } else {
//                        Utils.print("No teacher requests")
//                    }
//                }
//            }
//        }
//        Server("$root/admin/requests", "GET", null, callback).execute()
//    }

    private fun setUI() {
        val recyclerView: RecyclerView = fragmentView.findViewById(R.id.t_req_list)
        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager
        teacherRequests = mutableListOf("Arun", "Devananth", "Fleming")
        val adapter = ItemAdapter(teacherRequests)
        recyclerView.adapter = adapter
    }
}

class ItemAdapter(private val items: MutableList<String>) :
    RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text1: TextView = view.findViewById(R.id.t_req_username)
        val text2: TextView = view.findViewById(R.id.t_req_name)
        val button1: Button = view.findViewById(R.id.t_req_accept)
        val button2: Button = view.findViewById(R.id.t_req_deny)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_t_req, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.text1.text = item
        holder.text2.text = item
        holder.button1.setOnClickListener {
        }
        holder.button2.setOnClickListener {
        }
    }

    override fun getItemCount() = items.size
}
