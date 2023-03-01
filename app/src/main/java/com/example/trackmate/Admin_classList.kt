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

private var classes = mutableListOf<String>()

class Admin_classList : DialogFragment() {
    private lateinit var fragmentView: View
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentView = inflater.inflate(R.layout.admin_class_list, container, false)
        return fragmentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Utils.print("Launching classes list")
        getList()
    }

    private fun getList() {
        val callback = object : HttpCallback {
            override fun onComplete(result: HttpResult?) {
                if (result?.data != null && result.statusCode == 200) {
                    val json = JSONObject(result.data).getJSONArray("classes")
                    Utils.print(json)
                    for(className in 0 until json.length()){
                        classes.add(json.get(className).toString())
                    }
                    if (classes.size > 0) {
                        setUI()
                    } else {
                        Utils.print("No classes")
                        val msg = fragmentView.findViewById<TextView>(R.id.text_no_class)
                        msg.visibility = View.VISIBLE
                    }
                }
            }
        }
        Server("/admin/class/list", "GET", null, callback).execute()
    }

    private fun setUI() {
        Utils.print("setUI()")
        val recyclerView: RecyclerView = fragmentView.findViewById(R.id.c_list_list)
        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager
        val adapter = AdapterClassesList(classes)
        recyclerView.adapter = adapter
    }

    class AdapterClassesList(private val items: MutableList<String>) :
        RecyclerView.Adapter<AdapterClassesList.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.c_list_name)
            val remove: Button = view.findViewById(R.id.button_c_remove)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_c_list, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.name.text = item
            holder.remove.setOnClickListener {
                Utils.print("removed")
                val pos = holder.adapterPosition
                val json = JSONObject()
                json.put("class", classes[pos])
                val callback = object : HttpCallback {
                    override fun onComplete(result: HttpResult?) {
                        if (result != null && result.statusCode == 200) {
                            Utils.print("class removed successfully")
                        }
                    }
                }
                Server("/admin/class/remove", "POST", json.toString(), callback).execute()
                classes.removeAt(pos)
                notifyItemRemoved(pos)
            }
        }

        override fun getItemCount() = items.size
    }
}