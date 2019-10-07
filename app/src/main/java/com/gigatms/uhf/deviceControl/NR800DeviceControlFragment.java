package com.gigatms.uhf.deviceControl;

import android.os.Bundle;
import android.widget.Toast;

import com.gigatms.DecodedTagData;
import com.gigatms.NR800;
import com.gigatms.TagInformationFormat;
import com.gigatms.UHFCallback;
import com.gigatms.uhf.DeviceControlFragment;
import com.gigatms.uhf.GeneralCommandItem;
import com.gigatms.uhf.Toaster;
import com.gigatms.uhf.paramsData.CheckBoxParamData;
import com.gigatms.uhf.paramsData.EditTextParamData;
import com.gigatms.uhf.paramsData.EditTextTitleParamData;
import com.gigatms.uhf.paramsData.SeekBarParamData;
import com.gigatms.uhf.paramsData.SpinnerParamData;
import com.gigatms.uhf.paramsData.SpinnerTitleParamData;
import com.gigatms.uhf.paramsData.TwoSpinnerParamData;
import com.gigatms.parameters.BuzzerAction;
import com.gigatms.parameters.BuzzerOperationMode;
import com.gigatms.parameters.IONumber;
import com.gigatms.parameters.IOState;
import com.gigatms.parameters.MemoryBank;
import com.gigatms.parameters.RfSensitivityLevel;
import com.gigatms.parameters.ScanMode;
import com.gigatms.parameters.Session;
import com.gigatms.parameters.TagPresentedType;
import com.gigatms.parameters.Target;
import com.gigatms.parameters.TextTagEventType;
import com.gigatms.tools.GTool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.gigatms.parameters.Session.SL;

public class NR800DeviceControlFragment extends DeviceControlFragment {
    private static final String TAG = NR800DeviceControlFragment.class.getSimpleName();

    private GeneralCommandItem mStopInventoryCommand;
    private GeneralCommandItem mSimpleStartIventoryCommand;
    private GeneralCommandItem mStartInventoryCommand;

    private GeneralCommandItem mReadWriteEpcCommand;
    private GeneralCommandItem mReadTagCommand;
    private GeneralCommandItem mWriteTagCommand;

    private GeneralCommandItem mRfPowerCommand;
    private GeneralCommandItem mRfSensitivityCommand;
    private GeneralCommandItem mSessionTargetCommand;
    private GeneralCommandItem mQCommand;
    private GeneralCommandItem mFrequencyCommand;
    private GeneralCommandItem mTagRemovedThresholdCommand;
    private GeneralCommandItem mTagPresentedRepeatIntervalCommand;
    private GeneralCommandItem mInventoryRoundIntervalCommand;

