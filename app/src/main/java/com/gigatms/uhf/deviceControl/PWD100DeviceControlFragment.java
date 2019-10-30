package com.gigatms.uhf.deviceControl;

import android.os.Bundle;
import android.widget.Toast;

import com.gigatms.DecodedTagData;
import com.gigatms.PWD100;
import com.gigatms.TagInformationFormat;
import com.gigatms.UHFCallback;
import com.gigatms.uhf.DeviceControlFragment;
import com.gigatms.uhf.GeneralCommandItem;
import com.gigatms.uhf.Toaster;
import com.gigatms.uhf.paramsData.CheckboxListParamData;
import com.gigatms.uhf.paramsData.EditTextParamData;
import com.gigatms.uhf.paramsData.EditTextTitleParamData;
import com.gigatms.uhf.paramsData.SeekBarParamData;
import com.gigatms.uhf.paramsData.SpinnerParamData;
import com.gigatms.uhf.paramsData.SpinnerTitleParamData;
import com.gigatms.uhf.paramsData.TwoSpinnerParamData;
import com.gigatms.uhf.paramsData.TwoSpinnerTitleParamData;
import com.gigatms.parameters.ActiveMode;
import com.gigatms.parameters.IONumber;
import com.gigatms.parameters.IOState;
import com.gigatms.parameters.InventoryOption;
import com.gigatms.parameters.MemoryBank;
import com.gigatms.parameters.RfSensitivityLevel;
import com.gigatms.parameters.ScanMode;
import com.gigatms.parameters.SelectInfo;
import com.gigatms.parameters.SelectInfo.Action;
import com.gigatms.parameters.Session;
import com.gigatms.parameters.State;
import com.gigatms.parameters.TagPresentedType;
import com.gigatms.parameters.Target;
import com.gigatms.parameters.TriggerType;
import com.gigatms.tools.GTool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.gigatms.parameters.ActiveMode.COMMAND;
import static com.gigatms.parameters.ActiveMode.READ;
import static com.gigatms.parameters.Session.SL;

public class PWD100DeviceControlFragment extends DeviceControlFragment {
    private static final String TAG = PWD100DeviceControlFragment.class.getSimpleName();

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

    private GeneralCommandItem mSsidPasswordCommand;
    private GeneralCommandItem mInventoryOptionCommand;
    private GeneralCommandItem mNewSearchingTagConditionCommand;
    private GeneralCommandItem mAppendSearchingTagConditionCommand;
    private GeneralCommandItem mTriggerCommand;
    private GeneralCommandItem mScanModeCommand;
    private GeneralCommandItem mCommandTrigger;

    public static PWD100DeviceControlFragment newFragment(String devMacAddress) {
        Bundle args = new Bundle();
        args.putString(MAC_ADDRESS, devMacAddress);
        PWD100DeviceControlFragment fragment = new PWD100DeviceControlFragment();
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
            public void didReadTag(MemoryBank memoryBank, int startWordAddress, byte[] readData) {
                final String dataString = GTool.bytesToHexString(readData);
                //TODO
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
            public void didGetInventoryOption(InventoryOption inventoryOption) {
                SpinnerParamData selected1 = (SpinnerParamData) mInventoryOptionCommand.getViewDataArray()[0];
                selected1.setSelected(inventoryOption);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mInventoryOptionCommand.getPosition()));
                onUpdateLog(TAG, "didGetInventoryOption: " + inventoryOption);
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
                mAdapter.notifyItemChanged(mTagPresentedRepeatIntervalCommand.getPosition());
                if (hundredMilliSeconds == 255 || hundredMilliSeconds == 0) {
                    onUpdateLog(TAG, "didGetTagPresentedRepeatInterval[Default]: Immediately");
                } else {
                    onUpdateLog(TAG, "didGetTagPresentedRepeatInterval[:" + hundredMilliSeconds + "*100 ms");
                }
            }

