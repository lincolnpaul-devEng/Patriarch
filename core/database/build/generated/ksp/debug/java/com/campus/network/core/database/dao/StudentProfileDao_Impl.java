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
import com.campus.network.core.database.entity.StudentProfileEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class StudentProfileDao_Impl implements StudentProfileDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<StudentProfileEntity> __insertionAdapterOfStudentProfileEntity;

  private final SharedSQLiteStatement __preparedStmtOfClearProfile;

  public StudentProfileDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfStudentProfileEntity = new EntityInsertionAdapter<StudentProfileEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `campusprofile` (`id`,`studentId`,`fullName`,`avatarUrl`,`universityEmail`,`universityName`,`degreeProgram`,`academicYear`,`campusResidence`,`phoneNumber`,`bio`,`consistencyStreakDays`,`behavioralTrustScore`,`campusMeritPoints`,`preferredRendezvousPoint`,`personaRole`,`statusNote`,`statusNoteEmoji`,`statusNoteExpiry`,`storageUsedBytes`,`storageQuotaBytes`,`notificationsEnabled`,`escrowAlertsEnabled`,`questRemindersEnabled`,`isTrustScorePublic`,`isOnlineStatusVisible`,`activityLedgerVisibility`,`locationPrivacyScope`,`walletBalance`,`vaultProtectedBalance`,`onboardingPreferencesJson`,`interestsCsv`,`savedListingIdsCsv`,`watchlistIdsCsv`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final StudentProfileEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getStudentId());
        statement.bindString(3, entity.getFullName());
        if (entity.getAvatarUrl() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getAvatarUrl());
        }
        statement.bindString(5, entity.getUniversityEmail());
        statement.bindString(6, entity.getUniversityName());
        statement.bindString(7, entity.getDegreeProgram());
        statement.bindString(8, entity.getAcademicYear());
        statement.bindString(9, entity.getCampusResidence());
        statement.bindString(10, entity.getPhoneNumber());
        statement.bindString(11, entity.getBio());
        statement.bindLong(12, entity.getConsistencyStreakDays());
        statement.bindLong(13, entity.getBehavioralTrustScore());
        statement.bindLong(14, entity.getCampusMeritPoints());
        statement.bindString(15, entity.getPreferredRendezvousPoint());
        statement.bindString(16, entity.getPersonaRole());
        if (entity.getStatusNote() == null) {
          statement.bindNull(17);
        } else {
          statement.bindString(17, entity.getStatusNote());
        }
        if (entity.getStatusNoteEmoji() == null) {
          statement.bindNull(18);
        } else {
          statement.bindString(18, entity.getStatusNoteEmoji());
        }
        if (entity.getStatusNoteExpiry() == null) {
          statement.bindNull(19);
        } else {
          statement.bindString(19, entity.getStatusNoteExpiry());
        }
        statement.bindLong(20, entity.getStorageUsedBytes());
        statement.bindLong(21, entity.getStorageQuotaBytes());
        final int _tmp = entity.getNotificationsEnabled() ? 1 : 0;
        statement.bindLong(22, _tmp);
        final int _tmp_1 = entity.getEscrowAlertsEnabled() ? 1 : 0;
        statement.bindLong(23, _tmp_1);
        final int _tmp_2 = entity.getQuestRemindersEnabled() ? 1 : 0;
        statement.bindLong(24, _tmp_2);
        final int _tmp_3 = entity.isTrustScorePublic() ? 1 : 0;
        statement.bindLong(25, _tmp_3);
        final int _tmp_4 = entity.isOnlineStatusVisible() ? 1 : 0;
        statement.bindLong(26, _tmp_4);
        statement.bindString(27, entity.getActivityLedgerVisibility());
        statement.bindString(28, entity.getLocationPrivacyScope());
        statement.bindDouble(29, entity.getWalletBalance());
        statement.bindDouble(30, entity.getVaultProtectedBalance());
        statement.bindString(31, entity.getOnboardingPreferencesJson());
        statement.bindString(32, entity.getInterestsCsv());
        statement.bindString(33, entity.getSavedListingIdsCsv());
        statement.bindString(34, entity.getWatchlistIdsCsv());
      }
    };
    this.__preparedStmtOfClearProfile = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM campusprofile";
        return _query;
      }
    };
  }

  @Override
  public Object insertOrUpdateProfile(final StudentProfileEntity profile,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfStudentProfileEntity.insert(profile);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearProfile(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearProfile.acquire();
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
          __preparedStmtOfClearProfile.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<StudentProfileEntity> getProfile() {
    final String _sql = "SELECT * FROM campusprofile LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"campusprofile"}, new Callable<StudentProfileEntity>() {
      @Override
      @Nullable
      public StudentProfileEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStudentId = CursorUtil.getColumnIndexOrThrow(_cursor, "studentId");
          final int _cursorIndexOfFullName = CursorUtil.getColumnIndexOrThrow(_cursor, "fullName");
          final int _cursorIndexOfAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarUrl");
          final int _cursorIndexOfUniversityEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "universityEmail");
          final int _cursorIndexOfUniversityName = CursorUtil.getColumnIndexOrThrow(_cursor, "universityName");
          final int _cursorIndexOfDegreeProgram = CursorUtil.getColumnIndexOrThrow(_cursor, "degreeProgram");
          final int _cursorIndexOfAcademicYear = CursorUtil.getColumnIndexOrThrow(_cursor, "academicYear");
          final int _cursorIndexOfCampusResidence = CursorUtil.getColumnIndexOrThrow(_cursor, "campusResidence");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfBio = CursorUtil.getColumnIndexOrThrow(_cursor, "bio");
          final int _cursorIndexOfConsistencyStreakDays = CursorUtil.getColumnIndexOrThrow(_cursor, "consistencyStreakDays");
          final int _cursorIndexOfBehavioralTrustScore = CursorUtil.getColumnIndexOrThrow(_cursor, "behavioralTrustScore");
          final int _cursorIndexOfCampusMeritPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "campusMeritPoints");
          final int _cursorIndexOfPreferredRendezvousPoint = CursorUtil.getColumnIndexOrThrow(_cursor, "preferredRendezvousPoint");
          final int _cursorIndexOfPersonaRole = CursorUtil.getColumnIndexOrThrow(_cursor, "personaRole");
          final int _cursorIndexOfStatusNote = CursorUtil.getColumnIndexOrThrow(_cursor, "statusNote");
          final int _cursorIndexOfStatusNoteEmoji = CursorUtil.getColumnIndexOrThrow(_cursor, "statusNoteEmoji");
          final int _cursorIndexOfStatusNoteExpiry = CursorUtil.getColumnIndexOrThrow(_cursor, "statusNoteExpiry");
          final int _cursorIndexOfStorageUsedBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "storageUsedBytes");
          final int _cursorIndexOfStorageQuotaBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "storageQuotaBytes");
          final int _cursorIndexOfNotificationsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "notificationsEnabled");
          final int _cursorIndexOfEscrowAlertsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "escrowAlertsEnabled");
          final int _cursorIndexOfQuestRemindersEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "questRemindersEnabled");
          final int _cursorIndexOfIsTrustScorePublic = CursorUtil.getColumnIndexOrThrow(_cursor, "isTrustScorePublic");
          final int _cursorIndexOfIsOnlineStatusVisible = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnlineStatusVisible");
          final int _cursorIndexOfActivityLedgerVisibility = CursorUtil.getColumnIndexOrThrow(_cursor, "activityLedgerVisibility");
          final int _cursorIndexOfLocationPrivacyScope = CursorUtil.getColumnIndexOrThrow(_cursor, "locationPrivacyScope");
          final int _cursorIndexOfWalletBalance = CursorUtil.getColumnIndexOrThrow(_cursor, "walletBalance");
          final int _cursorIndexOfVaultProtectedBalance = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultProtectedBalance");
          final int _cursorIndexOfOnboardingPreferencesJson = CursorUtil.getColumnIndexOrThrow(_cursor, "onboardingPreferencesJson");
          final int _cursorIndexOfInterestsCsv = CursorUtil.getColumnIndexOrThrow(_cursor, "interestsCsv");
          final int _cursorIndexOfSavedListingIdsCsv = CursorUtil.getColumnIndexOrThrow(_cursor, "savedListingIdsCsv");
          final int _cursorIndexOfWatchlistIdsCsv = CursorUtil.getColumnIndexOrThrow(_cursor, "watchlistIdsCsv");
          final StudentProfileEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpStudentId;
            _tmpStudentId = _cursor.getString(_cursorIndexOfStudentId);
            final String _tmpFullName;
            _tmpFullName = _cursor.getString(_cursorIndexOfFullName);
            final String _tmpAvatarUrl;
            if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
              _tmpAvatarUrl = null;
            } else {
              _tmpAvatarUrl = _cursor.getString(_cursorIndexOfAvatarUrl);
            }
            final String _tmpUniversityEmail;
            _tmpUniversityEmail = _cursor.getString(_cursorIndexOfUniversityEmail);
            final String _tmpUniversityName;
            _tmpUniversityName = _cursor.getString(_cursorIndexOfUniversityName);
            final String _tmpDegreeProgram;
            _tmpDegreeProgram = _cursor.getString(_cursorIndexOfDegreeProgram);
            final String _tmpAcademicYear;
            _tmpAcademicYear = _cursor.getString(_cursorIndexOfAcademicYear);
            final String _tmpCampusResidence;
            _tmpCampusResidence = _cursor.getString(_cursorIndexOfCampusResidence);
            final String _tmpPhoneNumber;
            _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            final String _tmpBio;
            _tmpBio = _cursor.getString(_cursorIndexOfBio);
            final int _tmpConsistencyStreakDays;
            _tmpConsistencyStreakDays = _cursor.getInt(_cursorIndexOfConsistencyStreakDays);
            final int _tmpBehavioralTrustScore;
            _tmpBehavioralTrustScore = _cursor.getInt(_cursorIndexOfBehavioralTrustScore);
            final int _tmpCampusMeritPoints;
            _tmpCampusMeritPoints = _cursor.getInt(_cursorIndexOfCampusMeritPoints);
            final String _tmpPreferredRendezvousPoint;
            _tmpPreferredRendezvousPoint = _cursor.getString(_cursorIndexOfPreferredRendezvousPoint);
            final String _tmpPersonaRole;
            _tmpPersonaRole = _cursor.getString(_cursorIndexOfPersonaRole);
            final String _tmpStatusNote;
            if (_cursor.isNull(_cursorIndexOfStatusNote)) {
              _tmpStatusNote = null;
            } else {
              _tmpStatusNote = _cursor.getString(_cursorIndexOfStatusNote);
            }
            final String _tmpStatusNoteEmoji;
            if (_cursor.isNull(_cursorIndexOfStatusNoteEmoji)) {
              _tmpStatusNoteEmoji = null;
            } else {
              _tmpStatusNoteEmoji = _cursor.getString(_cursorIndexOfStatusNoteEmoji);
            }
            final String _tmpStatusNoteExpiry;
            if (_cursor.isNull(_cursorIndexOfStatusNoteExpiry)) {
              _tmpStatusNoteExpiry = null;
            } else {
              _tmpStatusNoteExpiry = _cursor.getString(_cursorIndexOfStatusNoteExpiry);
            }
            final long _tmpStorageUsedBytes;
            _tmpStorageUsedBytes = _cursor.getLong(_cursorIndexOfStorageUsedBytes);
            final long _tmpStorageQuotaBytes;
            _tmpStorageQuotaBytes = _cursor.getLong(_cursorIndexOfStorageQuotaBytes);
            final boolean _tmpNotificationsEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfNotificationsEnabled);
            _tmpNotificationsEnabled = _tmp != 0;
            final boolean _tmpEscrowAlertsEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfEscrowAlertsEnabled);
            _tmpEscrowAlertsEnabled = _tmp_1 != 0;
            final boolean _tmpQuestRemindersEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfQuestRemindersEnabled);
            _tmpQuestRemindersEnabled = _tmp_2 != 0;
            final boolean _tmpIsTrustScorePublic;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsTrustScorePublic);
            _tmpIsTrustScorePublic = _tmp_3 != 0;
            final boolean _tmpIsOnlineStatusVisible;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsOnlineStatusVisible);
            _tmpIsOnlineStatusVisible = _tmp_4 != 0;
            final String _tmpActivityLedgerVisibility;
            _tmpActivityLedgerVisibility = _cursor.getString(_cursorIndexOfActivityLedgerVisibility);
            final String _tmpLocationPrivacyScope;
            _tmpLocationPrivacyScope = _cursor.getString(_cursorIndexOfLocationPrivacyScope);
            final double _tmpWalletBalance;
            _tmpWalletBalance = _cursor.getDouble(_cursorIndexOfWalletBalance);
            final double _tmpVaultProtectedBalance;
            _tmpVaultProtectedBalance = _cursor.getDouble(_cursorIndexOfVaultProtectedBalance);
            final String _tmpOnboardingPreferencesJson;
            _tmpOnboardingPreferencesJson = _cursor.getString(_cursorIndexOfOnboardingPreferencesJson);
            final String _tmpInterestsCsv;
            _tmpInterestsCsv = _cursor.getString(_cursorIndexOfInterestsCsv);
            final String _tmpSavedListingIdsCsv;
            _tmpSavedListingIdsCsv = _cursor.getString(_cursorIndexOfSavedListingIdsCsv);
            final String _tmpWatchlistIdsCsv;
            _tmpWatchlistIdsCsv = _cursor.getString(_cursorIndexOfWatchlistIdsCsv);
            _result = new StudentProfileEntity(_tmpId,_tmpStudentId,_tmpFullName,_tmpAvatarUrl,_tmpUniversityEmail,_tmpUniversityName,_tmpDegreeProgram,_tmpAcademicYear,_tmpCampusResidence,_tmpPhoneNumber,_tmpBio,_tmpConsistencyStreakDays,_tmpBehavioralTrustScore,_tmpCampusMeritPoints,_tmpPreferredRendezvousPoint,_tmpPersonaRole,_tmpStatusNote,_tmpStatusNoteEmoji,_tmpStatusNoteExpiry,_tmpStorageUsedBytes,_tmpStorageQuotaBytes,_tmpNotificationsEnabled,_tmpEscrowAlertsEnabled,_tmpQuestRemindersEnabled,_tmpIsTrustScorePublic,_tmpIsOnlineStatusVisible,_tmpActivityLedgerVisibility,_tmpLocationPrivacyScope,_tmpWalletBalance,_tmpVaultProtectedBalance,_tmpOnboardingPreferencesJson,_tmpInterestsCsv,_tmpSavedListingIdsCsv,_tmpWatchlistIdsCsv);
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
  public Object getProfileSync(final Continuation<? super StudentProfileEntity> $completion) {
    final String _sql = "SELECT * FROM campusprofile LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<StudentProfileEntity>() {
      @Override
      @Nullable
      public StudentProfileEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStudentId = CursorUtil.getColumnIndexOrThrow(_cursor, "studentId");
          final int _cursorIndexOfFullName = CursorUtil.getColumnIndexOrThrow(_cursor, "fullName");
          final int _cursorIndexOfAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarUrl");
          final int _cursorIndexOfUniversityEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "universityEmail");
          final int _cursorIndexOfUniversityName = CursorUtil.getColumnIndexOrThrow(_cursor, "universityName");
          final int _cursorIndexOfDegreeProgram = CursorUtil.getColumnIndexOrThrow(_cursor, "degreeProgram");
          final int _cursorIndexOfAcademicYear = CursorUtil.getColumnIndexOrThrow(_cursor, "academicYear");
          final int _cursorIndexOfCampusResidence = CursorUtil.getColumnIndexOrThrow(_cursor, "campusResidence");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfBio = CursorUtil.getColumnIndexOrThrow(_cursor, "bio");
          final int _cursorIndexOfConsistencyStreakDays = CursorUtil.getColumnIndexOrThrow(_cursor, "consistencyStreakDays");
          final int _cursorIndexOfBehavioralTrustScore = CursorUtil.getColumnIndexOrThrow(_cursor, "behavioralTrustScore");
          final int _cursorIndexOfCampusMeritPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "campusMeritPoints");
          final int _cursorIndexOfPreferredRendezvousPoint = CursorUtil.getColumnIndexOrThrow(_cursor, "preferredRendezvousPoint");
          final int _cursorIndexOfPersonaRole = CursorUtil.getColumnIndexOrThrow(_cursor, "personaRole");
          final int _cursorIndexOfStatusNote = CursorUtil.getColumnIndexOrThrow(_cursor, "statusNote");
          final int _cursorIndexOfStatusNoteEmoji = CursorUtil.getColumnIndexOrThrow(_cursor, "statusNoteEmoji");
          final int _cursorIndexOfStatusNoteExpiry = CursorUtil.getColumnIndexOrThrow(_cursor, "statusNoteExpiry");
          final int _cursorIndexOfStorageUsedBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "storageUsedBytes");
          final int _cursorIndexOfStorageQuotaBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "storageQuotaBytes");
          final int _cursorIndexOfNotificationsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "notificationsEnabled");
          final int _cursorIndexOfEscrowAlertsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "escrowAlertsEnabled");
          final int _cursorIndexOfQuestRemindersEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "questRemindersEnabled");
          final int _cursorIndexOfIsTrustScorePublic = CursorUtil.getColumnIndexOrThrow(_cursor, "isTrustScorePublic");
          final int _cursorIndexOfIsOnlineStatusVisible = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnlineStatusVisible");
          final int _cursorIndexOfActivityLedgerVisibility = CursorUtil.getColumnIndexOrThrow(_cursor, "activityLedgerVisibility");
          final int _cursorIndexOfLocationPrivacyScope = CursorUtil.getColumnIndexOrThrow(_cursor, "locationPrivacyScope");
          final int _cursorIndexOfWalletBalance = CursorUtil.getColumnIndexOrThrow(_cursor, "walletBalance");
          final int _cursorIndexOfVaultProtectedBalance = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultProtectedBalance");
          final int _cursorIndexOfOnboardingPreferencesJson = CursorUtil.getColumnIndexOrThrow(_cursor, "onboardingPreferencesJson");
          final int _cursorIndexOfInterestsCsv = CursorUtil.getColumnIndexOrThrow(_cursor, "interestsCsv");
          final int _cursorIndexOfSavedListingIdsCsv = CursorUtil.getColumnIndexOrThrow(_cursor, "savedListingIdsCsv");
          final int _cursorIndexOfWatchlistIdsCsv = CursorUtil.getColumnIndexOrThrow(_cursor, "watchlistIdsCsv");
          final StudentProfileEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpStudentId;
            _tmpStudentId = _cursor.getString(_cursorIndexOfStudentId);
            final String _tmpFullName;
            _tmpFullName = _cursor.getString(_cursorIndexOfFullName);
            final String _tmpAvatarUrl;
            if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
              _tmpAvatarUrl = null;
            } else {
              _tmpAvatarUrl = _cursor.getString(_cursorIndexOfAvatarUrl);
            }
            final String _tmpUniversityEmail;
            _tmpUniversityEmail = _cursor.getString(_cursorIndexOfUniversityEmail);
            final String _tmpUniversityName;
            _tmpUniversityName = _cursor.getString(_cursorIndexOfUniversityName);
            final String _tmpDegreeProgram;
            _tmpDegreeProgram = _cursor.getString(_cursorIndexOfDegreeProgram);
            final String _tmpAcademicYear;
            _tmpAcademicYear = _cursor.getString(_cursorIndexOfAcademicYear);
            final String _tmpCampusResidence;
            _tmpCampusResidence = _cursor.getString(_cursorIndexOfCampusResidence);
            final String _tmpPhoneNumber;
            _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            final String _tmpBio;
            _tmpBio = _cursor.getString(_cursorIndexOfBio);
            final int _tmpConsistencyStreakDays;
            _tmpConsistencyStreakDays = _cursor.getInt(_cursorIndexOfConsistencyStreakDays);
            final int _tmpBehavioralTrustScore;
            _tmpBehavioralTrustScore = _cursor.getInt(_cursorIndexOfBehavioralTrustScore);
            final int _tmpCampusMeritPoints;
            _tmpCampusMeritPoints = _cursor.getInt(_cursorIndexOfCampusMeritPoints);
            final String _tmpPreferredRendezvousPoint;
            _tmpPreferredRendezvousPoint = _cursor.getString(_cursorIndexOfPreferredRendezvousPoint);
            final String _tmpPersonaRole;
            _tmpPersonaRole = _cursor.getString(_cursorIndexOfPersonaRole);
            final String _tmpStatusNote;
            if (_cursor.isNull(_cursorIndexOfStatusNote)) {
              _tmpStatusNote = null;
            } else {
              _tmpStatusNote = _cursor.getString(_cursorIndexOfStatusNote);
            }
            final String _tmpStatusNoteEmoji;
            if (_cursor.isNull(_cursorIndexOfStatusNoteEmoji)) {
              _tmpStatusNoteEmoji = null;
            } else {
              _tmpStatusNoteEmoji = _cursor.getString(_cursorIndexOfStatusNoteEmoji);
            }
            final String _tmpStatusNoteExpiry;
            if (_cursor.isNull(_cursorIndexOfStatusNoteExpiry)) {
              _tmpStatusNoteExpiry = null;
            } else {
              _tmpStatusNoteExpiry = _cursor.getString(_cursorIndexOfStatusNoteExpiry);
            }
            final long _tmpStorageUsedBytes;
            _tmpStorageUsedBytes = _cursor.getLong(_cursorIndexOfStorageUsedBytes);
            final long _tmpStorageQuotaBytes;
            _tmpStorageQuotaBytes = _cursor.getLong(_cursorIndexOfStorageQuotaBytes);
            final boolean _tmpNotificationsEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfNotificationsEnabled);
            _tmpNotificationsEnabled = _tmp != 0;
            final boolean _tmpEscrowAlertsEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfEscrowAlertsEnabled);
            _tmpEscrowAlertsEnabled = _tmp_1 != 0;
            final boolean _tmpQuestRemindersEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfQuestRemindersEnabled);
            _tmpQuestRemindersEnabled = _tmp_2 != 0;
            final boolean _tmpIsTrustScorePublic;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsTrustScorePublic);
            _tmpIsTrustScorePublic = _tmp_3 != 0;
            final boolean _tmpIsOnlineStatusVisible;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsOnlineStatusVisible);
            _tmpIsOnlineStatusVisible = _tmp_4 != 0;
            final String _tmpActivityLedgerVisibility;
            _tmpActivityLedgerVisibility = _cursor.getString(_cursorIndexOfActivityLedgerVisibility);
            final String _tmpLocationPrivacyScope;
            _tmpLocationPrivacyScope = _cursor.getString(_cursorIndexOfLocationPrivacyScope);
            final double _tmpWalletBalance;
            _tmpWalletBalance = _cursor.getDouble(_cursorIndexOfWalletBalance);
            final double _tmpVaultProtectedBalance;
            _tmpVaultProtectedBalance = _cursor.getDouble(_cursorIndexOfVaultProtectedBalance);
            final String _tmpOnboardingPreferencesJson;
            _tmpOnboardingPreferencesJson = _cursor.getString(_cursorIndexOfOnboardingPreferencesJson);
            final String _tmpInterestsCsv;
            _tmpInterestsCsv = _cursor.getString(_cursorIndexOfInterestsCsv);
            final String _tmpSavedListingIdsCsv;
            _tmpSavedListingIdsCsv = _cursor.getString(_cursorIndexOfSavedListingIdsCsv);
            final String _tmpWatchlistIdsCsv;
            _tmpWatchlistIdsCsv = _cursor.getString(_cursorIndexOfWatchlistIdsCsv);
            _result = new StudentProfileEntity(_tmpId,_tmpStudentId,_tmpFullName,_tmpAvatarUrl,_tmpUniversityEmail,_tmpUniversityName,_tmpDegreeProgram,_tmpAcademicYear,_tmpCampusResidence,_tmpPhoneNumber,_tmpBio,_tmpConsistencyStreakDays,_tmpBehavioralTrustScore,_tmpCampusMeritPoints,_tmpPreferredRendezvousPoint,_tmpPersonaRole,_tmpStatusNote,_tmpStatusNoteEmoji,_tmpStatusNoteExpiry,_tmpStorageUsedBytes,_tmpStorageQuotaBytes,_tmpNotificationsEnabled,_tmpEscrowAlertsEnabled,_tmpQuestRemindersEnabled,_tmpIsTrustScorePublic,_tmpIsOnlineStatusVisible,_tmpActivityLedgerVisibility,_tmpLocationPrivacyScope,_tmpWalletBalance,_tmpVaultProtectedBalance,_tmpOnboardingPreferencesJson,_tmpInterestsCsv,_tmpSavedListingIdsCsv,_tmpWatchlistIdsCsv);
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
  public Object getProfileByEmail(final String email,
      final Continuation<? super StudentProfileEntity> $completion) {
    final String _sql = "SELECT * FROM campusprofile WHERE LOWER(universityEmail) = LOWER(?) LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, email);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<StudentProfileEntity>() {
      @Override
      @Nullable
      public StudentProfileEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStudentId = CursorUtil.getColumnIndexOrThrow(_cursor, "studentId");
          final int _cursorIndexOfFullName = CursorUtil.getColumnIndexOrThrow(_cursor, "fullName");
          final int _cursorIndexOfAvatarUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "avatarUrl");
          final int _cursorIndexOfUniversityEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "universityEmail");
          final int _cursorIndexOfUniversityName = CursorUtil.getColumnIndexOrThrow(_cursor, "universityName");
          final int _cursorIndexOfDegreeProgram = CursorUtil.getColumnIndexOrThrow(_cursor, "degreeProgram");
          final int _cursorIndexOfAcademicYear = CursorUtil.getColumnIndexOrThrow(_cursor, "academicYear");
          final int _cursorIndexOfCampusResidence = CursorUtil.getColumnIndexOrThrow(_cursor, "campusResidence");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfBio = CursorUtil.getColumnIndexOrThrow(_cursor, "bio");
          final int _cursorIndexOfConsistencyStreakDays = CursorUtil.getColumnIndexOrThrow(_cursor, "consistencyStreakDays");
          final int _cursorIndexOfBehavioralTrustScore = CursorUtil.getColumnIndexOrThrow(_cursor, "behavioralTrustScore");
          final int _cursorIndexOfCampusMeritPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "campusMeritPoints");
          final int _cursorIndexOfPreferredRendezvousPoint = CursorUtil.getColumnIndexOrThrow(_cursor, "preferredRendezvousPoint");
          final int _cursorIndexOfPersonaRole = CursorUtil.getColumnIndexOrThrow(_cursor, "personaRole");
          final int _cursorIndexOfStatusNote = CursorUtil.getColumnIndexOrThrow(_cursor, "statusNote");
          final int _cursorIndexOfStatusNoteEmoji = CursorUtil.getColumnIndexOrThrow(_cursor, "statusNoteEmoji");
          final int _cursorIndexOfStatusNoteExpiry = CursorUtil.getColumnIndexOrThrow(_cursor, "statusNoteExpiry");
          final int _cursorIndexOfStorageUsedBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "storageUsedBytes");
          final int _cursorIndexOfStorageQuotaBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "storageQuotaBytes");
          final int _cursorIndexOfNotificationsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "notificationsEnabled");
          final int _cursorIndexOfEscrowAlertsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "escrowAlertsEnabled");
          final int _cursorIndexOfQuestRemindersEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "questRemindersEnabled");
          final int _cursorIndexOfIsTrustScorePublic = CursorUtil.getColumnIndexOrThrow(_cursor, "isTrustScorePublic");
          final int _cursorIndexOfIsOnlineStatusVisible = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnlineStatusVisible");
          final int _cursorIndexOfActivityLedgerVisibility = CursorUtil.getColumnIndexOrThrow(_cursor, "activityLedgerVisibility");
          final int _cursorIndexOfLocationPrivacyScope = CursorUtil.getColumnIndexOrThrow(_cursor, "locationPrivacyScope");
          final int _cursorIndexOfWalletBalance = CursorUtil.getColumnIndexOrThrow(_cursor, "walletBalance");
          final int _cursorIndexOfVaultProtectedBalance = CursorUtil.getColumnIndexOrThrow(_cursor, "vaultProtectedBalance");
          final int _cursorIndexOfOnboardingPreferencesJson = CursorUtil.getColumnIndexOrThrow(_cursor, "onboardingPreferencesJson");
          final int _cursorIndexOfInterestsCsv = CursorUtil.getColumnIndexOrThrow(_cursor, "interestsCsv");
          final int _cursorIndexOfSavedListingIdsCsv = CursorUtil.getColumnIndexOrThrow(_cursor, "savedListingIdsCsv");
          final int _cursorIndexOfWatchlistIdsCsv = CursorUtil.getColumnIndexOrThrow(_cursor, "watchlistIdsCsv");
          final StudentProfileEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpStudentId;
            _tmpStudentId = _cursor.getString(_cursorIndexOfStudentId);
            final String _tmpFullName;
            _tmpFullName = _cursor.getString(_cursorIndexOfFullName);
            final String _tmpAvatarUrl;
            if (_cursor.isNull(_cursorIndexOfAvatarUrl)) {
              _tmpAvatarUrl = null;
            } else {
              _tmpAvatarUrl = _cursor.getString(_cursorIndexOfAvatarUrl);
            }
            final String _tmpUniversityEmail;
            _tmpUniversityEmail = _cursor.getString(_cursorIndexOfUniversityEmail);
            final String _tmpUniversityName;
            _tmpUniversityName = _cursor.getString(_cursorIndexOfUniversityName);
            final String _tmpDegreeProgram;
            _tmpDegreeProgram = _cursor.getString(_cursorIndexOfDegreeProgram);
            final String _tmpAcademicYear;
            _tmpAcademicYear = _cursor.getString(_cursorIndexOfAcademicYear);
            final String _tmpCampusResidence;
            _tmpCampusResidence = _cursor.getString(_cursorIndexOfCampusResidence);
            final String _tmpPhoneNumber;
            _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            final String _tmpBio;
            _tmpBio = _cursor.getString(_cursorIndexOfBio);
            final int _tmpConsistencyStreakDays;
            _tmpConsistencyStreakDays = _cursor.getInt(_cursorIndexOfConsistencyStreakDays);
            final int _tmpBehavioralTrustScore;
            _tmpBehavioralTrustScore = _cursor.getInt(_cursorIndexOfBehavioralTrustScore);
            final int _tmpCampusMeritPoints;
            _tmpCampusMeritPoints = _cursor.getInt(_cursorIndexOfCampusMeritPoints);
            final String _tmpPreferredRendezvousPoint;
            _tmpPreferredRendezvousPoint = _cursor.getString(_cursorIndexOfPreferredRendezvousPoint);
            final String _tmpPersonaRole;
            _tmpPersonaRole = _cursor.getString(_cursorIndexOfPersonaRole);
            final String _tmpStatusNote;
            if (_cursor.isNull(_cursorIndexOfStatusNote)) {
              _tmpStatusNote = null;
            } else {
              _tmpStatusNote = _cursor.getString(_cursorIndexOfStatusNote);
            }
            final String _tmpStatusNoteEmoji;
            if (_cursor.isNull(_cursorIndexOfStatusNoteEmoji)) {
              _tmpStatusNoteEmoji = null;
            } else {
              _tmpStatusNoteEmoji = _cursor.getString(_cursorIndexOfStatusNoteEmoji);
            }
            final String _tmpStatusNoteExpiry;
            if (_cursor.isNull(_cursorIndexOfStatusNoteExpiry)) {
              _tmpStatusNoteExpiry = null;
            } else {
              _tmpStatusNoteExpiry = _cursor.getString(_cursorIndexOfStatusNoteExpiry);
            }
            final long _tmpStorageUsedBytes;
            _tmpStorageUsedBytes = _cursor.getLong(_cursorIndexOfStorageUsedBytes);
            final long _tmpStorageQuotaBytes;
            _tmpStorageQuotaBytes = _cursor.getLong(_cursorIndexOfStorageQuotaBytes);
            final boolean _tmpNotificationsEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfNotificationsEnabled);
            _tmpNotificationsEnabled = _tmp != 0;
            final boolean _tmpEscrowAlertsEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfEscrowAlertsEnabled);
            _tmpEscrowAlertsEnabled = _tmp_1 != 0;
            final boolean _tmpQuestRemindersEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfQuestRemindersEnabled);
            _tmpQuestRemindersEnabled = _tmp_2 != 0;
            final boolean _tmpIsTrustScorePublic;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsTrustScorePublic);
            _tmpIsTrustScorePublic = _tmp_3 != 0;
            final boolean _tmpIsOnlineStatusVisible;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfIsOnlineStatusVisible);
            _tmpIsOnlineStatusVisible = _tmp_4 != 0;
            final String _tmpActivityLedgerVisibility;
            _tmpActivityLedgerVisibility = _cursor.getString(_cursorIndexOfActivityLedgerVisibility);
            final String _tmpLocationPrivacyScope;
            _tmpLocationPrivacyScope = _cursor.getString(_cursorIndexOfLocationPrivacyScope);
            final double _tmpWalletBalance;
            _tmpWalletBalance = _cursor.getDouble(_cursorIndexOfWalletBalance);
            final double _tmpVaultProtectedBalance;
            _tmpVaultProtectedBalance = _cursor.getDouble(_cursorIndexOfVaultProtectedBalance);
            final String _tmpOnboardingPreferencesJson;
            _tmpOnboardingPreferencesJson = _cursor.getString(_cursorIndexOfOnboardingPreferencesJson);
            final String _tmpInterestsCsv;
            _tmpInterestsCsv = _cursor.getString(_cursorIndexOfInterestsCsv);
            final String _tmpSavedListingIdsCsv;
            _tmpSavedListingIdsCsv = _cursor.getString(_cursorIndexOfSavedListingIdsCsv);
            final String _tmpWatchlistIdsCsv;
            _tmpWatchlistIdsCsv = _cursor.getString(_cursorIndexOfWatchlistIdsCsv);
            _result = new StudentProfileEntity(_tmpId,_tmpStudentId,_tmpFullName,_tmpAvatarUrl,_tmpUniversityEmail,_tmpUniversityName,_tmpDegreeProgram,_tmpAcademicYear,_tmpCampusResidence,_tmpPhoneNumber,_tmpBio,_tmpConsistencyStreakDays,_tmpBehavioralTrustScore,_tmpCampusMeritPoints,_tmpPreferredRendezvousPoint,_tmpPersonaRole,_tmpStatusNote,_tmpStatusNoteEmoji,_tmpStatusNoteExpiry,_tmpStorageUsedBytes,_tmpStorageQuotaBytes,_tmpNotificationsEnabled,_tmpEscrowAlertsEnabled,_tmpQuestRemindersEnabled,_tmpIsTrustScorePublic,_tmpIsOnlineStatusVisible,_tmpActivityLedgerVisibility,_tmpLocationPrivacyScope,_tmpWalletBalance,_tmpVaultProtectedBalance,_tmpOnboardingPreferencesJson,_tmpInterestsCsv,_tmpSavedListingIdsCsv,_tmpWatchlistIdsCsv);
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
