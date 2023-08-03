package com.example.bookapp

import android.app.Application
import android.app.ProgressDialog
import android.content.Context
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.bookapp.activities.PdfDetailActivity
import com.github.barteksc.pdfviewer.PDFView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import org.w3c.dom.Text
import java.util.Calendar
import java.util.Locale

class MyApplication: Application() {

    override fun onCreate() {
        super.onCreate()
    }

    companion object{
        //created a static method to convert timestamp to proper date format, so we can use it everywhere in project, no need to rewrite again
        fun formatTimeStamp(timestamp: Long): String{
            val cal = Calendar.getInstance(Locale.ENGLISH)
            cal.timeInMillis = timestamp
            //format dd/mm/yyyy
            return DateFormat.format("dd/MM/yyyy", cal).toString()
        }
        //function to get pdf size
        fun loadPdfSize(pdfUrl: String, pdfTitle: String, sizeTv: TextView){
            val TAG = "PDF_SIZE_TAG"
            //using url we can get file and its metadata form FB storage
            val ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
            ref.metadata
                .addOnSuccessListener {storageMetadata->
                    Log.d(TAG, "loadPdfSize: got metadata")
                    val bytes = storageMetadata.sizeBytes.toDouble()
                    Log.d(TAG, "loadPdfSize: Size bytes $bytes")

                    //convert bytes to KB/MB
                    val kb = bytes/1024
                    val mb = kb/1024
                    if(mb>=1){
                        sizeTv.text = "${String.format("%.2f", mb)}MB"
                    }
                    else if(kb>=1){
                        sizeTv.text = "${String.format("%.2f", kb)}KB"
                    }
                    else{
                        sizeTv.text = "${String.format("%.2f", bytes)}MB"
                    }
                }
                .addOnFailureListener {
                    //failed to get metadata
                    Log.d(TAG, "loadPdfSize: Failed to get metadata due to ${it.message}")
                }
        }
        //---------------------------------------------------------------------------------
        /*instead of making new function toadPdfPageCount() to just toad pages count it would be better to use some existing function to do that
        * i.e. loadPdfFromUrtSinglePage
        * We Will add another parameter of type TextView e. g. pagesTv
        * Whenever we call that function
        * 1) if we require page numbers we Will pass pagesTv (TextView)
        * 2) if f we don't require page number we Will pass null
        * And in function if pagesTv (TextView) parameter is not null we set the page number count*/
        //---------------------------------------------------------------------------------

        fun loadPdfFromUrlSinglePage(pdfUrl: String, pdfTitle: String, pdfView: PDFView, progressBar: ProgressBar, pagesTv: TextView?){
            //using url we can get file and its metadata from firebase storage
            val TAG = "PDF_THUMBNAIL_TAG"
            val ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
            ref.getBytes(Constants.MAX_BYTES_PDF)
                .addOnSuccessListener {bytes->
                    Log.d(TAG, "loadPdfSize: Size bytes $bytes")

                    //set to PDFView
                    pdfView.fromBytes(bytes)
                        .pages(0)//show first page only
                        .spacing(0)
                        .swipeHorizontal(false)
                        .enableSwipe(false)
                        .onError { t->
                            progressBar.visibility = View.INVISIBLE
                            Log.d(TAG, "loadPdfFromUrlSinglePage: ${t.message}")
                        }
                        .onPageError { page, t ->
                            progressBar.visibility = View.INVISIBLE
                            Log.d(TAG, "loadPdfFromUrlSinglePage: ${t.message}")
                        }
                        .onLoad { nbPages ->
                            Log.d(TAG, "loadPdfFromUrlSinglePage: Pages: $nbPages")
                            //pdf loaded we can set page count, pdf thumbnail
                            progressBar.visibility = View.INVISIBLE
                            //if pagesTv param is not null then set page numbers
                            if(pagesTv != null){
                                pagesTv.text = "$nbPages"
                            }
                        }
                        .load()
                }
                .addOnFailureListener {
                    //failed to get metadata
                    Log.d(TAG, "loadPdfSize: Failed to get metadata due to ${it.message}")
                }
        }
        fun loadCategory(categoryId: String, categoryTv: TextView){
            //load category using category id from FB
            val ref = FirebaseDatabase.getInstance().getReference("Categories")
            ref.child(categoryId)
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        //get category
                        val category= "${snapshot.child("category").value}"
                        //set category
                        categoryTv.text = category
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
        }

        fun deleteBook(context: Context, bookId: String, bookUrl: String, bookTitle: String){
            val TAG = "DELETE_BOOK_TAG"
            Log.d(TAG, "deleteBook: deleting")
            val progressDialog = ProgressDialog(context)
            progressDialog.setMessage("Deleting book $bookTitle")
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.show()

            Log.d(TAG, "deleteBook: Deleting from storage")
            val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl)
            storageReference.delete()
                .addOnSuccessListener {
                    Log.d(TAG, "deleteBook: Deleted")
                    Log.d(TAG, "deleteBook: Deleting from db now")

                    val ref = FirebaseDatabase.getInstance().getReference("Books")
                    ref.child(bookId)
                        .removeValue()
                        .addOnSuccessListener {
                            progressDialog.dismiss()
                            Toast.makeText(context, "Deleted Book", Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "deleteBook: Deleted from db")
                        }
                        .addOnFailureListener {
                            progressDialog.dismiss()
                            Log.d(TAG, "deleteBook: Failed to delete from db due to ${it.message}")
                            Toast.makeText(context, "Failed to delete from db due to ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    progressDialog.dismiss()
                    Log.d(TAG, "deleteBook: Failed to delete from storage due to ${it.message}")
                    Toast.makeText(context, "Failed to delete from storage due to ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        fun incrementBookViewCount(bookId: String){
            //Get book views count
            val ref = FirebaseDatabase.getInstance().getReference("Books")
            ref.child(bookId)
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        //get views count
                        var viewsCount = "${snapshot.child("viewsCount").value}"
                        if(viewsCount == "" || viewsCount == "null"){
                            viewsCount = "0"
                        }
                        //Increment views count
                        val newViewsCount = viewsCount.toLong() + 1
                        //setup data to update in db
                        val hashMap = HashMap<String, Any>()
                        hashMap["viewsCount"] = newViewsCount
                        //set to db
                        val dbref = FirebaseDatabase.getInstance().getReference("Books")
                        dbref.child(bookId)
                            .updateChildren(hashMap)
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
        }

        fun removeFromFavorite(context: Context, bookId: String){
            val TAG = "REMOVE_FAV_TAG"
            Log.d(TAG, "removeFormFavorite: Removing from fav")

            val firebaseAuth = FirebaseAuth.getInstance()

            val ref = FirebaseDatabase.getInstance().getReference("Users")
            ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
                .removeValue()
                .addOnSuccessListener {
                    Log.d(TAG, "removeFormFavorite: Removed from favs")
                    Toast.makeText(context, "Removed from fav", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Log.d(TAG, "removeFormFavorite: Failed to remove from fav due to ${it.message}")
                    Toast.makeText(context, "Failed to remove due to ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

}