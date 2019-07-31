package com.gigatms.uhf;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.gigatms.CommunicationCallback;
import com.gigatms.CommunicationType;
import com.gigatms.ConnectionState;
import com.gigatms.DecodedTagData;
import com.gigatms.DeviceDebugCallback;
import com.gigatms.TS100;
import com.gigatms.TS800;
import com.gigatms.TagInformationFormat;
import com.gigatms.UHFCallback;
import com.gigatms.UHFDevice;
import com.gigatms.UR0250;
import com.gigatms.uhf.command.CheckboxCommand;
import com.gigatms.uhf.command.Command;
import com.gigatms.uhf.command.EditTextCommand;
import com.gigatms.uhf.command.SeekBarCommand;
import com.gigatms.uhf.command.SpinnerCommand;
import com.gigatms.uhf.command.TwoSpinnerCommand;
import com.gigatms.parameters.BuzzerAction;
import com.gigatms.parameters.BuzzerOperationMode;
import com.gigatms.parameters.EventType;
import com.gigatms.parameters.IONumber;
import com.gigatms.parameters.IOState;
import com.gigatms.parameters.MemoryBank;
import com.gigatms.parameters.MemoryBankSelection;
import com.gigatms.parameters.OutputInterface;
import com.gigatms.parameters.PostDataDelimiter;
import com.gigatms.parameters.RfSensitivityLevel;
import com.gigatms.parameters.Session;
import com.gigatms.parameters.TagDataEncodeType;
import com.gigatms.parameters.TagPresentedType;
import com.gigatms.parameters.Target;
import com.gigatms.parameters.TriggerType;
import com.gigatms.tools.GLog;
import com.gigatms.tools.GTool;
import com.squareup.leakcanary.RefWatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.gigatms.parameters.Session.SL;

public class DeviceControlFragment extends DebugFragment implements CommunicationCallback {
    public static final String MAC_ADDRESS = "devMAcAddress";
    private static final String TAG = DeviceControlFragment.class.getSimpleName();

    private UHFDevice mUhf;
    private boolean mTemp = false;
    private TextView mTvConnectionStatus;
    private TextView mTvMacAddress;
    private TextView mTvIpValue;
    private TextView mTvIp;
    private TextView mTvFirmware;
    private Button mBtnConnect;

    private BottomNavigationView mBottomNavigationView;
    private ReadWriteTagFragment mReadWriteTagFragment;
    private TextView mTvBleFirmware;
    private TextView mTvBleFirmwareValue;
    private FrameLayout mFrameLayout;
    private RecyclerView mRecyclerView;
    private CommandRecyclerViewAdapter mAdapter;

    private Command mStopInventoryCommand;
    private SpinnerCommand mInventoryCommand;
    private CheckboxCommand<TagDataEncodeType> mInventoryExCommand;

    private EditTextCommand mBleDeviceNameCommand;
    private SeekBarCommand mRfPowerCommand;
    private SeekBarCommand mRfSensitivityCommand;
    private TwoSpinnerCommand<Session, Target> mSessionTargetCommand;
    private SeekBarCommand mQCommand;
    private EditTextCommand mFrequencyCommand;
    private SpinnerCommand<EventType> mEventTypeCommand;
    private CheckboxCommand<OutputInterface> mOutputInterfaceCommand;

    private SpinnerCommand<BuzzerOperationMode> mBuzzerOperationCommand;
    private SpinnerCommand<BuzzerAction> mControlBuzzerCommand;
    private SpinnerCommand<PostDataDelimiter> mPostDataDelimiterCommand;
    private CheckboxCommand<MemoryBankSelection> mMemoryBankSelectionCommand;
    private SpinnerCommand<TriggerType> mTriggerCommand;
    private TwoSpinnerCommand<IONumber, IOState> mIoStateCommand;
    private Command mDisableFilterCommand;
    private CheckboxCommand<TagDataEncodeType> mEnableFilterCommand;
    private CheckboxCommand<TagDataEncodeType> mGetFilterCommand;

    public static DeviceControlFragment newFragment(String devMacAddress) {
        Bundle args = new Bundle();
        args.putString(MAC_ADDRESS, devMacAddress);
        DeviceControlFragment fragment = new DeviceControlFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_control, container, false);
        findViews(view);
        initUHF();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mUhf.setCommunicationCallback(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        GLog.d(TAG, Arrays.toString(ConnectedDevices.getInstance().keySet().toArray()));
        mAdapter = new CommandRecyclerViewAdapter();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mAdapter);

