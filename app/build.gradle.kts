plugins {
        id("com.android.application")

        // Add the Google services Gradle plugin
        id("com.google.gms.google-services")


}

android {
    namespace = "com.example.givedonnationapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.givedonnationapp"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.storage)
    implementation(libs.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation ("com.google.firebase:firebase-firestore:24.10.1")
    implementation ("com.google.android.material:material:1.11.0")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation ("com.squareup.picasso:picasso:2.8")
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation ("com.airbnb.android:lottie:6.1.0")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("androidx.multidex:multidex:2.0.1")
    implementation ("com.google.android.material:material:1.12.0")
    implementation ("com.google.android.material:material:1.9.0")

    // ConstraintLayout
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")

    // RecyclerView
    implementation ("androidx.recyclerview:recyclerview:1.3.2")
    implementation ("com.google.firebase:firebase-firestore:24.4.1")
    implementation ("com.squareup.picasso:picasso:2.8")
    implementation ("androidx.recyclerview:recyclerview:1.3.0")
    // Core AndroidX dependencies (if not already included)
    implementation ("androidx.core:core:1.13.1")
    implementation ("androidx.appcompat:appcompat:1.7.0")


}