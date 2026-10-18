package com.campus.network.core.database.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.campus.network.core.database.entity.FlashMeetEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class FlashMeetDao_Impl implements FlashMeetDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<FlashMeetEntity> __insertionAdapterOfFlashMeetEntity;

  private final SharedSQLiteStatement __preparedStmtOfPurgeExpired;

  public FlashMeetDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfFlashMeetEntity = new EntityInsertionAdapter<FlashMeetEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `flash_meets` (`id`,`title`,`description`,`categoryName`,`latitude`,`longitude`,`locationName`,`creatorName`,`creatorAvatar`,`createdAtEpochMs`,`expiresAtEpochMs`,`attendeeCount`,`maxAttendees`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FlashMeetEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getDescription());
        statement.bindString(4, entity.getCategoryName());
        statement.bindDouble(5, entity.getLatitude());
        statement.bindDouble(6, entity.getLongitude());
        statement.bindString(7, entity.getLocationName());
        statement.bindString(8, entity.getCreatorName());
        if (entity.getCreatorAvatar() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getCreatorAvatar());
        }
        statement.bindLong(10, entity.getCreatedAtEpochMs());
        statement.bindLong(11, entity.getExpiresAtEpochMs());
        statement.bindLong(12, entity.getAttendeeCount());
        statement.bindLong(13, entity.getMaxAttendees());
      }
    };
    this.__preparedStmtOfPurgeExpired = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM flash_meets WHERE expiresAtEpochMs <= ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertMeets(final List<FlashMeetEntity> meets,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfFlashMeetEntity.insert(meets);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object purgeExpired(final long currentTime, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfPurgeExpired.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, currentTime);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfPurgeExpired.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<FlashMeetEntity>> getLiveMeets(final long currentTime) {
    final String _sql = "SELECT * FROM flash_meets WHERE expiresAtEpochMs > ? ORDER BY createdAtEpochMs DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, currentTime);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"flash_meets"}, new Callable<List<FlashMeetEntity>>() {
      @Override
      @NonNull
      public List<FlashMeetEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCategoryName = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryName");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfLocationName = CursorUtil.getColumnIndexOrThrow(_cursor, "locationName");
          final int _cursorIndexOfCreatorName = CursorUtil.getColumnIndexOrThrow(_cursor, "creatorName");
          final int _cursorIndexOfCreatorAvatar = CursorUtil.getColumnIndexOrThrow(_cursor, "creatorAvatar");
          final int _cursorIndexOfCreatedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAtEpochMs");
          final int _cursorIndexOfExpiresAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "expiresAtEpochMs");
          final int _cursorIndexOfAttendeeCount = CursorUtil.getColumnIndexOrThrow(_cursor, "attendeeCount");
          final int _cursorIndexOfMaxAttendees = CursorUtil.getColumnIndexOrThrow(_cursor, "maxAttendees");
          final List<FlashMeetEntity> _result = new ArrayList<FlashMeetEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FlashMeetEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpCategoryName;
            _tmpCategoryName = _cursor.getString(_cursorIndexOfCategoryName);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final String _tmpLocationName;
            _tmpLocationName = _cursor.getString(_cursorIndexOfLocationName);
            final String _tmpCreatorName;
            _tmpCreatorName = _cursor.getString(_cursorIndexOfCreatorName);
            final String _tmpCreatorAvatar;
            if (_cursor.isNull(_cursorIndexOfCreatorAvatar)) {
              _tmpCreatorAvatar = null;
            } else {
              _tmpCreatorAvatar = _cursor.getString(_cursorIndexOfCreatorAvatar);
            }
            final long _tmpCreatedAtEpochMs;
            _tmpCreatedAtEpochMs = _cursor.getLong(_cursorIndexOfCreatedAtEpochMs);
            final long _tmpExpiresAtEpochMs;
            _tmpExpiresAtEpochMs = _cursor.getLong(_cursorIndexOfExpiresAtEpochMs);
            final int _tmpAttendeeCount;
            _tmpAttendeeCount = _cursor.getInt(_cursorIndexOfAttendeeCount);
            final int _tmpMaxAttendees;
            _tmpMaxAttendees = _cursor.getInt(_cursorIndexOfMaxAttendees);
            _item = new FlashMeetEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpCategoryName,_tmpLatitude,_tmpLongitude,_tmpLocationName,_tmpCreatorName,_tmpCreatorAvatar,_tmpCreatedAtEpochMs,_tmpExpiresAtEpochMs,_tmpAttendeeCount,_tmpMaxAttendees);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
