package com.example.trackmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import org.json.JSONObject
import java.io.File

class Address : DialogFragment() {
    private lateinit var fragmentView: View
    private lateinit var save: Button
    private lateinit var address: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentView = inflater.inflate(R.layout.address, container, false)
        return fragmentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Utils.print("Launching address")
        setUI()
    }

    private fun setUI() {
        Utils.print("setUI()")
        save = fragmentView.findViewById(R.id.change_address)
        address = fragmentView.findViewById(R.id.address)
        val file = readFile("address.json")
        if (file != null)
            address.setText(file.getString("address"))
        save.setOnClickListener {
            val json = JSONObject()
            json.put("address", address.text.toString())
            writeFile("address.json", json)
            this.dismiss()
        }
    }

    private fun writeFile(fname: String, data: JSONObject) {
        val jsonString = data.toString()
        val file = File(requireContext().filesDir, fname)
        file.writeText(jsonString)
    }

    private fun readFile(fname: String): JSONObject? {
        val file = File(requireContext().filesDir, fname)
        return if (file.exists()) {
            val jsonString = file.readText()
            JSONObject(jsonString)
        } else {
            null
        }
    }
}