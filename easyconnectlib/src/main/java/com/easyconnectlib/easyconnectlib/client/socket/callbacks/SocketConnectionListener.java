package com.easyconnectlib.easyconnectlib.client.callbacks;

import java.nio.ByteBuffer;

public interface SocketConnectionListener {

    void onClientConnected(String serverIpAddress, int port);

    void onClientConnecting(String serverIpAddress, int port);

    void onDataReceived(ByteBuffer dataBuffer, String data);

    void onClientSocketClose();

    void onClientSocketError(SOCKET_ERROR socket_error);

    enum SOCKET_ERROR {
        INTERNAL_ERROR,
        CONNECTION_ERROR,
        NOT_CONNECTED
    }
}
