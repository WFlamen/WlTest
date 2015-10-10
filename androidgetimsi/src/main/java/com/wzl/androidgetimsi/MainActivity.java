package com.wzl.androidgetimsi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.wzl.androidgetimsi.base.IMSIConfig;
import com.wzl.androidgetimsi.base.ItemClickListener;
import com.wzl.androidgetimsi.decrypt.PropDecrypt;
import com.wzl.androidgetimsi.util.Utils;

import java.io.File;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
	
	private static final String TAG = "main-activiy";
	
	private static final int SD_IMSI_INFO = 1;
	private static final int PHONE_IMSI_INFO = 2;
	private static final int SHOW_AD_HOST_PKGINFO = 3;
	private static final int CLEAR_INFO = 4;
	
	private static final String IMSI = "imsi";
	private static final String IMEI = "imei";
	
	private Button phoneImsiBtn;
	private Button clearBtn;
	
	private TextView sdImsiText;
	private TextView phoneImsiText;
	
	private AlertDialog alertDialog;
	
	private MyHandler handler;
	private PropDecrypt propDecrypt;
	
	private OnItemOnClickListener listener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		handler = new MyHandler(getApplicationContext(), getMainLooper());
		propDecrypt = new PropDecrypt();
		
		sdImsiText = (TextView)findViewById(R.id.sd_imsi);
		phoneImsiText = (TextView)findViewById(R.id.phohe_imsi);
		
		phoneImsiBtn = (Button)findViewById(R.id.get_phone_imsi);
		phoneImsiBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				List<PackageInfo> pkgInfos = Utils.getADHostPkgInfos(getApplicationContext());
				
				if(pkgInfos.isEmpty()) {
					Log.w(TAG, "ad host is not found");
					sendMessage(PHONE_IMSI_INFO, null);
				} else {
					sendMessage(SHOW_AD_HOST_PKGINFO, pkgInfos);
				}
			}
		});
		
		clearBtn = (Button)findViewById(R.id.clear_info);
		clearBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				sendMessage(CLEAR_INFO, null);
			}
		});
		
		printSDImsiInfo();
	}
	
	protected void actionAlertDialog(List<PackageInfo> pkgInfos){
        AlertDialog.Builder builder;
        
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.ad_host_pkginfo, (ViewGroup)findViewById(R.id.app_list_layout));
        ListView myListView = (ListView) layout.findViewById(R.id.app_list);
        
        AdHostPkgInfoAdapter adapter = new AdHostPkgInfoAdapter(this, pkgInfos);
        myListView.setAdapter(adapter);
        
        listener = new OnItemOnClickListener(getApplicationContext());
        adapter.setOnItemChildClickListener(listener);
        
        builder = new AlertDialog.Builder(this);
        builder.setView(layout);
        alertDialog = builder.create();
        alertDialog.show();
    }
	
	private void printSDImsiInfo() {
		Map<String, String> imsiInfos = propDecrypt.decrypt(getSDImsiDataFiles());
		
		if(imsiInfos.isEmpty()) {
			Log.w(TAG, "get imsi info failed from sd, save imsi data file is not found");
			sendMessage(SD_IMSI_INFO, null);
			return;
		}

		Log.i(TAG, "get imsi info from sd : ");
		DataInfo info = new DataInfo();
		info.infos = imsiInfos;
		sendMessage(SD_IMSI_INFO, info);
	}
	
	private void printInfo(Map<String, String> imsiInfos, TextView tv) {
		for(String key : imsiInfos.keySet()) {
			if(key.equals(IMSI) || key.equals(IMEI)) {
				String value = imsiInfos.get(key);
				
				Log.i(TAG, key + " : " + value);
				value = key + " : " + value;
				
				tv.append("\n| -->> " + key + " : " +value);
			}
		}
	}
	
	private void sendMessage(int what, Object obj) {
		Message msg = new Message();
		msg.what = what;
		msg.obj = obj;
		handler.sendMessage(msg);
	}
	
	
	private class MyHandler extends Handler {
		Context context;
		
		public MyHandler(Context context, Looper looper) {
			super(looper);
			this.context = context;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			
			switch (msg.what) {
				case SD_IMSI_INFO:
					if(null == msg.obj) {
						sdImsiText.append("\n|        save imsi data file is not found");
					} else {
						DataInfo sdInfo = (DataInfo)msg.obj;
						printInfo(sdInfo.infos, sdImsiText);
					}
					
					sdImsiText.setVisibility(View.VISIBLE);
					break;
					
				case PHONE_IMSI_INFO:
					if(null == msg.obj) {
						phoneImsiText.append("\n|        save imsi data file is not found");
						phoneImsiText.setVisibility(View.VISIBLE);
						break;
					}
					
					DataInfo hostInfo = (DataInfo)msg.obj;
					
					phoneImsiText.append("\n*********************");
					phoneImsiText.append("\n| appName : " + hostInfo.appName);
					phoneImsiText.append("\n| appPackageName : " + hostInfo.pkgName);
					phoneImsiText.append("\n| ad versionCode : " + hostInfo.versionCode);
					printInfo(hostInfo.infos, phoneImsiText);
					phoneImsiText.append("\n*********************\n");
					
					phoneImsiText.setVisibility(View.VISIBLE);
					break;
					
				case SHOW_AD_HOST_PKGINFO:
					List<PackageInfo> pkgInfos = (List<PackageInfo>)msg.obj;
					actionAlertDialog(pkgInfos);
					break;
					
				case CLEAR_INFO:
					phoneImsiText.setText(context.getResources().getText(R.string.phone_imsi));
					phoneImsiText.setVisibility(View.GONE);
					break;
	
				default:
					break;
			}
			
		}
		
	}
	
	private class OnItemOnClickListener extends ItemClickListener {
		
		private Context mContext;
		
		public OnItemOnClickListener(Context context) {
			this.mContext = context;
		}

		@Override
		public void OnClick(PackageInfo pkgInfo) {
			alertDialog.dismiss();
			
			String ismFile = getHostImsiFile(mContext, pkgInfo.packageName);
			
			Map<String, String> imsiInfos = propDecrypt.decrypt(ismFile);
			if(imsiInfos.isEmpty()) {
				Log.w(TAG, "get imsi info failed from phone, save imsi data file is not found");
				sendMessage(PHONE_IMSI_INFO, null);
				return;
			}

			DataInfo info = new DataInfo();
			info.appName = pkgInfo.applicationInfo.
								   loadLabel(mContext.getPackageManager()).toString();
			info.pkgName = pkgInfo.packageName;
			info.infos = imsiInfos;
			info.versionCode = getADVersionCode(mContext, pkgInfo.packageName);
			
			Log.i(TAG, "get imsi info from phone : ");
			sendMessage(PHONE_IMSI_INFO, info);
		}
		
	}
	
	private String getHostImsiFile(Context context, String pkgName) {
		String pkgFilesDir = Utils.getPkgFilesDir(context, pkgName);
		String imsiFile = Utils.getFileByFileName(pkgFilesDir, IMSIConfig.PHONE_DATA, true);
		
		String outDir = context.getCacheDir().getAbsolutePath() + 
						File.separator + IMSIConfig.TEMP_IMSI_FILE;
		
		if(!Utils.cpFileToPkgTemp(imsiFile, outDir)) {
			return null;
		}
		
		return outDir;
	}
	
	private String getSDImsiDataFiles() {
		String sdImsiFileRoot = Utils.getSDIMSIFileRoot();
		return Utils.getFileByFileName(sdImsiFileRoot, IMSIConfig.PHONE_DATA, false);
	}
	
	private String getADVersionCode(Context context, String pkgName) {
		String pkgFilesDir = Utils.getPkgFilesDir(context, pkgName);
		String adFile  = Utils.getFileByFileName(pkgFilesDir, IMSIConfig.AD_FILE, true);
		
		String outDir = context.getCacheDir().getAbsolutePath() + 
				File.separator + IMSIConfig.TEMP_AD_FIlE;
		if(!Utils.cpFileToPkgTemp(adFile, outDir)) {
			return null;
		}
		
		String versoinCode = context.getPackageManager()
				 				 .getPackageArchiveInfo(outDir, 0)
				 				 .versionName;
		return versoinCode;
	}
	
	private class DataInfo{
		public String appName;
		public String pkgName;
		public Map<String, String> infos;
		public String versionCode;
	}

}
