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
import com.campus.network.core.database.entity.ServerRuleEntity;
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
public final class ServerRuleDao_Impl implements ServerRuleDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ServerRuleEntity> __insertionAdapterOfServerRuleEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteRule;

  public ServerRuleDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfServerRuleEntity = new EntityInsertionAdapter<ServerRuleEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `server_rules` (`id`,`serverId`,`ruleIndex`,`title`,`description`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ServerRuleEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getServerId());
        statement.bindLong(3, entity.getRuleIndex());
        statement.bindString(4, entity.getTitle());
        statement.bindString(5, entity.getDescription());
      }
    };
    this.__preparedStmtOfDeleteRule = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM server_rules WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertRule(final ServerRuleEntity rule,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfServerRuleEntity.insert(rule);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertRules(final List<ServerRuleEntity> rules,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfServerRuleEntity.insert(rules);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteRule(final String ruleId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteRule.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, ruleId);
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
          __preparedStmtOfDeleteRule.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ServerRuleEntity>> getRulesForServer(final String serverId) {
    final String _sql = "SELECT * FROM server_rules WHERE serverId = ? ORDER BY ruleIndex ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, serverId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"server_rules"}, new Callable<List<ServerRuleEntity>>() {
      @Override
      @NonNull
      public List<ServerRuleEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfRuleIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "ruleIndex");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final List<ServerRuleEntity> _result = new ArrayList<ServerRuleEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ServerRuleEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpServerId;
            _tmpServerId = _cursor.getString(_cursorIndexOfServerId);
            final int _tmpRuleIndex;
            _tmpRuleIndex = _cursor.getInt(_cursorIndexOfRuleIndex);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            _item = new ServerRuleEntity(_tmpId,_tmpServerId,_tmpRuleIndex,_tmpTitle,_tmpDescription);
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
