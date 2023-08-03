package com.example.bookapp.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.bumptech.glide.Glide
import com.example.bookapp.MyApplication
import com.example.bookapp.R
import com.example.bookapp.adapters.AdapterPdfFavorite
import com.example.bookapp.databinding.ActivityProfileBinding
import com.example.bookapp.models.ModelPdf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding

    private lateinit var firebaseAuth: FirebaseAuth
    //arraylist to hold books
    private lateinit var booksArrayList: ArrayList<ModelPdf>
    private lateinit var adapterPdfFavorite: AdapterPdfFavorite
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        loadUserInfo()
        loadFavoriteBooks()

        //go back
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
        //open edit profile
        binding.profileEditBtn.setOnClickListener {
            startActivity(Intent(this, ProfileEditActivity::class.java))
        }
    }

    private fun loadUserInfo() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get user info
                    val email = "${snapshot.child("email").value}"
                    val name = "${snapshot.child("name").value}"
                    val profileImage = "${snapshot.child("profileImage").value}"
                    val timestamp = "${snapshot.child("timestamp").value}"
                    val uid = "${snapshot.child("uid").value}"
                    val userType = "${snapshot.child("userType").value}"

                    val formattedDate = MyApplication.formatTimeStamp(timestamp.toLong())

                    //set Data
                    binding.nameTv.text = name
                    binding.emailTv.text = email
                    binding.memberDateTv.text = formattedDate
                    binding.accountTypeTv.text = userType
                    //set Image
                    try{
                        Glide.with(this@ProfileActivity)
                            .load(profileImage)
                            .placeholder(R.drawable.ic_person_gray)
                            .into(binding.profileIv)
                    }catch (e: Exception){

                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun loadFavoriteBooks(){
        //init arraylist
        booksArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //clear array befor use
                    booksArrayList.clear()
                    for(ds in snapshot.children){
                        //get only id of books, other info is loaded in adapter class
                        val bookId = "${ds.child("bookId").value}"
                        //set model
                        val modelPdf = ModelPdf()
                        modelPdf.id = bookId
                        //add model to list
                        booksArrayList.add(modelPdf)
                    }
                    //set number of fav books
                    binding.favoriteBookCountTv.text = "${booksArrayList.size}"
                    //setup adapter
                    adapterPdfFavorite = AdapterPdfFavorite(this@ProfileActivity, booksArrayList)
                    //set adapter to recyclerview
                    binding.favoriteRv.adapter = adapterPdfFavorite
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
}