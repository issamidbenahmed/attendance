package com.example.attendanceqr

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import org.json.JSONObject
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

class ScanActivity : AppCompatActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var apiService: AttendanceApiService
    private lateinit var btnScanQR: Button
    private lateinit var btnViewAttendances: Button
    private lateinit var tvResult: TextView

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 101
    }

    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            if (result.text == null) {
                return
            }

            barcodeView.pause()
            tvResult.text = result.text
            tvResult.visibility = TextView.VISIBLE

            val token = sharedPreferences.getString("ACCESS_TOKEN", null) ?: run {
                Toast.makeText(this@ScanActivity, "Please login first", Toast.LENGTH_SHORT).show()
                return
            }

            try {
                val rawJson = JSONObject(result.text)
                val gson = Gson()
                val qrDataObject = gson.fromJson(rawJson.toString(), QrData::class.java)
                val qrDataJsonString = gson.toJson(qrDataObject) // ⬅️ Convertir en string

                val scanRequest = ScanRequest(qr_data = qrDataJsonString)

                apiService.scanAttendance("Bearer $token", scanRequest)
                    .enqueue(object : Callback<AttendanceResponse> {
                        override fun onResponse(
                            call: Call<AttendanceResponse>,
                            response: Response<AttendanceResponse>
                        ) {
                            if (response.isSuccessful) {
                                response.body()?.let {
                                    Toast.makeText(
                                        this@ScanActivity,
                                        "Attendance recorded for ${it.studentName}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } else {
                                val errorMsg = when (response.code()) {
                                    401 -> "Session expired, please login again"
                                    403 -> "You don't have permission to scan"
                                    else -> "Error: ${response.code()}"
                                }
                                Toast.makeText(this@ScanActivity, errorMsg, Toast.LENGTH_SHORT).show()
                            }
                            barcodeView.resume()
                        }

                        override fun onFailure(call: Call<AttendanceResponse>, t: Throwable) {
                            Toast.makeText(
                                this@ScanActivity,
                                "Network error: ${t.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            barcodeView.resume()
                        }
                    })
            } catch (e: Exception) {
                Toast.makeText(
                    this@ScanActivity,
                    "QR Code invalide ou mal formaté",
                    Toast.LENGTH_SHORT
                ).show()
                barcodeView.resume()
            }
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        sharedPreferences = getSharedPreferences("AuthPrefs", MODE_PRIVATE)
        barcodeView = findViewById(R.id.barcodeView)
        btnScanQR = findViewById(R.id.btnScanQR)
        btnViewAttendances = findViewById(R.id.btnViewAttendances)
        tvResult = findViewById(R.id.tvResult)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://100.70.32.233:8000/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(AttendanceApiService::class.java)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            initializeScanner()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        }

        btnScanQR.setOnClickListener {
            barcodeView.decodeSingle(callback)
        }

        btnViewAttendances.setOnClickListener {
            startActivity(Intent(this, AttendanceActivity::class.java))
        }
    }

    private fun initializeScanner() {
        barcodeView.barcodeView.decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
        barcodeView.setStatusText("")
        barcodeView.decodeContinuous(callback)

        barcodeView.setTorchListener(object : DecoratedBarcodeView.TorchListener {
            override fun onTorchOn() {}
            override fun onTorchOff() {}
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    initializeScanner()
                } else {
                    Toast.makeText(
                        this,
                        "Camera permission is required to scan QR codes",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            barcodeView.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        barcodeView.barcodeView.cameraInstance?.close()
    }

    // ✅ Nouvelle data class pour contenir le contenu JSON du QR Code
    data class QrData(
        @SerializedName("Code Apogée")
        val codeApogee: String,

        @SerializedName("Nom - Prénom")
        val nomPrenom: String,

        @SerializedName("Email")
        val email: String,

        @SerializedName("Module")
        val module: String
    )

    // 🔁 Modifiée pour contenir un objet au lieu d’une simple chaîne
    data class ScanRequest(val qr_data: String)

    data class AttendanceResponse(
        val status: String,
        val attendanceId: Int,
        val studentName: String,
        val module: String,
        val timestamp: String
    )

    interface AttendanceApiService {
        @POST("scan/")
        fun scanAttendance(
            @Header("Authorization") token: String,
            @Body request: ScanRequest
        ): Call<AttendanceResponse>
    }
}
