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
    version = 17,
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
                listOf(
                    "flash_meets",
                    "chat_messages",
                    "market_items",
                    "micro_gigs",
                    "live_streams",
                    "stream_comments",
                    "servers",
                    "server_channels",
                    "server_channel_messages",
                    "server_rules",
                    "server_members",
                    "friends",
                    "server_invites",
                    "campusprofile"
                ).forEach { repairOfflineSyncColumns(db, it) }
            }
        }
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE market_items ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE'")
            }
        }

        private fun repairOfflineSyncColumns(db: SupportSQLiteDatabase, tableName: String) {
            val columns = mutableListOf<Pair<String, String?>>()
            db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                val nameColumnIndex = cursor.getColumnIndexOrThrow("name")
                val defaultColumnIndex = cursor.getColumnIndexOrThrow("dflt_value")
                while (cursor.moveToNext()) {
                    columns += cursor.getString(nameColumnIndex) to
                        cursor.getString(defaultColumnIndex)
                }
            }

            check(columns.isNotEmpty()) { "Cannot migrate missing table $tableName" }
            val existingColumnNames = columns.mapTo(mutableSetOf()) { it.first }
            if ("syncStatus" !in existingColumnNames) {
                db.execSQL(
                    "ALTER TABLE `$tableName` " +
                        "ADD COLUMN `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED'"
                )
            }
            if ("syncId" !in existingColumnNames) {
                db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `syncId` TEXT")
            }

            val createSql = db.query(
                "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = '$tableName'"
            ).use { cursor ->
                check(cursor.moveToFirst()) { "Cannot migrate missing table $tableName" }
                checkNotNull(cursor.getString(0)) { "Cannot read schema for $tableName" }
            }
            val schemaWithoutMigrationDefaults = Regex(
                """([`"]?(?:syncStatus|creatorUserId|posterUserId)[`"]?\s+TEXT\s+NOT NULL)\s+DEFAULT\s+(?:'[^']*'|"[^"]*"|[^,\s)]+)""",
                RegexOption.IGNORE_CASE
            ).replace(createSql) { match -> match.groupValues[1] }

            if (schemaWithoutMigrationDefaults == createSql) return

            val temporaryTableName = "_v16_$tableName"
            val createTablePrefix = Regex(
                """(?i)^(CREATE\s+TABLE(?:\s+IF\s+NOT\s+EXISTS)?\s+)([`"\[]?[\w]+[`"\]]?)"""
            )
            val createPrefixMatch = checkNotNull(createTablePrefix.find(schemaWithoutMigrationDefaults)) {
                "Cannot rebuild table $tableName"
            }
            val temporaryCreateSql = schemaWithoutMigrationDefaults.replaceRange(
                createPrefixMatch.range,
                "${createPrefixMatch.groupValues[1]}`$temporaryTableName`"
            )
            db.execSQL("DROP TABLE IF EXISTS `$temporaryTableName`")
            db.execSQL(temporaryCreateSql)

            val currentColumns = mutableListOf<String>()
            db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                val nameColumnIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) {
                    currentColumns += cursor.getString(nameColumnIndex)
                }
            }
            val columnList = currentColumns.joinToString(", ") { "`$it`" }
            db.execSQL(
                "INSERT INTO `$temporaryTableName` ($columnList) " +
                    "SELECT $columnList FROM `$tableName`"
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
                    .addMigrations(MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
