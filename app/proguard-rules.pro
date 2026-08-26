# --- kotlinx.serialization ----------------------------------------------------
# Keep the generated serializers and Companion objects for API DTOs.
-keepclassmembers class com.shellanddeploy.fpllive.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.shellanddeploy.fpllive.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Retrofit -----------------------------------------------------------------
# Keep the API service interface and its annotations (Retrofit invokes by reflection).
-keep,allowobfuscation,allowshrinking interface com.shellanddeploy.fpllive.data.api.FplApi

# --- General ------------------------------------------------------------------
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses, EnclosingMethod
