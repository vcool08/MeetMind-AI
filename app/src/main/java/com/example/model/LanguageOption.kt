package com.example.model

data class LanguageOption(
  val code: String,
  val name: String,
  val nativeName: String
)

val SupportedIndianLanguages = listOf(
  LanguageOption("en", "English", "English (Global)"),
  LanguageOption("hi", "Hindi", "हिन्दी"),
  LanguageOption("hinglish", "Hinglish", "Hinglish (Colloquial)"),
  LanguageOption("mr", "Marathi", "मराठी"),
  LanguageOption("gu", "Gujarati", "ગુજરાતી"),
  LanguageOption("ta", "Tamil", "தமிழ்"),
  LanguageOption("te", "Telugu", "తెలుగు"),
  LanguageOption("bn", "Bengali", "বাংলা"),
  LanguageOption("kn", "Kannada", "ಕನ್ನಡ"),
  LanguageOption("ml", "Malayalam", "മലയാളം"),
  LanguageOption("pa", "Punjabi", "ਪੰਜਾਬੀ")
)
