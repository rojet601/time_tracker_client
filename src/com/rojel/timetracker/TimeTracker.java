package com.rojel.timetracker;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
	
	private Image icon32;
	private Image icon16;
	
	private boolean warnedTen;
	private boolean warnedOne;
	private long connectionLossBeginning;
	private boolean paused;
	
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
			icon32 = ImageIO.read(new File("icon.png"));
			icon16 = ImageIO.read(new File("icon_tray.png"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		warnedTen = false;
		warnedOne = false;
		paused = false;
		connectionLossBeginning = 0;
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		TrayIcon trayIcon = new TrayIcon(icon16);
		
		PopupMenu menu = new PopupMenu();
		MenuItem pauseItem = new MenuItem("Pause");
		menu.add(pauseItem);
		trayIcon.setPopupMenu(menu);
		
		menu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pause();
			}
		});
		
		try {
			SystemTray.getSystemTray().add(trayIcon);
		} catch (AWTException e) {
			e.printStackTrace();
		}
		
		start();
	}
	
	public void start() {
		while (true) {
			while (paused)
				wait(5);
			
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
	
	public void pause() {
		List<JFrame> frames = new ArrayList<JFrame>();
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		
		for (GraphicsDevice gd : ge.getScreenDevices()) {
			Rectangle rect = gd.getDefaultConfiguration().getBounds();
			JFrame frame = new JFrame("Time Tracker");
			frame.setBounds(rect);
			frame.setUndecorated(true);
			frame.getContentPane().setBackground(Color.BLACK);
			frame.setIconImage(icon32);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
			frames.add(frame);
		}
		
		try {
			Runtime.getRuntime().exec("taskkill /F /IM explorer.exe").waitFor();
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
		
		paused = true;
		System.out.println("Paused");
		JOptionPane.showMessageDialog(frames.get(frames.size() - 1), "Paused. Press OK to continue.", "Time Tracker", JOptionPane.WARNING_MESSAGE);
		paused = false;
		System.out.println("Unpaused");
		
		try {
			Runtime.getRuntime().exec("explorer.exe");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for (JFrame frame : frames)
			frame.dispose();
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
	
	public void blockingMessage(String text) {
		Dimension monitorSize = Toolkit.getDefaultToolkit().getScreenSize();
		
		JFrame frame = new JFrame("Time Tracker");
		frame.setSize(monitorSize);
		frame.setUndecorated(true);
		frame.setOpacity(0.01f);
		frame.setAlwaysOnTop(true);
		frame.setIconImage(icon32);
		frame.setVisible(true);
		JOptionPane.showMessageDialog(frame, text, "Time Tracker", JOptionPane.WARNING_MESSAGE);
		frame.dispose();
	}
	
	public void asyncMessage(final String text) {
		new Thread(new Runnable() {
			public void run() {
				blockingMessage(text);
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
