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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import com.gigatms.MU400H;
import com.gigatms.TS100;
import com.gigatms.TS800;
import com.gigatms.TagInformationFormat;
import com.gigatms.UHFCallback;
import com.gigatms.UHFDevice;
import com.gigatms.UR0250;
import com.gigatms.uhf.paramsData.CheckboxParamData;
import com.gigatms.uhf.paramsData.EditTextParamData;
import com.gigatms.uhf.paramsData.SeekBarParamData;
import com.gigatms.uhf.paramsData.SpinnerParamData;
import com.gigatms.uhf.paramsData.TwoSpinnerParamData;
import com.gigatms.parameters.BuzzerAction;
import com.gigatms.parameters.BuzzerOperationMode;
import com.gigatms.parameters.EventType;
import com.gigatms.parameters.IONumber;
import com.gigatms.parameters.IOState;
import com.gigatms.parameters.KeyboardSimulation;
import com.gigatms.parameters.MemoryBank;
import com.gigatms.parameters.MemoryBankSelection;
import com.gigatms.parameters.MissingInventoryThreshold;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.gigatms.parameters.OutputInterface.DEFAULT;
import static com.gigatms.parameters.OutputInterface.HID_KEYBOARD;
import static com.gigatms.parameters.OutputInterface.TCP_SERVER;
import static com.gigatms.parameters.Session.SL;

public class DeviceControlFragment extends DebugFragment implements CommunicationCallback {
    public static final String MAC_ADDRESS = "devMAcAddress";
    private static final String TAG = DeviceControlFragment.class.getSimpleName();

    private UHFDevice mUhf;
    private boolean mTemp = false;
    private TextView mTvConnectionStatus;
    private TextView mTvMacAddress;
    private TextView mTvFirmware;
    private Button mBtnConnect;

    private BottomNavigationView mBottomNavigationView;
    private ReadWriteTagFragment mReadWriteTagFragment;
    private TextView mTvBleFirmware;
    private TextView mTvBleFirmwareValue;
    private FrameLayout mFrameLayout;
    private RecyclerView mRecyclerView;
    private CommandRecyclerViewAdapter mAdapter;

    private GeneralCommandItem mStopInventoryCommand;
    private GeneralCommandItem mInventoryCommand;
    private GeneralCommandItem mInventoryExCommand;

    private GeneralCommandItem mBleDeviceNameCommand;
    private GeneralCommandItem mRfPowerCommand;
    private GeneralCommandItem mRfSensitivityCommand;
    private GeneralCommandItem mSessionTargetCommand;
    private GeneralCommandItem mQCommand;
    private GeneralCommandItem mFrequencyCommand;
    private GeneralCommandItem mTagRemovedEventThresholdCommand;
    private GeneralCommandItem mTagPresentedEventThresholdCommand;
    private GeneralCommandItem mOutputInterfacesCommand;
    private GeneralCommandItem mOutputInterfaceCommand;

    private GeneralCommandItem mEventTypeCommand;
    private GeneralCommandItem mBuzzerOperationCommand;
    private GeneralCommandItem mControlBuzzerCommand;
    private GeneralCommandItem mPostDataDelimiterCommand;
    private GeneralCommandItem mMemoryBankSelectionCommand;
    private GeneralCommandItem mTriggerCommand;
    private GeneralCommandItem mIoStateCommand;
    private GeneralCommandItem mFilterCommand;

