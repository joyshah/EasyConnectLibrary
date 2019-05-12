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
      In case it is not turned on and you are calling connectToWifi method of wifiConnection class,
      this callback will be called.
      */
    }
});
wifiConnection.connectToWifi("Your_Wifi_SSID","Your Wifi Password");
```
