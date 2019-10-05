package com.gigatms.uhf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gigatms.CommunicationCallback;
import com.gigatms.CommunicationType;
import com.gigatms.ConnectionState;
import com.gigatms.DeviceDebugCallback;
import com.gigatms.TS100;
import com.gigatms.TS800;
import com.gigatms.UHFCallback;
import com.gigatms.UHFDevice;
import com.gigatms.UR0250;
import com.gigatms.tools.GLog;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Arrays;
import java.util.Objects;

public abstract class DeviceControlFragment extends DebugFragment implements CommunicationCallback {
    public static final String MAC_ADDRESS = "devMAcAddress";
    private static final String TAG = DeviceControlFragment.class.getSimpleName();

    protected UHFDevice mUhf;
    protected UHFCallback mUHFCallback;
    protected boolean mTemp = false;
    private TextView mTvConnectionStatus;
    private TextView mTvMacAddress;
    protected TextView mTvFirmware;
    private Button mBtnConnect;

    private BottomNavigationView mBottomNavigationView;
    private TextView mTvBleFirmware;
    protected TextView mTvBleFirmwareValue;
    protected RecyclerView mRecyclerView;
    protected CommandRecyclerViewAdapter mAdapter;

    private boolean mBackPressing = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.fragment_device_control, container, false);
        findViews(view);
        initUHF();
        return view;
    }

    protected abstract void initUHFCallback();

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        Log.d(TAG, "onResume: ");
        initUHFCallback();
        mUhf.setUHFCallback(mUHFCallback);
        mUhf.setCommunicationCallback(this);
        GLog.v(TAG, Arrays.toString(ConnectedDevices.getInstance().keySet().toArray()));
        mAdapter = new CommandRecyclerViewAdapter();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mAdapter);

        if (getActivity() != null) {
            getActivity().registerReceiver(mUsbReceiver, filter);
            getActivity().setTitle(mUhf.getDeviceName());
        }
        setDeviceInformation();
        setConnectionButton();
        mUhf.getRomVersion();
        if (mUhf.getCommunicationType().equals(CommunicationType.BLE)) {
            if (mUhf instanceof TS800) {
                ((TS800) mUhf).getBleRomVersion();
            } else if (mUhf instanceof TS100) {
                ((TS100) mUhf).getBleRomVersion();
            } else if (mUhf instanceof UR0250) {
                ((UR0250) mUhf).getBleRomVersion();
            }
        }
        mUhf.initializeSettings();
        new ViewCreateAsyncTask().execute();
    }

    public class ViewCreateAsyncTask extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] objects) {
            initCommandViews();
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            showCommandViews(mBottomNavigationView.getSelectedItemId());
        }
    }


    private void initCommandViews() {
        onNewInventoryCommands();
        onNewSettingCommands();
        onNewAdvanceCommands();
        onNewReadWriteTagCommands();
        mBottomNavigationView.setOnNavigationItemSelectedListener(menuItem -> showCommandViews(menuItem.getItemId()));
    }

    private boolean showCommandViews(int id) {
        mAdapter.clear();
        boolean isViewSet = false;
        switch (id) {
            case R.id.inventory:
                onShowInventoryViews();
                isViewSet = true;
                break;
            case R.id.read_write_tag:
                onShowReadWriteTagViews();
                isViewSet = true;
                break;
            case R.id.setting:
                onShowSettingViews();
                isViewSet = true;
                break;
            case R.id.advance_setting:
                onShowAdvanceViews();
                isViewSet = true;
                break;
        }
        mRecyclerView.post(() -> mAdapter.notifyDataSetChanged());
        return isViewSet;
    }

    protected abstract void onNewInventoryCommands();

    protected abstract void onNewAdvanceCommands();

    protected abstract void onNewReadWriteTagCommands();

    protected abstract void onNewSettingCommands();

    protected abstract void onShowInventoryViews();

    protected abstract void onShowAdvanceViews();

    protected abstract void onShowReadWriteTagViews();

    protected abstract void onShowSettingViews();

    private void setDeviceInformation() {
        updateConnectionViews(mUhf.getConnectionState());
        mTvMacAddress.setText(mUhf.getDeviceID());
        if (mUhf.getCommunicationType() != CommunicationType.BLE) {
            mTvBleFirmware.setVisibility(View.GONE);
            mTvBleFirmwareValue.setVisibility(View.GONE);
        } else {
            mTvBleFirmware.setVisibility(View.VISIBLE);
            mTvBleFirmwareValue.setVisibility(View.VISIBLE);
        }
    }

    private void initUHF() {
        assert getArguments() != null;
        String macAddress = getArguments().getString(MAC_ADDRESS);
        mUhf = (UHFDevice) ConnectedDevices.getInstance().get(macAddress);
        GLog.v(TAG, mUhf.toString());
        mUhf.setDeviceDebugCallback(mDeviceDebugCallback);
    }

    private void findViews(View view) {
        findHeaderView(view);
        mRecyclerView = view.findViewById(R.id.recyclerview);
    }

    private void findHeaderView(View view) {
        mBottomNavigationView = view.findViewById(R.id.bottom_navigation_view);
        mTvConnectionStatus = view.findViewById(R.id.tv_connection_status);
        mTvMacAddress = view.findViewById(R.id.tv_mac_address);
        mTvFirmware = view.findViewById(R.id.tv_firmware);
        mTvBleFirmwareValue = view.findViewById(R.id.tv_ble_firmware_value);
        mTvBleFirmware = view.findViewById(R.id.tv_ble_firmware);
        mBtnConnect = view.findViewById(R.id.btn_connect);
    }


    @Override
    public void didUpdateConnection(final ConnectionState connectState, CommunicationType type) {
        GLog.v(TAG, "Connected Device: " + Arrays.toString(ConnectedDevices.getInstance().keySet().toArray()));
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> updateConnectionViews(connectState));
        }
        if (connectState.equals(ConnectionState.CONNECTED)) {
            mUhf.initializeSettings();
        }
        onUpdateDebugLog(TAG, "didUpdateConnection: " + mUhf.getDeviceID() + ": " + connectState.name());
    }

    private void updateConnectionViews(ConnectionState connectState) {
        if (!mBackPressing) {
            GLog.d(TAG, "updateConnectionViews: " + connectState);
            mTvConnectionStatus.setText(connectState.name());
            switch (connectState) {
                case CONNECTED:
                    mBtnConnect.setEnabled(true);
                    mBtnConnect.setText(getString(R.string.disconnect));
                    mTvConnectionStatus.setBackgroundColor(Color.GREEN);
                    break;
                case CONNECTING:
                    mBtnConnect.setEnabled(false);
                    mBtnConnect.setText(getString(R.string.connecting));
                    mTvConnectionStatus.setBackgroundColor(Color.GRAY);
                    break;
                case DISCONNECTED:
                    mBtnConnect.setEnabled(true);
                    mBtnConnect.setText(getString(R.string.connect));
                    mTvConnectionStatus.setBackgroundColor(Color.RED);
                    break;
            }
        }
    }

    @Override
    public void didConnectionTimeout(CommunicationType type) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Objects.requireNonNull(getActivity()).invalidateOptionsMenu();
                updateConnectionViews(mUhf.getConnectionState());
            });
        }
        onUpdateDebugLog(TAG, GLog.v(TAG, "Connection Timeout: " + mUhf.getDeviceID() + ": " + mUhf.getConnectionState().name()));
    }

    private DeviceDebugCallback mDeviceDebugCallback = new DeviceDebugCallback() {
        @Override
        public void didReceive(byte[] data, CommunicationType type) {
            if (mDebugFragmentListener != null) {
                mDebugFragmentListener.onUpdateDebugLog(GLog.d(TAG, "didReceive:\n", data));
            }
        }

        @Override
        public void didSend(byte[] data, CommunicationType type) {
            if (mDebugFragmentListener != null) {
                mDebugFragmentListener.onUpdateDebugLog(GLog.d(TAG, "didSend:\n", data));
            }
        }

        @Override
        public void didGeneralSuccess(final String invokeApi) {
            String message = "didGeneralSuccess: " + invokeApi;
            if (mDebugFragmentListener != null) {
                mDebugFragmentListener.onUpdateDebugLog(message);
                mDebugFragmentListener.onUpdateDebugInformation(message, R.color.device_operation_background);
            }
        }

        @Override
        public void didGeneralError(final String invokeApi, final String errorMessage) {
            String message = "didGeneralError: " + invokeApi + " " + errorMessage;
            if (mDebugFragmentListener != null) {
                mDebugFragmentListener.onUpdateDebugLog(message);
                mDebugFragmentListener.onUpdateDebugInformation(message, R.color.colorAccent);
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        GLog.d(TAG, "onPause");
        getActivity().unregisterReceiver(mUsbReceiver);
        mUhf.setCommunicationCallback(null);
        mUhf.setUHFCallback(null);
        mRecyclerView.setAdapter(null);
        if (mUhf.getConnectionState() != ConnectionState.DISCONNECTED) {
            mUhf.disconnect();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        GLog.v(TAG, "onDestroy");
        ConnectedDevices.getInstance().clear(mUhf.getDeviceID());
    }

    private void setConnectionButton() {
        mBtnConnect.setOnClickListener(v -> {
            switch (mUhf.getConnectionState()) {
                case CONNECTED:
                    mBtnConnect.setEnabled(false);
                    mUhf.disconnect();
                    break;
                case DISCONNECTED:
                    mBtnConnect.setEnabled(false);
                    mUhf.connect();
                    break;
            }
        });
    }

    private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            GLog.v(TAG, "Broadcast action: " + action);
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    mBackPressing = true;
                    getActivity().onBackPressed();
                }
            }
        }
    };
}