package com.campus.network.core.database.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.campus.network.core.database.entity.FriendEntity;
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
public final class FriendDao_Impl implements FriendDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<FriendEntity> __insertionAdapterOfFriendEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateFriendStatus;

  private final SharedSQLiteStatement __preparedStmtOfUpdateDmChannelId;

  private final SharedSQLiteStatement __preparedStmtOfDeleteFriendRelationship;

  public FriendDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfFriendEntity = new EntityInsertionAdapter<FriendEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `friends` (`id`,`userId`,`friendUserId`,`friendDisplayName`,`friendHandle`,`friendAvatarResName`,`status`,`lastInteractionEpochMs`,`dmChannelId`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FriendEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getUserId());
        statement.bindString(3, entity.getFriendUserId());
        statement.bindString(4, entity.getFriendDisplayName());
        statement.bindString(5, entity.getFriendHandle());
        statement.bindString(6, entity.getFriendAvatarResName());
        statement.bindString(7, entity.getStatus());
        statement.bindLong(8, entity.getLastInteractionEpochMs());
        if (entity.getDmChannelId() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getDmChannelId());
        }
      }
    };
    this.__preparedStmtOfUpdateFriendStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE friends SET status = ?, lastInteractionEpochMs = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateDmChannelId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE friends SET dmChannelId = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteFriendRelationship = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM friends WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertFriend(final FriendEntity friend,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfFriendEntity.insert(friend);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertFriends(final List<FriendEntity> friends,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfFriendEntity.insert(friends);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateFriendStatus(final String id, final String status,
      final long lastInteractionMs, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateFriendStatus.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, lastInteractionMs);
        _argIndex = 3;
        _stmt.bindString(_argIndex, id);
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
          __preparedStmtOfUpdateFriendStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateDmChannelId(final String id, final String dmChannelId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateDmChannelId.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, dmChannelId);
        _argIndex = 2;
        _stmt.bindString(_argIndex, id);
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
          __preparedStmtOfUpdateDmChannelId.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteFriendRelationship(final String id,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteFriendRelationship.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
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
          __preparedStmtOfDeleteFriendRelationship.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<FriendEntity>> getFriendsForUser(final String userId) {
    final String _sql = "SELECT * FROM friends WHERE userId = ? ORDER BY lastInteractionEpochMs DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"friends"}, new Callable<List<FriendEntity>>() {
      @Override
      @NonNull
      public List<FriendEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfFriendUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "friendUserId");
          final int _cursorIndexOfFriendDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "friendDisplayName");
          final int _cursorIndexOfFriendHandle = CursorUtil.getColumnIndexOrThrow(_cursor, "friendHandle");
          final int _cursorIndexOfFriendAvatarResName = CursorUtil.getColumnIndexOrThrow(_cursor, "friendAvatarResName");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfLastInteractionEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "lastInteractionEpochMs");
          final int _cursorIndexOfDmChannelId = CursorUtil.getColumnIndexOrThrow(_cursor, "dmChannelId");
          final List<FriendEntity> _result = new ArrayList<FriendEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FriendEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpFriendUserId;
            _tmpFriendUserId = _cursor.getString(_cursorIndexOfFriendUserId);
            final String _tmpFriendDisplayName;
            _tmpFriendDisplayName = _cursor.getString(_cursorIndexOfFriendDisplayName);
            final String _tmpFriendHandle;
            _tmpFriendHandle = _cursor.getString(_cursorIndexOfFriendHandle);
            final String _tmpFriendAvatarResName;
            _tmpFriendAvatarResName = _cursor.getString(_cursorIndexOfFriendAvatarResName);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpLastInteractionEpochMs;
            _tmpLastInteractionEpochMs = _cursor.getLong(_cursorIndexOfLastInteractionEpochMs);
            final String _tmpDmChannelId;
            if (_cursor.isNull(_cursorIndexOfDmChannelId)) {
              _tmpDmChannelId = null;
            } else {
              _tmpDmChannelId = _cursor.getString(_cursorIndexOfDmChannelId);
            }
            _item = new FriendEntity(_tmpId,_tmpUserId,_tmpFriendUserId,_tmpFriendDisplayName,_tmpFriendHandle,_tmpFriendAvatarResName,_tmpStatus,_tmpLastInteractionEpochMs,_tmpDmChannelId);
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

  @Override
  public Flow<FriendEntity> getFriendRelationship(final String userId, final String friendUserId) {
    final String _sql = "SELECT * FROM friends WHERE userId = ? AND friendUserId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    _argIndex = 2;
    _statement.bindString(_argIndex, friendUserId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"friends"}, new Callable<FriendEntity>() {
      @Override
      @Nullable
      public FriendEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfFriendUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "friendUserId");
          final int _cursorIndexOfFriendDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "friendDisplayName");
          final int _cursorIndexOfFriendHandle = CursorUtil.getColumnIndexOrThrow(_cursor, "friendHandle");
          final int _cursorIndexOfFriendAvatarResName = CursorUtil.getColumnIndexOrThrow(_cursor, "friendAvatarResName");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfLastInteractionEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "lastInteractionEpochMs");
          final int _cursorIndexOfDmChannelId = CursorUtil.getColumnIndexOrThrow(_cursor, "dmChannelId");
          final FriendEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpFriendUserId;
            _tmpFriendUserId = _cursor.getString(_cursorIndexOfFriendUserId);
            final String _tmpFriendDisplayName;
            _tmpFriendDisplayName = _cursor.getString(_cursorIndexOfFriendDisplayName);
            final String _tmpFriendHandle;
            _tmpFriendHandle = _cursor.getString(_cursorIndexOfFriendHandle);
            final String _tmpFriendAvatarResName;
            _tmpFriendAvatarResName = _cursor.getString(_cursorIndexOfFriendAvatarResName);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpLastInteractionEpochMs;
            _tmpLastInteractionEpochMs = _cursor.getLong(_cursorIndexOfLastInteractionEpochMs);
            final String _tmpDmChannelId;
            if (_cursor.isNull(_cursorIndexOfDmChannelId)) {
              _tmpDmChannelId = null;
            } else {
              _tmpDmChannelId = _cursor.getString(_cursorIndexOfDmChannelId);
            }
            _result = new FriendEntity(_tmpId,_tmpUserId,_tmpFriendUserId,_tmpFriendDisplayName,_tmpFriendHandle,_tmpFriendAvatarResName,_tmpStatus,_tmpLastInteractionEpochMs,_tmpDmChannelId);
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

  @Override
  public Object getFriendRelationshipSync(final String userId, final String friendUserId,
      final Continuation<? super FriendEntity> $completion) {
    final String _sql = "SELECT * FROM friends WHERE userId = ? AND friendUserId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    _argIndex = 2;
    _statement.bindString(_argIndex, friendUserId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<FriendEntity>() {
      @Override
      @Nullable
      public FriendEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfFriendUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "friendUserId");
          final int _cursorIndexOfFriendDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "friendDisplayName");
          final int _cursorIndexOfFriendHandle = CursorUtil.getColumnIndexOrThrow(_cursor, "friendHandle");
          final int _cursorIndexOfFriendAvatarResName = CursorUtil.getColumnIndexOrThrow(_cursor, "friendAvatarResName");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfLastInteractionEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "lastInteractionEpochMs");
          final int _cursorIndexOfDmChannelId = CursorUtil.getColumnIndexOrThrow(_cursor, "dmChannelId");
          final FriendEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpFriendUserId;
            _tmpFriendUserId = _cursor.getString(_cursorIndexOfFriendUserId);
            final String _tmpFriendDisplayName;
            _tmpFriendDisplayName = _cursor.getString(_cursorIndexOfFriendDisplayName);
            final String _tmpFriendHandle;
            _tmpFriendHandle = _cursor.getString(_cursorIndexOfFriendHandle);
            final String _tmpFriendAvatarResName;
            _tmpFriendAvatarResName = _cursor.getString(_cursorIndexOfFriendAvatarResName);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpLastInteractionEpochMs;
            _tmpLastInteractionEpochMs = _cursor.getLong(_cursorIndexOfLastInteractionEpochMs);
            final String _tmpDmChannelId;
            if (_cursor.isNull(_cursorIndexOfDmChannelId)) {
              _tmpDmChannelId = null;
            } else {
              _tmpDmChannelId = _cursor.getString(_cursorIndexOfDmChannelId);
            }
            _result = new FriendEntity(_tmpId,_tmpUserId,_tmpFriendUserId,_tmpFriendDisplayName,_tmpFriendHandle,_tmpFriendAvatarResName,_tmpStatus,_tmpLastInteractionEpochMs,_tmpDmChannelId);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
