package com.example.tositta

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AdapterView
import androidx.activity.result.contract.ActivityResultContracts
import com.example.tositta.databinding.ActivityMainBinding
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions



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
        val spinner = binding.selectLanguageSpinner
        //List of supported languages
        val languages = listOf("Latin", "Japanese")
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            languages
        )
        spinner.adapter = adapter

        //When selecting a language, set global 'selectLanguage' variable equal to the index of the selected language
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
        // Uses Latin by default
        val recognizer = when (selectedLanguage) {
            1 -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build()) // Japanese
            else -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) // Latin
        }

        recognizer.process(image)
            .addOnSuccessListener { result ->
                binding.ocrResultTextView.text = result.text
                binding.ocrResultContainer.visibility = View.VISIBLE
                runTranslation(result.text)

            }
            .addOnFailureListener { e ->
                binding.ocrResultTextView.text = "OCR Error: ${e.message}"
                binding.ocrResultContainer.visibility = View.VISIBLE
            }




    }



    private fun runTranslation(textToTranslate: String){
        val languageIdentifier = LanguageIdentification.getClient()
        languageIdentifier.identifyLanguage(textToTranslate)
            .addOnSuccessListener { languageCode ->
                if (languageCode == "und") {
                    binding.langIDTextView.text = "Cannot Determine Language: Make sure you have selected the correct language to detect."
                    binding.langIDContainer.visibility = View.VISIBLE
                    binding.translatedTextContainer.visibility = View.INVISIBLE
                } else {
                    binding.langIDTextView.text = languageCode
                    binding.langIDContainer.visibility = View.VISIBLE

                    val options = TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.fromLanguageTag(languageCode).toString())
                        .setTargetLanguage(TranslateLanguage.ENGLISH)
                        .build()
                    val textTranslator = Translation.getClient(options)

                    var conditions = DownloadConditions.Builder()
                        .requireWifi()
                        .build()
                    textTranslator.downloadModelIfNeeded(conditions)
                        .addOnSuccessListener {
                            textTranslator.translate(textToTranslate)
                                .addOnSuccessListener { translatedText ->
                                    binding.translatedTextTextView.text = translatedText
                                    binding.translatedTextContainer.visibility = View.VISIBLE
                                    textTranslator.close()
                                }
                                .addOnFailureListener { exception ->
                                    binding.translatedTextTextView.text = "Cannot Translate"
                                    binding.translatedTextContainer.visibility = View.VISIBLE
                                    textTranslator.close()
                                }
                        }
                        .addOnFailureListener { exception ->
                            binding.translatedTextTextView.text = "Cannot Download Model"
                            binding.translatedTextContainer.visibility = View.VISIBLE
                        }

                }
            }
            .addOnFailureListener {
                // Model couldn’t be loaded or other internal error.
                // ...
            }

    }

}
