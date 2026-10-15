package com.campus.network.feature.channels

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campus.network.feature.channels.components.CourseMateAvatarLineup
import com.campus.network.feature.channels.components.CourseMateDiscoverScreen
import com.campus.network.feature.channels.components.IndividualStudentProfileScreen
import com.campus.network.feature.channels.model.CareerCourse
import com.campus.network.feature.channels.model.ChannelViewMode
import com.campus.network.feature.channels.model.InDemandCompany
import com.campus.network.core.ui.theme.BrandBlueInteractive
import com.campus.network.core.ui.theme.BrandBluePrimary
import com.campus.network.core.ui.theme.BrandGold
import com.campus.network.core.ui.theme.BrandGoldDark
import com.campus.network.core.ui.theme.SurfaceSubtle
import com.campus.network.core.ui.theme.TextPrimary
import com.campus.network.core.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private val BrandNavy = BrandBluePrimary
private val BrandGoldColor = BrandGold
private val BrandGoldDarkColor = BrandGoldDark
private val TextPrimaryNavy = TextPrimary
private val TextSecondarySlate = TextSecondary
private val LinkBlue = BrandBlueInteractive
private val SectionDividerBg = SurfaceSubtle

/**
 * Pixel-Perfect Academic Workspace & Career Channels Screen:
 * Complete UI flow with peer course-mate matching and discovery:
 * - CHANNELS_HUB: LinkedIn-inspired Academic Workspace with live course-mate avatar lineups
 * - DISCOVER_CAROUSEL: Full-screen Discover screen matching user reference image
 * - STUDENT_PROFILE: Individual student profile view with Helper rewards, study goals & actions
 */
@Composable
fun ChannelsScreen(
    viewModel: ChannelsViewModel,
    onNavigateToChannelChat: (String) -> Unit = {},
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Android back navigation per view mode
    BackHandler(enabled = uiState.viewMode != ChannelViewMode.CHANNELS_HUB) {
        viewModel.onEvent(ChannelsEvent.BackToPreviousMode)
    }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onEvent(ChannelsEvent.ClearFeedback())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        AnimatedContent(
            targetState = uiState.viewMode,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "ChannelsViewModeTransition"
        ) { mode ->
            when (mode) {
                ChannelViewMode.CHANNELS_HUB -> {
                    ChannelsHubContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onBackClick = onBackClick,
                        onOpenDiscovery = { course ->
                            viewModel.onEvent(ChannelsEvent.OpenDiscovery(course))
                        },
                        onCourseClick = { course ->
                            viewModel.onEvent(ChannelsEvent.SelectCourse(course))
                            onNavigateToChannelChat(course.id)
                        },
                        onCustomFilterClick = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Filtering registered courses for your engineering track")
                            }
                        },
                        onCompanyClick = { company ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Opening recruitment pipeline for ${company.name}")
                            }
                        }
                    )
                }

                ChannelViewMode.DISCOVER_CAROUSEL -> {
                    CourseMateDiscoverScreen(
                        course = uiState.selectedCourseForDiscovery,
                        mates = uiState.discoverMates,
                        likedIds = uiState.likedStudentIds,
                        onBackClick = {
                            viewModel.onEvent(ChannelsEvent.BackToPreviousMode)
                        },
                        onStudentClick = { student ->
                            viewModel.onEvent(ChannelsEvent.OpenStudentProfile(student))
                        },
                        onToggleLike = { studentId ->
                            viewModel.onEvent(ChannelsEvent.ToggleLikeStudent(studentId))
                        }
                    )
                }

                ChannelViewMode.STUDENT_PROFILE -> {
                    uiState.selectedStudentForProfile?.let { student ->
                        IndividualStudentProfileScreen(
                            student = student,
                            isLiked = uiState.likedStudentIds.contains(student.id),
                            onBackClick = {
                                viewModel.onEvent(ChannelsEvent.BackToPreviousMode)
                            },
                            onConnectClick = {
                                viewModel.onEvent(ChannelsEvent.ToggleLikeStudent(student.id))
                            },
                            onMessageClick = {
                                val channelId = student.sharedCourses.firstOrNull() ?: "course_eng_201"
                                onNavigateToChannelChat(channelId)
                            }
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 80.dp)
        )
    }
}

/**
 * Main Academic Workspace & Channels Hub:
 * Replicates the reference design layout with faculty switching,
 * course listings with course-mate avatar lineups, and recruitment pipeline.
 */
