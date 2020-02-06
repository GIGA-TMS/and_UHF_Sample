package com.gigatms.uhf.deviceControl;

import android.os.Bundle;

import com.gigatms.CommunicationType;
import com.gigatms.TS100;
import com.gigatms.UHFCallback;
import com.gigatms.uhf.DeviceControlFragment;
import com.gigatms.uhf.GeneralCommandItem;
import com.gigatms.uhf.Toaster;
import com.gigatms.uhf.paramsData.CheckboxListParamData;
import com.gigatms.uhf.paramsData.EditTextParamData;
import com.gigatms.uhf.paramsData.EditTextTitleParamData;
import com.gigatms.uhf.paramsData.EventTypesParamData;
import com.gigatms.uhf.paramsData.InterchangeableParamData;
import com.gigatms.uhf.paramsData.ParamData;
import com.gigatms.uhf.paramsData.SeekBarParamData;
import com.gigatms.uhf.paramsData.SeekBarTitleParamData;
import com.gigatms.uhf.paramsData.SpinnerParamData;
import com.gigatms.uhf.paramsData.SpinnerTitleParamData;
import com.gigatms.uhf.paramsData.TwoSpinnerParamData;
import com.gigatms.exceptions.ErrorParameterException;
import com.gigatms.parameters.ActiveMode;
import com.gigatms.parameters.BarcodeFormat;
import com.gigatms.parameters.BuzzerAction;
import com.gigatms.parameters.BuzzerOperationMode;
import com.gigatms.parameters.DecodedTagData;
import com.gigatms.parameters.IONumber;
import com.gigatms.parameters.IOState;
import com.gigatms.parameters.KeyboardSimulation;
import com.gigatms.parameters.MemoryBank;
import com.gigatms.parameters.MemoryBankSelection;
import com.gigatms.parameters.OutputInterface;
import com.gigatms.parameters.PostDataDelimiter;
import com.gigatms.parameters.RfSensitivityLevel;
import com.gigatms.parameters.RxDecodeType;
import com.gigatms.parameters.Session;
import com.gigatms.parameters.TagDataEncodeType;
import com.gigatms.parameters.TagInformationFormat;
import com.gigatms.parameters.TagPresentedType;
import com.gigatms.parameters.Target;
import com.gigatms.parameters.b2e.BaseTagData;
import com.gigatms.parameters.b2e.BaseTagData.EpcHeader;
import com.gigatms.parameters.b2e.CompanyPrefixLength;
import com.gigatms.parameters.b2e.Filter;
import com.gigatms.parameters.b2e.SGTIN96EASTagData;
import com.gigatms.parameters.b2e.SGTIN96TagData;
import com.gigatms.parameters.b2e.UDCTagData;
import com.gigatms.parameters.event.BaseTagEvent;
import com.gigatms.parameters.event.TagPresentedEvent;
import com.gigatms.parameters.event.TagPresentedEventEx;
import com.gigatms.tools.GLog;
import com.gigatms.tools.GTool;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.widget.Toast.LENGTH_LONG;
import static com.gigatms.parameters.MemoryBankSelection.EPC_ASCII;
import static com.gigatms.parameters.OutputInterface.TCP_SERVER;
import static com.gigatms.parameters.Session.SL;
import static com.gigatms.parameters.TagDataEncodeType.EAN_UPC;
import static com.gigatms.parameters.TagDataEncodeType.EAN_UPC_EAS;
import static com.gigatms.parameters.TagDataEncodeType.RAW_DATA;
import static com.gigatms.parameters.TagDataEncodeType.UDC;
import static com.gigatms.parameters.b2e.BaseTagData.EpcHeader.EPC_EAS;
import static com.gigatms.parameters.b2e.BaseTagData.EpcHeader.EPC_SGTIN96;
import static com.gigatms.parameters.b2e.BaseTagData.EpcHeader.EPC_UDC;
import static com.gigatms.tools.GTool.isDigital;

public class TS100DeviceControlFragment extends DeviceControlFragment {
    private static final String TAG = TS100DeviceControlFragment.class.getSimpleName();
    private final String[] SECOND_CHOICES = {"REMOVE EVENT", "TID BANK"};
    private final String[] EVENT_TYPES = {"TAG_PRESENTED_EVENT", "TAG_PRESENTED_EVENT_EX"};

