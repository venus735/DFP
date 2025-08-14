package com.venus735.devicefingerprint;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.ActivityCompat;

public class LocationCollector {
    private Context context;
    private LocationManager locationManager;
    private Location currentLocation;
    private static final String TAG = "LocationCollector";
    private static final int LOCATION_TIMEOUT = 10000; // 10秒超时
    private long lastLocationRequestTime = 0;
    private static final long MIN_LOCATION_REQUEST_INTERVAL = 5000; // 最小请求间隔5秒
    
    public LocationCollector(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }
    
    /**
     * 获取当前位置信息
     * @return Location对象，包含经纬度等信息
     */
    public Location getCurrentLocation() {
        long currentTime = System.currentTimeMillis();
        
        // 如果已经有最近的位置信息，直接返回
        if (currentLocation != null && (currentTime - currentLocation.getTime()) < LOCATION_TIMEOUT) {
            Log.d(TAG, "Using cached location");
            return currentLocation;
        }
        
        // 检查权限
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED && 
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted");
            return null;
        }
        
        // 检查定位服务是否启用
        if (!isLocationServiceAvailable()) {
            Log.w(TAG, "Location service not available");
            return null;
        }
        
        // 优先获取GPS定位
        Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        
        // 返回更精确的位置信息
        if (gpsLocation != null && (currentTime - gpsLocation.getTime()) < LOCATION_TIMEOUT) {
            currentLocation = gpsLocation;
            Log.d(TAG, "Using GPS location: " + gpsLocation.getLatitude() + ", " + gpsLocation.getLongitude());
        } else if (networkLocation != null && (currentTime - networkLocation.getTime()) < LOCATION_TIMEOUT) {
            currentLocation = networkLocation;
            Log.d(TAG, "Using network location: " + networkLocation.getLatitude() + ", " + networkLocation.getLongitude());
        } else {
            Log.w(TAG, "No recent last known location available");
            // 控制请求频率，避免频繁请求
            if (currentTime - lastLocationRequestTime > MIN_LOCATION_REQUEST_INTERVAL) {
                // 尝试请求实时位置
                requestCurrentLocation();
                lastLocationRequestTime = currentTime;
            }
        }
        
        return currentLocation;
    }
    
    /**
     * 请求当前位置更新
     */
    private void requestCurrentLocation() {
        try {
            LocationListener singleLocationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    currentLocation = location;
                    Log.d(TAG, "Location updated: " + location.getLatitude() + ", " + location.getLongitude());
                    // 获取到位置后移除监听器
                    locationManager.removeUpdates(this);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {}
            };
            
            // 请求位置更新
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // 使用Looper.getMainLooper()确保在主线程中处理位置更新
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, singleLocationListener, Looper.getMainLooper());
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, singleLocationListener, Looper.getMainLooper());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error requesting current location", e);
        }
    }
    
    /**
     * 请求位置更新
     * @param listener 位置更新监听器
     */
    public void requestLocationUpdates(LocationListener listener) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED && 
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        // 注册位置更新监听
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, listener, Looper.getMainLooper());
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 10, listener, Looper.getMainLooper());
    }
    
    /**
     * 移除位置更新监听
     * @param listener 位置更新监听器
     */
    public void removeLocationUpdates(LocationListener listener) {
        locationManager.removeUpdates(listener);
    }
    
    /**
     * 检查定位服务是否可用
     * @return true表示定位服务可用
     */
    public boolean isLocationServiceAvailable() {
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        Log.d(TAG, "GPS enabled: " + gpsEnabled + ", Network enabled: " + networkEnabled);
        return gpsEnabled || networkEnabled;
    }
    
    // 添加清空缓存位置的方法
    public void clearCachedLocation() {
        currentLocation = null;
    }
}