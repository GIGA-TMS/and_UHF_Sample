package com.gigatms.uhf.deviceControl;

import android.os.Bundle;
import android.widget.Toast;

import com.gigatms.DecodedTagData;
import com.gigatms.MU400H;
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
import com.gigatms.tools.GTool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.gigatms.parameters.OutputInterface.DEFAULT;
import static com.gigatms.parameters.OutputInterface.HID_KEYBOARD;
import static com.gigatms.parameters.Session.SL;

public class MU400HDeviceControlFragment extends DeviceControlFragment {
    private static final String TAG = MU400HDeviceControlFragment.class.getSimpleName();

    private GeneralCommandItem mStopInventoryCommand;
    private GeneralCommandItem mInventoryCommand;
    private GeneralCommandItem mInventoryExCommand;

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

    private GeneralCommandItem mEventTypeCommand;
    private GeneralCommandItem mFilterCommand;
    private GeneralCommandItem mPostDataDelimiterCommand;
    private GeneralCommandItem mMemoryBankSelectionCommand;
    private GeneralCommandItem mOutputInterfaceCommand;

    public static MU400HDeviceControlFragment newFragment(String devMacAddress) {
        Bundle args = new Bundle();
        args.putString(MAC_ADDRESS, devMacAddress);
        MU400HDeviceControlFragment fragment = new MU400HDeviceControlFragment();
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
            public void didGetEventType(final EventType eventType) {
                SpinnerParamData selected = (SpinnerParamData) mEventTypeCommand.getViewDataArray()[0];
                selected.setSelected(eventType);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mEventTypeCommand.getPosition()));
                onUpdateLog(TAG, "didGetEventType: " + eventType.name());
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
                onUpdateLog(TAG, "didGetTagPresentedRepeatInterval: " + hundredMilliSeconds + "*100 ms");
            }

            @Override
            public void didGetTagRemovedThreshold(int hundredMilliSeconds) {
                SeekBarParamData selected1 = (SeekBarParamData) mTagRemovedThresholdCommand.getViewDataArray()[0];
                selected1.setSelected(hundredMilliSeconds);
                mAdapter.notifyItemChanged(mTagRemovedThresholdCommand.getPosition());
                onUpdateLog(TAG, "didGetTagRemovedThreshold: " + hundredMilliSeconds + "*100 ms");
            }

            @Override
            public void didGetInventoryRoundInterval(int tenMilliSeconds) {
                SeekBarParamData selected1 = (SeekBarParamData) mInventoryRoundIntervalCommand.getViewDataArray()[0];
                selected1.setSelected(tenMilliSeconds);
                mAdapter.notifyItemChanged(mInventoryRoundIntervalCommand.getPosition());
                onUpdateLog(TAG, "didGetInventoryRoundInterval: " + tenMilliSeconds + "*10 ms");
            }

            @Override
            public void didGetOutputInterface(OutputInterface outputInterface) {
                SpinnerParamData selected1 = (SpinnerParamData) mOutputInterfaceCommand.getViewDataArray()[0];
                selected1.setSelected(outputInterface);
                mRecyclerView.post(() -> mAdapter.notifyItemChanged(mOutputInterfaceCommand.getPosition()));
                onUpdateLog(TAG, "didGetOutputInterface: " + outputInterface.name());
            }
        };

    }

    @Override
    protected void onNewInventoryCommands() {
        newStopInventoryCommand();
        newStartInventoryCommand();
        newStartInventoryCommandEx();
    }

    @Override
    protected void onNewAdvanceCommands() {
        newEventTypeCommand();
        newEnableFilterCommand();
        newPostDataDelimiterCommand();
        newMemoryBankSelectionCommand();
        newOutputInterfaceCommand();
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
        mAdapter.add(mInventoryCommand);
        mAdapter.add(mInventoryExCommand);
    }

    @Override
    protected void onShowAdvanceViews() {
        mAdapter.add(mEventTypeCommand);
        mAdapter.add(mFilterCommand);
        mAdapter.add(mPostDataDelimiterCommand);
        mAdapter.add(mMemoryBankSelectionCommand);
        mAdapter.add(mOutputInterfaceCommand);
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

    private void newStartInventoryCommandEx() {
        mInventoryExCommand = new GeneralCommandItem("Start Inventory Ex", null, "Start Ex"
                , new CheckboxListParamData<>(TagDataEncodeType.class));
        mInventoryExCommand.setRightOnClickListener(v -> {
            CheckboxListParamData viewData = (CheckboxListParamData) mInventoryExCommand.getViewDataArray()[0];
            ((MU400H) mUhf).startInventoryEx(viewData.getSelected());
        });
    }

    private void newStartInventoryCommand() {
        mInventoryCommand = new GeneralCommandItem("Start Inventory", null, "Start"
                , new SpinnerParamData<>(TagPresentedType.class));
        mInventoryCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mInventoryCommand.getViewDataArray()[0];
            mUhf.startInventory((TagPresentedType) viewData.getSelected());
        });
    }

    private void newStopInventoryCommand() {
        mStopInventoryCommand = new GeneralCommandItem("Stop Inventory", null, "Stop");
        mStopInventoryCommand.setRightOnClickListener(v -> mUhf.stopInventory());
    }

    private void newEventTypeCommand() {
        mEventTypeCommand = new GeneralCommandItem("Get/Set Event Type"
                , new SpinnerParamData<>(EventType.class));
        mEventTypeCommand.setLeftOnClickListener(v -> {
            ((MU400H) mUhf).getEventType(mTemp);
        });
        mEventTypeCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mEventTypeCommand.getViewDataArray()[0];
            ((MU400H) mUhf).setEventType(mTemp, (EventType) viewData.getSelected());
        });
    }

    private void newEnableFilterCommand() {
        mFilterCommand = new GeneralCommandItem("Get/Set Filter",
                new CheckboxListParamData<>(TagDataEncodeType.class));
        mFilterCommand.setRightOnClickListener(v -> {
            CheckboxListParamData viewData = (CheckboxListParamData) mFilterCommand.getViewDataArray()[0];
            ((MU400H) mUhf).setFilter(mTemp, viewData.getSelected());
        });
        mFilterCommand.setLeftOnClickListener(v -> {
            ((MU400H) mUhf).getFilter(mTemp);
        });
    }

    private void newPostDataDelimiterCommand() {
        mPostDataDelimiterCommand = new GeneralCommandItem("Get/Set Post Data Delimiter"
                , new SpinnerParamData<>(PostDataDelimiter.class));
        mPostDataDelimiterCommand.setLeftOnClickListener(v -> {
            ((MU400H) mUhf).getPostDataDelimiter(mTemp);
        });
        mPostDataDelimiterCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mPostDataDelimiterCommand.getViewDataArray()[0];
            ((MU400H) mUhf).setPostDataDelimiter(mTemp, (PostDataDelimiter) viewData.getSelected());
        });
    }

    private void newMemoryBankSelectionCommand() {
        mMemoryBankSelectionCommand = new GeneralCommandItem("Get/Set Memory Bank Selection"
                , new SpinnerParamData<>(MemoryBankSelection.class));
        mMemoryBankSelectionCommand.setLeftOnClickListener(v -> {
            ((MU400H) mUhf).getMemoryBankSelection(mTemp);
        });

        mMemoryBankSelectionCommand.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mMemoryBankSelectionCommand.getViewDataArray()[0];
            ((MU400H) mUhf).setMemoryBankSelection(mTemp, (MemoryBankSelection) viewData.getSelected());
        });
    }

    private void newOutputInterfaceCommand() {
        mOutputInterfaceCommand = new GeneralCommandItem("Get/Set Output Interface"
                , new SpinnerParamData<>(new OutputInterface[]{DEFAULT, HID_KEYBOARD}));
        mOutputInterfaceCommand.setLeftOnClickListener(v -> ((MU400H) mUhf).getOutputInterface(mTemp));
        mOutputInterfaceCommand.setRightOnClickListener(v -> {
            SpinnerParamData outputInterface = (SpinnerParamData) mOutputInterfaceCommand.getViewDataArray()[0];
            ((MU400H) mUhf).setOutputInterface(mTemp, (OutputInterface) outputInterface.getSelected());
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

}
