package com.campus.network.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.campus.network.core.data.CampusDirectoryRepository
import com.campus.network.core.data.StudentProfileRepository
import com.campus.network.core.model.CountryDirectoryEntry
import com.campus.network.core.model.OnboardingPreferences
import com.campus.network.core.model.UniversityDirectoryEntry
import com.campus.network.core.ui.theme.BorderStrong
import com.campus.network.core.ui.theme.BorderSubtle
import com.campus.network.core.ui.theme.BrandBlueAction
import com.campus.network.core.ui.theme.BrandBlueInteractive
import com.campus.network.core.ui.theme.BrandBlueLight
import com.campus.network.core.ui.theme.BrandBluePrimary
import com.campus.network.core.ui.theme.BrandGreen
import com.campus.network.core.ui.theme.BrandGreenSoft
import com.campus.network.core.ui.theme.SurfaceElevated
import com.campus.network.core.ui.theme.SurfaceSubtle
import com.campus.network.core.ui.theme.SurfaceWhite
import com.campus.network.core.ui.theme.TextMuted
import com.campus.network.core.ui.theme.TextPrimary
import com.campus.network.core.ui.theme.TextSecondary
import kotlinx.coroutines.launch

// Global Theme Color Tokens from Patriarch Design System (Color.kt / Theme.kt)
private val PureWhite = SurfaceWhite
private val InkBlack = TextPrimary               // Slate 900 (#0F172A)
private val BrandPrimary = BrandBluePrimary      // Patriarch Brand Blue (#014784)
private val BrandAction = BrandBlueAction        // Interactive CTA Blue (#2658B7)
private val BrandSelectedBg = BrandBlueLight     // Subtle blue tint for active state (#E0EDFA)
private val MutedText = TextSecondary            // Slate 500 (#64748B)
private val SubtleGrey = TextMuted               // Slate 400 (#94A3B8)
private val UnselectedBorder = BorderStrong      // Slate 300 (#CBD5E1)
private val DividerHairline = BorderSubtle       // Slate 200 (#E2E8F0)
private val DisabledPillGrey = BorderStrong      // Disabled button fill (#CBD5E1)
private val SkipLinkBlue = BrandBlueInteractive  // Interactive link blue (#0284C7)
private val CalloutBubbleBg = SurfaceSubtle      // Slate 50 (#F8FAFC)
private val VerifiedGreenBg = BrandGreenSoft
private val VerifiedGreenText = BrandGreen

enum class OnboardingStep {
    LANGUAGE,
    COUNTRY,
    REGION,
    UNIVERSITY,
    WELCOME,
    SURVEY_1_ACADEMIC,
    SURVEY_2_INTENT,
    SURVEY_3_CATEGORIES,
    SURVEY_4_PRIORITIES,
    PUSH_OPT_IN,
    PERSONALIZED_NEWS
}

