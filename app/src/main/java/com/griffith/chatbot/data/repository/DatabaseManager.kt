package com.griffith.chatbot.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.griffith.chatbot.data.models.*
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

// Data classes for encrypted chat
data class ChatMessage(
    val id: String = "",
    val lectureId: String = "",
    val content: String = "", // This will be encrypted
    val role: String = "user", // "user" or "assistant"
    val timestamp: Long = System.currentTimeMillis(),
    val isEncrypted: Boolean = true
)

data class LectureFile(
    val id: String = "",
    val lectureId: String = "",
    val fileName: String = "",
    val fileUrl: String = "",
    val fileType: String = "", // "pdf", "image", "document"
    val extractedText: String = "", // This will be encrypted
    val summary: String = "", // This will be encrypted
    val uploadTime: Long = System.currentTimeMillis(),
    val isProcessed: Boolean = false
)

// Simple Profile Data Class for Firebase
data class UserProfile(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val profileImageUrl: String = "",
    val theme: String = "system", // "light", "dark", "system"
    val notificationsEnabled: Boolean = true,
    val studyReminders: Boolean = true,
    val emailNotifications: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

class DatabaseManager private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: DatabaseManager? = null

        fun getInstance(): DatabaseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseManager().also { INSTANCE = it }
            }
        }
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Encryption key
    private val encryptionKey = "MySecretKey12345" // 16 bytes for AES-128

    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    // --- Helper function to get the base collection for a user ---
    private fun getUserBaseCollection() = firestore.collection("users")
        .document(getCurrentUserId() ?: "")

    // --- Helper function to get the lectures subcollection for a given module ---
    private fun getModuleLecturesCollection(moduleId: String) =
        getUserBaseCollection()
            .collection("modules")
            .document(moduleId)
            .collection("lectures")

    // ---  Helper function to get the top-level chat messages collection ---
    private fun getUserChatMessagesCollection() =
        getUserBaseCollection().collection("chatMessages")

    // ---  Helper function to get the top-level lecture files collection ---
    private fun getUserLectureFilesCollection() =
        getUserBaseCollection().collection("lectureFiles")

    // --- Helper function to get the top-level study events collection ---
    private fun getUserStudyEventsCollection() =
        getUserBaseCollection().collection("studyEvents")

    // --- Helper function to get the user profile document ---
    private fun getUserProfileDocument() =
        getUserBaseCollection()

    // Encryption/Decryption methods
    private fun encrypt(text: String): String {
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val secretKey = SecretKeySpec(encryptionKey.toByteArray(), "AES")

            // Generate random IV
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encryptedBytes = cipher.doFinal(text.toByteArray())

            // Combine IV + encrypted data
            val combined = iv + encryptedBytes
            Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Encryption failed", e)
            text // Return original text if encryption fails
        }
    }

    private fun decrypt(encryptedText: String): String {
        return try {
            val combined = Base64.decode(encryptedText, Base64.DEFAULT)

            // Extract IV (first 16 bytes) and encrypted data
            val iv = combined.sliceArray(0..15)
            val encryptedData = combined.sliceArray(16 until combined.size)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val secretKey = SecretKeySpec(encryptionKey.toByteArray(), "AES")
            val ivSpec = IvParameterSpec(iv)

            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            val decryptedBytes = cipher.doFinal(encryptedData)

            String(decryptedBytes)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Decryption failed", e)
            encryptedText // Return encrypted text if decryption fails
        }
    }

    // === PROFILE MANAGEMENT METHODS ===

    suspend fun getUserProfile(): Result<UserProfile?> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            val doc = getUserProfileDocument().get().await()

            if (doc.exists()) {
                val profileData = doc.data
                val profile = if (profileData != null) {
                    UserProfile(
                        id = userId,
                        name = profileData["name"] as? String ?: "",
                        phoneNumber = profileData["phoneNumber"] as? String ?: "",
                        profileImageUrl = profileData["profileImageUrl"] as? String ?: "",
                        theme = profileData["theme"] as? String ?: "system",
                        notificationsEnabled = profileData["notificationsEnabled"] as? Boolean
                            ?: true,
                        studyReminders = profileData["studyReminders"] as? Boolean ?: true,
                        emailNotifications = profileData["emailNotifications"] as? Boolean ?: true,
                        createdAt = profileData["createdAt"] as? Long ?: System.currentTimeMillis(),
                        updatedAt = profileData["updatedAt"] as? Long ?: System.currentTimeMillis()
                    )
                } else {
                    null
                }

                Log.d("DatabaseManager", "Loaded user profile: ${profile?.name}")
                Result.success(profile)
            } else {
                Log.d("DatabaseManager", "No profile found for user")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error loading user profile", e)
            Result.failure(e)
        }
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            val profileData = mapOf(
                "name" to profile.name,
                "phoneNumber" to profile.phoneNumber,
                "profileImageUrl" to profile.profileImageUrl,
                "theme" to profile.theme,
                "notificationsEnabled" to profile.notificationsEnabled,
                "studyReminders" to profile.studyReminders,
                "emailNotifications" to profile.emailNotifications,
                "createdAt" to profile.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )

            getUserProfileDocument().set(
                profileData,
                com.google.firebase.firestore.SetOptions.merge()
            ).await()

            Log.d("DatabaseManager", "User profile saved: ${profile.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error saving user profile", e)
            Result.failure(e)
        }
    }

    suspend fun updateUserTheme(theme: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            getUserProfileDocument()
                .update(
                    mapOf(
                        "theme" to theme,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()

            Log.d("DatabaseManager", "User theme updated: $theme")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error updating user theme", e)
            Result.failure(e)
        }
    }

    suspend fun updateNotificationSettings(
        notificationsEnabled: Boolean,
        studyReminders: Boolean,
        emailNotifications: Boolean
    ): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            getUserProfileDocument()
                .update(
                    mapOf(
                        "notificationsEnabled" to notificationsEnabled,
                        "studyReminders" to studyReminders,
                        "emailNotifications" to emailNotifications,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()

            Log.d("DatabaseManager", "Notification settings updated")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error updating notification settings", e)
            Result.failure(e)
        }
    }

    suspend fun uploadProfileImage(imageUri: Uri): Result<String> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            // Create unique file path for profile image
            val timestamp = System.currentTimeMillis()
            val fileName = "profile_${userId}_$timestamp.jpg"
            val filePath = "profiles/$userId/$fileName"

            // Upload to Firebase Storage
            val storageRef = storage.reference.child(filePath)
            val uploadTask = storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await()

            // Update profile with new image URL
            getUserProfileDocument()
                .update(
                    mapOf(
                        "profileImageUrl" to downloadUrl.toString(),
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()

            Log.d("DatabaseManager", "Profile image uploaded: $downloadUrl")
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error uploading profile image", e)
            Result.failure(e)
        }
    }

    suspend fun deleteUserProfile(): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            // Delete profile image from storage if exists
            val profile = getUserProfile().getOrNull()
            profile?.profileImageUrl?.takeIf { it.isNotEmpty() }?.let { imageUrl ->
                try {
                    val storageRef = storage.getReferenceFromUrl(imageUrl)
                    storageRef.delete().await()
                } catch (e: Exception) {
                    Log.w("DatabaseManager", "Could not delete profile image from storage", e)
                }
            }

            // Delete entire user document (this will cascade delete all subcollections)
            getUserProfileDocument().delete().await()

            Log.d("DatabaseManager", "User profile deleted")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error deleting user profile", e)
            Result.failure(e)
        }
    }

    suspend fun signOutUser(): Result<Unit> {
        return try {
            auth.signOut()
            Log.d("DatabaseManager", "User signed out")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error signing out user", e)
            Result.failure(e)
        }
    }

    // Helper method to initialize default profile for new users
    suspend fun initializeDefaultProfile(): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            val existingProfile = getUserProfile().getOrNull()
            if (existingProfile == null) {
                val defaultProfile = UserProfile(
                    id = userId,
                    name = "",
                    phoneNumber = "",
                    profileImageUrl = "",
                    theme = "system",
                    notificationsEnabled = true,
                    studyReminders = true,
                    emailNotifications = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                saveUserProfile(defaultProfile)
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error initializing default profile", e)
            Result.failure(e)
        }
    }

    // === EXISTING MODULE METHODS ===

    suspend fun createModule(module: Module): Result<String> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            val moduleData = module.copy(id = "")
            val docRef = getUserBaseCollection().collection("modules").add(moduleData).await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error creating module", e)
            Result.failure(e)
        }
    }

    suspend fun getModules(): Result<List<Module>> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            val snapshot = getUserBaseCollection().collection("modules")
                .orderBy("name")
                .get()
                .await()

            val modules = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data
                    if (data != null) {
                        val module = doc.toObject(Module::class.java)?.copy(id = doc.id)

                        if (module == null) {
                            val safeModule = Module(
                                id = doc.id,
                                name = data["name"] as? String ?: "",
                                moduleCode = data["moduleCode"] as? String ?: "",
                                lecturerName = data["lecturerName"] as? String ?: "",
                                credits = (data["credits"] as? Long)?.toInt() ?: 0,
                                semester = data["semester"] as? String ?: "",
                                color = data["color"] as? String ?: "#6200EE",
                                description = data["description"] as? String ?: "",
                                totalLectures = (data["totalLectures"] as? Long)?.toInt() ?: 0,
                                completedLectures = (data["completedLectures"] as? Long)?.toInt()
                                    ?: 0,
                                createdAt = data["createdAt"] as? Long
                                    ?: System.currentTimeMillis(),
                                lastModified = data["lastModified"] as? Long
                                    ?: System.currentTimeMillis(),
                                isActive = data["isActive"] as? Boolean ?: true,
                                schedule = emptyList()
                            )
                            safeModule
                        } else {
                            module
                        }
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.w(
                        "DatabaseManager",
                        "Failed to parse module: ${doc.id}, using safe fallback",
                        e
                    )
                    val data = doc.data
                    if (data != null) {
                        Module(
                            id = doc.id,
                            name = data["name"] as? String ?: "Unknown Module",
                            moduleCode = data["moduleCode"] as? String ?: "",
                            lecturerName = data["lecturerName"] as? String ?: "",
                            credits = (data["credits"] as? Long)?.toInt() ?: 0,
                            semester = data["semester"] as? String ?: "",
                            color = data["color"] as? String ?: "#6200EE",
                            schedule = emptyList()
                        )
                    } else {
                        null
                    }
                }
            }

            Log.d("DatabaseManager", "Loaded ${modules.size} modules")
            Result.success(modules)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error loading modules", e)
            Result.failure(e)
        }
    }

    suspend fun updateModule(module: Module): Result<Unit> {
        return try {
            getUserBaseCollection().collection("modules")
                .document(module.id)
                .set(module)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error updating module", e)
            Result.failure(e)
        }
    }

    suspend fun deleteModule(moduleId: String): Result<Unit> {
        return try {
            // First delete all lectures in this module
            val lectures = getLecturesForModule(moduleId).getOrNull()
            lectures?.forEach { lecture ->
                deleteLecture(moduleId, lecture.id)
            }

            // Then delete the module
            getUserBaseCollection().collection("modules")
                .document(moduleId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error deleting module", e)
            Result.failure(e)
        }
    }

    // === LECTURE METHODS ===

    suspend fun createLecture(moduleId: String, lecture: Lecture): Result<String> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            val lectureData = lecture.copy(
                id = "",
                moduleId = moduleId,
                createdAt = System.currentTimeMillis(),
                lastModified = System.currentTimeMillis()
            )

            val docRef = getModuleLecturesCollection(moduleId).add(lectureData).await()

            Log.d(
                "DatabaseManager",
                "Lecture created successfully: ${docRef.id} under module $moduleId"
            )
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error creating lecture", e)
            Result.failure(e)
        }
    }

    suspend fun getLecturesForModule(moduleId: String): Result<List<Lecture>> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            val snapshot = getModuleLecturesCollection(moduleId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()

            val lectures = snapshot.documents.mapNotNull { doc ->
                try {
                    val lecture = doc.toObject(Lecture::class.java)?.copy(id = doc.id)
                    lecture
                } catch (e: Exception) {
                    Log.w("DatabaseManager", "Failed to parse lecture: ${doc.id}", e)
                    val data = doc.data
                    if (data != null) {
                        Lecture(
                            id = doc.id,
                            moduleId = data["moduleId"] as? String ?: moduleId,
                            title = data["title"] as? String ?: "Unknown Lecture",
                            description = data["description"] as? String ?: "",
                            tags = (data["tags"] as? List<*>)?.mapNotNull { it as? String }
                                ?: emptyList(),
                            createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
                            lastModified = data["lastModified"] as? Long
                                ?: System.currentTimeMillis()
                        )
                    } else {
                        null
                    }
                }
            }

            Log.d("DatabaseManager", "Loaded ${lectures.size} lectures for module $moduleId")
            Result.success(lectures)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error loading lectures", e)
            Result.failure(e)
        }
    }

    suspend fun getAllLectures(): Result<List<Lecture>> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            val modulesSnapshot = getUserBaseCollection().collection("modules").get().await()
            val allLectures = mutableListOf<Lecture>()

            for (moduleDoc in modulesSnapshot.documents) {
                val moduleId = moduleDoc.id
                val lecturesSnapshot = getModuleLecturesCollection(moduleId)
                    .orderBy("lastModified", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val lecturesInModule = lecturesSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Lecture::class.java)?.copy(id = doc.id)
                }
                allLectures.addAll(lecturesInModule)
            }

            Log.d("DatabaseManager", "Loaded ${allLectures.size} total lectures across all modules")
            Result.success(allLectures)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error loading all lectures", e)
            Result.failure(e)
        }
    }

    suspend fun getLecture(moduleId: String, lectureId: String): Result<Lecture> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            val doc = getModuleLecturesCollection(moduleId)
                .document(lectureId)
                .get()
                .await()

            if (doc.exists()) {
                val lecture = doc.toObject(Lecture::class.java)?.copy(id = doc.id)
                if (lecture != null) {
                    Log.d("DatabaseManager", "Loaded lecture: ${lecture.title}")
                    Result.success(lecture)
                } else {
                    Result.failure(Exception("Failed to parse lecture data"))
                }
            } else {
                Result.failure(Exception("Lecture not found"))
            }
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error loading lecture", e)
            Result.failure(e)
        }
    }

    suspend fun updateLecture(moduleId: String, lecture: Lecture): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            val updatedLecture = lecture.copy(
                lastModified = System.currentTimeMillis(),
                moduleId = moduleId
            )

            getModuleLecturesCollection(moduleId)
                .document(lecture.id)
                .set(updatedLecture)
                .await()

            Log.d("DatabaseManager", "Lecture updated: ${lecture.title}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error updating lecture", e)
            Result.failure(e)
        }
    }

    suspend fun deleteLecture(moduleId: String, lectureId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            // Delete all related chat messages
            val chatMessages = getChatMessages(lectureId).getOrNull()
            chatMessages?.forEach { message ->
                deleteChatMessage(message.id)
            }

            // Delete all related files
            val files = getLectureFiles(lectureId).getOrNull()
            files?.forEach { file ->
                deleteLectureFile(file.id)
            }

            // Delete lecture from module subcollection
            getModuleLecturesCollection(moduleId)
                .document(lectureId)
                .delete()
                .await()

            Log.d("DatabaseManager", "Lecture deleted: $lectureId from module $moduleId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error deleting lecture", e)
            Result.failure(e)
        }
    }

    suspend fun searchLectures(query: String): Result<List<Lecture>> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            val allLecturesResult = getAllLectures()
            val allLectures = allLecturesResult.getOrThrow()

            val filteredLectures = allLectures.filter { lecture ->
                lecture.title.contains(query, ignoreCase = true) ||
                        lecture.description.contains(query, ignoreCase = true) ||
                        lecture.tags.any { tag -> tag.contains(query, ignoreCase = true) }
            }

            Log.d("DatabaseManager", "Found ${filteredLectures.size} lectures matching '$query'")
            Result.success(filteredLectures)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error searching lectures", e)
            Result.failure(e)
        }
    }

    suspend fun getLecturesByTag(tag: String): Result<List<Lecture>> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            val allLecturesResult = getAllLectures()
            val allLectures = allLecturesResult.getOrThrow()

            val taggedLectures = allLectures.filter { lecture ->
                lecture.tags.any { it.contains(tag, ignoreCase = true) }
            }

            Log.d("DatabaseManager", "Found ${taggedLectures.size} lectures with tag '$tag'")
            Result.success(taggedLectures)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error loading lectures by tag", e)
            Result.failure(e)
        }
    }

    // === CHAT METHODS ===

    suspend fun saveChatMessage(
        lectureId: String,
        content: String,
        role: String
    ): Result<String> {
        return try {
            val encryptedContent = encrypt(content)
            val chatMessage = ChatMessage(
                lectureId = lectureId,
                content = encryptedContent,
                role = role,
                timestamp = System.currentTimeMillis(),
                isEncrypted = true
            )

            val docRef = getUserChatMessagesCollection().add(chatMessage).await()
            Log.d("DatabaseManager", "Chat message saved: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error saving chat message", e)
            Result.failure(e)
        }
    }

    private suspend fun getChatMessages(lectureId: String): Result<List<ChatMessage>> {
        return try {
            val snapshot = getUserChatMessagesCollection()
                .whereEqualTo("lectureId", lectureId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()

            val messages = snapshot.documents.mapNotNull { doc ->
                val message = doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                message?.let {
                    if (it.isEncrypted) {
                        it.copy(content = decrypt(it.content))
                    } else {
                        it
                    }
                }
            }

            Log.d("DatabaseManager", "Loaded ${messages.size} chat messages for lecture $lectureId")
            Result.success(messages)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error loading chat messages", e)
            Result.failure(e)
        }
    }

    private suspend fun deleteChatMessage(messageId: String): Result<Unit> {
        return try {
            getUserChatMessagesCollection()
                .document(messageId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error deleting chat message", e)
            Result.failure(e)
        }
    }

    // === FILE METHODS ===

    suspend fun uploadLectureFile(
        lectureId: String,
        fileUri: Uri,
        fileName: String,
        context: Context
    ): Result<String> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            val timestamp = System.currentTimeMillis()
            val fileExtension = fileName.substringAfterLast(".", "")
            val uniqueFileName = "${lectureId}_${timestamp}.$fileExtension"
            val filePath = "lectures/$userId/$lectureId/$uniqueFileName"

            val storageRef = storage.reference.child(filePath)
            val uploadTask = storageRef.putFile(fileUri).await()
            val downloadUrl = storageRef.downloadUrl.await()

            val fileType = when (fileExtension.lowercase()) {
                "pdf" -> "pdf"
                "jpg", "jpeg", "png", "gif" -> "image"
                else -> "document"
            }

            val lectureFile = LectureFile(
                lectureId = lectureId,
                fileName = fileName,
                fileUrl = downloadUrl.toString(),
                fileType = fileType,
                uploadTime = timestamp,
                isProcessed = false
            )

            val docRef = getUserLectureFilesCollection().add(lectureFile).await()

            Log.d("DatabaseManager", "File uploaded: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error uploading file", e)
            Result.failure(e)
        }
    }

    suspend fun saveLectureFileData(
        fileId: String,
        extractedText: String,
        summary: String
    ): Result<Unit> {
        return try {
            val encryptedText = encrypt(extractedText)
            val encryptedSummary = encrypt(summary)

            getUserLectureFilesCollection()
                .document(fileId)
                .update(
                    mapOf(
                        "extractedText" to encryptedText,
                        "summary" to encryptedSummary,
                        "isProcessed" to true
                    )
                )
                .await()

            Log.d("DatabaseManager", "File data saved for: $fileId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error saving file data", e)
            Result.failure(e)
        }
    }

    private suspend fun getLectureFiles(lectureId: String): Result<List<LectureFile>> {
        return try {
            val snapshot = getUserLectureFilesCollection()
                .whereEqualTo("lectureId", lectureId)
                .orderBy("uploadTime", Query.Direction.DESCENDING)
                .get()
                .await()

            val files = snapshot.documents.mapNotNull { doc ->
                val file = doc.toObject(LectureFile::class.java)?.copy(id = doc.id)
                file?.let {
                    it.copy(
                        extractedText = if (it.extractedText.isNotEmpty()) decrypt(it.extractedText) else "",
                        summary = if (it.summary.isNotEmpty()) decrypt(it.summary) else ""
                    )
                }
            }

            Log.d("DatabaseManager", "Loaded ${files.size} files for lecture $lectureId")
            Result.success(files)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error loading lecture files", e)
            Result.failure(e)
        }
    }

    private suspend fun deleteLectureFile(fileId: String): Result<Unit> {
        return try {
            // Get file info first to delete from storage
            val fileDoc = getUserLectureFilesCollection().document(fileId).get().await()
            val file = fileDoc.toObject(LectureFile::class.java)

            // Delete from storage if URL exists
            file?.fileUrl?.let { url ->
                try {
                    val storageRef = storage.getReferenceFromUrl(url)
                    storageRef.delete().await()
                } catch (e: Exception) {
                    Log.w("DatabaseManager", "Could not delete file from storage", e)
                }
            }

            // Delete from Firestore
            getUserLectureFilesCollection()
                .document(fileId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error deleting lecture file", e)
            Result.failure(e)
        }
    }

    // === STUDY EVENTS METHODS ===

    suspend fun createStudyEvent(event: StudyEvent): Result<String> {
        return try {
            val eventData = event.copy(id = "")
            val docRef = getUserStudyEventsCollection().add(eventData).await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error creating study event", e)
            Result.failure(e)
        }
    }

    suspend fun getStudyEvents(): Result<List<StudyEvent>> {
        return try {
            val snapshot = getUserStudyEventsCollection()
                .orderBy("startTime")
                .get()
                .await()

            val events = snapshot.documents.mapNotNull { doc ->
                doc.toObject(StudyEvent::class.java)?.copy(id = doc.id)
            }

            Result.success(events)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error loading study events", e)
            Result.failure(e)
        }
    }

    suspend fun updateStudyEvent(event: StudyEvent): Result<Unit> {
        return try {
            getUserStudyEventsCollection()
                .document(event.id)
                .set(event)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error updating study event", e)
            Result.failure(e)
        }
    }

    suspend fun deleteStudyEvent(eventId: String): Result<Unit> {
        return try {
            getUserStudyEventsCollection()
                .document(eventId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error deleting study event", e)
            Result.failure(e)
        }
    }

    // === UTILITY METHODS ===

    suspend fun exportUserData(): Result<Map<String, Any?>> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            val profile = getUserProfile().getOrNull()
            val modules = getModules().getOrNull() ?: emptyList()
            val allLectures = getAllLectures().getOrNull() ?: emptyList()
            val studyEvents = getStudyEvents().getOrNull() ?: emptyList()

            val exportData = mapOf(
                "profile" to profile,
                "modules" to modules,
                "lectures" to allLectures,
                "study_events" to studyEvents,
                "export_timestamp" to System.currentTimeMillis(),
                "export_version" to "1.0"
            )

            Log.d("DatabaseManager", "User data exported successfully")
            Result.success(exportData)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error exporting user data", e)
            Result.failure(e)
        }
    }

    suspend fun getUserStats(): Result<Map<String, Any>> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            val profile = getUserProfile().getOrNull()
            val modules = getModules().getOrNull() ?: emptyList()
            val allLectures = getAllLectures().getOrNull() ?: emptyList()
            val studyEvents = getStudyEvents().getOrNull() ?: emptyList()

            val stats = mapOf(
                "total_modules" to modules.size,
                "active_modules" to modules.count { it.isActive },
                "total_lectures" to allLectures.size,
                "total_study_events" to studyEvents.size,
                "profile_created" to (profile?.createdAt ?: 0L),
                "last_activity" to (profile?.updatedAt ?: 0L),
                "account_age_days" to ((System.currentTimeMillis() - (profile?.createdAt
                    ?: System.currentTimeMillis())) / (1000 * 60 * 60 * 24))
            )

            Log.d("DatabaseManager", "User stats calculated: $stats")
            Result.success(stats)
        } catch (e: Exception) {
            Log.e("DatabaseManager", "Error calculating user stats", e)
            Result.failure(e)
        }
    }

    // Check if user is authenticated
    fun isUserAuthenticated(): Boolean {
        return auth.currentUser != null
    }

    // Get current user email
    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    // Get current user display name
    fun getCurrentUserDisplayName(): String? {
        return auth.currentUser?.displayName
    }



    suspend fun cleanupDuplicateLecturesByTitle(moduleId: String, title: String): Result<Int> {
        // Ensure user is authenticated (as per your existing pattern)
        val userId = getCurrentUserId()
            ?: return Result.failure(Exception("User not authenticated for cleanupDuplicateLecturesByTitle"))

        val lecturesCollection = getModuleLecturesCollection(moduleId)

        return try {
            // 1. Query for all lectures with the given title in the specified module
            val querySnapshot = lecturesCollection
                .whereEqualTo("title", title)
                .get()
                .await()

            // 2. If 0 or 1 lecture is found, there are no duplicates to delete
            if (querySnapshot.documents.size <= 1) {
                Log.d(
                    "DatabaseManager",
                    "No duplicates found for title '$title' in module '$moduleId'. No cleanup needed."
                )
                return Result.success(0) // 0 items deleted
            }

            // 3. Map documents to Lecture objects and sort them by 'createdAt' (oldest first)
            val lectures = querySnapshot.documents.mapNotNull { doc ->
                // Assuming your Lecture data class has an 'id' field and 'createdAt' field
                doc.toObject(Lecture::class.java)?.copy(id = doc.id)
            }.sortedBy { it.createdAt } // Oldest createdAt timestamp first

            // 4. The first lecture in the sorted list is the one to keep (oldest)
            val lectureToKeep = lectures.first()

            // 5. All other lectures in the list are considered duplicates to be deleted
            val lecturesToDelete = lectures.drop(1) // Get all elements except the first one

            if (lecturesToDelete.isEmpty()) {
                // This case should ideally not be reached if querySnapshot.documents.size > 1,
                // but it's a safe check.
                Log.d(
                    "DatabaseManager",
                    "Consistency check: No lectures marked for deletion for title '$title' after sorting, though multiple were found initially."
                )
                return Result.success(0)
            }

            // 6. Use a batch write to delete the duplicate lectures
            val batch = firestore.batch()
            lecturesToDelete.forEach { lecture ->
                Log.d(
                    "DatabaseManager",
                    "Scheduling deletion of duplicate lecture ID: ${lecture.id}, Title: '${lecture.title}', CreatedAt: ${lecture.createdAt}"
                )
                batch.delete(lecturesCollection.document(lecture.id))
            }


            batch.commit().await()

            Log.i(
                "DatabaseManager",
                "Successfully cleaned up ${lecturesToDelete.size} duplicate lectures for title '$title' in module '$moduleId'. Kept lecture ID: ${lectureToKeep.id} (CreatedAt: ${lectureToKeep.createdAt})"
            )
            Result.success(lecturesToDelete.size)

        } catch (e: Exception) {
            Log.e(
                "DatabaseManager",
                "Error cleaning up duplicate lectures for title '$title' in module '$moduleId': ${e.message}",
                e
            )
            Result.failure(e)
        }
    }
}