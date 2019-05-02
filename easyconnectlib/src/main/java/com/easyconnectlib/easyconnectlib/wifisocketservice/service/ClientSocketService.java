package com.easyconnectlib.easyconnectlib.wifisocketservice.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.easyconnectlib.easyconnectlib.client.callbacks.SocketConnectionListener;
import com.easyconnectlib.easyconnectlib.client.socket.lib.ClientSocketConnection;
import com.easyconnectlib.easyconnectlib.wifi.callbacks.WifiConnectionListener;
import com.easyconnectlib.easyconnectlib.wifi.callbacks.WifiScanListener;
import com.easyconnectlib.easyconnectlib.wifi.lib.WifiConnection;
import com.easyconnectlib.easyconnectlib.wifisocketservice.callbacks.ClientSocketServiceListener;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;

public class ClientSocketService extends Service {
    private final String TAG = ClientSocketService.class.getSimpleName();
    private ClientSocketServiceListener mClientSocketServiceListener;
    private IBinder clientSocketServiceBinder = new ClientSocketServiceBinder();
    private WifiConnection mWifiConnection;
    private ClientSocketConnection mClientSocketConnection;
    private String mSSID, mPassword, mIpAddress;
    private int mPort;
    private int mRetry;
    private int mRetried;
    private boolean mIsRetryEnabled;
    private CountDownTimer countDownTimer;

