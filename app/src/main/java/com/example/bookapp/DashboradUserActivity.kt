package com.example.bookapp

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.bookapp.databinding.ActivityDashboardAdminBinding
import com.example.bookapp.databinding.ActivityDashboradUserBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DashboradUserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboradUserBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var categoryArrayList: ArrayList<ModelCategory>
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboradUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()

        setupWithViewPAgerAdapter(binding.viewPager)
        binding.tabLayout.setupWithViewPager(binding.viewPager)

        binding.logoutBtn.setOnClickListener {
            firebaseAuth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

    }

    private fun setupWithViewPAgerAdapter(viewPager: ViewPager){
        viewPagerAdapter = ViewPagerAdapter(
            supportFragmentManager,
            FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT,
            this)

        categoryArrayList = ArrayList()
        //load categories from db
        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //clear list
                    categoryArrayList.clear()
                    //load some static categories ex: all, most view
                    //add data to models
                    val modelAll = ModelCategory("01", "All", 1, "")
                    val modelMostViewed = ModelCategory("01", "Most Viewed", 1, "")
                    val modelMostDownloaded = ModelCategory("01", "Most Downloaded", 1, "")
                    //add to list
                    categoryArrayList.add(modelAll)
                    categoryArrayList.add(modelMostViewed)
                    categoryArrayList.add(modelMostDownloaded)
                    //add to viewPagerAdapter
                    viewPagerAdapter.addFragment(
                        BooksUserFragment.newInstance(
                            "${modelAll.id}",
                            "${modelAll.category}",
                            "${modelAll.uid}"
                        ),modelAll.category
                    )
                    viewPagerAdapter.addFragment(
                        BooksUserFragment.newInstance(
                            "${modelMostViewed.id}",
                            "${modelMostViewed.category}",
                            "${modelMostViewed.uid}"
                        ),modelMostViewed.category
                    )
                    viewPagerAdapter.addFragment(
                        BooksUserFragment.newInstance(
                            "${modelMostDownloaded.id}",
                            "${modelMostDownloaded.category}",
                            "${modelMostDownloaded.uid}"
                        ),modelMostDownloaded.category
                    )
                    //refresh list
                    viewPagerAdapter.notifyDataSetChanged()
                    //Now load from DB
                    for(ds in snapshot.children){
                        //get data in model
                        val model = ds.getValue(ModelCategory::class.java)
                        //add to list
                        categoryArrayList.add(model!!)
                        //add to viewPagerAdapter
                        viewPagerAdapter.addFragment(
                            BooksUserFragment.newInstance(
                                "${model.id}",
                                "${model.category}",
                                "${model.uid}"
                            ),model.category
                        )
                        //refresh list
                        viewPagerAdapter.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })

        //setup adapter to viewPager
        viewPager.adapter = viewPagerAdapter
    }

    class ViewPagerAdapter(fm: FragmentManager, behaviour: Int, context: Context): FragmentPagerAdapter(fm, behaviour) {
        //holds list of fragments ex: new instances of some fragment for each category
        private val fragmentsList: ArrayList<BooksUserFragment> = ArrayList()
        //list of titles of categories, for tabs
        private val fragmentTitleList: ArrayList<String> = ArrayList()
        private val context: Context

        init {
            this.context = context
        }

        override fun getCount(): Int {
            return fragmentsList.size
        }

        override fun getItem(position: Int): Fragment {
            return fragmentsList[position]
        }

        override fun getPageTitle(position: Int): CharSequence {
            return fragmentTitleList[position]
        }

        fun addFragment(fragment: BooksUserFragment, title: String){
            //add fragment thet will be passed as parameter in fragment list
            fragmentsList.add(fragment)
            //add title that will be passed as parameter
            fragmentTitleList.add(title)
        }
    }

    private fun checkUser() {
        val firebaseUser = firebaseAuth.currentUser
        if(firebaseUser == null){
            binding.subtitleTv.text = "Not logged in"
        }
        else{
            val email = firebaseUser.email
            binding.subtitleTv.text = email
        }
    }
}