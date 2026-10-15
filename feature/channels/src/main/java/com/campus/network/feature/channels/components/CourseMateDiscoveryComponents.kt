package com.campus.network.feature.channels.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.network.core.ui.theme.BrandBluePrimary
import com.campus.network.core.ui.theme.BrandGold
import com.campus.network.core.ui.theme.BrandGoldSoft
import com.campus.network.core.ui.theme.BrandGoldText
import com.campus.network.core.ui.theme.TextPrimary
import com.campus.network.core.ui.theme.TextSecondary
import com.campus.network.feature.channels.R
import com.campus.network.feature.channels.model.CareerCourse
import com.campus.network.feature.channels.model.CourseMate

private val BrandNavy = BrandBluePrimary
private val BrandYellowPill = BrandGoldSoft
private val TextYellowPill = BrandGoldText
private val TextNavy = TextPrimary
private val TextSlate = TextSecondary

/**
 * Avatar Stack Lineup: (o o o o +14 more)
 * Displayed under each course card to show enrolled course-mates.
 */
@Composable
fun CourseMateAvatarLineup(
    course: CareerCourse,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mates = course.enrolledMates
    val visibleMates = mates.take(4)
    val remaining = course.enrolledMatesCount - visibleMates.size

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onMoreClick
        )
    ) {
        // Overlapping Avatars Stack
        Box {
            visibleMates.forEachIndexed { index, mate ->
                val avatarRes = getStudentPhotoDrawable(mate.photoResName)
                Image(
                    painter = painterResource(avatarRes),
                    contentDescription = mate.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(start = (index * 18).dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // "+14 more" Interactive Link
        Surface(
            shape = CircleShape,
            color = Color(0xFFF1F5F9),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "+$remaining more",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandNavy
                )
            }
        }
    }
}

/**
 * Discovery Screen:
 * Direct recreation of the reference design for Discovering Course-Mates.
 */
@Composable
fun CourseMateDiscoverScreen(
    course: CareerCourse?,
    mates: List<CourseMate>,
    likedIds: Set<String>,
    onBackClick: () -> Unit,
    onStudentClick: (CourseMate) -> Unit,
    onToggleLike: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // 1. Top Bar: Back Icon + Large "Discover" + Info Question Icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextNavy,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Discover",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextNavy,
                    letterSpacing = (-0.5).sp
                )
            }

            // Info Question Mark Circle
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "Info",
                    tint = TextNavy,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 2. Yellow Refresh Pill
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            Surface(
                shape = CircleShape,
                color = BrandYellowPill
            ) {
                Text(
                    text = "See new people in 18 hours",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextYellowPill,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Subtitle Description
        Text(
            text = "Connect over common ground with people who match your vibe, refreshed every day.",
            fontSize = 14.5.sp,
            color = TextNavy,
            lineHeight = 21.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 4. "Recommended for you" Section
        Text(
            text = "Recommended for you",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextNavy,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Horizontal Card Carousel with Peek Effect
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            mates.forEach { mate ->
                val isLiked = likedIds.contains(mate.id)

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxSize()
                        .padding(vertical = 12.dp)
                        .clickable { onStudentClick(mate) }
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Large Portrait Image
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        ) {
                            val photoRes = getStudentPhotoDrawable(mate.photoResName)
                            Image(
                                painter = painterResource(photoRes),
                                contentDescription = mate.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Helper Badge Overlay
                            if (mate.isHelper) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = BrandGold,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Peer Helper",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Info Bar inside Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${mate.name}, ${mate.ageOrYear}",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextNavy
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (mate.isVerified) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified Student",
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }

                            // Heart Button
                            IconButton(
                                onClick = { onToggleLike(mate.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Connect with ${mate.name}",
                                    tint = if (isLiked) Color(0xFFEF4444) else TextNavy,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 6. Subtext Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = TextSlate,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Based on your registered course ${course?.courseCode ?: "#eng-201"} and study goals",
                fontSize = 12.sp,
                color = TextSlate,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Individual Student Profile View Screen:
 * Detailed profile inspecting a classmate's study DNA, helper rating, and shared courses.
 */
@Composable
fun IndividualStudentProfileScreen(
    student: CourseMate,
    isLiked: Boolean,
    onBackClick: () -> Unit,
    onConnectClick: () -> Unit,
    onMessageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val photoRes = getStudentPhotoDrawable(student.photoResName)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 90.dp)
        ) {
            // 1. Large Hero Photo Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
            ) {
                Image(
                    painter = painterResource(photoRes),
                    contentDescription = student.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Gradient Scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent),
                                endY = 220f
                            )
                        )
                )

                // Back Button
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(8.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. Identity Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${student.name}, ${student.ageOrYear}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextNavy
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (student.isVerified) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified Student",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Karma Score Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${student.karmaScore} XP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${student.major} \u00B7 ${student.faculty}",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSlate
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Helper Recognition Card
                if (student.isHelper) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF08A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF854D0E),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = student.helperBadgeText ?: "Academic Workspace Helper",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextNavy
                                )
                                Text(
                                    text = "Certified peer mentor assisting classmates in problem sets",
                                    fontSize = 12.sp,
                                    color = TextSlate
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 3. Study Goals & Style
                Text(
                    text = "Study Persona & Goals",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextNavy
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Current Goal:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSlate
                        )
                        Text(
                            text = student.studyGoal,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextNavy
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Study Style & Availability:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSlate
                        )
                        Text(
                            text = "${student.studyStyle} \u00B7 ${student.availabilityText}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextNavy
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 4. Shared Registered Courses
                Text(
                    text = "Shared Registered Courses",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextNavy
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    student.sharedCourses.forEach { code ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE0F2FE),
                            border = BorderStroke(1.dp, Color(0xFFBAE6FD))
                        ) {
                            Text(
                                text = code,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandNavy,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 5. About Bio
                Text(
                    text = "About",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextNavy
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = student.bio,
                    fontSize = 14.sp,
                    color = Color(0xFF334155),
                    lineHeight = 21.sp
                )
            }
        }

        // 6. Fixed Bottom Action Dock
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Color.White,
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Outlined Message Button
                OutlinedButton(
                    onClick = onMessageClick,
                    shape = CircleShape,
                    border = BorderStroke(1.5.dp, BrandNavy),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = BrandNavy,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Message",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy
                    )
                }

                // Primary Send Study Request Button
                Button(
                    onClick = onConnectClick,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLiked) "Request Sent" else "Study Request",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Resolves student photo drawable.
 */
fun getStudentPhotoDrawable(name: String): Int {
    return when (name) {
        "student_rk" -> R.drawable.student_rk
        "student_eliza" -> R.drawable.student_eliza
        "student_clin" -> R.drawable.student_clin
        "student_june" -> R.drawable.student_june
        "student_andrew" -> R.drawable.student_andrew
        else -> R.drawable.student_rk
    }
}
