plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.proyectofinal6to_ecobox"
    compileSdk = 36 // Se recomienda usar una versión estable como 34 o 35

    defaultConfig {
        applicationId = "com.example.proyectofinal6to_ecobox"
        minSdk = 24
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
    kotlinOptions {
        jvmTarget = "11"
    }

    // AGREGA ESTO PARA SOLUCIONAR EL ERROR DE DUPLICADOS (Sintaxis Kotlin)
    packaging {
        resources {
            pickFirsts.add("META-INF/NOTICE.md")
            pickFirsts.add("META-INF/LICENSE.md")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.play.services.cast.framework)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Si estás usando Room y te da error, comenta esta línea o revisa tu catálogo de versiones
    // implementation(libs.androidx.room.common.jvm)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Tus librerías de conexión y correo (Sintaxis Kotlin con paréntesis y comillas dobles)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("mysql:mysql-connector-java:5.1.49")
    implementation ("com.google.android.material:material:1.11.0")

    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
    // Retrofit (Cliente HTTP)
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    // Convertidor GSON (Para convertir JSON a objetos)
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    // Corutinas (Para no bloquear la interfaz de usuario)
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0") // Esto incluye CardView
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation ("com.airbnb.android:lottie:6.1.0")
// Material Design Components (for Cards, Buttons, Switches, CoordinatorLayout)
    implementation ("com.google.android.material:material:1.9.0")
    implementation ("de.hdodenhof:circleimageview:3.1.0")

    // MPAndroidChart (for com.github.mikephil.charting.charts.LineChart)
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation ("com.github.bumptech.glide:glide:4.16.0")

    // Interceptor de Logging para OkHttp/Retrofit
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // WorkManager para notificaciones en segundo plano
    val workVersion = "2.9.0"
    implementation("androidx.work:work-runtime-ktx:$workVersion")
}