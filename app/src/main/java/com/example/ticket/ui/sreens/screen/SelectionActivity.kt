package com.example.ticket.ui.sreens.screen

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ticket.R
import com.example.ticket.data.repository.CategoryRepository
import com.example.ticket.data.repository.CompanyRepository
import com.example.ticket.data.room.entity.Category
import com.example.ticket.databinding.ActivityMainBinding
import com.example.ticket.databinding.ActivitySelectionBinding
import com.example.ticket.ui.adapter.CategoryAdapter
import com.example.ticket.ui.dialog.CustomInactivityDialog
import com.example.ticket.utils.common.CommonMethod.setLocale
import com.example.ticket.utils.common.CompanyKey
import com.example.ticket.utils.common.InactivityHandler
import com.example.ticket.utils.common.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.util.Locale
import kotlin.getValue

class SelectionActivity : AppCompatActivity(),
    CategoryAdapter.OnCategoryClickListener, CustomInactivityDialog.InactivityCallback {
    private lateinit var binding: ActivitySelectionBinding

    private lateinit var inactivityHandler: InactivityHandler
    private lateinit var inactivityDialog: CustomInactivityDialog
    private val categoryRepository: CategoryRepository by inject()
    private val companyRepository: CompanyRepository by inject()
    private val sessionManager: SessionManager by inject()
    private var selectedCategoryId: Int = 0
    private var selectedLanguage: String? = ""


    private lateinit var categoryAdapter: CategoryAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectionBinding.inflate(layoutInflater)


        setContentView(binding.root)

        selectedLanguage = sessionManager.getSelectedLanguage()

        inactivityDialog = CustomInactivityDialog(this)

        inactivityHandler = InactivityHandler(
            this@SelectionActivity,
            supportFragmentManager,
            inactivityDialog
        )

        onBackPressedDispatcher.addCallback(this) {
            lifecycleScope.launch {
                finish()
            }
        }
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
        val layoutManager = GridLayoutManager(this, 3)
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


    override fun resetInactivityTimer() {
        inactivityHandler.resetTimer()
    }

    override fun onRestart() {
        getCategory()
        super.onRestart()
    }

    override fun onPause() {
        super.onPause()
        inactivityHandler.pauseInactivityCheck()
    }

    override fun onDestroy() {
        super.onDestroy()

        inactivityHandler.cleanup()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        inactivityHandler.resetTimer()
        return super.dispatchTouchEvent(ev)
    }
    override fun attachBaseContext(newBase: Context) {
        val sessionManager = SessionManager(newBase)
        val lang = sessionManager.getSelectedLanguage()

        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)


        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)

        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

}