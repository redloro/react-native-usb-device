package com.darkpos.usbdevice;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.Charset;

import java.util.List;

public class UsbDeviceAdapter {
    private static final String LOG_TAG = "UsbDevice";

    private final UsbManager mUsbManager;
    private final UsbDevice mUsbDevice;
    private UsbDeviceConnection mUsbDeviceConnection;
    private UsbInterface mUsbInterface;
    private UsbEndpoint mEndPoint;
    
    public UsbDeviceAdapter(final UsbManager usbManager, final UsbDevice usbDevice) {
        this.mUsbManager = usbManager;
        this.mUsbDevice = usbDevice;
    }

    public void open() {
        if (mUsbDeviceConnection != null) {
            return;
        }

        if (mUsbManager == null) {
            Log.e(LOG_TAG, "usb manager is not initialized");
            return;
        }

        if (mUsbDevice == null){
            Log.e(LOG_TAG, "usb device is not initialized");
            return;
        }

        close();

        UsbInterface usbInterface = mUsbDevice.getInterface(0);
        for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
            final UsbEndpoint ep = usbInterface.getEndpoint(i);
            if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                    UsbDeviceConnection usbDeviceConnection = mUsbManager.openDevice(mUsbDevice);
                    if (usbDeviceConnection == null) {
                        Log.e(LOG_TAG, "failed to open usb device connection");
                        return;
                    }

                    if (usbDeviceConnection.claimInterface(usbInterface, true)) {
                        mEndPoint = ep;
                        mUsbInterface = usbInterface;
                        mUsbDeviceConnection = usbDeviceConnection;
                        return;
                    } else {
                        usbDeviceConnection.close();
                        Log.e(LOG_TAG, "failed to claim usb connection");
                        return;
                    }
                }
            }
        }
    }

    public void close() {
        if (mUsbDeviceConnection == null) {
            return;
        }

        mUsbDeviceConnection.releaseInterface(mUsbInterface);
        mUsbDeviceConnection.close();
        mUsbInterface = null;
        mEndPoint = null;
        mUsbDeviceConnection = null;
    }

    public boolean send(String base64Data) {
        Log.v(LOG_TAG, "calling send");
        final String data = base64Data;

        new Thread(new Runnable() {
            @Override
            public void run() {
                byte [] bytes = Base64.decode(data, Base64.DEFAULT);

                if (mUsbDeviceConnection == null) {
                    Log.v(LOG_TAG, "usb device connection is not open");
                    return;
                }

                try {
                    int b = mUsbDeviceConnection.bulkTransfer(mEndPoint, bytes, bytes.length, 100000);
                    Log.i(LOG_TAG, "send bulk transfer result: " + b);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "send error during bulk transfer: " + e.getMessage(), e);
                }
            }
        }).start();
        return true;
    }

}
