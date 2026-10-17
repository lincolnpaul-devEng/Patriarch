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
import com.campus.network.core.database.entity.ServerMemberEntity;
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
public final class ServerMemberDao_Impl implements ServerMemberDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ServerMemberEntity> __insertionAdapterOfServerMemberEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateMemberRole;

  private final SharedSQLiteStatement __preparedStmtOfRemoveMember;

  public ServerMemberDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfServerMemberEntity = new EntityInsertionAdapter<ServerMemberEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `server_members` (`id`,`userId`,`serverId`,`displayName`,`handle`,`role`,`roleTitle`,`roleColorHex`,`avatarUrl`,`avatarResName`,`isOnline`,`customStatus`,`joinedAtEpochMs`,`badge`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ServerMemberEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getUserId());
        statement.bindString(3, entity.getServerId());
        statement.bindString(4, entity.getDisplayName());
        statement.bindString(5, entity.getHandle());
        statement.bindString(6, entity.getRole());
        statement.bindString(7, entity.getRoleTitle());
        statement.bindLong(8, entity.getRoleColorHex());
        if (entity.getAvatarUrl() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getAvatarUrl());
        }
        statement.bindString(10, entity.getAvatarResName());
        final int _tmp = entity.isOnline() ? 1 : 0;
        statement.bindLong(11, _tmp);
        if (entity.getCustomStatus() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getCustomStatus());
        }
        statement.bindLong(13, entity.getJoinedAtEpochMs());
        if (entity.getBadge() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getBadge());
        }
      }
    };
    this.__preparedStmtOfUpdateMemberRole = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE server_members SET role = ?, roleTitle = ?, roleColorHex = ? WHERE serverId = ? AND userId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfRemoveMember = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM server_members WHERE serverId = ? AND userId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertMember(final ServerMemberEntity member,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfServerMemberEntity.insert(member);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertMembers(final List<ServerMemberEntity> members,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfServerMemberEntity.insert(members);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateMemberRole(final String serverId, final String userId, final String role,
      final String roleTitle, final long roleColorHex,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateMemberRole.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, role);
        _argIndex = 2;
        _stmt.bindString(_argIndex, roleTitle);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, roleColorHex);
        _argIndex = 4;
        _stmt.bindString(_argIndex, serverId);
        _argIndex = 5;
        _stmt.bindString(_argIndex, userId);
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
          __preparedStmtOfUpdateMemberRole.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object removeMember(final String serverId, final String userId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfRemoveMember.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, serverId);
        _argIndex = 2;
        _stmt.bindString(_argIndex, userId);
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
          __preparedStmtOfRemoveMember.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ServerMemberEntity>> getMembersForServer(final String serverId) {
    final String _sql = "SELECT * FROM server_members WHERE serverId = ? ORDER BY isOnline DESC, role ASC, displayName ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, serverId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"server_members"}, new Callable<List<ServerMemberEntity>>() {
      @Override
      @NonNull
      public List<ServerMemberEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfHandle = CursorUtil.getColumnIndexOrThrow(_cursor, "handle");
          final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
          final int _cursorIndexOfRoleTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "roleTitle");
          final int _cursorIndexOfRoleColorHex = CursorUtil.getColumnIndexOrThrow(_cursor, "roleColorHex");
          final int _cursorIndexOfAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarUrl");
          final int _cursorIndexOfAvatarResName = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarResName");
          final int _cursorIndexOfIsOnline = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnline");
          final int _cursorIndexOfCustomStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "customStatus");
          final int _cursorIndexOfJoinedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "joinedAtEpochMs");
          final int _cursorIndexOfBadge = CursorUtil.getColumnIndexOrThrow(_cursor, "badge");
          final List<ServerMemberEntity> _result = new ArrayList<ServerMemberEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ServerMemberEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpServerId;
            _tmpServerId = _cursor.getString(_cursorIndexOfServerId);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final String _tmpHandle;
            _tmpHandle = _cursor.getString(_cursorIndexOfHandle);
            final String _tmpRole;
            _tmpRole = _cursor.getString(_cursorIndexOfRole);
            final String _tmpRoleTitle;
            _tmpRoleTitle = _cursor.getString(_cursorIndexOfRoleTitle);
            final long _tmpRoleColorHex;
            _tmpRoleColorHex = _cursor.getLong(_cursorIndexOfRoleColorHex);
            final String _tmpAvatarUrl;
            if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
              _tmpAvatarUrl = null;
            } else {
              _tmpAvatarUrl = _cursor.getString(_cursorIndexOfAvatarUrl);
            }
            final String _tmpAvatarResName;
            _tmpAvatarResName = _cursor.getString(_cursorIndexOfAvatarResName);
            final boolean _tmpIsOnline;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsOnline);
            _tmpIsOnline = _tmp != 0;
            final String _tmpCustomStatus;
            if (_cursor.isNull(_cursorIndexOfCustomStatus)) {
              _tmpCustomStatus = null;
            } else {
              _tmpCustomStatus = _cursor.getString(_cursorIndexOfCustomStatus);
            }
            final long _tmpJoinedAtEpochMs;
            _tmpJoinedAtEpochMs = _cursor.getLong(_cursorIndexOfJoinedAtEpochMs);
            final String _tmpBadge;
            if (_cursor.isNull(_cursorIndexOfBadge)) {
              _tmpBadge = null;
            } else {
              _tmpBadge = _cursor.getString(_cursorIndexOfBadge);
            }
            _item = new ServerMemberEntity(_tmpId,_tmpUserId,_tmpServerId,_tmpDisplayName,_tmpHandle,_tmpRole,_tmpRoleTitle,_tmpRoleColorHex,_tmpAvatarUrl,_tmpAvatarResName,_tmpIsOnline,_tmpCustomStatus,_tmpJoinedAtEpochMs,_tmpBadge);
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
  public Flow<ServerMemberEntity> getMember(final String serverId, final String userId) {
    final String _sql = "SELECT * FROM server_members WHERE serverId = ? AND userId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, serverId);
    _argIndex = 2;
    _statement.bindString(_argIndex, userId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"server_members"}, new Callable<ServerMemberEntity>() {
      @Override
      @Nullable
      public ServerMemberEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfHandle = CursorUtil.getColumnIndexOrThrow(_cursor, "handle");
          final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
          final int _cursorIndexOfRoleTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "roleTitle");
          final int _cursorIndexOfRoleColorHex = CursorUtil.getColumnIndexOrThrow(_cursor, "roleColorHex");
          final int _cursorIndexOfAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarUrl");
          final int _cursorIndexOfAvatarResName = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarResName");
          final int _cursorIndexOfIsOnline = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnline");
          final int _cursorIndexOfCustomStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "customStatus");
          final int _cursorIndexOfJoinedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "joinedAtEpochMs");
          final int _cursorIndexOfBadge = CursorUtil.getColumnIndexOrThrow(_cursor, "badge");
          final ServerMemberEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpServerId;
            _tmpServerId = _cursor.getString(_cursorIndexOfServerId);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final String _tmpHandle;
            _tmpHandle = _cursor.getString(_cursorIndexOfHandle);
            final String _tmpRole;
            _tmpRole = _cursor.getString(_cursorIndexOfRole);
            final String _tmpRoleTitle;
            _tmpRoleTitle = _cursor.getString(_cursorIndexOfRoleTitle);
            final long _tmpRoleColorHex;
            _tmpRoleColorHex = _cursor.getLong(_cursorIndexOfRoleColorHex);
            final String _tmpAvatarUrl;
            if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
              _tmpAvatarUrl = null;
            } else {
              _tmpAvatarUrl = _cursor.getString(_cursorIndexOfAvatarUrl);
            }
            final String _tmpAvatarResName;
            _tmpAvatarResName = _cursor.getString(_cursorIndexOfAvatarResName);
            final boolean _tmpIsOnline;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsOnline);
            _tmpIsOnline = _tmp != 0;
            final String _tmpCustomStatus;
            if (_cursor.isNull(_cursorIndexOfCustomStatus)) {
              _tmpCustomStatus = null;
            } else {
              _tmpCustomStatus = _cursor.getString(_cursorIndexOfCustomStatus);
            }
            final long _tmpJoinedAtEpochMs;
            _tmpJoinedAtEpochMs = _cursor.getLong(_cursorIndexOfJoinedAtEpochMs);
            final String _tmpBadge;
            if (_cursor.isNull(_cursorIndexOfBadge)) {
              _tmpBadge = null;
            } else {
              _tmpBadge = _cursor.getString(_cursorIndexOfBadge);
            }
            _result = new ServerMemberEntity(_tmpId,_tmpUserId,_tmpServerId,_tmpDisplayName,_tmpHandle,_tmpRole,_tmpRoleTitle,_tmpRoleColorHex,_tmpAvatarUrl,_tmpAvatarResName,_tmpIsOnline,_tmpCustomStatus,_tmpJoinedAtEpochMs,_tmpBadge);
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
  public Object getMemberSync(final String serverId, final String userId,
      final Continuation<? super ServerMemberEntity> $completion) {
    final String _sql = "SELECT * FROM server_members WHERE serverId = ? AND userId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, serverId);
    _argIndex = 2;
    _statement.bindString(_argIndex, userId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ServerMemberEntity>() {
      @Override
      @Nullable
      public ServerMemberEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfHandle = CursorUtil.getColumnIndexOrThrow(_cursor, "handle");
          final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
          final int _cursorIndexOfRoleTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "roleTitle");
          final int _cursorIndexOfRoleColorHex = CursorUtil.getColumnIndexOrThrow(_cursor, "roleColorHex");
          final int _cursorIndexOfAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarUrl");
          final int _cursorIndexOfAvatarResName = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarResName");
          final int _cursorIndexOfIsOnline = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnline");
          final int _cursorIndexOfCustomStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "customStatus");
          final int _cursorIndexOfJoinedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "joinedAtEpochMs");
          final int _cursorIndexOfBadge = CursorUtil.getColumnIndexOrThrow(_cursor, "badge");
          final ServerMemberEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpServerId;
            _tmpServerId = _cursor.getString(_cursorIndexOfServerId);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final String _tmpHandle;
            _tmpHandle = _cursor.getString(_cursorIndexOfHandle);
            final String _tmpRole;
            _tmpRole = _cursor.getString(_cursorIndexOfRole);
            final String _tmpRoleTitle;
            _tmpRoleTitle = _cursor.getString(_cursorIndexOfRoleTitle);
            final long _tmpRoleColorHex;
            _tmpRoleColorHex = _cursor.getLong(_cursorIndexOfRoleColorHex);
            final String _tmpAvatarUrl;
            if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
              _tmpAvatarUrl = null;
            } else {
              _tmpAvatarUrl = _cursor.getString(_cursorIndexOfAvatarUrl);
            }
            final String _tmpAvatarResName;
            _tmpAvatarResName = _cursor.getString(_cursorIndexOfAvatarResName);
            final boolean _tmpIsOnline;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsOnline);
            _tmpIsOnline = _tmp != 0;
            final String _tmpCustomStatus;
            if (_cursor.isNull(_cursorIndexOfCustomStatus)) {
              _tmpCustomStatus = null;
            } else {
              _tmpCustomStatus = _cursor.getString(_cursorIndexOfCustomStatus);
            }
            final long _tmpJoinedAtEpochMs;
            _tmpJoinedAtEpochMs = _cursor.getLong(_cursorIndexOfJoinedAtEpochMs);
            final String _tmpBadge;
            if (_cursor.isNull(_cursorIndexOfBadge)) {
              _tmpBadge = null;
            } else {
              _tmpBadge = _cursor.getString(_cursorIndexOfBadge);
            }
            _result = new ServerMemberEntity(_tmpId,_tmpUserId,_tmpServerId,_tmpDisplayName,_tmpHandle,_tmpRole,_tmpRoleTitle,_tmpRoleColorHex,_tmpAvatarUrl,_tmpAvatarResName,_tmpIsOnline,_tmpCustomStatus,_tmpJoinedAtEpochMs,_tmpBadge);
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
