package com.campus.network.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.campus.network.core.database.dao.AcademicChannelDao
import com.campus.network.core.database.dao.ChatMessageDao
import com.campus.network.core.database.dao.FlashMeetDao
import com.campus.network.core.database.dao.MarketItemDao
import com.campus.network.core.database.dao.MicroGigDao
import com.campus.network.core.database.dao.LiveStreamDao
import com.campus.network.core.database.dao.StreamerProfileDao
import com.campus.network.core.database.dao.StreamCommentDao
import com.campus.network.core.database.dao.ServerDao
import com.campus.network.core.database.dao.ServerChannelDao
import com.campus.network.core.database.dao.ServerChannelMessageDao
import com.campus.network.core.database.dao.ServerRuleDao
import com.campus.network.core.database.dao.ServerMemberDao
import com.campus.network.core.database.dao.FriendDao
import com.campus.network.core.database.dao.ServerInviteDao
import com.campus.network.core.database.entity.AcademicChannelEntity
import com.campus.network.core.database.entity.ChatMessageEntity
import com.campus.network.core.database.entity.FlashMeetEntity
import com.campus.network.core.database.entity.MarketItemEntity
import com.campus.network.core.database.entity.MicroGigEntity
import com.campus.network.core.database.entity.LiveStreamEntity
import com.campus.network.core.database.entity.StreamerProfileEntity
import com.campus.network.core.database.entity.StreamCommentEntity
import com.campus.network.core.database.entity.ServerEntity
import com.campus.network.core.database.dao.StudentProfileDao
import com.campus.network.core.database.entity.StudentProfileEntity
import com.campus.network.core.database.entity.ServerChannelEntity
import com.campus.network.core.database.entity.ServerChannelMessageEntity
import com.campus.network.core.database.entity.ServerRuleEntity
import com.campus.network.core.database.entity.ServerMemberEntity
import com.campus.network.core.database.entity.FriendEntity
import com.campus.network.core.database.entity.ServerInviteEntity

@Database(
    entities = [
        FlashMeetEntity::class,
        AcademicChannelEntity::class,
        ChatMessageEntity::class,
        MarketItemEntity::class,
        MicroGigEntity::class,
        LiveStreamEntity::class,
        StreamerProfileEntity::class,
        StreamCommentEntity::class,
        ServerEntity::class,
        ServerChannelEntity::class,
        ServerChannelMessageEntity::class,
        ServerRuleEntity::class,
        ServerMemberEntity::class,
        FriendEntity::class,
        ServerInviteEntity::class,
        StudentProfileEntity::class
    ],
    version = 16,
    exportSchema = true
)
abstract class CampusDatabase : RoomDatabase() {
    abstract fun flashMeetDao(): FlashMeetDao
    abstract fun academicChannelDao(): AcademicChannelDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun marketItemDao(): MarketItemDao
    abstract fun microGigDao(): MicroGigDao
    abstract fun liveStreamDao(): LiveStreamDao
    abstract fun streamerProfileDao(): StreamerProfileDao
    abstract fun streamCommentDao(): StreamCommentDao
    abstract fun serverDao(): ServerDao
    abstract fun serverChannelDao(): ServerChannelDao
    abstract fun serverChannelMessageDao(): ServerChannelMessageDao
    abstract fun serverRuleDao(): ServerRuleDao
    abstract fun serverMemberDao(): ServerMemberDao
    abstract fun friendDao(): FriendDao
    abstract fun serverInviteDao(): ServerInviteDao
    abstract fun studentProfileDao(): StudentProfileDao

