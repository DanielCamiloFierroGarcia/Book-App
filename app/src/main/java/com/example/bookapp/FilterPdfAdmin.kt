package com.example.bookapp

import android.widget.Filter

//used to filter data from recyclerview | search pdf from pdf list in recycler
class FilterPdfAdmin : Filter{
    //arraylist in which we want to search
    var filterList: ArrayList<ModelPdf>
    //adapter in which filter need to be implemented
    var adapterPdfAdmin: AdapterPdfAdmin

    //constructor
    constructor(filterList: ArrayList<ModelPdf>, adapterPdfAdmin: AdapterPdfAdmin) {
        this.filterList = filterList
        this.adapterPdfAdmin = adapterPdfAdmin
    }

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        var constraintVar: CharSequence? = constraint//value to seacrh
        val results = FilterResults()
        //value to be searched should not be null and not empty
        if(constraintVar != null && constraintVar.isNotEmpty()){
            //change to upper case
            constraintVar = constraintVar.toString().lowercase()
            var filteredModels = ArrayList<ModelPdf>()
            for(i in filterList.indices){
                //validate if match
                if(filterList[i].title.lowercase().contains(constraintVar)){
                    //searched value is similar to value in list, add to filtered list
                    filteredModels.add(filterList[i])
                }
            }
            results.count = filteredModels.size
            results.values = filteredModels
        }
        else{
            //searched value is either null or empty, retur all data
            results.count = filterList.size
            results.values = filterList
        }
        return results
    }

    override fun publishResults(constraint: CharSequence, results: FilterResults) {
        //apply filter changes
        adapterPdfAdmin.pdfArrayList = results?.values as ArrayList<ModelPdf>//si no funciona remplazar ? por !
        //notify changes
        adapterPdfAdmin.notifyDataSetChanged()
    }
}