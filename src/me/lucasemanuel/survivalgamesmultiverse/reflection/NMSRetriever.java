/**
 *  Name:    NMSRetriever.java
 *  Created: 14:30:09 - 3 jun 2013
 * 
 *  Author:  Lucas Arnstr�m - LucasEmanuel @ Bukkit forums
 *  Contact: lucasarnstrom(at)gmail(dot)com
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
 * 
 */

package me.lucasemanuel.survivalgamesmultiverse.reflection;

import java.lang.reflect.InvocationTargetException;

import org.bukkit.plugin.java.JavaPlugin;

public class NMSRetriever {
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static NMS getNMS(JavaPlugin plugin) {
		NMS nms = null;
		
		try {
			Class c = Class.forName("me.lucasemanuel.survivalgamesmultiverse.reflection." + getServerVersion(plugin));
			if(NMS.class.isAssignableFrom(c)) {
				nms = (NMS) c.getConstructor().newInstance();
			}
		}
		catch (ClassNotFoundException 
				| InstantiationException 
				| IllegalAccessException 
				| IllegalArgumentException 
				| InvocationTargetException 
				| NoSuchMethodException 
				| SecurityException e) {
			return null;
		}
		
		return nms;
	}
	
	public static String getServerVersion(JavaPlugin plugin) {
		return plugin.getServer().getClass().getPackage().getName().split("\\.")[3];
	}
}
