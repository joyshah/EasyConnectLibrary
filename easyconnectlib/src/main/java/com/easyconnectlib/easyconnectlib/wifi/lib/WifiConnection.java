package com.easyconnectlib.easyconnectlib.wifi.lib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import com.easyconnectlib.easyconnectlib.wifi.callbacks.ApStatus;
import com.easyconnectlib.easyconnectlib.wifi.callbacks.WifiConnectionListener;
import com.easyconnectlib.easyconnectlib.wifi.callbacks.WifiScanListener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;


public class WifiConnection {
    private final String TAG = WifiConnection.class.getSimpleName();
    public static final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";
    private static WifiConnection ourInstance;
    private final WifiManager mWifiManager;
    private WifiConnectionListener mWifiConnectionListener;
    private WifiScanListener mWifiScanListener;
    private ApStatus mApStatus;
    private String mConnectedSSID;
    private boolean isConnectToWifiRunning;
    private Context mContext;

    private WifiConnection(Context context) {
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        isConnectToWifiRunning = false;
        mConnectedSSID = "NONE";
        this.mContext = context;
    }

    public static WifiConnection getInstance(Context context) {
        return ourInstance == null ? (ourInstance = new WifiConnection(context)) : ourInstance;
    }


    public String getConnectedSSID() {
        WifiInfo wifiInfo = getWifiInfo();
        if (wifiInfo != null && wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
            Log.i(TAG, "getConnectedSSID" + wifiInfo.getSSID());
            return wifiInfo.getSSID().replace("\"", "");
        } else {
            if (mWifiConnectionListener != null) {
                mWifiConnectionListener.onError(!mWifiManager.isWifiEnabled() ? WifiConnectionListener.WIFI_ERROR.WIFI_DISABLED : WifiConnectionListener.WIFI_ERROR.WIFI_NOT_CONNECTED);
            }
            return null;
        }
    }

