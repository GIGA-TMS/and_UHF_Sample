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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gigatms.CommunicationCallback;
import com.gigatms.CommunicationType;
import com.gigatms.ConnectionState;
import com.gigatms.DeviceDebugCallback;
import com.gigatms.PWD100;
import com.gigatms.TS100;
import com.gigatms.TS800;
import com.gigatms.UHFCallback;
import com.gigatms.UHFDevice;
import com.gigatms.UR0250;
import com.gigatms.uhf.paramsData.EditTextTitleParamData;
import com.gigatms.uhf.paramsData.SpinnerTitleParamData;
import com.gigatms.exceptions.ErrorParameterException;
import com.gigatms.parameters.LockAction;
import com.gigatms.parameters.MemoryBank;
import com.gigatms.tools.GLog;
import com.gigatms.tools.GTool;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.gigatms.parameters.MemoryBank.ACCESS_PASSWORD;
import static com.gigatms.parameters.MemoryBank.EPC_BANK;
import static com.gigatms.parameters.MemoryBank.KILL_PASSWORD;
import static com.gigatms.parameters.MemoryBank.RESERVE_BANK;
import static com.gigatms.parameters.MemoryBank.TID_BANK;
import static com.gigatms.parameters.MemoryBank.USER_BANK;

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

    protected GeneralCommandItem mWriteEpcCommand;
    protected GeneralCommandItem mWriteSelectedEpcCommand;
    protected GeneralCommandItem mReadTagWithSelectedEpcCommand;
    protected GeneralCommandItem mReadTagCommand;
    protected GeneralCommandItem mWriteTagWithSelectedEpcCommand;
    protected GeneralCommandItem mWriteTagCommand;
    protected GeneralCommandItem mLockTagWithPassword;
    protected GeneralCommandItem mLockTagWithoutPassword;
    protected GeneralCommandItem mKillTagWithPassword;
    protected GeneralCommandItem mKillTagWithoutPassword;

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
        mUhf.getFirmwareVersion();
        if (mUhf.getCommunicationType().equals(CommunicationType.BLE)) {
            if (mUhf instanceof TS800) {
                ((TS800) mUhf).getBleRomVersion();
            } else if (mUhf instanceof TS100) {
                ((TS100) mUhf).getBleRomVersion();
            } else if (mUhf instanceof UR0250) {
                ((UR0250) mUhf).getBleRomVersion();
            }
        }
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

    private void onNewReadWriteTagCommands() {
        newWriteEPCCommand();
        newWriteSelectedEpcCommand();
        newReadTagSelectedEpcCommand();
        newReadTagCommand();
        newWriteTagSelectedEpcCommand();
        newWriteTagCommand();
        newLockTagWithPassword();
        newLockTagWithoutPassword();
        newKillTagWithPassword();
        newKillTagWithoutPassword();
    }

    protected abstract void onNewSettingCommands();

    protected abstract void onShowInventoryViews();

    protected abstract void onShowAdvanceViews();

    private void onShowReadWriteTagViews() {
        mAdapter.add(mWriteEpcCommand);
        mAdapter.add(mWriteSelectedEpcCommand);
        mAdapter.add(mReadTagWithSelectedEpcCommand);
        mAdapter.add(mReadTagCommand);
        mAdapter.add(mWriteTagWithSelectedEpcCommand);
        mAdapter.add(mWriteTagCommand);
        mAdapter.add(mLockTagWithPassword);
        mAdapter.add(mLockTagWithoutPassword);
        mAdapter.add(mKillTagWithPassword);
        mAdapter.add(mKillTagWithoutPassword);
    }

    protected abstract void onShowSettingViews();

    private void setDeviceInformation() {
        updateConnectionViews(mUhf.getConnectionState());
        mTvMacAddress.setText(mUhf.getDeviceID());
        if (mUhf.getCommunicationType() != CommunicationType.BLE || mUhf instanceof PWD100) {
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
        GLog.v(TAG, mUhf.getDeviceName());
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

    private void newWriteEPCCommand() {
        mWriteEpcCommand = new GeneralCommandItem("Write EPC", null, "Write"
                , new EditTextTitleParamData("Password", "00000000", "00000000")
                , new EditTextTitleParamData("EPC", "EPC"));
        mWriteEpcCommand.setRightOnClickListener(v -> {
            try {
                EditTextTitleParamData password = (EditTextTitleParamData) mWriteEpcCommand.getViewDataArray()[0];
                EditTextTitleParamData epc = (EditTextTitleParamData) mWriteEpcCommand.getViewDataArray()[1];
                mUhf.writeEpc(password.getSelected(), GTool.hexStringToByteArray(epc.getSelected()));
            } catch (ErrorParameterException e) {
                Toaster.showToast(getContext(), e.getMessage(), Toast.LENGTH_LONG);
            }
        });
    }

    private void newWriteSelectedEpcCommand() {
        mWriteSelectedEpcCommand = new GeneralCommandItem("Write EPC", null, "Write"
                , new EditTextTitleParamData("Selected EPC", "PC+EPC")
                , new EditTextTitleParamData("Password", "00000000", "00000000")
                , new EditTextTitleParamData("EPC to Write", "EPC"));
        mWriteSelectedEpcCommand.setRightOnClickListener(v -> {
            try {
                EditTextTitleParamData selectedEpc = (EditTextTitleParamData) mWriteSelectedEpcCommand.getViewDataArray()[0];
                EditTextTitleParamData password = (EditTextTitleParamData) mWriteSelectedEpcCommand.getViewDataArray()[1];
                EditTextTitleParamData writeData = (EditTextTitleParamData) mWriteSelectedEpcCommand.getViewDataArray()[2];
                mUhf.writeEpc(selectedEpc.getSelected(), password.getSelected(), GTool.hexStringToByteArray(writeData.getSelected()));
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
    }

    private void newReadTagSelectedEpcCommand() {
        mReadTagWithSelectedEpcCommand = new GeneralCommandItem("Read Tag", null, "Read"
                , new EditTextTitleParamData("Selected Epc", "PC+EPC")
                , new EditTextTitleParamData("Password", "00000000", "00000000")
                , new SpinnerTitleParamData<>(new MemoryBank[]{RESERVE_BANK, EPC_BANK, TID_BANK, USER_BANK})
                , new EditTextTitleParamData("Start Address", "Start with 0", "" + 0)
                , new EditTextTitleParamData("Read Length", "0 means all", "" + 0)
        );
        mReadTagWithSelectedEpcCommand.setRightOnClickListener(v -> {
            EditTextTitleParamData selectedEpc = (EditTextTitleParamData) mReadTagWithSelectedEpcCommand.getViewDataArray()[0];
            EditTextTitleParamData password = (EditTextTitleParamData) mReadTagWithSelectedEpcCommand.getViewDataArray()[1];
            SpinnerTitleParamData memoryBand = (SpinnerTitleParamData) mReadTagWithSelectedEpcCommand.getViewDataArray()[2];
            EditTextTitleParamData startAddress = (EditTextTitleParamData) mReadTagWithSelectedEpcCommand.getViewDataArray()[3];
            EditTextTitleParamData readLength = (EditTextTitleParamData) mReadTagWithSelectedEpcCommand.getViewDataArray()[4];
            mUhf.readTag(selectedEpc.getSelected()
                    , password.getSelected()
                    , (MemoryBank) memoryBand.getSelected()
                    , Integer.valueOf(startAddress.getSelected())
                    , Integer.valueOf(readLength.getSelected()));
        });
    }

    private void newReadTagCommand() {
        mReadTagCommand = new GeneralCommandItem("Read Tag", null, "Read"
                , new EditTextTitleParamData("Password", "00000000", "00000000")
                , new SpinnerTitleParamData<>(new MemoryBank[]{RESERVE_BANK, EPC_BANK, TID_BANK, USER_BANK})
                , new EditTextTitleParamData("Start Address", "Start with 0", "" + 0)
                , new EditTextTitleParamData("Read Length", "0 means all", "" + 0)
        );
        mReadTagCommand.setRightOnClickListener(v -> {
            EditTextTitleParamData password = (EditTextTitleParamData) mReadTagCommand.getViewDataArray()[0];
            SpinnerTitleParamData memoryBand = (SpinnerTitleParamData) mReadTagCommand.getViewDataArray()[1];
            EditTextTitleParamData startAddress = (EditTextTitleParamData) mReadTagCommand.getViewDataArray()[2];
            EditTextTitleParamData readLength = (EditTextTitleParamData) mReadTagCommand.getViewDataArray()[3];
            mUhf.readTag(password.getSelected()
                    , (MemoryBank) memoryBand.getSelected()
                    , Integer.valueOf(startAddress.getSelected())
                    , Integer.valueOf(readLength.getSelected()));
        });
    }

    private void newWriteTagSelectedEpcCommand() {
        mWriteTagWithSelectedEpcCommand = new GeneralCommandItem("Write Tag", null, "Write"
                , new EditTextTitleParamData("Selected Epc", "PC+EPC")
                , new EditTextTitleParamData("Password", "00000000", "00000000")
                , new SpinnerTitleParamData<>(new MemoryBank[]{RESERVE_BANK, EPC_BANK, TID_BANK, USER_BANK})
                , new EditTextTitleParamData("Start Address", "Start from 0", "" + 0)
                , new EditTextTitleParamData("Write Data", "Data to Write")
        );
        mWriteTagWithSelectedEpcCommand.setRightOnClickListener(v -> {
            EditTextTitleParamData selectedEpc = (EditTextTitleParamData) mWriteTagWithSelectedEpcCommand.getViewDataArray()[0];
            EditTextTitleParamData password = (EditTextTitleParamData) mWriteTagWithSelectedEpcCommand.getViewDataArray()[1];
            SpinnerTitleParamData memoryBand = (SpinnerTitleParamData) mWriteTagWithSelectedEpcCommand.getViewDataArray()[2];
            EditTextTitleParamData startAddress = (EditTextTitleParamData) mWriteTagWithSelectedEpcCommand.getViewDataArray()[3];
            EditTextTitleParamData writeData = (EditTextTitleParamData) mWriteTagWithSelectedEpcCommand.getViewDataArray()[4];
            try {
                mUhf.writeTag(selectedEpc.getSelected()
                        , password.getSelected()
                        , (MemoryBank) memoryBand.getSelected()
                        , Integer.valueOf(startAddress.getSelected())
                        , GTool.hexStringToByteArray(writeData.getSelected()));
            } catch (Exception e) {
                e.printStackTrace();
                Toaster.showToast(getContext(), "Please Input Right \"Write Data\".", Toast.LENGTH_LONG);
            }
        });
    }

    private void newWriteTagCommand() {
        mWriteTagCommand = new GeneralCommandItem("Write Tag", null, "Write"
                , new EditTextTitleParamData("Password", "00000000", "00000000")
                , new SpinnerTitleParamData<>(new MemoryBank[]{RESERVE_BANK, EPC_BANK, TID_BANK, USER_BANK})
                , new EditTextTitleParamData("Start Address", "Start from 0", "" + 0)
                , new EditTextTitleParamData("Write Data", "Data to Write")
        );
        mWriteTagCommand.setRightOnClickListener(v -> {
            EditTextTitleParamData password = (EditTextTitleParamData) mWriteTagCommand.getViewDataArray()[0];
            SpinnerTitleParamData memoryBand = (SpinnerTitleParamData) mWriteTagCommand.getViewDataArray()[1];
            EditTextTitleParamData startAddress = (EditTextTitleParamData) mWriteTagCommand.getViewDataArray()[2];
            EditTextTitleParamData writeData = (EditTextTitleParamData) mWriteTagCommand.getViewDataArray()[3];
            try {
                mUhf.writeTag(password.getSelected()
                        , (MemoryBank) memoryBand.getSelected()
                        , Integer.valueOf(startAddress.getSelected())
                        , GTool.hexStringToByteArray(writeData.getSelected()));
            } catch (Exception e) {
                e.printStackTrace();
                Toaster.showToast(getContext(), "Please Input Right \"Write Data\".", Toast.LENGTH_LONG);
            }
        });
    }

    private void newLockTagWithPassword() {
        mLockTagWithPassword = new GeneralCommandItem("Lock Tag", null, "LOCK"
                , new EditTextTitleParamData("Password", "00000000", "00000000")
                , new SpinnerTitleParamData<>(new MemoryBank[]{EPC_BANK, TID_BANK, USER_BANK, KILL_PASSWORD, ACCESS_PASSWORD})
                , new SpinnerTitleParamData<>(LockAction.class)
        );
        mLockTagWithPassword.setRightOnClickListener(v -> {
            EditTextTitleParamData password = (EditTextTitleParamData) mLockTagWithPassword.getViewDataArray()[0];
            SpinnerTitleParamData memoryBank = (SpinnerTitleParamData) mLockTagWithPassword.getViewDataArray()[1];
            SpinnerTitleParamData lockAction = (SpinnerTitleParamData) mLockTagWithPassword.getViewDataArray()[2];
            Map<MemoryBank, LockAction> lockInfos = new HashMap<>();
            lockInfos.put((MemoryBank) memoryBank.getSelected(), (LockAction) lockAction.getSelected());
            mUhf.lockTag(password.getSelected(), lockInfos);
        });
    }

    private void newLockTagWithoutPassword() {
        mLockTagWithoutPassword = new GeneralCommandItem("Lock Tag", null, "LOCK"
                , new SpinnerTitleParamData<>(new MemoryBank[]{EPC_BANK, TID_BANK, USER_BANK, KILL_PASSWORD, ACCESS_PASSWORD})
                , new SpinnerTitleParamData<>(LockAction.class)
        );
        mLockTagWithoutPassword.setRightOnClickListener(v -> {
            SpinnerTitleParamData memoryBank = (SpinnerTitleParamData) mLockTagWithoutPassword.getViewDataArray()[0];
            SpinnerTitleParamData lockAction = (SpinnerTitleParamData) mLockTagWithoutPassword.getViewDataArray()[1];
            Map<MemoryBank, LockAction> lockInfos = new HashMap<>();
            lockInfos.put((MemoryBank) memoryBank.getSelected(), (LockAction) lockAction.getSelected());
            mUhf.lockTag(lockInfos);
        });
    }

    private void newKillTagWithPassword() {
        mKillTagWithPassword = new GeneralCommandItem("Kill Tag", null, "Kill"
                , new EditTextTitleParamData("Access Password", "00000000", "00000000")
                , new EditTextTitleParamData("Kill Password", "00000000", "00000000")
        );
        mKillTagWithPassword.setRightOnClickListener(v -> {
            EditTextTitleParamData accessPassword = (EditTextTitleParamData) mKillTagWithPassword.getViewDataArray()[0];
            EditTextTitleParamData killPassword = (EditTextTitleParamData) mKillTagWithPassword.getViewDataArray()[1];
            mUhf.killTag(accessPassword.getSelected(), killPassword.getSelected());
        });
    }

    private void newKillTagWithoutPassword() {
        mKillTagWithoutPassword = new GeneralCommandItem("Kill Tag", null, "Kill"
                , new EditTextTitleParamData("Kill Password", "00000000", "00000000")
        );
        mKillTagWithoutPassword.setRightOnClickListener(v -> {
            EditTextTitleParamData killPassword = (EditTextTitleParamData) mKillTagWithoutPassword.getViewDataArray()[0];
            mUhf.killTag(killPassword.getSelected());
        });
    }

    @Override
    public void didUpdateConnection(final ConnectionState connectState, CommunicationType type) {
        GLog.v(TAG, "Connected Device: "
                + Arrays.toString(ConnectedDevices.getInstance().keySet().toArray()));
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> updateConnectionViews(connectState));
        }
        if (connectState.equals(ConnectionState.CONNECTED)) {

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
        onUpdateDebugLog(TAG, GLog.v(TAG, "Connection Timeout: "
                + mUhf.getDeviceID() + ": " + mUhf.getConnectionState().name()));
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