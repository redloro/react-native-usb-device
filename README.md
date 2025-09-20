# react-native-usb-device

A React Native Library to support USB devices for Android platform 

## Installation

```
npm install react-native-usb-device --save
```

## Integrate module

To integrate `react-native-usb-device` with the rest of your react app just execute:
```
react-native link react-native-usb-device
```

## Usage

```javascript
import { UsbDevice } from "react-native-usb-device";

await UsbDevice.getDevices();
const device = await UsbDevice.open("/usb/path/1");
device.sendText("This is test print.")
device.send("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEhlbGxvIHdvcmxkIRtkCRtpG0A=")
```