            @Override
            public void didGetTagRemovedThreshold(int inventoryRound) {
                SeekBarParamData selected = (SeekBarParamData) mTagRemovedThresholdCommand.getViewDataArray()[0];
                selected.setSelected(inventoryRound);
                mAdapter.notifyItemChanged(mTagRemovedThresholdCommand.getPosition());
                if (inventoryRound == 255 || inventoryRound == 5) {
                    onUpdateLog(TAG, "didGetTagRemovedThreshold[Default]: " + 5 + " inventory rounds.");
                } else {
                    onUpdateLog(TAG, "didGetTagRemovedThreshold: " + inventoryRound + " inventory rounds.");
                }
            }

            @Override
            public void didGetInventoryRoundInterval(int tenMilliSeconds) {
                SeekBarParamData selected = (SeekBarParamData) mInventoryRoundIntervalCommand.getViewDataArray()[0];
                selected.setSelected(tenMilliSeconds);
                mAdapter.notifyItemChanged(mInventoryRoundIntervalCommand.getPosition());
                if (tenMilliSeconds == 0 || tenMilliSeconds == 255) {
                    onUpdateLog(TAG, "didGetInventoryRoundInterval[Default]: " + 0 + "*10 ms");
                } else {
                    onUpdateLog(TAG, "didGetInventoryRoundInterval: " + tenMilliSeconds + "*10 ms");
                }
            }

