package com.rojel.timetracker;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.http.message.BasicNameValuePair;

public class TimeTracker {
	private String username;
	private String password;
	private String url;
	
	private Image icon;
	
	private boolean warnedTen;
	private boolean warnedOne;
	private long connectionLossBeginning;
	
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
		
		try {
			icon = ImageIO.read(new File("icon.png"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		warnedTen = false;
		warnedOne = false;
		connectionLossBeginning = 0;
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		start();
	}
	
	public void start() {
		while (true) {
			String response = "";
			try {
				response = HttpHelper.post(url + "/time", new BasicNameValuePair("username", username), new BasicNameValuePair("password", password));
			} catch (IOException e) {
				if (connectionLossBeginning == 0)
					connectionLossBeginning = System.currentTimeMillis();
				if (System.currentTimeMillis() - connectionLossBeginning > 5 * 60000) {
					asyncMessage("Sorry, but we couldn't reach the server for 5 minutes so we have to log off.");
					wait(10);
					logout();
				}
				System.out.println(e.getMessage());
				System.out.println("Can't reach the server, retrying in 10s");
				wait(10);
				continue;
			}
			
			System.out.println(response);
			String[] split = response.split(" ");
			int hours = Integer.parseInt(split[0]);
			int minutes = Integer.parseInt(split[1]);
			int seconds = Integer.parseInt(split[2]);
			
			int totalSeconds = toSeconds(hours, minutes, seconds);
			
			if (totalSeconds <= toSeconds(0, 10, 0)) {
				if (!warnedTen) {
					asyncMessage("You have less than 10 minutes left.");
					warnedTen = true;
				}
			} else
				warnedTen = false;
			
			if (totalSeconds <= toSeconds(0, 1, 0)) {
				if (!warnedOne) {
					asyncMessage("You will be logged off in 1 minute.");
					warnedOne = true;
				}
			} else
				warnedOne = false;
			
			if (totalSeconds == 0)
				logout();
			
			connectionLossBeginning = 0;
			wait(30);
		}
	}
	
	public int toSeconds(int hours, int minutes, int seconds) {
		return seconds + minutes * 60 + hours * 60 * 60;
	}
	
	public void wait(int seconds) {
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void logout() {
		try {
			Runtime.getRuntime().exec("shutdown -l");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	public void asyncMessage(final String text) {
		new Thread(new Runnable() {
			public void run() {
				Dimension monitorSize = Toolkit.getDefaultToolkit().getScreenSize();
				
				JFrame frame = new JFrame("Time Tracker");
				frame.setSize(monitorSize);
				frame.setUndecorated(true);
				frame.setOpacity(0.01f);
				frame.setAlwaysOnTop(true);
				frame.setIconImage(icon);
				frame.setVisible(true);
				JOptionPane.showMessageDialog(frame, text, "Time Tracker", JOptionPane.WARNING_MESSAGE);
				frame.dispose();
			}
		}).start();
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
