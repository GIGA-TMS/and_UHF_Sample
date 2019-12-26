package com.gigatms.uhf.deviceControl;

import android.os.Bundle;

import com.gigatms.TS100A;
import com.gigatms.uhf.GeneralCommandItem;
import com.gigatms.uhf.paramsData.SpinnerParamData;
import com.gigatms.parameters.ActiveMode;

public class TS100ADeviceControlFragment extends TS100DeviceControlFragment {

    public static TS100ADeviceControlFragment newFragment(String devMacAddress) {
        Bundle args = new Bundle();
        args.putString(MAC_ADDRESS, devMacAddress);
        TS100ADeviceControlFragment fragment = new TS100ADeviceControlFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void newInventoryActiveModeCommand() {
        mInventoryActiveMode = new GeneralCommandItem("Inventory Active Mode", "Get", "Set", new SpinnerParamData<>(ActiveMode.class));
        mInventoryActiveMode.setLeftOnClickListener(v -> ((TS100A) mUhf).getInventoryActiveMode(true));
        mInventoryActiveMode.setRightOnClickListener(v -> {
            SpinnerParamData viewData = (SpinnerParamData) mInventoryActiveMode.getViewDataArray()[0];
            ((TS100A) mUhf).setInventoryActiveMode(true, (ActiveMode) viewData.getSelected());
        });
    }
}
