/**
 *  Name: StatusManager.java
 *  Date: 23:58:26 - 15 sep 2012
 * 
 *  Author: LucasEmanuel @ bukkit forums
 *  
 *  
 *  Description:
 *  
 *  
 *  
 * 
 * 
 */

package me.lucasemanuel.survivalgamesmultiverse.managers;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import me.lucasemanuel.survivalgamesmultiverse.Main;
import me.lucasemanuel.survivalgamesmultiverse.utils.ConsoleLogger;

public class StatusManager {
	
	private final Main plugin;
	private ConsoleLogger logger;
	
	// Key = worldname, Value flags :: 0 = waiting, 1 = started, 2 = frozen
	private HashMap<String, Integer> worlds_status_flags;
	
	// Key = Worldname , Value = TaskID || There should only be one task per world
	private HashMap<String, Integer> worlds_tasks;
	
	public StatusManager(Main instance) {
		plugin = instance;
		logger = new ConsoleLogger(instance, "StatusManager");
		
		worlds_status_flags = new HashMap<String, Integer>();
		worlds_tasks  = new HashMap<String, Integer>();
		
		logger.debug("Initiated");
	}
	
	public synchronized void addWorld(String worldname) {
		worlds_status_flags.put(worldname, 0);
		worlds_tasks.put(worldname, -1); // -1 means no task
	}

	private synchronized boolean setStatusFlag(String worldname, int value) {
		if(worlds_status_flags.containsKey(worldname)) {
			worlds_status_flags.put(worldname, value);
			return true;
		}
		else
			return false;
	}
	
	public synchronized int getStatusFlag(String worldname) {
		return worlds_status_flags.get(worldname);
	}

