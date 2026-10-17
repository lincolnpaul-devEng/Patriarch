package com.campus.network.core.database.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.campus.network.core.database.entity.StreamCommentEntity;
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
public final class StreamCommentDao_Impl implements StreamCommentDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<StreamCommentEntity> __insertionAdapterOfStreamCommentEntity;

  public StreamCommentDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfStreamCommentEntity = new EntityInsertionAdapter<StreamCommentEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `stream_comments` (`id`,`streamId`,`senderId`,`senderName`,`senderAvatarUrl`,`senderAvatarRes`,`commentText`,`timestampEpochMs`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final StreamCommentEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getStreamId());
        statement.bindString(3, entity.getSenderId());
        statement.bindString(4, entity.getSenderName());
        if (entity.getSenderAvatarUrl() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getSenderAvatarUrl());
        }
        statement.bindLong(6, entity.getSenderAvatarRes());
        statement.bindString(7, entity.getCommentText());
        statement.bindLong(8, entity.getTimestampEpochMs());
      }
    };
  }

  @Override
  public Object insertComment(final StreamCommentEntity comment,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfStreamCommentEntity.insert(comment);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertComments(final List<StreamCommentEntity> comments,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfStreamCommentEntity.insert(comments);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<StreamCommentEntity>> getCommentsForStream(final String streamId) {
    final String _sql = "SELECT * FROM stream_comments WHERE streamId = ? ORDER BY timestampEpochMs ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, streamId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"stream_comments"}, new Callable<List<StreamCommentEntity>>() {
      @Override
      @NonNull
      public List<StreamCommentEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStreamId = CursorUtil.getColumnIndexOrThrow(_cursor, "streamId");
          final int _cursorIndexOfSenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "senderId");
          final int _cursorIndexOfSenderName = CursorUtil.getColumnIndexOrThrow(_cursor, "senderName");
          final int _cursorIndexOfSenderAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "senderAvatarUrl");
          final int _cursorIndexOfSenderAvatarRes = CursorUtil.getColumnIndexOrThrow(_cursor, "senderAvatarRes");
          final int _cursorIndexOfCommentText = CursorUtil.getColumnIndexOrThrow(_cursor, "commentText");
          final int _cursorIndexOfTimestampEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "timestampEpochMs");
          final List<StreamCommentEntity> _result = new ArrayList<StreamCommentEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StreamCommentEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpStreamId;
            _tmpStreamId = _cursor.getString(_cursorIndexOfStreamId);
            final String _tmpSenderId;
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId);
            final String _tmpSenderName;
            _tmpSenderName = _cursor.getString(_cursorIndexOfSenderName);
            final String _tmpSenderAvatarUrl;
            if (_cursor.isNull(_cursorIndexOfSenderAvatarUrl)) {
              _tmpSenderAvatarUrl = null;
            } else {
              _tmpSenderAvatarUrl = _cursor.getString(_cursorIndexOfSenderAvatarUrl);
            }
            final int _tmpSenderAvatarRes;
            _tmpSenderAvatarRes = _cursor.getInt(_cursorIndexOfSenderAvatarRes);
            final String _tmpCommentText;
            _tmpCommentText = _cursor.getString(_cursorIndexOfCommentText);
            final long _tmpTimestampEpochMs;
            _tmpTimestampEpochMs = _cursor.getLong(_cursorIndexOfTimestampEpochMs);
            _item = new StreamCommentEntity(_tmpId,_tmpStreamId,_tmpSenderId,_tmpSenderName,_tmpSenderAvatarUrl,_tmpSenderAvatarRes,_tmpCommentText,_tmpTimestampEpochMs);
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
