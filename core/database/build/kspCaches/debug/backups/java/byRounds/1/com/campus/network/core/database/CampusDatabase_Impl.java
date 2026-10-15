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
import com.campus.network.core.database.dao.MarketItemDao;
import com.campus.network.core.database.dao.MarketItemDao_Impl;
import com.campus.network.core.database.dao.MicroGigDao;
import com.campus.network.core.database.dao.MicroGigDao_Impl;
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

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `flash_meets` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `categoryName` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `locationName` TEXT NOT NULL, `creatorName` TEXT NOT NULL, `creatorAvatar` TEXT, `createdAtEpochMs` INTEGER NOT NULL, `expiresAtEpochMs` INTEGER NOT NULL, `attendeeCount` INTEGER NOT NULL, `maxAttendees` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `academic_channels` (`id` TEXT NOT NULL, `courseCode` TEXT NOT NULL, `courseTitle` TEXT NOT NULL, `faculty` TEXT NOT NULL, `department` TEXT NOT NULL, `unreadCount` INTEGER NOT NULL, `lastMessageSnippet` TEXT NOT NULL, `lastMessageTime` TEXT NOT NULL, `activeStudentsCount` INTEGER NOT NULL, `isPinned` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `chat_messages` (`id` TEXT NOT NULL, `channelId` TEXT NOT NULL, `senderId` TEXT NOT NULL, `senderName` TEXT NOT NULL, `text` TEXT NOT NULL, `timestampEpochMs` INTEGER NOT NULL, `isOptimistic` INTEGER NOT NULL, `settlementPayloadJson` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `market_items` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `price` REAL NOT NULL, `currency` TEXT NOT NULL, `category` TEXT NOT NULL, `condition` TEXT NOT NULL, `sellerName` TEXT NOT NULL, `sellerRating` REAL NOT NULL, `isCampusVerifiedSeller` INTEGER NOT NULL, `campusHallLocation` TEXT NOT NULL, `imageUrl` TEXT, `timestampMs` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `micro_gigs` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `payoutAmount` REAL NOT NULL, `currency` TEXT NOT NULL, `estimatedDuration` TEXT NOT NULL, `posterName` TEXT NOT NULL, `posterTrustScore` INTEGER NOT NULL, `tag` TEXT NOT NULL, `urgencyName` TEXT NOT NULL, `spotsAvailable` INTEGER NOT NULL, `timestampMs` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '7a0f6290d50777cbc6efd685452bddca')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `flash_meets`");
        db.execSQL("DROP TABLE IF EXISTS `academic_channels`");
        db.execSQL("DROP TABLE IF EXISTS `chat_messages`");
        db.execSQL("DROP TABLE IF EXISTS `market_items`");
        db.execSQL("DROP TABLE IF EXISTS `micro_gigs`");
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
        final HashMap<String, TableInfo.Column> _columnsMarketItems = new HashMap<String, TableInfo.Column>(12);
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
        final HashSet<TableInfo.ForeignKey> _foreignKeysMarketItems = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMarketItems = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMarketItems = new TableInfo("market_items", _columnsMarketItems, _foreignKeysMarketItems, _indicesMarketItems);
        final TableInfo _existingMarketItems = TableInfo.read(db, "market_items");
        if (!_infoMarketItems.equals(_existingMarketItems)) {
          return new RoomOpenHelper.ValidationResult(false, "market_items(com.campus.network.core.database.entity.MarketItemEntity).\n"
                  + " Expected:\n" + _infoMarketItems + "\n"
                  + " Found:\n" + _existingMarketItems);
        }
        final HashMap<String, TableInfo.Column> _columnsMicroGigs = new HashMap<String, TableInfo.Column>(12);
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
        final HashSet<TableInfo.ForeignKey> _foreignKeysMicroGigs = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMicroGigs = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMicroGigs = new TableInfo("micro_gigs", _columnsMicroGigs, _foreignKeysMicroGigs, _indicesMicroGigs);
        final TableInfo _existingMicroGigs = TableInfo.read(db, "micro_gigs");
        if (!_infoMicroGigs.equals(_existingMicroGigs)) {
          return new RoomOpenHelper.ValidationResult(false, "micro_gigs(com.campus.network.core.database.entity.MicroGigEntity).\n"
                  + " Expected:\n" + _infoMicroGigs + "\n"
                  + " Found:\n" + _existingMicroGigs);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "7a0f6290d50777cbc6efd685452bddca", "9bc0b9a810e824e62319b5a7bb908add");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "flash_meets","academic_channels","chat_messages","market_items","micro_gigs");
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
}
