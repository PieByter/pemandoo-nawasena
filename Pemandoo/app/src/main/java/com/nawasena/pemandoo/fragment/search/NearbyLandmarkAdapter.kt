package com.nawasena.pemandoo.fragment.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nawasena.pemandoo.R
import com.nawasena.pemandoo.data.Landmark
import com.nawasena.pemandoo.data.NearbyLandmark
import com.nawasena.pemandoo.databinding.ItemNearbyLandmarkBinding
import com.nawasena.pemandoo.util.DistanceUtils
import com.nawasena.pemandoo.util.LandmarkPhotoLoader

class NearbyLandmarkAdapter(
    private val onClick: (Landmark) -> Unit
) : ListAdapter<NearbyLandmark, NearbyLandmarkAdapter.LandmarkViewHolder>(DIFF_CALLBACK) {

    private var selectedId: String? = null

    fun submit(items: List<NearbyLandmark>, selectedLandmarkId: String?) {
        selectedId = selectedLandmarkId
        submitList(items)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LandmarkViewHolder {
        val binding = ItemNearbyLandmarkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LandmarkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LandmarkViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LandmarkViewHolder(
        private val binding: ItemNearbyLandmarkBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NearbyLandmark) {
            val landmark = item.landmark
            binding.tvLandmarkName.text = landmark.name
            binding.tvLandmarkDistance.text = DistanceUtils.formatDistanceShort(item.distanceMeters)
            LandmarkPhotoLoader.load(binding.ivLandmarkPhoto, landmark.photo)

            val selected = landmark.id == selectedId
            binding.cardLandmark.strokeWidth = if (selected) {
                (2 * binding.root.resources.displayMetrics.density).toInt()
            } else {
                0
            }
            binding.cardLandmark.strokeColor = ContextCompat.getColor(
                binding.root.context,
                R.color.primary_green
            )

            binding.root.setOnClickListener { onClick(landmark) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NearbyLandmark>() {
            override fun areItemsTheSame(oldItem: NearbyLandmark, newItem: NearbyLandmark): Boolean {
                return oldItem.landmark.id == newItem.landmark.id
            }

            override fun areContentsTheSame(oldItem: NearbyLandmark, newItem: NearbyLandmark): Boolean {
                return oldItem == newItem
            }
        }
    }
}
