package com.xenia.ticket.ui.screens.billing

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.xenia.ticket.data.repository.ReportRepository
import com.xenia.ticket.databinding.ActivityTransctionBinding
import com.xenia.ticket.ui.adapter.TransactionAdapter
import com.xenia.ticket.utils.common.JwtUtils
import com.xenia.ticket.utils.common.SessionManager
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.*

class TransactionReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransctionBinding
    private lateinit var adapter: TransactionAdapter
    private val repository: ReportRepository by inject()
    private val sessionManager: SessionManager by inject()

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    private var fromDate = ""
    private var toDate = ""

    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransctionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupDatePickers()

        val today = dateFormat.format(Date())
        fromDate = today
        toDate = today

        binding.edtFromDate.setText(today)
        binding.edtToDate.setText(today)

        fetchTransactions()
    }

    private fun setupRecyclerView() {
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)

        adapter = TransactionAdapter { transaction ->
            val intent = Intent(applicationContext, TransactionDetailActivity::class.java)
            intent.putExtra("orderId", transaction.OrderId)
            intent.putExtra("transID", transaction.TransactionId)
            intent.putExtra("receiptNumber", transaction.ReceiptNo)
            intent.putExtra("name", transaction.CustomerName)
            intent.putExtra("phoneNo", transaction.PhoneNumber)
            startActivity(intent)
        }

        binding.rvTransactions.adapter = adapter
    }


    private fun setupDatePickers() {

        binding.edtFromDate.setOnClickListener {
            openDatePicker { selectedDate ->
                fromDate = selectedDate
                binding.edtFromDate.setText(selectedDate)
                fetchTransactions()
            }
        }

        binding.edtToDate.setOnClickListener {
            openDatePicker { selectedDate ->
                toDate = selectedDate
                binding.edtToDate.setText(selectedDate)
                fetchTransactions()
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


    private fun fetchTransactions() {
        if (isLoading) return
        isLoading = true

        lifecycleScope.launch {
            try {
                val rawToken = sessionManager.getToken()
                    ?.replace("Bearer ", "")
                    ?: ""

                val userId = JwtUtils.getUserId(rawToken)!!

                val response = repository.fetchTransactionReport(
                    startDate = fromDate,
                    endDate = toDate,
                    userId = userId
                )

                adapter.submitList(response)

            } catch (e: Exception) {
                Toast.makeText(
                    this@TransactionReportActivity,
                    e.message ?: "Something went wrong",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isLoading = false
            }
        }
    }
}