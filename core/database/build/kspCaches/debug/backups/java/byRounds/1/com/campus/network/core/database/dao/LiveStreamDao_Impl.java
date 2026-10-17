package com.campus.network.core.database.dao;

import android.database.Cursor;
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
import com.campus.network.core.database.entity.LiveStreamEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
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
public final class LiveStreamDao_Impl implements LiveStreamDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LiveStreamEntity> __insertionAdapterOfLiveStreamEntity;

  private final SharedSQLiteStatement __preparedStmtOfEndStream;

  private final SharedSQLiteStatement __preparedStmtOfUpdateViewerCount;

  private final SharedSQLiteStatement __preparedStmtOfIncrementLikes;

  public LiveStreamDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLiveStreamEntity = new EntityInsertionAdapter<LiveStreamEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `live_streams` (`id`,`broadcasterId`,`broadcasterName`,`broadcasterHandle`,`broadcasterAvatarRes`,`broadcasterAvatarUrl`,`title`,`description`,`category`,`audience`,`isLive`,`streamPlaybackUrl`,`backdropImageRes`,`backdropImageUrl`,`viewerCount`,`peakViewers`,`likeCount`,`startedAtEpochMs`,`endedAtEpochMs`,`durationSeconds`,`serverId`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LiveStreamEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getBroadcasterId());
        statement.bindString(3, entity.getBroadcasterName());
        statement.bindString(4, entity.getBroadcasterHandle());
        statement.bindLong(5, entity.getBroadcasterAvatarRes());
        if (entity.getBroadcasterAvatarUrl() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getBroadcasterAvatarUrl());
        }
        statement.bindString(7, entity.getTitle());
        statement.bindString(8, entity.getDescription());
        statement.bindString(9, entity.getCategory());
        statement.bindString(10, entity.getAudience());
        final int _tmp = entity.isLive() ? 1 : 0;
        statement.bindLong(11, _tmp);
        if (entity.getStreamPlaybackUrl() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getStreamPlaybackUrl());
        }
        statement.bindLong(13, entity.getBackdropImageRes());
        if (entity.getBackdropImageUrl() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getBackdropImageUrl());
        }
        statement.bindLong(15, entity.getViewerCount());
        statement.bindLong(16, entity.getPeakViewers());
        statement.bindLong(17, entity.getLikeCount());
        statement.bindLong(18, entity.getStartedAtEpochMs());
        if (entity.getEndedAtEpochMs() == null) {
          statement.bindNull(19);
        } else {
          statement.bindLong(19, entity.getEndedAtEpochMs());
        }
        statement.bindLong(20, entity.getDurationSeconds());
        if (entity.getServerId() == null) {
          statement.bindNull(21);
        } else {
          statement.bindString(21, entity.getServerId());
        }
      }
    };
    this.__preparedStmtOfEndStream = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE live_streams SET isLive = 0, endedAtEpochMs = ?, durationSeconds = ?, peakViewers = ?, likeCount = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateViewerCount = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE live_streams SET viewerCount = ?, peakViewers = MAX(peakViewers, ?) WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfIncrementLikes = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE live_streams SET likeCount = likeCount + 1 WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertStream(final LiveStreamEntity stream,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLiveStreamEntity.insert(stream);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertStreams(final List<LiveStreamEntity> streams,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLiveStreamEntity.insert(streams);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object endStream(final String streamId, final long endedAt, final int duration,
      final int peak, final int likes, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfEndStream.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, endedAt);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, duration);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, peak);
        _argIndex = 4;
        _stmt.bindLong(_argIndex, likes);
        _argIndex = 5;
        _stmt.bindString(_argIndex, streamId);
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
          __preparedStmtOfEndStream.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateViewerCount(final String streamId, final int viewers,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateViewerCount.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, viewers);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, viewers);
        _argIndex = 3;
        _stmt.bindString(_argIndex, streamId);
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
          __preparedStmtOfUpdateViewerCount.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object incrementLikes(final String streamId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementLikes.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, streamId);
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
          __preparedStmtOfIncrementLikes.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<LiveStreamEntity>> getAllStreams() {
    final String _sql = "SELECT * FROM live_streams ORDER BY isLive DESC, startedAtEpochMs DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"live_streams"}, new Callable<List<LiveStreamEntity>>() {
      @Override
      @NonNull
      public List<LiveStreamEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfBroadcasterId = CursorUtil.getColumnIndexOrThrow(_cursor, "broadcasterId");
          final int _cursorIndexOfBroadcasterName = CursorUtil.getColumnIndexOrThrow(_cursor, "broadcasterName");
          final int _cursorIndexOfBroadcasterHandle = CursorUtil.getColumnIndexOrThrow(_cursor, "broadcasterHandle");
          final int _cursorIndexOfBroadcasterAvatarRes = CursorUtil.getColumnIndexOrThrow(_cursor, "broadcasterAvatarRes");
          final int _cursorIndexOfBroadcasterAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "broadcasterAvatarUrl");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfAudience = CursorUtil.getColumnIndexOrThrow(_cursor, "audience");
          final int _cursorIndexOfIsLive = CursorUtil.getColumnIndexOrThrow(_cursor, "isLive");
          final int _cursorIndexOfStreamPlaybackUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamPlaybackUrl");
          final int _cursorIndexOfBackdropImageRes = CursorUtil.getColumnIndexOrThrow(_cursor, "backdropImageRes");
          final int _cursorIndexOfBackdropImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "backdropImageUrl");
          final int _cursorIndexOfViewerCount = CursorUtil.getColumnIndexOrThrow(_cursor, "viewerCount");
          final int _cursorIndexOfPeakViewers = CursorUtil.getColumnIndexOrThrow(_cursor, "peakViewers");
          final int _cursorIndexOfLikeCount = CursorUtil.getColumnIndexOrThrow(_cursor, "likeCount");
          final int _cursorIndexOfStartedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAtEpochMs");
          final int _cursorIndexOfEndedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "endedAtEpochMs");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final List<LiveStreamEntity> _result = new ArrayList<LiveStreamEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LiveStreamEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpBroadcasterId;
            _tmpBroadcasterId = _cursor.getString(_cursorIndexOfBroadcasterId);
            final String _tmpBroadcasterName;
            _tmpBroadcasterName = _cursor.getString(_cursorIndexOfBroadcasterName);
            final String _tmpBroadcasterHandle;
            _tmpBroadcasterHandle = _cursor.getString(_cursorIndexOfBroadcasterHandle);
            final int _tmpBroadcasterAvatarRes;
            _tmpBroadcasterAvatarRes = _cursor.getInt(_cursorIndexOfBroadcasterAvatarRes);
            final String _tmpBroadcasterAvatarUrl;
            if (_cursor.isNull(_cursorIndexOfBroadcasterAvatarUrl)) {
              _tmpBroadcasterAvatarUrl = null;
            } else {
              _tmpBroadcasterAvatarUrl = _cursor.getString(_cursorIndexOfBroadcasterAvatarUrl);
            }
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpAudience;
            _tmpAudience = _cursor.getString(_cursorIndexOfAudience);
            final boolean _tmpIsLive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsLive);
            _tmpIsLive = _tmp != 0;
            final String _tmpStreamPlaybackUrl;
            if (_cursor.isNull(_cursorIndexOfStreamPlaybackUrl)) {
              _tmpStreamPlaybackUrl = null;
            } else {
              _tmpStreamPlaybackUrl = _cursor.getString(_cursorIndexOfStreamPlaybackUrl);
            }
            final int _tmpBackdropImageRes;
            _tmpBackdropImageRes = _cursor.getInt(_cursorIndexOfBackdropImageRes);
            final String _tmpBackdropImageUrl;
            if (_cursor.isNull(_cursorIndexOfBackdropImageUrl)) {
              _tmpBackdropImageUrl = null;
            } else {
              _tmpBackdropImageUrl = _cursor.getString(_cursorIndexOfBackdropImageUrl);
            }
            final int _tmpViewerCount;
            _tmpViewerCount = _cursor.getInt(_cursorIndexOfViewerCount);
            final int _tmpPeakViewers;
            _tmpPeakViewers = _cursor.getInt(_cursorIndexOfPeakViewers);
            final int _tmpLikeCount;
            _tmpLikeCount = _cursor.getInt(_cursorIndexOfLikeCount);
            final long _tmpStartedAtEpochMs;
            _tmpStartedAtEpochMs = _cursor.getLong(_cursorIndexOfStartedAtEpochMs);
            final Long _tmpEndedAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfEndedAtEpochMs)) {
              _tmpEndedAtEpochMs = null;
            } else {
              _tmpEndedAtEpochMs = _cursor.getLong(_cursorIndexOfEndedAtEpochMs);
            }
            final int _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getInt(_cursorIndexOfDurationSeconds);
            final String _tmpServerId;
            if (_cursor.isNull(_cursorIndexOfServerId)) {
              _tmpServerId = null;
            } else {
              _tmpServerId = _cursor.getString(_cursorIndexOfServerId);
            }
            _item = new LiveStreamEntity(_tmpId,_tmpBroadcasterId,_tmpBroadcasterName,_tmpBroadcasterHandle,_tmpBroadcasterAvatarRes,_tmpBroadcasterAvatarUrl,_tmpTitle,_tmpDescription,_tmpCategory,_tmpAudience,_tmpIsLive,_tmpStreamPlaybackUrl,_tmpBackdropImageRes,_tmpBackdropImageUrl,_tmpViewerCount,_tmpPeakViewers,_tmpLikeCount,_tmpStartedAtEpochMs,_tmpEndedAtEpochMs,_tmpDurationSeconds,_tmpServerId);
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
  public Flow<List<LiveStreamEntity>> getActiveStreams() {
    final String _sql = "SELECT * FROM live_streams WHERE isLive = 1 ORDER BY startedAtEpochMs DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"live_streams"}, new Callable<List<LiveStreamEntity>>() {
      @Override
      @NonNull
      public List<LiveStreamEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfBroadcasterId = CursorUtil.getColumnIndexOrThrow(_cursor, "broadcasterId");
          final int _cursorIndexOfBroadcasterName = CursorUtil.getColumnIndexOrThrow(_cursor, "broadcasterName");
          final int _cursorIndexOfBroadcasterHandle = CursorUtil.getColumnIndexOrThrow(_cursor, "broadcasterHandle");
          final int _cursorIndexOfBroadcasterAvatarRes = CursorUtil.getColumnIndexOrThrow(_cursor, "broadcasterAvatarRes");
          final int _cursorIndexOfBroadcasterAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "broadcasterAvatarUrl");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfAudience = CursorUtil.getColumnIndexOrThrow(_cursor, "audience");
          final int _cursorIndexOfIsLive = CursorUtil.getColumnIndexOrThrow(_cursor, "isLive");
          final int _cursorIndexOfStreamPlaybackUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamPlaybackUrl");
          final int _cursorIndexOfBackdropImageRes = CursorUtil.getColumnIndexOrThrow(_cursor, "backdropImageRes");
          final int _cursorIndexOfBackdropImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "backdropImageUrl");
          final int _cursorIndexOfViewerCount = CursorUtil.getColumnIndexOrThrow(_cursor, "viewerCount");
          final int _cursorIndexOfPeakViewers = CursorUtil.getColumnIndexOrThrow(_cursor, "peakViewers");
          final int _cursorIndexOfLikeCount = CursorUtil.getColumnIndexOrThrow(_cursor, "likeCount");
          final int _cursorIndexOfStartedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAtEpochMs");
          final int _cursorIndexOfEndedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "endedAtEpochMs");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final List<LiveStreamEntity> _result = new ArrayList<LiveStreamEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LiveStreamEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpBroadcasterId;
            _tmpBroadcasterId = _cursor.getString(_cursorIndexOfBroadcasterId);
            final String _tmpBroadcasterName;
            _tmpBroadcasterName = _cursor.getString(_cursorIndexOfBroadcasterName);
            final String _tmpBroadcasterHandle;
            _tmpBroadcasterHandle = _cursor.getString(_cursorIndexOfBroadcasterHandle);
            final int _tmpBroadcasterAvatarRes;
            _tmpBroadcasterAvatarRes = _cursor.getInt(_cursorIndexOfBroadcasterAvatarRes);
            final String _tmpBroadcasterAvatarUrl;
            if (_cursor.isNull(_cursorIndexOfBroadcasterAvatarUrl)) {
              _tmpBroadcasterAvatarUrl = null;
            } else {
              _tmpBroadcasterAvatarUrl = _cursor.getString(_cursorIndexOfBroadcasterAvatarUrl);
            }
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpAudience;
            _tmpAudience = _cursor.getString(_cursorIndexOfAudience);
            final boolean _tmpIsLive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsLive);
            _tmpIsLive = _tmp != 0;
            final String _tmpStreamPlaybackUrl;
            if (_cursor.isNull(_cursorIndexOfStreamPlaybackUrl)) {
              _tmpStreamPlaybackUrl = null;
            } else {
              _tmpStreamPlaybackUrl = _cursor.getString(_cursorIndexOfStreamPlaybackUrl);
            }
            final int _tmpBackdropImageRes;
            _tmpBackdropImageRes = _cursor.getInt(_cursorIndexOfBackdropImageRes);
            final String _tmpBackdropImageUrl;
            if (_cursor.isNull(_cursorIndexOfBackdropImageUrl)) {
              _tmpBackdropImageUrl = null;
            } else {
              _tmpBackdropImageUrl = _cursor.getString(_cursorIndexOfBackdropImageUrl);
            }
            final int _tmpViewerCount;
            _tmpViewerCount = _cursor.getInt(_cursorIndexOfViewerCount);
            final int _tmpPeakViewers;
            _tmpPeakViewers = _cursor.getInt(_cursorIndexOfPeakViewers);
            final int _tmpLikeCount;
            _tmpLikeCount = _cursor.getInt(_cursorIndexOfLikeCount);
            final long _tmpStartedAtEpochMs;
            _tmpStartedAtEpochMs = _cursor.getLong(_cursorIndexOfStartedAtEpochMs);
            final Long _tmpEndedAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfEndedAtEpochMs)) {
              _tmpEndedAtEpochMs = null;
            } else {
              _tmpEndedAtEpochMs = _cursor.getLong(_cursorIndexOfEndedAtEpochMs);
            }
            final int _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getInt(_cursorIndexOfDurationSeconds);
            final String _tmpServerId;
            if (_cursor.isNull(_cursorIndexOfServerId)) {
              _tmpServerId = null;
            } else {
              _tmpServerId = _cursor.getString(_cursorIndexOfServerId);
            }
            _item = new LiveStreamEntity(_tmpId,_tmpBroadcasterId,_tmpBroadcasterName,_tmpBroadcasterHandle,_tmpBroadcasterAvatarRes,_tmpBroadcasterAvatarUrl,_tmpTitle,_tmpDescription,_tmpCategory,_tmpAudience,_tmpIsLive,_tmpStreamPlaybackUrl,_tmpBackdropImageRes,_tmpBackdropImageUrl,_tmpViewerCount,_tmpPeakViewers,_tmpLikeCount,_tmpStartedAtEpochMs,_tmpEndedAtEpochMs,_tmpDurationSeconds,_tmpServerId);
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
  public Flow<LiveStreamEntity> getStreamById(final String streamId) {
    final String _sql = "SELECT * FROM live_streams WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, streamId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"live_streams"}, new Callable<LiveStreamEntity>() {
      @Override
      @Nullable
      public LiveStreamEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfBroadcasterId = CursorUtil.getColumnIndexOrThrow(_cursor, "broadcasterId");
          final int _cursorIndexOfBroadcasterName = CursorUtil.getColumnIndexOrThrow(_cursor, "broadcasterName");
          final int _cursorIndexOfBroadcasterHandle = CursorUtil.getColumnIndexOrThrow(_cursor, "broadcasterHandle");
          final int _cursorIndexOfBroadcasterAvatarRes = CursorUtil.getColumnIndexOrThrow(_cursor, "broadcasterAvatarRes");
          final int _cursorIndexOfBroadcasterAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "broadcasterAvatarUrl");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfAudience = CursorUtil.getColumnIndexOrThrow(_cursor, "audience");
          final int _cursorIndexOfIsLive = CursorUtil.getColumnIndexOrThrow(_cursor, "isLive");
          final int _cursorIndexOfStreamPlaybackUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamPlaybackUrl");
          final int _cursorIndexOfBackdropImageRes = CursorUtil.getColumnIndexOrThrow(_cursor, "backdropImageRes");
          final int _cursorIndexOfBackdropImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "backdropImageUrl");
          final int _cursorIndexOfViewerCount = CursorUtil.getColumnIndexOrThrow(_cursor, "viewerCount");
          final int _cursorIndexOfPeakViewers = CursorUtil.getColumnIndexOrThrow(_cursor, "peakViewers");
          final int _cursorIndexOfLikeCount = CursorUtil.getColumnIndexOrThrow(_cursor, "likeCount");
          final int _cursorIndexOfStartedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAtEpochMs");
          final int _cursorIndexOfEndedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "endedAtEpochMs");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final LiveStreamEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpBroadcasterId;
            _tmpBroadcasterId = _cursor.getString(_cursorIndexOfBroadcasterId);
            final String _tmpBroadcasterName;
            _tmpBroadcasterName = _cursor.getString(_cursorIndexOfBroadcasterName);
            final String _tmpBroadcasterHandle;
            _tmpBroadcasterHandle = _cursor.getString(_cursorIndexOfBroadcasterHandle);
            final int _tmpBroadcasterAvatarRes;
            _tmpBroadcasterAvatarRes = _cursor.getInt(_cursorIndexOfBroadcasterAvatarRes);
            final String _tmpBroadcasterAvatarUrl;
            if (_cursor.isNull(_cursorIndexOfBroadcasterAvatarUrl)) {
              _tmpBroadcasterAvatarUrl = null;
            } else {
              _tmpBroadcasterAvatarUrl = _cursor.getString(_cursorIndexOfBroadcasterAvatarUrl);
            }
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpAudience;
            _tmpAudience = _cursor.getString(_cursorIndexOfAudience);
            final boolean _tmpIsLive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsLive);
            _tmpIsLive = _tmp != 0;
            final String _tmpStreamPlaybackUrl;
            if (_cursor.isNull(_cursorIndexOfStreamPlaybackUrl)) {
              _tmpStreamPlaybackUrl = null;
            } else {
              _tmpStreamPlaybackUrl = _cursor.getString(_cursorIndexOfStreamPlaybackUrl);
            }
            final int _tmpBackdropImageRes;
            _tmpBackdropImageRes = _cursor.getInt(_cursorIndexOfBackdropImageRes);
            final String _tmpBackdropImageUrl;
            if (_cursor.isNull(_cursorIndexOfBackdropImageUrl)) {
              _tmpBackdropImageUrl = null;
            } else {
              _tmpBackdropImageUrl = _cursor.getString(_cursorIndexOfBackdropImageUrl);
            }
            final int _tmpViewerCount;
            _tmpViewerCount = _cursor.getInt(_cursorIndexOfViewerCount);
            final int _tmpPeakViewers;
            _tmpPeakViewers = _cursor.getInt(_cursorIndexOfPeakViewers);
            final int _tmpLikeCount;
            _tmpLikeCount = _cursor.getInt(_cursorIndexOfLikeCount);
            final long _tmpStartedAtEpochMs;
            _tmpStartedAtEpochMs = _cursor.getLong(_cursorIndexOfStartedAtEpochMs);
            final Long _tmpEndedAtEpochMs;
            if (_cursor.isNull(_cursorIndexOfEndedAtEpochMs)) {
              _tmpEndedAtEpochMs = null;
            } else {
              _tmpEndedAtEpochMs = _cursor.getLong(_cursorIndexOfEndedAtEpochMs);
            }
            final int _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getInt(_cursorIndexOfDurationSeconds);
            final String _tmpServerId;
            if (_cursor.isNull(_cursorIndexOfServerId)) {
              _tmpServerId = null;
            } else {
              _tmpServerId = _cursor.getString(_cursorIndexOfServerId);
            }
            _result = new LiveStreamEntity(_tmpId,_tmpBroadcasterId,_tmpBroadcasterName,_tmpBroadcasterHandle,_tmpBroadcasterAvatarRes,_tmpBroadcasterAvatarUrl,_tmpTitle,_tmpDescription,_tmpCategory,_tmpAudience,_tmpIsLive,_tmpStreamPlaybackUrl,_tmpBackdropImageRes,_tmpBackdropImageUrl,_tmpViewerCount,_tmpPeakViewers,_tmpLikeCount,_tmpStartedAtEpochMs,_tmpEndedAtEpochMs,_tmpDurationSeconds,_tmpServerId);
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
