package com.gigatms.uhf;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gigatms.BaseScanner;
import com.gigatms.CommunicationType;
import com.gigatms.UHF.UhfClassVersion;
import com.gigatms.UHFScanner;
import com.gigatms.tools.GLog;
import com.squareup.leakcanary.RefWatcher;

import java.util.Arrays;

import static com.gigatms.CommunicationType.BLE;
import static com.gigatms.CommunicationType.TCP;
import static com.gigatms.CommunicationType.UDP;

public class DeviceScanFragment extends BaseScanFragment{
    private static final String TAG = DeviceScanFragment.class.getSimpleName();
    private final int REQUEST_COARSE_LOCATION = 99;

    @Override
    public BaseScanner newScanner() {
        return new UHFScanner(UhfClassVersion.TS800, getContext(), this, BLE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        addUhfClassVersionRadioButton();
        addInterfaceRadioButton();
        return view;
    }

    @Override
    public void setClassVersion(byte checkedProduct) {
        UhfClassVersion uhfClassVersion = UhfClassVersion.getClassCode(checkedProduct);
        ((UHFScanner) mBaseScanner).setClassVersion(uhfClassVersion);
    }

    void addUhfClassVersionRadioButton() {
        for (UhfClassVersion version : UhfClassVersion.values()) {
            if (version != UhfClassVersion.NO_DEFINE) {
                addRgProduct(version.name(), version.getValue());
            }
        }
        mRgProduct.check(UhfClassVersion.TS800.getValue());
        ((UHFScanner) mBaseScanner).setClassVersion(UhfClassVersion.TS800);
    }

    void addInterfaceRadioButton() {
        for (CommunicationType type : CommunicationType.values()) {
            if (type != UDP) {
                if (type == TCP) {
                    addRgInterface("Wi-Fi", type.ordinal(), v -> mBaseScanner.setCommunicationType(UDP));
                } else {
                    addRgInterface(type.name(), type.ordinal(), v -> mBaseScanner.setCommunicationType(BLE));
                }
            }
        }
        mRgInterface.check(BLE.ordinal());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        requestNeededPermissions();
    }

    @Override
    public void onResume() {
        super.onResume();
        GLog.d(TAG, Arrays.toString(ConnectedDevices.getInstance().keySet().toArray()));
        mRgProduct.check(UhfClassVersion.TS800.getValue());
        ((UHFScanner) mBaseScanner).setClassVersion(UhfClassVersion.TS800);
        mRgInterface.check(BLE.ordinal());
        mBaseScanner.setCommunicationType(BLE);
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
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        RefWatcher refWatcher = LeakWatcherApplication.getRefWatcher(getActivity());
        refWatcher.watch(this);
    }
}
