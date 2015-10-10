package com.droidlogic.utils.tunerinput.tvutil;

import java.util.Map;
import java.util.HashMap;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;

/*
{
	Map<String, String> map = new HashMap<String, String>();
	map.put("color", "red");
	map.put("symbols", "{,=&*?}");
	map.put("empty", "");
	String output = MapUtil.mapToString(map);
	Map<String, String> parsedMap = MapUtil.stringToMap(output);
	for (String key : map.keySet()) {
	 Assert.assertEquals(parsedMap.get(key), map.get(key));
	}
}
*/
public class MapUtil {
		public static String mapToString(Map<String, String> map) {
			StringBuilder stringBuilder = new StringBuilder();

			for (String key : map.keySet()) {
				if (stringBuilder.length() > 0) {
					stringBuilder.append("&");
				}
				String value = map.get(key);
				try {
					stringBuilder.append((key != null ? URLEncoder.encode(key, "UTF-8") : ""));
					stringBuilder.append("=");
					stringBuilder.append(value != null ? URLEncoder.encode(value, "UTF-8") : "");
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException("This method requires UTF-8 encoding support", e);
				}
			}

			return stringBuilder.toString();
		}

		public static Map<String, String> stringToMap(String input) {
			Map<String, String> map = new HashMap<String, String>();

			String[] nameValuePairs = input.split("&");
			for (String nameValuePair : nameValuePairs) {
				String[] nameValue = nameValuePair.split("=");
				try {
					map.put(URLDecoder.decode(nameValue[0], "UTF-8"), nameValue.length > 1 ? URLDecoder.decode(
					nameValue[1], "UTF-8") : "");
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException("This method requires UTF-8 encoding support", e);
				}
			}

			return map;
		}
}
