/**
 *  Name: StatsManager.java
 *  Date: 14:53:19 - 12 sep 2012
 * 
 *  Author: LucasEmanuel @ bukkit forums
 *  
 *  
 *  Copyright 2013 Lucas Arnstr�m
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 *  
 *
 *
 *  Filedescription:
 *  
 *  Used by the main-thread to initiate sub-threads that modify the data
 *  in the database.
 * 
 */

package me.lucasemanuel.survivalgamesmultiverse.managers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import me.lucasemanuel.survivalgamesmultiverse.Main;
import me.lucasemanuel.survivalgamesmultiverse.threading.ConcurrentConnection;
import me.lucasemanuel.survivalgamesmultiverse.utils.ConsoleLogger;

public class StatsManager {
	
	private ConsoleLogger logger;
	
	private final String username;
	private final String password;
	private final String host;
	private final int    port;
	private final String database;
	private final String tablename;
	
	private ConcurrentConnection insertobject = null;
	
	public StatsManager(Main instance) {
		
		logger = new ConsoleLogger(instance, "StatsManager");
		logger.debug("Loading settings");
		
		username  = instance.getConfig().getString("database.auth.username");
		password  = instance.getConfig().getString("database.auth.password");
		host      = instance.getConfig().getString("database.settings.host");
		port      = instance.getConfig().getInt   ("database.settings.port");
		database  = instance.getConfig().getString("database.settings.database");
		tablename = instance.getConfig().getString("database.settings.tablename");
		
		if(instance.getConfig().getBoolean("database.enabled")) {
			
			logger.info("Testing connection to database, please wait!");
			
			Connection con = null;
			
			try {
				Class.forName("com.mysql.jdbc.Driver");
				
				String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
				
				con = DriverManager.getConnection(url, username, password);
			}
			catch(SQLException | ClassNotFoundException e) {
				logger.severe("Error while testing connection! Message: " + e.getMessage());
			}
			
			if(con != null) {
				
				logger.debug("Initiated");
				logger.info("Connected!");
				
				insertobject = new ConcurrentConnection(username, password, host, port, database, tablename);
				
				try {
					con.close();
				}
				catch (SQLException e) {
					logger.severe("Error while closing test connection! Message: " + e.getMessage());
				}
			}
			else
				logger.severe("No connection to database! Stats will not be saved!");
		}
		else {
			logger.info("Database logging disabled! No stats will be saved.");
		}
	}
	
	public void addWinPoints(final String playername, final int points) {
		if(insertobject != null) {
			
			Thread thread = new Thread() {
				public void run() {
					insertobject.update(playername, points, 0, 0);
				}
			};
			
			thread.start();
		}
	}
	
	public void addKillPoints(final String playername, final int points) {
		if(insertobject != null) {
			
			Thread thread = new Thread() {
				public void run() {
					insertobject.update(playername, 0, points, 0);
				}
			};
			
			thread.start();
		}
	}
	
	public void addDeathPoints(final String playername, final int points) {
		if(insertobject != null) {
			
			Thread thread = new Thread() {
				public void run() {
					insertobject.update(playername, 0, 0, points);
				}
			};
			
			thread.start();
		}
	}
}

class Score {
	
	private int deaths;
	private int kills;
	private double kdr;
	
	public Score() {
		this.kills  = 0;
		this.deaths = 0;
		this.kdr    = 0.0;
	}
	
	private void calculateKDR() {
		if(this.deaths > 0) {
			this.kdr = this.kills / this.deaths;
		}
		else {
			this.kdr = this.kills;
		}
	}
	
	public void addKills(int amount) {
		this.kills += amount;
		calculateKDR();
	}
	
	public void addDeaths(int amount) {
		this.deaths += amount;
		calculateKDR();
	}
	
	public int getKills() {
		return this.kills;
	}
	
	public int getDeaths() {
		return this.deaths;
	}
	
	public double getKDR() {
		return this.kdr;
	}
}