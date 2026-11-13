package com.nawasena.pemandoo.fragment.navigation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.nawasena.pemandoo.GuideActivity
import com.nawasena.pemandoo.R
import com.nawasena.pemandoo.authentication.SettingActivity
import com.nawasena.pemandoo.data.Landmark
import com.nawasena.pemandoo.database.AddLandmarkActivity
import com.nawasena.pemandoo.databinding.FragmentNavigationBinding
import com.nawasena.pemandoo.util.AppPreferences
import com.nawasena.pemandoo.util.DistanceUtils
import com.nawasena.pemandoo.util.LandmarkPhotoLoader
import com.nawasena.pemandoo.viewmodel.LandmarkViewModel

class NavigationFragment : Fragment() {

    private var _binding: FragmentNavigationBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: LandmarkViewModel
    private var selectedLandmark: Landmark? = null
    private var selectedId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNavigationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[LandmarkViewModel::class.java]
        selectedId = arguments?.getString("landmarkId")
            ?: AppPreferences.getLastLandmarkId(requireContext())

        viewModel.landmarks.observe(viewLifecycleOwner) { landmarks ->
            val location = viewModel.currentLocation.value
            val target = landmarks.firstOrNull { it.id == selectedId }
                ?: location?.let { userLocation ->
                    landmarks.minByOrNull {
                        it.distanceTo(userLocation.latitude, userLocation.longitude)
                    }
                }
                ?: landmarks.firstOrNull()

            if (target != null) {
                selectedId = target.id
                selectedLandmark = target
                AppPreferences.setLastLandmarkId(requireContext(), target.id)
                bindLandmark(target)
            } else {
                selectedLandmark = null
                bindEmptyState()
            }
        }

        viewModel.currentLocation.observe(viewLifecycleOwner) {
            selectedLandmark?.let(::updateDistance)
        }

        binding.btnPlay.setOnClickListener {
            selectedLandmark?.let { landmark ->
                findNavController().navigate(
                    R.id.action_navigationFragment_to_resultFragment,
                    bundleOf("landmarkId" to landmark.id)
                )
            }
        }

        binding.cvPlayDescription.setOnClickListener {
            selectedLandmark?.let { landmark ->
                findNavController().navigate(
                    R.id.action_navigationFragment_to_detailFragment,
                    bundleOf("landmarkId" to landmark.id)
                )
            }
        }

        binding.btnPanduan.setOnClickListener {
            startActivity(Intent(requireContext(), GuideActivity::class.java))
        }

        binding.btnTambah.setOnClickListener {
            startActivity(Intent(requireContext(), AddLandmarkActivity::class.java))
        }

        binding.btnPengaturan.setOnClickListener {
            startActivity(Intent(requireContext(), SettingActivity::class.java))
        }
    }

    private fun bindLandmark(landmark: Landmark) {
        binding.tvLandmarkName.text = landmark.name
        binding.tvLandmarkDescription.text = landmark.overview
        LandmarkPhotoLoader.load(binding.ivLandmarkImage, landmark.photo)
        updateDistance(landmark)
    }

    private fun updateDistance(landmark: Landmark) {
        val distance = viewModel.currentLocation.value?.let { location ->
            landmark.distanceTo(location.latitude, location.longitude)
        }
        binding.tvLandmarkRange.text = buildString {
            append("Radius ${landmark.radiusMeters.toInt()} m")
            if (distance != null) {
                append(" · ")
                append(DistanceUtils.formatDistanceShort(distance))
            }
        }
    }

    private fun bindEmptyState() {
        binding.tvLandmarkName.text = getString(R.string.no_landmark_title)
        binding.tvLandmarkDescription.text = getString(R.string.no_landmark_description)
        binding.tvLandmarkRange.text = ""
        LandmarkPhotoLoader.load(binding.ivLandmarkImage, null)
        binding.btnPlay.isEnabled = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
