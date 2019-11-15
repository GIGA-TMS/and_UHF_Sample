package com.gigatms.uhf.deviceControl;

import android.os.Bundle;

import com.gigatms.CommunicationType;
import com.gigatms.DecodedTagData;
import com.gigatms.TS100;
import com.gigatms.TagInformationFormat;
import com.gigatms.UHFCallback;
import com.gigatms.uhf.DeviceControlFragment;
import com.gigatms.uhf.GeneralCommandItem;
import com.gigatms.uhf.paramsData.CheckboxListParamData;
import com.gigatms.uhf.paramsData.EditTextParamData;
import com.gigatms.uhf.paramsData.EditTextTitleParamData;
import com.gigatms.uhf.paramsData.SeekBarParamData;
import com.gigatms.uhf.paramsData.SpinnerParamData;
import com.gigatms.uhf.paramsData.TwoSpinnerParamData;
import com.gigatms.parameters.ActiveMode;
import com.gigatms.parameters.BuzzerAction;
import com.gigatms.parameters.BuzzerOperationMode;
import com.gigatms.parameters.EventType;
import com.gigatms.parameters.IONumber;
import com.gigatms.parameters.IOState;
import com.gigatms.parameters.KeyboardSimulation;
import com.gigatms.parameters.MemoryBank;
import com.gigatms.parameters.MemoryBankSelection;
import com.gigatms.parameters.OutputInterface;
import com.gigatms.parameters.PostDataDelimiter;
import com.gigatms.parameters.RfSensitivityLevel;
import com.gigatms.parameters.Session;
import com.gigatms.parameters.TagDataEncodeType;
import com.gigatms.parameters.TagPresentedType;
import com.gigatms.parameters.Target;
import com.gigatms.tools.GLog;
import com.gigatms.tools.GTool;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.gigatms.parameters.OutputInterface.TCP_SERVER;
import static com.gigatms.parameters.Session.SL;

public class TS100DeviceControlFragment extends DeviceControlFragment {
    private static final String TAG = TS100DeviceControlFragment.class.getSimpleName();

    private GeneralCommandItem mStopInventoryCommand;
    private GeneralCommandItem mInventoryCommand;
    private GeneralCommandItem mInventoryExCommand;
    private GeneralCommandItem mInventoryActiveMode;

    private GeneralCommandItem mRfPowerCommand;
    private GeneralCommandItem mRfSensitivityCommand;
    private GeneralCommandItem mSessionTargetCommand;
    private GeneralCommandItem mQCommand;
    private GeneralCommandItem mFrequencyCommand;
    private GeneralCommandItem mTagRemovedThresholdCommand;
    private GeneralCommandItem mTagPresentedRepeatIntervalCommand;
    private GeneralCommandItem mInventoryRoundIntervalCommand;
    private GeneralCommandItem mGetFwVersion;

    private GeneralCommandItem mBleDeviceNameCommand;
    private GeneralCommandItem mBuzzerOperationCommand;
    private GeneralCommandItem mControlBuzzerCommand;
    private GeneralCommandItem mOutputInterfacesCommand;
    private GeneralCommandItem mEventTypeCommand;
    private GeneralCommandItem mFilterCommand;
    private GeneralCommandItem mPostDataDelimiterCommand;
    private GeneralCommandItem mMemoryBankSelectionCommand;
    private GeneralCommandItem mSsidPasswordCommand;
    private GeneralCommandItem mSsidPasswordIpCommand;

