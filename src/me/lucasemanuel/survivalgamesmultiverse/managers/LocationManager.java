/**
 *  Name: LocationManager.java
 *  Date: 19:56:01 - 15 sep 2012
 * 
 *  Author: LucasEmanuel @ bukkit forums
 *  
 *  
 *  Description:
 *  
 *  Manages all locations for all gameworlds.
 *  
 * 
 * 
 */

package me.lucasemanuel.survivalgamesmultiverse.managers;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import me.lucasemanuel.survivalgamesmultiverse.Main;
import me.lucasemanuel.survivalgamesmultiverse.utils.ConsoleLogger;
import me.lucasemanuel.survivalgamesmultiverse.utils.SLAPI;
import me.lucasemanuel.survivalgamesmultiverse.utils.SerializedLocation;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class LocationManager {
	
	private Main plugin;
	final private ConsoleLogger logger;
	
	// Key = Worldname, Value = Main/Arena lists, ValueOfValue = key=location, boolean=true means the location is available
	private HashMap<String, HashMap<String, HashMap<Location, Boolean>>> locations;
	
	public LocationManager(Main instance) {
		plugin = instance;
		logger = new ConsoleLogger(instance, "LocationManager");
		
		locations = new HashMap<String, HashMap<String, HashMap<Location, Boolean>>>();
		
		logger.debug("Initiated");
	}
	
	public void addWorld(String worldname) {
		logger.debug("Adding world: " + worldname);
		
		locations.put(worldname, new HashMap<String, HashMap<Location, Boolean>>());
		loadLocations(worldname);
	}
	
	public void resetLocationStatuses(World world) {
		
		String worldname = world.getName();
		
		HashMap<Location, Boolean> main  = locations.get(worldname).get("main");
		HashMap<Location, Boolean> arena = locations.get(worldname).get("arena");
		
		if(main != null) 
			for(Entry<Location, Boolean> entry : main.entrySet()) {
				entry.setValue(true);
			}
		
		if(arena != null)
			for(Entry<Location, Boolean> entry : arena.entrySet()) {
				entry.setValue(true);
			}
	}
	
	public boolean tpToStart(Player player) {
		
		HashMap<Location, Boolean> locationlist = locations.get(player.getWorld().getName()).get("main");
		
		if(locationlist == null) {
			logger.severe("No saved locations for world: " + player.getWorld().getName());
			return false;
		}
		
		for(Entry<Location, Boolean> entry : locationlist.entrySet()) {
			if(entry.getValue()) {
				
				logger.debug("Teleporting player: " + player.getName() + " to start");
				
				player.teleport(entry.getKey());
				entry.setValue(false);
				
				return true;
			}
		}
		
		logger.debug("No locations left for world: " + player.getWorld().getName());
		return false;
	}
	
	public void addLocation(String type, Location location) {
		
		logger.debug("Adding locationtype: " + type + ", to world: " + location.getWorld().getName());
		
		HashMap<Location, Boolean> map = locations.get(location.getWorld().getName()).get(type);
		
		if(map == null) {
			map = new HashMap<Location, Boolean>();
			locations.get(location.getWorld().getName()).put(type, map);
		}
		
		map.put(location, true);
	}

	public void saveLocationList(final String listtype, String worldname) {
		
		logger.debug("Saving locationtype: " + listtype + " for world: " + worldname);
		
		final String path = plugin.getDataFolder().getAbsolutePath() + "/locations/" + worldname;
		
		final HashSet<SerializedLocation> tempmap = new HashSet<SerializedLocation>();
		
		HashMap<Location, Boolean> locationlist = locations.get(worldname).get(listtype);
		
		if(locationlist != null) {
			for(Location location : locationlist.keySet()) {
				tempmap.add(new SerializedLocation(location));
			}
		}
		
		/*
		 *  This could potentially lead to several threads modifying the same save-file, which isn't a good thing!
		 *  But hopefully no one will use this command that many times at the same time that the file would be in danger.
		 */
		
		Thread thread = new Thread() {
			public void run() {
				
				String fullpath = path + "/" + listtype + ".dat";
				
				File test = new File(path);
				
				if(!test.exists()) {
					test.mkdirs();
				}
				
				try {
					logger.debug("Saving locationtype: " + listtype + " to: " + fullpath);
					SLAPI.save(tempmap, fullpath);
				}
				catch (Exception e) {
					logger.severe("Error while saving locationlist! Message: " + e.getMessage());
				}
			}
		};
		
		thread.start();
	}
	
	@SuppressWarnings("unchecked")
	private void loadLocations(String worldname) {
		
		logger.debug("Loading locations for world: " + worldname);
		
		String path = plugin.getDataFolder().getAbsolutePath() + "/locations/" + worldname;
		
		if(new File(path).exists()) {
			
			String mainpath  = path + "/" + "main.dat";
			String arenapath = path + "/" + "arena.dat";
			
			HashSet<SerializedLocation> mainmap  = new HashSet<SerializedLocation>();
			HashSet<SerializedLocation> arenamap = new HashSet<SerializedLocation>();
			
			try {
				mainmap  = (HashSet<SerializedLocation>) SLAPI.load(mainpath);
				arenamap = (HashSet<SerializedLocation>) SLAPI.load(arenapath);
			}
			catch (Exception e) {
				logger.severe("Error while loading saved locations for world: " + worldname);
				logger.severe("Message: " + e.getMessage());
			}
			
			if(!mainmap.isEmpty()) {
				for(SerializedLocation serialized : mainmap) {
					addLocation("main", serialized.deserialize());
				}
			}
			
			if(!arenamap.isEmpty()) {
				for(SerializedLocation serialized : arenamap) {
					addLocation("arena", serialized.deserialize());
				}
			}
		}
		else
			logger.warning("No saved locations for world: " + worldname);
	}

	public void clearLocationList(String type, String worldname) {
		
		HashMap<Location, Boolean> locationlist = locations.get(worldname).get(type);
		
		if(locationlist != null)
			locationlist.clear();
	}
}
