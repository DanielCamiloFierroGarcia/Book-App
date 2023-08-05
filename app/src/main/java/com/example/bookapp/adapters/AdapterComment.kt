package com.example.bookapp.adapters

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.bumptech.glide.Glide
import com.example.bookapp.MyApplication
import com.example.bookapp.R
import com.example.bookapp.databinding.RowCommentBinding
import com.example.bookapp.models.ModelComment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdapterComment : RecyclerView.Adapter<AdapterComment.HolderComment> {
    val context: Context
    //array to hold comments
    private val commentArrayList: ArrayList<ModelComment>

    //view binding row_comment.xml =>RowCommentBinding
    private lateinit var binding: RowCommentBinding
    private lateinit var firebaseAuth: FirebaseAuth

    //constructor
    constructor(context: Context, commentArrayList: ArrayList<ModelComment>) {
        this.context = context
        this.commentArrayList = commentArrayList
        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderComment {
        //bind/inflate row_comment.xml
        binding = RowCommentBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderComment(binding.root)
    }

    override fun onBindViewHolder(holder: HolderComment, position: Int) {
        //get, set data. Handle click
        //get
        val model = commentArrayList[position]
        val id = model.id
        val bookId = model.bookId
        val comment = model.comment
        val uid = model.uid
        val timestamp = model.timestamp

        val date = MyApplication.formatTimeStamp(timestamp.toLong())

        //set data
        holder.dateTv.text = date
        holder.commentTv.text = comment

        loadUserDetails(model, holder)

        //click show dialog to delete omment
        holder.itemView.setOnClickListener {
            //requirement to delete comment
            //1) user must be logged in
            //2) uid in comment (to delete) must be same as uid of current user
            if(firebaseAuth.currentUser != null && firebaseAuth.uid == uid){
                deleteCommentDialog(model, holder)
            }
        }
    }

    private fun deleteCommentDialog(model: ModelComment, holder: AdapterComment.HolderComment) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Delete comment")
            .setMessage("Are you sure you want to delete the comment?")
            .setPositiveButton("DELETE"){d,e ->
                val bookId = model.bookId
                val commentId = model.id
                //delete comment
                val ref = FirebaseDatabase.getInstance().getReference("Books")
                ref.child(bookId).child("Comments").child(commentId)
                    .removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to delete comment due to ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("CANCEL"){d,e ->
                d.dismiss()
            }.show()
    }

    private fun loadUserDetails(model: ModelComment, holder: AdapterComment.HolderComment) {
        val uid = model.uid
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get name profile pic
                    val name = "${snapshot.child("name").value}"
                    val profileImage = "${snapshot.child("profileImage").value}"
                    //set data
                    holder.nameTv.text = name
                    try{
                        Glide.with(context)
                            .load(profileImage)
                            .placeholder(R.drawable.ic_person_gray)
                            .into(holder.profileIv)
                    }catch (e:Exception){
                        //in case image is empty or null set default
                        holder.profileIv.setImageResource(R.drawable.ic_person_gray)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    override fun getItemCount(): Int {
        return commentArrayList.size
    }

    //viewholder class for row_comment.xml
    inner class HolderComment(itemView: View) : RecyclerView.ViewHolder(itemView){
        //init ui views of row_comment.xml
        val profileIv = binding.profileIv
        val nameTv = binding.nameTv
        val dateTv = binding.dateTv
        val commentTv = binding.commentTv
    }
}