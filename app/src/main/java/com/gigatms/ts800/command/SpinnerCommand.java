package com.gigatms.ts800.command;

import static com.gigatms.ts800.CommandRecyclerViewAdapter.SPINNER;

public class SpinnerCommand<E extends Enum<E>> extends Command {
    private OnGetValue mOnGetValue;
    private Class<E> mEnum;
    private Enum mSelected;

    public SpinnerCommand(String title, Class<E> enumData) {
        super(title);
        mEnum = enumData;
    }

    public SpinnerCommand(String title, String leftBtnName, String rightBtnName, Class<E> enumData) {
        super(title, leftBtnName, rightBtnName);
        mEnum = enumData;
    }

    public interface OnGetValue {
        void onGetValue(Enum value);
    }

    @Override
    public int getViewType() {
        return SPINNER;
    }

    public void setOnGetValue(OnGetValue onGetValue) {
        mOnGetValue = onGetValue;
    }

    public Class<E> getEnum() {
        return mEnum;
    }

    public void setSelected(Enum selected) {
        mSelected = selected;
    }

    public Enum getSelected() {
        return mSelected;
    }

    public void didGetValue(Enum value) {
        mOnGetValue.onGetValue(value);
    }

}
