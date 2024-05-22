package com.saba.bulletjournal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var logoutButton: Button
    private lateinit var addNoteButton: Button
    private lateinit var mAuth: FirebaseAuth
    private lateinit var notesRecyclerView: RecyclerView
    private lateinit var notesAdapter: NotesAdapter
    private lateinit var notesList: MutableList<Note>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        mAuth = FirebaseAuth.getInstance()
        logoutButton = findViewById(R.id.logoutButton)
        addNoteButton = findViewById(R.id.addNoteButton)
        notesRecyclerView = findViewById(R.id.notesRecyclerView)

        notesList = mutableListOf()
        notesAdapter = NotesAdapter(notesList)
        notesRecyclerView.adapter = notesAdapter
        notesRecyclerView.layoutManager = LinearLayoutManager(this)

        // Load notes from a local or remote database (this example uses dummy data)
        loadNotes()

        logoutButton.setOnClickListener {
            mAuth.signOut()

            // Clear login state
            val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
            sharedPreferences.edit().putBoolean("isLoggedIn", false).apply()

            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        addNoteButton.setOnClickListener {
            startActivity(Intent(this, AddNoteActivity::class.java))
        }
    }

    private fun loadNotes() {
        // Example of adding dummy data, replace this with actual data loading logic
        notesList.add(Note("Sample Note 1", "This is the content of note 1"))
        notesList.add(Note("Sample Note 2", "This is the content of note 2"))
        notesAdapter.notifyDataSetChanged()
    }
}
