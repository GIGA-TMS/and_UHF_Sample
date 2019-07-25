package com.gigatms.ts800;

import com.gigatms.BaseDevice;
import com.gigatms.tools.GLog;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;


public class ConnectedDevices extends HashMap<String, BaseDevice> {
    private static final String TAG = ConnectedDevices.class.getSimpleName();
    private static volatile ConnectedDevices instance;

    private ConnectedDevices() {
    }

    static ConnectedDevices getInstance() {
        if (instance == null) {
            synchronized (ConnectedDevices.class) {
                if (instance == null) {
                    instance = new ConnectedDevices();
                }
            }
        }
        return instance;
    }

    @Override
    public BaseDevice put(String key, BaseDevice value) {
        GLog.d(TAG, Arrays.toString(ConnectedDevices.getInstance().keySet().toArray()));
        GLog.d(TAG, "put: " + key);
        return super.put(key, value);
    }

    @Override
    public BaseDevice remove(Object key) {
        GLog.d(TAG, Arrays.toString(ConnectedDevices.getInstance().keySet().toArray()));
        GLog.d(TAG, "remove: " + key);
        return super.remove(key);
    }

    public void clear(String deviceMacAddress) {
        GLog.d(TAG, "clear: " + deviceMacAddress);
        Objects.requireNonNull(instance.get(deviceMacAddress)).disconnect();
        Objects.requireNonNull(instance.get(deviceMacAddress)).destroy();
        remove(deviceMacAddress);
        GLog.d(TAG, Arrays.toString(ConnectedDevices.getInstance().keySet().toArray()));
    }

    @Override
    public void clear() {
        GLog.d(TAG, "clear all");
        for (String deviceMac : instance.keySet()) {
            Objects.requireNonNull(instance.get(deviceMac)).disconnect();
            Objects.requireNonNull(instance.get(deviceMac)).destroy();
        }
        super.clear();
        GLog.d(TAG, Arrays.toString(ConnectedDevices.getInstance().keySet().toArray()));
    }
}
