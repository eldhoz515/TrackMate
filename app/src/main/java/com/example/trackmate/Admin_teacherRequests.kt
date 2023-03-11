package com.example.trackmate

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

private var teacherRequests = mutableListOf<JSONObject>()

class Admin_teacherRequests : DialogFragment() {
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
        getRequests()
    }

    private fun getRequests() {
        Utils.print("getRequests()")
        val callback = object : HttpCallback {
            override fun onComplete(result: HttpResult?) {
                if (result?.data != null && result.statusCode == 200) {
                    val json = JSONObject(result.data)
                    val requests = json.getJSONArray("requests")
                    for (i in 0 until requests.length()) {
                        teacherRequests.add(requests.getJSONObject(i))
                    }
                    if (teacherRequests.size > 0) {
                        Utils.print(teacherRequests)
                        setUI()
                    } else {
                        Utils.print("No teacher requests")
                        val msg=fragmentView.findViewById<TextView>(R.id.s_no_req)
                        msg.visibility=View.VISIBLE
                    }
                }
            }
        }
        Server("/admin/requests", "GET", null, callback).execute()
    }

    private fun setUI() {
        Utils.print("setUI()")
        val recyclerView: RecyclerView = fragmentView.findViewById(R.id.s_req_list)
        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager
        val adapter = AdapterAdminRequests(teacherRequests)
        recyclerView.adapter = adapter

    }
}

class AdapterAdminRequests(private val items: MutableList<JSONObject>) :
    RecyclerView.Adapter<AdapterAdminRequests.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val username: TextView = view.findViewById(R.id.t_req_username)
        val name: TextView = view.findViewById(R.id.t_req_name)
        val accept: Button = view.findViewById(R.id.t_req_accept)
        val deny: Button = view.findViewById(R.id.t_req_deny)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_t_req, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.username.text = item.get("username").toString()
        holder.name.text = item.get("name").toString()
        holder.accept.setOnClickListener {
            Utils.print("accepted")
            val pos = holder.adapterPosition
            val json = JSONObject()
            json.put("teacher", teacherRequests[pos])
            json.put("accept", 1)
            val callback = object : HttpCallback {
                override fun onComplete(result: HttpResult?) {
                    if (result != null && result.statusCode == 200)
                        Utils.print("responded successfully")
                }
            }
            Server("/admin/respond", "POST", json.toString(), callback).execute()
            teacherRequests.removeAt(pos)
            notifyItemRemoved(pos)
        }
        holder.deny.setOnClickListener {
            Utils.print("denied")
            val pos = holder.adapterPosition
            val json = JSONObject()
            json.put("teacher", teacherRequests[pos])
            val callback = object : HttpCallback {
                override fun onComplete(result: HttpResult?) {
                    if (result != null && result.statusCode == 200)
                        Utils.print("responded successfully")
                }
            }
            Server("/admin/respond", "POST", json.toString(), callback).execute()
            teacherRequests.removeAt(pos)
            notifyItemRemoved(pos)
        }
    }

    override fun getItemCount() = items.size
}
