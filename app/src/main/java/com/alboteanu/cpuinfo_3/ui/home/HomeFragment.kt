package com.alboteanu.cpuinfo_3.ui.home

import android.arch.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.alboteanu.cpuinfo_3.databinding.FragmentHomeBinding
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    val series: LineGraphSeries<DataPoint> = LineGraphSeries()
    var oldCpuTime: CpuTime? = null
    val handler = Handler(Looper.getMainLooper())
    var timestamp = 0
    val stepValueSec = 1
    val updatePeriod: Long = (stepValueSec * 1000).toLong()   // milli-seconds


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(
                this,
                ViewModelProvider.NewInstanceFactory()
            ).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        binding.graph.apply {
            viewport.isYAxisBoundsManual = true
            viewport.setMinY(0.0)
            viewport.setMaxY(100.0)
            title = "CPU load"
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        timestamp = 0
        handler.post(runnable)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(runnable)
    }


    private val runnable = object : Runnable {
        override fun run() {
            val bufferedReader = BufferedReader(InputStreamReader(FileInputStream("/proc/stat")))
            val cpuTime = getCpuTime(bufferedReader.readLine())
            bufferedReader.close()

            if (oldCpuTime != null) {
                val deltaIdleTime = cpuTime.idleTime - oldCpuTime!!.idleTime
                val deltaTotalTime = cpuTime.totalTime - oldCpuTime!!.totalTime
                val cpuLoad = ((1.0 - deltaIdleTime.toDouble() / deltaTotalTime) * 100.0)
                timestamp += stepValueSec

//                update Graph UI
                series.appendData(DataPoint(timestamp.toDouble(), cpuLoad), false, 20)
//                binding.graph.removeAllSeries()
                binding.graph.addSeries(series)
            }

            oldCpuTime = cpuTime
            handler.removeCallbacks(this)
            handler.postDelayed(this, updatePeriod)
        }
    }

    private fun getCpuTime(firstLine: String): CpuTime {
        //  "cpu  92108 51071 104297 2776324 9670 38 2742 0 0 0"
        val timesData = firstLine.drop(5)   // get rid of cpu plus 2 spaces

        val splitList = timesData.split(' ')

        var idleTime = 0L
        try {
            idleTime = splitList[3].toLong()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }

        var totalTime = 0L
        try {
            totalTime = splitList.sumOf {
                it.toLong()
            }
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }

        return CpuTime(idleTime, totalTime)
    }

    // stores time values for CPU
    data class CpuTime(val idleTime: Long, val totalTime: Long)



}