package com.darkpos.usbdevice;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class UsbDeviceModule extends ReactContextBaseJavaModule {

    private static final String LOG_TAG = "UsbDevice";
    private static final String ACTION_USB_PERMISSION = "com.darkpos.usbdevice.UsbDevice.USB_PERMISSION";

    private final Context mContext;
    private final UsbManager mUsbManager;
    private final PendingIntent mPermissionIntent;
    private final Map<String, UsbDeviceAdapter> mUsbDeviceAdapters = new HashMap<String, UsbDeviceAdapter>();

    private final BroadcastReceiver mUsbDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    String deviceId = usbDevice != null ? String.valueOf(usbDevice.getDeviceId()) : "unknown";

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Log.i(LOG_TAG, "grant permission success for device: " + deviceId);
                        if (usbDevice != null) {
                            openUsbDeviceAdapter(usbDevice.getDeviceName());
                        }
                    } else {
                        Log.i(LOG_TAG, "grant permission failed for device: " + deviceId);
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                synchronized (this) {
                    UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    String deviceId = usbDevice != null ? String.valueOf(usbDevice.getDeviceId()) : "unknown";

                    Log.i(LOG_TAG, "device detached: " + deviceId);
                    if (usbDevice != null) {
                        closeUsbDeviceAdapter(usbDevice.getDeviceName());
                    }
                }
            }
        }
    };

    private List<UsbDevice> getUsbDevices() {
        if (mUsbManager == null) {
            return Collections.emptyList();
        }
        return new ArrayList<UsbDevice>(mUsbManager.getDeviceList().values());
    }

    private UsbDeviceAdapter getUsbDeviceAdapter(String deviceName) {
        if (!mUsbDeviceAdapters.containsKey(deviceName)) {
            return null;
        }
        return mUsbDeviceAdapters.get(deviceName);
    }

    private void openUsbDeviceAdapter(String deviceName) {
        UsbDeviceAdapter adapter = getUsbDeviceAdapter(deviceName);
        if (adapter != null) {
            adapter.open();
        }
    }

    private void closeUsbDeviceAdapter(String deviceName) {
        UsbDeviceAdapter adapter = mUsbDeviceAdapters.remove(deviceName);
        if (adapter != null) {
            adapter.close();
        }
    }

    public UsbDeviceModule(final ReactApplicationContext reactContext){
        super(reactContext);
        this.mContext = reactContext;
        this.mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);

        Intent intent = new Intent(ACTION_USB_PERMISSION);
        intent.setPackage(mContext.getPackageName()); 
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        this.mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, intent, flags);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            mContext.registerReceiver(mUsbDeviceReceiver, filter, mContext.RECEIVER_EXPORTED);
        } else {
            mContext.registerReceiver(mUsbDeviceReceiver, filter);
        }
        Log.v(LOG_TAG, "initialized");
    }

    @Override
    public void onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy();

        try {
            if (!mUsbDeviceAdapters.isEmpty()) {
                for (String deviceName : mUsbDeviceAdapters.keySet()) {
                    closeUsbDeviceAdapter(deviceName);
                }
                mUsbDeviceAdapters.clear();
                Log.v(LOG_TAG, "adapters closed and cleared");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "error closing adapters", e);
        }

        try {
            if (mContext != null && mUsbDeviceReceiver != null) {
                mContext.unregisterReceiver(mUsbDeviceReceiver);
                Log.v(LOG_TAG, "receiver unregistered");
            }
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "receiver not registered or already unregistered", e);
        }
    }

    @Override
    public String getName() {
        return "UsbDevice";
    }

    @ReactMethod
    public void getDevices(Promise promise) {
        Log.v(LOG_TAG, "get device list...");
        List<UsbDevice> usbDevices = getUsbDevices();
        WritableArray pairedDeviceList = Arguments.createArray();
        for (UsbDevice usbDevice : usbDevices) {
            boolean hasPermission = mUsbManager.hasPermission(usbDevice);

            WritableMap deviceMap = Arguments.createMap();
            deviceMap.putInt("deviceId", usbDevice.getDeviceId());
            deviceMap.putString("deviceName", usbDevice.getDeviceName());
            deviceMap.putInt("vendorId", usbDevice.getVendorId());
            deviceMap.putInt("productId", usbDevice.getProductId());
            deviceMap.putString("productName", usbDevice.getProductName());
            deviceMap.putBoolean("hasPermission", hasPermission);
            if (hasPermission) {
                deviceMap.putString("serialNumber", usbDevice.getSerialNumber());
                deviceMap.putString("manufacturerName", usbDevice.getManufacturerName());
            }
            pairedDeviceList.pushMap(deviceMap);
        }
        promise.resolve(pairedDeviceList);
    }

    @ReactMethod
    public void open(String deviceName, Promise promise) {
        Log.v(LOG_TAG, "open device "+deviceName);

        if (mUsbDeviceAdapters.containsKey(deviceName)) {
            Log.v(LOG_TAG, "selecting current device...");
            openUsbDeviceAdapter(deviceName);
            promise.resolve(true);
            return;
        }

        Log.v(LOG_TAG, "select new device...");
        List<UsbDevice> usbDevices = getUsbDevices();
        for(UsbDevice usbDevice: usbDevices) {
            if(usbDevice.getDeviceName().equals(deviceName)) {
                Log.v(LOG_TAG, "request for device "+usbDevice.getDeviceId()+", vendorId: " + usbDevice.getVendorId() + ", productId: " + usbDevice.getProductId());

                if (!mUsbManager.hasPermission(usbDevice)) {
                    mUsbManager.requestPermission(usbDevice, mPermissionIntent);
                    promise.resolve(false);
                    return;
                }

                UsbDeviceAdapter adapter = new UsbDeviceAdapter(mUsbManager, usbDevice);
                mUsbDeviceAdapters.put(deviceName, adapter);
                openUsbDeviceAdapter(deviceName);
                promise.resolve(true);
                return;
            }
        }

        promise.resolve(false);
    }

    @ReactMethod
    public void close(String deviceName, Promise promise) {
        closeUsbDeviceAdapter(deviceName);
        promise.resolve(null);
    }

    @ReactMethod
    public void send(String deviceName, String base64Data) {
        UsbDeviceAdapter adapter = getUsbDeviceAdapter(deviceName);
        if (adapter != null) {
            adapter.send(base64Data);
        }
    }

}

