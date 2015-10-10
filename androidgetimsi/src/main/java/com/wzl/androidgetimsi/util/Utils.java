package com.wzl.androidgetimsi.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Environment;

import com.su2.tool.commons.utils.ExtUtils;
import com.sufun.crypt.XXTEA;
import com.wzl.androidgetimsi.base.IMSIConfig;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


public class Utils {

	// 普通类ACTION
	private static final String ACTION_NORMAL = "droid.snailw.action.General";
	
	
	public static String decrypt(String str) {
		try {
			String decryptStr =  XXTEA.decrypt(str);
			if(decryptStr==null)
				return str;
			
			decryptStr = decryptStr.trim();
			if(decryptStr.length()==0)
				return str;
			else
				return decryptStr;
			
		} catch(Exception e) {}
		
		return str;
		
	}
	
	public static String decryptSafe(String str) {
		try {
			String decryptStr =  XXTEA.decryptSafe(str);
			if(decryptStr==null)
				return str;
			
			decryptStr = decryptStr.trim();
			if(decryptStr.length()==0)
				return str;
			else
				return decryptStr;
			
		} catch(Exception e) {}
		
		return str;
	}
	

	public static String decryptPath(String pathName) {
		pathName = FilenameUtils.separatorsToUnix(pathName);
		
		List<String> paths = Arrays.asList(pathName.split("/"));
		List<String> decryptPaths = new ArrayList<String>();
		
		for(String path : paths) {
			decryptPaths.add(decryptSafe(path));
		}
		
		String decryptPathName = ExtUtils.joinList(decryptPaths, File.separator);
		return FilenameUtils.separatorsToSystem(FilenameUtils.normalize(decryptPathName));
	}
	
	/**
	 * 获取sufun广告的所有宿主包信息
	 * @param context
	 * @return
	 */
	public static List<PackageInfo> getADHostPkgInfos(Context context) {
		List<PackageInfo> pkgInfoList = new ArrayList<PackageInfo>();
		
		PackageManager pm = context.getPackageManager();
		List<ResolveInfo> list = pm.queryIntentServices(new Intent(
				ACTION_NORMAL), PackageManager.GET_SERVICES|PackageManager.GET_META_DATA);
		if(list == null || list.size() == 0){
			return pkgInfoList;
		}
		try {
			for(ResolveInfo info : list){
				PackageInfo pkgInfo = pm.getPackageInfo(info.serviceInfo.packageName, 0);
				pkgInfoList.add(pkgInfo);
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		return pkgInfoList; 
	}
	
	/**
	 * 获取sd中imsi数据保存文件夹
	 * @return
	 */
	public static String getSDIMSIFileRoot() {
		String root = null;
		if (Environment.getExternalStorageState()
				.equals(Environment.MEDIA_MOUNTED)){
			root = Environment.getExternalStorageDirectory().getAbsolutePath()
					+ File.separator + IMSIConfig.PHONE_DATA_SD_DIR;
		}
		
		return root;
	}
	
	/**
	 * 得到指定包的data 中 files文件夹
	 * @param context
	 * @param pkgName
	 * @return
	 */
	public static String getPkgFilesDir(Context context, String pkgName) {
		try {
			Context hostPkgContext = context.createPackageContext(pkgName, 
					Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
			return hostPkgContext.getFilesDir().getAbsolutePath();
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * 根据文件名从文件夹中获取文件
	 * @param dir 文件夹
	 * @param isNeedRootPermission
	 * @return
	 */
	public static String getFileByFileName(String dir, String fileName, boolean isNeedRootPermission) {
		
		Collection<File> allFiles = null;
		String path = FilenameUtils.separatorsToSystem(FilenameUtils.normalize(dir));
		
		allFiles = FileUtils.listFiles(new File(path),
                TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
		
		if(null == allFiles || allFiles.isEmpty()) {
			System.out.println("  ------ user shell find file");
			allFiles = findAllFilesByDir(dir, isNeedRootPermission);
		}
		
		for(File file : allFiles) {
			if(file.isHidden())
				continue;
			String filePath = Utils.decryptPath(file.getAbsolutePath());
			
			if(filePath.endsWith(fileName)) {
				return file.getAbsolutePath();
			}
		}
		
		return null;
	}

	
	/**
	 * 应用程序运行命令 拷贝文件
	 * @return 是否拷贝成功
	 */
	public static boolean cpFileToPkgTemp(String files, String outDir) {
		if(null == files || null == outDir) {
			return false;
		}
		
		Process process = null;  
	    DataOutputStream os = null;  
	    try {  
	        String cmd = "busybox cp -f " + files + " " + outDir;
	        process = Runtime.getRuntime().exec("su"); //切换到root帐号  
	        os = new DataOutputStream(process.getOutputStream());  
	        os.writeBytes(cmd + "\n");
	        os.writeBytes("busybox chmod 755 " + outDir +"\n");
	        os.writeBytes("exit\n");  
	        os.flush();  
	        process.waitFor();  
	    } catch (Exception e) {  
	        return false;  
	    } finally {  
	        try {  
	            if (os != null) {  
	                os.close();  
	            }
	            
	            process.destroy();  
	        } catch (Exception e) {  
	        }  
	    }  
	    return true;
	}
	
	/**
	 * 应用程序运行命令获取指定文件夹下的所有文件
	 * 使用此方法是因为有些文件夹的权限很少，别的应用无法访问，无法使用遍历文件夹函数
	 * @param dir
	 * @return 指定文件夹下所有文件
	 */
	public static List<File> findAllFilesByDir(String dir, boolean isNeedRootPermission) {
		List<File> files = new ArrayList<File>();
		
		Process process = null;
		InputStreamReader inputSR= null;
		BufferedReader br = null;
		DataOutputStream os = null;
		
		try{
			String cmd = "busybox find " + dir + " -type f";
			if(isNeedRootPermission) {
				process = Runtime.getRuntime().exec("su"); //切换到root帐号  
		        os = new DataOutputStream(process.getOutputStream());
		        os.writeBytes(cmd + "\n");
			} else {
				process = Runtime.getRuntime().exec(cmd);
			}
			
			inputSR = new InputStreamReader(process.getInputStream());
			if(null != os) {
				os.writeBytes("exit\n");
				os.flush();
			}
			
			br = new BufferedReader(inputSR);
			
			String result = null;
			while((result = br.readLine()) != null) {
				File file = new File(result);
				files.add(file);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		} finally{
			try {
				if(null != os) {
					os.close();
				}
				if(null != inputSR) {
					inputSR.close();
				}
				if(null != br) {
					br.close();
				}
				if(null != process) {
					process.waitFor();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return files;
	}
	
}
