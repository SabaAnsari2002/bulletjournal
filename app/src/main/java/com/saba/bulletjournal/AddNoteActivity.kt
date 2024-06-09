package com.saba.bulletjournal

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*
import kotlin.collections.HashMap

class AddNoteActivity : AppCompatActivity() {
    private lateinit var noteTitleEditText: EditText
    private lateinit var noteContentEditText: EditText
    private lateinit var saveNoteButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var selectImageButton: ImageButton  // Change the type to ImageButton
    private lateinit var imagesRecyclerView: RecyclerView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storageReference: StorageReference
    private lateinit var mAuth: FirebaseAuth
    private lateinit var noteDateEditText: EditText
    private val selectedImageUris = mutableListOf<Uri>()
    private lateinit var imagesAdapter: ImagesAdapter

    private var position: Int = -1
    private var note: Note? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)

        noteTitleEditText = findViewById(R.id.noteTitleEditText)
        noteContentEditText = findViewById(R.id.noteContentEditText)
        saveNoteButton = findViewById(R.id.saveNoteButton)
        backButton = findViewById(R.id.back_button)
        selectImageButton = findViewById(R.id.selectImageButton)  // Correct the type here
        imagesRecyclerView = findViewById(R.id.imagesRecyclerView)
        noteDateEditText = findViewById(R.id.noteDateEditText)
        noteDateEditText.setText(getDefaultDate())

        firestore = FirebaseFirestore.getInstance()
        storageReference = FirebaseStorage.getInstance().reference
        mAuth = FirebaseAuth.getInstance()


        backButton.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        imagesAdapter = ImagesAdapter(this, selectedImageUris) { position ->
            selectedImageUris.removeAt(position)
            imagesAdapter.notifyItemRemoved(position)
        }
        imagesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        imagesRecyclerView.adapter = imagesAdapter



        note = intent.getParcelableExtra("note")
        if (note != null) {
            noteTitleEditText.setText(note!!.title)
            noteContentEditText.setText(note!!.content)
            position = intent.getIntExtra("position", -1)

            // Load existing images
            val imageUrls = note?.imageUrl?.split(",")
            imageUrls?.forEach { imageUrl ->
                if (imageUrl.isNotEmpty()) {
                    selectedImageUris.add(Uri.parse(imageUrl))
                }
            }
            imagesAdapter.notifyDataSetChanged()
        }

        selectImageButton.setOnClickListener {
            selectImage()
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
                if (selectedImageUris.isNotEmpty()) {
                    uploadImagesAndSaveNote(title, content)
                } else {
                    addNewNoteToFirestore(title, content, null)
                }
            } else {
                // Update existing note
                if (selectedImageUris.isNotEmpty()) {
                    uploadImagesAndUpdateNote(note!!.documentId, title, content)
                } else {
                    updateNoteInFirestore(note!!.documentId, title, content, note!!.imageUrl)
                }
            }
        }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, REQUEST_CODE_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            if (data?.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val imageUri = data.clipData!!.getItemAt(i).uri
                    selectedImageUris.add(imageUri)
                }
            } else if (data?.data != null) {
                val imageUri = data.data!!
                selectedImageUris.add(imageUri)
            }
            imagesAdapter.notifyDataSetChanged()
        }
    }


    private fun saveUpdatedNoteToFirestore(documentId: String, noteMap: HashMap<String, String>, imagesUrls: List<String>) {
        firestore.collection("notes").document(documentId)
            .set(noteMap)
            .addOnSuccessListener {
                val updatedNote = Note(documentId = documentId, title = noteMap["title"] as String, content = noteMap["content"] as String, userId = noteMap["userId"] as String, date = noteMap["date"] as String, imageUrl = imagesUrls.joinToString(","))
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

    private fun uploadImagesAndSaveNote(title: String, content: String) {
        val userId = mAuth.currentUser?.uid ?: return
        val date = noteDateEditText.text.toString().trim()
        val noteMap = hashMapOf(
            "title" to title,
            "content" to content,
            "userId" to userId,
            "date" to date,
        )
        val imagesUrls = mutableListOf<String>()
        var uploadedImagesCount = 0
        selectedImageUris.forEach { uri ->
            val imageName = UUID.randomUUID().toString() // ایجاد نام تصادفی برای تصویر
            val imageReference = storageReference.child("images/$imageName.jpg")
            imageReference.putFile(uri)
                .addOnSuccessListener { taskSnapshot ->
                    imageReference.downloadUrl.addOnSuccessListener { imageUrl ->
                        imagesUrls.add(imageUrl.toString())
                        uploadedImagesCount++
                        if (uploadedImagesCount == selectedImageUris.size) {
                            noteMap["imageUrls"] = imagesUrls.joinToString(",")
                            firestore.collection("notes")
                                .add(noteMap)
                                .addOnSuccessListener { documentReference ->
                                    val note = Note(documentId = documentReference.id, title = title, content = content, userId = userId, date = date, imageUrl = imagesUrls.joinToString(","))
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
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error uploading image: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun uploadImagesAndUpdateNote(documentId: String, title: String, content: String) {
        val userId = mAuth.currentUser?.uid ?: return
        val date = noteDateEditText.text.toString().trim()
        val noteMap = hashMapOf(
            "title" to title,
            "content" to content,
            "userId" to userId,
            "date" to date,
        )
        val imagesUrls = mutableListOf<String>()
        var uploadedImagesCount = 0

        // اضافه کردن عکس‌های موجود قبلی به لیست آدرس‌ها
        note?.imageUrl?.split(",")?.let { imagesUrls.addAll(it) }

        selectedImageUris.forEach { uri ->
            if (uri.toString().startsWith("https://")) {
                // عکس‌های موجود قبلی که به Uri تبدیل شده‌اند، اضافه شوند
                uploadedImagesCount++
                imagesUrls.add(uri.toString())
                if (uploadedImagesCount == selectedImageUris.size) {
                    noteMap["imageUrls"] = imagesUrls.joinToString(",")
                    saveUpdatedNoteToFirestore(documentId, noteMap, imagesUrls)
                }
            } else {
                val imageName = UUID.randomUUID().toString() // ایجاد نام تصادفی برای تصویر
                val imageReference = storageReference.child("images/$imageName.jpg")
                imageReference.putFile(uri)
                    .addOnSuccessListener { taskSnapshot ->
                        imageReference.downloadUrl.addOnSuccessListener { imageUrl ->
                            imagesUrls.add(imageUrl.toString())
                            uploadedImagesCount++
                            if (uploadedImagesCount == selectedImageUris.size) {
                                noteMap["imageUrls"] = imagesUrls.joinToString(",")
                                saveUpdatedNoteToFirestore(documentId, noteMap, imagesUrls)
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Error uploading image: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun addNewNoteToFirestore(title: String, content: String, imageUrl: String?) {
        val userId = mAuth.currentUser?.uid ?: return
        val date = noteDateEditText.text.toString().trim()
        val noteMap = hashMapOf(
            "title" to title,
            "content" to content,
            "userId" to userId,
            "date" to date,
            "imageUrls" to imageUrl,
        )
        firestore.collection("notes")
            .add(noteMap)
            .addOnSuccessListener { documentReference ->
                val note = Note(documentId = documentReference.id, title = title, content = content, userId = userId, date = date, imageUrl = imageUrl ?: "")
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

    private fun updateNoteInFirestore(documentId: String, title: String, content: String, imageUrl: String?) {
        val userId = mAuth.currentUser?.uid ?: return
        val date = noteDateEditText.text.toString().trim()
        val noteMap = hashMapOf(
            "title" to title,
            "content" to content,
            "userId" to userId,
            "date" to date,
            "imageUrls" to imageUrl,
        )
        firestore.collection("notes").document(documentId)
            .set(noteMap)
            .addOnSuccessListener {
                val updatedNote = Note(documentId = documentId, title = title, content = content, userId = userId, date = date, imageUrl = imageUrl ?: "")
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

    companion object {
        private const val REQUEST_CODE_IMAGE_PICK = 1
    }
}