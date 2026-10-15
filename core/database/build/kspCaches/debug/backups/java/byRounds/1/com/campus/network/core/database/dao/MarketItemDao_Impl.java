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
import com.campus.network.core.database.entity.MarketItemEntity;
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
public final class MarketItemDao_Impl implements MarketItemDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MarketItemEntity> __insertionAdapterOfMarketItemEntity;

  public MarketItemDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMarketItemEntity = new EntityInsertionAdapter<MarketItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `market_items` (`id`,`title`,`price`,`currency`,`category`,`condition`,`sellerName`,`sellerRating`,`isCampusVerifiedSeller`,`campusHallLocation`,`imageUrl`,`timestampMs`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MarketItemEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindDouble(3, entity.getPrice());
        statement.bindString(4, entity.getCurrency());
        statement.bindString(5, entity.getCategory());
        statement.bindString(6, entity.getCondition());
        statement.bindString(7, entity.getSellerName());
        statement.bindDouble(8, entity.getSellerRating());
        final int _tmp = entity.isCampusVerifiedSeller() ? 1 : 0;
        statement.bindLong(9, _tmp);
        statement.bindString(10, entity.getCampusHallLocation());
        if (entity.getImageUrl() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getImageUrl());
        }
        statement.bindLong(12, entity.getTimestampMs());
      }
    };
  }

  @Override
  public Object insertItems(final List<MarketItemEntity> items,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMarketItemEntity.insert(items);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MarketItemEntity>> getAllMarketItems() {
    final String _sql = "SELECT * FROM market_items ORDER BY timestampMs DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"market_items"}, new Callable<List<MarketItemEntity>>() {
      @Override
      @NonNull
      public List<MarketItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "price");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfCondition = CursorUtil.getColumnIndexOrThrow(_cursor, "condition");
          final int _cursorIndexOfSellerName = CursorUtil.getColumnIndexOrThrow(_cursor, "sellerName");
          final int _cursorIndexOfSellerRating = CursorUtil.getColumnIndexOrThrow(_cursor, "sellerRating");
          final int _cursorIndexOfIsCampusVerifiedSeller = CursorUtil.getColumnIndexOrThrow(_cursor, "isCampusVerifiedSeller");
          final int _cursorIndexOfCampusHallLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "campusHallLocation");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfTimestampMs = CursorUtil.getColumnIndexOrThrow(_cursor, "timestampMs");
          final List<MarketItemEntity> _result = new ArrayList<MarketItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MarketItemEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final double _tmpPrice;
            _tmpPrice = _cursor.getDouble(_cursorIndexOfPrice);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpCondition;
            _tmpCondition = _cursor.getString(_cursorIndexOfCondition);
            final String _tmpSellerName;
            _tmpSellerName = _cursor.getString(_cursorIndexOfSellerName);
            final float _tmpSellerRating;
            _tmpSellerRating = _cursor.getFloat(_cursorIndexOfSellerRating);
            final boolean _tmpIsCampusVerifiedSeller;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCampusVerifiedSeller);
            _tmpIsCampusVerifiedSeller = _tmp != 0;
            final String _tmpCampusHallLocation;
            _tmpCampusHallLocation = _cursor.getString(_cursorIndexOfCampusHallLocation);
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final long _tmpTimestampMs;
            _tmpTimestampMs = _cursor.getLong(_cursorIndexOfTimestampMs);
            _item = new MarketItemEntity(_tmpId,_tmpTitle,_tmpPrice,_tmpCurrency,_tmpCategory,_tmpCondition,_tmpSellerName,_tmpSellerRating,_tmpIsCampusVerifiedSeller,_tmpCampusHallLocation,_tmpImageUrl,_tmpTimestampMs);
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
