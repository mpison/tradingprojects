package com.quantlabs.stockApp.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JsonUtils {
    private static final Logger logger = Logger.getLogger(JsonUtils.class.getName());

    private JsonUtils() {
        // Private constructor to prevent instantiation
    }
    
 // Array methods
    public static boolean isNull(JSONArray array, int index) {
        return array == null || index >= array.length() || array.isNull(index);
    }

    public static String getString(JSONArray array, int index, String defaultValue) {
        try {
            return !isNull(array, index) ? array.getString(index) : defaultValue;
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Error getting string at index " + index, e);
            return defaultValue;
        }
    }

    // Object methods
    public static boolean hasKey(JSONObject obj, String key) {
        return obj != null && obj.has(key);
    }

    public static String getString(JSONObject obj, String key, String defaultValue) {
        try {
            return hasKey(obj, key) ? obj.getString(key) : defaultValue;
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Error getting string for key " + key, e);
            return defaultValue;
        }
    }

    public static JSONArray getJSONArray(JSONObject obj, String key) {
        try {
            return hasKey(obj, key) ? obj.getJSONArray(key) : new JSONArray();
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Error getting JSONArray for key " + key, e);
            return new JSONArray();
        }
    }

    public static JSONObject getJSONObject(JSONObject obj, String key) {
        try {
            return hasKey(obj, key) ? obj.getJSONObject(key) : new JSONObject();
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Error getting JSONObject for key " + key, e);
            return new JSONObject();
        }
    }    

    public static Double getDouble(JSONArray array, int index) {
        return getDouble(array, index, 0.0); // Default to 0.0 instead of null
    }

    public static Double getDouble(JSONArray array, int index, Double defaultValue) {
        try {
            if (isNull(array, index)) {
                return defaultValue != null ? defaultValue : 0.0;
            }
            return array.getDouble(index);
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Error getting double at index " + index, e);
            return defaultValue != null ? defaultValue : 0.0;
        }
    }

    public static Integer getInteger(JSONArray array, int index) {
        return getInteger(array, index, 0); // Default to 0 instead of null
    }

    public static Integer getInteger(JSONArray array, int index, Integer defaultValue) {
        try {
            if (isNull(array, index)) {
                return defaultValue != null ? defaultValue : 0;
            }
            return array.getInt(index);
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Error getting integer at index " + index, e);
            return defaultValue != null ? defaultValue : 0;
        }
    }

    public static Long getLong(JSONArray array, int index) {
        return getLong(array, index, 0L); // Default to 0L instead of null
    }

    public static Long getLong(JSONArray array, int index, Long defaultValue) {
        try {
            if (isNull(array, index)) {
                return defaultValue != null ? defaultValue : 0L;
            }
            return array.getLong(index);
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Error getting long at index " + index, e);
            return defaultValue != null ? defaultValue : 0L;
        }
    }

    public static Boolean getBoolean(JSONArray array, int index, Boolean defaultValue) {
        try {
            return !isNull(array, index) ? array.getBoolean(index) : defaultValue;
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Error getting boolean at index " + index, e);
            return defaultValue;
        }
    }

    public static JSONArray getJSONArray(JSONArray array, int index) {
        try {
            return !isNull(array, index) ? array.getJSONArray(index) : new JSONArray();
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Error getting JSONArray at index " + index, e);
            return new JSONArray();
        }
    }

    public static JSONObject getJSONObject(JSONArray array, int index) {
        try {
            return !isNull(array, index) ? array.getJSONObject(index) : new JSONObject();
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Error getting JSONObject at index " + index, e);
            return new JSONObject();
        }
    }

 // Add these methods to JsonUtils.java:

    public static Double getDouble(JSONObject obj, String key) {
        return getDouble(obj, key, 0.0);
    }

    public static Double getDouble(JSONObject obj, String key, Double defaultValue) {
        try {
            return hasKey(obj, key) ? obj.getDouble(key) : defaultValue;
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Error getting double for key " + key, e);
            return defaultValue;
        }
    }

    public static Integer getInteger(JSONObject obj, String key) {
        return getInteger(obj, key, 0);
    }

    public static Integer getInteger(JSONObject obj, String key, Integer defaultValue) {
        try {
            return hasKey(obj, key) ? obj.getInt(key) : defaultValue;
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Error getting integer for key " + key, e);
            return defaultValue;
        }
    }

    public static Long getLong(JSONObject obj, String key) {
        return getLong(obj, key, 0L);
    }

    public static Long getLong(JSONObject obj, String key, Long defaultValue) {
        try {
            return hasKey(obj, key) ? obj.getLong(key) : defaultValue;
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Error getting long for key " + key, e);
            return defaultValue;
        }
    }

    public static Boolean getBoolean(JSONObject obj, String key, Boolean defaultValue) {
        try {
            return hasKey(obj, key) ? obj.getBoolean(key) : defaultValue;
        } catch (JSONException e) {
            logger.log(Level.WARNING, "Error getting boolean for key " + key, e);
            return defaultValue;
        }
    }
}