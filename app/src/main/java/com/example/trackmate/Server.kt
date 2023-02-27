package com.example.trackmate

import android.os.AsyncTask
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class HttpResult(
    val statusCode: Int,
    val data: String?
)

interface HttpCallback {
    fun onComplete(result: HttpResult?)
}

class Server(
    private val url: String,
    private val method: String,
    private val data: String?,
    private val callback: HttpCallback
) : AsyncTask<Void, Void, HttpResult>() {

    override fun doInBackground(vararg params: Void?): HttpResult? {
        try {
            val urlObj = URL(url)
            val connection = urlObj.openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.connectTimeout = 10000 // optional
            connection.readTimeout = 10000 // optional


            if (data != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                val output = OutputStreamWriter(connection.outputStream)
                output.write(data)
                output.flush()
                output.close()
            }

            val statusCode = connection.responseCode
            val inputStream = if (statusCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val response = StringBuffer()
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))

            var inputLine = bufferedReader.readLine()
            while (inputLine != null) {
                response.append(inputLine)
                inputLine = bufferedReader.readLine()
            }
            bufferedReader.close()
            inputStream.close()
            connection.disconnect()
            return HttpResult(statusCode, response.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun onPostExecute(result: HttpResult?) {
        super.onPostExecute(result)
        callback.onComplete(result)
    }
}
