Aptitude – AI-Powered Study Assistant

An intelligent Android application that transforms static lecture notes into interactive, AI-powered study materials.


Overview
Aptitude is a native Android app built to help students organise their study materials, generate AI summaries from lecture notes, and interact with course content through a context-aware chatbot. It was developed as a final year project for a BSc in Computing Science at Griffith College Dublin (2025).

Features

Module & Lecture Organisation — Structure your study content by module and individual lecture
AI-Generated Summaries — Upload lecture note images and receive instant GPT-4 powered summaries and keyword extraction
Context-Aware Chatbot — Ask questions about specific lecture content; the AI responds using your uploaded notes as context
OCR Text Extraction — Google ML Kit reads text from uploaded images automatically
Study Calendar — Schedule and track study sessions with a visual calendar interface
Progress Tracking — Monitor study activity and engagement over time
Firebase Authentication — Secure email/password login and account management


Tech Stack
LayerTechnologyLanguageKotlinUI FrameworkJetpack ComposeArchitectureMVVMDatabaseFirebase FirestoreFile StorageFirebase StorageAuthenticationFirebase AuthenticationAI / NLPOpenAI GPT-4 APIOCRGoogle ML Kit (Text Recognition)NetworkingRetrofitCalendar UIKizitonwose Calendar ComposeTestingJUnit 5, Espresso, Mockito, Firebase Test Lab

Architecture
Aptitude follows the Model-View-ViewModel (MVVM) architectural pattern:
View (Jetpack Compose UI)
    ↓ observes
ViewModel Layer (AuthViewModel, ModuleViewModel, LectureViewModel, ChatViewModel)
    ↓ calls
Model / Repository Layer (DatabaseManager.kt, ChatGptApi.kt)
    ↓ communicates with
Firebase Services + OpenAI GPT-4 API

AI Processing Workflow

User uploads an image of lecture notes
ML Kit OCR extracts raw text from the image
Extracted text is sent to OpenAI GPT-4 API
GPT-4 returns a concise summary and key terms
Results are stored in Firestore and displayed in the app
The chatbot uses the same extracted text as context for follow-up questions


Getting Started
Prerequisites

Android Studio (latest stable)
Android device or emulator (API 26+)
Firebase project with Firestore, Storage, and Authentication enabled
OpenAI API key

Setup

Clone the repository

bash   git clone https://github.com/bistrobk/aptitude.git
   cd aptitude

Configure Firebase

Create a Firebase project at console.firebase.google.com
Download google-services.json and place it in the /app directory
Enable Firestore, Firebase Storage, and Email/Password Authentication


Add your API key securely

In local.properties (this file is gitignored):
