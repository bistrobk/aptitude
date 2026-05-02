package com.griffith.chatbot.viewModel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.griffith.chatbot.data.remote.Message
import com.griffith.chatbot.data.remote.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.InputStream


class ChatViewModel : ViewModel() {

    val chatMessages = mutableStateListOf<Message>()


    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    private var currentModuleId: String = ""
    private var currentLectureId: String = ""

    // Enhanced state management for file processing
    var summaryText by mutableStateOf("")
        private set

    var keywordsText by mutableStateOf("")
        private set

    var pendingFileUri by mutableStateOf<Uri?>(null)
        private set

    var isProcessingFile by mutableStateOf(false)
        private set

    var uploadedFileName by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf("")
        private set

    // File processing methods
    fun setPendingFile(uri: Uri) {
        pendingFileUri = uri
    }

    fun clearPendingFileUri() {
        pendingFileUri = null
    }

    fun clearError() {
        errorMessage = ""
    }

    private fun addSystemMessage(message: String) {
        val systemMessage = Message("system", message)
        chatMessages.add(systemMessage)
        saveMessage(systemMessage)
    }

    // Modules
    fun createModule(moduleName: String, onComplete: (Boolean) -> Unit = {}) {
        val userId = auth.currentUser?.uid ?: run {
            onComplete(false)
            return
        }
        val moduleRef = database.child("users/$userId/modules").push()
        currentModuleId = moduleRef.key ?: run {
            onComplete(false)
            return
        }
        val meta = mapOf("name" to moduleName, "timestamp" to System.currentTimeMillis())
        moduleRef.child("meta").setValue(meta)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener {
                errorMessage = "Failed to create module: ${it.message}"
                onComplete(false)
            }
    }

