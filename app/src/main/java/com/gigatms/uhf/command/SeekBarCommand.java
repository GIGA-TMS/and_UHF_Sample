package com.gigatms.uhf.command;

import static com.gigatms.uhf.CommandRecyclerViewAdapter.SEEK_BAR;

public class SeekBarCommand extends Command {
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
}
