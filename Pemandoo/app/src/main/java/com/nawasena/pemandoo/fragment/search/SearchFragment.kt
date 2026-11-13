package com.nawasena.pemandoo.fragment.search

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.nawasena.pemandoo.R
import com.nawasena.pemandoo.data.Landmark
import com.nawasena.pemandoo.data.NearbyLandmark
import com.nawasena.pemandoo.databinding.FragmentSearchBinding
import com.nawasena.pemandoo.util.AppPreferences
import com.nawasena.pemandoo.viewmodel.LandmarkViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: LandmarkViewModel
    private lateinit var adapter: NearbyLandmarkAdapter

    private var allLandmarks: List<Landmark> = emptyList()
    private var currentLocation: Location? = null
    private var selectedLandmarkId: String? = null
    private var locationTimedOut = false
    private var timeoutJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[LandmarkViewModel::class.java]

        adapter = NearbyLandmarkAdapter { landmark ->
            selectedLandmarkId = landmark.id
            refreshList()
        }
        binding.rvNearby.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvNearby.adapter = adapter

        binding.btnSearchStart.setOnClickListener { startTour() }

        viewModel.landmarks.observe(viewLifecycleOwner) { landmarks ->
            allLandmarks = landmarks
            refreshList()
        }

        viewModel.currentLocation.observe(viewLifecycleOwner) { location ->
            if (location != null) {
                currentLocation = location
                locationTimedOut = false
                timeoutJob?.cancel()
                refreshList()
            }
        }

        if (hasLocationPermission()) {
            viewModel.startLocationUpdates()
            startLocationTimeout()
        } else {
            // The permission dialog is shown by MapsActivity. Meanwhile show all
            // destinations without distance instead of an endless spinner.
            locationTimedOut = true
            refreshList()
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasLocationPermission() && currentLocation == null) {
            viewModel.startLocationUpdates()
            startLocationTimeout()
        }
    }

    private fun refreshList() {
        val binding = _binding ?: return
        val waitingForLocation = hasLocationPermission() &&
            currentLocation == null &&
            !locationTimedOut

        if (waitingForLocation) {
            binding.loadingContainer.visibility = View.VISIBLE
            binding.rvNearby.visibility = View.GONE
            binding.btnSearchStart.isEnabled = false
            return
        }

        val items = buildList {
            if (currentLocation != null) {
                addAll(
                    allLandmarks
                        .map { NearbyLandmark(it, it.distanceTo(currentLocation)) }
                        .sortedBy { it.distanceMeters ?: Float.MAX_VALUE }
                )
            } else {
                addAll(allLandmarks.map { NearbyLandmark(it) }.sortedBy { it.landmark.name })
            }
        }.take(MAX_RECOMMENDATIONS)

        if (items.isEmpty()) {
            binding.loadingContainer.visibility = View.VISIBLE
            binding.loadingBar.visibility = View.GONE
            binding.tvLoadingText.text = getString(R.string.search_empty)
            binding.rvNearby.visibility = View.GONE
            binding.btnSearchStart.isEnabled = false
            return
        }

        binding.loadingBar.visibility = View.VISIBLE
        binding.loadingContainer.visibility = View.GONE
        binding.rvNearby.visibility = View.VISIBLE
        binding.btnSearchStart.isEnabled = true

        if (selectedLandmarkId == null || items.none { it.landmark.id == selectedLandmarkId }) {
            selectedLandmarkId = items.first().landmark.id
        }

        adapter.submit(items, selectedLandmarkId)
    }

    private fun startTour() {
        val landmarkId = selectedLandmarkId ?: allLandmarks.firstOrNull()?.id
        if (landmarkId == null) {
            Toast.makeText(requireContext(), R.string.search_empty, Toast.LENGTH_SHORT).show()
            return
        }

        AppPreferences.setLastLandmarkId(requireContext(), landmarkId)
        findNavController().navigate(
            R.id.action_searchFragment_to_navigationFragment,
            bundleOf("landmarkId" to landmarkId)
        )
    }

    private fun startLocationTimeout() {
        timeoutJob?.cancel()
        timeoutJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(LOCATION_TIMEOUT_MS)
            if (currentLocation == null) {
                locationTimedOut = true
                refreshList()
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timeoutJob?.cancel()
        _binding = null
    }

    companion object {
        private const val MAX_RECOMMENDATIONS = 6
        private const val LOCATION_TIMEOUT_MS = 6_000L
    }
}
