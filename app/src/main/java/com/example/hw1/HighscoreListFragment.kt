package com.example.hw1

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.model.LatLng

class HighscoreListFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_highscore_list, container, false)

        // Set title
        val title = view.findViewById<TextView>(R.id.high_score_title)
        title.text = "Top 10 High Scores"

        // Load highscores from SharedPreferences
        val sharedPreferences =
            requireContext().getSharedPreferences("Highscores", Context.MODE_PRIVATE)
        val highscores = mutableListOf<String>()
        val locations = mutableListOf<LatLng>()

        // Debug SharedPreferences contents
        val allEntries = sharedPreferences.all
        Log.d("HighscoreFragment", "SharedPreferences contents: $allEntries")

        for (i in 0 until 10) {
            val name = sharedPreferences.getString("name_$i", null)
            val score = sharedPreferences.getInt("score_$i", -1)
            val lat = sharedPreferences.getFloat("lat_$i", 0f)
            val lng = sharedPreferences.getFloat("lng_$i", 0f)
            if (name != null) { // Include all scores, including zero
                highscores.add("$name : $score")
                locations.add(LatLng(lat.toDouble(), lng.toDouble()))
            }
        }

        // Log loaded highscores
        Log.d("HighscoreFragment", "Loaded highscores: $highscores")
        Log.d("HighscoreFragment", "Loaded locations: $locations")

        // If no highscores, add a default message
        if (highscores.isEmpty()) {
            highscores.add("No highscores yet")
            locations.add(LatLng(0.0, 0.0)) // Default location for "No highscores yet"
        }

        // Set up the ListView with a custom adapter
        val listView = view.findViewById<ListView>(R.id.highscoreListView)
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            highscores
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).setTextColor(android.graphics.Color.BLACK)
                view.setBackgroundColor(android.graphics.Color.WHITE)
                return view
            }
        }
        listView.adapter = adapter

        // Handle ListView item clicks
        listView.setOnItemClickListener { _, _, position, _ ->
            if (position < locations.size && highscores[position] != "No highscores yet") {
                val location = locations[position]
                Log.d("HighscoreFragment", "Clicked highscore at position $position: $location")
                // Directly notify HighscoreActivity to update the map
                (activity as? HighscoreActivity)?.updateMapLocation(location)
            }
        }

        // Log adapter item count
        Log.d("HighscoreFragment", "Adapter item count: ${adapter.count}")

        return view
    }
}