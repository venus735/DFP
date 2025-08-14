package com.venus735.devicefingerprint;

import android.media.MediaDrm;

import java.util.UUID;

public class DeviceFingerprintGenerator {
    public static String getDrmUniqueId() {
        // Widevine DRM 的 UUID
        UUID widevineUuid = new UUID(0xEDEF8BA979D64ACEL, 0xA3C827DCD51D21EDL);
        try (MediaDrm mediaDrm = new MediaDrm(widevineUuid)) {
            byte[] deviceId = mediaDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID);
            return bytesToHex(deviceId); // 转换为16进制字符串
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
