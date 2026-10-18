package com.campus.network.core.database.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.campus.network.core.database.entity.StreamerProfileEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class StreamerProfileDao_Impl implements StreamerProfileDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<StreamerProfileEntity> __insertionAdapterOfStreamerProfileEntity;

  public StreamerProfileDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfStreamerProfileEntity = new EntityInsertionAdapter<StreamerProfileEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `streamer_profiles` (`studentId`,`location`,`joinedDate`,`hoursLive`,`followersCount`,`bestDay`,`streamsPerWeek`,`topTopicsCsv`,`weeklyActivityCsv`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final StreamerProfileEntity entity) {
        statement.bindString(1, entity.getStudentId());
        statement.bindString(2, entity.getLocation());
        statement.bindString(3, entity.getJoinedDate());
        statement.bindLong(4, entity.getHoursLive());
        statement.bindString(5, entity.getFollowersCount());
        statement.bindString(6, entity.getBestDay());
        statement.bindLong(7, entity.getStreamsPerWeek());
        statement.bindString(8, entity.getTopTopicsCsv());
        statement.bindString(9, entity.getWeeklyActivityCsv());
      }
    };
  }

  @Override
  public Object insertProfile(final StreamerProfileEntity profile,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfStreamerProfileEntity.insert(profile);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<StreamerProfileEntity> getProfile(final String studentId) {
    final String _sql = "SELECT * FROM streamer_profiles WHERE studentId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, studentId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"streamer_profiles"}, new Callable<StreamerProfileEntity>() {
      @Override
      @Nullable
      public StreamerProfileEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfStudentId = CursorUtil.getColumnIndexOrThrow(_cursor, "studentId");
          final int _cursorIndexOfLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "location");
          final int _cursorIndexOfJoinedDate = CursorUtil.getColumnIndexOrThrow(_cursor, "joinedDate");
          final int _cursorIndexOfHoursLive = CursorUtil.getColumnIndexOrThrow(_cursor, "hoursLive");
          final int _cursorIndexOfFollowersCount = CursorUtil.getColumnIndexOrThrow(_cursor, "followersCount");
          final int _cursorIndexOfBestDay = CursorUtil.getColumnIndexOrThrow(_cursor, "bestDay");
          final int _cursorIndexOfStreamsPerWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "streamsPerWeek");
          final int _cursorIndexOfTopTopicsCsv = CursorUtil.getColumnIndexOrThrow(_cursor, "topTopicsCsv");
          final int _cursorIndexOfWeeklyActivityCsv = CursorUtil.getColumnIndexOrThrow(_cursor, "weeklyActivityCsv");
          final StreamerProfileEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpStudentId;
            _tmpStudentId = _cursor.getString(_cursorIndexOfStudentId);
            final String _tmpLocation;
            _tmpLocation = _cursor.getString(_cursorIndexOfLocation);
            final String _tmpJoinedDate;
            _tmpJoinedDate = _cursor.getString(_cursorIndexOfJoinedDate);
            final int _tmpHoursLive;
            _tmpHoursLive = _cursor.getInt(_cursorIndexOfHoursLive);
            final String _tmpFollowersCount;
            _tmpFollowersCount = _cursor.getString(_cursorIndexOfFollowersCount);
            final String _tmpBestDay;
            _tmpBestDay = _cursor.getString(_cursorIndexOfBestDay);
            final int _tmpStreamsPerWeek;
            _tmpStreamsPerWeek = _cursor.getInt(_cursorIndexOfStreamsPerWeek);
            final String _tmpTopTopicsCsv;
            _tmpTopTopicsCsv = _cursor.getString(_cursorIndexOfTopTopicsCsv);
            final String _tmpWeeklyActivityCsv;
            _tmpWeeklyActivityCsv = _cursor.getString(_cursorIndexOfWeeklyActivityCsv);
            _result = new StreamerProfileEntity(_tmpStudentId,_tmpLocation,_tmpJoinedDate,_tmpHoursLive,_tmpFollowersCount,_tmpBestDay,_tmpStreamsPerWeek,_tmpTopTopicsCsv,_tmpWeeklyActivityCsv);
          } else {
            _result = null;
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
