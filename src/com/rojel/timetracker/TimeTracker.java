package com.rojel.timetracker;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.http.message.BasicNameValuePair;

public class TimeTracker {
	private String username;
	private String password;
	private String url;
	
	public static void main(String[] args) {
		new TimeTracker();
	}
	
	public TimeTracker() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("timetracker"));
			username = br.readLine();
			password = br.readLine();
			url = br.readLine();
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		HttpHelper.post(url + "/start", new BasicNameValuePair("username", username), new BasicNameValuePair("password", password));
		new Thread(new TimeTrackerRunnable(this)).start();
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getUrl() {
		return url;
	}
}