            @Override
            public void didGetScanMode(ScanMode scanMode) {
                SpinnerParamData selected1 = (SpinnerParamData) mScanModeCommand.getViewDataArray()[0];
                selected1.setSelected(scanMode);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mScanModeCommand.getPosition()));
                onUpdateLog(TAG, "didGetScanMode: " + scanMode.name());
            }

            @Override
            public void didGetTriggerType(final Set<TriggerType> triggerSource) {
                CheckboxListParamData selected1 = (CheckboxListParamData) mTriggerCommand.getViewDataArray()[0];
                selected1.setSelected(triggerSource);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mTriggerCommand.getPosition()));
                onUpdateLog(TAG, "didGetTriggerType: " + Arrays.toString(triggerSource.toArray()));
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
        newInventoryActiveModeCommand();
    }

    @Override
    protected void onNewAdvanceCommands() {
        newCommandTrigger();
        newPWD100ScanModeCommand();
        newPWD100TriggerCommand();
        newPWD100SsidPasswordCommand();
        newPWD100InventoryOption();
        newPWD100NewSearchingTagCondition();
        newPWD100AppendSearchingTagCondition();
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
        mAdapter.add(mInventoryCommand);
        mAdapter.add(mInventoryActiveMode);
    }

    @Override
    protected void onShowAdvanceViews() {
        mAdapter.add(mScanModeCommand);
        mAdapter.add(mTriggerCommand);
        mAdapter.add(mCommandTrigger);
        mAdapter.add(mSsidPasswordCommand);
        mAdapter.add(mInventoryOptionCommand);
        mAdapter.add(mNewSearchingTagConditionCommand);
        mAdapter.add(mAppendSearchingTagConditionCommand);
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

    private void newStartInventoryCommand() {
        mInventoryCommand = new GeneralCommandItem("Start Inventory", null, "Start", new SpinnerParamData<>(TagPresentedType.class));
        mInventoryCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mInventoryCommand.getViewDataArray()[0];
            mUhf.startInventory((TagPresentedType) viewData.getSelected());
        });
    }

    private void newInventoryActiveModeCommand() {
        mInventoryActiveMode = new GeneralCommandItem("Inventory Active Mode", "Set", "Get", new SpinnerParamData<>(new ActiveMode[]{READ, COMMAND}));
        mInventoryActiveMode.setLeftOnClickListener(v -> mUhf.getInventoryActiveMode());
        mInventoryActiveMode.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mInventoryActiveMode.getViewDataArray()[0];
            mUhf.setInventoryActiveMode((ActiveMode) viewData.getSelected());
        });
    }


    private void newPWD100SsidPasswordCommand() {
        mSsidPasswordCommand = new GeneralCommandItem("Set WiFi Settings", null, "Set"
                , new EditTextTitleParamData("SSID", "SSID of station mode")
                , new EditTextTitleParamData("Password", "Password of station mode"));
        mSsidPasswordCommand.setRightOnClickListener(v -> {
            EditTextTitleParamData viewData0 = (EditTextTitleParamData) mSsidPasswordCommand.getViewDataArray()[0];
            EditTextTitleParamData viewData1 = (EditTextTitleParamData) mSsidPasswordCommand.getViewDataArray()[1];
            ((PWD100) mUhf).setWifiSettings(viewData0.getSelected(), viewData1.getSelected());
        });
    }

    private void newPWD100InventoryOption() {
        mInventoryOptionCommand = new GeneralCommandItem("Get/Set Inventory Option", new SpinnerParamData<>(InventoryOption.class));
        mInventoryOptionCommand.setLeftOnClickListener(v -> ((PWD100) mUhf).getInventoryOption());
        mInventoryOptionCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mInventoryOptionCommand.getViewDataArray()[0];
            ((PWD100) mUhf).setInventoryOption((InventoryOption) viewData.getSelected());
        });
    }

    private void newPWD100NewSearchingTagCondition() {
        TwoSpinnerTitleParamData<Session, Target> sessionTargetTwoSpinnerTitleParamData = new TwoSpinnerTitleParamData<>(Session.values(), Target.getAbTargets());
        mNewSearchingTagConditionCommand = new GeneralCommandItem("New Searching and Filtering Tag Condition", null, "new"
                , sessionTargetTwoSpinnerTitleParamData
                , new SpinnerTitleParamData<>(Action.class)
                , new SpinnerTitleParamData<>(MemoryBank.class)
                , new EditTextTitleParamData("Start Pointer", "Start from 0", "" + 0)
                , new EditTextTitleParamData("Mask Data", "Data to Search"));
        mNewSearchingTagConditionCommand.setRightOnClickListener(v -> {
            try {
                Session session = (Session) ((TwoSpinnerTitleParamData) mNewSearchingTagConditionCommand.getViewDataArray()[0]).getFirstSelected();
                Target target = (Target) ((TwoSpinnerTitleParamData) mNewSearchingTagConditionCommand.getViewDataArray()[0]).getSecondSelected();
                Action action = (Action) ((SpinnerTitleParamData) mNewSearchingTagConditionCommand.getViewDataArray()[1]).getSelected();
                MemoryBank memoryBank = (MemoryBank) ((SpinnerTitleParamData) mNewSearchingTagConditionCommand.getViewDataArray()[2]).getSelected();
                int startPointer = Integer.valueOf(((EditTextTitleParamData) mNewSearchingTagConditionCommand.getViewDataArray()[3]).getSelected());
                String mMaskData = ((EditTextTitleParamData) mNewSearchingTagConditionCommand.getViewDataArray()[4]).getSelected();
                SelectInfo selectInfo = new SelectInfo(action, memoryBank, startPointer, GTool.hexStringToByteArray(mMaskData));
                List<SelectInfo> selectInfoList = new ArrayList<>();
                selectInfoList.add(selectInfo);
                ((PWD100) mUhf).newSearchOrFilterTagCondition(session, target, selectInfoList);
            } catch (Exception e) {
                Toaster.showToast(getContext(), "Please make sure the mask data is filled!", Toast.LENGTH_LONG);
            }
        });
        sessionTargetTwoSpinnerTitleParamData.setOnFirstItemSelected(selected -> {
            if (selected.equals(SL) && !Target.slContains((Target) sessionTargetTwoSpinnerTitleParamData.getSecondEnums()[0])) {
                sessionTargetTwoSpinnerTitleParamData.setSecondEnums(Target.getSlTargets());
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mNewSearchingTagConditionCommand.getPosition()));
            } else if ((!selected.equals(SL)) && !Target.abContains((Target) sessionTargetTwoSpinnerTitleParamData.getSecondEnums()[0])) {
                sessionTargetTwoSpinnerTitleParamData.setSecondEnums(Target.getAbTargets());
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mNewSearchingTagConditionCommand.getPosition()));
            }
        });
    }

    private void newPWD100AppendSearchingTagCondition() {
        mAppendSearchingTagConditionCommand = new GeneralCommandItem("Append Searching and Filtering Tag Condition", null, "append"
                , new SpinnerTitleParamData<>(Action.class)
                , new SpinnerTitleParamData<>(MemoryBank.class)
                , new EditTextTitleParamData("Start Pointer", "Start from 0", "" + 0)
                , new EditTextTitleParamData("Mask Data", "Data to Search"));
        mAppendSearchingTagConditionCommand.setRightOnClickListener(v -> {
            try {
                Action action = (Action) ((SpinnerTitleParamData) mAppendSearchingTagConditionCommand.getViewDataArray()[0]).getSelected();
                MemoryBank memoryBank = (MemoryBank) ((SpinnerTitleParamData) mAppendSearchingTagConditionCommand.getViewDataArray()[1]).getSelected();
                int startPointer = Integer.valueOf(((EditTextTitleParamData) mAppendSearchingTagConditionCommand.getViewDataArray()[2]).getSelected());
                String mMaskData = ((EditTextTitleParamData) mAppendSearchingTagConditionCommand.getViewDataArray()[3]).getSelected();
                SelectInfo selectInfo = new SelectInfo(action, memoryBank, startPointer, GTool.hexStringToByteArray(mMaskData));
                List<SelectInfo> selectInfoList = new ArrayList<>();
                selectInfoList.add(selectInfo);
                ((PWD100) mUhf).appendSearchOrFilterTagCondition(selectInfoList);
            } catch (Exception e) {
                Toaster.showToast(getContext(), "Please make sure the mask data is filled!", Toast.LENGTH_LONG);
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

    private void newPWD100TriggerCommand() {
        mTriggerCommand = new GeneralCommandItem("Get/Set Trigger"
                , new CheckboxListParamData<>(EnumSet.of(TriggerType.COMMAND, TriggerType.DIGITAL_INPUT)));
        mTriggerCommand.setLeftOnClickListener(v -> ((PWD100) mUhf).getTriggerType(mTemp));
        mTriggerCommand.setRightOnClickListener(v -> {
            CheckboxListParamData viewData = (CheckboxListParamData) mTriggerCommand.getViewDataArray()[0];
            ((PWD100) mUhf).setTriggerType(mTemp, viewData.getSelected());
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
            mUhf.setQValue(mTemp, viewData.getSelected());
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

    private void newPWD100ScanModeCommand() {
        mScanModeCommand = new GeneralCommandItem("Get/Set Scan Mode"
                , new SpinnerParamData<>(ScanMode.class));
        mScanModeCommand.setRightOnClickListener(v -> {
            SpinnerParamData scanMode = (SpinnerParamData) mScanModeCommand.getViewDataArray()[0];
            ((PWD100) mUhf).setScanMode(mTemp, (ScanMode) scanMode.getSelected());
        });
        mScanModeCommand.setLeftOnClickListener(v -> ((PWD100) mUhf).getScanMode(mTemp));
    }

    private void newCommandTrigger() {
        mCommandTrigger = new GeneralCommandItem("Get/Set Command Trigger", new SpinnerParamData<>(State.class));
        mCommandTrigger.setRightOnClickListener(v -> {
            SpinnerParamData state = (SpinnerParamData) mCommandTrigger.getViewDataArray()[0];
            ((PWD100) mUhf).setCommandTriggerState((State) state.getSelected());
        });
        mCommandTrigger.setLeftOnClickListener(v -> ((PWD100) mUhf).getCommandTriggerState());
    }
}
