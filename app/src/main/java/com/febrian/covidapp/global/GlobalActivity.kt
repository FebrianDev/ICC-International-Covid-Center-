package com.febrian.covidapp.global

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.location.Address
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.view.setPadding
import com.febrian.covidapp.MapActivity
import com.febrian.covidapp.R
import com.febrian.covidapp.api.ApiService
import com.febrian.covidapp.api.Constant
import com.febrian.covidapp.databinding.ActivityGlobalBinding
import com.febrian.covidapp.home.response.CountryResponse
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.huawei.hms.maps.CameraUpdateFactory
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.OnMapReadyCallback
import com.huawei.hms.maps.SupportMapFragment
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.MarkerOptions
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.AsyncHttpResponseHandler
import com.loopj.android.http.ResponseHandlerInterface
import cz.msebera.android.httpclient.Header
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.*
import java.io.IOException
import java.lang.Exception
import java.math.BigDecimal

class GlobalActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityGlobalBinding
    private lateinit var mMap: HuaweiMap

    @DelicateCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGlobalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ApiService.globalDataCovid.getGlobalData().enqueue(object : Callback<CountryResponse> {
            override fun onResponse(
                call: Call<CountryResponse>,
                response: Response<CountryResponse>
            ) {
                if (response.isSuccessful) {

                    val body = response.body()
                    if (body != null) {
                        setGlobalData(body)
                    }
                }
            }

            override fun onFailure(call: Call<CountryResponse>, t: Throwable) {

            }

        })

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapHome) as SupportMapFragment

        val btn = findViewById<Button>(R.id.btn_maps)
        btn.setOnClickListener {
            startActivity(Intent(applicationContext, MapActivity::class.java))
        }

        mapFragment.getMapAsync(this)
    }


    override fun onMapReady(googleMap: HuaweiMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = false
        val location = applicationContext.resources.configuration.locale.displayCountry
        findViewById<TextView>(R.id.country).text = location
        var addressList : List<Address>? = null
        var latLng : LatLng? = null
        val geocoder = Geocoder(this@GlobalActivity)
        var address : Address? = null
        try {
            // on below line we are getting location from the
            // location name and adding that location to address list.
            addressList = geocoder.getFromLocationName(location, 1)
            address = addressList!![0]
            latLng = LatLng(address!!.latitude, address!!.longitude)
            mMap.addMarker(MarkerOptions().position(latLng).title(location.toString()))
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(address.latitude, address.longitude), 3f))
        } catch (e: IOException) {
            println(e.message)
            e.printStackTrace()
        }
        // on below line we are getting the location
        // from our list a first position.
      //  val address: Address = addressList!![0]

    }

    @SuppressLint("SetTextI18n")
    private fun setGlobalData(body: CountryResponse) {
        val totalCase = body.confirmed?.value!!.toBigDecimal()
        val recovered = body.recovered?.value!!.toBigDecimal()
        val deaths = body.deaths?.value!!.toBigDecimal()
        val confirmed = totalCase - (recovered + deaths)

        binding.confirmed.text = confirmed.toString()
        binding.recovered.text = recovered.toString()
        binding.deceased.text = deaths.toString()

        setBarChart(confirmed, recovered, deaths, totalCase)
    }

    private fun setBarChart(
        confirmed: BigDecimal,
        recovered: BigDecimal,
        death: BigDecimal,
        totalCase: BigDecimal
    ) {
        val listPie = ArrayList<PieEntry>()
        val listColors = ArrayList<Int>()

        listPie.add(PieEntry(confirmed.toFloat()))
        listColors.add(resources.getColor(R.color.yellow_primary))
        listPie.add(PieEntry(recovered.toFloat()))
        listColors.add(resources.getColor(R.color.green_custom))
        listPie.add(PieEntry(death.toFloat()))
        listColors.add(resources.getColor(R.color.red_custom))

        val pieDataSet = PieDataSet(listPie, "")
        pieDataSet.colors = listColors

        val pieData = PieData(pieDataSet)
        pieData.setValueTextSize(0f)
        pieData.setValueTextColor(resources.getColor(android.R.color.transparent))
        binding.pieChart.data = pieData

        binding.pieChart.setUsePercentValues(true)
        binding.pieChart.animateY(1400, Easing.EaseInOutQuad)

        //set text center
        binding.pieChart.centerText = "Total Case\n${totalCase}"
        binding.pieChart.setCenterTextColor(Color.argb(255, 80,125,188))
        binding.pieChart.setCenterTextSize(18f)
        binding.pieChart.setCenterTextTypeface(Typeface.DEFAULT_BOLD)

        binding.pieChart.legend.isEnabled = false // hide tags labels

        binding.pieChart.setHoleColor(android.R.color.transparent)
        binding.pieChart.setTransparentCircleColor(android.R.color.white)

        binding.pieChart.holeRadius = 75f

        binding.pieChart.setDrawEntryLabels(false)
        binding.pieChart.description.isEnabled = false
    }

}