# Ergom_launcher
Kiosk launcher for my company.

Simple app that blocks Android system and give access only to Google Chrome.

On mainpage there are 3 shortcuts to URLs. 

Administrator can leave kiosk mode through shortcut (5 times click on IP in the right bottom corner) with password that is hardcoded.

App boots with system.

Usefull commands:

adb shell dpm set-device-owner com.ergom.launcher/.MyDeviceAdminReceiver

adb shell dpm remove-active-admin com.ergom.launcher/.MyDeviceAdminReceiver

adb uninstall com.ergom.launcher
