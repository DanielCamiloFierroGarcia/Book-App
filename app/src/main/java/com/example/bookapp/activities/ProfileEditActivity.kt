package com.example.bookapp.activities

import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.example.bookapp.MyApplication
import com.example.bookapp.R
import com.example.bookapp.databinding.ActivityProfileEditBinding
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class ProfileEditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileEditBinding

    private lateinit var firebaseAuth: FirebaseAuth
    //image uri (which we pick)
    private var imageUri: Uri? = null

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please Wait")
        progressDialog.setCanceledOnTouchOutside(false)

        firebaseAuth = FirebaseAuth.getInstance()
        loadUserInfo()

        //back
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
        //pick image from camera/gallery
        binding.profileIv.setOnClickListener {
            showImageAttachMenu()
        }

        //update profile
        binding.updateBtn.setOnClickListener {
            validateData()
        }
    }
    private var name= ""

    private fun validateData() {
        //get data
        name = binding.nameEt.text.toString().trim()
        //validate
        if(name.isEmpty()){
            //name not entered
            Toast.makeText(this, "Enter Name", Toast.LENGTH_SHORT).show()
        }
        else{
            //name is entered
            if(imageUri == null){
                //need to update withput image
                updateProfile("")
            }
            else{
                //need to update with image
                uploadImage()
            }
        }
    }

    private fun uploadImage() {
        progressDialog.setMessage("Uploading profile image")
        progressDialog.show()
        //image path and name, use uid to replace previous
        val filePathAndName = "ProfileImage/"+firebaseAuth.uid
        //storage ref
        val storage = FirebaseStorage.getInstance().getReference(filePathAndName)
        storage.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot->
                //image uploaded, get url of uploaded image
                val uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                while(!uriTask.isSuccessful);
                val uploadedImageUrl = "${uriTask.result}"

                updateProfile(uploadedImageUrl)
            }
            .addOnFailureListener{
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to upload image due to ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateProfile(uploadedImageUrl: String) {
        progressDialog.setMessage("Updating Profile")
        //setup infp to db
        val hashMap: HashMap<String, Any> = HashMap()
        hashMap["name"] = "$name"
        if(imageUri != null){
            hashMap["profileImage"] = uploadedImageUrl
        }
        //update to db
        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child(firebaseAuth.uid!!)
            .updateChildren(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener{
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to update db due to ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserInfo() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get user info
                    val email = "${snapshot.child("email").value}"
                    val name = "${snapshot.child("name").value}"
                    val profileImage = "${snapshot.child("profileImage").value}"
                    val timestamp = "${snapshot.child("timestamp").value}"

                    //set Data
                    binding.nameEt.setText(name)
                    //set Image
                    try{
                        Glide.with(this@ProfileEditActivity)
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

    private fun showImageAttachMenu(){
        //show popup menu with options camera, gallery
        val popupMenu = PopupMenu(this, binding.profileIv)
        popupMenu.menu.add(Menu.NONE, 0, 0, "Camera")
        popupMenu.menu.add(Menu.NONE, 1, 1, "Gallery")
        popupMenu.show()

        popupMenu.setOnMenuItemClickListener {
            //get id of clicked item
            val id = it.itemId
            if(id == 0){
                //camera clicked
                pickImageCamera()
            }
            else if(id==1){
                //galllery
                pickImageGallery()
            }
            true
        }
    }

    private fun pickImageCamera() {
        //intent to pick image from camera
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Temp_Title")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp_Description")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

    private fun pickImageGallery() {
        //intent to pick image from gallery
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }
    //used to handle result of camera intent (new way in replacement of startActivityfor results
    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult> { result ->
            //get uri of image
            if(result.resultCode == Activity.RESULT_OK){
                val data = result.data
                //set to imageview
                binding.profileIv.setImageURI(imageUri)
            }
            else{
                //cancelled
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    )

    //used to handle result of gallery intent (new way in replacement of startActivityfor results)
    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult> { result ->
            //get uri of image
            if(result.resultCode == Activity.RESULT_OK){
                val data = result.data
                imageUri = data!!.data
                //set to imageview
                binding.profileIv.setImageURI(imageUri)
            }
            else{
                //cancelled
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    )

}