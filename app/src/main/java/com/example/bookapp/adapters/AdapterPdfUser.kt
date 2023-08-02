package com.example.bookapp.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.example.bookapp.filters.FilterPdfUser
import com.example.bookapp.MyApplication
import com.example.bookapp.activities.PdfDetailActivity
import com.example.bookapp.databinding.RowPdfUserBinding
import com.example.bookapp.models.ModelPdf

class AdapterPdfUser : RecyclerView.Adapter<AdapterPdfUser.HolderPdfUser>, Filterable{
    private var context: Context
    //arraylist to hold pdf, using constructor
    var pdfArrayList: ArrayList<ModelPdf>
    var filterList: ArrayList<ModelPdf>
    private lateinit var binding: RowPdfUserBinding
    private var filter: FilterPdfUser? = null

    constructor(context: Context, pdfArrayList: ArrayList<ModelPdf>) {
        this.context = context
        this.pdfArrayList = pdfArrayList
        this.filterList = pdfArrayList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderPdfUser {
        //inflate layout
        binding = RowPdfUserBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderPdfUser(binding.root)
    }

    override fun getItemCount(): Int {
        return pdfArrayList.size
    }

    override fun onBindViewHolder(holder: HolderPdfUser, position: Int) {
        //get, set data handle click
        val model = pdfArrayList[position]
        val bookId = model.id
        val categoryID = model.categoryId
        val title = model.title
        val description = model.description
        val uid = model.uid
        val url = model.url
        val timestamp = model.timestamp
        val date = MyApplication.formatTimeStamp(timestamp)

        holder.titleTv.text = title
        holder.descriptionTv.text = description
        holder.dateTv.text = date

        MyApplication.loadPdfFromUrlSinglePage(
            pdfUrl = url,
            pdfTitle = title,
            pdfView = holder.pdfView,
            progressBar = holder.progressBar,
            pagesTv = null
        )//no need of pages so pass null

        MyApplication.loadCategory(categoryId = categoryID, categoryTv = holder.categoryTv)

        MyApplication.loadPdfSize(pdfUrl = url, pdfTitle = title, sizeTv = holder.sizeTv)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, PdfDetailActivity::class.java)
            intent.putExtra("bookId", bookId)
            context.startActivity(intent)
        }
    }

    override fun getFilter(): Filter {
        if(filter == null){
            filter = FilterPdfUser(filterList, this)
        }
        return filter as FilterPdfUser
    }

    //viewholder class row_pdf_user.xml
    inner class HolderPdfUser(itemView: View): RecyclerView.ViewHolder(itemView){
        //init UI components of row_pdf_user.xml
        var pdfView = binding.pdfView
        var progressBar = binding.progressBar
        var titleTv = binding.titleTv
        var descriptionTv = binding.descriptionTv
        var sizeTv = binding.sizeTv
        var categoryTv = binding.categoryTv
        var dateTv = binding.dateTv
    }
}