@Composable
fun OnboardingSurveyScreen(
    studentProfileRepository: StudentProfileRepository,
    directoryRepository: CampusDirectoryRepository,
    onSurveyCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val profile by studentProfileRepository.getProfile().collectAsState(initial = null)
    val initialPrefs = profile?.onboardingPreferences ?: OnboardingPreferences()

    var currentStep by remember { mutableStateOf(OnboardingStep.LANGUAGE) }

    // Survey state
    var languageCode by remember { mutableStateOf(initialPrefs.languageCode.ifBlank { "en" }) }
    var selectedCountryCode by remember { mutableStateOf(initialPrefs.countryCode.ifBlank { "KE" }) }
    var selectedCountryName by remember { mutableStateOf(initialPrefs.countryName.ifBlank { "Kenya" }) }
    var selectedCurrencyCode by remember { mutableStateOf(initialPrefs.currencyCode.ifBlank { "KES" }) }
    var selectedRegion by remember { mutableStateOf(initialPrefs.regionName.ifBlank { "Nairobi" }) }
    var selectedUniversityName by remember { mutableStateOf(initialPrefs.universityName.ifBlank { "Strathmore University" }) }
    var selectedUniversityDomain by remember { mutableStateOf(initialPrefs.universityDomain.ifBlank { "strathmore.edu" }) }
    var isCustomUniversity by remember { mutableStateOf(initialPrefs.isCustomUniversity) }
    var isVerifiedStudentEmail by remember { mutableStateOf(initialPrefs.isVerifiedStudentEmail) }

    var selectedFaculty by remember { mutableStateOf(initialPrefs.faculty.ifBlank { "Computing & Informatics" }) }
    var selectedYear by remember { mutableStateOf(initialPrefs.yearOfStudy.ifBlank { "Year 3" }) }
    val selectedIntents = remember { mutableStateListOf<String>().apply { addAll(initialPrefs.intents) } }
    val selectedCategories = remember { mutableStateListOf<String>().apply { addAll(initialPrefs.favoriteCategories) } }
    val selectedPriorities = remember { mutableStateListOf<String>().apply { addAll(initialPrefs.priorities) } }
    var pushNotificationsOptIn by remember { mutableStateOf(initialPrefs.pushNotificationsOptIn) }

    // Directory data
    var countries by remember { mutableStateOf<List<CountryDirectoryEntry>>(emptyList()) }
    var regionsForCountry by remember { mutableStateOf<List<String>>(emptyList()) }
    var universitiesForSelection by remember { mutableStateOf<List<UniversityDirectoryEntry>>(emptyList()) }
    var universitySearchQuery by remember { mutableStateOf("") }

    // Sync email-verified prefill if profile loads
    LaunchedEffect(profile?. onboardingPreferences) {
        val prefs = profile?.onboardingPreferences ?: return@LaunchedEffect
        if (prefs.isVerifiedStudentEmail) {
            selectedCountryCode = prefs.countryCode
            selectedCountryName = prefs.countryName
            selectedCurrencyCode = prefs.currencyCode
            if (prefs.regionName.isNotBlank()) selectedRegion = prefs.regionName
            selectedUniversityName = prefs.universityName
            selectedUniversityDomain = prefs.universityDomain
            isVerifiedStudentEmail = true
        }
    }

    LaunchedEffect(Unit) {
        countries = directoryRepository.getCountries()
    }

    LaunchedEffect(selectedCountryCode) {
        val regs = directoryRepository.getRegionsForCountry(selectedCountryCode)
        regionsForCountry = regs
        if (regs.isNotEmpty() && selectedRegion !in regs) {
            selectedRegion = regs.first()
        } else if (regs.isEmpty()) {
            selectedRegion = ""
        }
    }

    LaunchedEffect(selectedCountryCode, selectedRegion, universitySearchQuery) {
        val list = directoryRepository.getUniversitiesForCountry(
            countryCode = selectedCountryCode,
            regionName = selectedRegion,
            searchQuery = universitySearchQuery
        )
        universitiesForSelection = list
        if (!isCustomUniversity && list.isNotEmpty() && list.none { it.name == selectedUniversityName }) {
            val first = list.first()
            selectedUniversityName = first.name
            selectedUniversityDomain = first.domain
        }
    }

    val strings = remember(languageCode) { OnboardingSurveyCatalog.stringsFor(languageCode) }

    fun persistAndFinish(completedConsent: Boolean = true) {
        val finalPrefs = OnboardingPreferences(
            languageCode = languageCode,
            countryCode = selectedCountryCode,
            countryName = selectedCountryName,
            currencyCode = selectedCurrencyCode,
            regionName = selectedRegion,
            universityName = selectedUniversityName,
            universityDomain = selectedUniversityDomain,
            isCustomUniversity = isCustomUniversity,
            isVerifiedStudentEmail = isVerifiedStudentEmail,
            faculty = selectedFaculty,
            yearOfStudy = selectedYear,
            intents = selectedIntents.toList(),
            favoriteCategories = selectedCategories.toList(),
            priorities = selectedPriorities.toList(),
            pushNotificationsOptIn = pushNotificationsOptIn,
            personalizedNewsConsent = completedConsent,
            hasCompletedSurvey = true
        )
        coroutineScope.launch {
            studentProfileRepository.saveOnboardingPreferences(finalPrefs)
            onSurveyCompleted()
        }
    }

    fun stepBack() {
        currentStep = when (currentStep) {
            OnboardingStep.LANGUAGE -> OnboardingStep.LANGUAGE
            OnboardingStep.COUNTRY -> OnboardingStep.LANGUAGE
            OnboardingStep.REGION -> OnboardingStep.COUNTRY
            OnboardingStep.UNIVERSITY -> if (regionsForCountry.isNotEmpty()) OnboardingStep.REGION else OnboardingStep.COUNTRY
            OnboardingStep.WELCOME -> OnboardingStep.UNIVERSITY
            OnboardingStep.SURVEY_1_ACADEMIC -> OnboardingStep.WELCOME
            OnboardingStep.SURVEY_2_INTENT -> OnboardingStep.SURVEY_1_ACADEMIC
            OnboardingStep.SURVEY_3_CATEGORIES -> OnboardingStep.SURVEY_2_INTENT
            OnboardingStep.SURVEY_4_PRIORITIES -> OnboardingStep.SURVEY_3_CATEGORIES
            OnboardingStep.PUSH_OPT_IN -> OnboardingStep.SURVEY_4_PRIORITIES
            OnboardingStep.PERSONALIZED_NEWS -> OnboardingStep.PUSH_OPT_IN
        }
    }

    BackHandler(enabled = currentStep != OnboardingStep.LANGUAGE) {
        stepBack()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        pushNotificationsOptIn = granted
        com.campus.network.core.ui.notifications.CampusSystemNotificationCenter.setPushEnabled(context, granted)
        currentStep = OnboardingStep.PERSONALIZED_NEWS
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(PureWhite)
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = PureWhite
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "OnboardingSurveyStep"
        ) { step ->
            when (step) {
                OnboardingStep.LANGUAGE -> LanguageSelectionStep(
                    strings = strings,
                    selectedLanguageCode = languageCode,
                    onSelectLanguage = { languageCode = it },
                    onContinue = { currentStep = OnboardingStep.COUNTRY }
                )

                OnboardingStep.COUNTRY -> CountrySelectionStep(
                    strings = strings,
                    countries = countries,
                    selectedCountryCode = selectedCountryCode,
                    onSelectCountry = { entry ->
                        selectedCountryCode = entry.code
                        selectedCountryName = entry.name
                        selectedCurrencyCode = entry.currencyCode
                    },
                    onBack = { stepBack() },
                    onContinue = {
                        currentStep = if (regionsForCountry.isNotEmpty()) {
                            OnboardingStep.REGION
                        } else {
                            OnboardingStep.UNIVERSITY
                        }
                    }
                )

                OnboardingStep.REGION -> RegionSelectionStep(
                    strings = strings,
                    regionLabel = directoryRepository.getRegionLabelForCountry(selectedCountryCode, languageCode),
                    countryName = selectedCountryName,
                    regions = regionsForCountry,
                    selectedRegion = selectedRegion,
                    onSelectRegion = { selectedRegion = it },
                    onBack = { stepBack() },
                    onContinue = { currentStep = OnboardingStep.UNIVERSITY }
                )

                OnboardingStep.UNIVERSITY -> UniversitySelectionStep(
                    strings = strings,
                    countryName = selectedCountryName,
                    regionName = selectedRegion,
                    universities = universitiesForSelection,
                    searchQuery = universitySearchQuery,
                    onSearchQueryChange = { universitySearchQuery = it },
                    selectedUniversityName = selectedUniversityName,
                    isCustomUniversity = isCustomUniversity,
                    isVerifiedStudentEmail = isVerifiedStudentEmail,
                    onSelectUniversity = { uni ->
                        selectedUniversityName = uni.name
                        selectedUniversityDomain = uni.domain
                        isCustomUniversity = false
                    },
                    onSelectCustomUniversity = { customName ->
                        selectedUniversityName = customName
                        selectedUniversityDomain = ""
                        isCustomUniversity = true
                        isVerifiedStudentEmail = false
                    },
                    onBack = { stepBack() },
                    onContinue = { currentStep = OnboardingStep.WELCOME }
                )

                OnboardingStep.WELCOME -> WelcomeSurveyStep(
                    strings = strings,
                    universityName = selectedUniversityName,
                    onContinue = { currentStep = OnboardingStep.SURVEY_1_ACADEMIC },
                    onSkip = { persistAndFinish(completedConsent = false) }
                )

                OnboardingStep.SURVEY_1_ACADEMIC -> SurveyAcademicStep(
                    strings = strings,
                    languageCode = languageCode,
                    selectedFaculty = selectedFaculty,
                    selectedYear = selectedYear,
                    onSelectFaculty = { selectedFaculty = it },
                    onSelectYear = { selectedYear = it },
                    onBack = { stepBack() },
                    onSkip = { currentStep = OnboardingStep.SURVEY_2_INTENT },
                    onContinue = { currentStep = OnboardingStep.SURVEY_2_INTENT }
                )

                OnboardingStep.SURVEY_2_INTENT -> SurveyIntentStep(
                    strings = strings,
                    languageCode = languageCode,
                    selectedIntents = selectedIntents,
                    onToggleIntent = { id ->
                        if (id in selectedIntents) selectedIntents.remove(id) else selectedIntents.add(id)
                    },
                    onBack = { stepBack() },
                    onSkip = { currentStep = OnboardingStep.SURVEY_3_CATEGORIES },
                    onContinue = { currentStep = OnboardingStep.SURVEY_3_CATEGORIES }
                )

                OnboardingStep.SURVEY_3_CATEGORIES -> SurveyCategoriesStep(
                    strings = strings,
                    languageCode = languageCode,
                    selectedCategories = selectedCategories,
                    onToggleCategory = { id ->
                        if (id in selectedCategories) selectedCategories.remove(id) else selectedCategories.add(id)
                    },
                    onBack = { stepBack() },
                    onSkip = { currentStep = OnboardingStep.SURVEY_4_PRIORITIES },
                    onContinue = { currentStep = OnboardingStep.SURVEY_4_PRIORITIES }
                )

                OnboardingStep.SURVEY_4_PRIORITIES -> SurveyPrioritiesStep(
                    strings = strings,
                    languageCode = languageCode,
                    selectedPriorities = selectedPriorities,
                    onTogglePriority = { id ->
                        if (id in selectedPriorities) selectedPriorities.remove(id) else selectedPriorities.add(id)
                    },
                    onBack = { stepBack() },
                    onSkip = { currentStep = OnboardingStep.PUSH_OPT_IN },
                    onContinue = { currentStep = OnboardingStep.PUSH_OPT_IN }
                )

                OnboardingStep.PUSH_OPT_IN -> PushOptInStep(
                    strings = strings,
                    onContinue = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!hasPerm) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                pushNotificationsOptIn = true
                                currentStep = OnboardingStep.PERSONALIZED_NEWS
                            }
                        } else {
                            pushNotificationsOptIn = true
                            currentStep = OnboardingStep.PERSONALIZED_NEWS
                        }
                    },
                    onSkip = {
                        pushNotificationsOptIn = false
                        currentStep = OnboardingStep.PERSONALIZED_NEWS
                    }
                )

                OnboardingStep.PERSONALIZED_NEWS -> PersonalizedNewsConsentStep(
                    strings = strings,
                    onContinue = { persistAndFinish(completedConsent = true) }
                )
            }
        }
    }
}

