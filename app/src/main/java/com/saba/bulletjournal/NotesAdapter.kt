
package com.saba.bulletjournal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class NotesAdapter(
    private val notes: List<Note>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    private val selectedNotes = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(itemView)
    }





    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.titleTextView.text = note.title
        holder.contentTextView.text = note.content
        holder.checkBox.isChecked = selectedNotes.contains(position)
        holder.dateTextView.text = note.date

        if (!note.imageUrl.isNullOrEmpty()) {
            holder.imageView.visibility = View.VISIBLE
            Picasso.get().load(note.imageUrl!!.split(",").first()).into(holder.imageView)  // Load the first image as a thumbnail
        } else {
            holder.imageView.visibility = View.GONE
        }
    }


    override fun getItemCount() = notes.size

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val titleTextView: TextView = itemView.findViewById(R.id.noteTitleTextView)
        val contentTextView: TextView = itemView.findViewById(R.id.noteContentTextView)
        val checkBox: CheckBox = itemView.findViewById(R.id.noteCheckBox)
        val dateTextView: TextView = itemView.findViewById(R.id.noteDateTextView)
        val imageView: ImageView = itemView.findViewById(R.id.noteImageView)  // Add this line

        init {
            itemView.setOnClickListener(this)
            checkBox.setOnClickListener {
                toggleSelection(adapterPosition)
            }
        }

        override fun onClick(v: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(position)
            }
        }
    }

    fun toggleSelection(position: Int) {
        if (selectedNotes.contains(position)) {
            selectedNotes.remove(position)
        } else {
            selectedNotes.add(position)
        }
        notifyItemChanged(position)
    }

    fun getSelectedNotes(): List<Note> {
        return selectedNotes.map { notes[it] }
    }

    fun clearSelection() {
        selectedNotes.clear()
        notifyDataSetChanged()
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }
}