@Composable
private fun ChannelsHubContent(
    uiState: ChannelsUiState,
    viewModel: ChannelsViewModel,
    onBackClick: () -> Unit,
    onOpenDiscovery: (CareerCourse) -> Unit,
    onCourseClick: (CareerCourse) -> Unit,
    onCustomFilterClick: () -> Unit,
    onCompanyClick: (InDemandCompany) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // 1. Top Bar: Back Arrow + Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimaryNavy,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "Academic Workspace",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryNavy,
                letterSpacing = (-0.3).sp
            )
        }

        // Top Gold Accent Line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(BrandGold)
        )

        // 2. Faculty Switcher Pill Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            uiState.faculties.forEach { faculty ->
                val isSelected = faculty == uiState.selectedFaculty
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) TextPrimaryNavy else Color(0xFFF1F5F9),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) TextPrimaryNavy else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.clickable {
                        viewModel.onEvent(ChannelsEvent.SelectFaculty(faculty))
                    }
                ) {
                    Text(
                        text = if (faculty.contains("Engineering")) "Engineering & Tech" else faculty.removePrefix("Faculty of ").removePrefix("School of "),
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF334155),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 3. Scrollable Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // SECTION 1: Registered Courses & Course-Mates
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Title with Edit Circle Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Registered courses & course-mates",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimaryNavy,
                            letterSpacing = (-0.3).sp,
                            modifier = Modifier.weight(1f)
                        )

                        // Circular Filter Button
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            onClick = onCustomFilterClick
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Filter registered courses",
                                    tint = TextPrimaryNavy,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Subtitle
                    Text(
                        text = "Connect and study with peers in your enrolled faculty courses",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondarySlate
                    )


                    Spacer(modifier = Modifier.height(12.dp))

                    // Sub-Category / Department Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.departments.forEach { dept ->
                            val isDeptSelected = dept == uiState.selectedDepartment
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDeptSelected) Color(0xFFE0F2FE) else Color.White,
                                border = BorderStroke(
                                    1.dp,
                                    if (isDeptSelected) LinkBlue else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.clickable {
                                    viewModel.onEvent(ChannelsEvent.SelectDepartment(dept))
                                }
                            ) {
                                Text(
                                    text = dept,
                                    fontSize = 12.sp,
                                    fontWeight = if (isDeptSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isDeptSelected) LinkBlue else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Academic Workspace Helper Reward Banner
                    AcademicWorkspaceHelperCard(
                        onOpenDiscoveryFirstCourse = {
                            uiState.courses.firstOrNull()?.let { onOpenDiscovery(it) }
                        }
                    )
                }
            }

            // Course Item Rows with Live Avatar Lineup
            items(uiState.courses, key = { it.id }) { course ->
                CourseItemRow(
                    course = course,
                    onClick = { onCourseClick(course) },
                    onOpenDiscovery = { onOpenDiscovery(course) }
                )
                HorizontalDivider(
                    color = Color(0xFFF1F5F9),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Expand / Collapse Action: "Show all 9 courses"
            item {
                val isExpanded = uiState.isCoursesExpanded
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.onEvent(ChannelsEvent.ToggleCoursesExpanded)
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isExpanded) "Show less" else "Show all 9 courses",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = LinkBlue
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = LinkBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // SECTION SEPARATOR
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(SectionDividerBg)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(BrandGold)
                    )
                }
            }

            // SECTION 2: Companies Where This Role Is In Demand
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Companies where this role is in demand",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimaryNavy,
                        letterSpacing = (-0.3).sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Based on new job postings in the past 7 days",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondarySlate
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Company Cards
                    uiState.companies.forEach { company ->
                        CompanyDemandItem(
                            company = company,
                            onClick = { onCompanyClick(company) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

/**
 * Academic Workspace Helper card highlighting the peer reward and study buddy incentive system.
 */
@Composable
private fun AcademicWorkspaceHelperCard(
    onOpenDiscoveryFirstCourse: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDiscoveryFirstCourse),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF08A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF854D0E),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "Academic Workspace Helper",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryNavy
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE0F2FE)
                ) {
                    Text(
                        text = "14 Enrolled Helpers",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = LinkBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Match with course-mates taking your exact registered courses. Help peers with problem sets to earn karma points and verified Helper endorsements.",
                fontSize = 13.sp,
                color = TextSecondarySlate,
                lineHeight = 18.5.sp
            )
        }
    }
}

/**
 * Single Course Item matching the reference design layout:
 * Left 16:9 thumbnail + Right title, subtitle ("Course · 30m 25s"), viewer count,
 * AND live CourseMateAvatarLineup (o o o o +14 more) for peer matching.
 */
@Composable
private fun CourseItemRow(
    course: CareerCourse,
    onClick: () -> Unit,
    onOpenDiscovery: () -> Unit
) {
    val thumbnailRes = getCourseThumbnailDrawable(course.thumbnailResName)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // 16:9 Thumbnail
            Image(
                painter = painterResource(thumbnailRes),
                contentDescription = course.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(114.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Right Text Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.title,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryNavy,
                    lineHeight = 20.sp,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFDBEAFE))
                    ) {
                        Text(
                            text = course.courseCode,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = LinkBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = course.department,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondarySlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "${course.enrolledMatesCount} classmates enrolled",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondarySlate
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Course-Mate Lineup (o o o o +14 more)
        CourseMateAvatarLineup(
            course = course,
            onMoreClick = onOpenDiscovery,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Company where role is in demand row:
 * Logo (DataAnnotation DA logo or initials) + Name + Posts + Trend + Arrow.
 */
@Composable
private fun CompanyDemandItem(
    company: InDemandCompany,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Company Logo
            if (company.logoResName == "company_da") {
                Image(
                    painter = painterResource(R.drawable.company_da),
                    contentDescription = company.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = company.initials,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Middle: Name & Posts
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = company.name,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryNavy
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = company.jobPostsCountText,
                    fontSize = 13.5.sp,
                    color = TextSecondarySlate
                )
            }

            // Right: Trend Metric & Arrow
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = company.demandMetricText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (company.isPositiveTrend) Color(0xFF16A34A) else Color(0xFF475569)
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF334155),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Resolves course thumbnail drawable.
 */
private fun getCourseThumbnailDrawable(name: String): Int {
    return when (name) {
        "course_thumb_1" -> R.drawable.course_thumb_1
        "course_thumb_2" -> R.drawable.course_thumb_2
        "course_thumb_3" -> R.drawable.course_thumb_3
        "course_thumb_4" -> R.drawable.course_thumb_4
        "course_thumb_5" -> R.drawable.course_thumb_5
        "course_thumb_6" -> R.drawable.course_thumb_6
        else -> R.drawable.course_thumb_1
    }
}
