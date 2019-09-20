package com.gigatms.uhf.paramsData;

public class EditTextParamData extends ParamData {
    private String mHint;
    private String mSelected;

    public EditTextParamData(String hint) {
        super(ViewType.EDIT_TEXT);
        mHint = hint;
        mSelected = "";
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
