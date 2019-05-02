package com.easyconnectlib.easyconnectlib.wifisocketservice.callbacks;

import java.nio.ByteBuffer;

public interface ClientSocketServiceListener {

    void onConnected(String ssid, String clientIpAddress,int port);

    void onWifiConnecting(String ssid);

    void onSocketConnecting(String clientIpAddress,int port);

    void onDataReceived(ByteBuffer dataBuffer, String data);

    void onDisconnected();

    void onError(ERRORS socket_error);

    enum ERRORS {
        INTERNAL_ERROR,
        CONNECTION_ERROR,
        SSID_NOT_FOUND,
        AP_MODE_ON,
        AUTHENTICATING_ERROR,
        LOCATION_SERVICE_OFF,
        MINIMUM_PASSWORD_LENGTH_EIGHT,
        MAX_RETRY_EXCEED,
        NOT_CONNECTED,
        TIME_OUT
    }
}
