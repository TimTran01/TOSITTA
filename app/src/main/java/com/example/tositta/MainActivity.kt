package com.example.tositta

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
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
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var lastBitmap: Bitmap? = null

    // Store language selections as strings for clarity
    var selectedLanguage: String = "English (Default)"
    var outputLanguage: String = "English"

    // Handle image picking result
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult

            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            lastBitmap = bitmap // Store the bitmap for later use

            binding.imageView.setImageBitmap(bitmap)
            runOCR(bitmap)
        }
    }

    // Launch image picker
    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPickImage.setOnClickListener {
            pickImageFromGallery()
        }

        // Create Spinner for selecting input language
        val spinnerInput = binding.selectInputLanguageSpinner
        val languagesInput = listOf("English (Default)", "Japanese", "Spanish", "French", "Korean", "Chinese")
        val adapterInput = ArrayAdapter(this, android.R.layout.simple_spinner_item, languagesInput)
        spinnerInput.adapter = adapterInput

        // Handle input language selection.
        // When a new language is selected, the translation will also be updated
        spinnerInput.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                selectedLanguage = parent?.getItemAtPosition(pos).toString()
                // If an image has already been selected, re-run OCR with the new language
                lastBitmap?.let {
                    runOCR(it)
                }
            }

            // Default is english
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedLanguage = "English (Default)"
            }
        }

        // Create Spinner for selecting output language
        val spinnerOutput = binding.selectOutputLanguageSpinner
        val languagesOutput = listOf("English", "Japanese", "Spanish", "French", "Korean", "Chinese")
        val adapterOutput = ArrayAdapter(this, android.R.layout.simple_spinner_item, languagesOutput)
        spinnerOutput.adapter = adapterOutput

        // Handle output language selection
        // When a new language is selected, the translation will also be updated
        spinnerOutput.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                outputLanguage = parent?.getItemAtPosition(pos).toString()
                val currentOcrText = binding.ocrResultTextView.text.toString()
                // If there's already OCR text, re-translate it with the new target language
                if (currentOcrText.isNotBlank() && !currentOcrText.startsWith("OCR Error")) {
                    runTranslation(currentOcrText)
                }
            }
            // Default is english
            override fun onNothingSelected(parent: AdapterView<*>?) {
                outputLanguage = "English"
            }
        }
    }


    // Run OCR on the selected image
    private fun runOCR(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = when (selectedLanguage) {
            "Japanese" -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
            "Korean" -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
            "Chinese" -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            else -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) // Latin, Spanish, French, etc.
        }

        // Two options for showing OCR results, one for successful and one unsuccessful
        recognizer.process(image)
            .addOnSuccessListener { result ->
                binding.ocrResultTextView.text = result.text
                binding.ocrResultContainer.visibility = View.VISIBLE
                runTranslation(result.text)
            }
            .addOnFailureListener { e ->
                binding.ocrResultTextView.text = "OCR Error: ${e.message}"
                binding.ocrResultContainer.visibility = View.VISIBLE
                // Hide translation sections on OCR error
                binding.langIDContainer.visibility = View.GONE
                binding.translatedTextContainer.visibility = View.GONE
            }
    }

    // Run translation on the OCR result
    private fun runTranslation(textToTranslate: String) {
        val languageIdentifier = LanguageIdentification.getClient()
        languageIdentifier.identifyLanguage(textToTranslate)
            .addOnSuccessListener { languageCode ->
                // If the language is not detected, show an error message
                if (languageCode == "und") {
                    binding.langIDTextView.text = "Cannot Determine Language: Make sure you have selected the correct language to detect."
                    binding.langIDContainer.visibility = View.VISIBLE
                    binding.translatedTextContainer.visibility = View.GONE
                }
                else {
                    // Store the selected language from the spinner
                    val spinnerLanguageCode = when(selectedLanguage) {
                        "Japanese" -> "ja"
                        "Spanish" -> "es"
                        "French" -> "fr"
                        "Korean" -> "ko"
                        "Chinese" -> "zh"
                        else -> "en" // Default to English
                    }

                    // If the detected language is different from the selected language, show a suggestion
                    var suggestionText = ""
                    if (languageCode != spinnerLanguageCode) {
                        val detectedLanguageName = when(languageCode) {
                            "ja" -> "Japanese"
                            "es" -> "Spanish"
                            "fr" -> "French"
                            "en" -> "English"
                            "ko" -> "Korean"
                            "zh" -> "Chinese"
                            else -> languageCode
                        }
                        suggestionText = "\n(Did you mean to select $detectedLanguageName?)"
                    }

                    binding.langIDTextView.text = "$languageCode$suggestionText"
                    binding.langIDContainer.visibility = View.VISIBLE

                    // Set the source and targe languages to translate
                    val sourceLanguage = TranslateLanguage.fromLanguageTag(languageCode)
                    val targetLanguage = when (outputLanguage) {
                        "Japanese" -> TranslateLanguage.JAPANESE
                        "Spanish" -> TranslateLanguage.SPANISH
                        "French" -> TranslateLanguage.FRENCH
                        "Korean" -> TranslateLanguage.KOREAN
                        "Chinese" -> TranslateLanguage.CHINESE
                        else -> TranslateLanguage.ENGLISH
                    }

                    // If the source language is not supported, show an error message
                    if (sourceLanguage == null) {
                        binding.translatedTextTextView.text = "Unsupported language for translation."
                        binding.translatedTextContainer.visibility = View.VISIBLE
                        return@addOnSuccessListener
                    }

                    // Translate the text
                    val options = TranslatorOptions.Builder()
                        .setSourceLanguage(sourceLanguage)
                        .setTargetLanguage(targetLanguage)
                        .build()
                    val textTranslator = Translation.getClient(options)

                    // Download the model if it's not already downloaded
                    val conditions = DownloadConditions.Builder().requireWifi().build()
                    textTranslator.downloadModelIfNeeded(conditions)
                        .addOnSuccessListener {
                            textTranslator.translate(textToTranslate)
                                .addOnSuccessListener { translatedText ->
                                    binding.translatedTextTextView.text = translatedText
                                    binding.translatedTextContainer.visibility = View.VISIBLE
                                    textTranslator.close()
                                }
                                .addOnFailureListener { exception ->
                                    binding.translatedTextTextView.text = "Cannot Translate: ${exception.message}"
                                    binding.translatedTextContainer.visibility = View.VISIBLE
                                    textTranslator.close()
                                }
                        }
                        .addOnFailureListener { exception ->
                            binding.translatedTextTextView.text = "Cannot Download Model: ${exception.message}"
                            binding.translatedTextContainer.visibility = View.VISIBLE
                        }
                }
            }
            .addOnFailureListener {
                binding.langIDTextView.text = "Language identification failed."
                binding.langIDContainer.visibility = View.VISIBLE
                binding.translatedTextContainer.visibility = View.GONE
            }
    }
}
