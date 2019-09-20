package com.gigatms.uhf.paramsData;

import static com.gigatms.uhf.paramsData.ParamData.ViewType.SEEK_BAR;

public class SeekBarParamData extends ParamData {
    private int mMinValue;
    private int mMaxValue;
    private int mSelected;

    public SeekBarParamData(int minValue, int maxValue) {
        super(SEEK_BAR);
        mMaxValue = maxValue;
        mMinValue = minValue;
        mSelected = mMinValue;
    }

    public int getMaxValue() {
        return mMaxValue;
    }

    public int getMinValue() {
        return mMinValue;
    }

    public int getSelected() {
        return mSelected;
    }

    public void setSelected(int selected) {
        mSelected = selected;
    }
}
