package com.campus.network.feature.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.network.core.data.AcademicChannelsRepository
import com.campus.network.core.model.AcademicChannel
import com.campus.network.feature.channels.model.CareerCourse
import com.campus.network.feature.channels.model.ChannelViewMode
import com.campus.network.feature.channels.model.CourseMate
import com.campus.network.feature.channels.model.InDemandCompany
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChannelsUiState(
    val viewMode: ChannelViewMode = ChannelViewMode.CHANNELS_HUB,
    val selectedFaculty: String = "Faculty of Engineering & Technology",
    val faculties: List<String> = listOf(
        "Faculty of Engineering & Technology",
        "School of Computing & Informatics",
        "Faculty of Applied Sciences"
    ),
    val selectedDepartment: String = "All",
    val departments: List<String> = emptyList(),
    val courses: List<CareerCourse> = emptyList(),
    val isCoursesExpanded: Boolean = false,
    val companies: List<InDemandCompany> = emptyList(),
    val selectedCourseForDiscovery: CareerCourse? = null,
    val selectedStudentForProfile: CourseMate? = null,
    val discoverMates: List<CourseMate> = emptyList(),
    val likedStudentIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val activeChannel: AcademicChannel? = null,
    val feedbackMessage: String? = null
)

sealed interface ChannelsEvent {
    data class SelectFaculty(val facultyName: String) : ChannelsEvent
    data class SelectDepartment(val departmentName: String) : ChannelsEvent
    data object ToggleCoursesExpanded : ChannelsEvent
    data class Search(val query: String) : ChannelsEvent
    data class SelectCourse(val course: CareerCourse) : ChannelsEvent
    data class OpenDiscovery(val course: CareerCourse) : ChannelsEvent
    data class OpenStudentProfile(val student: CourseMate) : ChannelsEvent
    data object BackToPreviousMode : ChannelsEvent
    data class ToggleLikeStudent(val studentId: String) : ChannelsEvent
    data class ClearFeedback(val message: String? = null) : ChannelsEvent
    data object ClearSelectedChannel : ChannelsEvent
}

