package com.rojel.timetracker;

import java.io.IOException;

import javax.swing.JOptionPane;

import org.apache.http.message.BasicNameValuePair;


public class TimeTrackerRunnable implements Runnable {
	private TimeTracker tracker;
	private boolean warned;
	
	public TimeTrackerRunnable(TimeTracker tracker) {
		this.tracker = tracker;
	}
	
	@Override
	public void run() {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				HttpHelper.post(tracker.getUrl() + "/stop", new BasicNameValuePair("username", tracker.getUsername()), new BasicNameValuePair("password", tracker.getPassword()));
			}
		}));
		
		while (true) {
			String response = HttpHelper.post(tracker.getUrl() + "/time", new BasicNameValuePair("username", tracker.getUsername()), new BasicNameValuePair("password", tracker.getPassword()));
			System.out.println(response);
			String[] split = response.split(" ");
			int hours = Integer.parseInt(split[0]);
			int minutes = Integer.parseInt(split[1]);
			int seconds = Integer.parseInt(split[2]);
			
			if (hours == 0 && minutes < 10) {
				if (!warned) {
					JOptionPane.showMessageDialog(null, "You have less than 10 minutes left.", "Time Tracker", JOptionPane.WARNING_MESSAGE);
					warned = true;
				}
			} else {
				warned = false;
			}
			
			if (hours == 0 && minutes == 0 && seconds == 0)
				try {
					Runtime.getRuntime().exec("shutdown -l");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			
			try {
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
