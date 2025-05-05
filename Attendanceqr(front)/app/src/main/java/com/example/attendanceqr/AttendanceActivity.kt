package com.example.attendanceqr

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.attendanceqr.databinding.ActivityAttendanceBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

// ... (imports restent inchangés)

class AttendanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAttendanceBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var apiService: AttendanceApiService
    private lateinit var adapter: AttendanceAdapter
    private var attendanceList = mutableListOf<AttendanceRecord>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences = getSharedPreferences("AuthPrefs", MODE_PRIVATE)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://100.70.32.233:8000/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(AttendanceApiService::class.java)

        adapter = AttendanceAdapter(attendanceList)
        binding.rvAttendance.layoutManager = LinearLayoutManager(this)
        binding.rvAttendance.adapter = adapter

        loadAttendanceData()

        binding.btnFilter.setOnClickListener { applyFilters() }
        binding.btnExport.setOnClickListener { exportToCSV() }
        binding.fabRefresh.setOnClickListener { loadAttendanceData() }
    }

    private fun loadAttendanceData() {
        apiService.getAttendanceRecords().enqueue(object : Callback<List<AttendanceRecord>> {
            override fun onResponse(call: Call<List<AttendanceRecord>>, response: Response<List<AttendanceRecord>>) {
                if (response.isSuccessful) {
                    attendanceList.clear()
                    response.body()?.let { attendanceList.addAll(it) }
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@AttendanceActivity, "Failed to load attendance", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<AttendanceRecord>>, t: Throwable) {
                Toast.makeText(this@AttendanceActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun applyFilters() {
        val searchQuery = binding.etSearch.text.toString().trim()
        if (searchQuery.isEmpty()) {
            adapter.updateList(attendanceList)
            return
        }

        val filteredList = attendanceList.filter {
            it.studentName.contains(searchQuery, ignoreCase = true) ||
                    it.module.contains(searchQuery, ignoreCase = true)
        }
        adapter.updateList(filteredList)
    }

    private fun exportToCSV() {
        if (attendanceList.isEmpty()) {
            Toast.makeText(this, "No attendance records to export", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Attendance_$timeStamp.csv"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val csvFile = File(storageDir, fileName)

            FileWriter(csvFile).use { writer ->
                writer.append("Student Name,Module,Timestamp\n")
                attendanceList.forEach { record ->
                    writer.append("${record.studentName},${record.module},${record.timestamp}\n")
                }
            }

            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", csvFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share Attendance CSV"))

        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // Data classes et interface
    data class AttendanceRecord(
        val id: Int,
        val studentName: String,
        val module: String,
        val timestamp: String
    )

    interface AttendanceApiService {
        @GET("attendances/")
        fun getAttendanceRecords(): Call<List<AttendanceRecord>>
    }
}

class AttendanceAdapter(private var attendanceList: List<AttendanceActivity.AttendanceRecord>) :
    RecyclerView.Adapter<AttendanceAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStudentName: TextView = itemView.findViewById(R.id.tvStudentName)
        val tvModuleValue: TextView = itemView.findViewById(R.id.tvModuleValue)
        val tvTimestampValue: TextView = itemView.findViewById(R.id.tvTimestampValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_attendance, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = attendanceList[position]
        holder.tvStudentName.text = record.studentName
        holder.tvModuleValue.text = record.module
        holder.tvTimestampValue.text = record.timestamp
    }

    override fun getItemCount() = attendanceList.size

    fun updateList(newList: List<AttendanceActivity.AttendanceRecord>) {
        attendanceList = newList
        notifyDataSetChanged()
    }
}
