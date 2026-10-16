package com.campus.network.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.campus.network.core.database.dao.AcademicChannelDao
import com.campus.network.core.database.dao.ChatMessageDao
import com.campus.network.core.database.dao.FlashMeetDao
import com.campus.network.core.database.dao.MarketItemDao
import com.campus.network.core.database.dao.MicroGigDao
import com.campus.network.core.database.entity.AcademicChannelEntity
import com.campus.network.core.database.entity.ChatMessageEntity
import com.campus.network.core.database.entity.FlashMeetEntity
import com.campus.network.core.database.entity.MarketItemEntity
import com.campus.network.core.database.entity.MicroGigEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        FlashMeetEntity::class,
        AcademicChannelEntity::class,
        ChatMessageEntity::class,
        MarketItemEntity::class,
        MicroGigEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CampusDatabase : RoomDatabase() {
    abstract fun flashMeetDao(): FlashMeetDao
    abstract fun academicChannelDao(): AcademicChannelDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun marketItemDao(): MarketItemDao
    abstract fun microGigDao(): MicroGigDao

    companion object {
        @Volatile
        private var INSTANCE: CampusDatabase? = null

        fun getInstance(context: Context): CampusDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CampusDatabase::class.java,
                    "campus_network.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Prepopulate seed data asynchronously on Dispatchers.IO to ensure zero frame drops on first open
                            CoroutineScope(Dispatchers.IO).launch {
                                populateInitialCache(getInstance(context))
                            }
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }

        private suspend fun populateInitialCache(database: CampusDatabase) {
            val now = System.currentTimeMillis()

            // 1. Initial Flash Meets on Campus
            val sampleMeets = listOf(
                FlashMeetEntity(
                    id = "meet-1",
                    title = "Algorithmic Complexity Jam",
                    description = "Tackling Dynamic Programming sets before tomorrow's midterm. Bring your notebooks!",
                    categoryName = "STUDY_JAM",
                    latitude = -1.3090,
                    longitude = 36.8120,
                    locationName = "Turing Hall 3rd Floor Lounge",
                    creatorName = "Brian Ochieng",
                    creatorAvatar = null,
                    createdAtEpochMs = now - (15 * 60 * 1000),
                    expiresAtEpochMs = now + (45 * 60 * 1000),
                    attendeeCount = 5,
                    maxAttendees = 8
                ),
                FlashMeetEntity(
                    id = "meet-2",
                    title = "Solar Car Telemetry Sync",
                    description = "Debugging CAN-bus serial packet loss on prototype board #3.",
                    categoryName = "PROJECT_HACK",
                    latitude = -1.3105,
                    longitude = 36.8142,
                    locationName = "MakerSpace Lab A",
                    creatorName = "Wanjiku N.",
                    creatorAvatar = null,
                    createdAtEpochMs = now - (5 * 60 * 1000),
                    expiresAtEpochMs = now + (75 * 60 * 1000),
                    attendeeCount = 3,
                    maxAttendees = 6
                ),
                FlashMeetEntity(
                    id = "meet-3",
                    title = "Quick Coffee & Chapati Run",
                    description = "Heading down to Student Center cafeteria before the 11 AM lecture.",
                    categoryName = "FOOD_RUN",
                    latitude = -1.3082,
                    longitude = 36.8111,
                    locationName = "Student Center Atrium",
                    creatorName = "Kelvin Mutua",
                    creatorAvatar = null,
                    createdAtEpochMs = now - (2 * 60 * 1000),
                    expiresAtEpochMs = now + (20 * 60 * 1000),
                    attendeeCount = 2,
                    maxAttendees = 4
                )
            )
            database.flashMeetDao().insertMeets(sampleMeets)

            // 2. Initial Academic Channels
            val sampleChannels = listOf(
                AcademicChannelEntity(
                    id = "ch-cs201",
                    courseCode = "#cs-201",
                    courseTitle = "Data Structures & Algorithms",
                    faculty = "School of Computing & Informatics",
                    department = "Computer Science",
                    unreadCount = 4,
                    lastMessageSnippet = "Prof. Mwangi pushed the red-black tree test cases to git.",
                    lastMessageTime = "10:14 AM",
                    activeStudentsCount = 184,
                    isPinned = true
                ),
                AcademicChannelEntity(
                    id = "ch-cs305",
                    courseCode = "#cs-305",
                    courseTitle = "Operating Systems & Concurrency",
                    faculty = "School of Computing & Informatics",
                    department = "Computer Science",
                    unreadCount = 12,
                    lastMessageSnippet = "Can someone check my semaphore deadlock trace?",
                    lastMessageTime = "09:42 AM",
                    activeStudentsCount = 142,
                    isPinned = true
                ),
                AcademicChannelEntity(
                    id = "ch-se202",
                    courseCode = "#se-202",
                    courseTitle = "Software Architecture & Design Patterns",
                    faculty = "School of Computing & Informatics",
                    department = "Software Engineering",
                    unreadCount = 0,
                    lastMessageSnippet = "UDF and Clean Architecture slide deck uploaded.",
                    lastMessageTime = "Yesterday",
                    activeStudentsCount = 98,
                    isPinned = false
                ),
                AcademicChannelEntity(
                    id = "ch-ee310",
                    courseCode = "#ee-310",
                    courseTitle = "Embedded Microcontrollers",
                    faculty = "Faculty of Engineering & Technology",
                    department = "Electrical Engineering",
                    unreadCount = 2,
                    lastMessageSnippet = "Oscilloscope lab slots available between 2 PM and 5 PM.",
                    lastMessageTime = "08:30 AM",
                    activeStudentsCount = 76,
                    isPinned = false
                )
            )
            database.academicChannelDao().insertChannels(sampleChannels)

            // 3. Initial Market Items
            val sampleItems = listOf(
                MarketItemEntity(
                    id = "item-1",
                    title = "Engineering Drafting Compass & Geometry Set",
                    price = 450.0,
                    currency = "KES",
                    category = "Hardware",
                    condition = "Complete 8-Piece Set",
                    sellerName = "Student Mart Hub",
                    sellerRating = 4.9f,
                    isCampusVerifiedSeller = true,
                    campusHallLocation = "Hostel Tuck Shop",
                    imageUrl = null,
                    timestampMs = now - (15 * 60 * 1000)
                ),
                MarketItemEntity(
                    id = "item-2",
                    title = "Stewart Calculus Early Transcendentals (8th Ed)",
                    price = 1450.0,
                    currency = "KES",
                    category = "Textbooks",
                    condition = "Hardcover (Like New)",
                    sellerName = "Brian Omondi",
                    sellerRating = 4.8f,
                    isCampusVerifiedSeller = true,
                    campusHallLocation = "Library Quad",
                    imageUrl = null,
                    timestampMs = now - (30 * 60 * 1000)
                ),
                MarketItemEntity(
                    id = "item-3",
                    title = "Casio FX-991EX ClassWiz Calculator",
                    price = 2800.0,
                    currency = "KES",
                    category = "Electronics",
                    condition = "Original Solar + Battery",
                    sellerName = "Faith Chebet",
                    sellerRating = 4.9f,
                    isCampusVerifiedSeller = true,
                    campusHallLocation = "Hall 4 Hostels",
                    imageUrl = null,
                    timestampMs = now - (45 * 60 * 1000)
                ),
                MarketItemEntity(
                    id = "item-4",
                    title = "Logitech Pebble Wireless Bluetooth Mouse",
                    price = 1800.0,
                    currency = "KES",
                    category = "Electronics",
                    condition = "Mint Condition (Silent Click)",
                    sellerName = "Campus Tech Hub",
                    sellerRating = 5.0f,
                    isCampusVerifiedSeller = true,
                    campusHallLocation = "Student Center",
                    imageUrl = null,
                    timestampMs = now - (60 * 60 * 1000)
                ),
                MarketItemEntity(
                    id = "item-5",
                    title = "Anker 65W GaN USB-C Fast Charger",
                    price = 2400.0,
                    currency = "KES",
                    category = "Electronics",
                    condition = "2x USB-C + 1x USB-A",
                    sellerName = "Denis Kiprono",
                    sellerRating = 4.7f,
                    isCampusVerifiedSeller = true,
                    campusHallLocation = "Engineering Block",
                    imageUrl = null,
                    timestampMs = now - (90 * 60 * 1000)
                ),
                MarketItemEntity(
                    id = "item-6",
                    title = "Pure White Cotton Lab Coat + Safety Goggles",
                    price = 950.0,
                    currency = "KES",
                    category = "Lab & Gear",
                    condition = "Size L (Freshly Laundered)",
                    sellerName = "Wanjiku N.",
                    sellerRating = 4.9f,
                    isCampusVerifiedSeller = true,
                    campusHallLocation = "Chemistry Annex",
                    imageUrl = null,
                    timestampMs = now - (120 * 60 * 1000)
                )
            )
            database.marketItemDao().insertItems(sampleItems)

            // 4. Initial Micro Gigs
            val sampleGigs = listOf(
                MicroGigEntity(
                    id = "gig-1",
                    title = "LaTeX Formatting for B.Sc Final Year Proposal",
                    description = "Need someone familiar with IEEE bibtex template to format my 18-page thesis proposal draft cleanly.",
                    payoutAmount = 1500.0,
                    currency = "KES",
                    estimatedDuration = "2 hours",
                    posterName = "Sarah Nduta",
                    posterTrustScore = 98,
                    tag = "Academic Aid",
                    urgencyName = "FLASH_URGENT",
                    spotsAvailable = 1,
                    timestampMs = now - (30 * 60 * 1000)
                ),
                MicroGigEntity(
                    id = "gig-2",
                    title = "Urgent Delivery: Soldering Iron & Flux to MakerSpace",
                    description = "Left my Weller kit in Hostel Block B Room 214. Bring it to Lab A entrance.",
                    payoutAmount = 350.0,
                    currency = "KES",
                    estimatedDuration = "20 mins",
                    posterName = "Evans Kariuki",
                    posterTrustScore = 94,
                    tag = "Campus Errand",
                    urgencyName = "FLASH_URGENT",
                    spotsAvailable = 1,
                    timestampMs = now - (10 * 60 * 1000)
                ),
                MicroGigEntity(
                    id = "gig-3",
                    title = "Physics I Calculus Problem Set Tutor",
                    description = "Looking for a 3rd/4th year student to walk through 4 rotational inertia problems tonight at the Library.",
                    payoutAmount = 1000.0,
                    currency = "KES",
                    estimatedDuration = "1 hour",
                    posterName = "Joy Achieng",
                    posterTrustScore = 92,
                    tag = "Tutoring",
                    urgencyName = "HIGH",
                    spotsAvailable = 2,
                    timestampMs = now - (90 * 60 * 1000)
                )
            )
            database.microGigDao().insertGigs(sampleGigs)
        }
    }
}
