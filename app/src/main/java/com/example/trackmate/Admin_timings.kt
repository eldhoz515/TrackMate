package com.example.trackmate

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

private fun request(json: JSONObject, con: Context) {
    val callback = object : HttpCallback {
        override fun onComplete(result: HttpResult?) {
            if (result != null && result.statusCode == 200) {
                Utils.print("timings updated successfully")
            }
        }
    }
    Server(con, "/admin/timings", "POST", json.toString(), callback).execute()
}

class Admin_timings : DialogFragment() {
    private var timings = mutableListOf<JSONObject>()
    private lateinit var fragmentView: View
    private lateinit var timePicker: TimePicker
    private lateinit var add_time: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Dialog)
    }

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
                    val t = json.getJSONArray("timings")
                    for (i in 0 until t.length()) {
                        timings.add(t.getJSONObject(i))
                    }
                    Utils.print(timings)
                    setUI()
                }
            }
        }
        Server(requireContext(), "/admin/timings", "GET", null, callback).execute()
    }

    private fun setUI() {
        Utils.print("setUI()")
        timePicker = fragmentView.findViewById(R.id.timePicker)
        add_time = fragmentView.findViewById(R.id.add_time)
        val recyclerView: RecyclerView = fragmentView.findViewById(R.id.timings)
        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager
        val adapter = AdapterTimingsList(timings, requireContext())
        recyclerView.adapter = adapter
        add_time.setOnClickListener {
            val time = JSONObject()
            time.put("hr", timePicker.hour)
            time.put("min", timePicker.minute)
            var pos = 0
            if (timings.size > 0) {
                while (pos < timings.size && timings[pos].getInt("hr") < time.getInt("hr")) {
                    ++pos
                }
                while (pos < timings.size && timings[pos].getInt("min") < time.getInt("min")) {
                    if (timings[pos].getInt("hr") > time.getInt("hr"))
                        break
                    ++pos
                }
            }
            Utils.print(pos)
            if (pos == timings.size || timings[pos]
                    .getInt("hr") != time.getInt("hr") || timings[pos]
                    .getInt("min") != time.getInt("min")
            ) {
                timings.add(pos, time)
                adapter.notifyItemInserted(pos)
                Utils.print(timings)
                val json = JSONObject()
                json.put("timings", JSONArray(timings))
                request(json, requireContext())
            } else {
                Utils.print("time already exists")
            }
        }
    }

    inner class AdapterTimingsList(
        private val items: MutableList<JSONObject>,
        private val con: Context
    ) :
        RecyclerView.Adapter<AdapterTimingsList.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val time: TextView = view.findViewById(R.id.time)
            val remove: Button = view.findViewById(R.id.time_remove)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_time, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item: JSONObject = items[position]
            holder.time.text = "${item.get("hr").toString()} : ${item.get("min").toString()}"
            holder.remove.setOnClickListener {
                Utils.print("removed")
                val pos = holder.adapterPosition
                timings.removeAt(pos)
                notifyItemRemoved(pos)
                val json = JSONObject()
                json.put("timings", JSONArray(timings))
                request(json, con)
            }
        }

        override fun getItemCount() = items.size
    }

}