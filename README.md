## Portals for Android

This Android app demonstrates connectivity to the One Platform via the [RPC API](https://github.com/exosite/docs/tree/master/rpc) and Portals API.

### Usage

TODO

### Build 

This application was built using Android Studio version 0.4.2.
http://developer.android.com/sdk/installing/studio.html

1.) Clone the source

```
$ git clone git@github.com:exosite-garage/AndroidDemo.git DemoProject
```

2.) Set up an Android device for development over USB (enable developer options, enable USB debugging)

3.) Set up PC for debugging (this varies by platform, see http://developer.android.com/tools/device.html)

4.) Open the project with Android Studio

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

### TODOs

- add timestamp to resource last value display. Maybe something like "n weeks/days/hours/minutes ago"
