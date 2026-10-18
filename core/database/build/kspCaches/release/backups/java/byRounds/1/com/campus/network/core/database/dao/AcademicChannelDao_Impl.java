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
import com.campus.network.core.database.entity.AcademicChannelEntity;
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
public final class AcademicChannelDao_Impl implements AcademicChannelDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AcademicChannelEntity> __insertionAdapterOfAcademicChannelEntity;

  private final SharedSQLiteStatement __preparedStmtOfMarkChannelRead;

  public AcademicChannelDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAcademicChannelEntity = new EntityInsertionAdapter<AcademicChannelEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `academic_channels` (`id`,`courseCode`,`courseTitle`,`faculty`,`department`,`unreadCount`,`lastMessageSnippet`,`lastMessageTime`,`activeStudentsCount`,`isPinned`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AcademicChannelEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getCourseCode());
        statement.bindString(3, entity.getCourseTitle());
        statement.bindString(4, entity.getFaculty());
        statement.bindString(5, entity.getDepartment());
        statement.bindLong(6, entity.getUnreadCount());
        statement.bindString(7, entity.getLastMessageSnippet());
        statement.bindString(8, entity.getLastMessageTime());
        statement.bindLong(9, entity.getActiveStudentsCount());
        final int _tmp = entity.isPinned() ? 1 : 0;
        statement.bindLong(10, _tmp);
      }
    };
    this.__preparedStmtOfMarkChannelRead = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE academic_channels SET unreadCount = 0 WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertChannels(final List<AcademicChannelEntity> channels,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAcademicChannelEntity.insert(channels);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object markChannelRead(final String channelId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkChannelRead.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, channelId);
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
          __preparedStmtOfMarkChannelRead.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<AcademicChannelEntity>> getAllChannels() {
    final String _sql = "SELECT * FROM academic_channels ORDER BY faculty, department, courseCode ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"academic_channels"}, new Callable<List<AcademicChannelEntity>>() {
      @Override
      @NonNull
      public List<AcademicChannelEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCourseCode = CursorUtil.getColumnIndexOrThrow(_cursor, "courseCode");
          final int _cursorIndexOfCourseTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "courseTitle");
          final int _cursorIndexOfFaculty = CursorUtil.getColumnIndexOrThrow(_cursor, "faculty");
          final int _cursorIndexOfDepartment = CursorUtil.getColumnIndexOrThrow(_cursor, "department");
          final int _cursorIndexOfUnreadCount = CursorUtil.getColumnIndexOrThrow(_cursor, "unreadCount");
          final int _cursorIndexOfLastMessageSnippet = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessageSnippet");
          final int _cursorIndexOfLastMessageTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMessageTime");
          final int _cursorIndexOfActiveStudentsCount = CursorUtil.getColumnIndexOrThrow(_cursor, "activeStudentsCount");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final List<AcademicChannelEntity> _result = new ArrayList<AcademicChannelEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AcademicChannelEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpCourseCode;
            _tmpCourseCode = _cursor.getString(_cursorIndexOfCourseCode);
            final String _tmpCourseTitle;
            _tmpCourseTitle = _cursor.getString(_cursorIndexOfCourseTitle);
            final String _tmpFaculty;
            _tmpFaculty = _cursor.getString(_cursorIndexOfFaculty);
            final String _tmpDepartment;
            _tmpDepartment = _cursor.getString(_cursorIndexOfDepartment);
            final int _tmpUnreadCount;
            _tmpUnreadCount = _cursor.getInt(_cursorIndexOfUnreadCount);
            final String _tmpLastMessageSnippet;
            _tmpLastMessageSnippet = _cursor.getString(_cursorIndexOfLastMessageSnippet);
            final String _tmpLastMessageTime;
            _tmpLastMessageTime = _cursor.getString(_cursorIndexOfLastMessageTime);
            final int _tmpActiveStudentsCount;
            _tmpActiveStudentsCount = _cursor.getInt(_cursorIndexOfActiveStudentsCount);
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            _item = new AcademicChannelEntity(_tmpId,_tmpCourseCode,_tmpCourseTitle,_tmpFaculty,_tmpDepartment,_tmpUnreadCount,_tmpLastMessageSnippet,_tmpLastMessageTime,_tmpActiveStudentsCount,_tmpIsPinned);
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
