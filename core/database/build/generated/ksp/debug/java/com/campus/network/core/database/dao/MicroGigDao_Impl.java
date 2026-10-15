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
import com.campus.network.core.database.entity.MicroGigEntity;
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
public final class MicroGigDao_Impl implements MicroGigDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MicroGigEntity> __insertionAdapterOfMicroGigEntity;

  public MicroGigDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMicroGigEntity = new EntityInsertionAdapter<MicroGigEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `micro_gigs` (`id`,`title`,`description`,`payoutAmount`,`currency`,`estimatedDuration`,`posterName`,`posterTrustScore`,`tag`,`urgencyName`,`spotsAvailable`,`timestampMs`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MicroGigEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getDescription());
        statement.bindDouble(4, entity.getPayoutAmount());
        statement.bindString(5, entity.getCurrency());
        statement.bindString(6, entity.getEstimatedDuration());
        statement.bindString(7, entity.getPosterName());
        statement.bindLong(8, entity.getPosterTrustScore());
        statement.bindString(9, entity.getTag());
        statement.bindString(10, entity.getUrgencyName());
        statement.bindLong(11, entity.getSpotsAvailable());
        statement.bindLong(12, entity.getTimestampMs());
      }
    };
  }

  @Override
  public Object insertGigs(final List<MicroGigEntity> gigs,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMicroGigEntity.insert(gigs);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MicroGigEntity>> getAllGigs() {
    final String _sql = "SELECT * FROM micro_gigs ORDER BY timestampMs DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"micro_gigs"}, new Callable<List<MicroGigEntity>>() {
      @Override
      @NonNull
      public List<MicroGigEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfPayoutAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "payoutAmount");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfEstimatedDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "estimatedDuration");
          final int _cursorIndexOfPosterName = CursorUtil.getColumnIndexOrThrow(_cursor, "posterName");
          final int _cursorIndexOfPosterTrustScore = CursorUtil.getColumnIndexOrThrow(_cursor, "posterTrustScore");
          final int _cursorIndexOfTag = CursorUtil.getColumnIndexOrThrow(_cursor, "tag");
          final int _cursorIndexOfUrgencyName = CursorUtil.getColumnIndexOrThrow(_cursor, "urgencyName");
          final int _cursorIndexOfSpotsAvailable = CursorUtil.getColumnIndexOrThrow(_cursor, "spotsAvailable");
          final int _cursorIndexOfTimestampMs = CursorUtil.getColumnIndexOrThrow(_cursor, "timestampMs");
          final List<MicroGigEntity> _result = new ArrayList<MicroGigEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MicroGigEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final double _tmpPayoutAmount;
            _tmpPayoutAmount = _cursor.getDouble(_cursorIndexOfPayoutAmount);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final String _tmpEstimatedDuration;
            _tmpEstimatedDuration = _cursor.getString(_cursorIndexOfEstimatedDuration);
            final String _tmpPosterName;
            _tmpPosterName = _cursor.getString(_cursorIndexOfPosterName);
            final int _tmpPosterTrustScore;
            _tmpPosterTrustScore = _cursor.getInt(_cursorIndexOfPosterTrustScore);
            final String _tmpTag;
            _tmpTag = _cursor.getString(_cursorIndexOfTag);
            final String _tmpUrgencyName;
            _tmpUrgencyName = _cursor.getString(_cursorIndexOfUrgencyName);
            final int _tmpSpotsAvailable;
            _tmpSpotsAvailable = _cursor.getInt(_cursorIndexOfSpotsAvailable);
            final long _tmpTimestampMs;
            _tmpTimestampMs = _cursor.getLong(_cursorIndexOfTimestampMs);
            _item = new MicroGigEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpPayoutAmount,_tmpCurrency,_tmpEstimatedDuration,_tmpPosterName,_tmpPosterTrustScore,_tmpTag,_tmpUrgencyName,_tmpSpotsAvailable,_tmpTimestampMs);
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