    companion object {
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS `server_channel_messages` (
                    `id` TEXT NOT NULL, `serverId` TEXT NOT NULL, `channelId` TEXT NOT NULL,
                    `senderId` TEXT NOT NULL, `senderName` TEXT NOT NULL, `senderAvatarUrl` TEXT,
                    `text` TEXT NOT NULL, `gifUrl` TEXT, `imageUrlsJson` TEXT NOT NULL,
                    `reactionCount` INTEGER NOT NULL, `timestampEpochMs` INTEGER NOT NULL,
                    `isOptimistic` INTEGER NOT NULL, `isDelivered` INTEGER NOT NULL,
                    `isMine` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
            }
        }
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE flash_meets ADD COLUMN creatorUserId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE micro_gigs ADD COLUMN posterUserId TEXT NOT NULL DEFAULT ''")
            }
        }
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                rebuildFlashMeets(db)
                rebuildMicroGigs(db)
            }
        }

        private fun rebuildFlashMeets(db: SupportSQLiteDatabase) {
            rebuildTable(
                db = db,
                tableName = "flash_meets",
                temporaryTableName = "_flash_meets_v16",
                createTableSql = """CREATE TABLE `_flash_meets_v16` (
                    `id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL,
                    `categoryName` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL,
                    `locationName` TEXT NOT NULL, `creatorName` TEXT NOT NULL,
                    `creatorUserId` TEXT NOT NULL, `creatorAvatar` TEXT,
                    `createdAtEpochMs` INTEGER NOT NULL, `expiresAtEpochMs` INTEGER NOT NULL,
                    `attendeeCount` INTEGER NOT NULL, `maxAttendees` INTEGER NOT NULL,
                    `syncStatus` TEXT NOT NULL, `syncId` TEXT, PRIMARY KEY(`id`))""",
                columns = listOf(
                    "id", "title", "description", "categoryName", "latitude", "longitude",
                    "locationName", "creatorName", "creatorUserId", "creatorAvatar",
                    "createdAtEpochMs", "expiresAtEpochMs", "attendeeCount", "maxAttendees",
                    "syncStatus", "syncId"
                ),
                fallbackExpressions = mapOf(
                    "creatorUserId" to "''",
                    "creatorAvatar" to "NULL",
                    "syncStatus" to "'SYNCED'",
                    "syncId" to "NULL"
                )
            )
        }

        private fun rebuildMicroGigs(db: SupportSQLiteDatabase) {
            rebuildTable(
                db = db,
                tableName = "micro_gigs",
                temporaryTableName = "_micro_gigs_v16",
                createTableSql = """CREATE TABLE `_micro_gigs_v16` (
                    `id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL,
                    `payoutAmount` REAL NOT NULL, `currency` TEXT NOT NULL,
                    `estimatedDuration` TEXT NOT NULL, `posterName` TEXT NOT NULL,
                    `posterUserId` TEXT NOT NULL, `posterTrustScore` INTEGER NOT NULL,
                    `tag` TEXT NOT NULL, `urgencyName` TEXT NOT NULL,
                    `spotsAvailable` INTEGER NOT NULL, `timestampMs` INTEGER NOT NULL,
                    `bannerUrl` TEXT, `syncStatus` TEXT NOT NULL, `syncId` TEXT,
                    PRIMARY KEY(`id`))""",
                columns = listOf(
                    "id", "title", "description", "payoutAmount", "currency",
                    "estimatedDuration", "posterName", "posterUserId", "posterTrustScore",
                    "tag", "urgencyName", "spotsAvailable", "timestampMs", "bannerUrl",
                    "syncStatus", "syncId"
                ),
                fallbackExpressions = mapOf(
                    "posterUserId" to "''",
                    "bannerUrl" to "NULL",
                    "syncStatus" to "'SYNCED'",
                    "syncId" to "NULL"
                )
            )
        }

        private fun rebuildTable(
            db: SupportSQLiteDatabase,
            tableName: String,
            temporaryTableName: String,
            createTableSql: String,
            columns: List<String>,
            fallbackExpressions: Map<String, String>
        ) {
            val existingColumns = mutableSetOf<String>()
            db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                val nameColumnIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) {
                    existingColumns += cursor.getString(nameColumnIndex)
                }
            }

            db.execSQL("DROP TABLE IF EXISTS `$temporaryTableName`")
            db.execSQL(createTableSql)
            val sourceExpressions = columns.map { column ->
                if (column in existingColumns) {
                    "`$column`"
                } else {
                    checkNotNull(fallbackExpressions[column]) {
                        "Cannot migrate $tableName: required column $column is missing"
                    }
                }
            }
            val columnList = columns.joinToString(", ") { "`$it`" }
            db.execSQL(
                "INSERT INTO `$temporaryTableName` ($columnList) " +
                    "SELECT ${sourceExpressions.joinToString(", ")} FROM `$tableName`"
            )
            db.execSQL("DROP TABLE `$tableName`")
            db.execSQL("ALTER TABLE `$temporaryTableName` RENAME TO `$tableName`")
        }

        @Volatile
        private var INSTANCE: CampusDatabase? = null

        fun getInstance(context: Context): CampusDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CampusDatabase::class.java,
                    "campus_network.db"
                )
                    .addMigrations(MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