/**
 * Shared Top Navigation Bar matching Reference Images 1, 4, 5, 6, 7:
 * Left: `<` back chevron, Center: `1 / 4` step counter, Right: blue `Skip` text link.
 */
@Composable
private fun SurveyTopBar(
    showBack: Boolean,
    stepText: String? = null,
    skipLabel: String? = null,
    onBack: () -> Unit = {},
    onSkip: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .padding(horizontal = 8.dp)
    ) {
        if (showBack) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = InkBlack,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        if (!stepText.isNullOrEmpty()) {
            Text(
                text = stepText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = InkBlack,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (!skipLabel.isNullOrEmpty()) {
            Text(
                text = skipLabel,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = SkipLinkBlue,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable(onClick = onSkip)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * Shared Question Header Row matching Reference Images 4, 5, 6, 7, 8:
 * Left: `CampusGuidePointingAvatar` mascot, Right: Bold 16.sp prompt text.
 */
@Composable
private fun QuestionHeaderWithMascot(prompt: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CampusGuidePointingAvatar(
            modifier = Modifier.size(width = 72.dp, height = 80.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = prompt,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            color = InkBlack,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Shared Full-Width Square Selection Card matching Reference Images 4, 5, 7:
 * 0.dp sharp corners, 1.dp #CCCCCC border when unselected, 2.dp #000000 border + Bold text when selected.
 */
@Composable
private fun SquareSurveyOptionCard(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingBadge: (@Composable () -> Unit)? = null,
    trailingBadge: (@Composable () -> Unit)? = null
) {
    val borderWidth = if (selected) 2.dp else 1.dp
    val borderColor = if (selected) BrandPrimary else UnselectedBorder
    val bgColor = if (selected) BrandSelectedBg.copy(alpha = 0.25f) else PureWhite

    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(width = borderWidth, color = borderColor, shape = RectangleShape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingBadge != null) {
            leadingBadge()
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                fontSize = 15.5.sp,
                lineHeight = 21.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) BrandPrimary else InkBlack
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    color = MutedText
                )
            }
        }
        if (trailingBadge != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingBadge()
        }
    }
}

/**
 * Shared Bottom Sticky Action Bar with top hairline divider & full-width pill CONTINUE button
 * using the global Patriarch theme primary CTA color.
 */
@Composable
private fun StickyContinueFooter(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PureWhite)
    ) {
        HorizontalDivider(thickness = 1.dp, color = DividerHairline)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                    contentColor = PureWhite,
                    disabledContainerColor = DisabledPillGrey,
                    disabledContentColor = PureWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

/**
 * Shared Side-by-Side CONTINUE (solid BrandPrimary pill) + SKIP (outlined BrandPrimary pill) footer
 * with top hairline divider matching Reference Images 1 and 6.
 */
@Composable
private fun DualActionFooter(
    continueLabel: String,
    skipLabel: String,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PureWhite)
    ) {
        HorizontalDivider(thickness = 1.dp, color = DividerHairline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Button(
                onClick = onContinue,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                    contentColor = PureWhite
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text(
                    text = continueLabel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onSkip,
                shape = CircleShape,
                border = BorderStroke(1.25.dp, BrandPrimary),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = PureWhite,
                    contentColor = BrandPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text(
                    text = skipLabel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Side-by-side Outlined Speech Bubble on the left (with right-pointing triangle tail)
 * and the Campus Advisor Mascot with floating bubbles on the right,
 * matching Reference Images 2 and 3 (`uploaded_media_2` & `uploaded_media_3`).
 */
@Composable
private fun RecommendationCalloutWithMascotRow(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Outlined rectangular box with right-pointing triangle tail
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(1.25.dp, InkBlack, RectangleShape)
                    .background(PureWhite)
                    .padding(horizontal = 14.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = InkBlack,
                    textAlign = TextAlign.Center
                )
            }
            Canvas(modifier = Modifier.size(width = 12.dp, height = 22.dp)) {
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, size.height / 2f)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(path, color = PureWhite, style = Fill)
                drawLine(
                    color = InkBlack,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 3.2f
                )
                drawLine(
                    color = InkBlack,
                    start = Offset(size.width, size.height / 2f),
                    end = Offset(0f, size.height),
                    strokeWidth = 3.2f
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Right: Hand-drawn Campus Advisor Mascot with 3 floating bubbles
        CampusGuideRecommendationScene(
            modifier = Modifier.size(width = 148.dp, height = 165.dp)
        )
    }
}

/**
 * Clean square search box for Country / County / University steps.
 */
@Composable
private fun SquareSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.25.dp, BrandPrimary, RectangleShape)
            .background(PureWhite)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = BrandPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 14.5.sp,
                    color = SubtleGrey
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    color = InkBlack,
                    fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(BrandPrimary),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear",
                tint = SubtleGrey,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onValueChange("") }
            )
        }
    }
}

// ============================================================================
// STEP A: Language Selection (English & Kiswahili)
// ============================================================================
@Composable
private fun LanguageSelectionStep(
    strings: OnboardingSurveyStrings,
    selectedLanguageCode: String,
    onSelectLanguage: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SurveyTopBar(showBack = false)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            QuestionHeaderWithMascot(prompt = strings.selectLanguageTitle)

            Text(
                text = strings.selectLanguageSubtitle,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MutedText
            )

            Spacer(modifier = Modifier.height(4.dp))

            SquareSurveyOptionCard(
                text = "English",
                subtitle = "Default international campus language",
                selected = selectedLanguageCode == "en",
                onClick = { onSelectLanguage("en") },
                leadingBadge = { CountryFlagVectorBadge(countryCode = "GB") }
            )

            SquareSurveyOptionCard(
                text = "Kiswahili",
                subtitle = "Lugha ya Afrika Mashariki na vyuo vikuu",
                selected = selectedLanguageCode == "sw",
                onClick = { onSelectLanguage("sw") },
                leadingBadge = { CountryFlagVectorBadge(countryCode = "KE") }
            )
        }

        StickyContinueFooter(
            label = strings.continueBtn,
            enabled = true,
            onClick = onContinue
        )
    }
}

