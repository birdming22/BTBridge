package com.tenzenway.tcm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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
	private boolean isBeginTransaction = false;

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

		private void _backupOldDb(int oldVersion) {
			final String IN_FILE = Constant.DB_PATH + Constant.DB_NAME;
			final String OUT_FILE = Constant.DB_PATH + Constant.DB_NAME + "." + Integer.toString(oldVersion) + ".bak";
			if(new File(Constant.DB_PATH + Constant.DB_NAME).exists()) {
				File f = new File(Constant.DB_PATH);
				if (!f.exists()) {
					f.mkdir();
				}
				try {
					InputStream is = new FileInputStream(IN_FILE);
					OutputStream os = new FileOutputStream(OUT_FILE);

					byte[] buffer = new byte[1024];
					int length;
					while ((length = is.read(buffer)) > 0) {
						os.write(buffer, 0, length);
					}
					os.flush();
					os.close();
					is.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
			_backupOldDb(oldVersion);
            db.execSQL("DROP TABLE IF EXISTS user");
            db.execSQL("DROP TABLE IF EXISTS record");
            db.execSQL("DROP TABLE IF EXISTS data");
            onCreate(db);
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
			user.id = userId;
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
	
	public int addRecord(long userId, int position) {
		ContentValues initialValues = new ContentValues();
		initialValues.put("userId", userId);
		initialValues.put("position", position);
		initialValues.put("recTime", _getCurrentTime());
		return (int) db.insert("record", null, initialValues);
	}
	
	public int addData(long recId, int value) {
		ContentValues initialValues = new ContentValues();
		initialValues.put("recId", recId);
		initialValues.put("value", value);
		return (int) db.insert("data", null, initialValues);
	}
	
	public void beginTransaction() {
		db.beginTransaction();
		isBeginTransaction = true;
	}
	
	public void endTransaction() {
		if (isBeginTransaction) {
			db.setTransactionSuccessful();
			db.endTransaction();
			isBeginTransaction = false;
		}
	}

	public void exportDb() {
		final String IN_FILE = Constant.DB_PATH + Constant.DB_NAME;
		final String OUT_FILE = Constant.DROPBPX_PATH + Constant.DB_NAME;
		if(new File(Constant.DB_PATH + Constant.DB_NAME).exists()) {
			File f = new File(Constant.DROPBPX_PATH);
			if (!f.exists()) {
				f.mkdirs();
			}
			try {
				InputStream is = new FileInputStream(IN_FILE);
				OutputStream os = new FileOutputStream(OUT_FILE);

				byte[] buffer = new byte[1024];
				int length;
				while ((length = is.read(buffer)) > 0) {
					os.write(buffer, 0, length);
				}
				os.flush();
				os.close();
				is.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
