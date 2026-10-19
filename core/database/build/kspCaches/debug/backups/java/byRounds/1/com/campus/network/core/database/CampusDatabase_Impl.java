package com.campus.network.core.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.campus.network.core.database.dao.AcademicChannelDao;
import com.campus.network.core.database.dao.AcademicChannelDao_Impl;
import com.campus.network.core.database.dao.ChatMessageDao;
import com.campus.network.core.database.dao.ChatMessageDao_Impl;
import com.campus.network.core.database.dao.FlashMeetDao;
import com.campus.network.core.database.dao.FlashMeetDao_Impl;
import com.campus.network.core.database.dao.FriendDao;
import com.campus.network.core.database.dao.FriendDao_Impl;
import com.campus.network.core.database.dao.LiveStreamDao;
import com.campus.network.core.database.dao.LiveStreamDao_Impl;
import com.campus.network.core.database.dao.MarketItemDao;
import com.campus.network.core.database.dao.MarketItemDao_Impl;
import com.campus.network.core.database.dao.MicroGigDao;
import com.campus.network.core.database.dao.MicroGigDao_Impl;
import com.campus.network.core.database.dao.ServerChannelDao;
import com.campus.network.core.database.dao.ServerChannelDao_Impl;
import com.campus.network.core.database.dao.ServerDao;
import com.campus.network.core.database.dao.ServerDao_Impl;
import com.campus.network.core.database.dao.ServerInviteDao;
import com.campus.network.core.database.dao.ServerInviteDao_Impl;
import com.campus.network.core.database.dao.ServerMemberDao;
import com.campus.network.core.database.dao.ServerMemberDao_Impl;
import com.campus.network.core.database.dao.ServerRuleDao;
import com.campus.network.core.database.dao.ServerRuleDao_Impl;
import com.campus.network.core.database.dao.StreamCommentDao;
import com.campus.network.core.database.dao.StreamCommentDao_Impl;
import com.campus.network.core.database.dao.StreamerProfileDao;
import com.campus.network.core.database.dao.StreamerProfileDao_Impl;
import com.campus.network.core.database.dao.StudentProfileDao;
import com.campus.network.core.database.dao.StudentProfileDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class CampusDatabase_Impl extends CampusDatabase {
  private volatile FlashMeetDao _flashMeetDao;

  private volatile AcademicChannelDao _academicChannelDao;

  private volatile ChatMessageDao _chatMessageDao;

  private volatile MarketItemDao _marketItemDao;

  private volatile MicroGigDao _microGigDao;

  private volatile LiveStreamDao _liveStreamDao;

  private volatile StreamerProfileDao _streamerProfileDao;

  private volatile StreamCommentDao _streamCommentDao;

  private volatile ServerDao _serverDao;

  private volatile ServerChannelDao _serverChannelDao;

  private volatile ServerRuleDao _serverRuleDao;

  private volatile ServerMemberDao _serverMemberDao;

  private volatile FriendDao _friendDao;

  private volatile ServerInviteDao _serverInviteDao;

  private volatile StudentProfileDao _studentProfileDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(11) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `flash_meets` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `categoryName` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `locationName` TEXT NOT NULL, `creatorName` TEXT NOT NULL, `creatorAvatar` TEXT, `createdAtEpochMs` INTEGER NOT NULL, `expiresAtEpochMs` INTEGER NOT NULL, `attendeeCount` INTEGER NOT NULL, `maxAttendees` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `academic_channels` (`id` TEXT NOT NULL, `courseCode` TEXT NOT NULL, `courseTitle` TEXT NOT NULL, `faculty` TEXT NOT NULL, `department` TEXT NOT NULL, `unreadCount` INTEGER NOT NULL, `lastMessageSnippet` TEXT NOT NULL, `lastMessageTime` TEXT NOT NULL, `activeStudentsCount` INTEGER NOT NULL, `isPinned` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `chat_messages` (`id` TEXT NOT NULL, `channelId` TEXT NOT NULL, `senderId` TEXT NOT NULL, `senderName` TEXT NOT NULL, `text` TEXT NOT NULL, `timestampEpochMs` INTEGER NOT NULL, `isOptimistic` INTEGER NOT NULL, `settlementPayloadJson` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `market_items` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `price` REAL NOT NULL, `currency` TEXT NOT NULL, `category` TEXT NOT NULL, `condition` TEXT NOT NULL, `sellerName` TEXT NOT NULL, `sellerRating` REAL NOT NULL, `isCampusVerifiedSeller` INTEGER NOT NULL, `campusHallLocation` TEXT NOT NULL, `imageUrl` TEXT, `timestampMs` INTEGER NOT NULL, `description` TEXT NOT NULL, `pickupSpotName` TEXT NOT NULL, `pickupSecurityLevel` TEXT NOT NULL, `pickupLatitude` REAL NOT NULL, `pickupLongitude` REAL NOT NULL, `pickupTiming` TEXT NOT NULL, `payoutMethod` TEXT NOT NULL, `requiresQrReleaseCode` INTEGER NOT NULL, `qrReleaseCode` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `micro_gigs` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `payoutAmount` REAL NOT NULL, `currency` TEXT NOT NULL, `estimatedDuration` TEXT NOT NULL, `posterName` TEXT NOT NULL, `posterTrustScore` INTEGER NOT NULL, `tag` TEXT NOT NULL, `urgencyName` TEXT NOT NULL, `spotsAvailable` INTEGER NOT NULL, `timestampMs` INTEGER NOT NULL, `bannerUrl` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `live_streams` (`id` TEXT NOT NULL, `broadcasterId` TEXT NOT NULL, `broadcasterName` TEXT NOT NULL, `broadcasterHandle` TEXT NOT NULL, `broadcasterAvatarRes` INTEGER NOT NULL, `broadcasterAvatarUrl` TEXT, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `category` TEXT NOT NULL, `audience` TEXT NOT NULL, `isLive` INTEGER NOT NULL, `streamPlaybackUrl` TEXT, `backdropImageRes` INTEGER NOT NULL, `backdropImageUrl` TEXT, `viewerCount` INTEGER NOT NULL, `peakViewers` INTEGER NOT NULL, `likeCount` INTEGER NOT NULL, `startedAtEpochMs` INTEGER NOT NULL, `endedAtEpochMs` INTEGER, `durationSeconds` INTEGER NOT NULL, `serverId` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `streamer_profiles` (`studentId` TEXT NOT NULL, `location` TEXT NOT NULL, `joinedDate` TEXT NOT NULL, `hoursLive` INTEGER NOT NULL, `followersCount` TEXT NOT NULL, `bestDay` TEXT NOT NULL, `streamsPerWeek` INTEGER NOT NULL, `topTopicsCsv` TEXT NOT NULL, `weeklyActivityCsv` TEXT NOT NULL, PRIMARY KEY(`studentId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `stream_comments` (`id` TEXT NOT NULL, `streamId` TEXT NOT NULL, `senderId` TEXT NOT NULL, `senderName` TEXT NOT NULL, `senderAvatarUrl` TEXT, `senderAvatarRes` INTEGER NOT NULL, `commentText` TEXT NOT NULL, `timestampEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `servers` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `category` TEXT NOT NULL, `ownerId` TEXT NOT NULL, `avatarResName` TEXT NOT NULL, `bannerResName` TEXT, `isVerified` INTEGER NOT NULL, `memberCount` INTEGER NOT NULL, `inviteCode` TEXT NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `server_channels` (`id` TEXT NOT NULL, `serverId` TEXT NOT NULL, `name` TEXT NOT NULL, `categoryName` TEXT NOT NULL, `channelType` TEXT NOT NULL, `topic` TEXT NOT NULL, `unreadCount` INTEGER NOT NULL, `isPinned` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `server_rules` (`id` TEXT NOT NULL, `serverId` TEXT NOT NULL, `ruleIndex` INTEGER NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `server_members` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `serverId` TEXT NOT NULL, `displayName` TEXT NOT NULL, `handle` TEXT NOT NULL, `role` TEXT NOT NULL, `roleTitle` TEXT NOT NULL, `roleColorHex` INTEGER NOT NULL, `avatarUrl` TEXT, `avatarResName` TEXT NOT NULL, `isOnline` INTEGER NOT NULL, `customStatus` TEXT, `joinedAtEpochMs` INTEGER NOT NULL, `badge` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `friends` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `friendUserId` TEXT NOT NULL, `friendDisplayName` TEXT NOT NULL, `friendHandle` TEXT NOT NULL, `friendAvatarResName` TEXT NOT NULL, `status` TEXT NOT NULL, `lastInteractionEpochMs` INTEGER NOT NULL, `dmChannelId` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `server_invites` (`code` TEXT NOT NULL, `serverId` TEXT NOT NULL, `serverName` TEXT NOT NULL, `inviterId` TEXT NOT NULL, `inviterName` TEXT NOT NULL, `expiresAtEpochMs` INTEGER NOT NULL, `maxUses` INTEGER NOT NULL, `usesCount` INTEGER NOT NULL, `isRevoked` INTEGER NOT NULL, PRIMARY KEY(`code`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `campusprofile` (`id` TEXT NOT NULL, `studentId` TEXT NOT NULL, `fullName` TEXT NOT NULL, `avatarUrl` TEXT, `universityEmail` TEXT NOT NULL, `universityName` TEXT NOT NULL, `degreeProgram` TEXT NOT NULL, `academicYear` TEXT NOT NULL, `campusResidence` TEXT NOT NULL, `phoneNumber` TEXT NOT NULL, `bio` TEXT NOT NULL, `consistencyStreakDays` INTEGER NOT NULL, `behavioralTrustScore` INTEGER NOT NULL, `campusMeritPoints` INTEGER NOT NULL, `preferredRendezvousPoint` TEXT NOT NULL, `personaRole` TEXT NOT NULL, `statusNote` TEXT, `statusNoteEmoji` TEXT, `statusNoteExpiry` TEXT, `storageUsedBytes` INTEGER NOT NULL, `storageQuotaBytes` INTEGER NOT NULL, `notificationsEnabled` INTEGER NOT NULL, `escrowAlertsEnabled` INTEGER NOT NULL, `questRemindersEnabled` INTEGER NOT NULL, `isTrustScorePublic` INTEGER NOT NULL, `isOnlineStatusVisible` INTEGER NOT NULL, `activityLedgerVisibility` TEXT NOT NULL, `locationPrivacyScope` TEXT NOT NULL, `walletBalance` REAL NOT NULL, `vaultProtectedBalance` REAL NOT NULL, `onboardingPreferencesJson` TEXT NOT NULL, `interestsCsv` TEXT NOT NULL, `savedListingIdsCsv` TEXT NOT NULL, `watchlistIdsCsv` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '344a07209e8426894f685fd20822f30f')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `flash_meets`");
        db.execSQL("DROP TABLE IF EXISTS `academic_channels`");
        db.execSQL("DROP TABLE IF EXISTS `chat_messages`");
        db.execSQL("DROP TABLE IF EXISTS `market_items`");
        db.execSQL("DROP TABLE IF EXISTS `micro_gigs`");
        db.execSQL("DROP TABLE IF EXISTS `live_streams`");
        db.execSQL("DROP TABLE IF EXISTS `streamer_profiles`");
        db.execSQL("DROP TABLE IF EXISTS `stream_comments`");
        db.execSQL("DROP TABLE IF EXISTS `servers`");
        db.execSQL("DROP TABLE IF EXISTS `server_channels`");
        db.execSQL("DROP TABLE IF EXISTS `server_rules`");
        db.execSQL("DROP TABLE IF EXISTS `server_members`");
        db.execSQL("DROP TABLE IF EXISTS `friends`");
        db.execSQL("DROP TABLE IF EXISTS `server_invites`");
        db.execSQL("DROP TABLE IF EXISTS `campusprofile`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsFlashMeets = new HashMap<String, TableInfo.Column>(13);
        _columnsFlashMeets.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFlashMeets.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFlashMeets.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFlashMeets.put("categoryName", new TableInfo.Column("categoryName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFlashMeets.put("latitude", new TableInfo.Column("latitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFlashMeets.put("longitude", new TableInfo.Column("longitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFlashMeets.put("locationName", new TableInfo.Column("locationName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFlashMeets.put("creatorName", new TableInfo.Column("creatorName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFlashMeets.put("creatorAvatar", new TableInfo.Column("creatorAvatar", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFlashMeets.put("createdAtEpochMs", new TableInfo.Column("createdAtEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFlashMeets.put("expiresAtEpochMs", new TableInfo.Column("expiresAtEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFlashMeets.put("attendeeCount", new TableInfo.Column("attendeeCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFlashMeets.put("maxAttendees", new TableInfo.Column("maxAttendees", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFlashMeets = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFlashMeets = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoFlashMeets = new TableInfo("flash_meets", _columnsFlashMeets, _foreignKeysFlashMeets, _indicesFlashMeets);
        final TableInfo _existingFlashMeets = TableInfo.read(db, "flash_meets");
        if (!_infoFlashMeets.equals(_existingFlashMeets)) {
          return new RoomOpenHelper.ValidationResult(false, "flash_meets(com.campus.network.core.database.entity.FlashMeetEntity).\n"
                  + " Expected:\n" + _infoFlashMeets + "\n"
                  + " Found:\n" + _existingFlashMeets);
        }
        final HashMap<String, TableInfo.Column> _columnsAcademicChannels = new HashMap<String, TableInfo.Column>(10);
        _columnsAcademicChannels.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAcademicChannels.put("courseCode", new TableInfo.Column("courseCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAcademicChannels.put("courseTitle", new TableInfo.Column("courseTitle", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAcademicChannels.put("faculty", new TableInfo.Column("faculty", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAcademicChannels.put("department", new TableInfo.Column("department", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAcademicChannels.put("unreadCount", new TableInfo.Column("unreadCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAcademicChannels.put("lastMessageSnippet", new TableInfo.Column("lastMessageSnippet", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAcademicChannels.put("lastMessageTime", new TableInfo.Column("lastMessageTime", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAcademicChannels.put("activeStudentsCount", new TableInfo.Column("activeStudentsCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAcademicChannels.put("isPinned", new TableInfo.Column("isPinned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAcademicChannels = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAcademicChannels = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAcademicChannels = new TableInfo("academic_channels", _columnsAcademicChannels, _foreignKeysAcademicChannels, _indicesAcademicChannels);
        final TableInfo _existingAcademicChannels = TableInfo.read(db, "academic_channels");
        if (!_infoAcademicChannels.equals(_existingAcademicChannels)) {
          return new RoomOpenHelper.ValidationResult(false, "academic_channels(com.campus.network.core.database.entity.AcademicChannelEntity).\n"
                  + " Expected:\n" + _infoAcademicChannels + "\n"
                  + " Found:\n" + _existingAcademicChannels);
        }
        final HashMap<String, TableInfo.Column> _columnsChatMessages = new HashMap<String, TableInfo.Column>(8);
        _columnsChatMessages.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatMessages.put("channelId", new TableInfo.Column("channelId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatMessages.put("senderId", new TableInfo.Column("senderId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatMessages.put("senderName", new TableInfo.Column("senderName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatMessages.put("text", new TableInfo.Column("text", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatMessages.put("timestampEpochMs", new TableInfo.Column("timestampEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatMessages.put("isOptimistic", new TableInfo.Column("isOptimistic", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatMessages.put("settlementPayloadJson", new TableInfo.Column("settlementPayloadJson", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysChatMessages = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesChatMessages = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoChatMessages = new TableInfo("chat_messages", _columnsChatMessages, _foreignKeysChatMessages, _indicesChatMessages);
        final TableInfo _existingChatMessages = TableInfo.read(db, "chat_messages");
        if (!_infoChatMessages.equals(_existingChatMessages)) {
          return new RoomOpenHelper.ValidationResult(false, "chat_messages(com.campus.network.core.database.entity.ChatMessageEntity).\n"
                  + " Expected:\n" + _infoChatMessages + "\n"
                  + " Found:\n" + _existingChatMessages);
        }
        final HashMap<String, TableInfo.Column> _columnsMarketItems = new HashMap<String, TableInfo.Column>(21);
        _columnsMarketItems.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMarketItems.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMarketItems.put("price", new TableInfo.Column("price", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMarketItems.put("currency", new TableInfo.Column("currency", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMarketItems.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMarketItems.put("condition", new TableInfo.Column("condition", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMarketItems.put("sellerName", new TableInfo.Column("sellerName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMarketItems.put("sellerRating", new TableInfo.Column("sellerRating", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMarketItems.put("isCampusVerifiedSeller", new TableInfo.Column("isCampusVerifiedSeller", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMarketItems.put("campusHallLocation", new TableInfo.Column("campusHallLocation", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMarketItems.put("imageUrl", new TableInfo.Column("imageUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMarketItems.put("timestampMs", new TableInfo.Column("timestampMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMarketItems.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMarketItems.put("pickupSpotName", new TableInfo.Column("pickupSpotName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMarketItems.put("pickupSecurityLevel", new TableInfo.Column("pickupSecurityLevel", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMarketItems.put("pickupLatitude", new TableInfo.Column("pickupLatitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMarketItems.put("pickupLongitude", new TableInfo.Column("pickupLongitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMarketItems.put("pickupTiming", new TableInfo.Column("pickupTiming", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMarketItems.put("payoutMethod", new TableInfo.Column("payoutMethod", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMarketItems.put("requiresQrReleaseCode", new TableInfo.Column("requiresQrReleaseCode", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMarketItems.put("qrReleaseCode", new TableInfo.Column("qrReleaseCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMarketItems = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMarketItems = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMarketItems = new TableInfo("market_items", _columnsMarketItems, _foreignKeysMarketItems, _indicesMarketItems);
        final TableInfo _existingMarketItems = TableInfo.read(db, "market_items");
        if (!_infoMarketItems.equals(_existingMarketItems)) {
          return new RoomOpenHelper.ValidationResult(false, "market_items(com.campus.network.core.database.entity.MarketItemEntity).\n"
                  + " Expected:\n" + _infoMarketItems + "\n"
                  + " Found:\n" + _existingMarketItems);
        }
        final HashMap<String, TableInfo.Column> _columnsMicroGigs = new HashMap<String, TableInfo.Column>(13);
        _columnsMicroGigs.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMicroGigs.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMicroGigs.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMicroGigs.put("payoutAmount", new TableInfo.Column("payoutAmount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMicroGigs.put("currency", new TableInfo.Column("currency", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMicroGigs.put("estimatedDuration", new TableInfo.Column("estimatedDuration", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMicroGigs.put("posterName", new TableInfo.Column("posterName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMicroGigs.put("posterTrustScore", new TableInfo.Column("posterTrustScore", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMicroGigs.put("tag", new TableInfo.Column("tag", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMicroGigs.put("urgencyName", new TableInfo.Column("urgencyName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMicroGigs.put("spotsAvailable", new TableInfo.Column("spotsAvailable", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMicroGigs.put("timestampMs", new TableInfo.Column("timestampMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMicroGigs.put("bannerUrl", new TableInfo.Column("bannerUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMicroGigs = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMicroGigs = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMicroGigs = new TableInfo("micro_gigs", _columnsMicroGigs, _foreignKeysMicroGigs, _indicesMicroGigs);
        final TableInfo _existingMicroGigs = TableInfo.read(db, "micro_gigs");
        if (!_infoMicroGigs.equals(_existingMicroGigs)) {
          return new RoomOpenHelper.ValidationResult(false, "micro_gigs(com.campus.network.core.database.entity.MicroGigEntity).\n"
                  + " Expected:\n" + _infoMicroGigs + "\n"
                  + " Found:\n" + _existingMicroGigs);
        }
        final HashMap<String, TableInfo.Column> _columnsLiveStreams = new HashMap<String, TableInfo.Column>(21);
        _columnsLiveStreams.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLiveStreams.put("broadcasterId", new TableInfo.Column("broadcasterId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLiveStreams.put("broadcasterName", new TableInfo.Column("broadcasterName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLiveStreams.put("broadcasterHandle", new TableInfo.Column("broadcasterHandle", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLiveStreams.put("broadcasterAvatarRes", new TableInfo.Column("broadcasterAvatarRes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLiveStreams.put("broadcasterAvatarUrl", new TableInfo.Column("broadcasterAvatarUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLiveStreams.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLiveStreams.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLiveStreams.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLiveStreams.put("audience", new TableInfo.Column("audience", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLiveStreams.put("isLive", new TableInfo.Column("isLive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLiveStreams.put("streamPlaybackUrl", new TableInfo.Column("streamPlaybackUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLiveStreams.put("backdropImageRes", new TableInfo.Column("backdropImageRes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLiveStreams.put("backdropImageUrl", new TableInfo.Column("backdropImageUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLiveStreams.put("viewerCount", new TableInfo.Column("viewerCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLiveStreams.put("peakViewers", new TableInfo.Column("peakViewers", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLiveStreams.put("likeCount", new TableInfo.Column("likeCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLiveStreams.put("startedAtEpochMs", new TableInfo.Column("startedAtEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLiveStreams.put("endedAtEpochMs", new TableInfo.Column("endedAtEpochMs", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLiveStreams.put("durationSeconds", new TableInfo.Column("durationSeconds", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLiveStreams.put("serverId", new TableInfo.Column("serverId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLiveStreams = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLiveStreams = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLiveStreams = new TableInfo("live_streams", _columnsLiveStreams, _foreignKeysLiveStreams, _indicesLiveStreams);
        final TableInfo _existingLiveStreams = TableInfo.read(db, "live_streams");
        if (!_infoLiveStreams.equals(_existingLiveStreams)) {
          return new RoomOpenHelper.ValidationResult(false, "live_streams(com.campus.network.core.database.entity.LiveStreamEntity).\n"
                  + " Expected:\n" + _infoLiveStreams + "\n"
                  + " Found:\n" + _existingLiveStreams);
        }
        final HashMap<String, TableInfo.Column> _columnsStreamerProfiles = new HashMap<String, TableInfo.Column>(9);
        _columnsStreamerProfiles.put("studentId", new TableInfo.Column("studentId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStreamerProfiles.put("location", new TableInfo.Column("location", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStreamerProfiles.put("joinedDate", new TableInfo.Column("joinedDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStreamerProfiles.put("hoursLive", new TableInfo.Column("hoursLive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStreamerProfiles.put("followersCount", new TableInfo.Column("followersCount", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStreamerProfiles.put("bestDay", new TableInfo.Column("bestDay", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStreamerProfiles.put("streamsPerWeek", new TableInfo.Column("streamsPerWeek", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStreamerProfiles.put("topTopicsCsv", new TableInfo.Column("topTopicsCsv", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStreamerProfiles.put("weeklyActivityCsv", new TableInfo.Column("weeklyActivityCsv", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysStreamerProfiles = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesStreamerProfiles = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoStreamerProfiles = new TableInfo("streamer_profiles", _columnsStreamerProfiles, _foreignKeysStreamerProfiles, _indicesStreamerProfiles);
        final TableInfo _existingStreamerProfiles = TableInfo.read(db, "streamer_profiles");
        if (!_infoStreamerProfiles.equals(_existingStreamerProfiles)) {
          return new RoomOpenHelper.ValidationResult(false, "streamer_profiles(com.campus.network.core.database.entity.StreamerProfileEntity).\n"
                  + " Expected:\n" + _infoStreamerProfiles + "\n"
                  + " Found:\n" + _existingStreamerProfiles);
        }
        final HashMap<String, TableInfo.Column> _columnsStreamComments = new HashMap<String, TableInfo.Column>(8);
        _columnsStreamComments.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStreamComments.put("streamId", new TableInfo.Column("streamId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStreamComments.put("senderId", new TableInfo.Column("senderId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStreamComments.put("senderName", new TableInfo.Column("senderName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStreamComments.put("senderAvatarUrl", new TableInfo.Column("senderAvatarUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStreamComments.put("senderAvatarRes", new TableInfo.Column("senderAvatarRes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStreamComments.put("commentText", new TableInfo.Column("commentText", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStreamComments.put("timestampEpochMs", new TableInfo.Column("timestampEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysStreamComments = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesStreamComments = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoStreamComments = new TableInfo("stream_comments", _columnsStreamComments, _foreignKeysStreamComments, _indicesStreamComments);
        final TableInfo _existingStreamComments = TableInfo.read(db, "stream_comments");
        if (!_infoStreamComments.equals(_existingStreamComments)) {
          return new RoomOpenHelper.ValidationResult(false, "stream_comments(com.campus.network.core.database.entity.StreamCommentEntity).\n"
                  + " Expected:\n" + _infoStreamComments + "\n"
                  + " Found:\n" + _existingStreamComments);
        }
        final HashMap<String, TableInfo.Column> _columnsServers = new HashMap<String, TableInfo.Column>(11);
        _columnsServers.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServers.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServers.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServers.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServers.put("ownerId", new TableInfo.Column("ownerId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServers.put("avatarResName", new TableInfo.Column("avatarResName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServers.put("bannerResName", new TableInfo.Column("bannerResName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServers.put("isVerified", new TableInfo.Column("isVerified", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServers.put("memberCount", new TableInfo.Column("memberCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServers.put("inviteCode", new TableInfo.Column("inviteCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServers.put("createdAtEpochMs", new TableInfo.Column("createdAtEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysServers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesServers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoServers = new TableInfo("servers", _columnsServers, _foreignKeysServers, _indicesServers);
        final TableInfo _existingServers = TableInfo.read(db, "servers");
        if (!_infoServers.equals(_existingServers)) {
          return new RoomOpenHelper.ValidationResult(false, "servers(com.campus.network.core.database.entity.ServerEntity).\n"
                  + " Expected:\n" + _infoServers + "\n"
                  + " Found:\n" + _existingServers);
        }
        final HashMap<String, TableInfo.Column> _columnsServerChannels = new HashMap<String, TableInfo.Column>(9);
        _columnsServerChannels.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerChannels.put("serverId", new TableInfo.Column("serverId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerChannels.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerChannels.put("categoryName", new TableInfo.Column("categoryName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerChannels.put("channelType", new TableInfo.Column("channelType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerChannels.put("topic", new TableInfo.Column("topic", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerChannels.put("unreadCount", new TableInfo.Column("unreadCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerChannels.put("isPinned", new TableInfo.Column("isPinned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerChannels.put("orderIndex", new TableInfo.Column("orderIndex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysServerChannels = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesServerChannels = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoServerChannels = new TableInfo("server_channels", _columnsServerChannels, _foreignKeysServerChannels, _indicesServerChannels);
        final TableInfo _existingServerChannels = TableInfo.read(db, "server_channels");
        if (!_infoServerChannels.equals(_existingServerChannels)) {
          return new RoomOpenHelper.ValidationResult(false, "server_channels(com.campus.network.core.database.entity.ServerChannelEntity).\n"
                  + " Expected:\n" + _infoServerChannels + "\n"
                  + " Found:\n" + _existingServerChannels);
        }
        final HashMap<String, TableInfo.Column> _columnsServerRules = new HashMap<String, TableInfo.Column>(5);
        _columnsServerRules.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerRules.put("serverId", new TableInfo.Column("serverId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerRules.put("ruleIndex", new TableInfo.Column("ruleIndex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerRules.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerRules.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysServerRules = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesServerRules = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoServerRules = new TableInfo("server_rules", _columnsServerRules, _foreignKeysServerRules, _indicesServerRules);
        final TableInfo _existingServerRules = TableInfo.read(db, "server_rules");
        if (!_infoServerRules.equals(_existingServerRules)) {
          return new RoomOpenHelper.ValidationResult(false, "server_rules(com.campus.network.core.database.entity.ServerRuleEntity).\n"
                  + " Expected:\n" + _infoServerRules + "\n"
                  + " Found:\n" + _existingServerRules);
        }
        final HashMap<String, TableInfo.Column> _columnsServerMembers = new HashMap<String, TableInfo.Column>(14);
        _columnsServerMembers.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerMembers.put("userId", new TableInfo.Column("userId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerMembers.put("serverId", new TableInfo.Column("serverId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerMembers.put("displayName", new TableInfo.Column("displayName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerMembers.put("handle", new TableInfo.Column("handle", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerMembers.put("role", new TableInfo.Column("role", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerMembers.put("roleTitle", new TableInfo.Column("roleTitle", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerMembers.put("roleColorHex", new TableInfo.Column("roleColorHex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerMembers.put("avatarUrl", new TableInfo.Column("avatarUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerMembers.put("avatarResName", new TableInfo.Column("avatarResName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerMembers.put("isOnline", new TableInfo.Column("isOnline", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerMembers.put("customStatus", new TableInfo.Column("customStatus", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerMembers.put("joinedAtEpochMs", new TableInfo.Column("joinedAtEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerMembers.put("badge", new TableInfo.Column("badge", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysServerMembers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesServerMembers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoServerMembers = new TableInfo("server_members", _columnsServerMembers, _foreignKeysServerMembers, _indicesServerMembers);
        final TableInfo _existingServerMembers = TableInfo.read(db, "server_members");
        if (!_infoServerMembers.equals(_existingServerMembers)) {
          return new RoomOpenHelper.ValidationResult(false, "server_members(com.campus.network.core.database.entity.ServerMemberEntity).\n"
                  + " Expected:\n" + _infoServerMembers + "\n"
                  + " Found:\n" + _existingServerMembers);
        }
        final HashMap<String, TableInfo.Column> _columnsFriends = new HashMap<String, TableInfo.Column>(9);
        _columnsFriends.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFriends.put("userId", new TableInfo.Column("userId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFriends.put("friendUserId", new TableInfo.Column("friendUserId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFriends.put("friendDisplayName", new TableInfo.Column("friendDisplayName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFriends.put("friendHandle", new TableInfo.Column("friendHandle", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFriends.put("friendAvatarResName", new TableInfo.Column("friendAvatarResName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFriends.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFriends.put("lastInteractionEpochMs", new TableInfo.Column("lastInteractionEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFriends.put("dmChannelId", new TableInfo.Column("dmChannelId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFriends = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFriends = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoFriends = new TableInfo("friends", _columnsFriends, _foreignKeysFriends, _indicesFriends);
        final TableInfo _existingFriends = TableInfo.read(db, "friends");
        if (!_infoFriends.equals(_existingFriends)) {
          return new RoomOpenHelper.ValidationResult(false, "friends(com.campus.network.core.database.entity.FriendEntity).\n"
                  + " Expected:\n" + _infoFriends + "\n"
                  + " Found:\n" + _existingFriends);
        }
        final HashMap<String, TableInfo.Column> _columnsServerInvites = new HashMap<String, TableInfo.Column>(9);
        _columnsServerInvites.put("code", new TableInfo.Column("code", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerInvites.put("serverId", new TableInfo.Column("serverId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerInvites.put("serverName", new TableInfo.Column("serverName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerInvites.put("inviterId", new TableInfo.Column("inviterId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerInvites.put("inviterName", new TableInfo.Column("inviterName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerInvites.put("expiresAtEpochMs", new TableInfo.Column("expiresAtEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerInvites.put("maxUses", new TableInfo.Column("maxUses", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerInvites.put("usesCount", new TableInfo.Column("usesCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServerInvites.put("isRevoked", new TableInfo.Column("isRevoked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysServerInvites = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesServerInvites = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoServerInvites = new TableInfo("server_invites", _columnsServerInvites, _foreignKeysServerInvites, _indicesServerInvites);
        final TableInfo _existingServerInvites = TableInfo.read(db, "server_invites");
        if (!_infoServerInvites.equals(_existingServerInvites)) {
          return new RoomOpenHelper.ValidationResult(false, "server_invites(com.campus.network.core.database.entity.ServerInviteEntity).\n"
                  + " Expected:\n" + _infoServerInvites + "\n"
                  + " Found:\n" + _existingServerInvites);
        }
        final HashMap<String, TableInfo.Column> _columnsCampusprofile = new HashMap<String, TableInfo.Column>(34);
        _columnsCampusprofile.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("studentId", new TableInfo.Column("studentId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("fullName", new TableInfo.Column("fullName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("avatarUrl", new TableInfo.Column("avatarUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("universityEmail", new TableInfo.Column("universityEmail", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("universityName", new TableInfo.Column("universityName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("degreeProgram", new TableInfo.Column("degreeProgram", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("academicYear", new TableInfo.Column("academicYear", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("campusResidence", new TableInfo.Column("campusResidence", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("phoneNumber", new TableInfo.Column("phoneNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("bio", new TableInfo.Column("bio", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("consistencyStreakDays", new TableInfo.Column("consistencyStreakDays", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("behavioralTrustScore", new TableInfo.Column("behavioralTrustScore", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("campusMeritPoints", new TableInfo.Column("campusMeritPoints", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("preferredRendezvousPoint", new TableInfo.Column("preferredRendezvousPoint", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("personaRole", new TableInfo.Column("personaRole", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("statusNote", new TableInfo.Column("statusNote", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("statusNoteEmoji", new TableInfo.Column("statusNoteEmoji", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("statusNoteExpiry", new TableInfo.Column("statusNoteExpiry", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("storageUsedBytes", new TableInfo.Column("storageUsedBytes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("storageQuotaBytes", new TableInfo.Column("storageQuotaBytes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("notificationsEnabled", new TableInfo.Column("notificationsEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("escrowAlertsEnabled", new TableInfo.Column("escrowAlertsEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("questRemindersEnabled", new TableInfo.Column("questRemindersEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("isTrustScorePublic", new TableInfo.Column("isTrustScorePublic", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("isOnlineStatusVisible", new TableInfo.Column("isOnlineStatusVisible", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("activityLedgerVisibility", new TableInfo.Column("activityLedgerVisibility", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("locationPrivacyScope", new TableInfo.Column("locationPrivacyScope", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("walletBalance", new TableInfo.Column("walletBalance", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("vaultProtectedBalance", new TableInfo.Column("vaultProtectedBalance", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("onboardingPreferencesJson", new TableInfo.Column("onboardingPreferencesJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("interestsCsv", new TableInfo.Column("interestsCsv", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("savedListingIdsCsv", new TableInfo.Column("savedListingIdsCsv", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCampusprofile.put("watchlistIdsCsv", new TableInfo.Column("watchlistIdsCsv", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCampusprofile = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCampusprofile = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCampusprofile = new TableInfo("campusprofile", _columnsCampusprofile, _foreignKeysCampusprofile, _indicesCampusprofile);
        final TableInfo _existingCampusprofile = TableInfo.read(db, "campusprofile");
        if (!_infoCampusprofile.equals(_existingCampusprofile)) {
          return new RoomOpenHelper.ValidationResult(false, "campusprofile(com.campus.network.core.database.entity.StudentProfileEntity).\n"
                  + " Expected:\n" + _infoCampusprofile + "\n"
                  + " Found:\n" + _existingCampusprofile);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "344a07209e8426894f685fd20822f30f", "2c8e5be6d8b71cf30fc7aa35284ba6c3");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "flash_meets","academic_channels","chat_messages","market_items","micro_gigs","live_streams","streamer_profiles","stream_comments","servers","server_channels","server_rules","server_members","friends","server_invites","campusprofile");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `flash_meets`");
      _db.execSQL("DELETE FROM `academic_channels`");
      _db.execSQL("DELETE FROM `chat_messages`");
      _db.execSQL("DELETE FROM `market_items`");
      _db.execSQL("DELETE FROM `micro_gigs`");
      _db.execSQL("DELETE FROM `live_streams`");
      _db.execSQL("DELETE FROM `streamer_profiles`");
      _db.execSQL("DELETE FROM `stream_comments`");
      _db.execSQL("DELETE FROM `servers`");
      _db.execSQL("DELETE FROM `server_channels`");
      _db.execSQL("DELETE FROM `server_rules`");
      _db.execSQL("DELETE FROM `server_members`");
      _db.execSQL("DELETE FROM `friends`");
      _db.execSQL("DELETE FROM `server_invites`");
      _db.execSQL("DELETE FROM `campusprofile`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(FlashMeetDao.class, FlashMeetDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AcademicChannelDao.class, AcademicChannelDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ChatMessageDao.class, ChatMessageDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MarketItemDao.class, MarketItemDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MicroGigDao.class, MicroGigDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(LiveStreamDao.class, LiveStreamDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(StreamerProfileDao.class, StreamerProfileDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(StreamCommentDao.class, StreamCommentDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ServerDao.class, ServerDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ServerChannelDao.class, ServerChannelDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ServerRuleDao.class, ServerRuleDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ServerMemberDao.class, ServerMemberDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(FriendDao.class, FriendDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ServerInviteDao.class, ServerInviteDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(StudentProfileDao.class, StudentProfileDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public FlashMeetDao flashMeetDao() {
    if (_flashMeetDao != null) {
      return _flashMeetDao;
    } else {
      synchronized(this) {
        if(_flashMeetDao == null) {
          _flashMeetDao = new FlashMeetDao_Impl(this);
        }
        return _flashMeetDao;
      }
    }
  }

  @Override
  public AcademicChannelDao academicChannelDao() {
    if (_academicChannelDao != null) {
      return _academicChannelDao;
    } else {
      synchronized(this) {
        if(_academicChannelDao == null) {
          _academicChannelDao = new AcademicChannelDao_Impl(this);
        }
        return _academicChannelDao;
      }
    }
  }

  @Override
  public ChatMessageDao chatMessageDao() {
    if (_chatMessageDao != null) {
      return _chatMessageDao;
    } else {
      synchronized(this) {
        if(_chatMessageDao == null) {
          _chatMessageDao = new ChatMessageDao_Impl(this);
        }
        return _chatMessageDao;
      }
    }
  }

  @Override
  public MarketItemDao marketItemDao() {
    if (_marketItemDao != null) {
      return _marketItemDao;
    } else {
      synchronized(this) {
        if(_marketItemDao == null) {
          _marketItemDao = new MarketItemDao_Impl(this);
        }
        return _marketItemDao;
      }
    }
  }

  @Override
  public MicroGigDao microGigDao() {
    if (_microGigDao != null) {
      return _microGigDao;
    } else {
      synchronized(this) {
        if(_microGigDao == null) {
          _microGigDao = new MicroGigDao_Impl(this);
        }
        return _microGigDao;
      }
    }
  }

  @Override
  public LiveStreamDao liveStreamDao() {
    if (_liveStreamDao != null) {
      return _liveStreamDao;
    } else {
      synchronized(this) {
        if(_liveStreamDao == null) {
          _liveStreamDao = new LiveStreamDao_Impl(this);
        }
        return _liveStreamDao;
      }
    }
  }

  @Override
  public StreamerProfileDao streamerProfileDao() {
    if (_streamerProfileDao != null) {
      return _streamerProfileDao;
    } else {
      synchronized(this) {
        if(_streamerProfileDao == null) {
          _streamerProfileDao = new StreamerProfileDao_Impl(this);
        }
        return _streamerProfileDao;
      }
    }
  }

  @Override
  public StreamCommentDao streamCommentDao() {
    if (_streamCommentDao != null) {
      return _streamCommentDao;
    } else {
      synchronized(this) {
        if(_streamCommentDao == null) {
          _streamCommentDao = new StreamCommentDao_Impl(this);
        }
        return _streamCommentDao;
      }
    }
  }

  @Override
  public ServerDao serverDao() {
    if (_serverDao != null) {
      return _serverDao;
    } else {
      synchronized(this) {
        if(_serverDao == null) {
          _serverDao = new ServerDao_Impl(this);
        }
        return _serverDao;
      }
    }
  }

  @Override
  public ServerChannelDao serverChannelDao() {
    if (_serverChannelDao != null) {
      return _serverChannelDao;
    } else {
      synchronized(this) {
        if(_serverChannelDao == null) {
          _serverChannelDao = new ServerChannelDao_Impl(this);
        }
        return _serverChannelDao;
      }
    }
  }

  @Override
  public ServerRuleDao serverRuleDao() {
    if (_serverRuleDao != null) {
      return _serverRuleDao;
    } else {
      synchronized(this) {
        if(_serverRuleDao == null) {
          _serverRuleDao = new ServerRuleDao_Impl(this);
        }
        return _serverRuleDao;
      }
    }
  }

  @Override
  public ServerMemberDao serverMemberDao() {
    if (_serverMemberDao != null) {
      return _serverMemberDao;
    } else {
      synchronized(this) {
        if(_serverMemberDao == null) {
          _serverMemberDao = new ServerMemberDao_Impl(this);
        }
        return _serverMemberDao;
      }
    }
  }

  @Override
  public FriendDao friendDao() {
    if (_friendDao != null) {
      return _friendDao;
    } else {
      synchronized(this) {
        if(_friendDao == null) {
          _friendDao = new FriendDao_Impl(this);
        }
        return _friendDao;
      }
    }
  }

  @Override
  public ServerInviteDao serverInviteDao() {
    if (_serverInviteDao != null) {
      return _serverInviteDao;
    } else {
      synchronized(this) {
        if(_serverInviteDao == null) {
          _serverInviteDao = new ServerInviteDao_Impl(this);
        }
        return _serverInviteDao;
      }
    }
  }

  @Override
  public StudentProfileDao studentProfileDao() {
    if (_studentProfileDao != null) {
      return _studentProfileDao;
    } else {
      synchronized(this) {
        if(_studentProfileDao == null) {
          _studentProfileDao = new StudentProfileDao_Impl(this);
        }
        return _studentProfileDao;
      }
    }
  }
}
