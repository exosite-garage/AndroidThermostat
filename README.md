## Android Thermostat

This Android app demonstrates connectivity to the One Platform via the [RPC API](https://github.com/exosite/api/tree/master/rpc). It displays a thermostat-like interface, with an ambient temperature indicator and slider for adjusting the temperature set point. Temperature values are read from the One Platform every couple of seconds, and set point is sent to the platform when the slider is adjusted.

### Usage

To use this app you need a device on Exosite's One Platform. At minimum the demo device needs to be configured as follows:

```
dataports:
    - alias: temp
      format: float
    - alias: setpoint
      format: float
```

You can use Portals to create this device, or Exosite's command line tool [Exoline](https://github.com/exosite/exoline). Here's how to create a temporary device and set it up with the correct schema:

```
$ exo spec <YOUR-DEVICE-CIK> https://raw.githubusercontent.com/exosite-garage/AndroidExample/master/thermostat.spec --create 
Running spec on: ee95d26abb3663cd039b89a82ccd1432e0e1061b
```

Once you have a CIK with the dataports specified above, enter that CIK in the demo app under Settings->Device CIK. Once this is done, you should see data from the temp and setpoint dataports. 

<img src="https://raw.githubusercontent.com/exosite-garage/AndroidExample/master/images/screenshot.png" align="left" width="220">

To change the logo, go to Settings and enter the URL of a 691x135 image. URL shorteners are not yet supported because the app does not yet know how to follow redirects. 

To revert to the Exosite logo, change the URL in settings to an empty string and then shut down and restart the app

### Build 

This application was built using Android Studio version 1.0.2. Here's how to get Android Studio. 

http://developer.android.com/sdk/index.html

Note that since Eclipse has a different project structure and build environment, using this project in Eclipse may require some manual steps.

1.) Clone the source

```
$ git clone git@github.com:exosite-garage/AndroidThermostat.git
```

2.) Set up an Android device for development over USB (enable developer options, enable USB debugging)

3.) Set up PC for debugging (this varies by platform, see http://developer.android.com/tools/device.html)

4.) Open the project with Android Studio (the `AndroidThermostat` folder)

5.) In Android Studio, select Build->Make Project

6.) Select Run->Run Demo

7.) Select your Android device. If you don't see your device, you may need to unplug it and plug it back in.

### Known Issues


### Release Guide

1.) Test (incl. in airplane mode and with incorrect CIK)

2.) Update version number in preferences.xml and AndroidManifest.xml

3.) Build -> Generate Signed APK 

4.) In preferences.xml, change `android:defaultValue="cb8d10a48650f62f6d56b01..."` to `android:defaultValue=DEFAULT_CIK`

5.) Commit and push in git

6.) Tag with version number and push that too
