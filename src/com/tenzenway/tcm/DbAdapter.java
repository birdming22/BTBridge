package com.tenzenway.tcm;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.Calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbAdapter {
	// Debugging
	private static final String TAG = "DB_ADAPTER";
	private SQLiteDatabase db;

	private static class SQLiteHelper extends SQLiteOpenHelper {
		private static final String sqlCreateUser = "create table user (id integer primary key autoincrement, "
				+ "username text not null, " + "birthday long DEFAULT 0);";
		private static final String sqlCreateTransaction = "create table record (id integer primary key autoincrement, "
				+ "userId integer, " + "position integer, " + "recTime long DEFAULT 0);";
		private static final String sqlCreateData = "create table data (id integer primary key autoincrement, "
				+ "recId integer, " + "value integer);";

		public SQLiteHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d("TestDbAdapter", "onCreate");
			db.execSQL(sqlCreateUser);
			db.execSQL(sqlCreateTransaction);
			db.execSQL(sqlCreateData);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub

		}

	}

	public DbAdapter(Context context) {
		SQLiteHelper helper = new SQLiteHelper(context, Constant.DB_NAME, null,
				Constant.DB_VERSION);
		db = helper.getWritableDatabase();
	}

	public void closeDb() {
		db.close();
	}

	private long _dateToLong(int year, int month, int day) {
		Calendar cal = Calendar.getInstance();
		String defaultTimeZone = "GMT+8:00";
		cal.setTimeZone(TimeZone.getTimeZone(defaultTimeZone));
		cal.set(year, month, day);
		cal.clear(Calendar.MILLISECOND);
		return cal.getTimeInMillis();
	}
	
	public long dateToLong(int year, int month, int day) {
		// for testing
		return _dateToLong(year, month, day);
	}
	
	public int addUser(String username, int year, int month, int day) {
		ContentValues initialValues = new ContentValues();
		initialValues.put("username", username);
		initialValues.put("birthday", _dateToLong(year, month, day));
		return (int) db.insert("user", null, initialValues);
	}

	private Cursor _fetchUserByName(String username) {
		return db.query("user", new String [] {"id", "username", "birthday"},
				"username = '" + username + "'",
				null, null, null, null, "1");
	}

	public User fetchUserByName(String username) {
		User user = new User();
		
		Cursor cursor = _fetchUserByName(username);
		if (cursor.getCount() == 1) {
			cursor.moveToFirst();
			user.id = cursor.getLong(cursor.getColumnIndex("id"));
			user.username = cursor.getString(cursor.getColumnIndex("username"));
			user.birthday = cursor.getLong(cursor.getColumnIndex("birthday"));
		} else {
			user = null;
		}
		cursor.close();

		return user;
	}

	private Cursor _fetchUserById(long userId) {
		return db.query("user", new String[] { "id", "username", "birthday" },
				"id = " + userId, null, null, null, null);
	}

	public User fetchUserById(long userId) {
		User user = new User();
		
		Cursor cursor = _fetchUserById(userId);
		if (cursor != null) {
			cursor.moveToFirst();
			user.username = cursor.getString(cursor.getColumnIndex("username"));
			user.birthday = cursor.getLong(cursor.getColumnIndex("birthday"));
		} else {
			user = null;
		}
		cursor.close();

		return user;
	}
	
	private Cursor _fetchUserAll() {
		return db.query("user", null, null, null, null,  
                null, "id");  
 	}
	
	public List<User> fetchUserAll() {
        List<User> users = new ArrayList<User>();

        Cursor cursor = _fetchUserAll();
        if (cursor == null)  
        {  
            return null;  
        }

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            User user = new User();
            user.id = cursor.getLong(cursor.getColumnIndex("id"));
        	user.username = cursor.getString(cursor.getColumnIndex("username"));
			user.birthday = cursor.getLong(cursor.getColumnIndex("birthday"));
			users.add(user);

			cursor.moveToNext();
        }
        cursor.close();
        
		return users;  
	}

	private int _delUserByName(String username) {
		return db.delete("user", "username=?", new String[] {username});
	}

	public boolean delUserByName(String username) {
		int result = _delUserByName(username);
		if (result != 0) {
			return true;
		}
		return false;
	}
	
	public int delUserAll() {
		return db.delete("user", "1", null);
	}

	public void truncateUser() {
		db.execSQL("delete from user;");
		db.execSQL("delete from sqlite_sequence where name='user';");
	}
	
	private long _getCurrentTime() {
		Calendar cal = Calendar.getInstance();
		String defaultTimeZone = "GMT+8:00";
		cal.setTimeZone(TimeZone.getTimeZone(defaultTimeZone));
		cal.clear(Calendar.MILLISECOND);
		return cal.getTimeInMillis();
	}
	
	public int addRecord(int userId, int position) {
		ContentValues initialValues = new ContentValues();
		initialValues.put("userId", userId);
		initialValues.put("position", position);
		initialValues.put("recTime", _getCurrentTime());
		return (int) db.insert("record", null, initialValues);
	}
	
	public int addData(int recId, int value) {
		ContentValues initialValues = new ContentValues();
		initialValues.put("recId", recId);
		initialValues.put("value", value);
		return (int) db.insert("data", null, initialValues);
	}
}
