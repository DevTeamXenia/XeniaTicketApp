package com.xenia.ticket.ui.screens.billing

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xenia.ticket.data.network.service.ApiClient
import com.xenia.ticket.databinding.ActivityTransctionBinding
import com.xenia.ticket.ui.adapter.TransactionAdapter
import com.xenia.ticket.utils.common.SessionManager
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.*

class TransctionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransctionBinding
    private lateinit var adapter: TransactionAdapter
    private val sessionManager: SessionManager by inject()
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private var totalPages = 1
    private var pageIndex = 1
    private val pageSize = 10
    private var isLoading = false
    private var isLastPage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransctionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSpinners()
        setupDatePickers()

        val today = dateFormat.format(Date())
        binding.edtFromDate.setText(today)
        binding.edtToDate.setText(today)

        fetchTransactions(today, today)
        setupPaginationButtons()

    }
    private fun setupPaginationButtons() {
        binding.btnPrevious.setOnClickListener {
            if (pageIndex > 1) {
                pageIndex--
                fetchTransactions(
                    binding.edtFromDate.text.toString(),
                    binding.edtToDate.text.toString()
                )
            }
        }

        binding.btnNext.setOnClickListener {
            if (pageIndex < totalPages) {
                pageIndex++
                fetchTransactions(
                    binding.edtFromDate.text.toString(),
                    binding.edtToDate.text.toString()
                )
            }
        }
    }
    private fun updatePageInfo() {
        binding.tvPageInfo.text = "Page $pageIndex of $totalPages"
        binding.btnPrevious.isEnabled = pageIndex > 1
        binding.btnNext.isEnabled = pageIndex < totalPages
    }
    private fun setupRecyclerView() {
        adapter = TransactionAdapter()
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.adapter = adapter
        binding.rvTransactions.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                val layoutManager = rv.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && !isLastPage) {
                    if (visibleItemCount + firstVisibleItem >= totalItemCount - 2) {
                        loadNextPage()
                    }
                }
            }
        })
    }

    private fun loadNextPage() {
        pageIndex++
        fetchTransactions(binding.edtFromDate.text.toString(), binding.edtToDate.text.toString())
    }
    private fun setupSpinners() {
        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                resetPaginationAndFetch()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.spinnerStatus.onItemSelectedListener = listener
        binding.spinnerMode.onItemSelectedListener = listener
    }

    private fun resetPaginationAndFetch() {
        pageIndex = 1
        isLastPage = false
        adapter.clear()
        fetchTransactions(binding.edtFromDate.text.toString(), binding.edtToDate.text.toString())
    }

    private fun setupDatePickers() {
        binding.edtFromDate.setOnClickListener {
            openDatePicker { date ->
                binding.edtFromDate.setText(date)
                resetPaginationAndFetch()
            }
        }

        binding.edtToDate.setOnClickListener {
            openDatePicker { date ->
                binding.edtToDate.setText(date)
                resetPaginationAndFetch()
            }
        }
    }

    private fun openDatePicker(onDateSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                cal.set(year, month, day)
                onDateSelected(dateFormat.format(cal.time))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    private fun fetchTransactions(fromDate: String, toDate: String) {

        Log.d("TXN_DEBUG", "fetchTransactions called")

        isLoading = true

        val status = binding.spinnerStatus.selectedItem?.toString()
            ?.takeIf { it != "All" } ?: ""

        lifecycleScope.launch {
            try {
                Log.d("TXN_DEBUG", "Coroutine started")

                val token = sessionManager.getToken()
                Log.d("TXN_DEBUG", "Token = $token")

                val response = ApiClient.apiService.getTransactionReport(
                    startDate = fromDate,
                    endDate = toDate,
                    status = status,
                    pageIndex = pageIndex,
                    pageSize = pageSize,
                    bearerToken = token.toString()
                )

                Log.d("TXN_DEBUG", "API called")

                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("TXN_DEBUG", "Body = $body")
                    Log.d("TXN_DEBUG", "Items size = ${body?.Items?.size}")
                    Log.d("TXN_DEBUG", "TotalPages = ${body?.TotalPages}")

                    val items = body?.Items ?: emptyList()
                    if (pageIndex == 1) {
                        adapter.submitList(items)
                    } else {
                        adapter.addMore(items)
                    }

                    totalPages = body?.TotalPages ?: 1
                    updatePageInfo()
                    isLastPage = pageIndex >= totalPages
                } else {
                    Log.e("TXN_DEBUG", "API failed: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("TXN_DEBUG", "Exception", e)
            } finally {
                isLoading = false
            }
        }
    }
}