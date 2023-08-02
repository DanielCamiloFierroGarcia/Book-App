package com.example.bookapp.activities

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.example.bookapp.databinding.ActivityPdfAddBinding
import com.example.bookapp.models.ModelCategory
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class PdfAddActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfAddBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var progressDialog: ProgressDialog
    //array to hole categories
    private lateinit var categoryArrayList: ArrayList<ModelCategory>
    //uri of picked pdf
    private var pdfUri: Uri? = null
    //TAG
    private val TAG = "PDF_ADD_TAG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        loadPdfCategories()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please Wait")
        progressDialog.setCanceledOnTouchOutside(false)

        //handle click, go back
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        //handle click, show category pick dialog
        binding.categoryTv.setOnClickListener {
            categoryPickDialog()
        }

        //hanlde click, pick pdf intent
        binding.attachPdfBtn.setOnClickListener {
            pdfPickIntent()
        }

        //handle click, start uploading pdf
        binding.submitBtn.setOnClickListener {
            /**
             * Step 1: Validate data -> in validateData method
             * Step 2: Upload pdf to FB storage -> in uploadPdfToStorage method
             * Step 3: Get url of uploaded pdf -> in uploadPdfToStorage method
             * Step 4: Upload pdf into firebase db -> in uploadPdfIntoToDb method **/
            validateData()
        }
    }
    private var title = ""
    private var description = ""
    private var category = ""
    private fun validateData() {
        Log.d(TAG, "validateData: validating data")
        //get data
        title = binding.titleEt.text.toString().trim()
        description = binding.descriptionEt.text.toString().trim()
        category = binding.categoryTv.text.toString().trim()
        //validate
        if(title.isEmpty() || description.isEmpty() || category.isEmpty()){
            Toast.makeText(this, "Enter all data", Toast.LENGTH_SHORT).show()
        }
        else if(pdfUri == null){
            Toast.makeText(this, "You must enter a File", Toast.LENGTH_SHORT).show()
        }
        else{
            //data validated, begin upload
            uploadPdfToStorage()
        }
    }

    private fun uploadPdfToStorage() {
        Log.d(TAG, "uploadPdfToStorage: uploading to storage")
        //show progress
        progressDialog.setMessage("Uploading PDF")
        progressDialog.show()
        //timestamp
        val timestamp = System.currentTimeMillis()
        //path of pdf in FB storage
        val filePathAndName = "Books/$timestamp"
        //storage ref
        val storageReference = FirebaseStorage.getInstance().getReference(filePathAndName)
        storageReference.putFile(pdfUri!!)
            .addOnSuccessListener {taskSnapshot ->
                Log.d(TAG, "uploadPdfToStorage: pdf uploaded getting url")
                //get url of pdf
                val uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);
                val uploadedPdfUrl = "${uriTask.result}"

                uploadPdfIntoToDb(uploadedPdfUrl, timestamp)
            }
            .addOnFailureListener{e->
                Log.d(TAG, "uploadPdfToStorage: failed to upload due to ${e.message}")
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to upload due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadPdfIntoToDb(uploadedPdfUrl: String, timestamp: Long) {
        Log.d(TAG, "uploadPdfIntoToDb: uploading to db")
        progressDialog.setMessage("Uploading pdf info")

        //uid of current user
        val uid = firebaseAuth.uid
        //setup data to upload
        val hashMap: HashMap<String, Any> = HashMap()
        hashMap["uid"] = "$uid"
        hashMap["id"] = "$timestamp"
        hashMap["title"] = title
        hashMap["description"] = description
        hashMap["categoryId"] = selectedCategoryId
        hashMap["url"] = uploadedPdfUrl
        hashMap["timestamp"] = timestamp
        hashMap["viewsCount"] = 0
        hashMap["downloadsCount"] = 0

        //db ref DB > Books >BookId > (Books info)
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child("$timestamp")
            .setValue(hashMap)
            .addOnSuccessListener {
                Log.d(TAG, "uploadPdfIntoToDb: uploaded to db")
                progressDialog.dismiss()
                Toast.makeText(this, "Uploaded", Toast.LENGTH_SHORT).show()
                pdfUri = null
            }
            .addOnFailureListener {e->
                Log.d(TAG, "uploadPdfIntoToDb: failed to upload due to ${e.message}")
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to upload due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadPdfCategories() {
        Log.d(TAG, "loadPdfCategories: Loading pdf categories")
        //init arraylist
        categoryArrayList = ArrayList()
        //db ref to load categories PDF > Categories
        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                //clear list before adding data
                categoryArrayList.clear()
                for(ds in snapshot.children){
                    //get data
                    val model = ds.getValue(ModelCategory::class.java)
                    //add to array
                    categoryArrayList.add(model!!)//better not to do this?
                    Log.d(TAG, "onDataChange: ${model.category}")
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private var selectedCategoryId = ""
    private var selectedCategoryTitle = ""

    private fun categoryPickDialog(){
        Log.d(TAG, "categoryPickDialog: Showing pdf category pick dialog")
        //get string aaray of categories from array
        val categoriesArray = arrayOfNulls<String>(categoryArrayList.size)
        for(i in categoryArrayList.indices){
            categoriesArray[i] = categoryArrayList[i].category
        }
        //alert dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pick Category")
            .setItems(categoriesArray){dialog, which->
                //handle item click
                //get clicked item
                val category = categoriesArray[which]
                selectedCategoryTitle = categoryArrayList[which].category
                selectedCategoryId = categoryArrayList[which].id
                //set category to textview
                binding.categoryTv.text = selectedCategoryTitle

                Log.d(TAG, "categoryPickDialog: Selected Category ID: $selectedCategoryId")
                Log.d(TAG, "categoryPickDialog: Selected Category Title: $selectedCategoryTitle")
            }.show()
    }

    private fun pdfPickIntent(){
        Log.d(TAG, "pdfPickIntent: starting pdf pick intent")

        val intent = Intent()
        intent.type = "application/pdf"
        intent.action = Intent.ACTION_GET_CONTENT
        pdfActivityResultLauncher.launch(intent)
    }
    val pdfActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult>{result->
            if(result.resultCode == RESULT_OK){
                Log.d(TAG, "PDF Picked")
                pdfUri = result.data?.data//if something doesnt work here replace ? with !!
            }
            else{
                Log.d(TAG, "PDF Pick Canceled")
                Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show()
            }
        }
    )
}