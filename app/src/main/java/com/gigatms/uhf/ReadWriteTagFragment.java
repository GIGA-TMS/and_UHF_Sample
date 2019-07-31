package com.gigatms.uhf;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.gigatms.UHFDevice;
import com.gigatms.parameters.MemoryBank;
import com.gigatms.tools.GLog;
import com.gigatms.tools.GTool;
import com.squareup.leakcanary.RefWatcher;

import static com.gigatms.uhf.DeviceControlFragment.MAC_ADDRESS;

public class ReadWriteTagFragment extends DebugFragment {
    private static final String TAG = ReadWriteTagFragment.class.getSimpleName();
    private UHFDevice mUhf;
    private Button mBtnReadEpc;
    private Button mBtnWriteEpc;
    private EditText mEtEpc;
    private EditText mEtPassword;
    private Button mBtnReadTag;
    private Button mBtnWriteTag;
    private EditText mEtTagPassword;
    private EditText mEtMemoryBank;
    private EditText mEtStartWordPosition;
    private EditText mEtData;
    private EditText mEtSelectedEpc;
    private EditText mEtReadLength;

    public static ReadWriteTagFragment newFragment(String devMacAddress) {
        Bundle args = new Bundle();
        args.putString(MAC_ADDRESS, devMacAddress);
        ReadWriteTagFragment fragment = new ReadWriteTagFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_read_write_tag, container, false);
        findViews(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        initUhfDevice();
        setViews();
    }

    private void setViews() {
        setReadWriteEpcViews();
        setReadWriteTagViews();
    }

    private void setReadWriteTagViews() {
        mBtnReadTag.setOnClickListener(v -> {
            try {
                mUhf.readTag(mEtTagPassword.getText().toString()
                        , mEtSelectedEpc.getText().toString()
                        , MemoryBank.getMemoryBank(Byte.parseByte(mEtMemoryBank.getText().toString()))
                        , Integer.parseInt(mEtStartWordPosition.getText().toString())
                        , Integer.parseInt(mEtReadLength.getText().toString()));
            } catch (Exception e) {
                e.printStackTrace();
                Toaster.showToast(getContext(), "Please make sure every required field is filled!", Toast.LENGTH_LONG);
            }
        });
        mBtnWriteTag.setOnClickListener(v -> {
            try {
                mUhf.writeTag(mEtTagPassword.getText().toString()
                        , mEtSelectedEpc.getText().toString()
                        , MemoryBank.getMemoryBank(Byte.parseByte(mEtMemoryBank.getText().toString()))
                        , Integer.parseInt(mEtStartWordPosition.getText().toString())
                        , GTool.hexStringToByteArray(mEtData.getText().toString()));
            } catch (Exception e) {
                e.printStackTrace();
                Toaster.showToast(getContext(), "Please make sure every required field is filled!", Toast.LENGTH_LONG);
            }
        });
    }

    private void setReadWriteEpcViews() {
        mBtnReadEpc.setOnClickListener(v -> {
            String psw = mEtPassword.getText().toString();
            mUhf.readEpc(psw);
        });
        mBtnWriteEpc.setOnClickListener(v -> {
            try {
                String psw = mEtPassword.getText().toString();
                String epc = mEtEpc.getText().toString().trim();
                Log.d(TAG, "onClick: " + epc);
                byte[] epcByte = GTool.hexStringToByteArray(epc);
                mUhf.writeEpc(psw, epcByte);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void findViews(View view) {
        findReadWriteEpc(view);
        findReadWriteTag(view);
    }

    private void findReadWriteTag(View view) {
        mBtnReadTag = view.findViewById(R.id.btn_read_tag);
        mBtnWriteTag = view.findViewById(R.id.btn_write_tag);
        mEtTagPassword = view.findViewById(R.id.et_tag_password);
        mEtMemoryBank = view.findViewById(R.id.et_memory_bank);
        mEtStartWordPosition = view.findViewById(R.id.et_start_word_position);
        mEtData = view.findViewById(R.id.et_data);
        mEtSelectedEpc = view.findViewById(R.id.et_selected_epc);
        mEtReadLength = view.findViewById(R.id.et_read_length);
    }

    private void findReadWriteEpc(View view) {
        mBtnReadEpc = view.findViewById(R.id.btn_read_ecp);
        mBtnWriteEpc = view.findViewById(R.id.btn_write_ecp);
        mEtEpc = view.findViewById(R.id.et_epc);
        mEtPassword = view.findViewById(R.id.et_epc_password);
    }

    private void initUhfDevice() {
        assert getArguments() != null;
        String devMacAddr = getArguments().getString(MAC_ADDRESS);
        UHFDevice uhf = (UHFDevice) ConnectedDevices.getInstance().get(devMacAddr);
        if (uhf != null) {
            this.mUhf = uhf;
        }
    }

    public void setEpc(final byte[] epc) {
        mEtEpc.post(() -> mEtEpc.setText(GTool.bytesToHexString(epc)));
    }

    public void setTagData(final MemoryBank memoryBank, final int startWordAddress, final byte[] readData) {
        mEtData.post(() -> mEtData.setText(GTool.bytesToHexString(readData)));
        mEtStartWordPosition.post(() -> mEtStartWordPosition.setText(String.valueOf(startWordAddress)));
        mEtMemoryBank.post(() -> mEtMemoryBank.setText(String.valueOf(memoryBank.getValue())));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RefWatcher refWatcher = LeakWatcherApplication.getRefWatcher(getActivity());
        refWatcher.watch(this);
        GLog.d(TAG, "onDestroy");
    }
}
