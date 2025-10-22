##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# CRITICAL: Keep generic type information for TypeToken to work properly
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes *Annotation*

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Application classes that will be serialized/deserialized over Gson
-keepclassmembers class org.openedx.**.data.model.** { *; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  <init>();
  @com.google.gson.annotations.SerializedName <fields>;
}

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
# CRITICAL: Do NOT allow obfuscation or shrinking of TypeToken - it needs to preserve generic type information
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken

# Keep TypeToken constructors and methods to preserve generic type information
-keepclassmembers class com.google.gson.reflect.TypeToken {
    <init>(...);
    <methods>;
}

# Keep all Gson reflection classes that handle generic types
-keep class com.google.gson.reflect.** { *; }

# CRITICAL: Keep Google Guava TypeToken and TypeCapture classes (used by Gson)
-keep class com.google.common.reflect.TypeToken { *; }
-keep class com.google.common.reflect.TypeCapture { *; }
-keep class com.google.common.reflect.TypeToken$* { *; }
-keep class com.google.common.reflect.TypeCapture$* { *; }

# Keep all anonymous subclasses of TypeToken (created by object : TypeToken<T>() {})
-keep class * extends com.google.common.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }

# Keep Gson TypeAdapter classes used by Room TypeConverters
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory

# Keep Room TypeConverters that use Gson (important for complex types like List<SectionScoreDb>)
-keep @androidx.room.TypeConverter class * { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter <methods>;
}

# Keep generic type information for Room entities with complex types
-keepclassmembers class org.openedx.**.data.model.room.** {
    <fields>;
    <init>(...);
    * mapToDomain();
    * mapToRoomEntity();
    * mapToEntity();
}

# CRITICAL: Keep the CourseConverter and all its TypeToken usage
-keep class org.openedx.course.data.storage.CourseConverter { *; }
-keepclassmembers class org.openedx.course.data.storage.CourseConverter {
    <init>(...);
    <methods>;
}

# Keep anonymous TypeToken subclasses created in CourseConverter
-keep class org.openedx.course.data.storage.CourseConverter$* { *; }

# CRITICAL: Prevent obfuscation of CourseConverter methods that use TypeToken
-keepclassmembers,allowobfuscation class org.openedx.course.data.storage.CourseConverter {
    @androidx.room.TypeConverter <methods>;
}

# Keep all TypeConverter classes that use Gson
-keep class org.openedx.discovery.data.converter.DiscoveryConverter { *; }

# Keep the specific TypeToken usage patterns in TypeConverters
-keepclassmembers class org.openedx.**.data.storage.** {
    @androidx.room.TypeConverter <methods>;
}

-keepclassmembers class org.openedx.**.data.converter.** {
    @androidx.room.TypeConverter <methods>;
}
##---------------End: proguard configuration for Gson  ----------

-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

##---------------Begin: proguard configuration for Kotlin Coroutines  ----------
# Keep all coroutine-related classes and methods
-keep class kotlinx.coroutines.** { *; }
-keep class kotlin.coroutines.** { *; }
-keep class kotlin.coroutines.intrinsics.** { *; }

# Keep suspend functions and coroutine builders
-keepclassmembers class * {
    kotlin.coroutines.Continuation *(...);
}

# Keep coroutine context and related classes
-keep class kotlinx.coroutines.CoroutineContext$* { *; }

# Keep Flow and StateFlow classes
-keep class kotlinx.coroutines.flow.** { *; }

# Keep coroutine dispatchers
-keep class kotlinx.coroutines.Dispatchers { *; }
-keep class kotlinx.coroutines.Dispatchers$* { *; }

# Keep coroutine scope and job classes
-keep class kotlinx.coroutines.CoroutineScope { *; }
-keep class kotlinx.coroutines.Job { *; }
-keep class kotlinx.coroutines.Job$* { *; }

# Keep coroutine intrinsics that are causing the error
-keep class kotlin.coroutines.intrinsics.IntrinsicsKt { *; }
-keep class kotlin.coroutines.intrinsics.IntrinsicsKt$* { *; }

# Keep suspend function markers
-keepclassmembers class * {
    @kotlin.coroutines.RestrictsSuspension <methods>;
}

# Keep coroutine-related annotations
-keep @kotlin.coroutines.RestrictsSuspension class * { *; }
##---------------End: proguard configuration for Kotlin Coroutines  ----------

-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn com.google.crypto.tink.subtle.Ed25519Sign$KeyPair
-dontwarn com.google.crypto.tink.subtle.Ed25519Sign
-dontwarn com.google.crypto.tink.subtle.Ed25519Verify
-dontwarn com.google.crypto.tink.subtle.X25519
-dontwarn edu.umd.cs.findbugs.annotations.NonNull
-dontwarn edu.umd.cs.findbugs.annotations.Nullable
-dontwarn edu.umd.cs.findbugs.annotations.SuppressFBWarnings
-dontwarn org.bouncycastle.asn1.ASN1Encodable
-dontwarn org.bouncycastle.asn1.pkcs.PrivateKeyInfo
-dontwarn org.bouncycastle.asn1.x509.AlgorithmIdentifier
-dontwarn org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
-dontwarn org.bouncycastle.cert.X509CertificateHolder
-dontwarn org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
-dontwarn org.bouncycastle.crypto.BlockCipher
-dontwarn org.bouncycastle.crypto.CipherParameters
-dontwarn org.bouncycastle.crypto.InvalidCipherTextException
-dontwarn org.bouncycastle.crypto.engines.AESEngine
-dontwarn org.bouncycastle.crypto.modes.GCMBlockCipher
-dontwarn org.bouncycastle.crypto.params.AEADParameters
-dontwarn org.bouncycastle.crypto.params.KeyParameter
-dontwarn org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider
-dontwarn org.bouncycastle.jce.provider.BouncyCastleProvider
-dontwarn org.bouncycastle.openssl.PEMKeyPair
-dontwarn org.bouncycastle.openssl.PEMParser
-dontwarn org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
-dontwarn com.android.billingclient.api.BillingClientStateListener
-dontwarn com.android.billingclient.api.PurchasesUpdatedListener
-dontwarn com.google.crypto.tink.subtle.XChaCha20Poly1305
-dontwarn net.jcip.annotations.GuardedBy
-dontwarn net.jcip.annotations.Immutable
-dontwarn net.jcip.annotations.ThreadSafe
