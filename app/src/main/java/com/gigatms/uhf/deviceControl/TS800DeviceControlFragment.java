package com.gigatms.uhf.deviceControl;

import android.os.Bundle;

import com.gigatms.CommunicationType;
import com.gigatms.DecodedTagData;
import com.gigatms.TS800;
import com.gigatms.TagInformationFormat;
import com.gigatms.UHFCallback;
import com.gigatms.uhf.DeviceControlFragment;
import com.gigatms.uhf.GeneralCommandItem;
import com.gigatms.uhf.paramsData.CheckboxListParamData;
import com.gigatms.uhf.paramsData.EditTextParamData;
import com.gigatms.uhf.paramsData.EditTextTitleParamData;
import com.gigatms.uhf.paramsData.EventTypesParamData;
import com.gigatms.uhf.paramsData.SeekBarParamData;
import com.gigatms.uhf.paramsData.SpinnerParamData;
import com.gigatms.uhf.paramsData.TwoSpinnerParamData;
import com.gigatms.parameters.ActiveMode;
import com.gigatms.parameters.BuzzerAction;
import com.gigatms.parameters.BuzzerOperationMode;
import com.gigatms.parameters.IONumber;
import com.gigatms.parameters.IOState;
import com.gigatms.parameters.MemoryBank;
import com.gigatms.parameters.OutputInterface;
import com.gigatms.parameters.RfSensitivityLevel;
import com.gigatms.parameters.ScanMode;
import com.gigatms.parameters.Session;
import com.gigatms.parameters.State;
import com.gigatms.parameters.TagPresentedType;
import com.gigatms.parameters.Target;
import com.gigatms.parameters.TriggerType;
import com.gigatms.parameters.event.BaseTagEvent;
import com.gigatms.parameters.event.TagPresentedEvent;
import com.gigatms.tools.GTool;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.gigatms.parameters.ActiveMode.COMMAND;
import static com.gigatms.parameters.ActiveMode.READ;
import static com.gigatms.parameters.OutputInterface.DEFAULT;
import static com.gigatms.parameters.OutputInterface.TCP_SERVER;
import static com.gigatms.parameters.Session.SL;

public class TS800DeviceControlFragment extends DeviceControlFragment {
    private final String[] SECOND_CHOICES = {"REMOVE EVENT", "TID BANK", "USER BANK"};
    private final String[] EVENT_TYPES = {"TAG_PRESENTED_EVENT"};

    private GeneralCommandItem mStopInventoryCommand;
    private GeneralCommandItem mInventoryCommand;
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

    private GeneralCommandItem mOutputInterfaceCommand;
    private GeneralCommandItem mBleDeviceNameCommand;
    private GeneralCommandItem mBuzzerOperationCommand;
    private GeneralCommandItem mControlBuzzerCommand;
    private GeneralCommandItem mIoStateCommand;
    private GeneralCommandItem mTriggerCommand;
    private GeneralCommandItem mScanModeCommand;
    private GeneralCommandItem mCommandTrigger;
    private GeneralCommandItem mEventTypeCommand;
    private GeneralCommandItem mSsidPasswordCommand;
    private GeneralCommandItem mSsidPasswordIpCommand;

    private static final String TAG = TS800DeviceControlFragment.class.getSimpleName();