    private GeneralCommandItem mPrefixCommand;
    private GeneralCommandItem mSuffixCommand;
    private GeneralCommandItem mTidDelimiterCommand;
    private GeneralCommandItem mScanModeCommand;
    private GeneralCommandItem mBuzzerOperationCommand;
    private GeneralCommandItem mControlBuzzerCommand;
    private GeneralCommandItem mTagEventIntervalCommand;
    private GeneralCommandItem mVibratorStateCommand;
    private GeneralCommandItem mTextTagEventTypeCommand;

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
            public void didGetFrequencyList(final List<Double> frequencyList) {
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
            public void didReadEpc(byte[] epc) {
                EditTextTitleParamData secondParam = (EditTextTitleParamData) mReadWriteEpcCommand.getViewDataArray()[1];
                secondParam.setSelected(GTool.bytesToHexString(epc));
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mReadWriteEpcCommand.getPosition()));
                onUpdateLog(TAG, "didReadEpc: " + GTool.bytesToHexString(epc));
            }

            @Override
            public void didGetTagPresentedRepeatInterval(int hundredMilliSeconds) {
                SeekBarParamData selected = (SeekBarParamData) mTagPresentedRepeatIntervalCommand.getViewDataArray()[0];
                selected.setSelected(hundredMilliSeconds);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mTagPresentedRepeatIntervalCommand.getPosition()));
                onUpdateLog(TAG, "didGetTagPresentedRepeatInterval: " + hundredMilliSeconds + "*100 ms");
            }

            @Override
            public void didGetTagRemovedThreshold(int hundredMilliSeconds) {
                SeekBarParamData selected1 = (SeekBarParamData) mTagRemovedThresholdCommand.getViewDataArray()[0];
                selected1.setSelected(hundredMilliSeconds);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mTagRemovedThresholdCommand.getPosition()));
                onUpdateLog(TAG, "didGetTagRemovedThreshold: " + hundredMilliSeconds + "*100 ms");
            }

            @Override
            public void didGetInventoryRoundInterval(int tenMilliSeconds) {
                SeekBarParamData selected1 = (SeekBarParamData) mInventoryRoundIntervalCommand.getViewDataArray()[0];
                selected1.setSelected(tenMilliSeconds);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mInventoryRoundIntervalCommand.getPosition()));
                onUpdateLog(TAG, "didGetInventoryRoundInterval: " + tenMilliSeconds + "*10 ms");
            }

            @Override
            public void didGetTagEventInterval(int tenMilliSeconds) {
                SeekBarParamData selected1 = (SeekBarParamData) mTagEventIntervalCommand.getViewDataArray()[0];
                selected1.setSelected(tenMilliSeconds);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mTagEventIntervalCommand.getPosition()));
                onUpdateLog(TAG, "didGetTagEventInterval: " + tenMilliSeconds + "*10 ms");
            }

            @Override
            public void didGetVibratorState(boolean on) {
                CheckBoxParamData stateOn = (CheckBoxParamData) mVibratorStateCommand.getViewDataArray()[0];
                stateOn.setChecked(on);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mVibratorStateCommand.getPosition()));
                onUpdateLog(TAG, "didGetVibratorState On: " + on);
            }

            @Override
            public void didGetPrefix(String prefix) {
                EditTextParamData prefixData = (EditTextParamData) mPrefixCommand.getViewDataArray()[0];
                prefixData.setSelected(prefix);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mPrefixCommand.getPosition()));
                onUpdateLog(TAG, "didGetPrefix: " + prefix);
            }

            @Override
            public void didGetSuffix(String suffix) {
                EditTextParamData suffixData = (EditTextParamData) mSuffixCommand.getViewDataArray()[0];
                suffixData.setSelected(suffix);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mSuffixCommand.getPosition()));
                onUpdateLog(TAG, "didGetSuffix: " + suffix);
            }

            @Override
            public void didGetTidDelimiter(Character charAt) {
                EditTextParamData tidDelimiter = (EditTextParamData) mTidDelimiterCommand.getViewDataArray()[0];
                tidDelimiter.setSelected(charAt.toString());
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mTidDelimiterCommand.getPosition()));
                onUpdateLog(TAG, "didGetTidDelimiter: " + charAt);
            }

            @Override
            public void didGetScanMode(ScanMode scanMode) {
                SpinnerParamData selected1 = (SpinnerParamData) mScanModeCommand.getViewDataArray()[0];
                selected1.setSelected(scanMode);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mScanModeCommand.getPosition()));
                onUpdateLog(TAG, "didGetScanMode: " + scanMode.name());
            }

            @Override
            public void didGetTextTagEventType(TextTagEventType textTagEventType) {
                SpinnerParamData textTagEventView = (SpinnerParamData) mTextTagEventTypeCommand.getViewDataArray()[0];
                textTagEventView.setSelected(textTagEventType);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mTextTagEventTypeCommand.getPosition()));
                onUpdateLog(TAG, "didGetTextTagEventType: " + textTagEventType.name());

            }
        };

    }

    @Override
    protected void onNewInventoryCommands() {
        newStopInventoryCommand();
        newSimpleStartInventoryCommand();
        newStartInventoryCommand();
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
        newTextTagEventTypeCommand();
    }

    @Override
    protected void onNewReadWriteTagCommands() {
        newReadWriteEPCCommand();
        newReadTagCommand();
        newWriteTagCommand();
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
    }

    @Override
    protected void onShowInventoryViews() {
        mAdapter.add(mStopInventoryCommand);
        mAdapter.add(mSimpleStartIventoryCommand);
        mAdapter.add(mStartInventoryCommand);
    }

    @Override
    protected void onShowAdvanceViews() {
        mAdapter.add(mPrefixCommand);
        mAdapter.add(mSuffixCommand);
        mAdapter.add(mTidDelimiterCommand);
        mAdapter.add(mScanModeCommand);
        mAdapter.add(mBuzzerOperationCommand);
        mAdapter.add(mControlBuzzerCommand);
        mAdapter.add(mVibratorStateCommand);
        mAdapter.add(mTagEventIntervalCommand);
        mAdapter.add(mTextTagEventTypeCommand);
    }

    @Override
    protected void onShowReadWriteTagViews() {
        mAdapter.add(mReadWriteEpcCommand);
        mAdapter.add(mReadTagCommand);
        mAdapter.add(mWriteTagCommand);
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
    }

    private void newStopInventoryCommand() {
        mStopInventoryCommand = new GeneralCommandItem("Stop Inventory", null, "Stop");
        mStopInventoryCommand.setRightOnClickListener(v -> mUhf.stopInventory());
    }

    private void newSimpleStartInventoryCommand() {
        mSimpleStartIventoryCommand = new GeneralCommandItem("Start Inventory", null, "Start");
        mSimpleStartIventoryCommand.setRightOnClickListener(view -> ((NR800) mUhf).startInventory());
    }

    private void newStartInventoryCommand() {
        mStartInventoryCommand = new GeneralCommandItem("Start Inventory", null, "Start"
                , new SpinnerParamData<>(TagPresentedType.class));
        mStartInventoryCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mStartInventoryCommand.getViewDataArray()[0];
            mUhf.startInventory((TagPresentedType) viewData.getSelected());
        });
    }


    private void newPrefixCommand() {
        mPrefixCommand = new GeneralCommandItem("Get/Set Prefix"
                , new EditTextParamData("Prefix"));
        mPrefixCommand.setRightOnClickListener(v -> {
            EditTextParamData prefix = (EditTextParamData) mPrefixCommand.getViewDataArray()[0];
            ((NR800) mUhf).setPrefix(prefix.getSelected());
        });
        mPrefixCommand.setLeftOnClickListener(v -> ((NR800) mUhf).getPrefix());
    }

    private void newSuffixCommand() {
        mSuffixCommand = new GeneralCommandItem("Get/Set Suffix"
                , new EditTextParamData("Suffix"));
        mSuffixCommand.setRightOnClickListener(v -> {
            EditTextParamData prefix = (EditTextParamData) mSuffixCommand.getViewDataArray()[0];
            ((NR800) mUhf).setSuffix(prefix.getSelected());
        });
        mSuffixCommand.setLeftOnClickListener(v -> ((NR800) mUhf).getSuffix());
    }

    private void newTidDelimiterCommand() {
        mTidDelimiterCommand = new GeneralCommandItem("Get/Set TID Delimiter"
                , new EditTextParamData("TID Delimiter"));
        mTidDelimiterCommand.setRightOnClickListener(v -> {
            EditTextParamData prefix = (EditTextParamData) mTidDelimiterCommand.getViewDataArray()[0];
            ((NR800) mUhf).setTidDelimiter(prefix.getSelected().charAt(0));
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
        mBuzzerOperationCommand = new GeneralCommandItem("Get/Set Buzzer Operation"
                , new SpinnerParamData<>(new BuzzerOperationMode[]{BuzzerOperationMode.OFF, BuzzerOperationMode.REPEAT}));
        mBuzzerOperationCommand.setLeftOnClickListener(v -> {
            ((NR800) mUhf).getBuzzerOperationMode(mTemp);
        });
        mBuzzerOperationCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mBuzzerOperationCommand.getViewDataArray()[0];
            ((NR800) mUhf).setBuzzerOperationMode(mTemp, (BuzzerOperationMode) viewData.getSelected());
        });
    }

    private void newControlBuzzerCommand() {
        mControlBuzzerCommand = new GeneralCommandItem("Control Buzzer", null, "Control"
                , new SpinnerParamData<>(BuzzerAction.class));
        mControlBuzzerCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mControlBuzzerCommand.getViewDataArray()[0];
            ((NR800) mUhf).controlBuzzer((BuzzerAction) viewData.getSelected());
        });
    }

    private void newVibratorStateCommand() {
        mVibratorStateCommand = new GeneralCommandItem("Get/Set Vibrator State"
                , new CheckBoxParamData("On"));
        mVibratorStateCommand.setLeftOnClickListener(v -> {
            ((NR800) mUhf).getVibratorState(mTemp);
        });
        mVibratorStateCommand.setRightOnClickListener(v -> {
            CheckBoxParamData viewData = (CheckBoxParamData) mVibratorStateCommand.getViewDataArray()[0];
            ((NR800) mUhf).setVibratorState(mTemp, viewData.isChecked());
        });
    }


    private void newTagEventIntervalCommand() {
        mTagEventIntervalCommand = new GeneralCommandItem("Get/Set Tag Event Interval"
                , new SeekBarParamData(0, 255));
        mTagEventIntervalCommand.setLeftOnClickListener(v -> ((NR800) mUhf).getTagEventInterval(mTemp));
        mTagEventIntervalCommand.setRightOnClickListener(v -> {
            SeekBarParamData viewData = (SeekBarParamData) mTagEventIntervalCommand.getViewDataArray()[0];
            ((NR800) mUhf).setTagEventInterval(mTemp, viewData.getSelected());
        });
    }


    private void newTextTagEventTypeCommand() {
        mTextTagEventTypeCommand = new GeneralCommandItem("Get/Set Text Tag Event Type"
                , new SpinnerParamData<>(TextTagEventType.class));
        mTextTagEventTypeCommand.setLeftOnClickListener(view -> ((NR800) mUhf).getTextTagEventType(mTemp));
        mTextTagEventTypeCommand.setRightOnClickListener(view -> {
            SpinnerParamData textTagEventType = (SpinnerParamData) mTextTagEventTypeCommand.getViewDataArray()[0];
            ((NR800) mUhf).setTextTagEventType(mTemp, (TextTagEventType) textTagEventType.getSelected());
        });
    }

    private void newReadWriteEPCCommand() {
        mReadWriteEpcCommand = new GeneralCommandItem("Read/Write EPC"
                , new EditTextTitleParamData("Password", "00000000", "00000000")
                , new EditTextTitleParamData("EPC", "EPC"));
        mReadWriteEpcCommand.setLeftOnClickListener(v -> {
            EditTextTitleParamData firstParam = (EditTextTitleParamData) mReadWriteEpcCommand.getViewDataArray()[0];
            mUhf.readEpc(firstParam.getSelected());
        });
        mReadWriteEpcCommand.setRightOnClickListener(v -> {
            try {
                EditTextTitleParamData password = (EditTextTitleParamData) mReadWriteEpcCommand.getViewDataArray()[0];
                EditTextTitleParamData epc = (EditTextTitleParamData) mReadWriteEpcCommand.getViewDataArray()[1];
                mUhf.writeEpc(password.getSelected()
                        , GTool.hexStringToByteArray(epc.getSelected()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void newReadTagCommand() {
        mReadTagCommand = new GeneralCommandItem("Read Tag", null, "Read"
                , new EditTextTitleParamData("Password", "00000000", "00000000")
                , new EditTextTitleParamData("Selected Epc", "PC+EPC")
                , new SpinnerTitleParamData<>(MemoryBank.class)
                , new EditTextTitleParamData("Start Address", "Start with 0", "" + 0)
                , new EditTextTitleParamData("Read Length", "0 means all", "" + 0)
        );
        mReadTagCommand.setRightOnClickListener(v -> {
            EditTextTitleParamData password = (EditTextTitleParamData) mReadTagCommand.getViewDataArray()[0];
            EditTextTitleParamData selectedEpc = (EditTextTitleParamData) mReadTagCommand.getViewDataArray()[1];
            SpinnerTitleParamData memoryBand = (SpinnerTitleParamData) mReadTagCommand.getViewDataArray()[2];
            EditTextTitleParamData startAddress = (EditTextTitleParamData) mReadTagCommand.getViewDataArray()[3];
            EditTextTitleParamData readLength = (EditTextTitleParamData) mReadTagCommand.getViewDataArray()[4];
            mUhf.readTag(password.getSelected()
                    , selectedEpc.getSelected()
                    , (MemoryBank) memoryBand.getSelected()
                    , Integer.valueOf(startAddress.getSelected())
                    , Integer.valueOf(readLength.getSelected()));
        });
    }

    private void newWriteTagCommand() {
        mWriteTagCommand = new GeneralCommandItem("Write Tag", null, "Write"
                , new EditTextTitleParamData("Password", "00000000", "00000000")
                , new EditTextTitleParamData("Selected Epc", "PC+EPC")
                , new SpinnerTitleParamData<>(MemoryBank.class)
                , new EditTextTitleParamData("Start Address", "Start from 0", "" + 0)
                , new EditTextTitleParamData("Write Data", "Data to Write")
        );
        mWriteTagCommand.setRightOnClickListener(v -> {
            EditTextTitleParamData password = (EditTextTitleParamData) mWriteTagCommand.getViewDataArray()[0];
            EditTextTitleParamData selectedEpc = (EditTextTitleParamData) mWriteTagCommand.getViewDataArray()[1];
            SpinnerTitleParamData memoryBand = (SpinnerTitleParamData) mWriteTagCommand.getViewDataArray()[2];
            EditTextTitleParamData startAddress = (EditTextTitleParamData) mWriteTagCommand.getViewDataArray()[3];
            EditTextTitleParamData writeData = (EditTextTitleParamData) mWriteTagCommand.getViewDataArray()[4];
            try {
                mUhf.writeTag(password.getSelected()
                        , selectedEpc.getSelected()
                        , (MemoryBank) memoryBand.getSelected()
                        , Integer.valueOf(startAddress.getSelected())
                        , GTool.hexStringToByteArray(writeData.getSelected()));
            } catch (Exception e) {
                e.printStackTrace();
                Toaster.showToast(getContext(), "Please Input Right \"Write Data\".", Toast.LENGTH_LONG);
            }
        });
    }

    private void newInventoryRoundIntervalCommand() {
        mInventoryRoundIntervalCommand = new GeneralCommandItem("Get/Set Inventory Round Interval"
                , new SeekBarParamData(0, 255));
        mInventoryRoundIntervalCommand.setLeftOnClickListener(v -> mUhf.getInventoryRoundInterval(mTemp));
        mInventoryRoundIntervalCommand.setRightOnClickListener(v -> {
            SeekBarParamData viewData = (SeekBarParamData) mInventoryRoundIntervalCommand.getViewDataArray()[0];
            mUhf.setInventoryRoundInterval(mTemp, viewData.getSelected());
        });
    }

    private void newTagPresentedEventThresholdCommand() {
        mTagPresentedRepeatIntervalCommand = new GeneralCommandItem("Get/Set Tag Presented Repeat Interval"
                , new SeekBarParamData(0, 255));
        mTagPresentedRepeatIntervalCommand.setLeftOnClickListener(v -> mUhf.getTagPresentedRepeatInterval(mTemp));
        mTagPresentedRepeatIntervalCommand.setRightOnClickListener(v -> {
            SeekBarParamData viewData = (SeekBarParamData) mTagPresentedRepeatIntervalCommand.getViewDataArray()[0];
            mUhf.setTagPresentedRepeatInterval(mTemp, viewData.getSelected());
        });
    }

    private void newTagRemovedThresholdCommand() {
        mTagRemovedThresholdCommand = new GeneralCommandItem("Get/Set Tag Removed Threshold"
                , new SeekBarParamData(0, 255));
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
        mQCommand = new GeneralCommandItem("Get/Set Q"
                , new SeekBarParamData(0, 15));
        mQCommand.setLeftOnClickListener(v -> mUhf.getQValue(mTemp));
        mQCommand.setRightOnClickListener(v -> {
            SeekBarParamData viewData = (SeekBarParamData) mQCommand.getViewDataArray()[0];
            mUhf.setQValue(mTemp, viewData.getSelected());
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

    private void newRfPowerCommand() {
        mRfPowerCommand = new GeneralCommandItem("Get/Set RF Power"
                , new SeekBarParamData(1, 27));
        mRfPowerCommand.setLeftOnClickListener(v -> mUhf.getRfPower(mTemp));
        mRfPowerCommand.setRightOnClickListener(v -> {
            SeekBarParamData viewData = (SeekBarParamData) mRfPowerCommand.getViewDataArray()[0];
            mUhf.setRfPower(mTemp, (byte) viewData.getSelected());
        });
    }
}
