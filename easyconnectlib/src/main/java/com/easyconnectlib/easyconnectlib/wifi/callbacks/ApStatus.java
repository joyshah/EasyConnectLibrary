package com.easyconnectlib.easyconnectlib.wifi.callbacks;



public interface ApStatus {

    void onApModeStarted(String apName);

    void onApModeStarting();

    void onError(AP_ERROR ap_error);

    void onApModeStopped();

    enum AP_ERROR {
        MINIMUM_PASSWORD_LENGTH_EIGHT,
        INTERNAL_ERROR,
        WIFI_STATE_FOUND_ON
    }
}