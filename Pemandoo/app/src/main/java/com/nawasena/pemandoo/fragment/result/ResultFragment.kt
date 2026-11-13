package com.nawasena.pemandoo.fragment.result

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.nawasena.pemandoo.R
import com.nawasena.pemandoo.authentication.SettingActivity
import com.nawasena.pemandoo.data.Landmark
import com.nawasena.pemandoo.databinding.FragmentResultBinding
import com.nawasena.pemandoo.util.AppPreferences
import com.nawasena.pemandoo.util.LandmarkPhotoLoader
import com.nawasena.pemandoo.util.TtsSpeaker
import com.nawasena.pemandoo.viewmodel.LandmarkViewModel

class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: LandmarkViewModel
    private lateinit var speaker: TtsSpeaker

    private var landmarkId: String? = null
    private var narrationText: String = ""
    private var hasAutoPlayed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[LandmarkViewModel::class.java]
        landmarkId = arguments?.getString("landmarkId")
            ?: AppPreferences.getLastLandmarkId(requireContext())

        speaker = TtsSpeaker(
            context = requireContext(),
            onSpeakingChanged = { speaking ->
                _binding?.let { binding ->
                    updatePlayPauseButton(speaking)
                    if (speaking) {
                        binding.ivSoundWave.playAnimation()
                    } else {
                        binding.ivSoundWave.pauseAnimation()
                    }
                }
            },
            onError = { message ->
                if (isAdded) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        )
        updatePlayPauseButton(false)
        binding.ivSoundWave.pauseAnimation()

        setupButtons()

        val id = landmarkId
        if (id.isNullOrBlank()) {
            Toast.makeText(requireContext(), R.string.landmark_not_found, Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        viewModel.observeLandmark(id).observe(viewLifecycleOwner) { landmark ->
            if (landmark != null) {
                bindLandmark(landmark)
            } else {
                Toast.makeText(requireContext(), R.string.landmark_not_found, Toast.LENGTH_SHORT)
                    .show()
                findNavController().popBackStack()
            }
        }
    }

    private fun setupButtons() {
        binding.btnPause.setOnClickListener {
            if (speaker.isSpeaking) {
                speaker.stop()
            } else {
                speaker.speak(narrationText)
            }
        }

        binding.btnBackResult.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnMenu.setOnClickListener {
            startActivity(Intent(requireContext(), SettingActivity::class.java))
        }

        binding.cvPlayDescription.setOnClickListener {
            navigateToDetail()
        }
    }

    private fun bindLandmark(landmark: Landmark) {
        binding.tvLandmarkName.text = landmark.name
        binding.tvLandmarkResultName.text = landmark.name
        binding.tvLandmarkDescription.text = landmark.overview
        binding.tvLandmarkRange.text = "Tur audio · radius ${landmark.radiusMeters.toInt()} m"
        LandmarkPhotoLoader.load(binding.ivLandmarkImage, landmark.photo)

        narrationText = landmark.narrationText()
        AppPreferences.setLastLandmarkId(requireContext(), landmark.id)
        landmarkId = landmark.id

        if (!hasAutoPlayed) {
            hasAutoPlayed = true
            speaker.speak(narrationText)
        }
    }

    private fun updatePlayPauseButton(speaking: Boolean) {
        binding.btnPause.setImageResource(
            if (speaking) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun navigateToDetail() {
        val id = landmarkId ?: return
        findNavController().navigate(
            R.id.action_resultFragment_to_detailLandmarkFragment,
            bundleOf("landmarkId" to id)
        )
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