        if (getActivity() != null) {
            getActivity().setTitle(mUhf.getDeviceName());
        }
        setDeviceInformation();
        setConnectionButton();
        mUhf.getRomVersion();
        if (mUhf.getCommunicationType().equals(CommunicationType.BLE)) {
            mUhf.getBleRomVersion();
        }
        mUhf.initializeSettings();
        new ViewCreateAsyncTask().execute();
    }

    public class ViewCreateAsyncTask extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] objects) {
            initCommandViews();
            if (mReadWriteTagFragment == null) {
                mReadWriteTagFragment = ReadWriteTagFragment.newFragment(mUhf.getDeviceMacAddr());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            switchView(mBottomNavigationView.getSelectedItemId());
            switch (mBottomNavigationView.getSelectedItemId()) {
                case R.id.inventory:
                    showInventoryViews();
                    break;
                case R.id.setting:
                    showSettingViews();
                    break;
                case R.id.advance_setting:
                    showAdvance();
                    break;
            }
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.device_container, mReadWriteTagFragment)
                    .commit();
        }
    }

    private void initCommandViews() {
        newInventoryCommands();
        newSettingCommands();
        newAdvanceCommands();
        mBottomNavigationView.setOnNavigationItemSelectedListener(menuItem -> {
            boolean idFound = false;
            switch (menuItem.getItemId()) {
                case R.id.inventory:
                    showInventoryViews();
                    idFound = true;
                    break;
                case R.id.setting:
                    showSettingViews();
                    idFound = true;
                    break;
                case R.id.advance_setting:
                    showAdvance();
                    idFound = true;
                    break;
                case R.id.read_write_tag:
                    idFound = true;
                    break;
            }
            switchView(menuItem.getItemId());
            return idFound;
        });
    }

    void showAdvance() {
        mAdapter.clear();
        if (mUhf instanceof TS100) {
            showTS100AdvanceSettingViews();
        } else if (mUhf instanceof TS800) {
            showTS800AdvanceSettingViews();
        } else if (mUhf instanceof UR0250) {
            showUR0250AdvanceSettingViews();
        }
        mAdapter.notifyDataSetChanged();
    }

    void showInventoryViews() {
        mAdapter.clear();
        mAdapter.add(mStopInventoryCommand);
        mAdapter.add(mInventoryCommand);
        if (mUhf instanceof TS100) {
            mAdapter.add(mInventoryExCommand);
        }
        mAdapter.notifyDataSetChanged();
    }

    private void newInventoryCommands() {
        newStopInventoryCommand();
        newStartInventoryCommand();
        if (mUhf instanceof TS100) {
            newStartInventoryCommandEx((TS100) mUhf);
        }
    }

    void showSettingViews() {
        mAdapter.clear();
        if (mUhf.getCommunicationType().equals(CommunicationType.BLE)) {
            mAdapter.add(mBleDeviceNameCommand);
        }
        mAdapter.add(mRfPowerCommand);
        mAdapter.add(mRfSensitivityCommand);
        mAdapter.add(mSessionTargetCommand);
        mAdapter.add(mQCommand);
        mAdapter.add(mFrequencyCommand);
        mAdapter.notifyDataSetChanged();
    }

    private void newSettingCommands() {
        newBleDeviceNameCommand();
        newRfPowerCommand();
        newRfSensitivityCommand();
        newSessionTargetCommand();
        newQCommand();
        newFrequencyCommand();
    }

    private void newAdvanceCommands() {
        if (mUhf instanceof TS100) {
            newTS100Commands();
        } else if (mUhf instanceof TS800) {
            newTS800Commands();
        } else if (mUhf instanceof UR0250) {
            newUr0250Commands();
        }
    }

    private void newTS800Commands() {
        final TS800 ts800 = (TS800) mUhf;
        newTS800TriggerCommand(ts800);
        newTS800IoStateCommand(ts800);
    }

    private void showTS800AdvanceSettingViews() {
        mAdapter.add(mTriggerCommand);
        mAdapter.add(mIoStateCommand);
    }

    private void newUr0250Commands() {
        final UR0250 ur0250 = (UR0250) mUhf;
        newUr0250TriggerCommand(ur0250);
        newUr0250IoStateCommand(ur0250);
    }

    private void showUR0250AdvanceSettingViews() {
        mAdapter.add(mTriggerCommand);
        mAdapter.add(mIoStateCommand);
    }

    private void newTS100Commands() {
        final TS100 ts100 = (TS100) mUhf;
        newTS100BuzzerOperationCommand(ts100);
        newTS100ControlBuzzerCommand(ts100);
        newTS100OutputInterfaceCommand(ts100);
        newTS100EventTypeCommand(ts100);
        newTS100EnableFilterCommand(ts100);
        newTS100DisableFilterCommand(ts100);
        newTS100PostDataDelimiterCommand(ts100);
        newTS100MemoryBankSelectionCommand(ts100);
        newTS100DisableFilterCommand(ts100);
        newTS100GetFilterTypeCommand(ts100);
    }

    void showTS100AdvanceSettingViews() {
        mAdapter.add(mBuzzerOperationCommand);
        mAdapter.add(mControlBuzzerCommand);
        mAdapter.add(mOutputInterfaceCommand);
        mAdapter.add(mEventTypeCommand);
        mAdapter.add(mDisableFilterCommand);
        mAdapter.add(mEnableFilterCommand);
        mAdapter.add(mGetFilterCommand);
        mAdapter.add(mPostDataDelimiterCommand);
        mAdapter.add(mMemoryBankSelectionCommand);
    }

    private void newTS100DisableFilterCommand(final TS100 ts100) {
        mDisableFilterCommand = new Command("Disable Filter", null, "Disable");
        mDisableFilterCommand.setRightOnClickListener(v -> ts100.disableFilter(mTemp));
    }

    private void newTS100EnableFilterCommand(final TS100 ts100) {
        mEnableFilterCommand = new CheckboxCommand<>("Enable Filter", null, "Enable", TagDataEncodeType.class);
        mEnableFilterCommand.setRightOnClickListener(v -> {
            Set<TagDataEncodeType> tagDataEncodeTypes = mEnableFilterCommand.getSelected();
            ts100.enableFilter(mTemp, tagDataEncodeTypes);
        });
    }

    private void newTS100GetFilterTypeCommand(final TS100 ts100) {
        mGetFilterCommand = new CheckboxCommand<>("Get Filter", null, "Get", TagDataEncodeType.class);
        mGetFilterCommand.setRightOnClickListener(v -> ts100.getFilter(mTemp));
    }

    private void newTS100ControlBuzzerCommand(final TS100 ts100) {
        mControlBuzzerCommand = new SpinnerCommand<>("Control Buzzer", null, "Control", BuzzerAction.class);
        mControlBuzzerCommand.setRightOnClickListener(v -> {
            BuzzerAction buzzerAction = (BuzzerAction) mControlBuzzerCommand.getSelected();
            ts100.controlBuzzer(buzzerAction);
        });
    }

    private void newTS100BuzzerOperationCommand(final TS100 ts100) {
        mBuzzerOperationCommand = new SpinnerCommand<>("Get/Set Buzzer Operation", BuzzerOperationMode.class);
        mBuzzerOperationCommand.setLeftOnClickListener(v -> ts100.getBuzzerOperationMode(mTemp));
        mBuzzerOperationCommand.setRightOnClickListener(v -> {
            BuzzerOperationMode mode = (BuzzerOperationMode) mBuzzerOperationCommand.getSelected();
            ts100.setBuzzerOperationMode(mTemp, mode);
        });
    }

    private void newTS800IoStateCommand(final TS800 ts800) {
        IONumber[] ioNumbers = new IONumber[IONumber.values().length - 1];
        int j = 0;
        for (int i = 0; i < IONumber.values().length; i++) {
            if (!IONumber.values()[i].equals(IONumber.INPUT_PIN_0)) {
                ioNumbers[j] = IONumber.values()[i];
                j++;
            }
        }
        mIoStateCommand = new TwoSpinnerCommand<>("Set I/O State", ioNumbers, IOState.values());
        mIoStateCommand.setLeftOnClickListener(v -> ts800.getIOState());
        mIoStateCommand.setRightOnClickListener(v -> {
            IONumber firstSelected = (IONumber) mIoStateCommand.getFirstSelected();
            IOState secondSelected = (IOState) mIoStateCommand.getSecondSelected();
            ts800.setIOState(firstSelected, secondSelected);
        });
    }

    private void newTS800TriggerCommand(final TS800 ts800) {
        mTriggerCommand = new SpinnerCommand<>("Get/Set Trigger", TriggerType.class);
        mTriggerCommand.setLeftOnClickListener(v -> ts800.getTriggerType(mTemp));
        mTriggerCommand.setRightOnClickListener(v -> {
            TriggerType triggerType = (TriggerType) mTriggerCommand.getSelected();
            ts800.setTriggerType(mTemp, triggerType);
        });
    }

    private void newUr0250TriggerCommand(final UR0250 ur0250) {
        mTriggerCommand = new SpinnerCommand<>("Get/Set Trigger", TriggerType.class);
        mTriggerCommand.setLeftOnClickListener(v -> ur0250.getTriggerType(mTemp));
        mTriggerCommand.setRightOnClickListener(v -> {
            TriggerType triggerType = (TriggerType) mTriggerCommand.getSelected();
            ur0250.setTriggerType(mTemp, triggerType);
        });
    }

    private void newUr0250IoStateCommand(final UR0250 ur0250) {
        IONumber[] ioNumbers = new IONumber[IONumber.values().length - 1];
        int j = 0;
        for (int i = 0; i < IONumber.values().length; i++) {
            if (!IONumber.values()[i].equals(IONumber.INPUT_PIN_0)) {
                ioNumbers[j] = IONumber.values()[i];
                j++;
            }
        }
        mIoStateCommand = new TwoSpinnerCommand<>("Set I/O State", ioNumbers, IOState.values());
        mIoStateCommand.setLeftOnClickListener(v -> ur0250.getIOState());
        mIoStateCommand.setRightOnClickListener(v -> {
            IONumber firstSelected = (IONumber) mIoStateCommand.getFirstSelected();
            IOState secondSelected = (IOState) mIoStateCommand.getSecondSelected();
            ur0250.setIOState(firstSelected, secondSelected);
        });
    }

    private void newStartInventoryCommandEx(final TS100 ts100) {
        mInventoryExCommand = new CheckboxCommand<>("Start Inventory Ex", null, "Start Ex", TagDataEncodeType.class);
        mInventoryExCommand.setRightOnClickListener(v -> ts100.startInventoryEx(mInventoryExCommand.getSelected()));
    }

    private void newStartInventoryCommand() {
        mInventoryCommand = new SpinnerCommand<>("Start Inventory", null, "Start", TagPresentedType.class);
        mInventoryCommand.setRightOnClickListener(v -> {
            TagPresentedType tagPresentedType = (TagPresentedType) mInventoryCommand.getSelected();
            mUhf.startInventory(tagPresentedType);
        });
    }

    private void newStopInventoryCommand() {
        mStopInventoryCommand = new Command("Stop Inventory", null, "Stop");
        mStopInventoryCommand.setRightOnClickListener(v -> mUhf.stopInventory());
    }

    void switchView(int menuItemId) {
        if (menuItemId == R.id.read_write_tag) {
            mFrameLayout.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        } else {
            mFrameLayout.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }

    }

    private void newTS100MemoryBankSelectionCommand(final TS100 ts100) {
        mMemoryBankSelectionCommand = new CheckboxCommand<>("Get/Set Memory Bank Selection", MemoryBankSelection.class);
        mMemoryBankSelectionCommand.setLeftOnClickListener(v -> ts100.getMemoryBankSelection(mTemp));

        mMemoryBankSelectionCommand.setRightOnClickListener(v -> {
            Set<MemoryBankSelection> selected = mMemoryBankSelectionCommand.getSelected();
            ts100.setMemoryBankSelection(mTemp, selected);
        });
    }

    private void newTS100PostDataDelimiterCommand(final TS100 ts100) {
        mPostDataDelimiterCommand = new SpinnerCommand<>("Get/Set Post Data Delimiter", PostDataDelimiter.class);
        mPostDataDelimiterCommand.setLeftOnClickListener(v -> ts100.getPostDataDelimiter(mTemp));
        mPostDataDelimiterCommand.setRightOnClickListener(v -> {
            PostDataDelimiter postDataDelimiter = (PostDataDelimiter) mPostDataDelimiterCommand.getSelected();
            ts100.setPostDataDelimiter(mTemp, postDataDelimiter);
        });
    }

    private void newTS100OutputInterfaceCommand(final TS100 ts100) {
        mOutputInterfaceCommand = new CheckboxCommand<>("Get/Set Output Interface", OutputInterface.class);
        mOutputInterfaceCommand.setLeftOnClickListener(v -> ts100.getOutputInterface(mTemp));
        mOutputInterfaceCommand.setRightOnClickListener(v -> {
            Set<OutputInterface> selected = mOutputInterfaceCommand.getSelected();
            ts100.setOutputInterface(mTemp, selected);
        });
    }

    private void newTS100EventTypeCommand(final TS100 ts100) {
        mEventTypeCommand = new SpinnerCommand<>("Get/Set Event Type", EventType.class);
        mEventTypeCommand.setLeftOnClickListener(v -> ts100.getEventType(mTemp));
        mEventTypeCommand.setRightOnClickListener(v -> {
            EventType selected = (EventType) mEventTypeCommand.getSelected();
            ts100.setEventType(mTemp, selected);
        });
    }

    private void newFrequencyCommand() {
        mFrequencyCommand = new EditTextCommand("Get/Set Frequency", "840.250, 842.000, 843.250");
        mFrequencyCommand.setLeftOnClickListener(v -> mUhf.getFrequency(mTemp));
        mFrequencyCommand.setRightOnClickListener(v -> {
            String selected = mFrequencyCommand.getSelected();
            String[] frequencyArray = selected.trim().split(",");
            if (frequencyArray.length > 1) {
                try {
                    ArrayList<Double> frequencyList = new ArrayList<>();
                    for (String frequency : frequencyArray) {
                        if (!frequency.equals("")) {
                            frequencyList.add(Double.parseDouble(frequency));
                        }
                    }
                    mUhf.setFrequency(mTemp, frequencyList);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void newQCommand() {
        mQCommand = new SeekBarCommand("Get/Set Q", 0, 15);
        mQCommand.setLeftOnClickListener(v -> mUhf.getQValue(mTemp));
        mQCommand.setRightOnClickListener(v -> {
            int selected = mQCommand.getSelected();
            mUhf.setQValue(mTemp, selected);
        });
    }

    private void newSessionTargetCommand() {
        mSessionTargetCommand = new TwoSpinnerCommand<>("Get/Set Session and Target", Session.values(), Target.getAbTargets());
        mSessionTargetCommand.setOnFirstItemSelected(selected -> {
            if (selected.equals(SL) && !Target.slContains((Target) mSessionTargetCommand.getSecondEnums()[0])) {
                mSessionTargetCommand.setSecondEnums(Target.getSlTargets());
                mAdapter.notifyItemChanged(mSessionTargetCommand.getPosition());
            } else if ((!selected.equals(SL)) && !Target.abContains((Target) mSessionTargetCommand.getSecondEnums()[0])) {
                mSessionTargetCommand.setSecondEnums(Target.getAbTargets());
                mAdapter.notifyItemChanged(mSessionTargetCommand.getPosition());
            }
        });
        mSessionTargetCommand.setLeftOnClickListener(v -> mUhf.getSessionAndTarget(mTemp));
        mSessionTargetCommand.setRightOnClickListener(v -> {
            Session session = (Session) mSessionTargetCommand.getFirstSelected();
            Target target = (Target) mSessionTargetCommand.getSecondSelected();
            mUhf.setSessionAndTarget(mTemp, session, target);
        });
    }

    private void newRfSensitivityCommand() {
        mRfSensitivityCommand = new SeekBarCommand("Get/Set RF Sensitivity", 1, 14);
        mRfSensitivityCommand.setLeftOnClickListener(v -> mUhf.getRfSensitivity(mTemp));
        mRfSensitivityCommand.setRightOnClickListener(v -> {
            int level = mRfSensitivityCommand.getSelected();
            mUhf.setRfSensitivity(mTemp, RfSensitivityLevel.getSensitivityFrom(level));
        });
    }

    private void newRfPowerCommand() {
        mRfPowerCommand = new SeekBarCommand("Get/Set RF Power", 1, 27);
        mRfPowerCommand.setLeftOnClickListener(v -> mUhf.getRfPower(mTemp));
        mRfPowerCommand.setRightOnClickListener(v -> {
            byte selected = (byte) mRfPowerCommand.getSelected();
            mUhf.setRfPower(mTemp, selected);
        });
    }

    private void newBleDeviceNameCommand() {
        mBleDeviceNameCommand = new EditTextCommand("Get/Set BLE Device Name", "BLE Device Name");
        mBleDeviceNameCommand.setRightOnClickListener(v -> {
            String deviceName = mBleDeviceNameCommand.getSelected();
            mUhf.setBleDeviceName(deviceName);
        });
        mBleDeviceNameCommand.setLeftOnClickListener(v -> mUhf.getBleDeviceName());
    }

    private void setDeviceInformation() {
        updateConnectionViews(mUhf.getConnectionState());
        mTvMacAddress.setText(mUhf.getDeviceMacAddr());
        if (mUhf.getCommunicationType() == CommunicationType.TCP) {
            mTvIp.setVisibility(View.VISIBLE);
            mTvIpValue.setVisibility(View.VISIBLE);
            mTvIpValue.setText(mUhf.getDeviceIp());
            mTvBleFirmware.setVisibility(View.GONE);
            mTvBleFirmwareValue.setVisibility(View.GONE);
        } else {
            mTvIp.setVisibility(View.GONE);
            mTvIpValue.setVisibility(View.GONE);
            mTvBleFirmware.setVisibility(View.VISIBLE);
            mTvBleFirmwareValue.setVisibility(View.VISIBLE);
        }
    }

    private void initUHF() {
        assert getArguments() != null;
        String macAddress = getArguments().getString(MAC_ADDRESS);
        mUhf = (UHFDevice) ConnectedDevices.getInstance().get(macAddress);
        assert mUhf != null;
        GLog.d(TAG, mUhf.toString());
        mUhf.setUHFCallback(mUHFCallback);
        mUhf.setDeviceDebugCallback(mDeviceDebugCallback);
    }

    private void findViews(View view) {
        findHeaderView(view);
        mFrameLayout = view.findViewById(R.id.device_container);
        mRecyclerView = view.findViewById(R.id.recyclerview);
    }

    private void findHeaderView(View view) {
        mBottomNavigationView = view.findViewById(R.id.bottom_navigation_view);
        mTvConnectionStatus = view.findViewById(R.id.tv_connection_status);
        mTvMacAddress = view.findViewById(R.id.tv_mac_address);
        mTvIp = view.findViewById(R.id.tv_ip);
        mTvIpValue = view.findViewById(R.id.tv_ip_value);
        mTvFirmware = view.findViewById(R.id.tv_firmware);
        mTvBleFirmwareValue = view.findViewById(R.id.tv_ble_firmware_value);
        mTvBleFirmware = view.findViewById(R.id.tv_ble_firmware);
        mBtnConnect = view.findViewById(R.id.btn_connect);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        assert getFragmentManager() != null;
        Fragment fragment = getFragmentManager().findFragmentById(R.id.device_container);
        if (fragment != null)
            getFragmentManager().beginTransaction().remove(fragment).commit();
    }

    UHFCallback mUHFCallback = new UHFCallback() {
        @Override
        public void didGeneralSuccess(String invokeApi) {
            final String message = "didGeneralSuccess: " + invokeApi;
            if (invokeApi.equals("SET_BLE_DEVICE_NAME")) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> getActivity().setTitle(mUhf.getDeviceName()));
                }
            }
            onUpdateLog(TAG, message);
        }

        @Override
        public void didGeneralError(String invokeApi, String errorMessage) {
            final String message = "didGeneralError:" +
                    "\n\tinvokeApi: " + invokeApi +
                    "\n\terrorMessage: " + errorMessage;
            onUpdateLog(TAG, message);
        }

        @Override
        public void didGetFirmwareVersion(final String firmwareVersion) {
            mTvFirmware.post(() -> mTvFirmware.setText(firmwareVersion));
            onUpdateLog(TAG, "didGetFirmwareVersion: " + firmwareVersion);
        }

        @Override
        public void didGetBleFirmwareVersion(final String firmwareVersion) {
            mTvBleFirmwareValue.post(() -> mTvBleFirmwareValue.setText(firmwareVersion));
            onUpdateLog(TAG, "didGetBleFirmwareVersion: " + firmwareVersion);
        }

        @Override
        public void didGetRfPower(final byte rfPower) {
            mRfPowerCommand.setSelected(rfPower);
            mAdapter.notifyItemChanged(mRfPowerCommand.getPosition());
            onUpdateLog(TAG, "didGetRfPower: " + rfPower);
        }

        @Override
        public void didGetRfSensitivity(final RfSensitivityLevel sensitivity) {
            String sensitivityName = sensitivity.name();
            String regEx = "[^0-9]";
            Pattern pattern = Pattern.compile(regEx);
            Matcher matcher = pattern.matcher(sensitivityName);
            final int sensitivityValue = Integer.parseInt(matcher.replaceAll("").trim());
            mRfSensitivityCommand.setSelected(sensitivityValue);
            mAdapter.notifyItemChanged(mRfSensitivityCommand.getPosition());
            onUpdateLog(TAG, "didGetRfSensitivity: " + sensitivity.name());
        }

        @Override
        public void didGetFrequencyList(final ArrayList<Double> frequencyList) {
            String frequencyData = Arrays.toString(frequencyList.toArray());
            mFrequencyCommand.setSelected(frequencyData.replace("[", "").replace("]", ""));
            mAdapter.notifyItemChanged(mFrequencyCommand.getPosition());
            onUpdateLog(TAG, "didGetFrequencyList:\n" + frequencyData);
        }

        @Override
        public void didGetSessionAndTarget(final Session session, final Target target) {
            mSessionTargetCommand.setFirstSelected(session);
            mSessionTargetCommand.setSecondSelected(target);
            mAdapter.notifyItemChanged(mSessionTargetCommand.getPosition());
            onUpdateLog(TAG, "didGetSessionAndTarget:" +
                    "\n\tSession: " + session.name() +
                    "\n\tTarget: " + target.name());
        }

        @Override
        public void didGetQValue(final byte qValue) {
            mQCommand.setSelected(qValue);
            mAdapter.notifyItemChanged(mQCommand.getPosition());
            onUpdateLog(TAG, "didGetQValue: " + qValue);
        }

        @Override
        public void didIOStatsChangedEventHandler(IONumber ioNumber, IOState ioState) {
            onUpdateLog(TAG, "didIOStatsChangedEventHandler:" +
                    "\n\tIONumber: " + ioNumber.name() + "," +
                    "\n\tIOState: " + ioState.name());
        }

        @Override
        public void didGetBleDeviceName(final String bleDeviceName) {
            mBleDeviceNameCommand.setSelected(bleDeviceName);
            mAdapter.notifyItemChanged(mBleDeviceNameCommand.getPosition());
            onUpdateLog(TAG, "didGetBleDeviceName: " + bleDeviceName);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> getActivity().setTitle(mUhf.getDeviceName()));
            }
        }

        @Override
        public void didReadTag(MemoryBank memoryBank, int startWordAddress, byte[] readData) {
            final String dataString = GTool.bytesToHexString(readData);
            mReadWriteTagFragment.setTagData(memoryBank, startWordAddress, readData);
            onUpdateLog(TAG, "didReadTag:" +
                    "\n\tMemoryBank: " + memoryBank.name() +
                    "\n\tstartWordAddress: " + startWordAddress +
                    "\n\treadData: " + dataString);
        }

        @Override
        public void didWriteTag(MemoryBank memoryBank, int startWordAddress) {
            onUpdateLog(TAG, "didWriteTag:" +
                    "\n\tMemoryBank: " + memoryBank.name() +
                    "\n\tstartWordAddress: " + startWordAddress);
        }

        @Override
        public void didGetEventType(final EventType eventType) {
            mEventTypeCommand.setSelected(eventType);
            mAdapter.notifyItemChanged(mEventTypeCommand.getPosition());
            onUpdateLog(TAG, "didGetEventType: " + eventType.name());
        }

        @Override
        public void didGetOutputInterface(final Set<OutputInterface> outputInterfaces) {
            mOutputInterfaceCommand.setSelected(outputInterfaces);
            mAdapter.notifyItemChanged(mOutputInterfaceCommand.getPosition());
            onUpdateLog(TAG, "didGetOutputInterface: " + Arrays.toString(outputInterfaces.toArray()));
        }

        @Override
        public void didGetBuzzerOperationMode(final BuzzerOperationMode buzzerOperationMode) {
            mBuzzerOperationCommand.setSelected(buzzerOperationMode);
            mAdapter.notifyItemChanged(mBuzzerOperationCommand.getPosition());
            onUpdateLog(TAG, "didGetBuzzerOperationMode: " + buzzerOperationMode.name());
        }

        @Override
        public void didGetIOState(Map<IONumber, IOState> gpioStatuses) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("didGetIOState");
            for (IONumber ioNumber : gpioStatuses.keySet()) {
                stringBuilder.append("\n\t").append(ioNumber).append(": ").append(gpioStatuses.get(ioNumber));
            }
            onUpdateLog(TAG, stringBuilder.toString());
        }

        @Override
        public void didGetTriggerType(final TriggerType triggerSource) {
            mTriggerCommand.setSelected(triggerSource);
            mAdapter.notifyItemChanged(mTriggerCommand.getPosition());
            onUpdateLog(TAG, "didGetTriggerType: " + triggerSource.name());
        }

        @Override
        public void didDiscoverTagInfo(TagInformationFormat tagInformationFormat) {
            String message = "didDiscoverTagInfo(Inventory Event):" +
                    "\n\tAntenna: " + tagInformationFormat.getAntenna() +
                    "\n\tFrequency: " + tagInformationFormat.getFrequency() +
                    "\n\tRssi: " + tagInformationFormat.getRssi() +
                    "\n\tPC EPC: " + tagInformationFormat.getPcEPCHex();
            if (tagInformationFormat.getTidHex().length() != 0) {
                message = message + "\n\tTID: " + tagInformationFormat.getTidHex();
            }
            onUpdateLog(TAG, message);
        }

        @Override
        public void didDiscoverTagInfoEx(DecodedTagData decodedTagData) {
            super.didDiscoverTagInfoEx(decodedTagData);
            StringBuilder message = new StringBuilder("didDiscoverTagInfoEx:(Inventory Ex Event)" +
                    "\n\tDecodedTagData:" +
                    "\n\t\tTagDataEncodeType: " + decodedTagData.getTagDataEncodeType() +
                    "\n\t\tDeviceSerialNumber: " + decodedTagData.getDeviceSerialNumber() +
                    "\n\t\tTID: " + decodedTagData.getTIDHexString() +
                    "\n\t\tTagDecimalSerialNumber: " + decodedTagData.getTagDecimalSerialNumber() +
                    "\n\t\tTagHexStringSerialNumber: " + decodedTagData.getTagSerialNumberHexString() +
                    "\n\t\tTagSerialNumberLength: " + decodedTagData.getTagSerialNumberLength());

            for (DecodedTagData.DecodedData decodedData : decodedTagData.getDecodedDataList()) {
                message.append("\n\t\tOutputDataType: ")
                        .append(decodedData.getOutputDataType())
                        .append(", ").append("Data: ").append(decodedData.getOutputTypeStringData());
            }
            onUpdateLog(TAG, message.toString());
        }

        @Override
        public void didReadEpc(byte[] epc) {
            mReadWriteTagFragment.setEpc(epc);
            onUpdateLog(TAG, "didReadEpc: " + GTool.bytesToHexString(epc));
        }

        @Override
        public void didGetPostDataDelimiter(PostDataDelimiter postDataDelimiter) {
            mPostDataDelimiterCommand.setSelected(postDataDelimiter);
            mAdapter.notifyItemChanged(mPostDataDelimiterCommand.getPosition());
            onUpdateLog(TAG, "didGetPostDataDelimiter: " + postDataDelimiter);
        }

        @Override
        public void didGetMemoryBankSelection(Set<MemoryBankSelection> memoryBankSelection) {
            mMemoryBankSelectionCommand.setSelected(memoryBankSelection);
            mAdapter.notifyItemChanged(mMemoryBankSelectionCommand.getPosition());
            onUpdateLog(TAG, "didGetMemoryBankSelection: " + Arrays.toString(memoryBankSelection.toArray()));
        }

        @Override
        public void didGetFilter(Set<TagDataEncodeType> tagDataEncodeTypes) {
            mGetFilterCommand.setSelected(tagDataEncodeTypes);
            mAdapter.notifyItemChanged(mGetFilterCommand.getPosition());
            if (tagDataEncodeTypes == null || tagDataEncodeTypes.size() == 0) {
                onUpdateLog(TAG, "didGetFilter: Filter is disable.");
            } else {
                onUpdateLog(TAG, "didGetFilter: " + Arrays.toString(tagDataEncodeTypes.toArray()));
            }
        }
    };

    @Override
    public void didUpdateConnection(final ConnectionState connectState, CommunicationType type) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> updateConnectionViews(connectState));
        }
        if (connectState.equals(ConnectionState.CONNECTED)) {
            mUhf.initializeSettings();
        }
        onUpdateDebugLog(TAG, GLog.v(TAG, "didUpdateConnection: " + mUhf.getDeviceMacAddr() + ": " + connectState.name()));
    }

    void updateConnectionViews(ConnectionState connectState) {
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

    @Override
    public void didConnectionTimeout(CommunicationType type) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Objects.requireNonNull(getActivity()).invalidateOptionsMenu();
                updateConnectionViews(mUhf.getConnectionState());
            });
        }
        onUpdateDebugLog(TAG, GLog.v(TAG, "Connection Timeout: " + mUhf.getDeviceMacAddr() + ": " + mUhf.getConnectionState().name()));
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
        mRecyclerView.setAdapter(null);
    }

    @Override
    public void onStop() {
        super.onStop();
        GLog.d(TAG, "onStop");
        mUhf.setCommunicationCallback(null);
        mUhf.disconnect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        GLog.d(TAG, "onDestroy");
        getChildFragmentManager().beginTransaction()
                .remove(mReadWriteTagFragment);
        ConnectedDevices.getInstance().clear(mUhf.getDeviceMacAddr());
        RefWatcher refWatcher = LeakWatcherApplication.getRefWatcher(getContext());
        refWatcher.watch(this);
    }

    void setConnectionButton() {
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
}