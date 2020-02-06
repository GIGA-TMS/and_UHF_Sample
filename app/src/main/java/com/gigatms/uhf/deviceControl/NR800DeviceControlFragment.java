package com.gigatms.uhf.deviceControl;

import android.os.Bundle;
import android.widget.Toast;

import com.gigatms.NR800;
import com.gigatms.UHFCallback;
import com.gigatms.uhf.DeviceControlFragment;
import com.gigatms.uhf.GeneralCommandItem;
import com.gigatms.uhf.Toaster;
import com.gigatms.uhf.paramsData.ASCIIEditTextParamData;
import com.gigatms.uhf.paramsData.EditTextParamData;
import com.gigatms.uhf.paramsData.EventTypesParamData;
import com.gigatms.uhf.paramsData.SeekBarParamData;
import com.gigatms.uhf.paramsData.SpinnerParamData;
import com.gigatms.uhf.paramsData.TwoSpinnerParamData;
import com.gigatms.parameters.ActiveMode;
import com.gigatms.parameters.BuzzerAction;
import com.gigatms.parameters.BuzzerOperationMode;
import com.gigatms.parameters.DecodedTagData;
import com.gigatms.parameters.IONumber;
import com.gigatms.parameters.IOState;
import com.gigatms.parameters.MemoryBank;
import com.gigatms.parameters.RfSensitivityLevel;
import com.gigatms.parameters.RxDecodeType;
import com.gigatms.parameters.ScanMode;
import com.gigatms.parameters.Session;
import com.gigatms.parameters.State;
import com.gigatms.parameters.TagInformationFormat;
import com.gigatms.parameters.TagPresentedType;
import com.gigatms.parameters.Target;
import com.gigatms.parameters.event.BaseTagEvent;
import com.gigatms.parameters.event.TagPresentedEvent;
import com.gigatms.parameters.event.TextTagEvent;
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
import static com.gigatms.parameters.Session.SL;
import static com.gigatms.parameters.event.TextTagEvent.BaseTextEventFormat.EPC;
import static com.gigatms.parameters.event.TextTagEvent.BaseTextEventFormat.NONE;
import static com.gigatms.parameters.event.TextTagEvent.BaseTextEventFormat.PC_EPC;
import static com.gigatms.parameters.event.TextTagEvent.BaseTextEventFormat.PC_EPC_WITH_REMOVE_EVENT;

public class NR800DeviceControlFragment extends DeviceControlFragment {
    private static final String TAG = NR800DeviceControlFragment.class.getSimpleName();
    private final String[] TAG_EVENT_FORMAT = {"REMOVE EVENT", "TID BANK"};
    private final String[] ADDITION_TEXT_TAG_EVENT_FORMAT_CHOICE = {"TID BANK"};
    private final String[] BASE_TEXT_TAG_EVENT_FORMAT_CHOICE = new String[]{NONE.name(), EPC.name(), PC_EPC.name(), PC_EPC_WITH_REMOVE_EVENT.name()};
    private final String[] EVENT_TYPES = {"TAG_PRESENTED_EVENT", "TEXT_TAG_EVENT"};

    private GeneralCommandItem mStopInventoryCommand;
    private GeneralCommandItem mStartInventoryCommand;
    private GeneralCommandItem mInventoryActiveMode;

    private GeneralCommandItem mRfPowerCommand;
    private GeneralCommandItem mRfSensitivityCommand;
    private GeneralCommandItem mRxDecodeTypeCommand;
    private GeneralCommandItem mSessionTargetCommand;
    private GeneralCommandItem mQCommand;
    private GeneralCommandItem mFrequencyCommand;
    private GeneralCommandItem mTagRemovedThresholdCommand;
    private GeneralCommandItem mTagPresentedRepeatIntervalCommand;
    private GeneralCommandItem mInventoryRoundIntervalCommand;
    private GeneralCommandItem mGetFwVersion;

    private GeneralCommandItem mPrefixCommand;
    private GeneralCommandItem mSuffixCommand;
    private GeneralCommandItem mTidDelimiterCommand;
    private GeneralCommandItem mScanModeCommand;
    private GeneralCommandItem mBuzzerOperationCommand;
    private GeneralCommandItem mControlBuzzerCommand;
    private GeneralCommandItem mTagEventIntervalCommand;
    private GeneralCommandItem mVibratorStateCommand;
    private GeneralCommandItem mEventTypeCommand;

