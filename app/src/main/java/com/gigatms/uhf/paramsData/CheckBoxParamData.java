package com.gigatms.uhf.paramsData;

public class CheckBoxParamData extends ParamData {
    private String mTitle;
    private boolean mChecked;

    public CheckBoxParamData(String title) {
        super(ViewType.CHECKBOX);
        mTitle = title;
    }

    public boolean isChecked() {
        return mChecked;
    }

    public void setChecked(boolean checked) {
        this.mChecked = checked;
    }

    public String getTitle() {
        return mTitle;
    }
}
