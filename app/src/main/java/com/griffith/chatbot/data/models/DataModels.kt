package  com.griffith.chatbot.data.models



import androidx.compose.ui.graphics.vector.ImageVector

import java.time.LocalDate








data class Module(

    val id: String = "",

    val name: String = "",

    val moduleCode: String = "",

    val lecturerName: String = "",

    val credits: Int = 0,

    val semester: String = "",

    val color: String = "#6200EE", // For visual identification

    val schedule: List<ClassSchedule> = emptyList(),

    val createdAt: Long = System.currentTimeMillis(),

    val lastModified: Long = System.currentTimeMillis(),

    val isActive: Boolean = true,

    val description: String = "",

    val totalLectures: Int = 0,

    val completedLectures: Int = 0

)



data class ClassSchedule(

    val dayOfWeek: Int = 1, // 1-7 (Monday-Sunday)

    val startTime: String = "", // Format: "HH:mm" (e.g., "09:00")

    val endTime: String = "", // Format: "HH:mm" (e.g., "10:30")

    val location: String = "",

    val type: String = "LECTURE"

) {

}


data class Lecture(

    val id: String = "",

    val moduleId: String = "",

    val title: String = "",

    val description: String = "",

    val content: String = "",

    val attachments: List<String> = emptyList(),

    val createdAt: Long = System.currentTimeMillis(),

    val lastModified: Long = System.currentTimeMillis(),

    val isCompleted: Boolean = false,

    val studyTime: Long = 0, // in minutes

    val tags: List<String> = emptyList(),

    val lectureType: String = "LECTURE", // LECTURE, LAB, TUTORIAL, SEMINAR

    val location: String = "",

    val timetable: Map<String, Any>? = null,

    val lectureNotes: List<LectureNote> = emptyList()

)



data class LectureNote(

    val id: String = "",

    val fileName: String = "",

    val fileUrl: String = "",

    val fileType: String = "",

    val mimeType: String = "",

    val fileSize: Long = 0,

    val uploadTime: Long = System.currentTimeMillis(),

    val extractedText: String = "",

    val thumbnailUrl: String = "",

    val isProcessed: Boolean = false,

    val processingStatus: String = "pending"

) {

}


data class StudySession(

    val id: String = "",

    val moduleId: String = "",

    val lectureId: String? = null,

    val startTime: Long = System.currentTimeMillis(),

    val endTime: Long? = null,

    val duration: Long = 0,

    val type: String = "STUDY",

    val notes: String = "",

    val effectiveness: Int = 0,

    val focusScore: Int = 0,

    val difficultyLevel: Int = 0,

    val studyMethod: String = "",

    val goals: List<String> = emptyList(),

    val achievements: List<String> = emptyList(),

    val nextSteps: String = ""

) {

}






data class StudyStats(

    val totalHours: Double = 0.0,

    val streakDays: Int = 0,

    val completedModules: Int = 0,

    val totalModules: Int = 0,



    val weeklyGoalHours: Double = 0.0,

    val weeklyActualHours: Double = 0.0,

    val monthlyHours: Double = 0.0,

    val averageSessionDuration: Double = 0.0, // in minutes

    val mostStudiedModule: String = "",

    val preferredStudyTime: String = "", // "morning", "afternoon", "evening", "night"

    val totalSessions: Int = 0,

    val averageEffectiveness: Double = 0.0, // 1-5 rating

    val lastStudyDate: Long = 0,

    val longestStreak: Int = 0,

    val favoriteStudyMethods: List<String> = emptyList()

) {

// No-argument constructor for Firestore

    constructor() : this(0.0, 0, 0, 0, 0.0, 0.0, 0.0, 0.0, "", "", 0, 0.0, 0, 0, emptyList())

}



data class StudyGoal(

    val id: String = "",

    val moduleId: String? = null,

    val title: String = "",

    val description: String = "",

    val targetHours: Double = 0.0,

    val currentHours: Double = 0.0,

    val targetDate: Long = System.currentTimeMillis(),

    val priority: Int = 1,

    val isCompleted: Boolean = false,

    val goalType: String = "HOURS",

    val targetValue: Double = 0.0,

    val currentValue: Double = 0.0,

    val unit: String = "hours",

    val createdAt: Long = System.currentTimeMillis(),

    val completedAt: Long? = null

) {

}



data class Achievement(

    val id: String = "",

    val title: String = "",

    val description: String = "",

    val icon: String = "",

    val category: String = "",

    val requirement: String = "",

    val points: Int = 0,

    val isUnlocked: Boolean = false,

    val unlockedAt: Long? = null,

    val progress: Double = 0.0,

    val isVisible: Boolean = true

) {

}


data class StudyEvent(

    val id: String = "",

    val title: String = "",

    val description: String = "",

    val moduleId: String = "",

    val moduleName: String = "",

    val startTime: Long = 0L,

    val endTime: Long = 0L,

    val duration: Int = 0, // in minutes

    val type: EventType = EventType.STUDY,

    val isCompleted: Boolean = false,

    val priority: Priority = Priority.MEDIUM,

    val location: String = "",

    val isFromTimetable: Boolean = false, // New field to distinguish timetable events

    val classScheduleId: String? = null, // Reference to ClassSchedule from Module

    val instructor: String = ""

) {

}



enum class EventType {

    STUDY, EXAM, REVISION, ASSIGNMENT, PROJECT, RESEARCH

}



enum class Priority {

    LOW, MEDIUM, HIGH, URGENT

}



data class StudyAnalytics(

    val totalHours: Double = 0.0,

    val streakDays: Int = 0,

    val completedSessions: Int = 0,

    val averageSessionLength: Double = 0.0,

    val mostStudiedModule: String = "",

    val weeklyProgress: List<DayProgress> = emptyList(),

    val monthlyProgress: List<MonthProgress> = emptyList(),

    val moduleBreakdown: List<ModuleStats> = emptyList(),

    val productivityTrends: ProductivityData = ProductivityData(),

    val goals: List<StudyGoal> = emptyList(),

    val achievements: List<Achievement> = emptyList()

)



data class DayProgress(

    val date: LocalDate,

    val hoursStudied: Double,

    val sessionsCompleted: Int,

    val targetMet: Boolean

)



data class MonthProgress(

    val month: String,

    val hoursStudied: Double,

    val targetHours: Double

)



data class ModuleStats(

    val moduleId: String,

    val moduleName: String,

    val hoursStudied: Double,

    val sessionsCompleted: Int,

    val averageScore: Double,

    val progress: Float,

    val color: String

)



data class ProductivityData(

    val bestStudyTime: String = "Morning",

    val averageFocusScore: Double = 0.0,

    val mostProductiveDay: String = "Monday",

    val studyHeatmap: Map<String, Double> = emptyMap()

)




sealed class UiSettingsItem {

    abstract val title: String

    abstract val icon: ImageVector

    abstract val key: String


}



data class SelectionOption(

    val value: String,

    val displayName: String,

    val description: String? = null

)