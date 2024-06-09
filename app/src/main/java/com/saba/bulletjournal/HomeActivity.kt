package com.saba.bulletjournal

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity(), NotesAdapter.OnItemClickListener {

    private lateinit var logoutButton: ImageButton
    private lateinit var addNoteButton: ImageButton
    private lateinit var deleteNoteButton: ImageButton
    private lateinit var switchDarkMode: Switch
    private lateinit var mAuth: FirebaseAuth
    private lateinit var notesRecyclerView: RecyclerView
    private lateinit var notesAdapter: NotesAdapter
    private lateinit var notesList: MutableList<Note>
    private lateinit var firestore: FirebaseFirestore
    private var isSelectionMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        mAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        logoutButton = findViewById(R.id.logoutButton)
        addNoteButton = findViewById(R.id.addNoteButton)
        deleteNoteButton = findViewById(R.id.deleteNoteButton)
        switchDarkMode = findViewById(R.id.switch_dark_mode)
        notesRecyclerView = findViewById(R.id.notesRecyclerView)

        notesList = mutableListOf()
        notesAdapter = NotesAdapter(notesList, this)
        notesRecyclerView.adapter = notesAdapter
        notesRecyclerView.layoutManager = LinearLayoutManager(this)

        loadNotes()

        logoutButton.setOnClickListener {
            mAuth.signOut()

            val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
            sharedPreferences.edit().putBoolean("isLoggedIn", false).apply()

            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        addNoteButton.setOnClickListener {
            val intent = Intent(this, AddNoteActivity::class.java)
            startActivityForResult(intent, ADD_NOTE_REQUEST_CODE)
        }

        deleteNoteButton.setOnClickListener {
            if (notesAdapter.getSelectedNotes().isEmpty()) {
                Toast.makeText(this, "No notes selected", Toast.LENGTH_SHORT).show()
            } else {
                showDeleteConfirmationDialog()
            }
        }

        // Check current mode and set switch accordingly
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        switchDarkMode.isChecked = currentNightMode == Configuration.UI_MODE_NIGHT_YES

        // Set listener for switch to change theme
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val note = data?.getParcelableExtra<Note>("note")
            val position = data?.getIntExtra("position", -1)

            if (note != null) {
                if (requestCode == ADD_NOTE_REQUEST_CODE) {
                    notesList.add(note)
                } else if (requestCode == EDIT_NOTE_REQUEST_CODE && position != null && position >= 0) {
                    notesList[position] = note
                }
                notesAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun loadNotes() {
        val userId = mAuth.currentUser?.uid ?: return
        firestore.collection("notes")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                notesList.clear()
                for (document in documents) {
                    val note = document.toObject(Note::class.java).apply {
                        documentId = document.id
                    }
                    notesList.add(note)
                }
                notesList.sortByDescending { it.date }
                notesAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error loading notes: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onItemClick(position: Int) {
        if (isSelectionMode) {
            notesAdapter.toggleSelection(position)
        } else {
            val intent = Intent(this, AddNoteActivity::class.java)
            intent.putExtra("note", notesList[position])
            intent.putExtra("position", position)
            startActivityForResult(intent, EDIT_NOTE_REQUEST_CODE)
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setMessage("Are you sure you want to delete the selected notes?")
            .setPositiveButton("Delete") { dialog, which ->
                deleteSelectedNotes()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSelectedNotes() {
        val selectedNotes = notesAdapter.getSelectedNotes()
        val userId = mAuth.currentUser?.uid ?: return
        var deleteCount = 0

        for (note in selectedNotes) {
            firestore.collection("notes").document(note.documentId)
                .delete()
                .addOnSuccessListener {
                    notesList.remove(note)
                    deleteCount++
                    if (deleteCount == selectedNotes.size) {
                        notesAdapter.notifyDataSetChanged()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error deleting note: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }

        notesAdapter.clearSelection()
    }

    override fun onResume() {
        super.onResume()
        loadNotes()
    }

    companion object {
        private const val ADD_NOTE_REQUEST_CODE = 1
        private const val EDIT_NOTE_REQUEST_CODE = 2
    }
}