    fun loadModules(onResult: (List<Pair<String, String>>) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            onResult(emptyList())
            return
        }
        database.child("users/$userId/modules").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val modules = snapshot.children.mapNotNull {
                    val id = it.key
                    val name = it.child("meta/name").getValue(String::class.java)
                    if (id != null && name != null) Pair(id, name) else null
                }
                onResult(modules)
            }

            override fun onCancelled(error: DatabaseError) {
                errorMessage = "Failed to load modules: ${error.message}"
                onResult(emptyList())
            }
        })
    }

    // Lectures
    private fun createLecture(moduleId: String, meta: Map<String, Any>, onComplete: (String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            onComplete(null)
            return
        }
        val lectureRef = database.child("users/$userId/modules/$moduleId/lectures").push()
        currentLectureId = lectureRef.key ?: run {
            onComplete(null)
            return
        }
        lectureRef.child("meta").setValue(meta)
            .addOnSuccessListener { onComplete(currentLectureId) }
            .addOnFailureListener {
                errorMessage = "Failed to create lecture: ${it.message}"
                onComplete(null)
            }
    }

    fun loadLectureChat(moduleId: String, lectureId: String) {
        val userId = auth.currentUser?.uid ?: return
        currentModuleId = moduleId
        currentLectureId = lectureId
        chatMessages.clear()

        // Load chat messages
        database.child("users/$userId/modules/$moduleId/lectures/$lectureId/chat/messages")
            .orderByChild("timestamp")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (snap in snapshot.children) {
                        val role = snap.child("role").getValue(String::class.java) ?: continue
                        val content = snap.child("content").getValue(String::class.java) ?: continue
                        chatMessages.add(Message(role, content))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    errorMessage = "Failed to load chat: ${error.message}"
                }
            })

        // Load existing summary and keywords
        loadSummaryAndKeywords(moduleId, lectureId)
    }

    private fun loadSummaryAndKeywords(moduleId: String, lectureId: String) {
        val userId = auth.currentUser?.uid ?: return

        // Load summary
        database.child("users/$userId/modules/$moduleId/lectures/$lectureId/summary")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    summaryText = snapshot.child("content").getValue(String::class.java) ?: ""
                }
                override fun onCancelled(error: DatabaseError) {
                    // Silently handle - summary may not exist yet
                }
            })

        // Load keywords
        database.child("users/$userId/modules/$moduleId/lectures/$lectureId/keywords")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    keywordsText = snapshot.child("content").getValue(String::class.java) ?: ""
                }
                override fun onCancelled(error: DatabaseError) {
                    // Silently handle - keywords may not exist yet
                }
            })
    }

    private fun saveMessage(message: Message) {
        val userId = auth.currentUser?.uid ?: return
        val path = "users/$userId/modules/$currentModuleId/lectures/$currentLectureId/chat/messages"
        val data = mapOf("role" to message.role, "content" to message.content, "timestamp" to System.currentTimeMillis())
        database.child(path).push().setValue(data)
            .addOnFailureListener {
                errorMessage = "Failed to save message: ${it.message}"
            }
    }

    fun sendMessage(input: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userMsg = Message("user", input)
                chatMessages.add(userMsg)
                saveMessage(userMsg)

                // Include context from summary and keywords if available
                val contextualMessages = mutableListOf<Map<String, String>>()

                // Add system context if summary exists
                if (summaryText.isNotEmpty()) {
                    contextualMessages.add(
                        mapOf(
                            "role" to "system",
                            "content" to "Context - Lecture Summary: $summaryText"
                        )
                    )
                }

                // Add keywords context if available
                if (keywordsText.isNotEmpty()) {
                    contextualMessages.add(
                        mapOf(
                            "role" to "system",
                            "content" to "Context - Key Topics: $keywordsText"
                        )
                    )
                }

                // Add chat history
                contextualMessages.addAll(
                    chatMessages.takeLast(10).map { // Limit context to last 10 messages
                        mapOf("role" to it.role, "content" to it.content)
                    }
                )

                val request = mapOf(
                    "model" to "gpt-4o",
                    "messages" to contextualMessages
                )

                val response = RetrofitInstance.api.sendVisionMessage(apiKey, request)
                val reply = response.choices.firstOrNull()?.message ?: Message("assistant", "No response from GPT-4o.")
                chatMessages.add(reply)
                saveMessage(reply)
            } catch (e: Exception) {
                val errorMessage = Message("assistant", "❌ Error: ${e.localizedMessage}")
                chatMessages.add(errorMessage)
                saveMessage(errorMessage)
            } finally {
                onComplete()
            }
        }
    }

    // ✅ FIXED: Enhanced file upload with proper callback signature
    fun uploadLectureFile(moduleId: String, lectureId: String, uri: Uri, context: Context, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                isProcessingFile = true
                val fileName = getFileName(context, uri)
                uploadedFileName = fileName

                // Check if it's an image file and use OCR
                if (isImageFile(fileName)) {
                    uploadImageWithOCR(moduleId, lectureId, uri, context, onComplete)
                    return@launch
                }

                // Extract content based on file type
                val fileContent = extractFileContent(context, uri, fileName)

                if (fileContent.isBlank() || fileContent.contains("Error", ignoreCase = true)) {
                    onComplete(false, "Unable to extract content from file: $fileContent")
                    return@launch
                }

                // ✅ FIXED: Upload file to Firebase Storage using putFile() instead of putStream()
                val userId = auth.currentUser?.uid ?: run {
                    onComplete(false, "User not authenticated")
                    return@launch
                }

                val filePath = "lectures/$userId/$moduleId/$lectureId/${System.currentTimeMillis()}_$fileName"
                val storageRef = storage.child(filePath)

                try {
                    // Use putFile() which handles the stream lifecycle properly
                    val uploadTask = storageRef.putFile(uri).await()

                    // Save file metadata
                    val fileData = mapOf(
                        "name" to fileName,
                        "path" to filePath,
                        "timestamp" to System.currentTimeMillis(),
                        "size" to fileContent.length,
                        "type" to getFileType(fileName)
                    )
                    database.child("users/$userId/modules/$moduleId/lectures/$lectureId/files")
                        .push().setValue(fileData)
                        .await() // Add await for proper async handling

                    // Generate summary and keywords
                    generateSummaryAndKeywords(moduleId, lectureId, fileContent, fileName)
                    onComplete(true, "File processed successfully")

                } catch (uploadException: Exception) {
                    errorMessage = "Failed to upload file: ${uploadException.message}"
                    onComplete(false, "Failed to upload file: ${uploadException.message}")
                }

            } catch (e: Exception) {
                onComplete(false, "Error processing file: ${e.localizedMessage}")
            } finally {
                isProcessingFile = false
            }
        }
    }

    // File type detection helpers
    private fun isImageFile(fileName: String): Boolean {
        return fileName.matches(Regex(".*\\.(jpg|jpeg|png|gif|bmp|webp|tiff|tif)$", RegexOption.IGNORE_CASE))
    }

    private fun isPdfFile(fileName: String): Boolean {
        return fileName.endsWith(".pdf", ignoreCase = true)
    }

    private fun isWordFile(fileName: String): Boolean {
        return fileName.matches(Regex(".*\\.(doc|docx)$", RegexOption.IGNORE_CASE))
    }

    private fun getFileType(fileName: String): String {
        return when {
            fileName.endsWith(".pdf", ignoreCase = true) -> "pdf"
            fileName.endsWith(".docx", ignoreCase = true) -> "docx"
            fileName.endsWith(".doc", ignoreCase = true) -> "doc"
            fileName.endsWith(".txt", ignoreCase = true) -> "text"
            fileName.endsWith(".csv", ignoreCase = true) -> "csv"
            fileName.endsWith(".rtf", ignoreCase = true) -> "rtf"
            isImageFile(fileName) -> "image"
            else -> "unknown"
        }
    }

    // ✅ FIXED: Batch file processing with proper error handling
    fun uploadMultipleFiles(moduleId: String, lectureId: String, uris: List<Uri>, context: Context, onProgress: (Int, Int) -> Unit, onComplete: (Int, Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var successCount = 0
            var totalFiles = uris.size

            uris.forEachIndexed { index, uri ->
                uploadLectureFile(moduleId, lectureId, uri, context) { success, message ->
                    if (success) {
                        successCount++
                    } else {
                        // Handle error message properly
                        errorMessage = message
                    }
                    onProgress(index + 1, totalFiles)

                    if (index == uris.size - 1) {
                        onComplete(successCount, totalFiles)
                    }
                }
            }
        }
    }

    // Enhanced file content extraction
    private fun extractFileContent(context: Context, uri: Uri, fileName: String): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                when {
                    fileName.endsWith(".txt", ignoreCase = true) -> {
                        extractTextFile(stream)
                    }
                    fileName.endsWith(".pdf", ignoreCase = true) -> {
                        extractPdfContent(stream)
                    }
                    fileName.endsWith(".docx", ignoreCase = true) -> {
                        extractDocxContent(stream)
                    }
                    fileName.endsWith(".doc", ignoreCase = true) -> {
                        extractDocContent(stream)
                    }
                    fileName.matches(Regex(".*\\.(jpg|jpeg|png|gif|bmp|webp)$", RegexOption.IGNORE_CASE)) -> {
                        extractImageText(context, uri)
                    }
                    fileName.endsWith(".csv", ignoreCase = true) -> {
                        extractCsvContent(stream)
                    }
                    fileName.endsWith(".rtf", ignoreCase = true) -> {
                        extractRtfContent(stream)
                    }
                    else -> {
                        // Try to read as plain text
                        try {
                            stream.bufferedReader().readText()
                        } catch (e: Exception) {
                            "Unable to extract text from this file type: ${e.message}"
                        }
                    }
                }
            } ?: "Unable to read file"
        } catch (e: Exception) {
            "Error reading file: ${e.localizedMessage}"
        }
    }

    // Text file extraction
    private fun extractTextFile(inputStream: InputStream): String {
        return try {
            inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            "Error reading text file: ${e.message}"
        }
    }

    // PDF text extraction using iText
    private fun extractPdfContent(inputStream: InputStream): String {
        return try {
            // Using iText 7 for PDF extraction
            val pdfDocument = com.itextpdf.kernel.pdf.PdfDocument(
                com.itextpdf.kernel.pdf.PdfReader(inputStream)
            )
            val strategy = com.itextpdf.kernel.pdf.canvas.parser.listener.SimpleTextExtractionStrategy()
            val text = StringBuilder()

            for (i in 1..pdfDocument.numberOfPages) {
                val page = pdfDocument.getPage(i)
                val pageText = com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(page, strategy)
                text.append(pageText).append("\n")
            }

            pdfDocument.close()
            text.toString().trim()
        } catch (e: Exception) {
            "Error extracting PDF content: ${e.message}. Please ensure the PDF is not password protected or corrupted."
        }
    }

    // DOCX extraction using Apache POI
    private fun extractDocxContent(inputStream: InputStream): String {
        return try {
            val document = org.apache.poi.xwpf.usermodel.XWPFDocument(inputStream)
            val extractor = org.apache.poi.xwpf.extractor.XWPFWordExtractor(document)
            val text = extractor.text
            extractor.close()
            document.close()
            text
        } catch (e: Exception) {
            "Error extracting DOCX content: ${e.message}"
        }
    }

    // DOC extraction using Apache POI
    private fun extractDocContent(inputStream: InputStream): String {
        return try {
            val document = org.apache.poi.hwpf.HWPFDocument(inputStream)
            val extractor = org.apache.poi.hwpf.extractor.WordExtractor(document)
            val text = extractor.text
            extractor.close()
            document.close()
            text
        } catch (e: Exception) {
            "Error extracting DOC content: ${e.message}"
        }
    }

    // CSV content extraction
    private fun extractCsvContent(inputStream: InputStream): String {
        return try {
            val reader = inputStream.bufferedReader()
            val lines = reader.readLines()
            reader.close()

            // Format CSV data into readable text
            val stringBuilder = StringBuilder()
            lines.forEachIndexed { index, line ->
                if (index == 0) {
                    stringBuilder.append("Headers: $line\n\n")
                } else {
                    stringBuilder.append("Row ${index}: $line\n")
                }
            }
            stringBuilder.toString()
        } catch (e: Exception) {
            "Error reading CSV file: ${e.message}"
        }
    }

    // RTF content extraction
    private fun extractRtfContent(inputStream: InputStream): String {
        return try {
            // Basic RTF extraction - removes RTF formatting codes
            val content = inputStream.bufferedReader().readText()

            // Remove RTF control codes and extract plain text
            var text = content
                .replace(Regex("\\{[^}]*\\}"), "") // Remove RTF groups
                .replace(Regex("\\\\[a-zA-Z]+\\d*"), "") // Remove RTF control words
                .replace("\\", "") // Remove remaining backslashes
                .replace(Regex("\\s+"), " ") // Normalize whitespace
                .trim()

            if (text.isBlank()) {
                "RTF file appears to be empty or contains only formatting codes"
            } else {
                text
            }
        } catch (e: Exception) {
            "Error extracting RTF content: ${e.message}"
        }
    }

    // OCR for image text extraction using ML Kit
    private fun extractImageText(context: Context, uri: Uri): String {
        return try {
            // Create InputImage from URI
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            // Since ML Kit is asynchronous, we'll use a different approach
            // For now, return placeholder and add async version
            "Image text extraction requires async processing. Use extractImageTextAsync() instead."

        } catch (e: Exception) {
            "Error processing image for text extraction: ${e.message}"
        }
    }

    // Async OCR implementation for images
    fun extractImageTextAsync(context: Context, uri: Uri, onResult: (String) -> Unit) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val extractedText = if (visionText.text.isNotBlank()) {
                        visionText.text
                    } else {
                        "No text found in the image."
                    }
                    onResult(extractedText)
                }
                .addOnFailureListener { e ->
                    onResult("OCR failed: ${e.message}")
                }
        } catch (e: Exception) {
            onResult("Error processing image for text extraction: ${e.message}")
        }
    }

    // ✅ FIXED: Enhanced file upload with async OCR support and proper stream handling
    fun uploadImageWithOCR(moduleId: String, lectureId: String, uri: Uri, context: Context, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                isProcessingFile = true
                val fileName = getFileName(context, uri)
                uploadedFileName = fileName

                // For images, use async OCR
                if (fileName.matches(Regex(".*\\.(jpg|jpeg|png|gif|bmp|webp)$", RegexOption.IGNORE_CASE))) {
                    extractImageTextAsync(context, uri) { extractedText ->
                        viewModelScope.launch(Dispatchers.IO) {
                            if (extractedText.isNotBlank() && !extractedText.contains("failed", ignoreCase = true)) {
                                // ✅ FIXED: Upload file to Firebase Storage using putFile()
                                val userId = auth.currentUser?.uid ?: run {
                                    onComplete(false, "User not authenticated")
                                    return@launch
                                }

                                val filePath = "lectures/$userId/$moduleId/$lectureId/${System.currentTimeMillis()}_$fileName"
                                val storageRef = storage.child(filePath)

                                try {
                                    // Use putFile() instead of putStream() with manual stream management
                                    val uploadTask = storageRef.putFile(uri).await()

                                    val fileData = mapOf(
                                        "name" to fileName,
                                        "path" to filePath,
                                        "timestamp" to System.currentTimeMillis(),
                                        "size" to extractedText.length,
                                        "type" to "image_with_text"
                                    )
                                    database.child("users/$userId/modules/$moduleId/lectures/$lectureId/files")
                                        .push().setValue(fileData)
                                        .await() // Add await for proper async handling

                                    // Generate summary and keywords from extracted text
                                    generateSummaryAndKeywords(moduleId, lectureId, extractedText, fileName)
                                    onComplete(true, "Image processed successfully with OCR")

                                } catch (uploadException: Exception) {
                                    errorMessage = "Failed to upload image: ${uploadException.message}"
                                    onComplete(false, "Failed to upload image: ${uploadException.message}")
                                }
                            } else {
                                onComplete(false, "Failed to extract text from image: $extractedText")
                            }
                            isProcessingFile = false
                        }
                    }
                } else {
                    // Handle non-image files
                    val fileContent = extractFileContent(context, uri, fileName)
                    if (fileContent.isNotBlank()) {
                        generateSummaryAndKeywords(moduleId, lectureId, fileContent, fileName)
                        onComplete(true, "File processed successfully")
                    } else {
                        onComplete(false, "Unable to extract content from file")
                    }
                    isProcessingFile = false
                }

            } catch (e: Exception) {
                onComplete(false, "Error processing file: ${e.localizedMessage}")
                isProcessingFile = false
            }
        }
    }

    // Get file name from URI
    private fun getFileName(context: Context, uri: Uri): String {
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && nameIndex >= 0) {
                it.getString(nameIndex) ?: "unknown_file"
            } else {
                "unknown_file"
            }
        } ?: "unknown_file"
    }

    // Enhanced summary and keyword generation
    private fun generateSummaryAndKeywords(moduleId: String, lectureId: String, content: String, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Generate summary
                val summaryRequest = mapOf(
                    "model" to "gpt-4o",
                    "messages" to listOf(
                        mapOf(
                            "role" to "system",
                            "content" to "You are an educational AI assistant. Create a comprehensive summary of the uploaded lecture material. Focus on key concepts, important points, and main takeaways. Make it clear and educational."
                        ),
                        mapOf(
                            "role" to "user",
                            "content" to "Please summarize this lecture content from file '$fileName':\n\n$content"
                        )
                    )
                )

                val summaryResponse = RetrofitInstance.api.sendVisionMessage(apiKey, summaryRequest)
                val generatedSummary = summaryResponse.choices.firstOrNull()?.message?.content ?: ""

                // Generate keywords
                val keywordsRequest = mapOf(
                    "model" to "gpt-4o",
                    "messages" to listOf(
                        mapOf(
                            "role" to "system",
                            "content" to "Extract the most important keywords and key phrases from the lecture content. Return only a comma-separated list of 8-12 relevant terms and concepts."
                        ),
                        mapOf(
                            "role" to "user",
                            "content" to "Extract keywords from this lecture content:\n\n$content"
                        )
                    )
                )

                val keywordsResponse = RetrofitInstance.api.sendVisionMessage(apiKey, keywordsRequest)
                val generatedKeywords = keywordsResponse.choices.firstOrNull()?.message?.content ?: ""

                // Update local state
                summaryText = generatedSummary
                keywordsText = generatedKeywords

                // Save to Firebase
                val userId = auth.currentUser?.uid ?: return@launch

                val summaryData = mapOf(
                    "content" to generatedSummary,
                    "source_file" to fileName,
                    "timestamp" to System.currentTimeMillis()
                )

                val keywordsData = mapOf(
                    "content" to generatedKeywords,
                    "source_file" to fileName,
                    "timestamp" to System.currentTimeMillis()
                )

                database.child("users/$userId/modules/$moduleId/lectures/$lectureId/summary")
                    .setValue(summaryData)
                    .addOnFailureListener {
                        errorMessage = "Failed to save summary: ${it.message}"
                    }

                database.child("users/$userId/modules/$moduleId/lectures/$lectureId/keywords")
                    .setValue(keywordsData)
                    .addOnFailureListener {
                        errorMessage = "Failed to save keywords: ${it.message}"
                    }

                // Add success message to chat
                addSystemMessage("✅ Generated summary and extracted keywords from '$fileName'")

            } catch (e: Exception) {
                errorMessage = "Failed to process '$fileName': ${e.localizedMessage}"
                addSystemMessage("❌ Failed to process '$fileName': ${e.localizedMessage}")
            }
        }
    }

    // Utility methods
    fun updateModule(moduleId: String, newName: String, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            onComplete(false)
            return
        }
        database.child("users/$userId/modules/$moduleId/meta/name").setValue(newName)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener {
                errorMessage = "Failed to update module: ${it.message}"
                onComplete(false)
            }
    }

    fun deleteModule(moduleId: String, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            onComplete(false)
            return
        }
        database.child("users/$userId/modules/$moduleId").removeValue()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener {
                errorMessage = "Failed to delete module: ${it.message}"
                onComplete(false)
            }
    }

    fun getLectureListForModule(moduleId: String, onResult: (List<Pair<String, String>>) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            onResult(emptyList())
            return
        }
        database.child("users").child(userId).child("modules").child(moduleId).child("lectures")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lectures = snapshot.children.mapNotNull {
                        val id = it.key
                        val title = it.child("meta").child("title").getValue(String::class.java)
                        if (id != null && title != null) Pair(id, title) else null
                    }
                    onResult(lectures)
                }

                override fun onCancelled(error: DatabaseError) {
                    errorMessage = "Failed to load lectures: ${error.message}"
                    onResult(emptyList())
                }
            })
    }

    fun createLecture(moduleId: String, title: String, onComplete: (Boolean) -> Unit = {}) {
        val meta = mapOf(
            "title" to title,
            "timestamp" to System.currentTimeMillis()
        )
        createLecture(moduleId, meta) { lectureId ->
            onComplete(lectureId != null)
        }
    }

    // Manual content processing (for direct text input)
    fun generateSummaryFromContent(content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                isProcessingFile = true

                val summaryRequest = mapOf(
                    "model" to "gpt-4o",
                    "messages" to listOf(
                        mapOf(
                            "role" to "system",
                            "content" to "Create a comprehensive summary and extract 8-12 key terms from the following content. Format your response as 'SUMMARY: [summary content] KEYWORDS: [comma-separated keywords]'"
                        ),
                        mapOf("role" to "user", "content" to content)
                    )
                )

                val response = RetrofitInstance.api.sendVisionMessage(apiKey, summaryRequest)
                val result = response.choices.firstOrNull()?.message?.content ?: "No summary generated."

                // Parse the response
                val summaryMatch = Regex("SUMMARY:\\s*(.+?)(?=KEYWORDS:|$)", RegexOption.DOT_MATCHES_ALL)
                    .find(result)
                val keywordsMatch = Regex("KEYWORDS:\\s*(.+)", RegexOption.DOT_MATCHES_ALL)
                    .find(result)

                summaryText = summaryMatch?.groupValues?.get(1)?.trim()
                    ?: result.split("Keywords:", ignoreCase = true).getOrNull(0)?.trim()
                            ?: result

                keywordsText = keywordsMatch?.groupValues?.get(1)?.trim()
                    ?: result.split("Keywords:", ignoreCase = true).getOrNull(1)?.trim()
                            ?: ""

            } catch (e: Exception) {
                errorMessage = "Failed to summarize: ${e.localizedMessage}"
                summaryText = "❌ Failed to summarize: ${e.localizedMessage}"
                keywordsText = ""
            } finally {
                isProcessingFile = false
            }
        }
    }

    // Clear current session data
    fun clearCurrentSession() {
        summaryText = ""
        keywordsText = ""
        uploadedFileName = null
        pendingFileUri = null
        chatMessages.clear()
        errorMessage = ""
    }

    // Clear uploaded file data
    fun clearUploadedFile() {
        uploadedFileName = null
        summaryText = ""
        keywordsText = ""
        pendingFileUri = null
    }

    // Generate quiz from current summary
    fun generateQuiz(onResult: (String) -> Unit) {
        if (summaryText.isEmpty()) {
            onResult("No summary available to generate quiz from.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val quizRequest = mapOf(
                    "model" to "gpt-4o",
                    "messages" to listOf(
                        mapOf(
                            "role" to "system",
                            "content" to "Generate 5 multiple choice questions based on the provided summary. Format each question with 4 options (A, B, C, D) and indicate the correct answer."
                        ),
                        mapOf(
                            "role" to "user",
                            "content" to "Generate quiz questions from this summary:\n\n$summaryText"
                        )
                    )
                )

                val response = RetrofitInstance.api.sendVisionMessage(apiKey, quizRequest)
                val quiz = response.choices.firstOrNull()?.message?.content ?: "Failed to generate quiz."
                onResult(quiz)

            } catch (e: Exception) {
                onResult("❌ Error generating quiz: ${e.localizedMessage}")
            }
        }
    }

    // Generate flashcards from keywords
    fun generateFlashcards(onResult: (String) -> Unit) {
        if (keywordsText.isEmpty()) {
            onResult("No keywords available to generate flashcards from.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val flashcardRequest = mapOf(
                    "model" to "gpt-4o",
                    "messages" to listOf(
                        mapOf(
                            "role" to "system",
                            "content" to "Create flashcards for the provided keywords. For each keyword, provide a clear definition or explanation. Format as 'TERM: [keyword] DEFINITION: [explanation]'"
                        ),
                        mapOf(
                            "role" to "user",
                            "content" to "Create flashcards for these keywords:\n\n$keywordsText"
                        )
                    )
                )

                val response = RetrofitInstance.api.sendVisionMessage(apiKey, flashcardRequest)
                val flashcards = response.choices.firstOrNull()?.message?.content ?: "Failed to generate flashcards."
                onResult(flashcards)

            } catch (e: Exception) {
                onResult("❌ Error generating flashcards: ${e.localizedMessage}")
            }
        }
    }
}