package com.example.bookapp.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.example.bookapp.filters.FilterPdfAdmin
import com.example.bookapp.MyApplication
import com.example.bookapp.activities.PdfDetailActivity
import com.example.bookapp.activities.PdfEditActivity
import com.example.bookapp.databinding.RowPdfAdminBinding
import com.example.bookapp.models.ModelPdf

class AdapterPdfAdmin : RecyclerView.Adapter<AdapterPdfAdmin.HolderPdfAdmin>, Filterable{

    private var context: Context
    //arraylist to hold pdf
    var pdfArrayList: ArrayList<ModelPdf>
    private val filterList: ArrayList<ModelPdf>
    private lateinit var binding: RowPdfAdminBinding

    //filter object
    private var filter: FilterPdfAdmin? = null

    //constructor
    constructor(
        context: Context,
        pdfArrayList: ArrayList<ModelPdf>
    ) : super() {
        this.context = context
        this.pdfArrayList = pdfArrayList
        this.filterList = pdfArrayList
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderPdfAdmin {
        //bind/inflate layout row_pdf_admin.xml
        binding = RowPdfAdminBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderPdfAdmin(binding.root)
    }

    override fun getItemCount(): Int {
        return pdfArrayList.size//items count
    }

    override fun getFilter(): Filter {
        if(filter == null){
            filter = FilterPdfAdmin(filterList, this)
        }
        return filter as FilterPdfAdmin
    }

    override fun onBindViewHolder(holder: HolderPdfAdmin, position: Int) {
        //Get data, set data, handle click
        //get
        val model = pdfArrayList[position]
        val pdfId = model.id
        val categoryId = model.categoryId
        val title = model.title
        val description = model.description
        val pdfUrl = model.url
        val timestamp = model.timestamp
        //convert timestamp to dd/mm/yyyy format
        //create an application class to contain the functions that will be used in multiple places in app
        val formattedDate = MyApplication.formatTimeStamp(timestamp)

        //set Date
        holder.titleTv.text = title
        holder.descriptionTv.text = description
        holder.dateTv.text = formattedDate
        //load further details like category, pdf from url size
        //loadcategory
        MyApplication.loadCategory(categoryId = categoryId, holder.categoryTv)
        //we dont need page number here, pass null for page number||load pdf thumbnail
        MyApplication.loadPdfFromUrlSinglePage(
            pdfUrl = pdfUrl,
            pdfTitle = title,
            pdfView = holder.pdfView,
            progressBar = holder.progressBar,
            pagesTv = null
        )
        //load pdf size
        MyApplication.loadPdfSize(pdfUrl = pdfUrl, pdfTitle = title, sizeTv = holder.sizeTv)
        //handle click, show dialog with options 1) edit book 2) delete book
        holder.moreBtn.setOnClickListener {
            moreOptionsDialog(model, holder)
        }
        //handle item click, open PdfDetailAci
        holder.itemView.setOnClickListener {
            val intent = Intent(context, PdfDetailActivity::class.java)
            intent.putExtra("bookId", pdfId)
            context.startActivity(intent)
        }
    }

    private fun moreOptionsDialog(model: ModelPdf, holder: HolderPdfAdmin) {
        //get id, url, title of book
        val bookId = model.id
        val bookUrl = model.url
        val bookTitle = model.title
        //options to show in dialog
        val options = arrayOf("Edit", "Delete")
        //alert dialog
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Choose option")
            .setItems(options){dialog, position ->
                //handle item click
                if(position == 0){
                    //edit is clicked
                    val intent = Intent(context, PdfEditActivity::class.java)
                    intent.putExtra("bookId", bookId)
                    context.startActivity(intent)
                }
                else if(position == 1){
                    //delete clicked
                    MyApplication.deleteBook(context, bookId, bookUrl, bookTitle)
                }
            }.show()
    }

    /*View Holder class for row_pdf_admin.xml*/
    inner class HolderPdfAdmin(itemView: View) : RecyclerView.ViewHolder(itemView){
        //UI Views of row_pdf_admin.xml
        val pdfView = binding.pdfView
        val progressBar = binding.progressBar
        val titleTv = binding.titleTv
        val descriptionTv = binding.descriptionTv
        val categoryTv = binding.categoryTv
        val sizeTv = binding.sizeTv
        val dateTv = binding.dateTv
        val moreBtn = binding.moreBtn
    }
}