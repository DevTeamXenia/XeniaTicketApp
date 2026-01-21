package com.example.ticket.ui.sreens.screen

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ticket.R
import com.example.ticket.databinding.ActivityPrinterSettingBinding
import com.example.ticket.utils.common.CommonMethod.setLocale
import com.example.ticket.utils.common.Constants.PRINTER_B200MAX
import com.example.ticket.utils.common.Constants.PRINTER_FALCON
import com.example.ticket.utils.common.Constants.PRINTER_KIOSK
import com.example.ticket.utils.common.SessionManager
import com.urovo.sdk.print.PrinterProviderImpl

class PrinterSettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrinterSettingBinding
    private var selectedPrinter: String? = null
    private lateinit var sessionManager: SessionManager

    private var status: String? = null
    private var amount: String? = null
    private var transID: String? = null
    private var orderID: String? = null
    private var phoneNo: String? = null
    private var from: String? = null
    private var prefix: String? = null
    private var name: String? = null
    private var star: String? = null
    private var devatha: String? = null
    private var devathaEn: String? = null
    private var devathaMl: String? = null
    private var billFrom: String? = null
    private var ps: String? = null
    private var pss: String? = null
    private var sr: String? = null
    private var pv: String? = null
    private var fromLogin: String? = null
    private var selectedLanguage: String? = null
    private var mPrintManager: PrinterProviderImpl? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrinterSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        mPrintManager = PrinterProviderImpl.getInstance(this@PrinterSettingActivity)
        initUI()
        setLocale(this, sessionManager.getSelectedLanguage())
        selectedLanguage = sessionManager.getBillingSelectedLanguage()

        loadSavedPrinterSelection()

        from = intent.getStringExtra("from")
        status = intent.getStringExtra("status")
        amount = intent.getStringExtra("amount")
        transID = intent.getStringExtra("transID")
        orderID = intent.getStringExtra("orderID")
        phoneNo = intent.getStringExtra("phno")
        prefix = intent.getStringExtra("prefix")
        name = intent.getStringExtra("name")
        star = intent.getStringExtra("star")
        devatha = intent.getStringExtra("devatha")
        devathaEn = intent.getStringExtra("devathaEn")
        devathaMl = intent.getStringExtra("devathaMl")
        orderID = intent.getStringExtra("orderID")
        phoneNo = intent.getStringExtra("phno")
        billFrom = intent.getStringExtra("billFrom")
        ps = intent.getStringExtra("ps")
        pv = intent.getStringExtra("pv")
        pss = intent.getStringExtra("pss")
        sr = intent.getStringExtra("SR")
        fromLogin = intent.getStringExtra("fromLogin")
        binding.imgFalcon.setOnClickListener { selectPrinter(PRINTER_FALCON, binding.imgFalcon) }
        binding.imgBPOS200Max.setOnClickListener {
            selectPrinter(
                PRINTER_B200MAX,
                binding.imgBPOS200Max
            )
        }
        binding.imgKiosk.setOnClickListener { selectPrinter(PRINTER_KIOSK, binding.imgKiosk) }


        binding.btnSave.setOnClickListener {
            if (selectedPrinter == null) {
                Toast.makeText(this, "Please select a printer", Toast.LENGTH_SHORT).show()
            } else {
                savePrinterSelection()
                finish()
            }
        }

//        binding.btnPrint.setOnClickListener {
//            startPrint_Bitmap(false)
//        }

    }

    private fun initUI() {
        binding.printerTitleTxt.text = getString(R.string.select_device_printer)
        binding.btnSave.text = getString(R.string.save)

    }

    private fun selectPrinter(name: String, selectedView: ImageView) {
        selectedPrinter = name
        binding.imgFalcon.setBackgroundResource(R.drawable.bg_printer_unselected)
        binding.imgBPOS200Max.setBackgroundResource(R.drawable.bg_printer_unselected)
        binding.imgKiosk.setBackgroundResource(R.drawable.bg_printer_unselected)
        selectedView.setBackgroundResource(R.drawable.bg_printer_selected)
    }
    private fun savePrinterSelection() {
        sessionManager.saveSelectedPrinter(selectedPrinter!!)

        when {
            ps == "pd" -> {
                val printIntent = Intent(this, PaymentActivity::class.java).apply {
                    putExtra("from", from)
                    putExtra("status", status)
                    putExtra("amount", amount)
                    putExtra("transID", transID)
                    putExtra("orderID", orderID)
                    putExtra("phno", phoneNo)
                    putExtra("ps", "pd")
                }
                startActivity(printIntent)
                finish()
            }


            fromLogin == "Login" -> {
                val intent = Intent(this, LanguageActivity::class.java).apply {
                    putExtra("printerSelected", true)
                }
                startActivity(intent)
                finish()
            }

            pss == "pdd" -> {
                val printIntent = Intent(this, PaymentActivity::class.java).apply {
                    putExtra("from", from)
                    putExtra("status", status)
                    putExtra("amount", amount)
                    putExtra("transID", transID)
                    putExtra("orderID", orderID)
                    putExtra("phno", phoneNo)
                    putExtra("prefix", prefix)
                    putExtra("name", name)
                    putExtra("star", star)
                    putExtra("devatha ", devatha)
                    putExtra("devathaEn", devathaEn)
                    putExtra("devathaMl", devathaMl)
                    putExtra("orderID ", orderID)
                    putExtra("pss", "pdd")
                }
                startActivity(printIntent)
                finish()
            }


            else -> {
                finish()
            }
        }
    }


    private fun loadSavedPrinterSelection() {
        val savedPrinter = sessionManager.getSelectedPrinter()

        savedPrinter?.let { printer ->
            selectedPrinter = printer
            when (printer) {
                PRINTER_FALCON -> selectPrinter(PRINTER_FALCON, binding.imgFalcon)
                PRINTER_B200MAX -> selectPrinter(PRINTER_B200MAX, binding.imgBPOS200Max)
                PRINTER_KIOSK -> selectPrinter(PRINTER_KIOSK, binding.imgKiosk)
            }
        }
    }
}