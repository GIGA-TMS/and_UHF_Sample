package com.gigatms.ts800.command;

import static com.gigatms.ts800.CommandRecyclerViewAdapter.EDIT_TEXT;

public class EditTextCommand extends Command {
    private OnGetValue mOnGetValue;
    private String mHint;
    private String mSelected;

    public EditTextCommand(String title, String hint) {
        super(title);
        mHint = hint;
    }

    public EditTextCommand(String title, String rightBtnName, String leftBtnName, String hint) {
        super(title, rightBtnName, leftBtnName);
        mHint = hint;
    }

    public interface OnGetValue {
        void onGetValue(String value);
    }

    public String getHint() {
        return mHint;
    }

    public String getSelected() {
        return mSelected;
    }

    public void setSelected(String selected) {
        mSelected = selected;
    }

    public void setOnGetValue(OnGetValue onGetValue) {
        mOnGetValue = onGetValue;
    }

    @Override
    public int getViewType() {
        return EDIT_TEXT;
    }

    public void didGetVale(String value) {
        mSelected = value;
        mOnGetValue.onGetValue(value);
    }
}
