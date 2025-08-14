package com.venus735.devicefingerprint;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityNr;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 基站信息收集类
 * 用于在应用启动后持续收集基站信息
 */
public class BaseStationCollector {
    private static final String TAG = "BaseStationCollector";
    private static final long COLLECTION_INTERVAL = 30000; // 30秒收集一次

    private Context context;
    private TelephonyManager telephonyManager;
    private ScheduledExecutorService scheduler;
    private BaseStationListener listener;

    public interface BaseStationListener {
        void onBaseStationInfoCollected(List<BaseStationInfo> baseStationInfoList);
    }

    public BaseStationCollector(Context context) {
        this.context = context.getApplicationContext();
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    /**
     * 开始收集基站信息
     */
    public void startCollecting() {
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleWithFixedDelay(this::collectBaseStationInfo, 0, COLLECTION_INTERVAL, TimeUnit.MILLISECONDS);
            Log.d(TAG, "基站信息收集已启动");
        }
    }

    /**
     * 停止收集基站信息
     */
    public void stopCollecting() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            Log.d(TAG, "基站信息收集已停止");
        }
    }

    /**
     * 设置基站信息监听器
     * @param listener 监听器
     */
    public void setBaseStationListener(BaseStationListener listener) {
        this.listener = listener;
    }

    /**
     * 收集基站信息
     */
    private void collectBaseStationInfo() {
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "缺少读取手机状态权限，无法收集基站信息");
                return;
            }

            List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
            if (cellInfoList == null || cellInfoList.isEmpty()) {
                Log.d(TAG, "未获取到基站信息");
                return;
            }

            List<BaseStationInfo> baseStationInfoList = new ArrayList<>();
            for (CellInfo cellInfo : cellInfoList) {
                BaseStationInfo info = extractBaseStationInfo(cellInfo);
                if (info != null) {
                    baseStationInfoList.add(info);
                }
            }

            // 回调监听器
            if (listener != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    listener.onBaseStationInfoCollected(baseStationInfoList);
                });
            }

            Log.d(TAG, "收集到 " + baseStationInfoList.size() + " 个基站信息");

        } catch (Exception e) {
            Log.e(TAG, "收集基站信息时发生错误", e);
        }
    }

    /**
     * 从CellInfo中提取基站信息
     * @param cellInfo CellInfo对象
     * @return BaseStationInfo对象
     */
    private BaseStationInfo extractBaseStationInfo(CellInfo cellInfo) {
        BaseStationInfo info = new BaseStationInfo();
        info.timestamp = System.currentTimeMillis();

        if (cellInfo instanceof CellInfoGsm) {
            CellInfoGsm gsm = (CellInfoGsm) cellInfo;
            info.type = "GSM";
            info.mcc = gsm.getCellIdentity().getMccString();
            info.mnc = gsm.getCellIdentity().getMncString();
            info.cid = gsm.getCellIdentity().getCid();
            info.lac = gsm.getCellIdentity().getLac();
            info.signalStrength = gsm.getCellSignalStrength().getDbm();
        } else if (cellInfo instanceof CellInfoLte) {
            CellInfoLte lte = (CellInfoLte) cellInfo;
            info.type = "LTE";
            info.mcc = lte.getCellIdentity().getMccString();
            info.mnc = lte.getCellIdentity().getMncString();
            info.cid = lte.getCellIdentity().getCi();
            info.lac = lte.getCellIdentity().getTac();
            info.signalStrength = lte.getCellSignalStrength().getDbm();
        } else if (cellInfo instanceof CellInfoWcdma) {
            CellInfoWcdma wcdma = (CellInfoWcdma) cellInfo;
            info.type = "WCDMA";
            info.mcc = wcdma.getCellIdentity().getMccString();
            info.mnc = wcdma.getCellIdentity().getMncString();
            info.cid = wcdma.getCellIdentity().getCid();
            info.lac = wcdma.getCellIdentity().getLac();
            info.signalStrength = wcdma.getCellSignalStrength().getDbm();
        } else if (cellInfo instanceof CellInfoCdma) {
            CellInfoCdma cdma = (CellInfoCdma) cellInfo;
            info.type = "CDMA";
            info.cid = cdma.getCellIdentity().getBasestationId();
            info.lac = cdma.getCellIdentity().getNetworkId();
            info.mcc = String.valueOf(cdma.getCellIdentity().getSystemId());
            info.signalStrength = cdma.getCellSignalStrength().getDbm();
        } 
        // 添加对5G NR基站的支持
        else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && 
                 cellInfo instanceof android.telephony.CellInfoNr) {
            CellInfoNr nr = (CellInfoNr) cellInfo;
            CellIdentityNr cellIdentityNr = (CellIdentityNr) nr.getCellIdentity();
            info.type = "NR";
            info.mcc = cellIdentityNr.getMccString();
            info.mnc = cellIdentityNr.getMncString();
            info.cid = cellIdentityNr.getNci();
            info.lac = cellIdentityNr.getTac();
            info.signalStrength = nr.getCellSignalStrength().getDbm();
        } 
        else {
            return null;
        }

        return info;
    }

    /**
     * 基站信息数据类
     */
    public static class BaseStationInfo {
        public String type;           // 网络类型 (GSM/LTE/WCDMA/CDMA/NR)
        public String mcc;            // 移动国家代码
        public String mnc;            // 移动网络代码
        public long cid;              // 小区标识 (NR使用long类型)
        public int lac;               // 位置区域码
        public int signalStrength;    // 信号强度 (dBm)
        public long timestamp;        // 时间戳

        @Override
        public String toString() {
            return "BaseStationInfo{" +
                    "type='" + type + '\'' +
                    ", mcc='" + mcc + '\'' +
                    ", mnc='" + mnc + '\'' +
                    ", cid=" + cid +
                    ", lac=" + lac +
                    ", signalStrength=" + signalStrength +
                    ", timestamp=" + timestamp +
                    '}';
        }
        
        // 添加获取显示文本的方法
        public String getDisplayText() {
            return String.format("%s\nMCC: %s MNC: %s\nCID: %d LAC: %d\nSignal: %d dBm",
                    type != null ? type : "Unknown",
                    mcc != null ? mcc : "N/A",
                    mnc != null ? mnc : "N/A",
                    cid,
                    lac,
                    signalStrength);
        }
    }
}