    private GeneralCommandItem mStopInventoryCommand;
    private GeneralCommandItem mInventoryCommand;
    private GeneralCommandItem mInventoryExCommand;
    GeneralCommandItem mInventoryActiveMode;

    private GeneralCommandItem mReadTagExWithPasswordCommand;
    private GeneralCommandItem mReadTagExCommand;
    private GeneralCommandItem mWriteTagExWithPasswordCommand;
    private GeneralCommandItem mWriteTagExCommand;

    private GeneralCommandItem mRfPowerCommand;
    private GeneralCommandItem mRfSensitivityCommand;
    private GeneralCommandItem mRxDecodeTypeCommand;
    private GeneralCommandItem mSessionTargetCommand;
    private GeneralCommandItem mQCommand;
    private GeneralCommandItem mFrequencyCommand;
    private GeneralCommandItem mTagRemovedThresholdCommand;
    private GeneralCommandItem mTagPresentedRepeatIntervalCommand;
    private GeneralCommandItem mInventoryRoundIntervalCommand;
    private GeneralCommandItem mGetFwVersionCommand;

    private GeneralCommandItem mBleDeviceNameCommand;
    private GeneralCommandItem mBuzzerOperationCommand;

    private GeneralCommandItem mControlBuzzerCommand;
    private GeneralCommandItem mOutputInterfacesCommand;
    private GeneralCommandItem mEventTypeCommand;
    private GeneralCommandItem mFilterCommand;
    private GeneralCommandItem mPostDataDelimiterCommand;
    private GeneralCommandItem mMemoryBankSelectionCommand;
    private GeneralCommandItem mRemoteHostCommand;
    private GeneralCommandItem mSsidPasswordCommand;
    private GeneralCommandItem mSsidPasswordIpCommand;
    private GeneralCommandItem mWiFiMacAddressCommand;
    private GeneralCommandItem mBarcodeReadFormatCommand;

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
                    eventType.setLastSelect(secondSelected);
                } else if (baseTagEvent instanceof TagPresentedEventEx) {
                    stringBuilder.append(EVENT_TYPES[1]);
                    eventType.setFirstSelect(EVENT_TYPES[1]);
                    eventType.setLastSelect(new HashSet<>());
                }

                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mEventTypeCommand.getPosition()));
                onUpdateLog(TAG, "didGetEventType: " + stringBuilder.toString());
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
            public void didGetPostDataDelimiter(Set<PostDataDelimiter> postDataDelimiter) {
                CheckboxListParamData selected1 = (CheckboxListParamData) mPostDataDelimiterCommand.getViewDataArray()[0];
                selected1.setSelected(postDataDelimiter);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mPostDataDelimiterCommand.getPosition()));
                onUpdateLog(TAG, "didGetPostDataDelimiter: " + postDataDelimiter);
            }

            @Override
            public void didGetMemoryBankSelection(Set<MemoryBankSelection> memoryBankSelections) {
                CheckboxListParamData selected1 = (CheckboxListParamData) mMemoryBankSelectionCommand.getViewDataArray()[0];
                selected1.setSelected(memoryBankSelections);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mMemoryBankSelectionCommand.getPosition()));
                onUpdateLog(TAG, "didGetMemoryBankSelection: " + memoryBankSelections);
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
            public void didGetInventoryActiveMode(ActiveMode activeMode) {
                SpinnerParamData selected = (SpinnerParamData) mInventoryActiveMode.getViewDataArray()[0];
                selected.setSelected(activeMode);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mInventoryActiveMode.getPosition()));
                onUpdateLog(TAG, "didGetInventoryActiveMode: " + activeMode.name());
            }

            @Override
            public void didGetBarcodeReadFormat(BarcodeFormat defaultFormat) {
                SpinnerParamData selected = (SpinnerParamData) mBarcodeReadFormatCommand.getViewDataArray()[0];
                selected.setSelected(defaultFormat);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mBarcodeReadFormatCommand.getPosition()));
                onUpdateLog(TAG, "didGetBarcodeReadFormat: " + defaultFormat.name());
            }

            @Override
            public void didReadTagEx(BaseTagData baseTagData) {
                StringBuilder stringBuilder = new StringBuilder();
                if (baseTagData instanceof SGTIN96TagData) {
                    stringBuilder.append(EPC_SGTIN96.name());
                    stringBuilder.append("\n\tBarcode: ").append(baseTagData.getBarcode());
                    stringBuilder.append("\n\tFilter: ").append(((SGTIN96TagData) baseTagData).getFilter());
                    stringBuilder.append("\n\tCompanyPrefixLength: ").append(((SGTIN96TagData) baseTagData).getCompanyPrefixLength());
                    stringBuilder.append("\n\tSerial Number: ").append(((SGTIN96TagData) baseTagData).getSerialNumber());
                } else if (baseTagData instanceof UDCTagData) {
                    stringBuilder.append(EPC_SGTIN96.name());
                    stringBuilder.append("\n\tBarcode: ").append(baseTagData.getBarcode());
                }
                onUpdateLog(TAG, "didReadTagEx: " + stringBuilder.toString());
            }

            @Override
            public void didGetRemoteHost(int connectTimeout, InetSocketAddress socketAddress) {
                if (socketAddress != null) {
                    SeekBarTitleParamData timeout = (SeekBarTitleParamData) mRemoteHostCommand.getViewDataArray()[0];
                    EditTextTitleParamData ip = (EditTextTitleParamData) mRemoteHostCommand.getViewDataArray()[1];
                    EditTextTitleParamData port = (EditTextTitleParamData) mRemoteHostCommand.getViewDataArray()[2];
                    timeout.setSelected(connectTimeout);
                    String ipString = socketAddress.getAddress().getHostAddress();
                    ip.setSelected(ipString);
                    String portString = "" + socketAddress.getPort();
                    port.setSelected(portString);
                    mRecyclerView.post(() -> mAdapter.notifyItemChanged(mRemoteHostCommand.getPosition()));
                    onUpdateLog(TAG, "didGetRemoteHost"
                            + "\n\tConnect Timeout: " + (timeout.getSelected() == 0 ? "0 (Stay Connected)" : "" + timeout.getSelected())
                            + "\n\tIP: " + ipString
                            + "\n\tPort: " + portString);
                } else {
                    onUpdateLog(TAG, "didGetRemoteHost: No Remote Host!");
                }
            }

            @Override
            public void didGetWiFiMacAddress(String macAddress) {
                onUpdateLog(TAG, "didGetWiFiMacAddress: " + macAddress);
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
        newFilterCommand();
        newPostDataDelimiterCommand();
        newMemoryBankSelectionCommand();
        newRemoteHostCommand();
        newSsidPasswordCommand();
        newSsidPasswordIpCommand();
        newWiFiMacAddressCommand();
        newBarcodeReadFormat();
    }

    @Override
    protected void onNewB2ECommands() {
        newReadTagExWithPassword();
        newReadTagExCommand();
        newWriteTagExWithPassword();
        newWriteTagExCommand();
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
        mAdapter.add(mRemoteHostCommand);
        if (!mUhf.getCommunicationType().equals(CommunicationType.TCP)) {
            mAdapter.add(mSsidPasswordCommand);
            mAdapter.add(mSsidPasswordIpCommand);
        }
        mAdapter.add(mWiFiMacAddressCommand);
        mAdapter.add(mBarcodeReadFormatCommand);
    }

    @Override
    protected void onShowB2ECommands() {
        mAdapter.add(mReadTagExWithPasswordCommand);
        mAdapter.add(mReadTagExCommand);
        mAdapter.add(mWriteTagExWithPasswordCommand);
        mAdapter.add(mWriteTagExCommand);
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
        mAdapter.add(mGetFwVersionCommand);
    }

    private void newStartInventoryCommandEx() {
        mInventoryExCommand = new GeneralCommandItem("Start Inventory Ex", null, "Start Ex"
                , new CheckboxListParamData<>(EnumSet.of(UDC, EAN_UPC_EAS, EAN_UPC, RAW_DATA)));
        mInventoryExCommand.setRightOnClickListener(v -> {
            CheckboxListParamData viewData = (CheckboxListParamData) mInventoryExCommand.getViewDataArray()[0];
            ((TS100) mUhf).startInventoryEx(viewData.getSelected());
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

    protected void newInventoryActiveModeCommand() {
        mInventoryActiveMode = new GeneralCommandItem("Inventory Active Mode", "Get", "Set", new SpinnerParamData<>(new ActiveMode[]{
                ActiveMode.READ,
                ActiveMode.COMMAND,
                ActiveMode.TAG_ANALYSIS,
                ActiveMode.CUSTOMIZED_READ,
                ActiveMode.DEACTIVATE,
                ActiveMode.REACTIVATE,
                ActiveMode.DEACTIVATE_USER_BANK,
                ActiveMode.REACTIVATE_USER_BANK,
        }));
        mInventoryActiveMode.setLeftOnClickListener(v -> ((TS100) mUhf).getInventoryActiveMode(true));
        mInventoryActiveMode.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mInventoryActiveMode.getViewDataArray()[0];
            ((TS100) mUhf).setInventoryActiveMode(true, (ActiveMode) viewData.getSelected());
        });
    }

    private void newReadTagExWithPassword() {
        mReadTagExWithPasswordCommand = new GeneralCommandItem("Read Tag Ex", null, "Read"
                , new EditTextTitleParamData("Password", "00000000", "00000000"));//TODO max length 8 and number
        mReadTagExWithPasswordCommand.setRightOnClickListener(v -> {
            EditTextTitleParamData password = (EditTextTitleParamData) mReadTagExWithPasswordCommand.getViewDataArray()[0];
            ((TS100) mUhf).readTagEx(password.getSelected());
        });
    }

    private void newReadTagExCommand() {
        mReadTagExCommand = new GeneralCommandItem("Read Tag Ex", null, "Read");
        mReadTagExCommand.setRightOnClickListener(v -> ((TS100) mUhf).readTagEx());
    }

    private void newWriteTagExWithPassword() {
        InterchangeableParamData<EpcHeader> b2eTagData = getTagDataParams();
        mWriteTagExWithPasswordCommand = new GeneralCommandItem("Write Tag Ex", null, "Write"
                , new EditTextTitleParamData("Password", "00000000", "00000000")
                , b2eTagData);
        setB2EOnclickListener(mWriteTagExWithPasswordCommand, b2eTagData);
        mWriteTagExWithPasswordCommand.setRightOnClickListener(v -> {
            try {
                EditTextTitleParamData accessPassword = (EditTextTitleParamData) mWriteTagExWithPasswordCommand.getViewDataArray()[0];
                InterchangeableParamData tagDataParameters = (InterchangeableParamData) mWriteTagExWithPasswordCommand.getViewDataArray()[1];
                BaseTagData baseTagData = getBaseTagData(tagDataParameters);
                if (baseTagData != null) {
                    ((TS100) mUhf).writeTagEx(accessPassword.getSelected(), baseTagData);
                }
            } catch (ErrorParameterException e) {
                e.printStackTrace();
                Toaster.showToast(getContext(), e.getMessage(), LENGTH_LONG);
            }
        });
    }

    private void newWriteTagExCommand() {
        InterchangeableParamData<EpcHeader> b2eTagData = getTagDataParams();
        mWriteTagExCommand = new GeneralCommandItem("Write Tag Ex", null, "Write", b2eTagData);
        setB2EOnclickListener(mWriteTagExCommand, b2eTagData);
        mWriteTagExCommand.setRightOnClickListener(v -> {
            try {
                InterchangeableParamData tagDataParameters = (InterchangeableParamData) mWriteTagExCommand.getViewDataArray()[0];
                BaseTagData baseTagData = getBaseTagData(tagDataParameters);
                if (baseTagData != null) {
                    ((TS100) mUhf).writeTagEx(baseTagData);
                }
            } catch (ErrorParameterException e) {
                e.printStackTrace();
                Toaster.showToast(getContext(), e.getMessage(), LENGTH_LONG);
            }
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
        mBuzzerOperationCommand.setLeftOnClickListener(v -> ((TS100) mUhf).getBuzzerOperationMode(mTemp));
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
        mOutputInterfacesCommand.setLeftOnClickListener(v -> ((TS100) mUhf).getOutputInterfaces(mTemp));
        mOutputInterfacesCommand.setRightOnClickListener(v -> {
            SpinnerParamData keyboard = (SpinnerParamData) mOutputInterfacesCommand.getViewDataArray()[0];
            CheckboxListParamData outputInterface = (CheckboxListParamData) mOutputInterfacesCommand.getViewDataArray()[1];
            ((TS100) mUhf).setOutputInterfaces(mTemp, (KeyboardSimulation) keyboard.getSelected(), outputInterface.getSelected());
        });
    }

    private void newEventTypeCommand() {
        EventTypesParamData mEventTypeParamData = new EventTypesParamData(
                EVENT_TYPES, null
                , SECOND_CHOICES);
        mEventTypeCommand = new GeneralCommandItem("Get/Set Event Type"
                , mEventTypeParamData);
        mEventTypeParamData.setOnFirstItemSelectedListener(selected -> {
            if (selected.equals(EVENT_TYPES[0])) {
                mEventTypeParamData.setLastChoices(SECOND_CHOICES);
            } else if (selected.equals(EVENT_TYPES[1])) {
                mEventTypeParamData.setLastChoices(new String[0]);
            }
            mAdapter.notifyItemChanged(mEventTypeCommand.getPosition());
        });
        mEventTypeCommand.setLeftOnClickListener(v -> ((TS100) mUhf).getEventType(mTemp));
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
                ((TS100) mUhf).setEventType(mTemp, builder.build());
            } else if (eventType.equals(EVENT_TYPES[1])) {
                ((TS100) mUhf).setEventType(mTemp, new TagPresentedEventEx());
            }
        });
    }

    private void newFilterCommand() {
        mFilterCommand = new GeneralCommandItem("Get/Set Filter",
                new CheckboxListParamData<>(EnumSet.of(UDC, EAN_UPC_EAS, EAN_UPC, RAW_DATA)));
        mFilterCommand.setRightOnClickListener(v -> {
            CheckboxListParamData viewData = (CheckboxListParamData) mFilterCommand.getViewDataArray()[0];
            ((TS100) mUhf).setFilter(mTemp, viewData.getSelected());
        });
        mFilterCommand.setLeftOnClickListener(v -> {
            ((TS100) mUhf).getFilter(mTemp);
        });
    }

    private void newPostDataDelimiterCommand() {
        mPostDataDelimiterCommand = new GeneralCommandItem("Get/Set Post Data Delimiter"
                , new CheckboxListParamData<>(PostDataDelimiter.class));
        mPostDataDelimiterCommand.setLeftOnClickListener(v -> {
            ((TS100) mUhf).getPostDataDelimiter(mTemp);
        });
        mPostDataDelimiterCommand.setRightOnClickListener(v -> {
            CheckboxListParamData viewData = (CheckboxListParamData) mPostDataDelimiterCommand.getViewDataArray()[0];
            ((TS100) mUhf).setPostDataDelimiter(mTemp, viewData.getSelected());
        });
    }

    private void newMemoryBankSelectionCommand() {
        mMemoryBankSelectionCommand = new GeneralCommandItem("Get/Set Memory Bank Selection"
                , new CheckboxListParamData<>(EnumSet.range(MemoryBankSelection.PC, EPC_ASCII)));
        mMemoryBankSelectionCommand.setLeftOnClickListener(v -> ((TS100) mUhf).getMemoryBankSelection(mTemp));

        mMemoryBankSelectionCommand.setRightOnClickListener(v -> {
            CheckboxListParamData viewData = (CheckboxListParamData) mMemoryBankSelectionCommand.getViewDataArray()[0];
            ((TS100) mUhf).setMemoryBankSelection(mTemp, viewData.getSelected());
        });
    }

    private void newInventoryRoundIntervalCommand() {
        mInventoryRoundIntervalCommand = new GeneralCommandItem("Get/Set Inventory Round Interval"
                , new SeekBarParamData(0, 254));
        mInventoryRoundIntervalCommand.setLeftOnClickListener(v -> mUhf.getInventoryRoundInterval(mTemp));
        mInventoryRoundIntervalCommand.setRightOnClickListener(v -> {
            SeekBarParamData viewData = (SeekBarParamData) mInventoryRoundIntervalCommand.getViewDataArray()[0];
            GLog.v(TAG, "InventoryRoundInterval: " + viewData.getSelected());
            mUhf.setInventoryRoundInterval(mTemp, viewData.getSelected());
        });
    }

    private void newTagPresentedEventThresholdCommand() {
        mTagPresentedRepeatIntervalCommand = new GeneralCommandItem("Get/Set Tag Presented Repeat Interval"
                , new SeekBarParamData(0, 254));
        mTagPresentedRepeatIntervalCommand.setLeftOnClickListener(v -> ((TS100) mUhf).getTagPresentedRepeatInterval(mTemp));
        mTagPresentedRepeatIntervalCommand.setRightOnClickListener(v -> {
            SeekBarParamData viewData = (SeekBarParamData) mTagPresentedRepeatIntervalCommand.getViewDataArray()[0];
            GLog.v(TAG, "Repeat: " + viewData.getSelected());
            ((TS100) mUhf).setTagPresentedRepeatInterval(mTemp, viewData.getSelected());
        });
    }

    private void newTagRemovedThresholdCommand() {
        mTagRemovedThresholdCommand = new GeneralCommandItem("Get/Set Tag Removed Threshold"
                , new SeekBarParamData(0, 254));
        mTagRemovedThresholdCommand.setLeftOnClickListener(v -> mUhf.getTagRemovedThreshold(mTemp));
        mTagRemovedThresholdCommand.setRightOnClickListener(v -> {
            SeekBarParamData viewData = (SeekBarParamData) mTagRemovedThresholdCommand.getViewDataArray()[0];
            GLog.v(TAG, "Removed: " + viewData.getSelected());
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
        mGetFwVersionCommand = new GeneralCommandItem("Get Firmware Version", null, "Get");
        mGetFwVersionCommand.setRightOnClickListener(v -> mUhf.getFirmwareVersion());
    }

    private void newRemoteHostCommand() {
        mRemoteHostCommand = new GeneralCommandItem("Get/Set Remote Host"
                , new SeekBarTitleParamData("Connect Timeout(100ms)", 0, 7)
                , new EditTextTitleParamData("IP", "XXX.XXX.XXX.XXX")
                , new EditTextTitleParamData("Port", "1111"));
        mRemoteHostCommand.setLeftOnClickListener(v -> ((TS100) mUhf).getRemoteHost());
        mRemoteHostCommand.setRightOnClickListener(v -> new Thread(() -> {
            try {
                SeekBarTitleParamData connectTimeout = (SeekBarTitleParamData) mRemoteHostCommand.getViewDataArray()[0];
                EditTextTitleParamData ip = (EditTextTitleParamData) mRemoteHostCommand.getViewDataArray()[1];
                EditTextTitleParamData port = (EditTextTitleParamData) mRemoteHostCommand.getViewDataArray()[2];
                if (GTool.isDigital(port.getSelected()) && !port.getSelected().equals("") && !ip.getSelected().equals("")) {
                    InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getByName(ip.getSelected()), Integer.parseInt(port.getSelected()));
                    ((TS100) mUhf).setRemoteHost((byte) connectTimeout.getSelected(), inetSocketAddress);
                } else if (port.getSelected().equals("") && ip.getSelected().equals("")) {
                    ((TS100) mUhf).setRemoteHost(0, null);
                } else if (port.getSelected().equals("")) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> Toaster.showToast(getContext(), "Please fill the Port!", LENGTH_LONG));
                    }
                } else if (ip.getSelected().equals("")) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> Toaster.showToast(getContext(), "Please fill the IP!", LENGTH_LONG));
                    }
                } else {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> Toaster.showToast(getContext(), "Port should be integer", LENGTH_LONG));
                    }
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toaster.showToast(getContext(), e.getMessage(), LENGTH_LONG));
                }
            }
        }).start());
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

    private void newWiFiMacAddressCommand() {
        mWiFiMacAddressCommand = new GeneralCommandItem("Get Wi-Fi Mac Address", null, "Get");
        mWiFiMacAddressCommand.setRightOnClickListener((view) -> ((TS100) mUhf).getWiFiMacAddress());
    }

    private void newBarcodeReadFormat() {
        mBarcodeReadFormatCommand = new GeneralCommandItem("Get/Set Barcode Read Format"
                , new SpinnerParamData<>(BarcodeFormat.class));
        mBarcodeReadFormatCommand.setLeftOnClickListener(v -> ((TS100) mUhf).getBarcodeReadFormat(mTemp));
        mBarcodeReadFormatCommand.setRightOnClickListener(v -> {
            SpinnerParamData spinnerParamData = (SpinnerParamData) mBarcodeReadFormatCommand.getViewDataArray()[0];
            ((TS100) mUhf).setBarcodeReadFormat(mTemp, (BarcodeFormat) spinnerParamData.getSelected());
        });
    }

    private InterchangeableParamData<EpcHeader> getTagDataParams() {
        List<ParamData> b2eTagDataStructure = new ArrayList<>();
        b2eTagDataStructure.add(new EditTextTitleParamData("Barcode", "GTIN8, 12, 13, and 14"));//TODO number only
        b2eTagDataStructure.add(new SpinnerTitleParamData<>(Filter.class));
        b2eTagDataStructure.add(new SpinnerTitleParamData<>(CompanyPrefixLength.class));
        b2eTagDataStructure.add(new EditTextTitleParamData("Serial Number", "", "1"));//TODO number only
        return new InterchangeableParamData<>("Epc Type", new EpcHeader[]{EPC_SGTIN96, EPC_EAS, EPC_UDC}, b2eTagDataStructure);
    }

    private void setB2EOnclickListener(GeneralCommandItem generalCommandItem, InterchangeableParamData<EpcHeader> b2eTagData) {
        b2eTagData.setOnFirstItemSelectedListener(selected -> {
            List<ParamData> b2eTagDataStructure = b2eTagData.getParamData();
            b2eTagDataStructure.clear();
            switch ((EpcHeader) selected) {
                case EPC_SGTIN96:
                    b2eTagDataStructure.add(new EditTextTitleParamData("Barcode", "GTIN8, 12, 13, and 14"));//TODO number only
                    b2eTagDataStructure.add(new SpinnerTitleParamData<>(Filter.class));
                    b2eTagDataStructure.add(new SpinnerTitleParamData<>(CompanyPrefixLength.class));
                    b2eTagDataStructure.add(new EditTextTitleParamData("Serial Number", "", "1"));//TODO number only
                    break;
                case EPC_EAS:
                    b2eTagDataStructure.add(new EditTextTitleParamData("Barcode", "GTIN8, 12, 13, and 14"));//TODO number only
                    b2eTagDataStructure.add(new SpinnerTitleParamData<>(Filter.class));
                    b2eTagDataStructure.add(new SpinnerTitleParamData<>(CompanyPrefixLength.class));
                    break;
                case EPC_UDC:
                    b2eTagDataStructure.add(new EditTextTitleParamData("Barcode", ""));
                    break;
            }
            mAdapter.notifyItemChanged(generalCommandItem.getPosition());
        });
    }

    private BaseTagData getBaseTagData(InterchangeableParamData tagDataParameters) throws ErrorParameterException {
        List<ParamData> data = tagDataParameters.getParamData();
        switch ((EpcHeader) tagDataParameters.getSelected()) {
            case EPC_SGTIN96:
                EditTextTitleParamData barcodeParameter = (EditTextTitleParamData) data.get(0);
                SpinnerTitleParamData filter = (SpinnerTitleParamData) data.get(1);
                SpinnerTitleParamData companyPrefixLength = (SpinnerTitleParamData) data.get(2);
                EditTextTitleParamData serialNumber = (EditTextTitleParamData) data.get(3);
                if (isDigital(serialNumber.getSelected())) {
                    return new SGTIN96TagData(barcodeParameter.getSelected()
                            , (Filter) filter.getSelected()
                            , (CompanyPrefixLength) companyPrefixLength.getSelected()
                            , Long.parseLong(serialNumber.getSelected()));
                }
                Toaster.showToast(getContext(), "Serial Number should be positive integer", LENGTH_LONG);
                break;
            case EPC_EAS:
                barcodeParameter = (EditTextTitleParamData) data.get(0);
                filter = (SpinnerTitleParamData) data.get(1);
                companyPrefixLength = (SpinnerTitleParamData) data.get(2);
                return new SGTIN96EASTagData(barcodeParameter.getSelected()
                        , (Filter) filter.getSelected()
                        , (CompanyPrefixLength) companyPrefixLength.getSelected());
            case EPC_UDC:
                barcodeParameter = (EditTextTitleParamData) data.get(0);
                return new UDCTagData(barcodeParameter.getSelected());
        }
        return null;
    }
}
