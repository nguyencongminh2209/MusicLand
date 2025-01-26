package com.example.musicland2308

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SongAdapter(var listener: View.OnClickListener? = null) :
    RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    var data: List<Song> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val item = LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false)
        val holder = SongViewHolder(item)
        return holder
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
//        holder.txt.text = data[position].title
//        holder.itemView.tag = data[position]
//        holder.itemView.setOnClickListener(listener)
        val song = data[position]

        // Gán tiêu đề bài hát
        holder.txt.text = song.title

        // Gán hình ảnh bài hát
        holder.image.setImageResource(R.drawable.minh_ai) // Ảnh mặc định nếu không có ảnh

        // Gán dữ liệu vào view và thiết lập sự kiện click
        holder.itemView.tag = song
        holder.itemView.setOnClickListener(listener)
    }

    override fun getItemCount(): Int {
        return data.size

    }

    inner class SongViewHolder(item: View) : RecyclerView.ViewHolder(item) {
        val txt = item.findViewById<TextView>(R.id.title)
        val image: ImageView = item.findViewById(R.id.imageTitle)
    }
}