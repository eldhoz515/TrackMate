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


class Teacher_studentRequests : DialogFragment() {
    private lateinit var fragmentView: View
    private var studentRequests = mutableListOf<JSONObject>()
    private lateinit var teacherName: String

    companion object {
        fun newInstance(data: String): Teacher_studentRequests {
            val dialogFragment = Teacher_studentRequests()
            val args = Bundle()
            args.putString("username", data)
            dialogFragment.arguments = args
            return dialogFragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentView = inflater.inflate(R.layout.teacher_student_requests, container, false)
        return fragmentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Utils.print("Launching student requests")
        teacherName = arguments?.getString("username").toString()
        Utils.print(teacherName)
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
                        studentRequests.add(requests.getJSONObject(i))
                    }
                    if (studentRequests.size > 0) {
                        Utils.print(studentRequests)
                        setUI()
                    } else {
                        Utils.print("No student requests")
                        val msg = fragmentView.findViewById<TextView>(R.id.s_no_req)
                        msg.visibility = View.VISIBLE
                    }
                }
            }
        }
        val data = JSONObject()
        data.put("username", teacherName)
        Server(requireContext(), "/teacher/requests", "POST", data.toString(), callback).execute()
    }

    private fun setUI() {
        Utils.print("setUI()")
        val recyclerView: RecyclerView = fragmentView.findViewById(R.id.s_req_list)
        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager
        val adapter = AdapterStudentRequests(studentRequests, requireContext())
        recyclerView.adapter = adapter
    }

    inner class AdapterStudentRequests(
        private val items: MutableList<JSONObject>,
        private val con: Context
    ) :
        RecyclerView.Adapter<AdapterStudentRequests.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val username: TextView = view.findViewById(R.id.s_req_username)
            val name: TextView = view.findViewById(R.id.s_req_name)
            val accept: Button = view.findViewById(R.id.s_accept)
            val deny: Button = view.findViewById(R.id.s_reject)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_s_req, parent, false)
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
                json.put("username", teacherName)
                json.put("student", studentRequests[pos])
                json.put("accept", 1)
                val callback = object : HttpCallback {
                    override fun onComplete(result: HttpResult?) {
                        if (result != null && result.statusCode == 200)
                            Utils.print("responded successfully")
                    }
                }
                Server(con, "/teacher/respond", "POST", json.toString(), callback).execute()
                studentRequests.removeAt(pos)
                notifyItemRemoved(pos)
            }
            holder.deny.setOnClickListener {
                Utils.print("denied")
                val pos = holder.adapterPosition
                val json = JSONObject()
                json.put("username", teacherName)
                json.put("student", studentRequests[pos])
                val callback = object : HttpCallback {
                    override fun onComplete(result: HttpResult?) {
                        if (result != null && result.statusCode == 200)
                            Utils.print("responded successfully")
                    }
                }
                Server(con, "/teacher/respond", "POST", json.toString(), callback).execute()
                studentRequests.removeAt(pos)
                notifyItemRemoved(pos)
            }
        }

        override fun getItemCount() = items.size

    }
}

