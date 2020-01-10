package com.gigatms.uhf;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.gigatms.BaseDevice;
import com.gigatms.BaseScanner;
import com.gigatms.CommunicationType;
import com.gigatms.UHF.UhfClassVersion;
import com.gigatms.UHFScanner;
import com.gigatms.uhf.deviceControl.MU400HDeviceControlFragment;
import com.gigatms.uhf.deviceControl.NR800DeviceControlFragment;
import com.gigatms.uhf.deviceControl.PWD100DeviceControlFragment;
import com.gigatms.uhf.deviceControl.TS100ADeviceControlFragment;
import com.gigatms.uhf.deviceControl.TS100DeviceControlFragment;
import com.gigatms.uhf.deviceControl.TS800DeviceControlFragment;
import com.gigatms.uhf.deviceControl.UR0250DeviceControlFragment;
import com.gigatms.tools.GLog;

import java.util.Arrays;
import java.util.Objects;

import static com.gigatms.CommunicationType.BLE;
import static com.gigatms.CommunicationType.UDP;
import static com.gigatms.CommunicationType.USB;
import static com.gigatms.UHF.UhfClassVersion.MU400H;
import static com.gigatms.UHF.UhfClassVersion.NR800;
import static com.gigatms.UHF.UhfClassVersion.PWD100;
import static com.gigatms.UHF.UhfClassVersion.TS100;
import static com.gigatms.UHF.UhfClassVersion.TS100A;
import static com.gigatms.UHF.UhfClassVersion.TS800;
import static com.gigatms.UHF.UhfClassVersion.UR0250;

public class DeviceScanFragment extends BaseScanFragment {
    private static final String TAG = DeviceScanFragment.class.getSimpleName();
    private final int REQUEST_COARSE_LOCATION = 99;

    @Override
    public BaseScanner newScanner() {
        return new UHFScanner(UhfClassVersion.TS800, getContext(), this, BLE);
    }

    @Override
    protected void hookReplaceToDeviceFragment(BaseDevice baseDevice) {
        if (baseDevice instanceof com.gigatms.TS100A) {
            replaceFragment(TS100ADeviceControlFragment.newFragment(baseDevice.getDeviceID()));
        } else if (baseDevice instanceof com.gigatms.TS100) {
            replaceFragment(TS100DeviceControlFragment.newFragment(baseDevice.getDeviceID()));
        } else if (baseDevice instanceof com.gigatms.TS800) {
            replaceFragment(TS800DeviceControlFragment.newFragment(baseDevice.getDeviceID()));
        } else if (baseDevice instanceof com.gigatms.MU400H) {
            replaceFragment(MU400HDeviceControlFragment.newFragment(baseDevice.getDeviceID()));
        } else if (baseDevice instanceof com.gigatms.UR0250) {
            replaceFragment(UR0250DeviceControlFragment.newFragment(baseDevice.getDeviceID()));
        } else if (baseDevice instanceof com.gigatms.NR800) {
            replaceFragment(NR800DeviceControlFragment.newFragment(baseDevice.getDeviceID()));
        } else if (baseDevice instanceof com.gigatms.PWD100) {
            replaceFragment(PWD100DeviceControlFragment.newFragment(baseDevice.getDeviceID()));
        }
    }

    private void replaceFragment(DeviceControlFragment fragment) {
        Objects.requireNonNull(getActivity()).getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void hookAddSpnProducts() {
        addSpnProducts(new String[]{TS800.name(), TS100A.name(), TS100.name(), MU400H.name(), UR0250.name(), NR800.name(), PWD100.name()});
        mSpnProduct.setSelection(0);
        ((UHFScanner) mBaseScanner).setClassVersion(UhfClassVersion.TS800);
    }

    @Override
    public void hookSetClassVersion(String selectedProductName) {
        UhfClassVersion uhfClassVersion = UhfClassVersion.valueOf(selectedProductName);
        ((UHFScanner) mBaseScanner).setClassVersion(uhfClassVersion);
    }

    @Override
    public void hookAddSpnCommunicationTypes() {
        addSpnCommunicationType(new String[]{"Wi-Fi", BLE.name(), USB.name()});
        mSpnCommunicationType.setSelection(0);
        mBaseScanner.setCommunicationType(UDP);
    }

    @Override
    public void hookSetCommunicationType(String communicationType) {
        try {
            CommunicationType type = CommunicationType.valueOf(communicationType);
            mBaseScanner.setCommunicationType(type);
        } catch (IllegalArgumentException e) {
            mBaseScanner.setCommunicationType(UDP);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        requestNeededPermissions();
    }

    @Override
    public void onStart() {
        super.onStart();
        GLog.v(TAG, Arrays.toString(ConnectedDevices.getInstance().keySet().toArray()));
        mSpnProduct.setSelection(0);
        ((UHFScanner) mBaseScanner).setClassVersion(UhfClassVersion.TS800);
        mSpnCommunicationType.setSelection(0);
        mBaseScanner.setCommunicationType(UDP);
    }

    public void requestNeededPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getActivity() != null) {
                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_COARSE_LOCATION);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_COARSE_LOCATION) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        getActivity().registerReceiver(mUsbReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mUsbReceiver);
    }

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            GLog.d(TAG, "Broadcast action: " + action);
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                mDevicesAdapter.clear();
            }
        }
    };
}
