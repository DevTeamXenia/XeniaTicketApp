package com.example.ticket.ui.sreens.billing

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ticket.R
import com.example.ticket.data.repository.CategoryRepository
import com.example.ticket.data.repository.CompanyRepository
import com.example.ticket.data.room.entity.Category
import com.example.ticket.databinding.ActivityBillingSelectionBinding
import com.example.ticket.databinding.ActivitySelectionBinding
import com.example.ticket.ui.adapter.CategoryAdapter
import com.example.ticket.ui.dialog.CustomInactivityDialog
import com.example.ticket.ui.sreens.screen.TicketActivity
import com.example.ticket.utils.common.CommonMethod.setLocale
import com.example.ticket.utils.common.CompanyKey
import com.example.ticket.utils.common.InactivityHandler
import com.example.ticket.utils.common.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import kotlin.getValue

class Billing_Selection_Activity : AppCompatActivity() ,

    CategoryAdapter.OnCategoryClickListener {
    private lateinit var binding: ActivityBillingSelectionBinding

    private lateinit var inactivityHandler: InactivityHandler
    private lateinit var inactivityDialog: CustomInactivityDialog
    private val categoryRepository: CategoryRepository by inject()
    private val companyRepository: CompanyRepository by inject()
    private var selectedCategoryId: Int = 0
    private var selectedLanguage: String? = ""
    private var backPressedTime: Long = 0
    private val sessionManager: SessionManager by inject()
    private lateinit var categoryAdapter: CategoryAdapter
    private var toast: Toast? = null

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val currentTime = System.currentTimeMillis()
            if (currentTime - backPressedTime < 2000) {
                toast?.cancel()
                finishAffinity()
            } else {
                backPressedTime = currentTime
                toast = Toast.makeText(
                    this@Billing_Selection_Activity,
                    "Press back again to exit",
                    Toast.LENGTH_SHORT
                )
                toast?.show()
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBillingSelectionBinding.inflate(layoutInflater)
        if (intent.getBooleanExtra("forceLandscape", false)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        setContentView(binding.root)

        selectedLanguage = sessionManager.getBillingSelectedLanguage()
        setLocale(this, selectedLanguage)
        setupUI()
        setupRecyclerViews()
        getCategory()
    }
    private fun setupUI() {
        binding.txtHome?.text = getString(R.string.more_people)
        binding.txtselectTicket.text = getString(R.string.choose_your_tickets)

    }
    @SuppressLint("NotifyDataSetChanged")
    private fun setupRecyclerViews() {

        categoryAdapter = CategoryAdapter(this, selectedLanguage!!, this)

        binding.ticketCat.layoutManager = LinearLayoutManager(this)
        binding.ticketCat.adapter = categoryAdapter
        val layoutManager = GridLayoutManager(this, 4)
        binding.ticketCat.layoutManager = layoutManager

    }

    private fun getCategory() {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken()

                if (token.isNullOrEmpty()) {
                    binding.ticketCat.visibility = View.GONE
                    return@launch
                }

                val isLoaded = withContext(Dispatchers.IO) {
                    categoryRepository.loadCategories(token)
                }

                if (!isLoaded) {
                    binding.ticketCat.visibility = View.GONE
                    return@launch
                }

                val categoryEntities = withContext(Dispatchers.IO) {
                    categoryRepository.getAllCategory()
                }

                if (categoryEntities.isEmpty()) {
                    binding.ticketCat.visibility = View.GONE
                    return@launch
                }

                val activeCategories = categoryEntities.filter { it.categoryActive }

                if (activeCategories.isEmpty()) {
                    binding.ticketCat.visibility = View.GONE
                    return@launch
                }

                binding.ticketCat.visibility =
                    if (companyRepository.getBoolean(CompanyKey.CATEGORY_ENABLE))
                        View.VISIBLE
                    else View.GONE

                val selectedCategory = activeCategories.first()
                selectedCategoryId = selectedCategory.categoryId

                val categories = activeCategories.map { entity ->
                    Category(
                        categoryId = entity.categoryId,
                        categoryName = entity.categoryName,
                        categoryNameMa = entity.categoryNameMa,
                        categoryNameTa = entity.categoryNameTa,
                        categoryNameTe = entity.categoryNameTe,
                        categoryNameKa = entity.categoryNameKa,
                        categoryNameHi = entity.categoryNameHi,
                        categoryNameMr = entity.categoryNameMr,
                        categoryNamePa = entity.categoryNamePa,
                        categoryNameSi = entity.categoryNameSi,
                        CategoryCompanyId = entity.CategoryCompanyId,
                        categoryCreatedDate = entity.categoryCreatedDate,
                        categoryCreatedBy = entity.categoryCreatedBy,
                        categoryModifiedDate = entity.categoryModifiedDate,
                        categoryModifiedBy = entity.categoryModifiedBy,
                        categoryActive = entity.categoryActive
                    )
                }

                categoryAdapter.updateCategories(categories)

            } catch (e: Exception) {
                Log.e("DEBUG", "Category load error", e)
                binding.ticketCat.visibility = View.GONE
            }
        }
    }

    override fun onCategoryClick(category: Category) {
        val categoryId = category.categoryId
        val intent = Intent(this, TicketActivity::class.java)
        intent.putExtra("CATEGORY_ID", categoryId)
        startActivity(intent)
    }
    override fun onResume() {
        super.onResume()
        getCategory()
    }
}