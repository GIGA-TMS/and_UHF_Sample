package com.gigatms.ts800.command;

import static com.gigatms.ts800.CommandRecyclerViewAdapter.SEEK_BAR;

public class SeekBarCommand extends Command {
    private OnGetValues mOnGetValue;
    private int mMinValue;
    private int mMaxValue;
    private int mSelected;

    public SeekBarCommand( String title, int minValue, int maxValue) {
        super( title);
        mMaxValue = maxValue;
        mMinValue = minValue;
    }

    public SeekBarCommand(String title, String rightBtnName, String leftBtnName, int minValue, int maxValue) {
        super(title, rightBtnName, leftBtnName);
        mMaxValue = maxValue;
        mMinValue = minValue;
    }


    public interface OnGetValues {
        void onGetValue(int value);
    }

    public int getViewType() {
        return SEEK_BAR;
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

    public void didGetVale(int value) {
        mOnGetValue.onGetValue(value);
    }

    public void setOnGetValue(OnGetValues onGetValue) {
        mOnGetValue = onGetValue;
    }
}
