package com.easyconnectlib.easyconnectlib.wifi.callbacks;

import android.net.wifi.ScanResult;

import java.util.List;

public interface WifiScanListener {

    void onWifiScanList(List<ScanResult> scanResultList);

    void onLocationServiceOff();

    void onError(WIFI_SCAN_ERROR wifi_scan_error);

    enum WIFI_SCAN_ERROR {
        AP_MODE_ON
    }

}
