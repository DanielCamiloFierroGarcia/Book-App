package com.example.bookapp.activities

import android.app.AlertDialog
import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.bookapp.databinding.ActivityPdfEditBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PdfEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfEditBinding

    private companion object{
        private const val TAG = "PDF_EDIT_TAG"
    }
    //book id from intent in AdapterPdfAdmin
    private var bookId = ""

    private lateinit var progressDialog: ProgressDialog
    //arraylist to hold category titles
    private lateinit var categoryTitleArrayList: ArrayList<String>
    //arraylist to hold category ids
    private lateinit var categoryIdArrayList: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookId = intent.getStringExtra("bookId")!!

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please Wait")
        progressDialog.setCanceledOnTouchOutside(false)

        loadCategories()
        loadBookInfo()

        binding.backbtn.setOnClickListener {
            onBackPressed()
        }
        //handle click pick category
        binding.categoryTv.setOnClickListener {
            categoryDialog()
        }
        //handle click beging update
        binding.submitBtn.setOnClickListener {
            validateData()
        }
    }

    private fun loadBookInfo() {
        Log.d(TAG, "loadBookInfo: Loading book info")
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get book info
                    selectedCategoryId = snapshot.child("categoryId").value.toString()
                    val description = snapshot.child("description").value.toString()
                    val title = snapshot.child("title").value.toString()

                    //set to views
                    binding.titleEt.setText(title)
                    binding.descriptionEt.setText(description)
                    //load book info using categoryId
                    Log.d(TAG, "onDataChange: Loading books category info")
                    val refBookCategory = FirebaseDatabase.getInstance().getReference("Categories")
                    refBookCategory.child(selectedCategoryId)
                        .addListenerForSingleValueEvent(object : ValueEventListener{
                            override fun onDataChange(snapshot: DataSnapshot) {
                                //get category
                                val category = snapshot.child("category").value
                                binding.categoryTv.text = category.toString()
                            }

                            override fun onCancelled(error: DatabaseError) {

                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private var title = ""
    private var description = ""
    private var selectedCategoryId = ""
    private var selectedCategoryTitle = ""

    private fun validateData() {
        title = binding.titleEt.text.toString().trim()
        description = binding.descriptionEt.text.toString().trim()
        //validate data
        if(title.isEmpty() || description.isEmpty() || selectedCategoryId.isEmpty()){
            Toast.makeText(this, "Enter information please", Toast.LENGTH_SHORT).show()
        }
        else{
            updatePdf()
        }
    }

    private fun updatePdf() {
        Log.d(TAG, "updatePdf: Starting updating pdf")
        progressDialog.setMessage("Updating book info")
        progressDialog.show()
        //setup data to update to db
        val hashMap = HashMap<String, Any>()
        hashMap["title"] = "$title"
        hashMap["description"] = "$description"
        hashMap["categoryId"] = "$selectedCategoryId"
        //start updating
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .updateChildren(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Log.d(TAG, "updatePdf: Update succeded")
                Toast.makeText(this, "Info updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Log.d(TAG, "updatePdf: Failed to update due to ${it.message}")
                progressDialog.dismiss()
                Toast.makeText(this, "Failde to update due to ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun categoryDialog() {
        //show dialog to pick category of pdf we already got categorues

        //make string array from arraylist of string
        val categoriesArray = arrayOfNulls<String>(categoryTitleArrayList.size)
        for(i in categoryTitleArrayList.indices){
            categoriesArray[i] = categoryTitleArrayList[i]
        }
        //alert dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose category")
            .setItems(categoriesArray){dialog, position ->
                //handle click save clicked category id and title
                selectedCategoryId = categoryIdArrayList[position]
                selectedCategoryTitle = categoryTitleArrayList[position]
                //set to textV
                binding.categoryTv.text = selectedCategoryTitle
            }.show()
    }

    private fun loadCategories() {
        Log.d(TAG, "loadCategories: loading categories")

        categoryTitleArrayList = ArrayList()
        categoryIdArrayList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                //clear list before using
                categoryIdArrayList.clear()
                categoryTitleArrayList.clear()

                for(ds in snapshot.children){
                    val id = "${ds.child("id").value}"
                    val category = "${ds.child("category").value}"

                    categoryIdArrayList.add(id)
                    categoryTitleArrayList.add(category)
                    Log.d(TAG, "onDataChange: Category Id $id")
                    Log.d(TAG, "onDataChange: Category $category")
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
}