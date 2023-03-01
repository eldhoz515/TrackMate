package com.example.trackmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private var timings = JSONArray()

private fun request(json: JSONObject) {
    val callback = object : HttpCallback {
        override fun onComplete(result: HttpResult?) {
            if (result != null && result.statusCode == 200) {
                Utils.print("timings updated successfully")
            }
        }
    }
    Server("/admin/timings", "POST", json.toString(), callback).execute()
}

class Admin_timings : DialogFragment() {
    private lateinit var fragmentView: View
    private lateinit var edit_time: EditText
    private lateinit var add_time: Button
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentView = inflater.inflate(R.layout.admin_timings, container, false)
        return fragmentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Utils.print("Launching class timings")
        getTimings()
    }

    private fun getTimings() {
        Utils.print("getTimings()")
        val callback = object : HttpCallback {
            override fun onComplete(result: HttpResult?) {
                if (result?.data != null && result.statusCode == 200) {
                    val json = JSONObject(result.data)
                    timings = json.getJSONArray("timings")
                    Utils.print(timings)
                    setUI()
                }
            }
        }
        Server("/admin/timings", "GET", null, callback).execute()
    }

    private fun setUI() {
        Utils.print("setUI()")
        edit_time = fragmentView.findViewById(R.id.edit_time)
        add_time = fragmentView.findViewById(R.id.time_remove)
        add_time.setOnClickListener {
            val input = edit_time.text.toString()
            if (input.length > 1) {
                val t = LocalTime.parse(input, DateTimeFormatter.ISO_LOCAL_TIME)
                val time = JSONObject()
                time.put("hr", t.hour)
                time.put("min", t.minute)
                timings.put(time)
                val json = JSONObject()
                json.put("timings", timings)
                request(json)
            }
        }
        val recyclerView: RecyclerView = fragmentView.findViewById(R.id.timings)
        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager
        val adapter = AdapterTimingsList(timings)
        recyclerView.adapter = adapter
    }

    class AdapterTimingsList(private val items: JSONArray) :
        RecyclerView.Adapter<AdapterTimingsList.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val time: TextView = view.findViewById(R.id.time)
            val remove: Button = view.findViewById(R.id.time_remove)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_time, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item: JSONObject = items.getJSONObject(position)
            holder.time.text = "${item.get("hr").toString()} : ${item.get("min").toString()}"
            holder.remove.setOnClickListener {
                Utils.print("removed")
                val pos = holder.adapterPosition
                timings.remove(pos)
                notifyItemRemoved(pos)
                val json = JSONObject()
                json.put("timings", timings)
                request(json)
            }
        }

        override fun getItemCount() = items.length()
    }

}