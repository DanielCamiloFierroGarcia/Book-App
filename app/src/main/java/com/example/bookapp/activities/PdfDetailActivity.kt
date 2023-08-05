package com.example.bookapp.activities

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.bookapp.Constants
import com.example.bookapp.MyApplication
import com.example.bookapp.R
import com.example.bookapp.adapters.AdapterComment
import com.example.bookapp.databinding.ActivityPdfDetailBinding
import com.example.bookapp.databinding.DialogCommentAddBinding
import com.example.bookapp.models.ModelComment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.FileOutputStream

class PdfDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfDetailBinding

    private var bookId = ""
    private var bookTitle = ""
    private var bookUrl = ""
    private var isInMyFavorite = false
    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var progressDialog: ProgressDialog
    //array to hold comments
    private lateinit var commentArrayList: ArrayList<ModelComment>
    //adapter to set to recycler
    private lateinit var adapterComment: AdapterComment
    private companion object{
        const val TAG = "BOOK_DETAILS_TAG"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookId = intent.getStringExtra("bookId").toString()//si no funciona quitar el to string y poner !!

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please Wait")
        progressDialog.setCanceledOnTouchOutside(false)

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()
        if(firebaseAuth.currentUser != null){
            checkIsFavorite()
        }

        //increment book view count, whenever this page starts
        MyApplication.incrementBookViewCount(bookId)
        loadBookDetails()
        showComments()

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
        //handle click, open pdf view activity
        binding.readBookBtn.setOnClickListener {
            val intent = Intent(this, PdfViewActivity::class.java)
            intent.putExtra("bookId", bookId)
            startActivity(intent)
        }
        //click, download
        binding.downloadsBookBtn.setOnClickListener {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                Log.d(TAG, "onCreate: Storage permisiion is granted")
            }
            else{
                Log.d(TAG, "onCreate: Storage permisiion not granted, now request it")
                requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        binding.favoriteBtn.setOnClickListener {
            //check if user is logged
            if(firebaseAuth.currentUser == null){
                Toast.makeText(this, "you are not logged in", Toast.LENGTH_SHORT).show()
            }
            else{
                if(isInMyFavorite){
                    MyApplication.removeFromFavorite(this, bookId)
                }
                else{
                    addToFavorite()
                }
            }
        }
        //show add comment dialog  click
        binding.addCommentBtn.setOnClickListener {
            //to add comment user must be logged in if not show message
            if(firebaseAuth.currentUser == null){
                Toast.makeText(this, "You are not logged in", Toast.LENGTH_SHORT).show()
            }
            else{
                addCommentDialog()
            }
        }
    }

    private fun showComments() {
        //innit array
        commentArrayList = ArrayList()
        //db path load comments
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId).child("Comments")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    commentArrayList.clear()
                    for(ds in snapshot.children){
                        //get data model
                        val model = ds.getValue(ModelComment::class.java)
                        //add to list
                        commentArrayList.add(model!!)
                    }
                    //setup adapter
                    adapterComment = AdapterComment(this@PdfDetailActivity, commentArrayList)
                    //set adapter to recycler
                    binding.commentsRv.adapter = adapterComment
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private var comment = ""
    private fun addCommentDialog() {
        //this binding is to set the dialog to the comment add view
        val commentAddBinding = DialogCommentAddBinding.inflate(LayoutInflater.from(this))

        val builder = AlertDialog.Builder(this, R.style.CustomDialog)
        builder.setView(commentAddBinding.root)

        val alertDialog = builder.create()
        alertDialog.show()

        commentAddBinding.backBtn.setOnClickListener {
            alertDialog.dismiss()
        }
        //add comment
        commentAddBinding.submitBtn.setOnClickListener {
            //get data
            comment = commentAddBinding.commentEt.text.toString().trim()
            //validate data
            if(comment.isEmpty()){
                Toast.makeText(this, "Enter comment...", Toast.LENGTH_SHORT).show()
            }
            else{
                alertDialog.dismiss()
                addComment()
            }
        }
    }

    private fun addComment() {
        progressDialog.setMessage("Adding comment")
        progressDialog.show()
        //timestamp for comment id
        val timestamp = "${System.currentTimeMillis()}"
        //setup data to add in db for comment
        val hashMap = HashMap<String, Any>()
        hashMap["id"] = "$timestamp"
        hashMap["bookId"] = "$bookId"
        hashMap["timestamp"] = "$timestamp"
        hashMap["comment"] = "$comment"
        hashMap["uid"] = "${firebaseAuth.uid}"
        //Db path to add data into it
        //Books > bookId > Comments > commentId > commentData
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId).child("Comments").child(timestamp)
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Comment added", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to add fue to ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private val requestStoragePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){isGranted:Boolean->
        if(isGranted){
            Log.d(TAG, "onCreate: Storage permisiion is granted")
            downloadBook()
        }
        else{
            Log.d(TAG, "onCreate: Storage permisiion denied")
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadBook(){
        progressDialog.setMessage("Dowloading")
        progressDialog.show()

        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl)
        storageReference.getBytes(Constants.MAX_BYTES_PDF)
            .addOnSuccessListener {bytes->
                Log.d(TAG, "downloadBook: Book Dowloaded")
                saveToDownloadFolder(bytes)
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Log.d(TAG, "downloadBook: Failed to dowload due to ${it.message}")
                Toast.makeText(this, "Failed to dowload due to ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveToDownloadFolder(bytes: ByteArray?) {
        val nameWithExtention = "${System.currentTimeMillis()}.pdf"
        try{
            val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsFolder.mkdirs()//create folder if not exists

            val filePath = downloadsFolder.path + "/" + nameWithExtention
            val out = FileOutputStream(filePath)
            out.write(bytes)
            out.close()
            Toast.makeText(this, "Saved to download folder", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
            incrementDownloadCount()
        }catch (e: Exception){
            progressDialog.dismiss()
            Log.d(TAG, "saveToDownloadFolder: Failed to download due to ${e.message}")
            Toast.makeText(this, "Failed to download due to ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun incrementDownloadCount() {
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    var downloadCount = "${snapshot.child("downloadsCount").value}"
                    if(downloadCount == "" || downloadCount == "null"){
                        downloadCount = "0"
                    }
                    //convert to long and increment
                    val newCount:Long = downloadCount.toLong()+1
                    Log.d(TAG, "onDataChange: New Download count")
                    //setup data to update to db
                    val hashMap: HashMap<String, Any> = HashMap()
                    hashMap["downloadsCount"] = newCount

                    val dbRef = FirebaseDatabase.getInstance().getReference("Books")
                    dbRef.child(bookId)
                        .updateChildren(hashMap)
                        .addOnSuccessListener {
                            Log.d(TAG, "onDataChange: Downloads incremented")
                        }
                        .addOnFailureListener {
                            Log.d(TAG, "onDataChange: Failed to increment due to ${it.message}")
                        }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun loadBookDetails() {
        //books > bookId > Details
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get data
                    val categoryId = "${snapshot.child("categoryId").value}"
                    val description = "${snapshot.child("description").value}"
                    val downloadsCount = "${snapshot.child("downloadsCount").value}"
                    val timestamp = "${snapshot.child("timestamp").value}"
                    bookTitle = "${snapshot.child("title").value}"
                    val uid = "${snapshot.child("uid").value}"
                    bookUrl = "${snapshot.child("url").value}"
                    val viewsCount = "${snapshot.child("viewsCount").value}"

                    //format date
                    val date = MyApplication.formatTimeStamp(timestamp.toLong())

                    MyApplication.loadCategory(categoryId, binding.categoryTv)

                    //load pdf thumbanil pages count
                    MyApplication.loadPdfFromUrlSinglePage(
                        "$bookUrl",
                        "$bookTitle",
                        binding.pdfView,
                        binding.progressBar,
                        binding.pagesTv
                    )
                    MyApplication.loadPdfSize("$bookUrl", "$bookTitle", binding.sizeTv)

                    //set data
                    binding.titleTv.text = title
                    binding.descriptionTv.text = description
                    binding.viewsTv.text = viewsCount
                    binding.downloadsTv.text = downloadsCount
                    binding.dateTv.text = date

                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun checkIsFavorite(){
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    isInMyFavorite = snapshot.exists()
                    if(isInMyFavorite){
                        binding.favoriteBtn.setCompoundDrawablesWithIntrinsicBounds(0,
                            R.drawable.ic_favorite_filled_white, 0, 0)
                        binding.favoriteBtn.text = "Remove Favorite"
                    }
                    else{
                        binding.favoriteBtn.setCompoundDrawablesWithIntrinsicBounds(0,
                            R.drawable.ic_favorite_border_white, 0, 0)
                        binding.favoriteBtn.text = "Add Favorite"
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun addToFavorite(){
        Log.d(TAG, "addToFavorite: Adding to favs")
        val timestamp = System.currentTimeMillis()
        val hashMap = HashMap<String, Any>()
        hashMap["bookId"] = bookId
        hashMap["timestamp"] = timestamp
        //save to db
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
            .setValue(hashMap)
            .addOnSuccessListener {
                Log.d(TAG, "addToFavorite: Added to fav")
                Toast.makeText(this, "Added to fav", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Log.d(TAG, "addToFavorite: Failed to add due to ${it.message}")
                Toast.makeText(this, "Failed to add due to ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}