    public static TS100DeviceControlFragment newFragment(String devMacAddress) {
        Bundle args = new Bundle();
        args.putString(MAC_ADDRESS, devMacAddress);
        TS100DeviceControlFragment fragment = new TS100DeviceControlFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void initUHFCallback() {
        mUHFCallback = new UHFCallback() {
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
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mRfPowerCommand.getPosition()));
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
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mRfSensitivityCommand.getPosition()));
                onUpdateLog(TAG, "didGetRfSensitivity: " + sensitivity.name());
            }

            @Override
            public void didGetFrequencyList(final Set<Double> frequencyList) {
                String frequencyData = Arrays.toString(frequencyList.toArray());
                EditTextParamData selected = (EditTextParamData) mFrequencyCommand.getViewDataArray()[0];
                selected.setSelected(frequencyData.replace("[", "").replace("]", ""));
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mFrequencyCommand.getPosition()));
                onUpdateLog(TAG, "didGetFrequencyList:\n" + frequencyData);
            }

            @Override
            public void didGetSessionAndTarget(final Session session, final Target target) {
                TwoSpinnerParamData viewData = (TwoSpinnerParamData) mSessionTargetCommand.getViewDataArray()[0];
                viewData.setFirstSelected(session);
                viewData.setSecondSelected(target);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mSessionTargetCommand.getPosition()));
                onUpdateLog(TAG, "didGetSessionAndTarget:" +
                        "\n\tSession: " + session.name() +
                        "\n\tTarget: " + target.name());
            }

            @Override
            public void didGetQValue(final byte qValue) {
                SeekBarParamData viewData = (SeekBarParamData) mQCommand.getViewDataArray()[0];
                viewData.setSelected(qValue);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mQCommand.getPosition()));
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
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mBleDeviceNameCommand.getPosition()));
                onUpdateLog(TAG, "didGetBleDeviceName: " + bleDeviceName);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> getActivity().setTitle(mUhf.getDeviceName()));
                }
            }

            @Override
            public void didReadTag(MemoryBank memoryBank, int startWordAddress, byte[] readData) {
                final String dataString = GTool.bytesToHexString(readData);
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
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mEventTypeCommand.getPosition()));
                onUpdateLog(TAG, "didGetEventType: " + eventType.name());
            }

            @Override
            public void didGetOutputInterfaces(KeyboardSimulation keyboardSimulation
                    , final Set<OutputInterface> outputInterfaces) {
                SpinnerParamData selected1 = (SpinnerParamData) mOutputInterfacesCommand.getViewDataArray()[0];
                selected1.setSelected(keyboardSimulation);
                CheckboxListParamData selected2 = (CheckboxListParamData) mOutputInterfacesCommand.getViewDataArray()[1];
                selected2.setSelected(outputInterfaces);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mOutputInterfacesCommand.getPosition()));
                onUpdateLog(TAG, "didGetOutputInterface: " +
                        "\n\tKeyboardSimulation: " + keyboardSimulation +
                        "\n\tOutputInterface: " + Arrays.toString(outputInterfaces.toArray()));
            }

            @Override
            public void didGetBuzzerOperationMode(final BuzzerOperationMode buzzerOperationMode) {
                SpinnerParamData selected1 = (SpinnerParamData) mBuzzerOperationCommand.getViewDataArray()[0];
                selected1.setSelected(buzzerOperationMode);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mBuzzerOperationCommand.getPosition()));
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
                EditTextTitleParamData secondParam = (EditTextTitleParamData) mReadWriteEpcCommand.getViewDataArray()[1];
                secondParam.setSelected(GTool.bytesToHexString(epc));
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mReadWriteEpcCommand.getPosition()));
                onUpdateLog(TAG, "didReadEpc: " + GTool.bytesToHexString(epc));
            }

            @Override
            public void didGetPostDataDelimiter(PostDataDelimiter postDataDelimiter) {
                SpinnerParamData selected1 = (SpinnerParamData) mPostDataDelimiterCommand.getViewDataArray()[0];
                selected1.setSelected(postDataDelimiter);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mPostDataDelimiterCommand.getPosition()));
                onUpdateLog(TAG, "didGetPostDataDelimiter: " + postDataDelimiter);
            }

            @Override
            public void didGetMemoryBankSelection(MemoryBankSelection memoryBankSelection) {
                SpinnerParamData selected1 = (SpinnerParamData) mMemoryBankSelectionCommand.getViewDataArray()[0];
                selected1.setSelected(memoryBankSelection);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mMemoryBankSelectionCommand.getPosition()));
                onUpdateLog(TAG, "didGetMemoryBankSelection: " + memoryBankSelection);
            }

            @Override
            public void didGetFilter(Set<TagDataEncodeType> tagDataEncodeTypes) {
                CheckboxListParamData selected1 = (CheckboxListParamData) mFilterCommand.getViewDataArray()[0];
                selected1.setSelected(tagDataEncodeTypes);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mFilterCommand.getPosition()));
                onUpdateLog(TAG, "didGetFilter: " + Arrays.toString(tagDataEncodeTypes.toArray()));
            }

            @Override
            public void didGetTagPresentedRepeatInterval(int hundredMilliSeconds) {
                SeekBarParamData selected = (SeekBarParamData) mTagPresentedRepeatIntervalCommand.getViewDataArray()[0];
                selected.setSelected(hundredMilliSeconds);
                mAdapter.notifyItemChanged(mTagPresentedRepeatIntervalCommand.getPosition());
                if (hundredMilliSeconds == 254) {
                    onUpdateLog(TAG, "didGetTagPresentedRepeatInterval: Never");
                } else if (hundredMilliSeconds == 0) {
                    onUpdateLog(TAG, "didGetTagPresentedRepeatInterval: Immediately");
                } else {
                    onUpdateLog(TAG, "didGetTagPresentedRepeatInterval[:" + hundredMilliSeconds + "*100 ms");
                }
            }

            @Override
            public void didGetTagRemovedThreshold(int inventoryRound) {
                SeekBarParamData selected = (SeekBarParamData) mTagRemovedThresholdCommand.getViewDataArray()[0];
                selected.setSelected(inventoryRound);
                mAdapter.notifyItemChanged(mTagRemovedThresholdCommand.getPosition());
                if (inventoryRound == 0) {
                    onUpdateLog(TAG, "didGetTagRemovedThreshold: Immediately");
                } else {
                    onUpdateLog(TAG, "didGetTagRemovedThreshold: " + inventoryRound + " inventory rounds.");
                }
            }

            @Override
            public void didGetInventoryRoundInterval(int tenMilliSeconds) {
                SeekBarParamData selected = (SeekBarParamData) mInventoryRoundIntervalCommand.getViewDataArray()[0];
                selected.setSelected(tenMilliSeconds);
                mAdapter.notifyItemChanged(mInventoryRoundIntervalCommand.getPosition());
                if (tenMilliSeconds == 0) {
                    onUpdateLog(TAG, "didGetInventoryRoundInterval: Immediately");
                } else {
                    onUpdateLog(TAG, "didGetInventoryRoundInterval: " + tenMilliSeconds + "*10 ms");
                }
            }

            @Override
            public void didGetInventoryActiveMode(ActiveMode activeMode) {
                SpinnerParamData selected = (SpinnerParamData) mInventoryActiveMode.getViewDataArray()[0];
                selected.setSelected(activeMode);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mInventoryActiveMode.getPosition()));
                onUpdateLog(TAG, "didGetInventoryActiveMode: " + activeMode.name());
            }
        };

    }

    @Override
    protected void onNewInventoryCommands() {
        newStopInventoryCommand();
        newStartInventoryCommand();
        newStartInventoryCommandEx();
        newInventoryActiveModeCommand();
    }

    @Override
    protected void onNewAdvanceCommands() {
        newBleDeviceNameCommand();
        newBuzzerOperationCommand();
        newControlBuzzerCommand();
        newOutputInterfacesCommand();
        newEventTypeCommand();
        newEnableFilterCommand();
        newPostDataDelimiterCommand();
        newMemoryBankSelectionCommand();
        newSsidPasswordCommand();
        newSsidPasswordIpCommand();
    }

    @Override
    protected void onNewSettingCommands() {
        newRfPowerCommand();
        newRfSensitivityCommand();
        newSessionTargetCommand();
        newQCommand();
        newFrequencyCommand();
        newTagRemovedThresholdCommand();
        newTagPresentedEventThresholdCommand();
        newInventoryRoundIntervalCommand();
        newGetFirmwareVersion();
    }

    @Override
    protected void onShowInventoryViews() {
        mAdapter.add(mStopInventoryCommand);
        mAdapter.add(mInventoryCommand);
        mAdapter.add(mInventoryExCommand);
        mAdapter.add(mInventoryActiveMode);
    }

    @Override
    protected void onShowAdvanceViews() {
        if (mUhf.getCommunicationType().equals(CommunicationType.BLE)) {
            mAdapter.add(mBleDeviceNameCommand);
        }
        mAdapter.add(mBuzzerOperationCommand);
        mAdapter.add(mControlBuzzerCommand);
        mAdapter.add(mOutputInterfacesCommand);
        mAdapter.add(mEventTypeCommand);
        mAdapter.add(mFilterCommand);
        mAdapter.add(mPostDataDelimiterCommand);
        mAdapter.add(mMemoryBankSelectionCommand);
        if (!mUhf.getCommunicationType().equals(CommunicationType.TCP)) {
            mAdapter.add(mSsidPasswordCommand);
            mAdapter.add(mSsidPasswordIpCommand);
        }
    }

    @Override
    protected void onShowSettingViews() {
        mAdapter.add(mRfPowerCommand);
        mAdapter.add(mRfSensitivityCommand);
        mAdapter.add(mSessionTargetCommand);
        mAdapter.add(mQCommand);
        mAdapter.add(mFrequencyCommand);
        mAdapter.add(mTagPresentedRepeatIntervalCommand);
        mAdapter.add(mTagRemovedThresholdCommand);
        mAdapter.add(mInventoryRoundIntervalCommand);
        mAdapter.add(mGetFwVersion);
    }

    protected void newStartInventoryCommandEx() {
        mInventoryExCommand = new GeneralCommandItem("Start Inventory Ex", null, "Start Ex", new CheckboxListParamData<>(TagDataEncodeType.class));
        mInventoryExCommand.setRightOnClickListener(v -> {
            CheckboxListParamData viewData = (CheckboxListParamData) mInventoryExCommand.getViewDataArray()[0];
            ((TS100) mUhf).startInventoryEx(viewData.getSelected());
        });
    }

    protected void newStartInventoryCommand() {
        mInventoryCommand = new GeneralCommandItem("Start Inventory", null, "Start", new SpinnerParamData<>(TagPresentedType.class));
        mInventoryCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mInventoryCommand.getViewDataArray()[0];
            mUhf.startInventory((TagPresentedType) viewData.getSelected());
        });
    }

    protected void newStopInventoryCommand() {
        mStopInventoryCommand = new GeneralCommandItem("Stop Inventory", null, "Stop");
        mStopInventoryCommand.setRightOnClickListener(v -> mUhf.stopInventory());
    }

    private void newInventoryActiveModeCommand() {
        mInventoryActiveMode = new GeneralCommandItem("Inventory Active Mode", "Get", "Set", new SpinnerParamData<>(ActiveMode.class));
        mInventoryActiveMode.setLeftOnClickListener(v -> mUhf.getInventoryActiveMode());
        mInventoryActiveMode.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mInventoryActiveMode.getViewDataArray()[0];
            mUhf.setInventoryActiveMode((ActiveMode) viewData.getSelected());
        });
    }

    private void newBleDeviceNameCommand() {
        mBleDeviceNameCommand = new GeneralCommandItem("Get/Set BLE Device Name", new EditTextParamData("BLE Device Name"));
        mBleDeviceNameCommand.setRightOnClickListener(v -> {
            String deviceName = ((EditTextParamData) mBleDeviceNameCommand.getViewDataArray()[0]).getSelected();
            ((TS100) mUhf).setBleDeviceName(deviceName);
        });
        mBleDeviceNameCommand.setLeftOnClickListener(v -> ((TS100) mUhf).getBleDeviceName());
    }


    private void newBuzzerOperationCommand() {
        mBuzzerOperationCommand = new GeneralCommandItem("Get/Set BuzzerAdapter Operation", new SpinnerParamData<>(BuzzerOperationMode.class));
        mBuzzerOperationCommand.setLeftOnClickListener(v -> {
            ((TS100) mUhf).getBuzzerOperationMode(mTemp);
        });
        mBuzzerOperationCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mBuzzerOperationCommand.getViewDataArray()[0];
            ((TS100) mUhf).setBuzzerOperationMode(mTemp, (BuzzerOperationMode) viewData.getSelected());
        });
    }

    private void newControlBuzzerCommand() {
        mControlBuzzerCommand = new GeneralCommandItem("Control BuzzerAdapter", null, "Control"
                , new SpinnerParamData<>(BuzzerAction.class));
        mControlBuzzerCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mControlBuzzerCommand.getViewDataArray()[0];
            ((TS100) mUhf).controlBuzzer((BuzzerAction) viewData.getSelected());
        });
    }

    private void newOutputInterfacesCommand() {
        Set<OutputInterface> outputInterfaces = new HashSet<>();
        outputInterfaces.add(OutputInterface.HID_N_VCOM);
        outputInterfaces.add(OutputInterface.TCP_CLIENT);
        outputInterfaces.add(TCP_SERVER);
        outputInterfaces.add(OutputInterface.BLE);
        mOutputInterfacesCommand = new GeneralCommandItem("Get/Set Output Interfaces"
                , new SpinnerParamData<>(KeyboardSimulation.class)
                , new CheckboxListParamData<>(outputInterfaces));
        mOutputInterfacesCommand.setLeftOnClickListener(v -> {
            ((TS100) mUhf).getOutputInterfaces(mTemp);
        });
        mOutputInterfacesCommand.setRightOnClickListener(v -> {
            SpinnerParamData keyboard = (SpinnerParamData) mOutputInterfacesCommand.getViewDataArray()[0];
            CheckboxListParamData outputInterface = (CheckboxListParamData) mOutputInterfacesCommand.getViewDataArray()[1];
            ((TS100) mUhf).setOutputInterfaces(mTemp, (KeyboardSimulation) keyboard.getSelected(), outputInterface.getSelected());
        });
    }

    private void newEventTypeCommand() {
        mEventTypeCommand = new GeneralCommandItem("Get/Set Event Type", new SpinnerParamData<>(EventType.class));
        mEventTypeCommand.setLeftOnClickListener(v -> {
            ((TS100) mUhf).getEventType(mTemp);
        });
        mEventTypeCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mEventTypeCommand.getViewDataArray()[0];
            ((TS100) mUhf).setEventType(mTemp, (EventType) viewData.getSelected());
        });
    }

    private void newEnableFilterCommand() {
        mFilterCommand = new GeneralCommandItem("Get/Set Filter",
                new CheckboxListParamData<>(TagDataEncodeType.class));
        mFilterCommand.setRightOnClickListener(v -> {
            CheckboxListParamData viewData = (CheckboxListParamData) mFilterCommand.getViewDataArray()[0];
            ((TS100) mUhf).setFilter(mTemp, viewData.getSelected());
        });
        mFilterCommand.setLeftOnClickListener(v -> {
            ((TS100) mUhf).getFilter(mTemp);
        });
    }

    private void newPostDataDelimiterCommand() {
        mPostDataDelimiterCommand = new GeneralCommandItem("Get/Set Post Data Delimiter", new SpinnerParamData<>(PostDataDelimiter.class));
        mPostDataDelimiterCommand.setLeftOnClickListener(v -> {
            ((TS100) mUhf).getPostDataDelimiter(mTemp);
        });
        mPostDataDelimiterCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mPostDataDelimiterCommand.getViewDataArray()[0];
            ((TS100) mUhf).setPostDataDelimiter(mTemp, (PostDataDelimiter) viewData.getSelected());
        });
    }

    private void newMemoryBankSelectionCommand() {
        mMemoryBankSelectionCommand = new GeneralCommandItem("Get/Set Memory Bank Selection", new SpinnerParamData<>(MemoryBankSelection.class));
        mMemoryBankSelectionCommand.setLeftOnClickListener(v -> {
            ((TS100) mUhf).getMemoryBankSelection(mTemp);
        });

        mMemoryBankSelectionCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mMemoryBankSelectionCommand.getViewDataArray()[0];
            ((TS100) mUhf).setMemoryBankSelection(mTemp, (MemoryBankSelection) viewData.getSelected());
        });
    }

    private void newInventoryRoundIntervalCommand() {
        mInventoryRoundIntervalCommand = new GeneralCommandItem("Get/Set Inventory Round Interval"
                , new SeekBarParamData(0, 254));
        mInventoryRoundIntervalCommand.setLeftOnClickListener(v -> mUhf.getInventoryRoundInterval(mTemp));
        mInventoryRoundIntervalCommand.setRightOnClickListener(v -> {
            SeekBarParamData viewData = (SeekBarParamData) mInventoryRoundIntervalCommand.getViewDataArray()[0];
            GLog.d(TAG, "InventoryRoundInterval: " + viewData.getSelected());
            mUhf.setInventoryRoundInterval(mTemp, viewData.getSelected());
        });
    }

    private void newTagPresentedEventThresholdCommand() {
        mTagPresentedRepeatIntervalCommand = new GeneralCommandItem("Get/Set Tag Presented Repeat Interval"
                , new SeekBarParamData(0, 254));
        mTagPresentedRepeatIntervalCommand.setLeftOnClickListener(v -> mUhf.getTagPresentedRepeatInterval(mTemp));
        mTagPresentedRepeatIntervalCommand.setRightOnClickListener(v -> {
            SeekBarParamData viewData = (SeekBarParamData) mTagPresentedRepeatIntervalCommand.getViewDataArray()[0];
            GLog.d(TAG, "Repeat: " + viewData.getSelected());
            mUhf.setTagPresentedRepeatInterval(mTemp, viewData.getSelected());
        });
    }

    private void newTagRemovedThresholdCommand() {
        mTagRemovedThresholdCommand = new GeneralCommandItem("Get/Set Tag Removed Threshold"
                , new SeekBarParamData(0, 254));
        mTagRemovedThresholdCommand.setLeftOnClickListener(v -> mUhf.getTagRemovedThreshold(mTemp));
        mTagRemovedThresholdCommand.setRightOnClickListener(v -> {
            SeekBarParamData viewData = (SeekBarParamData) mTagRemovedThresholdCommand.getViewDataArray()[0];
            GLog.d(TAG, "Removed: " + viewData.getSelected());
            mUhf.setTagRemovedThreshold(mTemp, viewData.getSelected());
        });
    }


    private void newFrequencyCommand() {
        mFrequencyCommand = new GeneralCommandItem("Get/Set Frequency"
                , new EditTextParamData("840.250, 842.000, 843.250"));
        mFrequencyCommand.setLeftOnClickListener(v -> mUhf.getFrequency(mTemp));
        mFrequencyCommand.setRightOnClickListener(v -> {
            EditTextParamData viewData = (EditTextParamData) mFrequencyCommand.getViewDataArray()[0];
            String selected = viewData.getSelected();
            String[] frequencyArray = selected.trim().split(",");
            if (frequencyArray.length > 1) {
                try {
                    LinkedHashSet<Double> frequencyList = new LinkedHashSet<>();
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
            mUhf.setQValue(mTemp, (byte) viewData.getSelected());
        });
    }

    private void newSessionTargetCommand() {
        mSessionTargetCommand = new GeneralCommandItem("Get/Set Session and Target"
                , new TwoSpinnerParamData<>(Session.values(), Target.getAbTargets()));
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
        mRfSensitivityCommand = new GeneralCommandItem("Get/Set RF Sensitivity"
                , new SeekBarParamData(1, 14));
        mRfSensitivityCommand.setLeftOnClickListener(v -> mUhf.getRfSensitivity(mTemp));
        mRfSensitivityCommand.setRightOnClickListener(v -> {
            SeekBarParamData viewData = (SeekBarParamData) mRfSensitivityCommand.getViewDataArray()[0];
            mUhf.setRfSensitivity(mTemp, RfSensitivityLevel.getSensitivityFrom(viewData.getSelected()));
        });
    }

    private void newRfPowerCommand() {
        mRfPowerCommand = new GeneralCommandItem("Get/Set RF Power"
                , new SeekBarParamData(1, 27));
        mRfPowerCommand.setLeftOnClickListener(v -> mUhf.getRfPower(mTemp));
        mRfPowerCommand.setRightOnClickListener(v -> {
            SeekBarParamData viewData = (SeekBarParamData) mRfPowerCommand.getViewDataArray()[0];
            mUhf.setRfPower(mTemp, (byte) viewData.getSelected());
        });
    }

    private void newGetFirmwareVersion() {
        mGetFwVersion = new GeneralCommandItem("Get Firmware Version", null, "Get");
        mGetFwVersion.setRightOnClickListener(v -> mUhf.getFirmwareVersion());
    }

    private void newSsidPasswordCommand() {
        mSsidPasswordCommand = new GeneralCommandItem("Set WiFi Settings", null, "Set"
                , new EditTextTitleParamData("SSID", "SSID of station mode")
                , new EditTextTitleParamData("Password", "Password of station mode"));
        mSsidPasswordCommand.setRightOnClickListener(v -> {
            EditTextTitleParamData ssid = (EditTextTitleParamData) mSsidPasswordCommand.getViewDataArray()[0];
            EditTextTitleParamData password = (EditTextTitleParamData) mSsidPasswordCommand.getViewDataArray()[1];
            ((TS100) mUhf).setWifiSettings(ssid.getSelected(), password.getSelected());
        });
    }

    private void newSsidPasswordIpCommand() {
        mSsidPasswordIpCommand = new GeneralCommandItem("Set WiFi Settings", null, "Set"
                , new EditTextTitleParamData("SSID", "SSID of station mode")
                , new EditTextTitleParamData("Password", "Password of station mode")
                , new EditTextTitleParamData("IP", "IP address")
                , new EditTextTitleParamData("Gateway", "Gateway")
                , new EditTextTitleParamData("Subnet mask", "Subnet mask")
        );
        mSsidPasswordIpCommand.setRightOnClickListener(v -> {
            EditTextTitleParamData ssid = (EditTextTitleParamData) mSsidPasswordIpCommand.getViewDataArray()[0];
            EditTextTitleParamData password = (EditTextTitleParamData) mSsidPasswordIpCommand.getViewDataArray()[1];
            EditTextTitleParamData ip = (EditTextTitleParamData) mSsidPasswordIpCommand.getViewDataArray()[2];
            EditTextTitleParamData gateway = (EditTextTitleParamData) mSsidPasswordIpCommand.getViewDataArray()[3];
            EditTextTitleParamData subnetMask = (EditTextTitleParamData) mSsidPasswordIpCommand.getViewDataArray()[4];
            ((TS100) mUhf).setWifiSettings(ssid.getSelected(), password.getSelected(), ip.getSelected(), gateway.getSelected(), subnetMask.getSelected());
        });
    }
}