class ChannelsViewModel(
    private val repository: AcademicChannelsRepository
) : ViewModel() {

    private val _viewMode = MutableStateFlow(ChannelViewMode.CHANNELS_HUB)
    private val _selectedFaculty = MutableStateFlow("Faculty of Engineering & Technology")
    private val _selectedDepartment = MutableStateFlow("All")
    private val _isCoursesExpanded = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedChannel = MutableStateFlow<AcademicChannel?>(null)
    private val _selectedCourseForDiscovery = MutableStateFlow<CareerCourse?>(null)
    private val _selectedStudentForProfile = MutableStateFlow<CourseMate?>(null)
    private val _likedStudentIds = MutableStateFlow<Set<String>>(emptySet())
    private val _feedbackMessage = MutableStateFlow<String?>(null)

    // Master Course-Mates Catalog
    private val sampleCourseMates = listOf(
        CourseMate(
            id = "mate_rk",
            name = "Rk",
            ageOrYear = "27",
            major = "Electrical & Embedded Engineering",
            faculty = "Faculty of Engineering & Technology",
            isVerified = true,
            isHelper = true,
            helperBadgeText = "Academic Workspace Helper \u00B7 Top 5% in Circuits",
            studyGoal = "Building prototype PCB schematics \u00B7 Seeking study buddy for weekly problem sets",
            studyStyle = "Hardware Lab & Discussion",
            availabilityText = "Weekday Evenings & Weekends",
            sharedCourses = listOf("#eng-201", "#ee-310"),
            photoResName = "student_rk",
            bio = "Senior engineering student passionate about low-power IoT devices, firmware architecture, and mentoring first-year peers.",
            karmaScore = 840
        ),
        CourseMate(
            id = "mate_eliza",
            name = "Eliza",
            ageOrYear = "21",
            major = "HCI & Product Design",
            faculty = "Faculty of Engineering & Technology",
            isVerified = true,
            isHelper = true,
            helperBadgeText = "Academic Workspace Helper",
            studyGoal = "Need study buddy for usability testing heuristics and design telemetry",
            studyStyle = "Visual Flowcharts & Interactive Reviews",
            availabilityText = "Afternoons & Evenings",
            sharedCourses = listOf("#eng-201", "#eng-202"),
            photoResName = "student_eliza",
            bio = "Product designer and researcher. I love breaking down complex user telemetry data into intuitive systems.",
            karmaScore = 620
        ),
        CourseMate(
            id = "mate_clin",
            name = "Clin",
            ageOrYear = "22",
            major = "Autonomous Robotics & Mechatronics",
            faculty = "Faculty of Engineering & Technology",
            isVerified = true,
            isHelper = false,
            helperBadgeText = null,
            studyGoal = "Exam prep on ROS2 kinematics & path planning algorithms",
            studyStyle = "Code & Simulation Sprints",
            availabilityText = "Night Owl Sprints",
            sharedCourses = listOf("#eng-201", "#me-340"),
            photoResName = "student_clin",
            bio = "Maker and roboticist. Working on autonomous campus delivery drones.",
            karmaScore = 310
        ),
        CourseMate(
            id = "mate_junebog",
            name = "June",
            ageOrYear = "23",
            major = "Distributed Systems & Telemetry",
            faculty = "Faculty of Engineering & Technology",
            isVerified = true,
            isHelper = true,
            helperBadgeText = "Lead Campus Peer Mentor",
            studyGoal = "Benchmarking telemetry pipelines and concurrency locks",
            studyStyle = "Theory & Whiteboard Sessions",
            availabilityText = "Mornings & Weekends",
            sharedCourses = listOf("#eng-201", "#se-305"),
            photoResName = "student_june",
            bio = "Passionate about high-throughput distributed message queues and helping classmates debug concurrency deadlocks.",
            karmaScore = 920
        ),
        CourseMate(
            id = "mate_andrew",
            name = "Andrew",
            ageOrYear = "22",
            major = "Mechanical & Aerospace Systems",
            faculty = "Faculty of Engineering & Technology",
            isVerified = true,
            isHelper = false,
            helperBadgeText = null,
            studyGoal = "Finite element analysis review for midterm prep",
            studyStyle = "Practice Exams & Problem Solving",
            availabilityText = "Tuesday & Thursday Evenings",
            sharedCourses = listOf("#eng-201", "#me-210"),
            photoResName = "student_andrew",
            bio = "Aerospace enthusiast, CAD designer, and always down for coffee and physics derivations.",
            karmaScore = 280
        )
    )

    private val allEngineeringCourses = listOf(
        CareerCourse(
            id = "ux_design_2",
            title = "UX Design: 2 Analyzing User Data",
            courseCode = "#eng-201",
            thumbnailResName = "course_thumb_1",
            faculty = "Faculty of Engineering & Technology",
            department = "HCI & Design Systems",
            enrolledMatesCount = 18,
            enrolledMates = sampleCourseMates,
            hasActiveHelper = true,
            activeDiscussionSnippet = "Session on usability telemetry metrics uploaded."
        ),
        CareerCourse(
            id = "ux_foundations",
            title = "UX Foundations: Making the Case for Usability Testing",
            courseCode = "#eng-202",
            thumbnailResName = "course_thumb_2",
            faculty = "Faculty of Engineering & Technology",
            department = "HCI & Design Systems",
            enrolledMatesCount = 12,
            enrolledMates = sampleCourseMates.take(3),
            hasActiveHelper = true,
            activeDiscussionSnippet = "Sample stakeholder presentation templates shared."
        ),
        CareerCourse(
            id = "interaction_design_flow",
            title = "Interaction Design: Flow",
            courseCode = "#eng-203",
            thumbnailResName = "course_thumb_3",
            faculty = "Faculty of Engineering & Technology",
            department = "HCI & Design Systems",
            enrolledMatesCount = 15,
            enrolledMates = sampleCourseMates.drop(1),
            hasActiveHelper = true,
            activeDiscussionSnippet = "State machine flow diagrams for mobile apps."
        ),
        CareerCourse(
            id = "embedded_microcontrollers",
            title = "Embedded Microcontrollers & IoT Architecture",
            courseCode = "#ee-310",
            thumbnailResName = "course_thumb_4",
            faculty = "Faculty of Engineering & Technology",
            department = "Electrical & Embedded",
            enrolledMatesCount = 24,
            enrolledMates = listOf(sampleCourseMates[0], sampleCourseMates[3]),
            hasActiveHelper = true,
            activeDiscussionSnippet = "Oscilloscope lab slots available between 2 PM and 5 PM."
        ),
        CareerCourse(
            id = "robotics_kinematics",
            title = "Autonomous Robotics & Kinematics Control",
            courseCode = "#me-340",
            thumbnailResName = "course_thumb_5",
            faculty = "Faculty of Engineering & Technology",
            department = "Mechatronics & Robotics",
            enrolledMatesCount = 16,
            enrolledMates = listOf(sampleCourseMates[2], sampleCourseMates[4]),
            hasActiveHelper = false,
            activeDiscussionSnippet = "ROS2 navigation stack configuration notes."
        ),
        CareerCourse(
            id = "distributed_cloud_telemetry",
            title = "Distributed Systems & Cloud Telemetry",
            courseCode = "#se-305",
            thumbnailResName = "course_thumb_6",
            faculty = "Faculty of Engineering & Technology",
            department = "Software & Systems",
            enrolledMatesCount = 20,
            enrolledMates = listOf(sampleCourseMates[3], sampleCourseMates[0]),
            hasActiveHelper = true,
            activeDiscussionSnippet = "Telemetry pipeline benchmark comparison charts."
        ),
        CareerCourse(
            id = "signal_processing",
            title = "Signal Processing & Fourier Analytics",
            courseCode = "#ee-220",
            thumbnailResName = "course_thumb_1",
            faculty = "Faculty of Engineering & Technology",
            department = "Electrical & Embedded",
            enrolledMatesCount = 11,
            enrolledMates = listOf(sampleCourseMates[0], sampleCourseMates[1]),
            hasActiveHelper = true,
            activeDiscussionSnippet = "Fast Fourier Transform implementation in C++."
        ),
        CareerCourse(
            id = "renewable_energy_grids",
            title = "Renewable Energy Grids & Power Electronics",
            courseCode = "#ee-410",
            thumbnailResName = "course_thumb_2",
            faculty = "Faculty of Engineering & Technology",
            department = "Electrical & Embedded",
            enrolledMatesCount = 9,
            enrolledMates = listOf(sampleCourseMates[0]),
            hasActiveHelper = true,
            activeDiscussionSnippet = "Inverter phase sync simulation files uploaded."
        ),
        CareerCourse(
            id = "fluid_dynamics_fea",
            title = "Fluid Dynamics & Finite Element Analysis",
            courseCode = "#me-210",
            thumbnailResName = "course_thumb_3",
            faculty = "Faculty of Engineering & Technology",
            department = "Mechanical",
            enrolledMatesCount = 14,
            enrolledMates = listOf(sampleCourseMates[4], sampleCourseMates[2]),
            hasActiveHelper = false,
            activeDiscussionSnippet = "Boundary condition setup guide for ANSYS Fluent."
        )
    )

    private val companiesList = listOf(
        InDemandCompany(
            id = "data_annotation",
            name = "DataAnnotation",
            jobPostsCountText = "510 job posts",
            demandMetricText = "\u25CF 0.0%",
            isPositiveTrend = false,
            logoResName = "company_da",
            initials = "DA"
        ),
        InDemandCompany(
            id = "google_eng",
            name = "Google Engineering",
            jobPostsCountText = "342 job posts",
            demandMetricText = "\u25CF +14.2%",
            isPositiveTrend = true,
            initials = "G"
        ),
        InDemandCompany(
            id = "tesla_energy",
            name = "Tesla Energy & Robotics",
            jobPostsCountText = "189 job posts",
            demandMetricText = "\u25CF +8.5%",
            isPositiveTrend = true,
            initials = "T"
        ),
        InDemandCompany(
            id = "safaricom_labs",
            name = "Safaricom Tech Labs",
            jobPostsCountText = "126 job posts",
            demandMetricText = "\u25CF +5.1%",
            isPositiveTrend = true,
            initials = "S"
        )
    )

    val uiState: StateFlow<ChannelsUiState> = combine(
        _viewMode,
        _selectedFaculty,
        _selectedDepartment,
        _isCoursesExpanded,
        _searchQuery,
        _selectedCourseForDiscovery,
        _selectedStudentForProfile,
        _likedStudentIds,
        _feedbackMessage
    ) { args: Array<Any?> ->
        val viewMode = args[0] as ChannelViewMode
        val faculty = args[1] as String
        val department = args[2] as String
        val isExpanded = args[3] as Boolean
        val query = args[4] as String
        val selectedCourse = args[5] as CareerCourse?
        val selectedStudent = args[6] as CourseMate?
        @Suppress("UNCHECKED_CAST")
        val likedIds = args[7] as Set<String>
        val feedback = args[8] as String?

        val rawCourses = allEngineeringCourses
        val departments = listOf("All") + rawCourses.map { it.department }.distinct()

        val filteredCourses = rawCourses.filter { course ->
            val matchesDept = department == "All" || course.department == department
            val matchesQuery = query.isBlank() ||
                    course.title.contains(query, ignoreCase = true) ||
                    course.courseCode.contains(query, ignoreCase = true) ||
                    course.department.contains(query, ignoreCase = true)
            matchesDept && matchesQuery
        }

        val displayCourses = if (isExpanded || filteredCourses.size <= 3) {
            filteredCourses
        } else {
            filteredCourses.take(3)
        }

        val discoverMates = selectedCourse?.enrolledMates ?: sampleCourseMates

        ChannelsUiState(
            viewMode = viewMode,
            selectedFaculty = faculty,
            selectedDepartment = department,
            departments = departments,
            courses = displayCourses,
            isCoursesExpanded = isExpanded,
            companies = companiesList,
            selectedCourseForDiscovery = selectedCourse,
            selectedStudentForProfile = selectedStudent,
            discoverMates = discoverMates,
            likedStudentIds = likedIds,
            searchQuery = query,
            feedbackMessage = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChannelsUiState()
    )

    fun onEvent(event: ChannelsEvent) {
        when (event) {
            is ChannelsEvent.SelectFaculty -> {
                _selectedFaculty.value = event.facultyName
                _selectedDepartment.value = "All"
                _isCoursesExpanded.value = false
            }
            is ChannelsEvent.SelectDepartment -> {
                _selectedDepartment.value = event.departmentName
            }
            is ChannelsEvent.ToggleCoursesExpanded -> {
                _isCoursesExpanded.value = !_isCoursesExpanded.value
            }
            is ChannelsEvent.Search -> _searchQuery.value = event.query
            is ChannelsEvent.SelectCourse -> {
                val dummyChannel = AcademicChannel(
                    id = event.course.id,
                    courseCode = event.course.courseCode,
                    courseTitle = event.course.title,
                    faculty = event.course.faculty,
                    department = event.course.department,
                    unreadCount = 0,
                    lastMessageSnippet = event.course.activeDiscussionSnippet,
                    lastMessageTime = "Just now",
                    activeStudentsCount = event.course.enrolledMatesCount,
                    isPinned = true
                )
                _selectedChannel.value = dummyChannel
                viewModelScope.launch {
                    repository.markChannelRead(event.course.id)
                }
            }
            is ChannelsEvent.OpenDiscovery -> {
                _selectedCourseForDiscovery.value = event.course
                _viewMode.value = ChannelViewMode.DISCOVER_CAROUSEL
            }
            is ChannelsEvent.OpenStudentProfile -> {
                _selectedStudentForProfile.value = event.student
                _viewMode.value = ChannelViewMode.STUDENT_PROFILE
            }
            is ChannelsEvent.BackToPreviousMode -> {
                when (_viewMode.value) {
                    ChannelViewMode.STUDENT_PROFILE -> {
                        _viewMode.value = ChannelViewMode.DISCOVER_CAROUSEL
                    }
                    ChannelViewMode.DISCOVER_CAROUSEL -> {
                        _viewMode.value = ChannelViewMode.CHANNELS_HUB
                        _selectedCourseForDiscovery.value = null
                    }
                    ChannelViewMode.CHANNELS_HUB -> Unit
                }
            }
            is ChannelsEvent.ToggleLikeStudent -> {
                val current = _likedStudentIds.value.toMutableSet()
                if (current.contains(event.studentId)) {
                    current.remove(event.studentId)
                    _feedbackMessage.value = "Removed from your study connections"
                } else {
                    current.add(event.studentId)
                    _feedbackMessage.value = "Study connection request sent!"
                }
                _likedStudentIds.value = current
            }
            is ChannelsEvent.ClearFeedback -> _feedbackMessage.value = null
            is ChannelsEvent.ClearSelectedChannel -> _selectedChannel.value = null
        }
    }
}
