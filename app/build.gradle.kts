plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)
    jacoco
}

android {
    namespace = "com.example.androidsampleapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.androidsampleapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // flavor ごとの接続先や挙動は BuildConfig に出し、config/AppConfig が読む。
    flavorDimensions += "environment"
    productFlavors {
        create("mock") {
            dimension = "environment"
            applicationIdSuffix = ".mock"
            versionNameSuffix = "-mock"
            // 通信の手前で FakeApiInterceptor が応答するので、この宛先には繋がない。
            buildConfigField("String", "API_BASE_URL", "\"http://localhost/api\"")
            buildConfigField("boolean", "USE_FAKE_API", "true")
            buildConfigField("String", "DEVICE_ID", "\"mock-device-001\"")
            buildConfigField("long", "SLEEP_TIMEOUT_MS", "30000L")
        }
        create("product") {
            dimension = "environment"
            // サンプル用の値。使う環境に合わせて差し替える。
            buildConfigField("String", "API_BASE_URL", "\"https://api.example.com\"")
            buildConfigField("boolean", "USE_FAKE_API", "false")
            buildConfigField("String", "DEVICE_ID", "\"device-001\"")
            buildConfigField("long", "SLEEP_TIMEOUT_MS", "30000L")
        }
    }

    buildTypes {
        debug {
            // 単体テストのカバレッジを JaCoCo で取る。結果は jacocoMockDebugUnitTestReport でまとめる。
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // AGP が単体テストに付ける JaCoCo のエージェントと、下の jacoco プラグインのバージョンを揃える。
    testCoverage {
        jacocoVersion = libs.versions.jacoco.get()
    }

    // Android Lint。既存の指摘は lint-baseline.xml に入れ、新しく増えたものだけでビルドを止める。
    // 作り方と更新のしかたは docs/static-analysis.md。
    lint {
        baseline = file("lint-baseline.xml")
        abortOnError = true
        warningsAsErrors = false
        checkReleaseBuilds = false
        htmlReport = true
        xmlReport = true
        sarifReport = true
    }
}

// ---- 静的解析とカバレッジ（docs/static-analysis.md） ----

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    baseline = rootProject.file("config/detekt/baseline.xml")
    source.setFrom("src/main/java", "src/test/java")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "17"
    reports {
        html.required.set(true)
        xml.required.set(true)
        sarif.required.set(true)
        txt.required.set(false)
        md.required.set(false)
    }
}

tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
    jvmTarget = "17"
}

// detekt は自分が作られた Kotlin のバージョンで動く必要がある。
// このプロジェクトの Kotlin（2.0.21）に引き上げられると起動時に落ちるので、detekt の分だけ戻す。
configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(io.gitlab.arturbosch.detekt.getSupportedKotlinVersion())
        }
    }
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

/** カバレッジの集計から外すクラス。自動生成されたコードを数えると、テストで触れない分だけ率が下がって見える。 */
val coverageExclusions = listOf(
    // Android と Gradle が作るもの
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    // Hilt / Dagger が作るもの
    "**/Hilt_*.*",
    "**/*_HiltModules*.*",
    "**/*_Factory*.*",
    "**/*_MembersInjector.*",
    "**/*_Provide*Factory*.*",
    "**/dagger/**",
    "**/hilt_aggregated_deps/**",
    // Compose と kotlinx.serialization が作るもの
    "**/*ComposableSingletons*.*",
    "**/*\$serializer.*",
)

tasks.register<JacocoReport>("jacocoMockDebugUnitTestReport") {
    group = "verification"
    description = "mockDebug の単体テストを実行し、JaCoCo のカバレッジレポート（HTML / XML）を作る。"
    dependsOn("testMockDebugUnitTest")

    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }

    val buildDir = layout.buildDirectory
    classDirectories.setFrom(
        buildDir.dir("tmp/kotlin-classes/mockDebug").map { fileTree(it) { exclude(coverageExclusions) } },
        buildDir.dir("intermediates/javac/mockDebug/compileMockDebugJavaWithJavac/classes")
            .map { fileTree(it) { exclude(coverageExclusions) } },
    )
    sourceDirectories.setFrom("src/main/java")
    executionData.setFrom(
        buildDir.file("outputs/unit_test_code_coverage/mockDebugUnitTest/testMockDebugUnitTest.exec"),
    )
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
