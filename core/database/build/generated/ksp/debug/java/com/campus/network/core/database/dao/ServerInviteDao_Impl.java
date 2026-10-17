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
import com.campus.network.core.database.entity.ServerInviteEntity;
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
public final class ServerInviteDao_Impl implements ServerInviteDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ServerInviteEntity> __insertionAdapterOfServerInviteEntity;

  private final SharedSQLiteStatement __preparedStmtOfIncrementInviteUsage;

  private final SharedSQLiteStatement __preparedStmtOfRevokeInvite;

  public ServerInviteDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfServerInviteEntity = new EntityInsertionAdapter<ServerInviteEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `server_invites` (`code`,`serverId`,`serverName`,`inviterId`,`inviterName`,`expiresAtEpochMs`,`maxUses`,`usesCount`,`isRevoked`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ServerInviteEntity entity) {
        statement.bindString(1, entity.getCode());
        statement.bindString(2, entity.getServerId());
        statement.bindString(3, entity.getServerName());
        statement.bindString(4, entity.getInviterId());
        statement.bindString(5, entity.getInviterName());
        statement.bindLong(6, entity.getExpiresAtEpochMs());
        statement.bindLong(7, entity.getMaxUses());
        statement.bindLong(8, entity.getUsesCount());
        final int _tmp = entity.isRevoked() ? 1 : 0;
        statement.bindLong(9, _tmp);
      }
    };
    this.__preparedStmtOfIncrementInviteUsage = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE server_invites SET usesCount = usesCount + 1 WHERE code = ?";
        return _query;
      }
    };
    this.__preparedStmtOfRevokeInvite = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE server_invites SET isRevoked = 1 WHERE code = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertInvite(final ServerInviteEntity invite,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfServerInviteEntity.insert(invite);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object incrementInviteUsage(final String code,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementInviteUsage.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, code);
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
          __preparedStmtOfIncrementInviteUsage.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object revokeInvite(final String code, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfRevokeInvite.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, code);
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
          __preparedStmtOfRevokeInvite.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<ServerInviteEntity> getInviteByCode(final String code) {
    final String _sql = "SELECT * FROM server_invites WHERE code = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, code);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"server_invites"}, new Callable<ServerInviteEntity>() {
      @Override
      @Nullable
      public ServerInviteEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfCode = CursorUtil.getColumnIndexOrThrow(_cursor, "code");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfServerName = CursorUtil.getColumnIndexOrThrow(_cursor, "serverName");
          final int _cursorIndexOfInviterId = CursorUtil.getColumnIndexOrThrow(_cursor, "inviterId");
          final int _cursorIndexOfInviterName = CursorUtil.getColumnIndexOrThrow(_cursor, "inviterName");
          final int _cursorIndexOfExpiresAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "expiresAtEpochMs");
          final int _cursorIndexOfMaxUses = CursorUtil.getColumnIndexOrThrow(_cursor, "maxUses");
          final int _cursorIndexOfUsesCount = CursorUtil.getColumnIndexOrThrow(_cursor, "usesCount");
          final int _cursorIndexOfIsRevoked = CursorUtil.getColumnIndexOrThrow(_cursor, "isRevoked");
          final ServerInviteEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpCode;
            _tmpCode = _cursor.getString(_cursorIndexOfCode);
            final String _tmpServerId;
            _tmpServerId = _cursor.getString(_cursorIndexOfServerId);
            final String _tmpServerName;
            _tmpServerName = _cursor.getString(_cursorIndexOfServerName);
            final String _tmpInviterId;
            _tmpInviterId = _cursor.getString(_cursorIndexOfInviterId);
            final String _tmpInviterName;
            _tmpInviterName = _cursor.getString(_cursorIndexOfInviterName);
            final long _tmpExpiresAtEpochMs;
            _tmpExpiresAtEpochMs = _cursor.getLong(_cursorIndexOfExpiresAtEpochMs);
            final int _tmpMaxUses;
            _tmpMaxUses = _cursor.getInt(_cursorIndexOfMaxUses);
            final int _tmpUsesCount;
            _tmpUsesCount = _cursor.getInt(_cursorIndexOfUsesCount);
            final boolean _tmpIsRevoked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRevoked);
            _tmpIsRevoked = _tmp != 0;
            _result = new ServerInviteEntity(_tmpCode,_tmpServerId,_tmpServerName,_tmpInviterId,_tmpInviterName,_tmpExpiresAtEpochMs,_tmpMaxUses,_tmpUsesCount,_tmpIsRevoked);
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
  public Object getInviteByCodeSync(final String code,
      final Continuation<? super ServerInviteEntity> $completion) {
    final String _sql = "SELECT * FROM server_invites WHERE code = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, code);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ServerInviteEntity>() {
      @Override
      @Nullable
      public ServerInviteEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfCode = CursorUtil.getColumnIndexOrThrow(_cursor, "code");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfServerName = CursorUtil.getColumnIndexOrThrow(_cursor, "serverName");
          final int _cursorIndexOfInviterId = CursorUtil.getColumnIndexOrThrow(_cursor, "inviterId");
          final int _cursorIndexOfInviterName = CursorUtil.getColumnIndexOrThrow(_cursor, "inviterName");
          final int _cursorIndexOfExpiresAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "expiresAtEpochMs");
          final int _cursorIndexOfMaxUses = CursorUtil.getColumnIndexOrThrow(_cursor, "maxUses");
          final int _cursorIndexOfUsesCount = CursorUtil.getColumnIndexOrThrow(_cursor, "usesCount");
          final int _cursorIndexOfIsRevoked = CursorUtil.getColumnIndexOrThrow(_cursor, "isRevoked");
          final ServerInviteEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpCode;
            _tmpCode = _cursor.getString(_cursorIndexOfCode);
            final String _tmpServerId;
            _tmpServerId = _cursor.getString(_cursorIndexOfServerId);
            final String _tmpServerName;
            _tmpServerName = _cursor.getString(_cursorIndexOfServerName);
            final String _tmpInviterId;
            _tmpInviterId = _cursor.getString(_cursorIndexOfInviterId);
            final String _tmpInviterName;
            _tmpInviterName = _cursor.getString(_cursorIndexOfInviterName);
            final long _tmpExpiresAtEpochMs;
            _tmpExpiresAtEpochMs = _cursor.getLong(_cursorIndexOfExpiresAtEpochMs);
            final int _tmpMaxUses;
            _tmpMaxUses = _cursor.getInt(_cursorIndexOfMaxUses);
            final int _tmpUsesCount;
            _tmpUsesCount = _cursor.getInt(_cursorIndexOfUsesCount);
            final boolean _tmpIsRevoked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRevoked);
            _tmpIsRevoked = _tmp != 0;
            _result = new ServerInviteEntity(_tmpCode,_tmpServerId,_tmpServerName,_tmpInviterId,_tmpInviterName,_tmpExpiresAtEpochMs,_tmpMaxUses,_tmpUsesCount,_tmpIsRevoked);
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

  @Override
  public Flow<List<ServerInviteEntity>> getInvitesForServer(final String serverId) {
    final String _sql = "SELECT * FROM server_invites WHERE serverId = ? ORDER BY expiresAtEpochMs ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, serverId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"server_invites"}, new Callable<List<ServerInviteEntity>>() {
      @Override
      @NonNull
      public List<ServerInviteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfCode = CursorUtil.getColumnIndexOrThrow(_cursor, "code");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfServerName = CursorUtil.getColumnIndexOrThrow(_cursor, "serverName");
          final int _cursorIndexOfInviterId = CursorUtil.getColumnIndexOrThrow(_cursor, "inviterId");
          final int _cursorIndexOfInviterName = CursorUtil.getColumnIndexOrThrow(_cursor, "inviterName");
          final int _cursorIndexOfExpiresAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "expiresAtEpochMs");
          final int _cursorIndexOfMaxUses = CursorUtil.getColumnIndexOrThrow(_cursor, "maxUses");
          final int _cursorIndexOfUsesCount = CursorUtil.getColumnIndexOrThrow(_cursor, "usesCount");
          final int _cursorIndexOfIsRevoked = CursorUtil.getColumnIndexOrThrow(_cursor, "isRevoked");
          final List<ServerInviteEntity> _result = new ArrayList<ServerInviteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ServerInviteEntity _item;
            final String _tmpCode;
            _tmpCode = _cursor.getString(_cursorIndexOfCode);
            final String _tmpServerId;
            _tmpServerId = _cursor.getString(_cursorIndexOfServerId);
            final String _tmpServerName;
            _tmpServerName = _cursor.getString(_cursorIndexOfServerName);
            final String _tmpInviterId;
            _tmpInviterId = _cursor.getString(_cursorIndexOfInviterId);
            final String _tmpInviterName;
            _tmpInviterName = _cursor.getString(_cursorIndexOfInviterName);
            final long _tmpExpiresAtEpochMs;
            _tmpExpiresAtEpochMs = _cursor.getLong(_cursorIndexOfExpiresAtEpochMs);
            final int _tmpMaxUses;
            _tmpMaxUses = _cursor.getInt(_cursorIndexOfMaxUses);
            final int _tmpUsesCount;
            _tmpUsesCount = _cursor.getInt(_cursorIndexOfUsesCount);
            final boolean _tmpIsRevoked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRevoked);
            _tmpIsRevoked = _tmp != 0;
            _item = new ServerInviteEntity(_tmpCode,_tmpServerId,_tmpServerName,_tmpInviterId,_tmpInviterName,_tmpExpiresAtEpochMs,_tmpMaxUses,_tmpUsesCount,_tmpIsRevoked);
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