// ============================================================================
// STEP B: Global Country Selection
// ============================================================================
@Composable
private fun CountrySelectionStep(
    strings: OnboardingSurveyStrings,
    countries: List<CountryDirectoryEntry>,
    selectedCountryCode: String,
    onSelectCountry: (CountryDirectoryEntry) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(countries, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) countries
        else countries.filter {
            it.name.lowercase().contains(q) ||
                it.code.lowercase().contains(q) ||
                it.currencyCode.lowercase().contains(q)
        }
    }
    val featured = remember(filtered) { filtered.filter { it.isFeatured } }
    val others = remember(filtered) { filtered.filterNot { it.isFeatured } }

    Column(modifier = Modifier.fillMaxSize()) {
        SurveyTopBar(showBack = true, onBack = onBack)

        QuestionHeaderWithMascot(prompt = strings.selectCountryTitle)

        Text(
            text = strings.selectCountrySubtitle,
            fontSize = 13.5.sp,
            color = MutedText,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        SquareSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = strings.searchCountryPlaceholder,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (featured.isNotEmpty()) {
                item {
                    Text(
                        text = strings.featuredCountriesHeader,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(featured, key = { "feat_${it.code}" }) { entry ->
                    SquareSurveyOptionCard(
                        text = entry.name,
                        subtitle = "Currency: ${entry.currencyCode}",
                        selected = entry.code == selectedCountryCode,
                        onClick = { onSelectCountry(entry) },
                        leadingBadge = { CountryFlagVectorBadge(countryCode = entry.code) }
                    )
                }
            }

            if (others.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.allCountriesHeader,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SubtleGrey,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(others, key = { "all_${it.code}" }) { entry ->
                    SquareSurveyOptionCard(
                        text = entry.name,
                        subtitle = "Currency: ${entry.currencyCode}",
                        selected = entry.code == selectedCountryCode,
                        onClick = { onSelectCountry(entry) },
                        leadingBadge = { CountryFlagVectorBadge(countryCode = entry.code) }
                    )
                }
            }
        }

        StickyContinueFooter(
            label = strings.continueBtn,
            enabled = selectedCountryCode.isNotBlank(),
            onClick = onContinue
        )
    }
}

// ============================================================================
// STEP C: County / State / Province Selection (47 Kenyan Counties, 50 US States, etc.)
// ============================================================================
@Composable
private fun RegionSelectionStep(
    strings: OnboardingSurveyStrings,
    regionLabel: String,
    countryName: String,
    regions: List<String>,
    selectedRegion: String,
    onSelectRegion: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(regions, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) regions else regions.filter { it.lowercase().contains(q) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SurveyTopBar(showBack = true, onBack = onBack)

        QuestionHeaderWithMascot(
            prompt = String.format(strings.selectRegionTitleFormat, regionLabel.lowercase())
        )

        Text(
            text = "$countryName — ${strings.selectRegionSubtitleFormat}",
            fontSize = 13.5.sp,
            color = MutedText,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        SquareSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = String.format(strings.searchRegionPlaceholderFormat, regionLabel.lowercase()),
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it }) { reg ->
                SquareSurveyOptionCard(
                    text = reg,
                    selected = reg.equals(selectedRegion, ignoreCase = true),
                    onClick = { onSelectRegion(reg) }
                )
            }
        }

        StickyContinueFooter(
            label = strings.continueBtn,
            enabled = selectedRegion.isNotBlank(),
            onClick = onContinue
        )
    }
}

