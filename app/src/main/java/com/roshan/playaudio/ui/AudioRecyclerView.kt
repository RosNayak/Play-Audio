package com.roshan.playaudio.ui

import android.content.Context
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.roshan.playaudio.R
import com.roshan.playaudio.entity.AudioEntity
import com.roshan.playaudio.listeners.AudioSelectedListener
import kotlinx.android.synthetic.main.list_view_layout.view.*

class AudioRecyclerView(
    private val context : Context,
    private val data : ArrayList<AudioEntity>,
    private val listener : AudioSelectedListener
) : RecyclerView.Adapter<AudioRecyclerView.ViewHolder>() {

    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        val audioName : TextView = itemView.tvAudioName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        return ViewHolder(inflater.inflate(R.layout.list_view_layout, parent, false))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val name = data[position].name
        val temp = name.split('.')
        holder.audioName.text = temp[0]

        holder.audioName.setOnClickListener {
//            val uri = data[position].uri
//            MediaPlayer().apply {
//                setDataSource(context, uri)
//                prepare()
//                start()
            listener.onAudioSelected(data[position])
        }
    }
}