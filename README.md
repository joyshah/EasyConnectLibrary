# EasyConnectLibrary
## How to include
 Add the library to your module **build.gradle**:
```Gradle
dependencies {
    implementation 'com.easyconnectlibrary.library:easyconnectlibrary:1.0.6'
}
```

## Usage
### Connect to Wifi 

```Java
WifiConnection wifiConnection = WifiConnection.getInstance(this);
wifiConnection.registerWifiStatusListener(new WifiConnectionListener() {
    @Override
    public void onWifiStateChanged(boolean isEnabled) {
      /*
      This callback will called whenever Wifi State is changed from ON to OFF or OFF to ON.
      */
    }

    @Override
    public void onWifiConnected(String ssid) {
     /*
     This callback will be called when device is connected to mentioned SSID/WIFI.
     */
    }

    @Override
    public void onWifiDisconnected(String ssid) {
     /*
     This callback will be called when device is disconnected from mentioned SSID/WIFI.
     */
    }

    @Override
    public void onWifiConnecting() {
     /*
      When you call connectToWifi method, the library start establishing connection with mentioned Wifi,
      at that point of time this callback will be called.
      After this callback, other callback namely onWifiConnected,onError or onLocationServiceOff callback 
      will be called base on results.
     */
    }

    @Override
    public void onError(WIFI_ERROR wifi_error) {
     //If any error occurs while connecting to WIFI this callback will be called.
    }

    @Override
    public void onLocationServiceOff() {
      /*
      In order to connect to wifi, device location service should be ON.
      In case it is not turned on and you are calling connectToWifi method of WifiConnection class,
      this callback will be called.
      */
    }
});
wifiConnection.connectToWifi("Your_Wifi_SSID","Your Wifi Password");
```

### Getting Wifi Scan Results 

```Java
WifiConnection wifiConnection = WifiConnection.getInstance(this);
wifiConnection.registerWifiScanListener(new WifiScanListener() {
    @Override
    public void onWifiScanList(List<ScanResult> scanResultList) {
     /*
       Once you call getWifiScanList method, this callback will be called giving wifi scan result.
     */
    }

    @Override
    public void onLocationServiceOff() {
     /*
      In order to get wifi scan result, device location service should be ON.
      In case it is not turned on and you are calling getWifiScanList method of WifiConnection class,
      this callback will be called.
     */
    }

    @Override
    public void onError(WIFI_SCAN_ERROR wifi_scan_error) {
     //If any error occurs while getting wifi scan result then this callback will be called.
    }
});
wifiConnection.getWifiScanList();
```
### Connecting to Wifi and Socket Server 

```Java
/*
...
...
*/
private ClientSocketService mClientSocketService;

@Override
protected void onCreate(Bundle savedInstanceState) {
    /*
    ...
    ...
    */
    Intent intent=new Intent();
    startService(intent);
    final ClientSocketServiceListener clientSocketServiceListener=new ClientSocketServiceListener() {
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
    };
    
    ServiceConnection mServiceConnection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ClientSocketService.ClientSocketServiceBinder myBinder = (ClientSocketService.ClientSocketServiceBinder) service;
            mClientSocketService = myBinder.getService();
            mClientSocketService.registerClientSocketServiceStatus(clientSocketServiceListener);
            
            //Connecting to Wifi and socket running on 192.168.0.11 IP and 8082 port.
            mClientSocketService.connect("MyWifiSSID","Password","192.168.0.11",8082);
        }
    
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mClientSocketService.unRegisterClientSocketServiceStatus();
        }
    };
    bindService(intent,mServiceConnection, BIND_AUTO_CREATE);

}
```


