package com.wzl.androidgetimsi;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wzl.androidgetimsi.base.ItemClickListener;

import java.util.List;

public class AdHostPkgInfoAdapter extends BaseAdapter {
	
	private static final String TAG = "ad-host-adapter";

    private List<PackageInfo> pkgInfos;
    private LayoutInflater inflater;
    
    private ItemClickListener listener;
    
    private Context mContext;
    
    public AdHostPkgInfoAdapter(Context context, List<PackageInfo> list){
    	this.mContext = context;
        this.pkgInfos = list;
        inflater = LayoutInflater.from(context);
    }
    
    @Override
    public int getCount() {
        return pkgInfos.size();
    }

    @Override
    public PackageInfo getItem(int position) {
        return pkgInfos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
	@Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Holder holder;
        if(convertView==null){
            holder = new Holder();
            convertView = inflater.inflate(R.layout.item_listview, null);
            holder.name = (TextView) convertView.findViewById(R.id.item_name);
            holder.icon = (ImageView) convertView.findViewById(R.id.item_pic);
            convertView.setTag(holder);
        }else{
            holder = (Holder) convertView.getTag();
        }
        
        holder.icon.setImageDrawable(getAppIcon(pkgInfos.get(position).applicationInfo));
        String appName = pkgInfos.get(position).applicationInfo.
        					      loadLabel(mContext.getPackageManager()).toString();
        holder.name.setText(appName);
        
        convertView.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Log.i(TAG, "get ad host package name, name : " + 
						    pkgInfos.get(position).packageName);
				listener.OnClick(pkgInfos.get(position));
			}
		});
        
        return convertView;
    }
    
    private Drawable getAppIcon(ApplicationInfo appInfo) {
    	PackageManager pkgMgr = mContext.getPackageManager();
    	return pkgMgr.getApplicationIcon(appInfo);
    }

    protected class Holder{
    	ImageView icon;
        TextView name;
    }
    
    public void setOnItemChildClickListener(ItemClickListener listener) {
    	this.listener = listener;
    }
}
