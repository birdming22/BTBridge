package com.tenzenway.tcm.test;

import java.util.Calendar;
import java.util.List;

import android.test.AndroidTestCase;
import android.util.Log;

import com.tenzenway.tcm.*;

public class TestDbAdapter extends AndroidTestCase {
	private DbAdapter dbAdapter;

	protected void setUp() throws Exception {
		super.setUp();

		dbAdapter = new DbAdapter(getContext());
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		
		dbAdapter.delUserAll();
		dbAdapter.truncateUser();
		dbAdapter.closeDb();
	}

	public void testAddUser(int i) {
		// get before add
		String username = "user" + i;
		User user;
		
		user = dbAdapter.fetchUserByName(username);
		assertNull(user);

		int userId = dbAdapter.addUser(username, 1977 + i, 7, 7);
		Log.d("TestDbAdapter", "userId: " + userId); 
		assertTrue(userId > 0);

		user = dbAdapter.fetchUserByName(username);
		Log.d("TestDbAdapter", user.toString());
		assertTrue(userId == user.id);
	}
	
	public void testDelUser(int i) {
		String username = "user" + i;
		boolean result = dbAdapter.delUserByName(username);
		assertTrue(result);
	}
	
	public void testAddOneUser() {
		testAddUser(1);
		testDelUser(1);
	}
	
	public void testAddSomeUser() {
		for(int i=0; i<10; i++) {
			testAddUser(i);
			testDelUser(i);
		}
	}

	public void testFetchUserAll() {
		for(int i=0; i< 10; i++) {
			testAddUser(i);
		}
		
		Log.d("TestDbAdapter", "testFetchUserAll");
		List<User> users = dbAdapter.fetchUserAll();
		Log.d("TestDbAdapter", "total users: " + users.size());

		for (int i = 0; i < users.size(); i++) {
			User user = users.get(i);
			Log.d("TestDbAdapter", user.toString());
		}
		
		for(int i=0; i< 10; i++) {
			testDelUser(i);
		}
	}
	
	public void testDateToLong() {
		for(int i=0; i<10; i++) {
			int year = 1977 + i;
			int month = 7;
			int day = 7;
			long birthday = dbAdapter.dateToLong(year, month, day);
			Calendar cal = Calendar.getInstance();
			cal.set(year, month, day);
			cal.clear(Calendar.MILLISECOND);
			assertTrue(birthday == cal.getTimeInMillis());
		}
	}

	public void testAddRecord() {
		for(int i=0; i<10;i++) {
			String username = "recUser" + i;
			int userId = dbAdapter.addUser(username, 1977 + i, 7, 7);
			assertTrue(userId > 0);
			
			int recId;
			recId = dbAdapter.addRecord(userId, Constant.LEFT_UP);
			assertTrue(recId > 0);
			dbAdapter.addRecord(userId, Constant.LEFT_MIDDLE);
			assertTrue(recId > 0);
			dbAdapter.addRecord(userId, Constant.LEFT_DOWN);
			assertTrue(recId > 0);
			recId = dbAdapter.addRecord(userId, Constant.RIGHT_UP);
			assertTrue(recId > 0);
			dbAdapter.addRecord(userId, Constant.RIGHT_MIDDLE);
			assertTrue(recId > 0);
			dbAdapter.addRecord(userId, Constant.RIGHT_DOWN);
			assertTrue(recId > 0);
		}
	}
	
	public void testAddData() {
		String username = "dataUser";
		int userId = dbAdapter.addUser(username, 1977, 7, 7);
		assertTrue(userId > 0);
		
		int recId;
		recId = dbAdapter.addRecord(userId, Constant.LEFT_UP);
		assertTrue(recId > 0);
		
		for(int i=0; i<1024; i++) {
			int dataId;
			dataId = dbAdapter.addData(recId, i);
			assertTrue(dataId > 0);
		}
	}
}
