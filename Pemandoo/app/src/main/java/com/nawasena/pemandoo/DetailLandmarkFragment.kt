package com.nawasena.pemandoo

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.nawasena.pemandoo.data.Landmark
import com.nawasena.pemandoo.database.AddLandmarkActivity
import com.nawasena.pemandoo.databinding.FragmentDetailLandmarkBinding
import com.nawasena.pemandoo.util.AppPreferences
import com.nawasena.pemandoo.util.DistanceUtils
import com.nawasena.pemandoo.util.LandmarkPhotoLoader
import com.nawasena.pemandoo.util.TtsSpeaker
import com.nawasena.pemandoo.viewmodel.LandmarkViewModel

class DetailLandmarkFragment : Fragment() {

    private var _binding: FragmentDetailLandmarkBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: LandmarkViewModel
    private lateinit var speaker: TtsSpeaker

    private var landmark: Landmark? = null
    private var narrationText: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailLandmarkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[LandmarkViewModel::class.java]
        speaker = TtsSpeaker(
            context = requireContext(),
            onSpeakingChanged = { speaking ->
                _binding?.btnPlaySound?.setImageResource(
                    if (speaking) R.drawable.ic_pause else R.drawable.ic_play
                )
            },
            onError = { message ->
                if (isAdded) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        )

        val landmarkId = arguments?.getString("landmarkId")
            ?: AppPreferences.getLastLandmarkId(requireContext())

        if (landmarkId.isNullOrBlank()) {
            Toast.makeText(requireContext(), R.string.landmark_not_found, Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        viewModel.observeLandmark(landmarkId).observe(viewLifecycleOwner) { loaded ->
            if (loaded != null) {
                landmark = loaded
                bindLandmark(loaded)
            } else {
                Toast.makeText(requireContext(), R.string.landmark_not_found, Toast.LENGTH_SHORT)
                    .show()
                findNavController().popBackStack()
            }
        }

        viewModel.currentLocation.observe(viewLifecycleOwner) {
            landmark?.let(::updateLocationText)
        }

        binding.btnBackDetail.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnEditDetail.setOnClickListener {
            landmark?.let { editable ->
                startActivity(
                    Intent(requireContext(), AddLandmarkActivity::class.java)
                        .putExtra(AddLandmarkActivity.EXTRA_LANDMARK_ID, editable.id)
                )
            }
        }

        binding.btnPlaySound.setOnClickListener {
            if (speaker.isSpeaking) {
                speaker.stop()
            } else {
                speaker.speak(narrationText)
            }
        }
    }

    private fun bindLandmark(landmark: Landmark) {
        binding.tvDetailName.text = landmark.name
        binding.tvDetailDescription.text = buildDetailText(landmark)
        LandmarkPhotoLoader.load(binding.ivDetailPhoto, landmark.photo)
        binding.btnEditDetail.visibility = if (landmark.isUserCreated) View.VISIBLE else View.GONE
        narrationText = landmark.narrationText()
        updateLocationText(landmark)
    }

    private fun updateLocationText(landmark: Landmark) {
        val distance = viewModel.currentLocation.value?.let {
            landmark.distanceTo(it.latitude, it.longitude)
        }
        binding.tvDetailRadius.text = buildString {
            append("Radius ${landmark.radiusMeters.toInt()} m")
            if (distance != null) {
                append(" · ")
                append(DistanceUtils.formatDistance(distance))
            }
        }
    }

    private fun buildDetailText(landmark: Landmark): String = buildString {
        append(landmark.overview.trim())

        if (landmark.architecturalFeatures.isNotBlank()) {
            append("\n\nArsitektur\n")
            append(landmark.architecturalFeatures.trim())
        }
        if (landmark.interestingFacts.isNotBlank()) {
            append("\n\nFakta menarik\n")
            append(landmark.interestingFacts.trim())
        }
        if (landmark.historicalSignificance.isNotBlank()) {
            append("\n\nNilai sejarah\n")
            append(landmark.historicalSignificance.trim())
        }
        if (landmark.hours.isNotBlank() || landmark.entryFee.isNotBlank()) {
            append("\n\nInformasi kunjungan")
            if (landmark.hours.isNotBlank()) {
                append("\nJam buka: ")
                append(landmark.hours.trim())
            }
            if (landmark.entryFee.isNotBlank()) {
                append("\nTiket: ")
                append(landmark.entryFee.trim())
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (::speaker.isInitialized) {
            speaker.stop()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::speaker.isInitialized) {
            speaker.shutdown()
        }
        _binding = null
    }
}
