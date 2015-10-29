## intellij-androidhost
This is the client to Android Host. It provides a GUI that can be called up using a button in Android Studio. The server can be found [here](https://github.com/duncanleo/go-androidhost).

### What is Android Host?
The Android Emulator is the most versatile emulator available - you can set any screen size, and run versions of Android such as Froyo (2.2) and Marshmallow (6.0). However, it's also the slowest, even with Intel HAXM acceleration.

Android Host allows you to run the Android Emulator on a separate machine. The server runs on that separate machine and provides remote deployment and control.

I built this during my time as an intern at [buUuk](http://www.buuuk.com), where the Android developers had to test their apps on the few devices in the office. Sometimes, bugs occurred on older versions of Android of which none of the devices ran.

### Features
- Remote starting of AVDs
- Deploy to any ADB device connected to the server machine, including USB devices
- Discovery through UDP - you can deploy to multiple servers in the same LAN

## Installation
### Easy way
1. Download the JAR file from the 'Releases' section in the repo.
2. Open Android Studio
3. Go to Preferences > Plugins
4. Select 'Install plugin from disk'
5. Choose the downloaded JAR file
6. Click 'OK'

### Complicated way
1. Clone this project
2. Open the directory in IntelliJ (not Android Studio!)
3. Select Build > Prepare plugin module for deployment
4. A JAR file will be produced in the directory root.
