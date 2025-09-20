import { NativeModules, Platform } from 'react-native';

const { UsbDevice:UsbDeviceModules } = NativeModules;

class UsbDeviceAdapter {
  constructor(deviceName) {
    this.deviceName = deviceName;
  }

  /**
   * @return {string} device name
   */
  getDeviceName() {
    return this.deviceName;
  }

  /**
   * Send base64 data
   * @param {string} base64 data
   */
  send(base64Data) {
    return UsbDeviceModules.send(this.deviceName, base64Data);
  }

  /**
   * Close serial port
   */
  close() {
    return UsbDeviceModules.close(this.deviceName);
  }
}

export default class UsbDevice {

  /**
   * Get usb devices
   * @returns {Promise<Array<string>>} devices
   */
  static getDevices() {
    if (Platform.OS !== 'android') throw new Error(`Unsupported platform: ${Platform.OS}`)
    return UsbDeviceModules.getDevices();
  }

  /**
   * Open usb device
   * @param {string} deviceName device name
   * @returns {Promise<UsbDeviceAdapter>} connected usb device
   */
  static open(deviceName) {
    if (Platform.OS !== 'android') throw new Error(`Unsupported platform: ${Platform.OS}`)
    return UsbDeviceModules.open(deviceName)
      .then((success) => {
        return Promise.resolve(success ? new UsbDeviceAdapter(deviceName) : null);
      })
  }
}