    public String getConnectedRouterIP() {
        if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            DhcpInfo dhcpInfo = mWifiManager.getDhcpInfo();
            int serverAddress = dhcpInfo.serverAddress;
            return String.format(Locale.US, "%d.%d.%d.%d", (serverAddress & 0xff), (serverAddress >> 8 & 0xff), (serverAddress >> 16 & 0xff), (serverAddress >> 24 & 0xff));
        }
        return "";
    }

    public void getWifiScanList() {
        if (!isApModeOn()) {
            if (isLocationServiceOn()) {
                enableWiFi(true);
                IntentFilter scanResultsFilters = new IntentFilter();
                scanResultsFilters.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
                mContext.registerReceiver(mWifiScanReceiver, scanResultsFilters);
                mWifiManager.startScan();
            } else {
                if (mWifiScanListener != null)
                    mWifiScanListener.onLocationServiceOff();
            }
        } else {
            if (mWifiScanListener != null)
                mWifiScanListener.onError(WifiScanListener.WIFI_SCAN_ERROR.AP_MODE_ON);
        }
    }


    public boolean isApModeOn() {
        boolean isWifiAPEnabled = false;
        Method[] wmMethods = mWifiManager.getClass().getDeclaredMethods();
        for (Method method : wmMethods) {
            if (method.getName().equals("isWifiApEnabled")) {
                try {
                    isWifiAPEnabled = (boolean) method.invoke(mWifiManager);
                } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        return isWifiAPEnabled;
    }

    public boolean isWifiConnectedTo(String ssid) {
        if (isApModeOn()) {
            return false;
        }
        String connectedSSID = getConnectedSSID();
        return (connectedSSID != null && connectedSSID.equalsIgnoreCase(ssid));
    }

    public boolean isWifiEnable() {
        return mWifiManager != null && mWifiManager.isWifiEnabled();
    }

    private boolean isLocationServiceOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final LocationManager manager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } else {
            return true;
        }
    }


    public void enableWiFi(boolean toEnable) {
        if (!mWifiManager.isWifiEnabled() && toEnable) {
            mWifiManager.setWifiEnabled(true);
        } else if (mWifiManager.isWifiEnabled() && !toEnable) {
            mWifiManager.setWifiEnabled(false);
        }
    }

    public void registerWifiStatusListener(WifiConnectionListener wifiConnectionListener) {
        mWifiConnectionListener = wifiConnectionListener;
        setWifiCallback();
    }

    public void registerApStatusListener(ApStatus apStatus) {
        mApStatus = apStatus;
        setApCallback();
    }

    public void registerWifiScanListener(WifiScanListener wifiScanListener) {
        this.mWifiScanListener = wifiScanListener;
    }

    public void unRegisterWifiScanListener() {
        this.mWifiScanListener = null;
    }

    public void unRegisterWifiStatusListener() {
        mWifiConnectionListener = null;
        unSetWifiCallback();
    }

    public void unregisterApStatusListener() {
        mApStatus = null;
        unSetApCallback();
    }

    public void forgetNetwork(String ssid) {
        List<WifiConfiguration> list = mWifiManager.getConfiguredNetworks();
        if (isWifiConnectedTo(ssid))
            disconnectFrom(ssid);

        if (list != null) {
            for (WifiConfiguration wifiConfiguration : list) {
                if (wifiConfiguration.SSID.replace("\"", "").equals(ssid)) {
                    mWifiManager.removeNetwork(wifiConfiguration.networkId);
                    mWifiManager.saveConfiguration();
                }
            }
        }
    }

    public void forgetConnectedNetwork() {
        String ssid = getConnectedSSID();
        List<WifiConfiguration> list = mWifiManager.getConfiguredNetworks();
        disconnectFrom(ssid);
        for (WifiConfiguration wifiConfiguration : list) {
            if (wifiConfiguration.SSID.replace("\"", "").equals(ssid)) {
                mWifiManager.removeNetwork(wifiConfiguration.networkId);
                mWifiManager.saveConfiguration();
            }
        }
    }

    public boolean disconnectFrom(String ssid) {
        return isWifiConnectedTo(ssid) && mWifiManager.disconnect();
    }


    public void connectToWifi(String ssid) {
        connectToWifi(ssid, null);
    }

    public void connectToWifi(final String ssid, final String password) {
        Log.i(TAG, "thread status" + isConnectToWifiRunning);
        if (!isConnectToWifiRunning) {
            isConnectToWifiRunning = true;
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "thread status" + isConnectToWifiRunning);
                    try {
                        if (!isApModeOn()) {
                            if (isLocationServiceOn()) {

                                if (mWifiConnectionListener != null)
                                    mWifiConnectionListener.onWifiConnecting();

                                //makeWifiStable(): It will enable wifi if not and allow the OS to connect to previous wifi network
                                if (!mWifiManager.isWifiEnabled()) {
                                    mWifiManager.setWifiEnabled(true);
                                    try {
                                        Thread.sleep(7000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }

                                if (!isWifiConnectedTo(ssid)) {


                                    ScanResult scanResult = getNetworkObject(ssid);

                                    if (scanResult != null) {
                                        String mode = getSecurityMode(scanResult);

                                        forgetNetwork(ssid);
                                        WifiConfiguration config = new WifiConfiguration();
                                        config.SSID = "\"".concat(ssid).concat("\"");
                                        config.status = WifiConfiguration.Status.DISABLED;

                                        switch (mode) {
                                            case "OPEN":
                                                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                                                break;
                                            case "WEP":
                                                config.wepKeys[0] = "\"" + password + "\"";
                                                config.wepTxKeyIndex = 0;
                                                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                                                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                                                break;
                                            case "WPA":
                                            case "EAP":
                                                if (password == null || password.length() < 8) {
                                                    if (mWifiConnectionListener != null)
                                                        mWifiConnectionListener.onError(WifiConnectionListener.WIFI_ERROR.MINIMUM_PASSWORD_LENGTH_EIGHT);
                                                    return;
                                                }
                                              /*  config.hiddenSSID = true;
                                                config.status = WifiConfiguration.Status.ENABLED;
                                                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                                                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);*/
                                                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                                                /*config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                                                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                                                config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                                                config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);*/
                                                config.preSharedKey = "\"" + password + "\"";
                                                break;

                                        }

                                        int res = mWifiManager.addNetwork(config);
                                        mWifiManager.saveConfiguration();
                                        if (res == -1) {
                                            //Internal error from android, as we are unable to add wifi configuration in WIFIMANAGER
                                            Log.e(TAG, "addNetwork: returns -1");
                                            if (mWifiConnectionListener != null)
                                                mWifiConnectionListener.onError(WifiConnectionListener.WIFI_ERROR.INTERNAL_ERROR);
                                            return;
                                        } else {
                                            mWifiManager.disconnect();
                                            mWifiManager.enableNetwork(res, true);
                                            mWifiManager.reconnect();

                                        }
                                    } else {
                                        if (mWifiConnectionListener != null)
                                            mWifiConnectionListener.onError(WifiConnectionListener.WIFI_ERROR.SSID_NOT_FOUND);
                                    }
                                } else {
                                    if (mWifiConnectionListener != null)
                                        mWifiConnectionListener.onWifiConnected(ssid);
                                }
                            } else {
                                if (mWifiConnectionListener != null)
                                    mWifiConnectionListener.onLocationServiceOff();
                            }
                        } else {
                            if (mWifiConnectionListener != null)
                                mWifiConnectionListener.onError(WifiConnectionListener.WIFI_ERROR.AP_MODE_ON);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (mWifiConnectionListener != null)
                            mWifiConnectionListener.onError(WifiConnectionListener.WIFI_ERROR.INTERNAL_ERROR);
                    } finally {
                        isConnectToWifiRunning = false;
                        Log.i(TAG, "thread status" + isConnectToWifiRunning);
                    }
                }
            });
            thread.start();
        } else {
            Log.i(TAG, "thread already running");
        }
    }


    //For creating open hotspot network.
    public void createHotSpot(String ssid) {
        createHotSpot(ssid, null);
    }

    //Open hotspot will be created if password is null.
    public void createHotSpot(String ssid, String password) {
        switch (mWifiManager.getWifiState()) {
            case WifiManager.WIFI_STATE_ENABLED:
            case WifiManager.WIFI_STATE_ENABLING:
            case WifiManager.WIFI_STATE_UNKNOWN:
                /*  mWifiManager.setWifiEnabled(false);*/
                if (mApStatus != null)
                    mApStatus.onError(ApStatus.AP_ERROR.WIFI_STATE_FOUND_ON);
                return;
        }
        if (mApStatus != null)
            mApStatus.onApModeStarting();

        Method[] mMethods = mWifiManager.getClass().getDeclaredMethods();
        for (Method mMethod : mMethods) {
            if (mMethod.getName().equals("setWifiApEnabled")) {
                WifiConfiguration netConfig = new WifiConfiguration();
                if (password == null || password.equals("")) {
                    netConfig.SSID = ssid;
                    netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                    netConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                    netConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                    netConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                } else if (password.length() >= 8) {
                    netConfig.SSID = ssid;
                    netConfig.preSharedKey = password;
                    netConfig.hiddenSSID = true;
                    netConfig.status = WifiConfiguration.Status.ENABLED;
                    netConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                    netConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                    netConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    netConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                    netConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                    netConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                    netConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                } else {
                    if (mApStatus != null)
                        mApStatus.onError(ApStatus.AP_ERROR.MINIMUM_PASSWORD_LENGTH_EIGHT);
                    return;
                }
                try {
                    mMethod.invoke(mWifiManager, netConfig, true);
                    if (mApStatus != null)
                        mApStatus.onApModeStarted(ssid);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (mApStatus != null)
                        mApStatus.onError(ApStatus.AP_ERROR.INTERNAL_ERROR);
                }
                break;
            }
        }
    }

    public void turnHotSpot(boolean enable) {
        String ssidHotSpot = "NONE";

        switch (mWifiManager.getWifiState()) {
            case WifiManager.WIFI_STATE_ENABLED:
            case WifiManager.WIFI_STATE_ENABLING:
            case WifiManager.WIFI_STATE_UNKNOWN:
                /*  mWifiManager.setWifiEnabled(false);*/
                if (mApStatus != null)
                    mApStatus.onError(ApStatus.AP_ERROR.WIFI_STATE_FOUND_ON);
                return;
        }

        if (mApStatus != null)
            mApStatus.onApModeStarting();

        Method[] mMethods = mWifiManager.getClass().getDeclaredMethods();
        for (Method mMethod : mMethods) {
            if (mMethod.getName().equals("getWifiApConfiguration")) {
                try {
                    WifiConfiguration config = (WifiConfiguration) mMethod.invoke(mWifiManager);
                    ssidHotSpot = config.SSID;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    if (mApStatus != null)
                        mApStatus.onError(ApStatus.AP_ERROR.INTERNAL_ERROR);
                }
                break;
            }
        }
        for (Method mMethod : mMethods) {
            if (mMethod.getName().equals("setWifiApEnabled")) {
                try {
                    mMethod.invoke(mWifiManager, null, enable);
                    if (mApStatus != null)
                        mApStatus.onApModeStarted(ssidHotSpot);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    if (mApStatus != null)
                        mApStatus.onError(ApStatus.AP_ERROR.INTERNAL_ERROR);
                }
                break;
            }
        }
    }

    private final BroadcastReceiver mWifiConnectivityState = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            String action = intent.getAction();

            switch (action) {
                case WifiManager.WIFI_STATE_CHANGED_ACTION:
                    if (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN) == WifiManager.WIFI_STATE_ENABLED) {
                        Log.i(TAG, "Wifi Enable");
                        if (mWifiConnectionListener != null)
                            mWifiConnectionListener.onWifiStateChanged(true);
                    } else if (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN) == WifiManager.WIFI_STATE_DISABLED) {
                        Log.i(TAG, "wifi disabled");
                        if (mWifiConnectionListener != null)
                            mWifiConnectionListener.onWifiStateChanged(false);
                    }
                    break;
                case ConnectivityManager.CONNECTIVITY_ACTION:
                    NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                        switch (networkInfo.getState()) {
                            case CONNECTED:
                                if (getWifiInfo() != null)
                                    mConnectedSSID = getWifiInfo().getSSID();
                                Log.i(TAG, "Wifi connected:" + mConnectedSSID);
                                if (mWifiConnectionListener != null) {
                                    mWifiConnectionListener.onWifiConnected(mConnectedSSID.replace("\"", ""));
                                }
                                break;
                            case CONNECTING:
                                Log.i(TAG, "Wifi connecting:" + getWifiInfo().getSSID());
                                if (mWifiConnectionListener != null)
                                    mWifiConnectionListener.onWifiConnecting();
                                break;
                            case DISCONNECTED:
                                Log.i(TAG, "Wifi disconnected:" + mConnectedSSID);
                                if (mWifiConnectionListener != null)
                                    mWifiConnectionListener.onWifiDisconnected(mConnectedSSID);
                                break;
                            case DISCONNECTING:
                                Log.i(TAG, "Wifi disconnecting");
                                break;
                            case SUSPENDED:
                            case UNKNOWN:
                                Log.i(TAG, "Suspended or unknown error occurred");
                                if (mWifiConnectionListener != null)
                                    mWifiConnectionListener.onError(WifiConnectionListener.WIFI_ERROR.INTERNAL_ERROR);
                                break;
                        }
                    }
                    break;
                case WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION:
                    NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf((SupplicantState)
                            intent.getParcelableExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED));
                    Log.i(TAG, "DetailedState" + state);
               /*      switch (state) {
                        case AUTHENTICATING:
                            break;
                        case BLOCKED:
                            break;
                        case CAPTIVE_PORTAL_CHECK:
                            break;
                        case CONNECTED:
                            break;
                        case CONNECTING:
                            break;
                        case DISCONNECTED:
                            break;
                        case DISCONNECTING:
                            break;
                        case FAILED:
                            break;
                        case IDLE:
                            break;
                        case OBTAINING_IPADDR:
                            break;
                        case SCANNING:
                            break;
                        case SUSPENDED:
                            break;
                        case VERIFYING_POOR_LINK:
                            break;
                    }*/
                    break;
                case WifiManager.SUPPLICANT_STATE_CHANGED_ACTION:
                    SupplicantState supplicantState = ((SupplicantState) intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE));
                    switch (supplicantState) {
                        case ASSOCIATED:
                            Log.i(TAG, "ASSOCIATED");
                            break;
                        case ASSOCIATING:
                            Log.i(TAG, "ASSOCIATING");
                            break;
                        case AUTHENTICATING:
                            Log.i(TAG, "Authenticating...");
                            break;
                        case COMPLETED:
                            Log.i(TAG, "Connected");
                            break;
                        case DISCONNECTED:
                            Log.i(TAG, "Disconnected");
                            break;
                        case DORMANT:
                            Log.i(TAG, "DORMANT");
                            break;
                        case FOUR_WAY_HANDSHAKE:
                            Log.i(TAG, "FOUR_WAY_HANDSHAKE");
                            break;
                        case GROUP_HANDSHAKE:
                            Log.i(TAG, "GROUP_HANDSHAKE");
                            break;
                        case INACTIVE:
                            Log.i(TAG, "INACTIVE");
                            break;
                        case INTERFACE_DISABLED:
                            Log.i(TAG, "INTERFACE_DISABLED");
                            break;
                        case INVALID:
                            Log.i(TAG, "INVALID");
                            break;
                        case SCANNING:
                            Log.i(TAG, "SCANNING");
                            break;
                        case UNINITIALIZED:
                            Log.i(TAG, "UNINITIALIZED");
                            break;
                        default:
                            Log.i(TAG, "Unknown");
                            break;
                    }
                    int suplError = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
                    if (suplError == WifiManager.ERROR_AUTHENTICATING) {
                        Log.i(TAG, "ERROR_AUTHENTICATING!");
                        if (mWifiConnectionListener != null)
                            mWifiConnectionListener.onError(WifiConnectionListener.WIFI_ERROR.AUTHENTICATING_ERROR);
                    }
                    break;
                case WIFI_AP_STATE_CHANGED_ACTION:
                    switch (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)) {
                        case 11/*Wifi ap state disabled*/:
                            Log.i(TAG, "Wifi ap state disabled!");
                            break;
                        case 13/*Wifi ap state enabled*/:
                            Log.i(TAG, "Wifi ap state enabled!");
                            break;
                        case 14/*wifi ap state failed*/:
                            Log.i(TAG, "wifi ap state failed*!");
                            break;
                    }
                    break;
            }
        }
    };

    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:
                    Log.i(TAG, "Wifi scan result");
                    if (mWifiScanListener != null)
                        mWifiScanListener.onWifiScanList(mWifiManager.getScanResults());
                    mContext.unregisterReceiver(mWifiScanReceiver);
                    break;
            }
        }
    };

    private final BroadcastReceiver mApStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case WIFI_AP_STATE_CHANGED_ACTION:
                    switch (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)) {
                        case 11/*Wifi ap state disabled*/:
                            Log.i(TAG, "Wifi ap state disabled!");
                            if (mApStatus != null)
                                mApStatus.onApModeStopped();
                            break;
                        case 13/*Wifi ap state enabled*/:
                            Log.i(TAG, "Wifi ap state enabled!");
                            break;
                        case 14/*wifi ap state failed*/:
                            Log.i(TAG, "wifi ap state failed*!");
                            if (mApStatus != null)
                                mApStatus.onError(ApStatus.AP_ERROR.INTERNAL_ERROR);
                            break;
                    }
                    break;
            }
        }
    };

    private List<ScanResult> getAvailableNetworkList() {
        if (isLocationServiceOn()) {
            enableWiFi(true);
            mWifiManager.startScan();
            synchronized (mWifiManager) {
                pause(mWifiManager, 3000);
                return mWifiManager.getScanResults();
            }
        } else {
            if (mWifiConnectionListener != null)
                mWifiConnectionListener.onLocationServiceOff();
            return null;
        }
    }

    private WifiInfo getWifiInfo() {
        WifiInfo info = mWifiManager.getConnectionInfo();
        NetworkInfo networkInfo = ((ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        boolean isConnected = networkInfo != null && networkInfo.isConnected();
        if (info != null && isConnected) {
            return info;
        } else {
            return null;
        }
    }

    private void setWifiCallback() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        // intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        mContext.registerReceiver(mWifiConnectivityState, intentFilter);
    }

    private void unSetWifiCallback() {
        mContext.unregisterReceiver(mWifiConnectivityState);
    }

    private void setApCallback() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WIFI_AP_STATE_CHANGED_ACTION);
        mContext.registerReceiver(mApStateReceiver, intentFilter);
    }

    private void unSetApCallback() {
        mContext.unregisterReceiver(mApStateReceiver);
    }

    private void pause(Object object, int timeout) {
        synchronized (object) {
            try {
                object.wait(timeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void resume(Object object) {
        try {
            synchronized (object) {
                object.notifyAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isNetworkAvailable(String ssid) {
        List<ScanResult> scanResults = getAvailableNetworkList();
        if (scanResults != null) {
            for (ScanResult sc : scanResults) {
                if ((sc.SSID).equals(ssid)) {
                    return true;
                }
            }
        }
        return false;
    }

    private ScanResult getNetworkObject(String ssid) {
        List<ScanResult> scanResults = getAvailableNetworkList();
        if (scanResults != null) {
            for (ScanResult sc : scanResults) {
                if ((sc.SSID).equals(ssid)) {
                    return sc;
                }
            }
        }
        return null;
    }

    private String getSecurityMode(ScanResult scanResult) {
        final String cap = scanResult.capabilities;
        final String[] modes = {"WPA", "EAP", "WEP"};
        for (int i = modes.length - 1; i >= 0; i--) {
            if (cap.contains(modes[i])) {
                return modes[i];
            }
        }
        return "OPEN";
    }

}


