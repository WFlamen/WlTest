package com.wzl.androidgetimsi.decrypt;

import android.util.Log;

import com.wzl.androidgetimsi.util.Utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class PropDecrypt {
	
	private static final String TAG = "prop_decrypt";
	
	private Properties loadData(String inFile) {
		try {
			
			BufferedInputStream inStr = new BufferedInputStream(
								new FileInputStream(inFile), 16*1024);
			Properties prop = new Properties();
			prop.load(inStr);
			inStr.close();
			return prop;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch(IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public Map<String, String> decrypt(String inFile) {
		
		Map<String, String> phoneData = new HashMap<String, String>();
		
		if(null == inFile) {
			Log.e(TAG, "decrypt imsi file failed, imsi data file not found");
			return phoneData;
		}
		
		try {
			Properties datas = loadData(inFile);
			if(datas == null) {
				Log.e(TAG, "load file failed, file : " + inFile);
				return phoneData;
			}
			
			for(String key : datas.stringPropertyNames()) {
				String deKey = Utils.decrypt(key);
				
				String val = datas.getProperty(key);
				String deVal = Utils.decrypt(val);
				
				phoneData.put(deKey, deVal);
			}
			
			return phoneData;
		} catch (Exception e) {
			e.printStackTrace();
			return phoneData;
		} 
	}
	
	
}