    public static TS800DeviceControlFragment newFragment(String devMacAddress) {
        Bundle args = new Bundle();
        args.putString(MAC_ADDRESS, devMacAddress);
        TS800DeviceControlFragment fragment = new TS800DeviceControlFragment();
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
            public void didGetTriggerType(final Set<TriggerType> triggerSource) {
                CheckboxListParamData selected1 = (CheckboxListParamData) mTriggerCommand.getViewDataArray()[0];
                selected1.setSelected(triggerSource);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mTriggerCommand.getPosition()));
                onUpdateLog(TAG, "didGetTriggerType: " + Arrays.toString(triggerSource.toArray()));
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
                if (tagInformationFormat.getUserHex().length() != 0) {
                    message = message + "\n\tUser: " + tagInformationFormat.getUserHex();
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
            public void didGetTagPresentedRepeatInterval(int hundredMilliSeconds) {
                SeekBarParamData selected = (SeekBarParamData) mTagPresentedRepeatIntervalCommand.getViewDataArray()[0];
                selected.setSelected(hundredMilliSeconds);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mTagPresentedRepeatIntervalCommand.getPosition()));
                if (hundredMilliSeconds == 254) {
                    onUpdateLog(TAG, "didGetTagPresentedRepeatInterval: Never");
                } else if (hundredMilliSeconds == 0) {
                    onUpdateLog(TAG, "didGetTagPresentedRepeatInterval: Immediately");
                } else {
                    onUpdateLog(TAG, "didGetTagPresentedRepeatInterval:" + hundredMilliSeconds + "*100 ms");
                }
            }

            @Override
            public void didGetTagRemovedThreshold(int inventoryRound) {
                SeekBarParamData selected = (SeekBarParamData) mTagRemovedThresholdCommand.getViewDataArray()[0];
                selected.setSelected(inventoryRound);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mTagRemovedThresholdCommand.getPosition()));
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
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mInventoryRoundIntervalCommand.getPosition()));
                if (tenMilliSeconds == 0) {
                    onUpdateLog(TAG, "didGetInventoryRoundInterval: Immediately");
                } else {
                    onUpdateLog(TAG, "didGetInventoryRoundInterval: " + tenMilliSeconds + "*10 ms");
                }
            }

            @Override
            public void didGetOutputInterface(OutputInterface outputInterface) {
                SpinnerParamData selected1 = (SpinnerParamData) mOutputInterfaceCommand.getViewDataArray()[0];
                selected1.setSelected(outputInterface);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mOutputInterfaceCommand.getPosition()));
                onUpdateLog(TAG, "didGetOutputInterface: " + outputInterface.name());
            }

            @Override
            public void didGetScanMode(ScanMode scanMode) {
                SpinnerParamData selected1 = (SpinnerParamData) mScanModeCommand.getViewDataArray()[0];
                selected1.setSelected(scanMode);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mScanModeCommand.getPosition()));
                onUpdateLog(TAG, "didGetScanMode: " + scanMode.name());
            }

            @Override
            public void didGetCommandTriggerState(State state) {
                SpinnerParamData selected = (SpinnerParamData) mCommandTrigger.getViewDataArray()[0];
                selected.setSelected(state);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mCommandTrigger.getPosition()));
                onUpdateLog(TAG, "didGetCommandTriggerState: " + state.name());
            }

            @Override
            public void didGetInventoryActiveMode(ActiveMode activeMode) {
                SpinnerParamData seleced = (SpinnerParamData) mInventoryActiveMode.getViewDataArray()[0];
                seleced.setSelected(activeMode);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mInventoryActiveMode.getPosition()));
                onUpdateLog(TAG, "didGetInventoryActiveMode: " + activeMode.name());
            }

            @Override
            public void didGetEventType(final BaseTagEvent baseTagEvent) {
                EventTypesParamData eventType = (EventTypesParamData) mEventTypeCommand.getViewDataArray()[0];
                StringBuilder stringBuilder = new StringBuilder();
                if (baseTagEvent instanceof TagPresentedEvent) {
                    stringBuilder.append(EVENT_TYPES[0]);
                    eventType.setFirstSelect(EVENT_TYPES[0]);
                    TagPresentedEvent event = (TagPresentedEvent) baseTagEvent;
                    Set<String> secondSelected = new HashSet<>();
                    if (event.hasRemoveEvent()) {
                        secondSelected.add(SECOND_CHOICES[0]);
                        stringBuilder.append("\n\t").append(SECOND_CHOICES[0]);
                    }
                    if (event.hasTidBank()) {
                        secondSelected.add(SECOND_CHOICES[1]);
                        stringBuilder.append("\n\t").append(SECOND_CHOICES[1]);
                    }
                    if (event.hasUserBank()) {
                        secondSelected.add(SECOND_CHOICES[2]);
                        stringBuilder.append("\n\t").append(SECOND_CHOICES[2]);
                    }
                    eventType.setLastSelect(secondSelected);
                }

                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mEventTypeCommand.getPosition()));
                onUpdateLog(TAG, "didGetEventType: " + stringBuilder.toString());
            }
        };

    }

    @Override
    protected void onNewInventoryCommands() {
        newStopInventoryCommand();
        newStartInventoryCommand();
        newInventoryActiveModeCommand();
    }

    @Override
    protected void onNewAdvanceCommands() {
        newBleDeviceNameCommand();
        newBuzzerOperationCommand();
        newControlBuzzerCommand();
        newTS800IoStateCommand();
        newOutputInterfaceCommand();
        newTS800TriggerCommand();
        newScanModeCommand();
        newCommandTrigger();
        newEventTypeCommand();
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
        mAdapter.add(mInventoryActiveMode);
    }

    @Override
    protected void onShowAdvanceViews() {
        if (mUhf.getCommunicationType().equals(CommunicationType.BLE)) {
            mAdapter.add(mBleDeviceNameCommand);
        }
        mAdapter.add(mScanModeCommand);
        mAdapter.add(mTriggerCommand);
        mAdapter.add(mCommandTrigger);
        mAdapter.add(mEventTypeCommand);
        mAdapter.add(mBuzzerOperationCommand);
        mAdapter.add(mControlBuzzerCommand);
        mAdapter.add(mIoStateCommand);
        mAdapter.add(mOutputInterfaceCommand);
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

    private void newStopInventoryCommand() {
        mStopInventoryCommand = new GeneralCommandItem("Stop Inventory", null, "Stop");
        mStopInventoryCommand.setRightOnClickListener(v -> mUhf.stopInventory());
    }

    private void newStartInventoryCommand() {
        mInventoryCommand = new GeneralCommandItem("Start Inventory", null, "Start", new SpinnerParamData<>(TagPresentedType.class));
        mInventoryCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mInventoryCommand.getViewDataArray()[0];
            mUhf.startInventory((TagPresentedType) viewData.getSelected());
        });
    }

    private void newInventoryActiveModeCommand() {
        mInventoryActiveMode = new GeneralCommandItem("Inventory Active Mode", "Get", "Set", new SpinnerParamData<>(new ActiveMode[]{READ, COMMAND}));
        mInventoryActiveMode.setLeftOnClickListener(v -> ((TS800) mUhf).getInventoryActiveMode(true));
        mInventoryActiveMode.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mInventoryActiveMode.getViewDataArray()[0];
            ((TS800) mUhf).setInventoryActiveMode(true, (ActiveMode) viewData.getSelected());
        });
    }


    private void newBleDeviceNameCommand() {
        mBleDeviceNameCommand = new GeneralCommandItem("Get/Set BLE Device Name"
                , new EditTextParamData("BLE Device Name"));
        mBleDeviceNameCommand.setRightOnClickListener(v -> {
            String deviceName = ((EditTextParamData) mBleDeviceNameCommand.getViewDataArray()[0]).getSelected();
            ((TS800) mUhf).setBleDeviceName(deviceName);
        });
        mBleDeviceNameCommand.setLeftOnClickListener(v -> {
            ((TS800) mUhf).getBleDeviceName();
        });
    }

    private void newBuzzerOperationCommand() {
        mBuzzerOperationCommand = new GeneralCommandItem("Get/Set BuzzerAdapter Operation"
                , new SpinnerParamData<>(new BuzzerOperationMode[]{BuzzerOperationMode.OFF, BuzzerOperationMode.REPEAT}));
        mBuzzerOperationCommand.setLeftOnClickListener(v -> ((TS800) mUhf).getBuzzerOperationMode(mTemp));
        mBuzzerOperationCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mBuzzerOperationCommand.getViewDataArray()[0];
            ((TS800) mUhf).setBuzzerOperationMode(mTemp, (BuzzerOperationMode) viewData.getSelected());
        });
    }

    private void newControlBuzzerCommand() {
        mControlBuzzerCommand = new GeneralCommandItem("Control BuzzerAdapter", null, "Control"
                , new SpinnerParamData<>(BuzzerAction.class));
        mControlBuzzerCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mControlBuzzerCommand.getViewDataArray()[0];
            ((TS800) mUhf).controlBuzzer((BuzzerAction) viewData.getSelected());
        });
    }

    private void newTS800TriggerCommand() {
        mTriggerCommand = new GeneralCommandItem("Get/Set Trigger"
                , new CheckboxListParamData<>(TriggerType.class));
        mTriggerCommand.setLeftOnClickListener(v -> ((TS800) mUhf).getTriggerType(mTemp));
        mTriggerCommand.setRightOnClickListener(v -> {
            CheckboxListParamData viewData = (CheckboxListParamData) mTriggerCommand.getViewDataArray()[0];
            ((TS800) mUhf).setTriggerType(mTemp, viewData.getSelected());
        });
    }

    private void newTS800IoStateCommand() {
        IONumber[] ioNumbers = new IONumber[IONumber.values().length - 1];
        int j = 0;
        for (int i = 0; i < IONumber.values().length; i++) {
            if (!IONumber.values()[i].equals(IONumber.INPUT_PIN_0)) {
                ioNumbers[j] = IONumber.values()[i];
                j++;
            }
        }
        mIoStateCommand = new GeneralCommandItem("Get/Set I/O State"
                , new TwoSpinnerParamData<>(ioNumbers, IOState.values()));
        mIoStateCommand.setLeftOnClickListener(v -> ((TS800) mUhf).getIOState());
        mIoStateCommand.setRightOnClickListener(v -> {
            TwoSpinnerParamData twoSpinnerParamData = (TwoSpinnerParamData) mIoStateCommand.getViewDataArray()[0];
            IONumber firstSelected = (IONumber) twoSpinnerParamData.getFirstSelected();
            IOState secondSelected = (IOState) twoSpinnerParamData.getSecondSelected();
            ((TS800) mUhf).setIOState(firstSelected, secondSelected);
        });
    }

    private void newOutputInterfaceCommand() {
        mOutputInterfaceCommand = new GeneralCommandItem("Get/Set Output Interface"
                , new SpinnerParamData<>(new OutputInterface[]{DEFAULT, TCP_SERVER}));
        mOutputInterfaceCommand.setLeftOnClickListener(v -> ((TS800) mUhf).getOutputInterface(mTemp));
        mOutputInterfaceCommand.setRightOnClickListener(v -> {
            SpinnerParamData outputInterface = (SpinnerParamData) mOutputInterfaceCommand.getViewDataArray()[0];
            ((TS800) mUhf).setOutputInterface(mTemp, (OutputInterface) outputInterface.getSelected());
        });
    }

    private void newInventoryRoundIntervalCommand() {
        mInventoryRoundIntervalCommand = new GeneralCommandItem("Get/Set Inventory Round Interval"
                , new SeekBarParamData(0, 254));
        mInventoryRoundIntervalCommand.setLeftOnClickListener(v -> mUhf.getInventoryRoundInterval(mTemp));
        mInventoryRoundIntervalCommand.setRightOnClickListener(v -> {
            SeekBarParamData viewData = (SeekBarParamData) mInventoryRoundIntervalCommand.getViewDataArray()[0];
            mUhf.setInventoryRoundInterval(mTemp, viewData.getSelected());
        });
    }

    private void newTagPresentedEventThresholdCommand() {
        mTagPresentedRepeatIntervalCommand = new GeneralCommandItem("Get/Set Tag Presented Repeat Interval"
                , new SeekBarParamData(0, 254));
        mTagPresentedRepeatIntervalCommand.setLeftOnClickListener(v -> ((TS800) mUhf).getTagPresentedRepeatInterval(mTemp));
        mTagPresentedRepeatIntervalCommand.setRightOnClickListener(v -> {
            SeekBarParamData viewData = (SeekBarParamData) mTagPresentedRepeatIntervalCommand.getViewDataArray()[0];
            ((TS800) mUhf).setTagPresentedRepeatInterval(mTemp, viewData.getSelected());
        });
    }

    private void newTagRemovedThresholdCommand() {
        mTagRemovedThresholdCommand = new GeneralCommandItem("Get/Set Tag Removed Threshold"
                , new SeekBarParamData(0, 254));
        mTagRemovedThresholdCommand.setLeftOnClickListener(v -> mUhf.getTagRemovedThreshold(mTemp));
        mTagRemovedThresholdCommand.setRightOnClickListener(v -> {
            SeekBarParamData viewData = (SeekBarParamData) mTagRemovedThresholdCommand.getViewDataArray()[0];
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

    private void newScanModeCommand() {
        mScanModeCommand = new GeneralCommandItem("Get/Set Scan Mode"
                , new SpinnerParamData<>(ScanMode.class));
        mScanModeCommand.setRightOnClickListener(v -> {
            SpinnerParamData scanMode = (SpinnerParamData) mScanModeCommand.getViewDataArray()[0];
            ((TS800) mUhf).setScanMode(mTemp, (ScanMode) scanMode.getSelected());
        });
        mScanModeCommand.setLeftOnClickListener(v -> ((TS800) mUhf).getScanMode(mTemp));
    }

    private void newCommandTrigger() {
        mCommandTrigger = new GeneralCommandItem("Get/Set Command Trigger", new SpinnerParamData<>(State.class));
        mCommandTrigger.setRightOnClickListener(v -> {
            SpinnerParamData state = (SpinnerParamData) mCommandTrigger.getViewDataArray()[0];
            ((TS800) mUhf).setCommandTriggerState((State) state.getSelected());
        });
        mCommandTrigger.setLeftOnClickListener(v -> ((TS800) mUhf).getCommandTriggerState());
    }

    private void newSsidPasswordCommand() {
        mSsidPasswordCommand = new GeneralCommandItem("Set WiFi Settings", null, "Set"
                , new EditTextTitleParamData("SSID", "SSID of station mode")
                , new EditTextTitleParamData("Password", "Password of station mode"));
        mSsidPasswordCommand.setRightOnClickListener(v -> {
            EditTextTitleParamData ssid = (EditTextTitleParamData) mSsidPasswordCommand.getViewDataArray()[0];
            EditTextTitleParamData password = (EditTextTitleParamData) mSsidPasswordCommand.getViewDataArray()[1];
            ((TS800) mUhf).setWifiSettings(ssid.getSelected(), password.getSelected());
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
            ((TS800) mUhf).setWifiSettings(ssid.getSelected(), password.getSelected(), ip.getSelected(), gateway.getSelected(), subnetMask.getSelected());
        });
    }

    private void newEventTypeCommand() {
        EventTypesParamData mEventTypeParamData = new EventTypesParamData(
                EVENT_TYPES, null
                , SECOND_CHOICES);
        mEventTypeCommand = new GeneralCommandItem("Get/Set Event Type"
                , mEventTypeParamData);
        mEventTypeCommand.setLeftOnClickListener(v -> ((TS800) mUhf).getEventType(mTemp));
        mEventTypeCommand.setRightOnClickListener(v -> {
            EventTypesParamData event = (EventTypesParamData) mEventTypeCommand.getViewDataArray()[0];
            String eventType = event.getFirstSelect();
            if (eventType.equals(EVENT_TYPES[0])) {
                TagPresentedEvent.Builder builder = new TagPresentedEvent.Builder();
                if (event.getLastSelect().contains(SECOND_CHOICES[0])) {
                    builder.setRemoveEvent(true);
                }
                if (event.getLastSelect().contains(SECOND_CHOICES[1])) {
                    builder.setTidBank(true);
                }
                if (event.getLastSelect().contains(SECOND_CHOICES[2])) {
                    builder.setUserBank(true);
                }
                ((TS800) mUhf).setEventType(mTemp, builder.build());
            }
        });
    }
}