    public ClientSocketService() {
        mRetried = 0;
        mIsRetryEnabled = false;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "oncreate");
        mClientSocketConnection = new ClientSocketConnection();
        mWifiConnection = WifiConnection.getInstance(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onbind");
        return clientSocketServiceBinder;
    }


    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.i(TAG, "onrebind");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "unbind");
        return super.onUnbind(intent);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "ondestroy");
        mClientSocketConnection.closeSocket();
    }

    private SocketConnectionListener socketConnectionListener = new SocketConnectionListener() {
        @Override
        public void onClientConnected(String serverIpAddress, final int port) {
            if (mClientSocketServiceListener != null) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mClientSocketServiceListener.onConnected(mSSID, mIpAddress,port);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }

        @Override
        public void onClientConnecting(final String clientIpAddress,final int port) {
            if (mClientSocketServiceListener != null) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mClientSocketServiceListener.onSocketConnecting( clientIpAddress, port);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }

        @Override
        public void onDataReceived(final ByteBuffer dataBuffer, final String data) {
            if (mClientSocketServiceListener != null) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mClientSocketServiceListener.onDataReceived(dataBuffer, data);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }

        @Override
        public void onClientSocketClose() {
            if (mClientSocketServiceListener != null) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mClientSocketServiceListener.onDisconnected();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }

        @Override
        public void onClientSocketError(final SOCKET_ERROR socket_error) {
            if (mClientSocketServiceListener != null) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            switch (socket_error) {
                                case CONNECTION_ERROR:
                                    mClientSocketServiceListener.onError(ClientSocketServiceListener.ERRORS.CONNECTION_ERROR);
                                    break;
                                case INTERNAL_ERROR:
                                    mClientSocketServiceListener.onError(ClientSocketServiceListener.ERRORS.INTERNAL_ERROR);
                                    break;
                                case NOT_CONNECTED:
                                    mClientSocketServiceListener.onError(ClientSocketServiceListener.ERRORS.NOT_CONNECTED);
                                    break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    };

    private WifiConnectionListener wifiConnectionListener = new WifiConnectionListener() {
        @Override
        public void onWifiStateChanged(boolean isEnabled) {

        }

        @Override
        public void onWifiConnected(String connectedSSID) {
            Log.i(TAG, "onWifiConnected :" + connectedSSID);
            if (connectedSSID.equals(mSSID)) {
                startSocketConnection();
                mIsRetryEnabled = false;
                stopCountDownTimer();
            } else {
                if (mIsRetryEnabled) {
                    if (mRetried < mRetry) {
                        Log.i(TAG, "retried :" + mRetried);
                        mWifiConnection.connectToWifi(mSSID, mPassword);
                        mRetried++;
                    } else if (mClientSocketServiceListener != null) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    mClientSocketServiceListener.onError(ClientSocketServiceListener.ERRORS.MAX_RETRY_EXCEED);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }
            }
        }

        @Override
        public void onWifiDisconnected(final String ssid) {
            Log.i(TAG, "onWifiDisconnected :" + ssid);
            if (mClientSocketServiceListener != null && ssid.equals(mSSID)) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mClientSocketServiceListener.onDisconnected();
                    }
                });
            }
        }

        @Override
        public void onWifiConnecting() {
            if (mClientSocketServiceListener != null) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mClientSocketServiceListener.onWifiConnecting(mSSID);
                    }
                });
            }
        }

        @Override
        public void onError(final WIFI_ERROR wifi_error) {
            mIsRetryEnabled = false;
            stopCountDownTimer();
            if (mClientSocketServiceListener != null) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            switch (wifi_error) {
                                case AP_MODE_ON:
                                    mClientSocketServiceListener.onError(ClientSocketServiceListener.ERRORS.AP_MODE_ON);
                                    break;
                                case SSID_NOT_FOUND:
                                    mClientSocketServiceListener.onError(ClientSocketServiceListener.ERRORS.SSID_NOT_FOUND);
                                    break;
                                case AUTHENTICATING_ERROR:
                                    mClientSocketServiceListener.onError(ClientSocketServiceListener.ERRORS.AUTHENTICATING_ERROR);
                                    break;
                                case INTERNAL_ERROR:
                                    mClientSocketServiceListener.onError(ClientSocketServiceListener.ERRORS.INTERNAL_ERROR);
                                    break;
                                case MINIMUM_PASSWORD_LENGTH_EIGHT:
                                    mClientSocketServiceListener.onError(ClientSocketServiceListener.ERRORS.MINIMUM_PASSWORD_LENGTH_EIGHT);
                                    break;
                               /* case WIFI_DISABLED:
                                    break;
                                case WIFI_NOT_CONNECTED:
                                    break;*/

                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }

        @Override
        public void onLocationServiceOff() {
            if (mClientSocketServiceListener != null) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mClientSocketServiceListener.onError(ClientSocketServiceListener.ERRORS.LOCATION_SERVICE_OFF);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    };

    private void connect(String ssid, String password, String ipAddress, int port, int retry, int timeout) {
        mSSID = ssid;
        mPassword = password;
        mIpAddress = ipAddress;
        mPort = port;
        mRetry = retry;
        mIsRetryEnabled = true;
        mRetried = 0;
        startWifiConnection();
    }

    public void connect(String ssid, String password, String ipAddress, int port, int retry) {
        this.connect(ssid,password,ipAddress,port,retry,2);
        startWifiConnection();
    }
    public void connect(String ssid, String password, String ipAddress, int port) {
        this.connect(ssid,password,ipAddress,port,1,2);
        startWifiConnection();
    }

    private void startSocketConnection() {
        mClientSocketConnection.openSocket(mIpAddress, mPort);
    }

    private void startWifiConnection() {
        if (mWifiConnection.isWifiConnectedTo(mSSID)) {
            startSocketConnection();
        } else {
            mWifiConnection.connectToWifi(mSSID, mPassword);
        }
    }

    public void getWifiScanList() {
        try {
            mWifiConnection.getWifiScanList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void write(String data) {
        try {
            mClientSocketConnection.writeData(ByteBuffer.wrap(data.getBytes(Charset.forName("UTF-8"))));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        try {
            mClientSocketConnection.closeSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void registerClientSocketServiceStatus(ClientSocketServiceListener clientSocketServiceListener) {
        this.mClientSocketServiceListener = clientSocketServiceListener;
        mClientSocketConnection.registerSocketConnectionListener(socketConnectionListener);
        mWifiConnection.registerWifiStatusListener(wifiConnectionListener);
    }

    public void registerWifiScanListener(final WifiScanListener wifiScanListener) {
        if (wifiScanListener != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    mWifiConnection.registerWifiScanListener(wifiScanListener);
                }
            });
        }
    }

    public void unRegisterWifiScanListener() {
        mWifiConnection.unRegisterWifiScanListener();
    }

    public void unRegisterClientSocketServiceStatus() {
        this.mClientSocketServiceListener = null;
        mClientSocketConnection.unRegisterSocketConnectionListener();
        mWifiConnection.unRegisterWifiStatusListener();
    }

    private void startCountDownTimer(int time) {
        countDownTimer = new CountDownTimer(time, time) {

            public void onTick(long millisUntilFinished) {

            }

            public void onFinish() {
                if (mClientSocketServiceListener != null) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mClientSocketServiceListener.onError(ClientSocketServiceListener.ERRORS.TIME_OUT);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }

            }
        };
        countDownTimer.start();
    }

    private void stopCountDownTimer() {
        if (countDownTimer != null) {

        }
    }

    public class ClientSocketServiceBinder extends Binder {
        public ClientSocketService getService() {
            return ClientSocketService.this;
        }
    }
}