	public synchronized void startCountDown(String worldname) {
		
		if(worlds_status_flags.containsKey(worldname) && worlds_tasks.get(worldname) == -1) {
			
			final CountDown info = new CountDown(worldname);
			
			info.setTaskID(plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable(){
				public void run() {
					countDown(info);
				}
			}, 20L, 200L));
			
			worlds_tasks.put(worldname, info.getTaskID());
			
			logger.debug("Started task for startCountDown in world: " + worldname + " :: taskID - " + info.getTaskID());
		}
	}
	
	public synchronized void startPlayerCheck(String worldname) {
		
		if(worlds_status_flags.containsKey(worldname) && worlds_tasks.get(worldname) == -1) {
			
			final GeneralTaskInfo info = new GeneralTaskInfo(worldname);
			
			info.setTaskID(plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
				public void run() {
					playerCheck(info);
				}
			}, 20L, 200L));
			
			worlds_tasks.put(worldname, info.getTaskID());
			
			logger.debug("Started task for startPlayerCheck in world: " + worldname + " :: taskID - " + info.getTaskID());
		}
	}
	
	private synchronized void playerCheck(GeneralTaskInfo info) {
		
		String worldname = info.getWorldname();
		int taskID = info.getTaskID();
		
		int playeramount = plugin.getPlayerManager().getPlayerAmount(worldname);
		
		logger.debug("PlayerCheck() called for world: " + worldname + " :: taskID - " + taskID + " :: Playeramount - " + playeramount);
		
		if(playeramount >= 2) {
			plugin.getServer().getScheduler().cancelTask(taskID);
			worlds_tasks.put(worldname, -1);
			startCountDown(worldname);
		}
		else if(playeramount == 0) {
			logger.debug("Cancelling task: " + taskID);
			plugin.getServer().getScheduler().cancelTask(taskID);
			worlds_tasks.put(worldname, -1);
		}
		else
			plugin.getWorldManager().broadcast(Bukkit.getWorld(worldname), ChatColor.LIGHT_PURPLE + plugin.getLanguageManager().getString("waitingForPlayers"));
	}
	
	private synchronized void countDown(final CountDown info) {
		
		String worldname = info.getWorldname();
		long timeOfInitiation = info.getStartTime();
		
		int taskID = info.getTaskID();
		
		logger.debug("CountDown() called for world: " + worldname + " :: TaskID - " + taskID);
		
		int timeToWait = plugin.getConfig().getInt("timeoutTillStart");
		
		int timepassed = (int) ((System.currentTimeMillis() - timeOfInitiation) / 1000);
		
		if(timepassed >= (timeToWait - 12) || plugin.getPlayerManager().getPlayerAmount(worldname) >= 20) {
			
			if(timepassed >= timeToWait && info.getStarted10() == true) {
				activate(worldname);
			}
			
			else if(info.getStarted10() == false) {
				
				logger.debug("Starting 1s countdown for world: " + worldname);
				
				plugin.getServer().getScheduler().cancelTask(taskID);
				worlds_tasks.put(worldname, -1);
				
				info.setStarted10();
				
				info.setTaskID(plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
					public void run() {
						countDown(info);
					}
				}, 20L, 20L));
				
				worlds_tasks.put(worldname, info.getTaskID());
			}
		}
		
		if((timeToWait - timepassed) > 0)
			plugin.getWorldManager().broadcast(Bukkit.getWorld(worldname), (timeToWait - timepassed) + " " + plugin.getLanguageManager().getString("timeleft"));
	}

	public synchronized boolean activate(String worldname) {
		
		if(worlds_status_flags.containsKey(worldname)) {
			setStatusFlag(worldname, 1);
			
			plugin.getWorldManager().broadcast(Bukkit.getWorld(worldname), ChatColor.GOLD + plugin.getLanguageManager().getString("gamestarted"));
			
			plugin.getSignManager().updateSigns();
			
			if(worlds_tasks.get(worldname) != -1) {
				plugin.getServer().getScheduler().cancelTask(worlds_tasks.get(worldname));
				worlds_tasks.put(worldname, -1);
			}
			
			startArenaCountdown(worldname);
			
			return true;
		}
		else
			return false;
	}

	private synchronized void startArenaCountdown(final String worldname) {
		
		logger.debug("Starting arena countdown for world: " + worldname);
		
		if(worlds_tasks.get(worldname) != -1) {
			plugin.getServer().getScheduler().cancelTask(worlds_tasks.get(worldname));
			worlds_tasks.put(worldname, -1);
		}
		
		final CountDown info = new CountDown(worldname);
		
		info.setTaskID(plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				
				// Broadcast that players will be transported to the arena in 5 seconds
				plugin.getWorldManager().broadcast(Bukkit.getWorld(worldname), plugin.getLanguageManager().getString("broadcast_before_arena"));
				
				// Schedule the teleport with 100 ticks delay
				info.setTaskID(plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					public void run() {
						sendEveryoneToArena(info);
					}
				}, 100L));
				
				worlds_tasks.put(worldname, info.getTaskID());
			}
		}, (long) (plugin.getConfig().getInt("timeoutTillArenaInSeconds") * 20)));
		
		worlds_tasks.put(worldname, info.getTaskID());
	}
	
	private synchronized void sendEveryoneToArena(CountDown info) {
		
		int taskID = info.getTaskID();
		
		logger.debug("sendEveryoneToArena() called by task: " + taskID);
		
		// Is the task that called this method registered?
		if(worlds_tasks.get(info.getWorldname()) == taskID) {
			
			plugin.getWorldManager().broadcast(Bukkit.getWorld(info.getWorldname()), ChatColor.LIGHT_PURPLE + plugin.getLanguageManager().getString("sendingEveryoneToArena"));
			
			Player[] playerlist = plugin.getPlayerManager().getPlayerList(info.getWorldname());
			
			for(Player player : playerlist) {
				if(player != null) {
					if(plugin.getLocationManager().tpToArena(player)) {
						player.sendMessage(ChatColor.GOLD + plugin.getLanguageManager().getString("sentYouToArena"));
					}
					else {
						player.setHealth(0);
						player.sendMessage(ChatColor.BLUE + plugin.getLanguageManager().getString("killedSendingArena"));
					}
				}
				else
					plugin.getPlayerManager().removePlayer(info.getWorldname(), player);
			}
			
			worlds_tasks.put(info.getWorldname(), -1);
			
			startEndGameCountdown(info.getWorldname());
		}
	}

	private synchronized void startEndGameCountdown(final String worldname) {
		
		logger.debug("Starting endgame countdown!");
		
		int timeout = plugin.getConfig().getInt("timeoutAfterArena");
		
		// Broadcast time left
		plugin.getWorldManager().broadcast(Bukkit.getWorld(worldname), timeout + " " + plugin.getLanguageManager().getString("secondsTillTheGameEnds"));
		
		// Schedule world reset
		worlds_tasks.put(worldname, plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				plugin.resetWorld(Bukkit.getWorld(worldname));
			}
		}, (long) (timeout * 20)));
	}

	public synchronized void reset(String worldname) {
		
		logger.debug("Resetting world: " + worldname);
		
		if(worlds_tasks.get(worldname) != -1) {
			plugin.getServer().getScheduler().cancelTask(worlds_tasks.get(worldname));
			worlds_tasks.put(worldname, -1);
		}
		
		setStatusFlag(worldname, 0);
	}
}

