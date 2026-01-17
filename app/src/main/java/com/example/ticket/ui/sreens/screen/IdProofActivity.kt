package com.example.ticket.ui.sreens.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.ticket.R
import com.example.ticket.data.listeners.InactivityHandlerActivity
import com.example.ticket.data.repository.TicketRepository
import com.example.ticket.databinding.ActivityIdProofBinding
import com.example.ticket.ui.dialog.CustomInactivityDialog
import com.example.ticket.utils.common.CommonMethod.enableInactivityReset
import com.example.ticket.utils.common.InactivityHandler
import com.example.ticket.utils.common.SessionManager
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.getValue

class IdProofActivity : AppCompatActivity(), CustomInactivityDialog.InactivityCallback,
    InactivityHandlerActivity {
    private lateinit var binding: ActivityIdProofBinding
    private val ticketRepository: TicketRepository by inject()
    private val sessionManager: SessionManager by inject()
    private lateinit var inactivityHandler: InactivityHandler
    private lateinit var inactivityDialog: CustomInactivityDialog
    private var totalAmount: String = "0.0"
    private var selectedImageView: ImageView? = null
    private var imageData: ByteArray? = null
    private var selectedLanguage: String? = ""
    private var selectedProofType: String = ""
    private var isProofSelected = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIdProofBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.imgAadhar.isClickable = false
        binding.imgPanCard.isClickable = false
        binding.imgDrivingLicence.isClickable = false
        selectedLanguage = sessionManager.getSelectedLanguage()
        inactivityDialog = CustomInactivityDialog(this)
        inactivityHandler =
            InactivityHandler(this, supportFragmentManager, inactivityDialog)
        totalAmount = intent.getStringExtra("ITEM_TOTAL") ?: "0.0"
        binding.btnProceed.text = getString(R.string.proceed) + "  Rs." + totalAmount
        initUI()
        binding.editTextId.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                if (!isProofSelected) {
                    binding.editTextId.text?.clear()
                    Toast.makeText(this@IdProofActivity, "Please select ID proof type", Toast.LENGTH_SHORT).show()
                    return
                }


            }


            override fun afterTextChanged(s: Editable?) {}
        })
        binding.btnProceed.setOnClickListener {
            val name = binding.editTextName.text.toString()
            val idNo = binding.editTextId.text.toString()
            val idType = selectedProofType
            val phone = binding.editTextPhoneNumber.text.toString().trim()

            if (phone.isNotEmpty() && phone.length != 10) {
                showMessage("Enter valid phone number")
                return@setOnClickListener
            }
            if (idNo.isEmpty()) {

            }else{
                when (idType) {

                    "AADHAAR" -> {
                        if (!isValidAadhaar(idNo)) {
                            showMessage("Invalid Aadhaar Number")
                            return@setOnClickListener
                        }
                    }

                    "PAN" -> {
                        if (!isValidPAN(idNo)) {
                            showMessage("Invalid PAN Number")
                            return@setOnClickListener
                        }
                    }

                    "DL" -> {
                        if (!isValidDL(idNo)) {
                            showMessage("Invalid Driving Licence Number")
                            return@setOnClickListener
                        }
                    }

                    else -> {
                        showMessage("Please select ID proof type")
                        return@setOnClickListener
                    }
                }
            }




            lifecycleScope.launch {
                ticketRepository.updateCartItemsInfo(
                    newName = name,
                    newPhoneNumber = phone,
                    newIdno = idNo,
                    newIdProof = binding.txtId.text.toString(),
                    newImg = imageData ?: ByteArray(0)
                )
            }

            val intent = Intent(this, TicketCartActivity::class.java).apply {
                putExtra("name", name)
                putExtra("phno", phone)
                putExtra("IDNO", idNo)
                putExtra("ID", binding.txtId.text.toString())
            }
            startActivity(intent)
        }


        requestCameraPermission()
        val imageClickListener = View.OnClickListener { v ->
            selectedImageView?.isSelected = false
            v.isSelected = true
            selectedImageView = v as ImageView

            when (v.id) {

                R.id.imgAadhar -> {
                    isProofSelected = true
                    selectedProofType = "AADHAAR"
                    binding.txtId.text = getLocalizedIdLabel("Aadhaar No", selectedLanguage!!)

                    binding.editTextId.filters = arrayOf(InputFilter.LengthFilter(12))
                    binding.editTextId.inputType = InputType.TYPE_CLASS_NUMBER
                    binding.editTextId.text?.clear()
                }

                R.id.imgPanCard -> {
                    isProofSelected = true
                    selectedProofType = "PAN"

                    binding.txtId.text = getLocalizedIdLabel("PAN Card No", selectedLanguage!!)
                    binding.editTextId.filters = arrayOf(
                        InputFilter.LengthFilter(10),
                        InputFilter { source, _, _, _, _, _ ->
                            if (source.matches(Regex("[A-Za-z0-9]+"))) source else ""
                        }
                    )
                    binding.editTextId.inputType =
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS

                    binding.editTextId.text?.clear()
                }

                R.id.imgDrivingLicence -> {
                    isProofSelected = true
                    selectedProofType = "DL"

                    binding.txtId.text = getLocalizedIdLabel("Driving Licence No", selectedLanguage!!)
                    binding.editTextId.filters = arrayOf(
                        InputFilter.LengthFilter(16),
                        InputFilter { source, _, _, _, _, _ ->
                            if (source.matches(Regex("[A-Za-z0-9]+"))) source else ""
                        }
                    )
                    binding.editTextId.inputType =
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS

                    binding.editTextId.text?.clear()
                }

            }
        }


        binding.imgAadhar.setOnClickListener(imageClickListener)
        binding.imgPanCard.setOnClickListener(imageClickListener)
        binding.imgDrivingLicence.setOnClickListener(imageClickListener)


    }
    fun showMessage(msg: String) {
        AlertDialog.Builder(this)
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun getLocalizedIdLabel(text: String, language: String): String {
        return when (language) {
            "ml" -> when (text) {
                "Aadhaar No" -> "ആധാർ നമ്പർ"
                "PAN Card No" -> "പാൻ കാർഡ് നമ്പർ"
                "Driving Licence No" -> "ഡ്രൈവിംഗ് ലൈസൻസ് നമ്പർ"
                else -> text
            }
            "ta" -> when (text) {
                "Aadhaar No" -> "ஆதார் எண்"
                "PAN Card No" -> "பேன் அட்டை எண்"
                "Driving Licence No" -> "டிரைவிங் லைசன்ஸ் எண்"
                else -> text
            }
            "kn" -> when (text) {
                "Aadhaar No" -> "ಆಧಾರ್ ಸಂಖ್ಯೆ"
                "PAN Card No" -> "ಪ್ಯಾನ್ ಕಾರ್ಡ್ ಸಂಖ್ಯೆ"
                "Driving Licence No" -> "ಡ್ರೈವಿಂಗ್ ಲೈಸೆನ್ಸ್ ಸಂಖ್ಯೆ"
                else -> text
            }
            "si" -> when (text) {
                "Aadhaar No" -> "ආධාර් අංකය"
                "PAN Card No" -> "PAN කාඩ් අංකය"
                "Driving Licence No" -> "රියදුරු බලපත්‍ර අංකය"
                else -> text
            }
            "te" -> when (text) {
                "Aadhaar No" -> "ఆధార్ సంఖ్య"
                "PAN Card No" -> "పాన్ కార్డ్ సంఖ్య"
                "Driving Licence No" -> "డ్రైవింగ్ లైసెన్స్ సంఖ్య"
                else -> text
            }
            "hi" -> when (text) {
                "Aadhaar No" -> "आधार नंबर"
                "PAN Card No" -> "पैन कार्ड नंबर"
                "Driving Licence No" -> "ड्राइविंग लाइसेंस नंबर"
                else -> text
            }
            "pa" -> when (text) {
                "Aadhaar No" -> "ਆਧਾਰ ਨੰਬਰ"
                "PAN Card No" -> "ਪੈਨ ਕਾਰਡ ਨੰਬਰ"
                "Driving Licence No" -> "ਡਰਾਈਵਿੰਗ ਲਾਇਸੈਂਸ ਨੰਬਰ"
                else -> text
            }
            "mr" -> when (text) {
                "Aadhaar No" -> "आधार क्रमांक"
                "PAN Card No" -> "पॅन कार्ड क्रमांक"
                "Driving Licence No" -> "ड्रायव्हिंग लायसन्स क्रमांक"
                else -> text
            }
            else -> text
        }
    }

    fun isValidAadhaar(aadhaar: String): Boolean {
        if (!aadhaar.matches(Regex("\\d{12}"))) return false
        return VerhoeffAlgorithm.validateVerhoeff(aadhaar)
    }

    fun isValidPAN(pan: String): Boolean {
        val panRegex = Regex("[A-Z]{5}[0-9]{4}[A-Z]")
        return pan.matches(panRegex)
    }

    fun isValidDL(dl: String): Boolean {
        val dlRegex = Regex("[A-Z0-9]{16}")
        return dl.matches(dlRegex)
    }
    object VerhoeffAlgorithm {
        private val d = arrayOf(
            intArrayOf(0,1,2,3,4,5,6,7,8,9),
            intArrayOf(1,2,3,4,0,6,7,8,9,5),
            intArrayOf(2,3,4,0,1,7,8,9,5,6),
            intArrayOf(3,4,0,1,2,8,9,5,6,7),
            intArrayOf(4,0,1,2,3,9,5,6,7,8),
            intArrayOf(5,9,8,7,6,0,4,3,2,1),
            intArrayOf(6,5,9,8,7,1,0,4,3,2),
            intArrayOf(7,6,5,9,8,2,1,0,4,3),
            intArrayOf(8,7,6,5,9,3,2,1,0,4),
            intArrayOf(9,8,7,6,5,4,3,2,1,0)
        )

        private val p = arrayOf(
            intArrayOf(0,1,2,3,4,5,6,7,8,9),
            intArrayOf(1,5,7,6,2,8,3,0,9,4),
            intArrayOf(5,8,0,3,7,9,6,1,4,2),
            intArrayOf(8,9,1,6,0,4,3,5,2,7),
            intArrayOf(9,4,5,3,1,2,6,8,7,0),
            intArrayOf(4,2,8,6,5,7,3,9,0,1),
            intArrayOf(2,7,9,3,8,0,6,4,1,5),
            intArrayOf(7,0,4,6,9,1,3,2,5,8)
        )
        private val inv = intArrayOf(0,4,3,2,1,5,6,7,8,9)
        fun validateVerhoeff(num: String): Boolean {
            var c = 0
            var pos = 0
            for (i in num.length - 1 downTo 0) {
                val digit = num[i] - '0'
                c = d[c][p[pos % 8][digit]]
                pos++
            }
            return c == 0
        }
    }


    private fun initUI() {
        binding.txtName.text = getString(R.string.name)
        binding.txtPhoneNumber.text = getString(R.string.phone_number)
        binding.txtId.text = getString(R.string.id_no)
        binding.editTextName.enableInactivityReset(inactivityHandler)
        binding.editTextPhoneNumber.enableInactivityReset(inactivityHandler)
        binding.editTextId.enableInactivityReset(inactivityHandler)

    }

    private fun requestCameraPermission() {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            //startCamera()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                //startCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun resetInactivityTimer() {
        inactivityHandler.resetTimer()
    }

    override fun onResume() {
        super.onResume()
        inactivityHandler.resumeInactivityCheck()
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
}