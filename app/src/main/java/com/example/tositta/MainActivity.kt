package com.example.tositta

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.AdapterView
import androidx.activity.result.contract.ActivityResultContracts
import com.example.tositta.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    //Global variable to let us set selected language. Default is 0 which is also default for spinner pos 0 (latin)
    var selectedLanguage: Int = 0

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult

            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            binding.imageView.setImageBitmap(bitmap)

            runOCR(bitmap)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPickImage.setOnClickListener {
            pickImageFromGallery()
        }

        //Create Spinner for selecting languages
        val spinner: Spinner = findViewById(R.id.selectLanguageSpinner)
        //List of supported languages
        val languages = listOf("Latin", "Japanese")
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            languages
        )
        spinner.adapter = adapter

        //When selecting a language, set global 'selectLanguage' variable equal to the index of the selected language
        //TODO: probably a better way to do this so that we're not using magic numbers for OCR else if but it works for now
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                pos: Int,
                id: Long
            ) {
                selectedLanguage = pos
            }

            //If nothing is selected, set language to default (latin)
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedLanguage = 0
            }

        }

    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun runOCR(bitmap: android.graphics.Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizerLT = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) //Latin recognizer
        val recognizerJP = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build()) //Japanese recognizer

        /* Language List
        Latin = 0
        Japanese = 1
        */

        //using global selectedLanguage value, run appropriate text detection
        //TODO maybe change to when (switch) statement? Android Studio suggests this but cascaded else ifs work for now
        if (selectedLanguage == 0){ //Latin
            recognizerLT.process(image)
                .addOnSuccessListener { result ->
                    android.app.AlertDialog.Builder(this)
                        .setTitle("OCR Result")
                        .setMessage(result.text)
                        .setPositiveButton("OK", null)
                        .show()
                }
                .addOnFailureListener { e ->
                    android.app.AlertDialog.Builder(this)
                        .setTitle("OCR Error")
                        .setMessage(e.message)
                        .setPositiveButton("OK", null)
                        .show()
                }
        } else if (selectedLanguage == 1){ // Japanese
            recognizerJP.process(image)
                .addOnSuccessListener { result ->
                    android.app.AlertDialog.Builder(this)
                        .setTitle("OCR Result")
                        .setMessage(result.text)
                        .setPositiveButton("OK", null)
                        .show()
                }
                .addOnFailureListener { e ->
                    android.app.AlertDialog.Builder(this)
                        .setTitle("OCR Error")
                        .setMessage(e.message)
                        .setPositiveButton("OK", null)
                        .show()
                }
        }  else { //Use latin by default if somehow an invalid value is passed in
            recognizerLT.process(image)
                .addOnSuccessListener { result ->
                    android.app.AlertDialog.Builder(this)
                        .setTitle("OCR Result")
                        .setMessage(result.text)
                        .setPositiveButton("OK", null)
                        .show()
                }
                .addOnFailureListener { e ->
                    android.app.AlertDialog.Builder(this)
                        .setTitle("OCR Error")
                        .setMessage(e.message)
                        .setPositiveButton("OK", null)
                        .show()
                }
        }


    }
}