// ============================================================================
// STEP D: University Selection (Featured + Full Directory + Self-Service Custom Input)
// ============================================================================
@Composable
private fun UniversitySelectionStep(
    strings: OnboardingSurveyStrings,
    countryName: String,
    regionName: String,
    universities: List<UniversityDirectoryEntry>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedUniversityName: String,
    isCustomUniversity: Boolean,
    isVerifiedStudentEmail: Boolean,
    onSelectUniversity: (UniversityDirectoryEntry) -> Unit,
    onSelectCustomUniversity: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    var customInputText by remember { mutableStateOf(if (isCustomUniversity) selectedUniversityName else "") }

    val featuredUnis = remember(universities) { universities.filter { it.featuredOrder > 0 } }
    val otherUnis = remember(universities) { universities.filter { it.featuredOrder == 0 } }
    val locationContext = if (regionName.isNotBlank()) "$regionName, $countryName" else countryName

    Column(modifier = Modifier.fillMaxSize()) {
        SurveyTopBar(showBack = true, onBack = onBack)

        QuestionHeaderWithMascot(prompt = strings.selectUniversityTitle)

        Text(
            text = String.format(strings.selectUniversitySubtitleFormat, locationContext),
            fontSize = 13.5.sp,
            color = MutedText,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        if (isVerifiedStudentEmail) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .background(VerifiedGreenBg, RoundedCornerShape(4.dp))
                    .border(1.dp, VerifiedGreenText.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = VerifiedGreenText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${strings.verifiedEmailBadge}: $selectedUniversityName",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VerifiedGreenText
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SquareSearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = strings.searchUniversityPlaceholder,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "self_service_custom_uni") {
                val customLabel = if (isCustomUniversity && selectedUniversityName.isNotBlank()) {
                    "$selectedUniversityName (Custom)"
                } else {
                    strings.unlistedUniversityOption
                }
                SquareSurveyOptionCard(
                    text = customLabel,
                    selected = isCustomUniversity,
                    onClick = { showCustomDialog = true },
                    trailingBadge = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }

            if (featuredUnis.isNotEmpty()) {
                item(key = "header_featured_unis") {
                    Text(
                        text = strings.featuredCampusesHeader,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                items(featuredUnis, key = { "feat_uni_${it.name}_${it.domain}" }) { uni ->
                    val sub = buildString {
                        if (uni.region.isNotBlank()) append(uni.region)
                        if (uni.domain.isNotBlank()) {
                            if (isNotEmpty()) append(" • ")
                            append(uni.domain)
                        }
                    }
                    SquareSurveyOptionCard(
                        text = uni.name,
                        subtitle = sub.ifBlank { null },
                        selected = !isCustomUniversity && uni.name == selectedUniversityName,
                        onClick = { onSelectUniversity(uni) }
                    )
                }
            }

            if (otherUnis.isNotEmpty()) {
                item(key = "header_other_unis") {
                    Text(
                        text = strings.allCampusesHeader,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SubtleGrey,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                items(otherUnis, key = { "other_uni_${it.name}_${it.domain}" }) { uni ->
                    val sub = buildString {
                        if (uni.region.isNotBlank()) append(uni.region)
                        if (uni.domain.isNotBlank()) {
                            if (isNotEmpty()) append(" • ")
                            append(uni.domain)
                        }
                    }
                    SquareSurveyOptionCard(
                        text = uni.name,
                        subtitle = sub.ifBlank { null },
                        selected = !isCustomUniversity && uni.name == selectedUniversityName,
                        onClick = { onSelectUniversity(uni) }
                    )
                }
            }
        }

        StickyContinueFooter(
            label = strings.continueBtn,
            enabled = selectedUniversityName.isNotBlank(),
            onClick = onContinue
        )
    }

    if (showCustomDialog) {
        Dialog(onDismissRequest = { showCustomDialog = false }) {
            Surface(
                shape = RectangleShape,
                color = PureWhite,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, BrandPrimary, RectangleShape)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Text(
                        text = strings.customUniversityDialogTitle,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkBlack
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    SquareSearchField(
                        value = customInputText,
                        onValueChange = { customInputText = it },
                        placeholder = strings.customUniversityInputPlaceholder
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = {
                            val clean = customInputText.trim()
                            if (clean.isNotEmpty()) {
                                onSelectCustomUniversity(clean)
                                showCustomDialog = false
                            }
                        },
                        enabled = customInputText.trim().isNotEmpty(),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPrimary,
                            contentColor = PureWhite,
                            disabledContainerColor = DisabledPillGrey
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = strings.customUniversityConfirmBtn,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// STEP E: Welcome to Campus Network (Exact Replica of Reference Image 1)
// ============================================================================
@Composable
private fun WelcomeSurveyStep(
    strings: OnboardingSurveyStrings,
    universityName: String,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.9f))

        CampusGuideNotepadIllustration(
            modifier = Modifier.size(width = 200.dp, height = 220.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = strings.welcomeTitle,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = InkBlack,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        if (universityName.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = universityName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = strings.welcomeBody,
            fontSize = 15.sp,
            lineHeight = 23.sp,
            color = InkBlack,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 28.dp)
        )

        Spacer(modifier = Modifier.weight(1.1f))

        DualActionFooter(
            continueLabel = strings.continueBtn,
            skipLabel = strings.skipBtn,
            onContinue = onContinue,
            onSkip = onSkip
        )
    }
}

// ============================================================================
// STEP F: Survey 1 / 4 — Faculty & Year of Study
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SurveyAcademicStep(
    strings: OnboardingSurveyStrings,
    languageCode: String,
    selectedFaculty: String,
    selectedYear: String,
    onSelectFaculty: (String) -> Unit,
    onSelectYear: (String) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize()) {
        SurveyTopBar(
            showBack = true,
            stepText = "1 / 4",
            skipLabel = strings.skipTop,
            onBack = onBack,
            onSkip = onSkip
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(bottom = 16.dp)
        ) {
            QuestionHeaderWithMascot(prompt = strings.q1Prompt)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = strings.q1YearSectionLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BrandPrimary,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OnboardingSurveyCatalog.yearsOfStudy.forEach { option ->
                    val isSelected = selectedYear == option.id
                    Box(
                        modifier = Modifier
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) BrandPrimary else UnselectedBorder,
                                shape = RectangleShape
                            )
                            .background(if (isSelected) BrandSelectedBg.copy(alpha = 0.25f) else PureWhite)
                            .clickable { onSelectYear(option.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.label(languageCode),
                            fontSize = 14.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) BrandPrimary else InkBlack
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = strings.q1FacultySectionLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BrandPrimary,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OnboardingSurveyCatalog.faculties.forEach { faculty ->
                    SquareSurveyOptionCard(
                        text = faculty.label(languageCode),
                        selected = selectedFaculty == faculty.id,
                        onClick = { onSelectFaculty(faculty.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            RecommendationCalloutWithMascotRow(text = strings.q1CalloutBubble)
        }

        StickyContinueFooter(
            label = strings.continueBtn,
            enabled = selectedFaculty.isNotBlank() && selectedYear.isNotBlank(),
            onClick = onContinue
        )
    }
}

// ============================================================================
// STEP G: Survey 2 / 4 — Primary Intent (Exact Replica of Reference Images 2 & 3)
// ============================================================================
@Composable
private fun SurveyIntentStep(
    strings: OnboardingSurveyStrings,
    languageCode: String,
    selectedIntents: List<String>,
    onToggleIntent: (String) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize()) {
        SurveyTopBar(
            showBack = true,
            stepText = "2 / 4",
            skipLabel = strings.skipTop,
            onBack = onBack,
            onSkip = onSkip
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            QuestionHeaderWithMascot(prompt = strings.q2Prompt)

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OnboardingSurveyCatalog.intents.forEach { option ->
                    val isSelected = option.id in selectedIntents
                    SquareSurveyOptionCard(
                        text = option.label(languageCode),
                        selected = isSelected,
                        onClick = { onToggleIntent(option.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            RecommendationCalloutWithMascotRow(text = strings.q2CalloutBubble)
        }

        StickyContinueFooter(
            label = strings.continueBtn,
            enabled = selectedIntents.isNotEmpty(),
            onClick = onContinue
        )
    }
}

// ============================================================================
// STEP H: Survey 3 / 4 — Category Interests (Exact Replica of Reference Image 4)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SurveyCategoriesStep(
    strings: OnboardingSurveyStrings,
    languageCode: String,
    selectedCategories: List<String>,
    onToggleCategory: (String) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize()) {
        SurveyTopBar(
            showBack = true,
            stepText = "3 / 4",
            skipLabel = strings.skipTop,
            onBack = onBack,
            onSkip = onSkip
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(bottom = 20.dp)
        ) {
            QuestionHeaderWithMascot(prompt = strings.q3Prompt)

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OnboardingSurveyCatalog.categories.forEach { cat ->
                    val isSelected = cat.id in selectedCategories
                    Box(
                        modifier = Modifier
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) BrandPrimary else UnselectedBorder,
                                shape = RectangleShape
                            )
                            .background(if (isSelected) BrandSelectedBg.copy(alpha = 0.25f) else PureWhite)
                            .clickable { onToggleCategory(cat.id) }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat.label(languageCode),
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) BrandPrimary else InkBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        StickyContinueFooter(
            label = strings.continueBtn,
            enabled = selectedCategories.isNotEmpty(),
            onClick = onContinue
        )
    }
}

// ============================================================================
// STEP I: Survey 4 / 4 — Marketplace Priorities (Exact Replica of Reference Image 5)
// ============================================================================
@Composable
private fun SurveyPrioritiesStep(
    strings: OnboardingSurveyStrings,
    languageCode: String,
    selectedPriorities: List<String>,
    onTogglePriority: (String) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize()) {
        SurveyTopBar(
            showBack = true,
            stepText = "4 / 4",
            skipLabel = strings.skipTop,
            onBack = onBack,
            onSkip = onSkip
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(bottom = 20.dp)
        ) {
            QuestionHeaderWithMascot(prompt = strings.q4Prompt)

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OnboardingSurveyCatalog.priorities.forEach { priority ->
                    val isSelected = priority.id in selectedPriorities
                    SquareSurveyOptionCard(
                        text = priority.label(languageCode),
                        selected = isSelected,
                        onClick = { onTogglePriority(priority.id) }
                    )
                }
            }
        }

        StickyContinueFooter(
            label = strings.continueBtn,
            enabled = selectedPriorities.isNotEmpty(),
            onClick = onContinue
        )
    }
}

// ============================================================================
// STEP J: Push Notifications Opt-In (Exact Replica of Reference Image 6)
// ============================================================================
@Composable
private fun PushOptInStep(
    strings: OnboardingSurveyStrings,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SurveyTopBar(showBack = true, onBack = onSkip)

        QuestionHeaderWithMascot(prompt = strings.pushPrompt)

        Spacer(modifier = Modifier.weight(0.6f))

        CampusPushNotificationScene(
            modifier = Modifier.size(width = 270.dp, height = 220.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = strings.pushBody,
            fontSize = 15.sp,
            lineHeight = 23.sp,
            color = InkBlack,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        DualActionFooter(
            continueLabel = strings.continueBtn,
            skipLabel = strings.skipBtn,
            onContinue = onContinue,
            onSkip = onSkip
        )
    }
}

// ============================================================================
// STEP K: Personalized News & Recommendations Consent (Exact Replica of Reference Image 7)
// ============================================================================
@Composable
private fun PersonalizedNewsConsentStep(
    strings: OnboardingSurveyStrings,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SurveyTopBar(showBack = true, onBack = onContinue)

        Spacer(modifier = Modifier.weight(0.7f))

        CampusPersonalizedNewsScene(
            modifier = Modifier.size(width = 265.dp, height = 225.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = strings.personalizedTitle,
            fontSize = 21.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            color = InkBlack,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = strings.personalizedBody,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = InkBlack,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        )

        Spacer(modifier = Modifier.weight(1.2f))

        StickyContinueFooter(
            label = strings.continueBtn,
            enabled = true,
            onClick = onContinue
        )
    }
}
