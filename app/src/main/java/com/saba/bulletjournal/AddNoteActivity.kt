package com.saba.bulletjournal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class AddNoteActivity : AppCompatActivity() {

    private lateinit var noteTitleEditText: EditText
    private lateinit var noteContentEditText: EditText
    private lateinit var saveNoteButton: Button
    private lateinit var firestore: FirebaseFirestore
    private lateinit var mAuth: FirebaseAuth
    private lateinit var noteDateEditText: EditText

    private var position: Int = -1
    private var note: Note? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)

        noteTitleEditText = findViewById(R.id.noteTitleEditText)
        noteContentEditText = findViewById(R.id.noteContentEditText)
        saveNoteButton = findViewById(R.id.saveNoteButton)
        noteDateEditText = findViewById(R.id.noteDateEditText)
        noteDateEditText.setText(getDefaultDate())

        firestore = FirebaseFirestore.getInstance()
        mAuth = FirebaseAuth.getInstance()

        note = intent.getParcelableExtra("note")
        if (note != null) {
            noteTitleEditText.setText(note!!.title)
            noteContentEditText.setText(note!!.content)
            position = intent.getIntExtra("position", -1)
        }

        saveNoteButton.setOnClickListener {
            val title = noteTitleEditText.text.toString().trim()
            val content = noteContentEditText.text.toString().trim()

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "Please enter all the details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (note == null) {
                // Add new note
                addNewNoteToFirestore(title, content)
            } else {
                // Update existing note
                updateNoteInFirestore(note!!.documentId, title, content)
            }
        }
    }

    private fun addNewNoteToFirestore(title: String, content: String) {
        val userId = mAuth.currentUser?.uid ?: return
        val date = noteDateEditText.text.toString().trim()

        val noteMap = hashMapOf(
            "title" to title,
            "content" to content,
            "userId" to userId,
            "date" to date // اضافه کردن تاریخ به noteMap

        )

        firestore.collection("notes")
            .add(noteMap)
            .addOnSuccessListener { documentReference ->
                val note = Note(documentId = documentReference.id, title = title, content = content, userId = userId)
                val resultIntent = Intent().apply {
                    putExtra("note", note)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error adding note: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateNoteInFirestore(documentId: String, title: String, content: String) {
        val userId = mAuth.currentUser?.uid ?: return
        val date = noteDateEditText.text.toString().trim()

        val noteMap = hashMapOf(
            "title" to title,
            "content" to content,
            "userId" to userId,
            "date" to date // اضافه کردن تاریخ به noteMap

        )

        firestore.collection("notes").document(documentId)
            .set(noteMap)
            .addOnSuccessListener {
                val updatedNote = Note(documentId = documentId, title = title, content = content, userId = userId)
                val resultIntent = Intent().apply {
                    putExtra("note", updatedNote)
                    putExtra("position", position)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error updating note: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getDefaultDate(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // ماه‌ها از 0 شروع می‌شوند
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }



}

