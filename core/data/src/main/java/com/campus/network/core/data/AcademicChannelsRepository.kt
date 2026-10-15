package com.campus.network.core.data

import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.database.entity.AcademicChannelEntity
import com.campus.network.core.model.AcademicChannel
import com.campus.network.core.model.DepartmentAccordion
import com.campus.network.core.model.FacultyDepartmentGroup
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository for Tab 2: Channels (Academic Workspace).
 * Groups course codes (e.g. #cs-201) into nested, cascading accordion lists.
 */
class AcademicChannelsRepository(
    private val database: CampusDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val dao = database.academicChannelDao()

    fun getFacultyGroups(): Flow<List<FacultyDepartmentGroup>> {
        return dao.getAllChannels()
            .map { entities ->
                val domainChannels = entities.map { it.toDomain() }

                // Group by faculty -> group by department -> courses
                domainChannels.groupBy { it.faculty }
                    .map { (faculty, facultyCourses) ->
                        val deptGroups = facultyCourses.groupBy { it.department }
                            .map { (dept, deptCourses) ->
                                DepartmentAccordion(
                                    departmentName = dept,
                                    courses = deptCourses
                                )
                            }
                        FacultyDepartmentGroup(
                            facultyName = faculty,
                            departments = deptGroups
                        )
                    }
            }
            .flowOn(ioDispatcher)
    }

    suspend fun markChannelRead(channelId: String) = withContext(ioDispatcher) {
        dao.markChannelRead(channelId)
    }

    private fun AcademicChannelEntity.toDomain() = AcademicChannel(
        id = id,
        courseCode = courseCode,
        courseTitle = courseTitle,
        faculty = faculty,
        department = department,
        unreadCount = unreadCount,
        lastMessageSnippet = lastMessageSnippet,
        lastMessageTime = lastMessageTime,
        activeStudentsCount = activeStudentsCount,
        isPinned = isPinned
    )
}
