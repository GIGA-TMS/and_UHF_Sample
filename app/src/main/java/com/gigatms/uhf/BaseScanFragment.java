package com.gigatms.uhf;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.gigatms.BaseDevice;
import com.gigatms.BaseScanner;
import com.gigatms.CommunicationType;
import com.gigatms.ConnectivitySimpleManager;
import com.gigatms.ScanDebugCallback;
import com.gigatms.ScannerCallback;
import com.gigatms.tools.GLog;
import com.squareup.leakcanary.RefWatcher;

import java.util.Objects;

import static com.gigatms.CommunicationType.BLE;
import static com.gigatms.CommunicationType.TCP;

public abstract class BaseScanFragment extends DebugFragment implements ScannerCallback, ScanDebugCallback {
    private static final String TAG = BaseScanFragment.class.getSimpleName();
    public static final String DEBUG = "DEBUG";

    protected BaseScanner mBaseScanner;
    protected DevicesAdapter mDevicesAdapter;
    private Button mBtnScan;
    private RecyclerView mRecyclerView;
    protected RadioGroup mRgProduct;
    protected RadioGroup mRgInterface;
    private ConnectivitySimpleManager mConnectivitySimpleManager;

    public abstract BaseScanner newScanner();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_scan, container, false);
        mBaseScanner = newScanner();
        findViews(view);
        mConnectivitySimpleManager = new ConnectivitySimpleManager();
        return view;
    }

    private void setViews() {
        Objects.requireNonNull(getActivity()).setTitle(getString(R.string.app_name));
        initRecyclerView();
        mBtnScan.setOnClickListener(v -> startScan());
    }

    void startScan() {
        ConnectedDevices.getInstance().clear();
        mDevicesAdapter.clear();
        if (isCommunicationEnable()) {
            mBaseScanner.startScan();
        } else {
            showAlert("Please Open " + (mBaseScanner.getCurrentCommunicationType() == BLE ? "BLE" : "Wi-Fi") + "!");
        }
    }

    protected boolean isCommunicationEnable() {
        CommunicationType type = mBaseScanner.getCurrentCommunicationType();
        switch (type) {
            case UDP:
                WifiManager wifiManager = (WifiManager) Objects.requireNonNull(getContext()).getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                return wifiManager.isWifiEnabled();
            case BLE:
                BluetoothManager bleManager = (BluetoothManager) Objects.requireNonNull(getContext()).getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
                BluetoothAdapter bluetoothAdapter = bleManager.getAdapter();
                return bluetoothAdapter.isEnabled();
            default:
                return false;
        }
    }

    private void showAlert(String title) {
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setPositiveButton("OK", null)
                .show();
    }

    private void initRecyclerView() {
        initDevicesAdapter();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setAdapter(mDevicesAdapter);
        for (String macAddress : ConnectedDevices.getInstance().keySet()) {
            mDevicesAdapter.addDevice(ConnectedDevices.getInstance().get(macAddress));
        }
        mRecyclerView.setLayoutManager(linearLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
                linearLayoutManager.getOrientation());
        mRecyclerView.addItemDecoration(dividerItemDecoration);
    }

    private void initDevicesAdapter() {
        if (mDevicesAdapter == null) {
            mDevicesAdapter = new DevicesAdapter(getContext());
            mDevicesAdapter.setControlCallback(baseDevice ->
                    Objects.requireNonNull(getActivity()).getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, DeviceControlFragment.newFragment(baseDevice.getDeviceMacAddr()))
                            .addToBackStack(null)
                            .commit());
        } else {
            mDevicesAdapter.clear();
        }
    }

    private void findViews(View view) {
        mRecyclerView = view.findViewById(R.id.device_list);
        mBtnScan = view.findViewById(R.id.btn_scan);
        mRgProduct = view.findViewById(R.id.rg_probuct);
        mRgInterface = view.findViewById(R.id.rg_interface);
    }

    public void addRgProduct(String text, int id) {
        final RadioButton radioButton = new RadioButton(getContext());
        radioButton.setText(text);
        radioButton.setId(id);
        radioButton.setOnClickListener(v -> {
            int checkedProduct = radioButton.getId();
            setClassVersion((byte) checkedProduct);
        });
        mRgProduct.addView(radioButton);
    }

    public abstract void setClassVersion(byte checkedProduct);

    public void addRgInterface(String text, int id, OnClickListener onClickListener) {
        RadioButton radioButton = new RadioButton(getContext());
        radioButton.setText(text);
        radioButton.setId(id);
        radioButton.setOnClickListener(onClickListener);
        mRgInterface.addView(radioButton);
    }

    @Override
    public void onResume() {
        super.onResume();
        setViews();
        mBaseScanner.setScanDebugCallback(this);
        mConnectivitySimpleManager.routeNetworkRequestsThroughWifi(null, getContext());
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
        if (mBaseScanner != null && isCommunicationEnable()) {
            mBaseScanner.stopScan();
            mBaseScanner.setScanDebugCallback(null);
        }
        mConnectivitySimpleManager.unregisterNetworkCallback();
        mDevicesAdapter.clear();
        mRecyclerView.setAdapter(null);
    }

    @Override
    public void didDiscoveredDevice(final BaseDevice baseDevice) {
        Log.d(TAG, "didDiscoveredDevice: ");
        String message = baseDevice.getDeviceName() + "\nMac Address: " + baseDevice.getDeviceMacAddr();
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> mDevicesAdapter.addDevice(baseDevice));
            if (mDebugFragmentListener != null) {
                if (baseDevice.getCommunicationType().equals(TCP)) {
                    message += ("\nIP: " + baseDevice.getDeviceIp());
                }
                mDebugFragmentListener.onUpdateDebugInformation(message, R.color.device_operation_background);
            }
            onUpdateLog(TAG, message);
        }
    }

    @Override
    public void didScanStop() {
        Log.d(TAG, "didScanStop: ");
    }

    @Override
    public void didSend(byte[] data, String idMessage) {
        if (mDebugFragmentListener != null && getActivity() != null) {
            mDebugFragmentListener.onUpdateDebugLog(GLog.v(TAG, idMessage + "\nTX", data));
        }
    }

    @Override
    public void didReceive(byte[] data, String idMessage) {
        if (mDebugFragmentListener != null && getActivity() != null) {
            mDebugFragmentListener.onUpdateDebugLog(GLog.v(TAG, idMessage + "\nRX", data));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        mBaseScanner.onDestroy();
        RefWatcher refWatcher = LeakWatcherApplication.getRefWatcher(getActivity());
        refWatcher.watch(this);
    }
}
