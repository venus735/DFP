package com.venus735.devicefingerprint;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import java.util.List;

public class DeviceInfoCollector {
    private LocationCollector locationCollector;
    private Context context;
    private String cachedLocationInfo;
    private long lastLocationInfoTime;
    private static final long CACHE_DURATION = 30000; // 30秒缓存
    
    public DeviceInfoCollector(Context context) {
        this.context = context;
        this.locationCollector = new LocationCollector(context);
    }
    
    /**
     * 收集设备定位信息
     * @return 定位信息字符串
     */
    public String collectLocationInfo() {
        long currentTime = System.currentTimeMillis();
        
        // 检查是否有有效的缓存
        if (cachedLocationInfo != null && (currentTime - lastLocationInfoTime) < CACHE_DURATION) {
            return cachedLocationInfo;
        }
        
        if (!locationCollector.isLocationServiceAvailable()) {
            cachedLocationInfo = "Location service not available";
            lastLocationInfoTime = currentTime;
            return cachedLocationInfo;
        }
        
        Location location = locationCollector.getCurrentLocation();
        if (location != null) {
            cachedLocationInfo = "Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude();
        } else {
            cachedLocationInfo = "Location not available";
        }
        lastLocationInfoTime = currentTime;
        return cachedLocationInfo;
    }
    
    /**
     * 注册定位更新监听
     * @param listener 定位更新监听器
     */
    public void registerLocationUpdates(LocationListener listener) {
        locationCollector.requestLocationUpdates(listener);
    }
    
    /**
     * 取消定位更新监听
     * @param listener 定位更新监听器
     */
    public void unregisterLocationUpdates(LocationListener listener) {
        locationCollector.removeLocationUpdates(listener);
    }
    
    /**
     * 收集基站信息
     * @return 基站信息字符串
     */
    public String collectCellTowerInfo() {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        StringBuilder cellInfoStr = new StringBuilder();
        
        try {
            List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
            if (cellInfoList != null) {
                for (CellInfo cellInfo : cellInfoList) {
                    if (cellInfo instanceof CellInfoGsm) {
                        CellInfoGsm gsmInfo = (CellInfoGsm) cellInfo;
                        cellInfoStr.append("GSM Cell - MCC: ").append(gsmInfo.getCellIdentity().getMccString())
                                .append(", MNC: ").append(gsmInfo.getCellIdentity().getMncString())
                                .append(", CID: ").append(gsmInfo.getCellIdentity().getCid())
                                .append(", LAC: ").append(gsmInfo.getCellIdentity().getLac())
                                .append("; ");
                    } else if (cellInfo instanceof CellInfoCdma) {
                        CellInfoCdma cdmaInfo = (CellInfoCdma) cellInfo;
                        cellInfoStr.append("CDMA Cell - SID: ").append(cdmaInfo.getCellIdentity().getSystemId())
                                .append(", NID: ").append(cdmaInfo.getCellIdentity().getNetworkId())
                                .append(", BID: ").append(cdmaInfo.getCellIdentity().getBasestationId())
                                .append("; ");
                    } else if (cellInfo instanceof CellInfoLte) {
                        CellInfoLte lteInfo = (CellInfoLte) cellInfo;
                        cellInfoStr.append("LTE Cell - MCC: ").append(lteInfo.getCellIdentity().getMccString())
                                .append(", MNC: ").append(lteInfo.getCellIdentity().getMncString())
                                .append(", CI: ").append(lteInfo.getCellIdentity().getCi())
                                .append(", TAC: ").append(lteInfo.getCellIdentity().getTac())
                                .append("; ");
                    } else if (cellInfo instanceof CellInfoWcdma) {
                        CellInfoWcdma wcdmaInfo = (CellInfoWcdma) cellInfo;
                        cellInfoStr.append("WCDMA Cell - MCC: ").append(wcdmaInfo.getCellIdentity().getMccString())
                                .append(", MNC: ").append(wcdmaInfo.getCellIdentity().getMncString())
                                .append(", CID: ").append(wcdmaInfo.getCellIdentity().getCid())
                                .append(", LAC: ").append(wcdmaInfo.getCellIdentity().getLac())
                                .append("; ");
                    }
                }
            }
        } catch (SecurityException e) {
            cellInfoStr.append("Permission denied to access cell info");
        }
        
        return cellInfoStr.length() > 0 ? cellInfoStr.toString() : "No cell tower info available";
    }
    
    /**
     * 收集设备硬件信息
     * @return 硬件信息字符串
     */
    public String collectHardwareInfo() {
        StringBuilder hardwareInfo = new StringBuilder();
        
        // 收集设备型号
        hardwareInfo.append("Device Model: ").append(android.os.Build.MODEL)
                .append(", Manufacturer: ").append(android.os.Build.MANUFACTURER)
                .append(", Brand: ").append(android.os.Build.BRAND);
        
        // 收集设备版本信息
        hardwareInfo.append(", OS Version: ").append(android.os.Build.VERSION.RELEASE)
                .append(", SDK: ").append(android.os.Build.VERSION.SDK_INT);
        
        // 收集设备硬件信息
        hardwareInfo.append(", Hardware: ").append(android.os.Build.HARDWARE)
                .append(", Device: ").append(android.os.Build.DEVICE)
                .append(", Product: ").append(android.os.Build.PRODUCT);
        
        return hardwareInfo.toString();
    }
    
    // 添加清除位置信息缓存的方法
    public void clearLocationInfoCache() {
        cachedLocationInfo = null;
        lastLocationInfoTime = 0;
        locationCollector.clearCachedLocation();
    }
}