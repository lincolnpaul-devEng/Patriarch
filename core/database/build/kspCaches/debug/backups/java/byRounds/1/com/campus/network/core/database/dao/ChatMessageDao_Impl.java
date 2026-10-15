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
import com.campus.network.core.database.entity.ChatMessageEntity;
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
public final class ChatMessageDao_Impl implements ChatMessageDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ChatMessageEntity> __insertionAdapterOfChatMessageEntity;

  private final SharedSQLiteStatement __preparedStmtOfMarkMessageSynced;

  public ChatMessageDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfChatMessageEntity = new EntityInsertionAdapter<ChatMessageEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `chat_messages` (`id`,`channelId`,`senderId`,`senderName`,`text`,`timestampEpochMs`,`isOptimistic`,`settlementPayloadJson`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ChatMessageEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getChannelId());
        statement.bindString(3, entity.getSenderId());
        statement.bindString(4, entity.getSenderName());
        statement.bindString(5, entity.getText());
        statement.bindLong(6, entity.getTimestampEpochMs());
        final int _tmp = entity.isOptimistic() ? 1 : 0;
        statement.bindLong(7, _tmp);
        if (entity.getSettlementPayloadJson() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getSettlementPayloadJson());
        }
      }
    };
    this.__preparedStmtOfMarkMessageSynced = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE chat_messages SET isOptimistic = 0 WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertOptimisticMessage(final ChatMessageEntity message,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfChatMessageEntity.insert(message);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object markMessageSynced(final String messageId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkMessageSynced.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, messageId);
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
          __preparedStmtOfMarkMessageSynced.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ChatMessageEntity>> getMessagesForChannel(final String channelId) {
    final String _sql = "SELECT * FROM chat_messages WHERE channelId = ? ORDER BY timestampEpochMs ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, channelId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"chat_messages"}, new Callable<List<ChatMessageEntity>>() {
      @Override
      @NonNull
      public List<ChatMessageEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfChannelId = CursorUtil.getColumnIndexOrThrow(_cursor, "channelId");
          final int _cursorIndexOfSenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "senderId");
          final int _cursorIndexOfSenderName = CursorUtil.getColumnIndexOrThrow(_cursor, "senderName");
          final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
          final int _cursorIndexOfTimestampEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "timestampEpochMs");
          final int _cursorIndexOfIsOptimistic = CursorUtil.getColumnIndexOrThrow(_cursor, "isOptimistic");
          final int _cursorIndexOfSettlementPayloadJson = CursorUtil.getColumnIndexOrThrow(_cursor, "settlementPayloadJson");
          final List<ChatMessageEntity> _result = new ArrayList<ChatMessageEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChatMessageEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpChannelId;
            _tmpChannelId = _cursor.getString(_cursorIndexOfChannelId);
            final String _tmpSenderId;
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId);
            final String _tmpSenderName;
            _tmpSenderName = _cursor.getString(_cursorIndexOfSenderName);
            final String _tmpText;
            _tmpText = _cursor.getString(_cursorIndexOfText);
            final long _tmpTimestampEpochMs;
            _tmpTimestampEpochMs = _cursor.getLong(_cursorIndexOfTimestampEpochMs);
            final boolean _tmpIsOptimistic;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsOptimistic);
            _tmpIsOptimistic = _tmp != 0;
            final String _tmpSettlementPayloadJson;
            if (_cursor.isNull(_cursorIndexOfSettlementPayloadJson)) {
              _tmpSettlementPayloadJson = null;
            } else {
              _tmpSettlementPayloadJson = _cursor.getString(_cursorIndexOfSettlementPayloadJson);
            }
            _item = new ChatMessageEntity(_tmpId,_tmpChannelId,_tmpSenderId,_tmpSenderName,_tmpText,_tmpTimestampEpochMs,_tmpIsOptimistic,_tmpSettlementPayloadJson);
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
