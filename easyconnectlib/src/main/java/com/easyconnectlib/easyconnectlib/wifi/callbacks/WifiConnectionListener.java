package com.easyconnectlib.easyconnectlib.wifi.callbacks;

public interface WifiConnectionListener {

    void onWifiStateChanged(boolean isEnabled);

    void onWifiConnected(String ssid);

    void onWifiDisconnected(String ssid);

    void onWifiConnecting();

    void onError(WIFI_ERROR wifi_error);

    void onLocationServiceOff();

    enum WIFI_ERROR {
        SSID_NOT_FOUND,
        INTERNAL_ERROR,
        WIFI_DISABLED,
        WIFI_NOT_CONNECTED,
        AUTHENTICATING_ERROR,
        AP_MODE_ON,
        MINIMUM_PASSWORD_LENGTH_EIGHT
    }
}