    public static NR800DeviceControlFragment newFragment(String devMacAddress) {
        Bundle args = new Bundle();
        args.putString(MAC_ADDRESS, devMacAddress);
        NR800DeviceControlFragment fragment = new NR800DeviceControlFragment();
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
            public void didGetRxDecode(RxDecodeType rxDecodeType) {
                SpinnerParamData rxDecodeViewData = (SpinnerParamData) mRxDecodeTypeCommand.getViewDataArray()[0];
                rxDecodeViewData.setSelected(rxDecodeType);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mRxDecodeTypeCommand.getPosition()));
                onUpdateLog(TAG, "didGetRxDecode: " + rxDecodeType.name());
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
            public void didGetTagEventInterval(int tenMilliSeconds) {
                SeekBarParamData selected1 = (SeekBarParamData) mTagEventIntervalCommand.getViewDataArray()[0];
                selected1.setSelected(tenMilliSeconds);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mTagEventIntervalCommand.getPosition()));
                onUpdateLog(TAG, "didGetTagEventInterval: " + tenMilliSeconds + "*10 ms");
            }

            @Override
            public void didGetVibratorState(State state) {
                SpinnerParamData stateOn = (SpinnerParamData) mVibratorStateCommand.getViewDataArray()[0];
                stateOn.setSelected(state);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mVibratorStateCommand.getPosition()));
                onUpdateLog(TAG, "didGetVibratorState On: " + state);
            }

            @Override
            public void didGetPrefix(byte[] asciiPrefix) {
                ASCIIEditTextParamData prefixData = (ASCIIEditTextParamData) mPrefixCommand.getViewDataArray()[0];
                prefixData.setSelected(asciiPrefix);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mPrefixCommand.getPosition()));
                onUpdateLog(TAG, "didGetPrefix: " + GTool.bytesToHexString(asciiPrefix, " "));
            }

            @Override
            public void didGetSuffix(byte[] asciiSuffix) {
                ASCIIEditTextParamData suffixData = (ASCIIEditTextParamData) mSuffixCommand.getViewDataArray()[0];
                suffixData.setSelected(asciiSuffix);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mSuffixCommand.getPosition()));
                onUpdateLog(TAG, "didGetSuffix: " + GTool.bytesToHexString(asciiSuffix, " "));
            }

            @Override
            public void didGetTidDelimiter(Byte asciiValue) {
                ASCIIEditTextParamData tidDelimiter = (ASCIIEditTextParamData) mTidDelimiterCommand.getViewDataArray()[0];
                tidDelimiter.setSelected(asciiValue == (byte) 0xFF ? new byte[0] : new byte[]{asciiValue});
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mTidDelimiterCommand.getPosition()));
                onUpdateLog(TAG, "didGetTidDelimiter: " + GTool.bytesToHexString(new byte[]{asciiValue}));
            }

            @Override
            public void didGetScanMode(ScanMode scanMode) {
                SpinnerParamData selected1 = (SpinnerParamData) mScanModeCommand.getViewDataArray()[0];
                selected1.setSelected(scanMode);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mScanModeCommand.getPosition()));
                onUpdateLog(TAG, "didGetScanMode: " + scanMode.name());
            }

            @Override
            public void didGetInventoryActiveMode(ActiveMode activeMode) {
                SpinnerParamData selected = (SpinnerParamData) mInventoryActiveMode.getViewDataArray()[0];
                selected.setSelected(activeMode);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mInventoryActiveMode.getPosition()));
                onUpdateLog(TAG, "didGetInventoryActiveMode: " + activeMode.name());
            }

            @Override
            public void didGetEventType(BaseTagEvent baseTagEvent) {
                EventTypesParamData eventType = (EventTypesParamData) mEventTypeCommand.getViewDataArray()[0];
                StringBuilder stringBuilder = new StringBuilder();
                if (baseTagEvent instanceof TagPresentedEvent) {
                    stringBuilder.append(EVENT_TYPES[0]);
                    eventType.setFirstSelect(EVENT_TYPES[0]);
                    eventType.setMiddleChoices(null);
                    eventType.setLastChoices(TAG_EVENT_FORMAT);
                    TagPresentedEvent event = (TagPresentedEvent) baseTagEvent;
                    Set<String> tagEventFormat = new HashSet<>();
                    if (event.hasRemoveEvent()) {
                        tagEventFormat.add(TAG_EVENT_FORMAT[0]);
                        stringBuilder.append("\n\t").append(TAG_EVENT_FORMAT[0]);
                    }
                    if (event.hasTidBank()) {
                        tagEventFormat.add(TAG_EVENT_FORMAT[1]);
                        stringBuilder.append("\n\t").append(TAG_EVENT_FORMAT[1]);
                    }
                    eventType.setLastSelect(tagEventFormat);
                } else if (baseTagEvent instanceof TextTagEvent) {
                    TextTagEvent event = (TextTagEvent) baseTagEvent;
                    stringBuilder.append(EVENT_TYPES[1]);
                    eventType.setFirstSelect(EVENT_TYPES[1]);

                    eventType.setMiddleChoices(BASE_TEXT_TAG_EVENT_FORMAT_CHOICE);
                    eventType.setMiddleSelect(event.getBaseTextEventFormat().name());

                    eventType.setLastChoices(ADDITION_TEXT_TAG_EVENT_FORMAT_CHOICE);
                    Set<String> textTagEventFormat = new HashSet<>();
                    if (event.hasTidBank()) {
                        textTagEventFormat.add(ADDITION_TEXT_TAG_EVENT_FORMAT_CHOICE[0]);
                    }
                    eventType.setLastSelect(textTagEventFormat);
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
        newPrefixCommand();
        newSuffixCommand();
        newTidDelimiterCommand();
        newScanModeCommand();
        newBuzzerOperationCommand();
        newControlBuzzerCommand();
        newVibratorStateCommand();
        newTagEventIntervalCommand();
        newEventTypeCommand();
    }

    @Override
    protected void onNewB2ECommands() {
        //NR800 doesn't have B2E Command
    }

    @Override
    protected void onNewSettingCommands() {
        newRfPowerCommand();
        newRfSensitivityCommand();
        newRxDecodeTypeCommand();
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
        mAdapter.add(mStartInventoryCommand);
        mAdapter.add(mInventoryActiveMode);
    }

    @Override
    protected void onShowAdvanceViews() {
        mAdapter.add(mScanModeCommand);
        mAdapter.add(mPrefixCommand);
        mAdapter.add(mSuffixCommand);
        mAdapter.add(mTidDelimiterCommand);
        mAdapter.add(mBuzzerOperationCommand);
        mAdapter.add(mControlBuzzerCommand);
        mAdapter.add(mVibratorStateCommand);
        mAdapter.add(mTagEventIntervalCommand);
        mAdapter.add(mEventTypeCommand);
    }

    @Override
    protected void onShowB2ECommands() {
        //NR800 doesn't have B2E Command
    }

    @Override
    protected void onShowSettingViews() {
        mAdapter.add(mRfPowerCommand);
        mAdapter.add(mRfSensitivityCommand);
        mAdapter.add(mRxDecodeTypeCommand);
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
        mStartInventoryCommand = new GeneralCommandItem("Start Inventory", null, "Start"
                , new SpinnerParamData<>(TagPresentedType.class));
        mStartInventoryCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mStartInventoryCommand.getViewDataArray()[0];
            mUhf.startInventory((TagPresentedType) viewData.getSelected());
        });
    }

    private void newInventoryActiveModeCommand() {
        mInventoryActiveMode = new GeneralCommandItem("Inventory Active Mode", "Get", "Set", new SpinnerParamData<>(new ActiveMode[]{READ, COMMAND}));
        mInventoryActiveMode.setLeftOnClickListener(v -> ((NR800) mUhf).getInventoryActiveMode(true));
        mInventoryActiveMode.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mInventoryActiveMode.getViewDataArray()[0];
            ((NR800) mUhf).setInventoryActiveMode(true, (ActiveMode) viewData.getSelected());
        });
    }

    private void newPrefixCommand() {
        mPrefixCommand = new GeneralCommandItem("Get/Set Prefix"
                , new ASCIIEditTextParamData("Prefix"));
        mPrefixCommand.setRightOnClickListener(v -> {
            ASCIIEditTextParamData prefix = (ASCIIEditTextParamData) mPrefixCommand.getViewDataArray()[0];
            ((NR800) mUhf).setPrefix(prefix.getSelected());
        });
        mPrefixCommand.setLeftOnClickListener(v -> ((NR800) mUhf).getPrefix());
    }

    private void newSuffixCommand() {
        mSuffixCommand = new GeneralCommandItem("Get/Set Suffix"
                , new ASCIIEditTextParamData("Suffix"));
        mSuffixCommand.setRightOnClickListener(v -> {
            ASCIIEditTextParamData prefix = (ASCIIEditTextParamData) mSuffixCommand.getViewDataArray()[0];
            ((NR800) mUhf).setSuffix(prefix.getSelected());
        });
        mSuffixCommand.setLeftOnClickListener(v -> ((NR800) mUhf).getSuffix());
    }

    private void newTidDelimiterCommand() {
        mTidDelimiterCommand = new GeneralCommandItem("Get/Set TID Delimiter"
                , new ASCIIEditTextParamData("TID Delimiter"));
        mTidDelimiterCommand.setRightOnClickListener(v -> {
            ASCIIEditTextParamData tidDelimiter = (ASCIIEditTextParamData) mTidDelimiterCommand.getViewDataArray()[0];
            if (tidDelimiter.getSelected() != null && tidDelimiter.getSelected().length == 1) {
                ((NR800) mUhf).setTidDelimiter(tidDelimiter.getSelected()[0]);
            } else if (tidDelimiter.getSelected() == null || tidDelimiter.getSelected().length == 0) {
                ((NR800) mUhf).setTidDelimiter((byte) 0xFF);
            } else {
                Toaster.showToast(getContext(), "MAX Length is 1!", Toast.LENGTH_LONG);
            }
        });
        mTidDelimiterCommand.setLeftOnClickListener(v -> ((NR800) mUhf).getTidDelimiter());
    }

    private void newScanModeCommand() {
        mScanModeCommand = new GeneralCommandItem("Get/Set Scan Mode"
                , new SpinnerParamData<>(ScanMode.class));
        mScanModeCommand.setRightOnClickListener(v -> {
            SpinnerParamData scanMode = (SpinnerParamData) mScanModeCommand.getViewDataArray()[0];
            ((NR800) mUhf).setScanMode(mTemp, (ScanMode) scanMode.getSelected());
        });
        mScanModeCommand.setLeftOnClickListener(v -> ((NR800) mUhf).getScanMode(mTemp));
    }

    private void newBuzzerOperationCommand() {
        mBuzzerOperationCommand = new GeneralCommandItem("Get/Set BuzzerAdapter Operation"
                , new SpinnerParamData<>(new BuzzerOperationMode[]{BuzzerOperationMode.OFF, BuzzerOperationMode.REPEAT}));
        mBuzzerOperationCommand.setLeftOnClickListener(v -> ((NR800) mUhf).getBuzzerOperationMode(mTemp));
        mBuzzerOperationCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mBuzzerOperationCommand.getViewDataArray()[0];
            ((NR800) mUhf).setBuzzerOperationMode(mTemp, (BuzzerOperationMode) viewData.getSelected());
        });
    }

    private void newControlBuzzerCommand() {
        mControlBuzzerCommand = new GeneralCommandItem("Control BuzzerAdapter", null, "Control"
                , new SpinnerParamData<>(BuzzerAction.class));
        mControlBuzzerCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mControlBuzzerCommand.getViewDataArray()[0];
            ((NR800) mUhf).controlBuzzer((BuzzerAction) viewData.getSelected());
        });
    }

    private void newVibratorStateCommand() {
        mVibratorStateCommand = new GeneralCommandItem("Get/Set Vibrator State"
                , new SpinnerParamData<>(State.class));
        mVibratorStateCommand.setLeftOnClickListener(v -> ((NR800) mUhf).getVibratorState(mTemp));
        mVibratorStateCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mVibratorStateCommand.getViewDataArray()[0];
            ((NR800) mUhf).setVibratorState(mTemp, (State) viewData.getSelected());
        });
    }

    private void newTagEventIntervalCommand() {
        mTagEventIntervalCommand = new GeneralCommandItem("Get/Set Tag Event Interval"
                , new SeekBarParamData(0, 254));
        mTagEventIntervalCommand.setLeftOnClickListener(v -> ((NR800) mUhf).getTagEventInterval(mTemp));
        mTagEventIntervalCommand.setRightOnClickListener(v -> {
            SeekBarParamData viewData = (SeekBarParamData) mTagEventIntervalCommand.getViewDataArray()[0];
            ((NR800) mUhf).setTagEventInterval(mTemp, viewData.getSelected());
        });
    }

    private void newEventTypeCommand() {
        EventTypesParamData mEventTypeParamData = new EventTypesParamData(
                EVENT_TYPES, null
                , TAG_EVENT_FORMAT);
        mEventTypeCommand = new GeneralCommandItem("Get/Set Event Type"
                , mEventTypeParamData);
        mEventTypeParamData.setOnFirstItemSelectedListener(selected -> {
            if (selected.equals(EVENT_TYPES[0])) {
                mEventTypeParamData.setMiddleChoices(null);
                mEventTypeParamData.setLastChoices(TAG_EVENT_FORMAT);
                mEventTypeParamData.getLastSelect().clear();
            } else if (selected.equals(EVENT_TYPES[1])) {
                mEventTypeParamData.setMiddleChoices(BASE_TEXT_TAG_EVENT_FORMAT_CHOICE);
                mEventTypeParamData.setLastChoices(ADDITION_TEXT_TAG_EVENT_FORMAT_CHOICE);
                mEventTypeParamData.getLastSelect().clear();
            }
            mAdapter.notifyItemChanged(mEventTypeCommand.getPosition());
        });
        mEventTypeCommand.setLeftOnClickListener(v -> ((NR800) mUhf).getEventType(mTemp));
        mEventTypeCommand.setRightOnClickListener(v -> {
            EventTypesParamData event = (EventTypesParamData) mEventTypeCommand.getViewDataArray()[0];
            String eventType = event.getFirstSelect();
            if (eventType.equals(EVENT_TYPES[0])) {
                TagPresentedEvent.Builder builder = new TagPresentedEvent.Builder();
                if (event.getLastSelect().contains(TAG_EVENT_FORMAT[0])) {
                    builder.setRemoveEvent(true);
                }
                if (event.getLastSelect().contains(TAG_EVENT_FORMAT[1])) {
                    builder.setTidBank(true);
                }
                ((NR800) mUhf).setEventType(mTemp, builder.build());
            } else if (eventType.equals(EVENT_TYPES[1])) {
                TextTagEvent.Builder builder = new TextTagEvent.Builder(TextTagEvent.BaseTextEventFormat.valueOf(event.getMiddleSelect()));
                if (event.getLastSelect().contains(ADDITION_TEXT_TAG_EVENT_FORMAT_CHOICE[0])) {
                    builder.setTidBank(true);
                }
                ((NR800) mUhf).setEventType(mTemp, builder.build());
            }
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
        mTagPresentedRepeatIntervalCommand.setLeftOnClickListener(v -> ((NR800) mUhf).getTagPresentedRepeatInterval(mTemp));
        mTagPresentedRepeatIntervalCommand.setRightOnClickListener(v -> {
            SeekBarParamData viewData = (SeekBarParamData) mTagPresentedRepeatIntervalCommand.getViewDataArray()[0];
            ((NR800) mUhf).setTagPresentedRepeatInterval(mTemp, viewData.getSelected());
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
                } catch (NumberFormatException | NullPointerException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void newQCommand() {
        mQCommand = new GeneralCommandItem("Get/Set Q"
                , new SeekBarParamData(0, 15));
        mQCommand.setLeftOnClickListener(v -> mUhf.getQValue(mTemp));
        mQCommand.setRightOnClickListener(v -> {
            SeekBarParamData viewData = (SeekBarParamData) mQCommand.getViewDataArray()[0];
            mUhf.setQValue(mTemp, (byte) viewData.getSelected());
        });
    }

    private void newSessionTargetCommand() {
        mSessionTargetCommand = new GeneralCommandItem("Get/Set Session and Target"
                , new TwoSpinnerParamData<>(Session.values(), Target.getAbTargets()));
        TwoSpinnerParamData<Session, Target> viewData = (TwoSpinnerParamData) mSessionTargetCommand.getViewDataArray()[0];
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

    private void newRxDecodeTypeCommand() {
        mRxDecodeTypeCommand = new GeneralCommandItem("Get/Set Rx Decode"
                , new SpinnerParamData<>(RxDecodeType.class));
        mRxDecodeTypeCommand.setLeftOnClickListener(v -> mUhf.getRxDecode(mTemp));
        mRxDecodeTypeCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mRxDecodeTypeCommand.getViewDataArray()[0];
            mUhf.setRxDecode(mTemp, (RxDecodeType) viewData.getSelected());
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
}
