package com.tenzenway.tcm;

public class User {
	public long id;
	public String username;
	public long birthday;

	public String toString() {
		return "User id: " + id 
			+ ", username: " + username 
			+ ", birthday: " + birthday;
	}
}
