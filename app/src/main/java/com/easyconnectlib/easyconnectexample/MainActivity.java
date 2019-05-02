package com.easyconnectlib.easyconnectexample;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.easyconnectlib.easyconnectlib.wifi.lib.WifiConnection;
import com.easyconnectlib.easyconnectlib.wifisocketservice.callbacks.ClientSocketServiceListener;
import com.easyconnectlib.easyconnectlib.wifisocketservice.service.ClientSocketService;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity  implements ClientSocketServiceListener  {
    private ServiceConnection mServiceConnection;
    private ClientSocketService mClientSocketService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent=new Intent();
        startService(intent);

        mServiceConnection=new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                ClientSocketService.ClientSocketServiceBinder myBinder = (ClientSocketService.ClientSocketServiceBinder) service;
                mClientSocketService = myBinder.getService();
                mClientSocketService.registerClientSocketServiceStatus(MainActivity.this);
                mClientSocketService.connect("SSID","Password","192.168.0.11",8082);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mClientSocketService.unRegisterClientSocketServiceStatus();
            }
        };
        bindService(intent,mServiceConnection, BIND_AUTO_CREATE);

    }

    @Override
    public void onConnected(String ssid, String clientIpAddress, int port) {

    }

    @Override
    public void onWifiConnecting(String ssid) {

    }

    @Override
    public void onSocketConnecting(String clientIpAddress, int port) {

    }

    @Override
    public void onDataReceived(ByteBuffer dataBuffer, String data) {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onError(ERRORS socket_error) {

    }
}
