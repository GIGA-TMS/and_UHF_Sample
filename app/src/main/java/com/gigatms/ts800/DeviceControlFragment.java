package com.gigatms.ts800;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
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
import com.gigatms.ts800.command.CheckboxCommand;
import com.gigatms.ts800.command.Command;
import com.gigatms.ts800.command.EditTextCommand;
import com.gigatms.ts800.command.SeekBarCommand;
import com.gigatms.ts800.command.SpinnerCommand;
import com.gigatms.ts800.command.TwoSpinnerCommand;
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
    public void onResume() {
        super.onResume();
        GLog.d(TAG, Arrays.toString(ConnectedDevices.getInstance().keySet().toArray()));
        initReadWriteFragment();
        if (getActivity() != null) {
            getActivity().setTitle(mUhf.getDeviceName());
        }
        setDeviceInformation();
        initCommandViews();
        setConnectionButton();
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
            case R.id.read_write_tag:
                break;
        }
        switchView(mBottomNavigationView.getSelectedItemId());
        mUhf.getRomVersion();
        mUhf.getBleRomVersion();
        mUhf.initializeSettings();
    }

    private void initReadWriteFragment() {
        mReadWriteTagFragment = ReadWriteTagFragment.newFragment(mUhf.getDeviceMacAddr());
        Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (getFragmentManager() != null) {
                    getFragmentManager().beginTransaction()
                            .replace(R.id.device_container, mReadWriteTagFragment)
                            .commit();
                }
            }
        });
        mFrameLayout.setVisibility(View.INVISIBLE);
    }

    private void initCommandViews() {
        mAdapter = new CommandRecyclerViewAdapter(getContext());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setHasFixedSize(true);

        newInventoryCommands();
        newSettingCommands();
        newAdvanceCommands();
        mRecyclerView.setAdapter(mAdapter);
        mBottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
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
            }


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

    private void showUR0250AdvanceSettingViews() {
        mAdapter.add(mTriggerCommand);
        mAdapter.add(mIoStateCommand);
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

    private void showTS800AdvanceSettingViews() {
        mAdapter.add(mTriggerCommand);
        mAdapter.add(mIoStateCommand);
    }

    private void newUr0250Commands() {
        final UR0250 ur0250 = (UR0250) mUhf;
        newUr0250TriggerCommand(ur0250);
        newUr0250IoStateCommand(ur0250);
    }

    void showTS100AdvanceSettingViews() {
        mAdapter.add(mBuzzerOperationCommand);
        mAdapter.add(mControlBuzzerCommand);
        mAdapter.add(mOutputInterfaceCommand);
        mAdapter.add(mEventTypeCommand);
        mAdapter.add(mEnableFilterCommand);
        mAdapter.add(mDisableFilterCommand);
        mAdapter.add(mPostDataDelimiterCommand);
        mAdapter.add(mMemoryBankSelectionCommand);
    }

    private void newTS800Commands() {
        final TS800 ts800 = (TS800) mUhf;
        newTS800TriggerCommand(ts800);
        newTS800IoStateCommand(ts800);
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
    }

    private void newTS100DisableFilterCommand(final TS100 ts100) {
        mDisableFilterCommand = new Command("Disable Filter", null, "Disable");
        mDisableFilterCommand.setRightOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ts100.disableFilter(mTemp);
            }
        });
    }

    private void newTS100EnableFilterCommand(final TS100 ts100) {
        mEnableFilterCommand = new CheckboxCommand<>("Enable Filter", null, "Enable", TagDataEncodeType.class);
        mEnableFilterCommand.setRightOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Set<TagDataEncodeType> tagDataEncodeTypes = mEnableFilterCommand.getSelected();
                ts100.enableFilter(mTemp, tagDataEncodeTypes);
            }
        });
    }

    private void newTS100ControlBuzzerCommand(final TS100 ts100) {
        mControlBuzzerCommand = new SpinnerCommand<>("Control Buzzer", null, "Control", BuzzerAction.class);
        mControlBuzzerCommand.setRightOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BuzzerAction buzzerAction = (BuzzerAction) mControlBuzzerCommand.getSelected();
                ts100.controlBuzzer(buzzerAction);
            }
        });
    }

    private void newTS100BuzzerOperationCommand(final TS100 ts100) {
        mBuzzerOperationCommand = new SpinnerCommand<>("Get/Set Buzzer Operation", BuzzerOperationMode.class);
        mBuzzerOperationCommand.setLeftOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ts100.getBuzzerOperationMode(mTemp);
            }
        });
        mBuzzerOperationCommand.setRightOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BuzzerOperationMode mode = (BuzzerOperationMode) mBuzzerOperationCommand.getSelected();
                ts100.setBuzzerOperationMode(mTemp, mode);
            }
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
        mIoStateCommand.setLeftOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ts800.getIOState();
            }
        });
        mIoStateCommand.setRightOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IONumber firstSelected = (IONumber) mIoStateCommand.getFirstSelected();
                IOState secondSelected = (IOState) mIoStateCommand.getSecondSelected();
                ts800.setIOState(firstSelected, secondSelected);
            }
        });
    }

    private void newTS800TriggerCommand(final TS800 ts800) {
        mTriggerCommand = new SpinnerCommand<>("Get/Set Trigger", TriggerType.class);
        mTriggerCommand.setLeftOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ts800.getTriggerType(mTemp);
            }
        });
        mTriggerCommand.setRightOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TriggerType triggerType = (TriggerType) mTriggerCommand.getSelected();
                ts800.setTriggerType(mTemp, triggerType);
            }
        });
    }

    private void newUr0250TriggerCommand(final UR0250 ur0250) {
        mTriggerCommand = new SpinnerCommand<>("Get/Set Trigger", TriggerType.class);
        mTriggerCommand.setLeftOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ur0250.getTriggerType(mTemp);
            }
        });
        mTriggerCommand.setRightOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TriggerType triggerType = (TriggerType) mTriggerCommand.getSelected();
                ur0250.setTriggerType(mTemp, triggerType);
            }
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
        mIoStateCommand.setLeftOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ur0250.getIOState();
            }
        });
        mIoStateCommand.setRightOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IONumber firstSelected = (IONumber) mIoStateCommand.getFirstSelected();
                IOState secondSelected = (IOState) mIoStateCommand.getSecondSelected();
                ur0250.setIOState(firstSelected, secondSelected);
            }
        });
    }

    private void newStartInventoryCommandEx(final TS100 ts100) {
        mInventoryExCommand = new CheckboxCommand<>("Start Inventory Ex", null, "Start Ex", TagDataEncodeType.class);
        mInventoryExCommand.setRightOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ts100.startInventoryEx(mInventoryExCommand.getSelected());
            }
        });
    }

    private void newStartInventoryCommand() {
        mInventoryCommand = new SpinnerCommand<>("Start Inventory", null, "Start", TagPresentedType.class);
        mInventoryCommand.setRightOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TagPresentedType tagPresentedType = (TagPresentedType) mInventoryCommand.getSelected();
                mUhf.startInventory(tagPresentedType);
            }
        });
    }

    private void newStopInventoryCommand() {
        mStopInventoryCommand = new Command("Stop Inventory", null, "Stop");
        mStopInventoryCommand.setRightOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUhf.stopInventory();
            }
        });
    }

    void switchView(int menuItemId) {
        mRecyclerView.setVisibility(menuItemId == R.id.read_write_tag ? View.INVISIBLE : View.VISIBLE);
        mFrameLayout.setVisibility(menuItemId == R.id.read_write_tag ? View.VISIBLE : View.INVISIBLE);
    }

    private void newTS100MemoryBankSelectionCommand(final TS100 ts100) {
        mMemoryBankSelectionCommand = new CheckboxCommand<>("Get/Set Memory Bank Selection", MemoryBankSelection.class);
        mMemoryBankSelectionCommand.setLeftOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ts100.getMemoryBankSelection(mTemp);
            }
        });

        mMemoryBankSelectionCommand.setRightOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Set<MemoryBankSelection> selected = mMemoryBankSelectionCommand.getSelected();
                ts100.setMemoryBankSelection(mTemp, selected);
            }
        });
    }

    private void newTS100PostDataDelimiterCommand(final TS100 ts100) {
        mPostDataDelimiterCommand = new SpinnerCommand<>("Get/Set Post Data Delimiter", PostDataDelimiter.class);
        mPostDataDelimiterCommand.setLeftOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ts100.getPostDataDelimiter(mTemp);
            }
        });
        mPostDataDelimiterCommand.setRightOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PostDataDelimiter postDataDelimiter = (PostDataDelimiter) mPostDataDelimiterCommand.getSelected();
                ts100.setPostDataDelimiter(mTemp, postDataDelimiter);
            }
        });
    }

    private void newTS100OutputInterfaceCommand(final TS100 ts100) {
        mOutputInterfaceCommand = new CheckboxCommand<>("Get/Set Output Interface", OutputInterface.class);
        mOutputInterfaceCommand.setLeftOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ts100.getOutputInterface(mTemp);
            }
        });
        mOutputInterfaceCommand.setRightOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Set<OutputInterface> selected = mOutputInterfaceCommand.getSelected();
                ts100.setOutputInterface(mTemp, selected);
            }
        });
    }

    private void newTS100EventTypeCommand(final TS100 ts100) {
        mEventTypeCommand = new SpinnerCommand<>("Get/Set Event Type", EventType.class);
        mEventTypeCommand.setLeftOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ts100.getEventType(mTemp);
            }
        });
        mEventTypeCommand.setRightOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventType selected = (EventType) mEventTypeCommand.getSelected();
                ts100.setEventType(mTemp, selected);
            }
        });
    }

    private void newFrequencyCommand() {
        mFrequencyCommand = new EditTextCommand("Get/Set Frequency", "840.250, 842.000, 843.250");
        mFrequencyCommand.setLeftOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUhf.getFrequency(mTemp);
            }
        });
        mFrequencyCommand.setRightOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });
    }

    private void newQCommand() {
        mQCommand = new SeekBarCommand("Get/Set Q", 0, 15);
        mQCommand.setLeftOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUhf.getQValue(mTemp);
            }
        });
        mQCommand.setRightOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selected = mQCommand.getSelected();
                mUhf.setQValue(mTemp, selected);
            }
        });
    }

    private void newSessionTargetCommand() {
        mSessionTargetCommand = new TwoSpinnerCommand<>("Get/Set Session and Target", Session.values(), Target.getAbTargets());
        mSessionTargetCommand.setOnFirstItemSelected(new TwoSpinnerCommand.OnFirstItemSelected() {
            @Override
            public void onFirstItemSelected(Enum selected) {
                if (selected.equals(SL) && !Target.slContains((Target) mSessionTargetCommand.getSecondEnums()[0])) {
                    mSessionTargetCommand.setSecondEnums(Target.getSlTargets());
                    mAdapter.notifyDataSetChanged();
                } else if ((!selected.equals(SL)) && !Target.abContains((Target) mSessionTargetCommand.getSecondEnums()[0])) {
                    mSessionTargetCommand.setSecondEnums(Target.getAbTargets());
                    mAdapter.notifyDataSetChanged();
                }
            }
        });
        mSessionTargetCommand.setLeftOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUhf.getSessionAndTarget(mTemp);
            }
        });
        mSessionTargetCommand.setRightOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Session session = (Session) mSessionTargetCommand.getFirstSelected();
                Target target = (Target) mSessionTargetCommand.getSecondSelected();
                mUhf.setSessionAndTarget(mTemp, session, target);
            }
        });
    }

    private void newRfSensitivityCommand() {
        mRfSensitivityCommand = new SeekBarCommand("Get/Set RF Sensitivity", 1, 14);
        mRfSensitivityCommand.setLeftOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUhf.getRfSensitivity(mTemp);
            }
        });
        mRfSensitivityCommand.setRightOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int level = mRfSensitivityCommand.getSelected();
                mUhf.setRfSensitivity(mTemp, RfSensitivityLevel.getSensitivityFrom(level));
            }
        });
    }

    private void newRfPowerCommand() {
        mRfPowerCommand = new SeekBarCommand("Get/Set RF Power", 1, 27);
        mRfPowerCommand.setLeftOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUhf.getRfPower(mTemp);
            }
        });
        mRfPowerCommand.setRightOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte selected = (byte) mRfPowerCommand.getSelected();
                mUhf.setRfPower(mTemp, selected);
            }
        });
    }

    private void newBleDeviceNameCommand() {
        mBleDeviceNameCommand = new EditTextCommand("Get/Set BLE Device Name", "BLE Device Name");
        mBleDeviceNameCommand.setRightOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String deviceName = mBleDeviceNameCommand.getSelected();
                mUhf.setBleDeviceName(deviceName);
            }
        });
        mBleDeviceNameCommand.setLeftOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUhf.getBleDeviceName();
            }
        });
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
        mUhf.setCommunicationCallback(this);
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
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            getActivity().setTitle(mUhf.getDeviceName());
                        }
                    });
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
            mTvFirmware.post(new Runnable() {
                @Override
                public void run() {
                    mTvFirmware.setText(firmwareVersion);
                }
            });
            onUpdateLog(TAG, "didGetFirmwareVersion: " + firmwareVersion);
        }

        @Override
        public void didGetBleFirmwareVersion(final String firmwareVersion) {
            mTvBleFirmwareValue.post(new Runnable() {
                @Override
                public void run() {
                    mTvBleFirmwareValue.setText(firmwareVersion);
                }
            });
            onUpdateLog(TAG, "didGetBleFirmwareVersion: " + firmwareVersion);
        }

        @Override
        public void didGetRfPower(final byte rfPower) {
            mRfPowerCommand.didGetVale(rfPower);
            onUpdateLog(TAG, "didGetRfPower: " + rfPower);
        }

        @Override
        public void didGetRfSensitivity(final RfSensitivityLevel sensitivity) {
            String sensitivityName = sensitivity.name();
            String regEx = "[^0-9]";
            Pattern pattern = Pattern.compile(regEx);
            Matcher matcher = pattern.matcher(sensitivityName);
            final int sensitivityValue = Integer.parseInt(matcher.replaceAll("").trim());
            mRfSensitivityCommand.didGetVale(sensitivityValue);
            onUpdateLog(TAG, "didGetRfSensitivity: " + sensitivity.name());
        }

        @Override
        public void didGetFrequencyList(final ArrayList<Double> frequencyList) {
            String frequencyData = Arrays.toString(frequencyList.toArray());
            mFrequencyCommand.didGetVale(frequencyData.replace("[", "").replace("]", ""));
            onUpdateLog(TAG, "didGetFrequencyList:\n" + frequencyData);
        }

        @Override
        public void didGetSessionAndTarget(final Session session, final Target target) {
            mSessionTargetCommand.didGetValue(session, target);
            onUpdateLog(TAG, "didGetSessionAndTarget:" +
                    "\n\tSession: " + session.name() +
                    "\n\tTarget: " + target.name());
        }

        @Override
        public void didGetQValue(final byte qValue) {
            mQCommand.didGetVale(qValue);
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
            mBleDeviceNameCommand.didGetVale(bleDeviceName);
            onUpdateLog(TAG, "didGetBleDeviceName: " + bleDeviceName);
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getActivity().setTitle(mUhf.getDeviceName());
                    }
                });
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
            mEventTypeCommand.didGetValue(eventType);
            onUpdateLog(TAG, "didGetEventType: " + eventType.name());
        }

        @Override
        public void didGetOutputInterface(final Set<OutputInterface> outputInterfaces) {
            mOutputInterfaceCommand.didGetValue(outputInterfaces);
            onUpdateLog(TAG, "didGetOutputInterface: " + Arrays.toString(outputInterfaces.toArray()));
        }

        @Override
        public void didGetBuzzerOperationMode(final BuzzerOperationMode buzzerOperationMode) {
            mBuzzerOperationCommand.didGetValue(buzzerOperationMode);
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
            mTriggerCommand.didGetValue(triggerSource);
            onUpdateLog(TAG, "didGetTriggerType: " + triggerSource.name());
        }

        @Override
        public void didDiscoverTagInfo(TagInformationFormat tagInformationFormat) {
            String message = "didDiscoverTagInfo(Inventory Event):" +
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
                    "\n\t\tInputDecodeType: " + decodedTagData.getInputDecodeType() +
                    "\n\t\tDeviceSerialNumber: " + decodedTagData.getDeviceSerialNumber() +
                    "\n\t\tTID: " + decodedTagData.getTIDHexString() +
                    "\n\t\tTagDecimalSerialNumber: " + decodedTagData.getTagDecimalSerialNumber() +
                    "\n\t\tTagHexStringSerialNumber: " + decodedTagData.getTagSerialNumberHexString() +
                    "\n\t\tTagSerialNumberLength: " + decodedTagData.getTagSerialNumberLength() +
                    "\n\t\tInputDecodeType: " + decodedTagData.getInputDecodeType());

            for (DecodedTagData.DecodedData decodedData : decodedTagData.getDecodedDataList()) {
                message.append("\n\t\tOutputDataType: ")
                        .append(decodedData.getOutputDataType())
                        .append(" ").append("Data: ").append(decodedData.getOutputTypeStringData());
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
            mPostDataDelimiterCommand.didGetValue(postDataDelimiter);
            onUpdateLog(TAG, "didGetPostDataDelimiter: " + postDataDelimiter);
        }

        @Override
        public void didGetMemoryBankSelection(Set<MemoryBankSelection> memoryBankSelection) {
            mMemoryBankSelectionCommand.didGetValue(memoryBankSelection);
            onUpdateLog(TAG, "didGetMemoryBankSelection: " + Arrays.toString(memoryBankSelection.toArray()));
        }
    };

    @Override
    public void didUpdateConnection(final ConnectionState connectState, CommunicationType type) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Objects.requireNonNull(getActivity()).invalidateOptionsMenu();
                    updateConnectionViews(connectState);
                }
            });
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
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Objects.requireNonNull(getActivity()).invalidateOptionsMenu();
                    updateConnectionViews(mUhf.getConnectionState());
                }
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
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ConnectedDevices.getInstance().clear(mUhf.getDeviceMacAddr());
    }

    void setConnectionButton() {
        mBtnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });
    }
}