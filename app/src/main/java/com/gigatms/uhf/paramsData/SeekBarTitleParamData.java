package com.gigatms.uhf.paramsData;

import static com.gigatms.uhf.paramsData.ParamData.ViewType.SEEK_BAR_WITH_TITLE;

public class SeekBarTitleParamData extends ParamData {
    private String mTitle;
    private int mMinValue;
    private int mMaxValue;
    private int mSelected;

    public SeekBarTitleParamData(String title, int minValue, int maxValue) {
        super(SEEK_BAR_WITH_TITLE);

        mTitle = title;
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

    public String getTitle() {
        return mTitle;
    }
}
