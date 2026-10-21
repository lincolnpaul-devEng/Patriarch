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
    version = 14,
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

        @Volatile
        private var INSTANCE: CampusDatabase? = null

        fun getInstance(context: Context): CampusDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CampusDatabase::class.java,
                    "campus_network.db"
                )
                    .addMigrations(MIGRATION_13_14)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
