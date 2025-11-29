package com.quantlabs.stockApp.helper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

public class JSONHelper {

	/**
	 * Convert JSON watchlists to Map<String, Set<String>> format
	 */
	@SuppressWarnings("unchecked")
	public	static Map<String, Set<String>> convertJSONWatchlistsToMap(JSONObject watchlistsJson) {
	    Map<String, Set<String>> watchlistsMap = new HashMap<>();
	    
	    try {
	        Iterator<String> keys = watchlistsJson.keys();
	        while (keys.hasNext()) {
	            String watchlistName = keys.next();
	            JSONArray symbolsArray = watchlistsJson.getJSONArray(watchlistName);
	            
	            Set<String> symbolsSet = new HashSet<>();
	            for (int i = 0; i < symbolsArray.length(); i++) {
	                symbolsSet.add(symbolsArray.getString(i));
	            }
	            
	            watchlistsMap.put(watchlistName, symbolsSet);
	        }
	    } catch (Exception e) {
	        //logToConsole("❌ Error converting JSON watchlists: " + e.getMessage());
	    }
	    
	    return watchlistsMap;
	}
}
