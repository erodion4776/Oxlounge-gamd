import java.util.Base64

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.plugin.compose")
  id("com.google.devtools.ksp")
  id("io.github.takahirom.roborazzi")
  id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
  namespace = "com.oxlounge"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.aistudio.neonwheel.qyzb7h"
    minSdk = 28
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  sourceSets {
    getByName("main") {
      val mainDir = if (project.projectDir.resolve("src/Main").exists()) "src/Main" else "src/main"
      manifest.srcFile("$mainDir/AndroidManifest.xml")
      java.srcDirs("$mainDir/java", "$mainDir/kotlin")
      res.srcDirs("$mainDir/res")
    }
  }

  val keystoreFile = file("${rootDir}/debug.keystore")
  if (!keystoreFile.exists()) {
    val base64File = file("${rootDir}/debug.keystore.base64")
    if (base64File.exists()) {
      try {
        val base64Bytes = base64File.readBytes()
        val base64Str = String(base64Bytes).replace("\\s".toRegex(), "")
        val decodedBytes = Base64.getDecoder().decode(base64Str)
        keystoreFile.writeBytes(decodedBytes)
      } catch (e: Exception) {
        project.logger.error("Failed to decode debug.keystore.base64: ${e.message}")
      }
    }
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Use default Android debug signing configuration instead of the custom debugConfig to prevent package parsing errors.
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform("androidx.compose:compose-bom:2024.09.00"))
  implementation(platform("com.google.firebase:firebase-bom:34.12.0"))
  // implementation("com.google.accompanist:accompanist-permissions:0.37.3")
  implementation("androidx.activity:activity-compose:1.10.1")
  // implementation("androidx.camera:camera-camera2:1.5.0")
  // implementation("androidx.camera:camera-core:1.5.0")
  // implementation("androidx.camera:camera-lifecycle:1.5.0")
  // implementation("androidx.camera:camera-view:1.5.0")
  implementation("androidx.compose.material:material-icons-core")
  // implementation("androidx.compose.material:material-icons-extended")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-graphics")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.core:core-ktx:1.15.0")
  // implementation("androidx.datastore:datastore-preferences:1.1.7")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
  // implementation("androidx.navigation:navigation-compose:2.8.9")
  implementation("androidx.room:room-ktx:2.7.0")
  implementation("androidx.room:room-runtime:2.7.0")
  // implementation("io.coil-kt:coil-compose:2.7.0")
  implementation("com.squareup.retrofit2:converter-moshi:2.12.0")
  // implementation("com.google.firebase:firebase-ai")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
  implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
  implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
  implementation("com.squareup.okhttp3:okhttp:4.10.0")
  // implementation("com.google.android.gms:play-services-location:21.3.0")
  implementation("com.squareup.retrofit2:retrofit:2.12.0")
  testImplementation("androidx.compose.ui:ui-test-junit4")
  testImplementation("androidx.test:core:1.6.1")
  testImplementation("androidx.test.ext:junit:1.3.0")
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
  testImplementation("org.robolectric:robolectric:4.16.1")
  testImplementation("io.github.takahirom.roborazzi:roborazzi:1.59.0")
  testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.59.0")
  testImplementation("io.github.takahirom.roborazzi:roborazzi-junit-rule:1.59.0")
  androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
  androidTestImplementation("androidx.compose.ui:ui-test-junit4")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
  androidTestImplementation("androidx.test.ext:junit:1.3.0")
  androidTestImplementation("androidx.test:runner:1.6.2")
  debugImplementation("androidx.compose.ui:ui-test-manifest")
  debugImplementation("androidx.compose.ui:ui-tooling")
  "ksp"("androidx.room:room-compiler:2.7.0")
  "ksp"("com.squareup.moshi:moshi-kotlin-codegen:1.15.2")
}

tasks.register<Copy>("copyApk") {
    from(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
    into(rootProject.layout.projectDirectory.dir(".build-outputs"))
}

tasks.register<Copy>("copyApkDownload") {
    from(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
    into(rootProject.layout.projectDirectory.dir("APK_DOWNLOAD"))
}

tasks.register("copyAllApks") {
    dependsOn("assembleDebug")
    finalizedBy("copyApk", "copyApkDownload")
}


