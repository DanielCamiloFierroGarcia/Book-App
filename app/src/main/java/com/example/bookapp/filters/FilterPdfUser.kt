package com.example.bookapp.filters

import android.widget.Filter
import com.example.bookapp.adapters.AdapterPdfUser
import com.example.bookapp.models.ModelPdf

class FilterPdfUser: Filter {
    //arraylist in which we search
    val filterList: ArrayList<ModelPdf>
    //adapter in which filter need to be implemented
    var adapterPdfUser: AdapterPdfUser

    constructor(filterList: ArrayList<ModelPdf>, adapterPdfUser: AdapterPdfUser) : super() {
        this.filterList = filterList
        this.adapterPdfUser = adapterPdfUser
    }

    override fun performFiltering(constraint: CharSequence): FilterResults {
        var const: CharSequence? = constraint
        val results = FilterResults()
        //value to be searched should not be null or empty
        if(const != null && const.isNotEmpty()){
            const = const.toString().uppercase()
            val filteredModels = ArrayList<ModelPdf>()
            for(i in filterList.indices){
                //validate if match
                if(filterList[i].title.uppercase().contains(const)){
                    //searched value matched with title, add to list
                    filteredModels.add(filterList[i])
                }
            }
            //returnn filtered list and size
            results.count = filteredModels.size
            results.values = filteredModels
        }
        else{
            //return original list
            results.count = filterList.size
            results.values = filterList
        }
        return results
    }

    override fun publishResults(constraint: CharSequence, results: FilterResults) {
        //apply filter changes
        adapterPdfUser.pdfArrayList = results.values as ArrayList<ModelPdf>
        //notify changes
        adapterPdfUser.notifyDataSetChanged()
    }
}