// Some small objects to keep track of task id's and what worlds they are working with.

class GeneralTaskInfo {
	
	private final String worldname;
	private int taskID = -1;
	
	public GeneralTaskInfo(String worldname) {
		this.worldname = worldname;
	}
	
	public synchronized String getWorldname() {
		return this.worldname;
	}
	
	public synchronized void setTaskID(int newID) {
		this.taskID = newID;
	}
	
	public synchronized int getTaskID() {
		return this.taskID;
	}
}

class CountDown {
	
	private final String worldname;
	private final long timeOfInitiation;
	
	private boolean started10;
	
	private int taskID = -1;
	
	public CountDown(String worldname) {
		this.worldname = worldname;
		this.timeOfInitiation = System.currentTimeMillis();
		
		started10 = false;
	}
	
	public synchronized String getWorldname() {
		return this.worldname;
	}
	
	public synchronized long getStartTime() {
		return this.timeOfInitiation;
	}
	
	public synchronized void setTaskID(int newID) {
		this.taskID = newID;
	}
	
	public synchronized int getTaskID() {
		return this.taskID;
	}
	
	public synchronized void setStarted10() {
		this.started10 = true;
	}
	
	public synchronized boolean getStarted10() {
		return this.started10;
	}
}





//public class StatusManager {
//
//private Main plugin;
//private ConsoleLogger logger;
//
//private HashMap<String, TaskInfo> worlds_taskinfo;
//private HashMap<String, Integer>  worlds_status_flags;
//
//private final int conftime;
//
//public StatusManager(Main instance) {
//	plugin = instance;
//	logger = new ConsoleLogger(instance, "StatusManager");
//	
//	worlds_taskinfo     = new HashMap<String, TaskInfo>();
//	worlds_status_flags = new HashMap<String, Integer>();
//	
//	conftime = plugin.getConfig().getInt("timeoutTillStart");
//	
//	logger.debug("Initiated");
//}
//
///*
// *  -- Basics
// */
//
//public synchronized void addWorld(String worldname) {
//	worlds_status_flags.put(worldname, 0);
//	worlds_taskinfo.put(worldname, null);
//}
//
//public synchronized void resetWorld(String worldname) {
//	
//	logger.debug("Resetting world " + worldname);
//	
//	TaskInfo info = worlds_taskinfo.get(worldname);
//	
//	if(info != null && info.getTaskID() != -1) {
//		cancelTask(info);
//		worlds_taskinfo.put(worldname, null);
//	}
//	
//	setStatusFlag(info.getWorldname(), 0);
//}
//
//public synchronized int getStatusFlag(String worldname) {
//	if(worlds_status_flags.containsKey(worldname)) {
//		return worlds_status_flags.get(worldname);
//	}
//	
//	return -1;
//}
//
//private synchronized boolean setStatusFlag(String worldname, int flag) {
//	if(worlds_status_flags.containsKey(worldname)) {
//		worlds_status_flags.put(worldname, flag);
//		return true;
//	}
//	
//	return false;
//}
//
//private synchronized void checkAndKill(String worldname) {
//	if(worlds_taskinfo.containsKey(worldname) && worlds_taskinfo.get(worldname) != null)
//		cancelTask(worlds_taskinfo.get(worldname));
//}
//
//private synchronized void cancelTask(TaskInfo info) {
//	if(info.getTaskID() != -1) {
//		plugin.getServer().getScheduler().cancelTask(info.getTaskID());
//		info.setTaskID(-1);
//	}
//}
//
//public synchronized boolean activate(String worldname) {
//	if(worlds_status_flags.containsKey(worldname)) {
//		setStatusFlag(worldname, 1);
//		plugin.getWorldManager().broadcast(Bukkit.getWorld(worldname), ChatColor.GOLD + plugin.getLanguageManager().getString("gamestarted"));
//		plugin.getSignManager().updateSigns();
//		startArenaCountdown(worldname);
//		return true;
//	}
//	else
//		return false;
//}
//
///*
// *  -- Schedule startups
// */
//
//public synchronized void startPlayerCheck(String worldname) {
//	
//	// Make sure there aren't any tasks registered for the world and that the world is in waiting status.
//	// This method gets called every time a player joins the match.
//	if(worlds_taskinfo.get(worldname) == null && worlds_status_flags.get(worldname) == 0) {
//		
//		final TaskInfo info = new TaskInfo(worldname);
//		
//		info.setTaskID(plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
//			public void run() {
//				playercheck(info);
//			}
//		}, 20L, 200L));
//		
//		// Register the task for the world so other methods can find it.
//		worlds_taskinfo.put(worldname, info);
//		
//		logger.debug("Initiated playercheck for " + worldname + " taskID: " + info.getTaskID());
//	}
//}
//
//private synchronized void startFirstCountDown(String worldname) {
//	
//	// Just a failsafe to make sure there isn't another countdown called if the game has started.
//	if(worlds_status_flags.get(worldname) == 0) {
//		
//		final TaskInfo info = worlds_taskinfo.get(worldname);
//		
//		// Reset so we get a correct countdown
//		info.resetTimeOfInitiation();
//		
//		// Make sure the pre-existing task is canceled
//		cancelTask(info);
//		
//		// Schedule a repeating call to the firstCountdown() method with 1 s delay and 10 s in between.
//		info.setTaskID(plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
//			public void run() {
//				firstCountdown(info);
//			}
//		}, 20L, 200L));
//		
//		logger.debug("Initiated firstCountdown for world: " + worldname + " ; TaskID: " + info.getTaskID());
//	}
//}
//
//private synchronized void startArenaCountdown(final String worldname) {
//	
//	// Make sure any previous task for this world is canceled.
//	checkAndKill(worldname);
//	
//	final TaskInfo info;
//	
//	if(worlds_taskinfo.get(worldname) != null) {
//		info = worlds_taskinfo.get(worldname);
//	}
//	else {
//		info = new TaskInfo(worldname);
//	}
//	
//	
//}
//
//
///*
// *  -- Tasks
// */
//
//private synchronized void playercheck(TaskInfo info) {
//	
//	String worldname = info.getWorldname();
//	int playeramount = plugin.getPlayerManager().getPlayerAmount(worldname);
//	
//	if(playeramount == 0) {
//		// Since there are no players left in the game, cancel the check.
//		resetWorld(worldname);
//	}
//	else if(playeramount >= 2) {
//		// There are now more then two players, lets start the countdown.
//		cancelTask(info);
//		startFirstCountDown(worldname);
//	}
//	else {
//		// We are still waiting for more players, lets notify the waiting player.
//		plugin.getWorldManager().broadcast(Bukkit.getWorld(worldname), ChatColor.LIGHT_PURPLE + plugin.getLanguageManager().getString("waitingForPlayers"));
//	}
//}
//
//private synchronized void firstCountdown(final TaskInfo info) {
//	
//	int timepassed   = (int) ((System.currentTimeMillis() - info.getTimeOfInitiation()) / 1000);
//	String worldname = info.getWorldname();
//	
//	if(timepassed >= (conftime - 12) || plugin.getPlayerManager().getPlayerAmount(worldname) >= 20) {
//		
//		// If enough time has passed and the 10s countdown has been initiated, activate the game.
//		if(timepassed >= conftime && info.getStarted10() == true) {
//			activate(worldname);
//		}
//		
//		else if(info.getStarted10() == false) {
//			
//			logger.debug("Starting 1s countdown for world: " + worldname);
//			
//			// Cancel the task that calls this method so we can initiate a new one with a shorter repeating delay of 1s.
//			cancelTask(info);
//			
//			// Tell the TaskInfo object that we are starting the 10s countdown.
//			info.setStarted10();
//			
//			info.setTaskID(plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
//				public void run() {
//					firstCountdown(info);
//				}
//			}, 20L, 20L));
//		}
//	}
//	
//	if((conftime - timepassed) > 0)
//		plugin.getWorldManager().broadcast(Bukkit.getWorld(worldname), (conftime - timepassed) + " " + plugin.getLanguageManager().getString("timeleft"));
//}
//}
//
//class TaskInfo {
//
//private final String  worldname;
//private       long    time_of_initiation;
//private       int     taskID;
//private       boolean s10;
//
//public TaskInfo(String worldname) {
//	this.worldname          = worldname;
//	this.taskID             = -1;
//	this.s10                = false;
//	this.time_of_initiation = System.currentTimeMillis();
//}
//
//public synchronized String getWorldname() {
//	return this.worldname;
//}
//
//public synchronized int getTaskID() {
//	return this.taskID;
//}
//
//public synchronized void setTaskID(int ID) {
//	this.taskID = ID;
//}
//
//public synchronized long getTimeOfInitiation() {
//	return this.time_of_initiation;
//}
//
//public synchronized void resetTimeOfInitiation() {
//	this.time_of_initiation = System.currentTimeMillis();
//}
//
//public synchronized void setStarted10() {
//	this.s10 = true;
//}
//
//public synchronized boolean getStarted10() {
//	return this.s10;
//}
//}