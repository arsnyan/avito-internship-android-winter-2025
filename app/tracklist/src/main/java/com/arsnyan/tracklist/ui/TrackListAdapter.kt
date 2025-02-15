package com.arsnyan.tracklist.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.arsnyan.tracklist.R
import com.arsnyan.tracklist.databinding.AdapterTrackBinding
import com.arsnyan.tracklist.network.model.Track

class TrackListAdapter(private val onItemClicked: (Track) -> Unit) :
    ListAdapter<Track, TrackListAdapter.TrackViewHolder>(TrackDiffCallback()) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TrackViewHolder = TrackViewHolder(
        AdapterTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(
        holder: TrackViewHolder,
        position: Int
    ) {
        val track = getItem(position)
        holder.bind(track)
        holder.itemView.setOnClickListener {
            onItemClicked(track)
            Log.d("TrackListAdapter", "Track clicked: ${track.title}")
        }
    }

    inner class TrackViewHolder(private val binding: AdapterTrackBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(track: Track) {
            binding.trackTitle.text = track.title
            binding.trackArtist.text = track.artist.name
            binding.trackCover.load(track.album.coverUrl) {
                placeholder(R.drawable.cover_placeholder)
                error(R.drawable.cover_placeholder)
                crossfade(true)
            }
        }
    }

    class TrackDiffCallback : DiffUtil.ItemCallback<Track>() {
        override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean {
            return oldItem == newItem
        }
    }
}