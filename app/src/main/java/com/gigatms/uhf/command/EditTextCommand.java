package com.gigatms.uhf.command;

import static com.gigatms.uhf.CommandRecyclerViewAdapter.EDIT_TEXT;

public class EditTextCommand extends Command {
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

    @Override
    public int getViewType() {
        return EDIT_TEXT;
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

}