    private boolean mBackPressing = false;

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
        Log.d(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.fragment_device_control, container, false);
        findViews(view);
        initUHF();
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        getActivity().registerReceiver(mUsbReceiver, filter);
        Log.d(TAG, "onResume: ");
        mUhf.setCommunicationCallback(this);
        mUhf.setUHFCallback(mUHFCallback);
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
                mReadWriteTagFragment = ReadWriteTagFragment.newFragment(mUhf.getDeviceID());
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
        } else if (mUhf instanceof MU400H) {
            showMU400AdvanceSettingViews();
        }
        mAdapter.notifyDataSetChanged();
    }

    void showInventoryViews() {
        mAdapter.clear();
        mAdapter.add(mStopInventoryCommand);
        mAdapter.add(mInventoryCommand);
        if (mUhf instanceof TS100 || mUhf instanceof MU400H) {
            mAdapter.add(mInventoryExCommand);
        }
        mAdapter.notifyDataSetChanged();
    }

    private void newInventoryCommands() {
        newStopInventoryCommand();
        newStartInventoryCommand();
        if (mUhf instanceof TS100) {
            newStartInventoryCommandEx(mUhf);
        }
        if (mUhf instanceof MU400H) {
            newStartInventoryCommandEx(mUhf);
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
        mAdapter.add(mTagPresentedEventThresholdCommand);
        mAdapter.add(mTagRemovedEventThresholdCommand);
        mAdapter.notifyDataSetChanged();
    }

    private void newSettingCommands() {
        newBleDeviceNameCommand();
        newRfPowerCommand();
        newRfSensitivityCommand();
        newSessionTargetCommand();
        newQCommand();
        newFrequencyCommand();
        newTagRemovedEventThresholdCommand();
        newTagPresentedEventThresholdCommand();
    }

    private void newTagPresentedEventThresholdCommand() {
        mTagPresentedEventThresholdCommand = new GeneralCommandItem("Get/Set Tag Presented Event Threshold", new EditTextParamData("1~25s, 0:Always Repeat, -1: Never Repeat"));
        mTagPresentedEventThresholdCommand.setLeftOnClickListener(v -> mUhf.getTagPresentedEventThreshold(mTemp));
        mTagPresentedEventThresholdCommand.setRightOnClickListener(v -> {
            EditTextParamData viewData = (EditTextParamData) mTagPresentedEventThresholdCommand.getViewDataArray()[0];
            int period = Integer.parseInt(viewData.getSelected());
            mUhf.setTagPresentedEventThreshold(mTemp, period);
        });
    }

    private void newTagRemovedEventThresholdCommand() {
        mTagRemovedEventThresholdCommand = new GeneralCommandItem("Get/Set Tag Removed Event Threshold", new SpinnerParamData<>(MissingInventoryThreshold.class));
        mTagRemovedEventThresholdCommand.setLeftOnClickListener(v -> mUhf.getTagRemovedEventThreshold(mTemp));
        mTagRemovedEventThresholdCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mTagRemovedEventThresholdCommand.getViewDataArray()[0];
            mUhf.setTagRemovedEventThreshold(mTemp, (MissingInventoryThreshold) viewData.getSelected());
        });
    }

    private void newAdvanceCommands() {
        if (mUhf instanceof TS100) {
            newTS100Commands();
        } else if (mUhf instanceof TS800) {
            newTS800Commands();
        } else if (mUhf instanceof UR0250) {
            newUr0250Commands();
        } else if (mUhf instanceof MU400H) {
            newMU400Commands();
        }
    }

    private void newTS800Commands() {
        final TS800 ts800 = (TS800) mUhf;
        newBuzzerOperationCommand(ts800);
        newControlBuzzerCommand(mUhf);
        newTS800TriggerCommand(ts800);
        newTS800IoStateCommand(ts800);
        newOutputInterfaceCommand(ts800);
    }

    private void showTS800AdvanceSettingViews() {
        mAdapter.add(mBuzzerOperationCommand);
        mAdapter.add(mControlBuzzerCommand);
        mAdapter.add(mTriggerCommand);
        mAdapter.add(mIoStateCommand);
        mAdapter.add(mOutputInterfaceCommand);
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
        newBuzzerOperationCommand((TS100) mUhf);
        newControlBuzzerCommand(mUhf);
        newOutputInterfacesCommand((TS100) mUhf);
        newEventTypeCommand(mUhf);
        newEnableFilterCommand(mUhf);
        newPostDataDelimiterCommand(mUhf);
        newMemoryBankSelectionCommand(mUhf);
    }

    void showTS100AdvanceSettingViews() {
        mAdapter.add(mBuzzerOperationCommand);
        mAdapter.add(mControlBuzzerCommand);
        mAdapter.add(mOutputInterfacesCommand);
        mAdapter.add(mEventTypeCommand);
        mAdapter.add(mFilterCommand);
        mAdapter.add(mPostDataDelimiterCommand);
        mAdapter.add(mMemoryBankSelectionCommand);
    }

    private void newMU400Commands() {
        newEventTypeCommand(mUhf);
        newEnableFilterCommand(mUhf);
        newPostDataDelimiterCommand(mUhf);
        newMemoryBankSelectionCommand(mUhf);
        newOutputInterfaceCommand((MU400H) mUhf);
    }

    void showMU400AdvanceSettingViews() {
        mAdapter.add(mEventTypeCommand);
        mAdapter.add(mFilterCommand);
        mAdapter.add(mPostDataDelimiterCommand);
        mAdapter.add(mMemoryBankSelectionCommand);
        mAdapter.add(mOutputInterfaceCommand);
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
        mIoStateCommand = new GeneralCommandItem("Get/Set I/O State", new TwoSpinnerParamData<>(ioNumbers, IOState.values()));
        mIoStateCommand.setLeftOnClickListener(v -> ts800.getIOState());
        mIoStateCommand.setRightOnClickListener(v -> {
            TwoSpinnerParamData twoSpinnerParamData = (TwoSpinnerParamData) mIoStateCommand.getViewDataArray()[0];
            IONumber firstSelected = (IONumber) twoSpinnerParamData.getFirstSelected();
            IOState secondSelected = (IOState) twoSpinnerParamData.getSecondSelected();
            ts800.setIOState(firstSelected, secondSelected);
        });
    }

    private void newTS800TriggerCommand(final TS800 ts800) {
        mTriggerCommand = new GeneralCommandItem("Get/Set Trigger", new SpinnerParamData<>(TriggerType.class));
        mTriggerCommand.setLeftOnClickListener(v -> ts800.getTriggerType(mTemp));
        mTriggerCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mTriggerCommand.getViewDataArray()[0];
            ts800.setTriggerType(mTemp, (TriggerType) viewData.getSelected());
        });
    }

    private void newUr0250TriggerCommand(final UR0250 ur0250) {
        mTriggerCommand = new GeneralCommandItem("Get/Set Trigger", new SpinnerParamData<>(TriggerType.class));
        mTriggerCommand.setLeftOnClickListener(v -> ur0250.getTriggerType(mTemp));
        mTriggerCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mTriggerCommand.getViewDataArray()[0];
            ur0250.setTriggerType(mTemp, (TriggerType) viewData.getSelected());
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
        mIoStateCommand = new GeneralCommandItem("Set I/O State", new TwoSpinnerParamData<>(ioNumbers, IOState.values()));
        mIoStateCommand.setLeftOnClickListener(v -> ur0250.getIOState());
        mIoStateCommand.setRightOnClickListener(v -> {
            TwoSpinnerParamData twoSpinnerParamData = (TwoSpinnerParamData) mIoStateCommand.getViewDataArray()[0];
            IONumber firstSelected = (IONumber) twoSpinnerParamData.getFirstSelected();
            IOState secondSelected = (IOState) twoSpinnerParamData.getSecondSelected();
            ur0250.setIOState(firstSelected, secondSelected);
        });
    }

    private void newStartInventoryCommandEx(final UHFDevice uhfDevice) {
        mInventoryExCommand = new GeneralCommandItem("Start Inventory Ex", null, "Start Ex", new CheckboxParamData<>(TagDataEncodeType.class));
        mInventoryExCommand.setRightOnClickListener(v -> {
            CheckboxParamData viewData = (CheckboxParamData) mInventoryExCommand.getViewDataArray()[0];
            if (uhfDevice instanceof TS100) {
                ((TS100) uhfDevice).startInventoryEx(viewData.getSelected());
            } else if (uhfDevice instanceof MU400H) {
                ((MU400H) uhfDevice).startInventoryEx(viewData.getSelected());
            }
        });
    }

    private void newStartInventoryCommand() {
        mInventoryCommand = new GeneralCommandItem("Start Inventory", null, "Start", new SpinnerParamData<>(TagPresentedType.class));
        mInventoryCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mInventoryCommand.getViewDataArray()[0];
            mUhf.startInventory((TagPresentedType) viewData.getSelected());
        });
    }

    private void newStopInventoryCommand() {
        mStopInventoryCommand = new GeneralCommandItem("Stop Inventory", null, "Stop");
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

    private void newEnableFilterCommand(final UHFDevice uhfDevice) {
        mFilterCommand = new GeneralCommandItem("Get/Set Filter",
                new CheckboxParamData<>(TagDataEncodeType.class));
        mFilterCommand.setRightOnClickListener(v -> {
            CheckboxParamData viewData = (CheckboxParamData) mFilterCommand.getViewDataArray()[0];
            if (uhfDevice instanceof TS100) {
                ((TS100) uhfDevice).setFilter(mTemp, viewData.getSelected());
            } else if (uhfDevice instanceof MU400H) {
                ((MU400H) uhfDevice).setFilter(mTemp, viewData.getSelected());
            }
        });
        mFilterCommand.setLeftOnClickListener(v -> {
            if (uhfDevice instanceof TS100) {
                ((TS100) uhfDevice).getFilter(mTemp);
            } else if (uhfDevice instanceof MU400H) {
                ((MU400H) uhfDevice).getFilter(mTemp);
            }
        });
    }

    private void newControlBuzzerCommand(final UHFDevice uhfDevice) {
        mControlBuzzerCommand = new GeneralCommandItem("Control Buzzer", null, "Control", new SpinnerParamData<>(BuzzerAction.class))
        ;
        mControlBuzzerCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mControlBuzzerCommand.getViewDataArray()[0];
            if (uhfDevice instanceof TS100) {
                ((TS100) uhfDevice).controlBuzzer((BuzzerAction) viewData.getSelected());
            } else if (uhfDevice instanceof TS800) {
                ((TS800) uhfDevice).controlBuzzer((BuzzerAction) viewData.getSelected());
            }
        });
    }

    private void newBuzzerOperationCommand(final TS100 ts100) {
        mBuzzerOperationCommand = new GeneralCommandItem("Get/Set Buzzer Operation", new SpinnerParamData<>(BuzzerOperationMode.class));
        mBuzzerOperationCommand.setLeftOnClickListener(v -> {
            ts100.getBuzzerOperationMode(mTemp);
        });
        mBuzzerOperationCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mBuzzerOperationCommand.getViewDataArray()[0];
            ts100.setBuzzerOperationMode(mTemp, (BuzzerOperationMode) viewData.getSelected());
        });
    }

    private void newBuzzerOperationCommand(final TS800 ts800) {
        mBuzzerOperationCommand = new GeneralCommandItem("Get/Set Buzzer Operation", new SpinnerParamData<>(new BuzzerOperationMode[]{BuzzerOperationMode.OFF, BuzzerOperationMode.REPEAT}));
        mBuzzerOperationCommand.setLeftOnClickListener(v -> {
            ts800.getBuzzerOperationMode(mTemp);
        });
        mBuzzerOperationCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mBuzzerOperationCommand.getViewDataArray()[0];
            ts800.setBuzzerOperationMode(mTemp, (BuzzerOperationMode) viewData.getSelected());
        });
    }

    private void newMemoryBankSelectionCommand(final UHFDevice uhfDevice) {
        mMemoryBankSelectionCommand = new GeneralCommandItem("Get/Set Memory Bank Selection", new SpinnerParamData<>(MemoryBankSelection.class));
        mMemoryBankSelectionCommand.setLeftOnClickListener(v -> {
            if (uhfDevice instanceof TS100) {
                ((TS100) uhfDevice).getMemoryBankSelection(mTemp);
            } else if (uhfDevice instanceof MU400H) {
                ((MU400H) uhfDevice).getMemoryBankSelection(mTemp);
            }
        });

        mMemoryBankSelectionCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mMemoryBankSelectionCommand.getViewDataArray()[0];
            if (uhfDevice instanceof TS100) {
                ((TS100) uhfDevice).setMemoryBankSelection(mTemp, (MemoryBankSelection) viewData.getSelected());
            } else if (uhfDevice instanceof MU400H) {
                ((MU400H) uhfDevice).setMemoryBankSelection(mTemp, (MemoryBankSelection) viewData.getSelected());
            }
        });
    }

    private void newPostDataDelimiterCommand(final UHFDevice uhfDevice) {
        mPostDataDelimiterCommand = new GeneralCommandItem("Get/Set Post Data Delimiter", new SpinnerParamData<>(PostDataDelimiter.class));
        mPostDataDelimiterCommand.setLeftOnClickListener(v -> {
            if (uhfDevice instanceof TS100) {
                ((TS100) uhfDevice).getPostDataDelimiter(mTemp);
            } else if (uhfDevice instanceof MU400H) {
                ((MU400H) uhfDevice).getPostDataDelimiter(mTemp);
            }
        });
        mPostDataDelimiterCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mPostDataDelimiterCommand.getViewDataArray()[0];
            if (uhfDevice instanceof TS100) {
                ((TS100) uhfDevice).setPostDataDelimiter(mTemp, (PostDataDelimiter) viewData.getSelected());
            } else if (uhfDevice instanceof MU400H) {
                ((MU400H) uhfDevice).setPostDataDelimiter(mTemp, (PostDataDelimiter) viewData.getSelected());
            }
        });
    }

    private void newOutputInterfaceCommand(final TS800 ts800) {
        mOutputInterfaceCommand = new GeneralCommandItem("Get/Set Output Interface"
                , new SpinnerParamData<>(new OutputInterface[]{DEFAULT, TCP_SERVER}));
        mOutputInterfaceCommand.setLeftOnClickListener(v -> ts800.getOutputInterface(mTemp));
        mOutputInterfaceCommand.setRightOnClickListener(v -> {
            SpinnerParamData outputInterface = (SpinnerParamData) mOutputInterfaceCommand.getViewDataArray()[0];
            ts800.setOutputInterface(mTemp, (OutputInterface) outputInterface.getSelected());
        });
    }

    private void newOutputInterfaceCommand(final MU400H mu400H) {
        mOutputInterfaceCommand = new GeneralCommandItem("Get/Set Output Interface"
                , new SpinnerParamData<>(new OutputInterface[]{DEFAULT, HID_KEYBOARD}));
        mOutputInterfaceCommand.setLeftOnClickListener(v -> mu400H.getOutputInterface(mTemp));
        mOutputInterfaceCommand.setRightOnClickListener(v -> {
            SpinnerParamData outputInterface = (SpinnerParamData) mOutputInterfaceCommand.getViewDataArray()[0];
            mu400H.setOutputInterface(mTemp, (OutputInterface) outputInterface.getSelected());
        });
    }

    private void newOutputInterfacesCommand(final TS100 ts100) {
        Set<OutputInterface> outputInterfaces = new HashSet<>();
        outputInterfaces.add(OutputInterface.HID_N_VCOM);
        outputInterfaces.add(OutputInterface.TCP_CLIENT);
        outputInterfaces.add(TCP_SERVER);
        outputInterfaces.add(OutputInterface.BLE);
        mOutputInterfacesCommand = new GeneralCommandItem("Get/Set Output Interfaces"
                , new SpinnerParamData<>(KeyboardSimulation.class)
                , new CheckboxParamData<>(outputInterfaces));
        mOutputInterfacesCommand.setLeftOnClickListener(v -> {
            ts100.getOutputInterfaces(mTemp);
        });
        mOutputInterfacesCommand.setRightOnClickListener(v -> {
            SpinnerParamData keyboard = (SpinnerParamData) mOutputInterfacesCommand.getViewDataArray()[0];
            CheckboxParamData outputInterface = (CheckboxParamData) mOutputInterfacesCommand.getViewDataArray()[1];
            ts100.setOutputInterfaces(mTemp, (KeyboardSimulation) keyboard.getSelected(), outputInterface.getSelected());
        });
    }

    private void newEventTypeCommand(final UHFDevice uhfDevice) {
        mEventTypeCommand = new GeneralCommandItem("Get/Set Event Type", new SpinnerParamData<>(EventType.class));
        mEventTypeCommand.setLeftOnClickListener(v -> {
            if (uhfDevice instanceof TS100) {
                ((TS100) uhfDevice).getEventType(mTemp);
            } else if (uhfDevice instanceof MU400H) {
                ((MU400H) uhfDevice).getEventType(mTemp);
            }
        });
        mEventTypeCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mEventTypeCommand.getViewDataArray()[0];
            if (uhfDevice instanceof TS100) {
                ((TS100) uhfDevice).setEventType(mTemp, (EventType) viewData.getSelected());
            } else if (uhfDevice instanceof MU400H) {
                ((MU400H) uhfDevice).setEventType(mTemp, (EventType) viewData.getSelected());
            }
        });
    }

    private void newFrequencyCommand() {
        mFrequencyCommand = new GeneralCommandItem("Get/Set Frequency", new EditTextParamData("840.250, 842.000, 843.250"));
        mFrequencyCommand.setLeftOnClickListener(v -> mUhf.getFrequency(mTemp));
        mFrequencyCommand.setRightOnClickListener(v -> {
            EditTextParamData viewData = (EditTextParamData) mFrequencyCommand.getViewDataArray()[0];
            String selected = viewData.getSelected();
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
        mQCommand = new GeneralCommandItem("Get/Set Q", new SeekBarParamData(0, 15));
        mQCommand.setLeftOnClickListener(v -> mUhf.getQValue(mTemp));
        mQCommand.setRightOnClickListener(v -> {
            SeekBarParamData viewData = (SeekBarParamData) mQCommand.getViewDataArray()[0];
            mUhf.setQValue(mTemp, viewData.getSelected());
        });
    }

    private void newSessionTargetCommand() {
        mSessionTargetCommand = new GeneralCommandItem("Get/Set Session and Target", new TwoSpinnerParamData<>(Session.values(), Target.getAbTargets()));
        TwoSpinnerParamData viewData = (TwoSpinnerParamData) mSessionTargetCommand.getViewDataArray()[0];
        viewData.setOnFirstItemSelected(selected -> {
            if (selected.equals(SL) && !Target.slContains((Target) viewData.getSecondEnums()[0])) {
                viewData.setSecondEnums(Target.getSlTargets());
                mAdapter.notifyItemChanged(mSessionTargetCommand.getPosition());
            } else if ((!selected.equals(SL)) && !Target.abContains((Target) viewData.getSecondEnums()[0])) {
                viewData.setSecondEnums(Target.getAbTargets());
                mAdapter.notifyItemChanged(mSessionTargetCommand.getPosition());
            }
        });
        mSessionTargetCommand.setLeftOnClickListener(v -> mUhf.getSessionAndTarget(mTemp));
        mSessionTargetCommand.setRightOnClickListener(v -> {
            Session session = (Session) viewData.getFirstSelected();
            Target target = (Target) viewData.getSecondSelected();
            mUhf.setSessionAndTarget(mTemp, session, target);
        });
    }

    private void newRfSensitivityCommand() {
        mRfSensitivityCommand = new GeneralCommandItem("Get/Set RF Sensitivity", new SeekBarParamData(1, 14));
        mRfSensitivityCommand.setLeftOnClickListener(v -> mUhf.getRfSensitivity(mTemp));
        mRfSensitivityCommand.setRightOnClickListener(v -> {
            SeekBarParamData viewData = (SeekBarParamData) mRfSensitivityCommand.getViewDataArray()[0];
            mUhf.setRfSensitivity(mTemp, RfSensitivityLevel.getSensitivityFrom(viewData.getSelected()));
        });
    }

    private void newRfPowerCommand() {
        mRfPowerCommand = new GeneralCommandItem("Get/Set RF Power", new SeekBarParamData(1, 27));
        mRfPowerCommand.setLeftOnClickListener(v -> mUhf.getRfPower(mTemp));
        mRfPowerCommand.setRightOnClickListener(v -> {
            SeekBarParamData viewData = (SeekBarParamData) mRfPowerCommand.getViewDataArray()[0];
            mUhf.setRfPower(mTemp, (byte) viewData.getSelected());
        });
    }

    private void newBleDeviceNameCommand() {
        mBleDeviceNameCommand = new GeneralCommandItem("Get/Set BLE Device Name", new EditTextParamData("BLE Device Name"));
        mBleDeviceNameCommand.setRightOnClickListener(v -> {
            String deviceName = ((EditTextParamData) mBleDeviceNameCommand.getViewDataArray()[0]).getSelected();
            mUhf.setBleDeviceName(deviceName);
        });
        mBleDeviceNameCommand.setLeftOnClickListener(v -> mUhf.getBleDeviceName());
    }

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
        //TODO when usb is mUHf
        GLog.d(TAG, mUhf.toString());
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
            SeekBarParamData viewData = (SeekBarParamData) mRfPowerCommand.getViewDataArray()[0];
            viewData.setSelected(rfPower);
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
            SeekBarParamData viewData = (SeekBarParamData) mRfSensitivityCommand.getViewDataArray()[0];
            viewData.setSelected(sensitivityValue);
            mAdapter.notifyItemChanged(mRfSensitivityCommand.getPosition());
            onUpdateLog(TAG, "didGetRfSensitivity: " + sensitivity.name());
        }

        @Override
        public void didGetFrequencyList(final List<Double> frequencyList) {
            String frequencyData = Arrays.toString(frequencyList.toArray());
            EditTextParamData selected = (EditTextParamData) mFrequencyCommand.getViewDataArray()[0];
            selected.setSelected(frequencyData.replace("[", "").replace("]", ""));
            mAdapter.notifyItemChanged(mFrequencyCommand.getPosition());
            onUpdateLog(TAG, "didGetFrequencyList:\n" + frequencyData);
        }

        @Override
        public void didGetSessionAndTarget(final Session session, final Target target) {
            TwoSpinnerParamData viewData = (TwoSpinnerParamData) mSessionTargetCommand.getViewDataArray()[0];
            viewData.setFirstSelected(session);
            viewData.setSecondSelected(target);
            mAdapter.notifyItemChanged(mSessionTargetCommand.getPosition());
            onUpdateLog(TAG, "didGetSessionAndTarget:" +
                    "\n\tSession: " + session.name() +
                    "\n\tTarget: " + target.name());
        }

        @Override
        public void didGetQValue(final byte qValue) {
            SeekBarParamData viewData = (SeekBarParamData) mQCommand.getViewDataArray()[0];
            viewData.setSelected(qValue);
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
            EditTextParamData selected = (EditTextParamData) mBleDeviceNameCommand.getViewDataArray()[0];
            selected.setSelected(bleDeviceName);
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
            SpinnerParamData selected = (SpinnerParamData) mEventTypeCommand.getViewDataArray()[0];
            selected.setSelected(eventType);
            mAdapter.notifyItemChanged(mEventTypeCommand.getPosition());
            onUpdateLog(TAG, "didGetEventType: " + eventType.name());
        }

        @Override
        public void didGetOutputInterfaces(KeyboardSimulation keyboardSimulation, final Set<OutputInterface> outputInterfaces) {
            SpinnerParamData selected1 = (SpinnerParamData) mOutputInterfacesCommand.getViewDataArray()[0];
            selected1.setSelected(keyboardSimulation);
            CheckboxParamData selected2 = (CheckboxParamData) mOutputInterfacesCommand.getViewDataArray()[1];
            selected2.setSelected(outputInterfaces);
            mAdapter.notifyItemChanged(mOutputInterfacesCommand.getPosition());
            onUpdateLog(TAG, "didGetOutputInterface: " +
                    "\n\tKeyboardSimulation: " + keyboardSimulation +
                    "\n\tOutputInterface: " + Arrays.toString(outputInterfaces.toArray()));
        }

        @Override
        public void didGetBuzzerOperationMode(final BuzzerOperationMode buzzerOperationMode) {
            SpinnerParamData selected1 = (SpinnerParamData) mBuzzerOperationCommand.getViewDataArray()[0];
            selected1.setSelected(buzzerOperationMode);
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
            SpinnerParamData selected1 = (SpinnerParamData) mTriggerCommand.getViewDataArray()[0];
            selected1.setSelected(triggerSource);
            mAdapter.notifyItemChanged(mTriggerCommand.getPosition());
            onUpdateLog(TAG, "didGetTriggerType: " + triggerSource.name());
        }

        @Override
        public void didDiscoverTagInfo(TagInformationFormat tagInformationFormat) {
            String message = "didDiscoverTagInfo(Inventory Event):" +
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
        public void didTagRemoved(TagInformationFormat tagInformationFormat) {
            String message = "didTagRemoved:" +
                    "\n\tFrequency: " + tagInformationFormat.getFrequency() +
                    "\n\tRssi: " + tagInformationFormat.getRssi() +
                    "\n\tPC EPC: " + tagInformationFormat.getPcEPCHex();
            if (tagInformationFormat.getTidHex().length() != 0) {
                message = message + "\n\tTID: " + tagInformationFormat.getTidHex();
            }
            onUpdateLog(TAG, message);
        }

        @Override
        public void didReadEpc(byte[] epc) {
            mReadWriteTagFragment.setEpc(epc);
            onUpdateLog(TAG, "didReadEpc: " + GTool.bytesToHexString(epc));
        }

        @Override
        public void didGetPostDataDelimiter(PostDataDelimiter postDataDelimiter) {
            SpinnerParamData selected1 = (SpinnerParamData) mPostDataDelimiterCommand.getViewDataArray()[0];
            selected1.setSelected(postDataDelimiter);
            mAdapter.notifyItemChanged(mPostDataDelimiterCommand.getPosition());
            onUpdateLog(TAG, "didGetPostDataDelimiter: " + postDataDelimiter);
        }

        @Override
        public void didGetMemoryBankSelection(MemoryBankSelection memoryBankSelection) {
            SpinnerParamData selected1 = (SpinnerParamData) mMemoryBankSelectionCommand.getViewDataArray()[0];
            selected1.setSelected(memoryBankSelection);
            mAdapter.notifyItemChanged(mMemoryBankSelectionCommand.getPosition());
            onUpdateLog(TAG, "didGetMemoryBankSelection: " + memoryBankSelection);
        }

        @Override
        public void didGetFilter(Set<TagDataEncodeType> tagDataEncodeTypes) {
            CheckboxParamData selected1 = (CheckboxParamData) mFilterCommand.getViewDataArray()[0];
            selected1.setSelected(tagDataEncodeTypes);
            mAdapter.notifyItemChanged(mFilterCommand.getPosition());
            onUpdateLog(TAG, "didGetFilter: " + Arrays.toString(tagDataEncodeTypes.toArray()));
        }

        @Override
        public void didGetTagPresentedEventThreshold(int period) {
            EditTextParamData selected = (EditTextParamData) mTagPresentedEventThresholdCommand.getViewDataArray()[0];
            selected.setSelected(period + "");
            mAdapter.notifyItemChanged(mTagPresentedEventThresholdCommand.getPosition());
            onUpdateLog(TAG, "didGetTagPresentedEventThreshold: " + period + "s");
        }

        @Override
        public void didGetTagRemovedEventThreshold(MissingInventoryThreshold missingInventoryThreshold) {
            SpinnerParamData selected1 = (SpinnerParamData) mTagRemovedEventThresholdCommand.getViewDataArray()[0];
            selected1.setSelected(missingInventoryThreshold);
            mAdapter.notifyItemChanged(mTagRemovedEventThresholdCommand.getPosition());
            onUpdateLog(TAG, "didGetTagRemovedEventThreshold: " + missingInventoryThreshold.name());
        }

        @Override
        public void didGetOutputInterface(OutputInterface outputInterface) {
            SpinnerParamData selected1 = (SpinnerParamData) mOutputInterfaceCommand.getViewDataArray()[0];
            selected1.setSelected(outputInterface);
            mAdapter.notifyItemChanged(mOutputInterfaceCommand.getPosition());
            onUpdateLog(TAG, "didGetOutputInterface: " + outputInterface.name());
        }
    };

    @Override
    public void didUpdateConnection(final ConnectionState connectState, CommunicationType type) {
        GLog.d(TAG, "Connected Device: " + Arrays.toString(ConnectedDevices.getInstance().keySet().toArray()));
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> updateConnectionViews(connectState));
        }
        if (connectState.equals(ConnectionState.CONNECTED)) {
            mUhf.initializeSettings();
        }
        onUpdateDebugLog(TAG, "didUpdateConnection: " + mUhf.getDeviceID() + ": " + connectState.name());
    }

    void updateConnectionViews(ConnectionState connectState) {
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
        GLog.d(TAG, "onDestroy");
        getChildFragmentManager().beginTransaction()
                .remove(mReadWriteTagFragment);
        ConnectedDevices.getInstance().clear(mUhf.getDeviceID());
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

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            GLog.d(TAG, "Broadcast action: " + action);
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