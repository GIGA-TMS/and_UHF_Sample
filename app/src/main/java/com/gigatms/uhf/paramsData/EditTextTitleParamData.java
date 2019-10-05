package com.gigatms.uhf.paramsData;

public class EditTextTitleParamData extends ParamData {
    private String mTitle;
    private String mHint;
    private String mSelected;

    public EditTextTitleParamData(String title, String hint) {
        super(ViewType.EDIT_TEXT_WITH_TITLE);
        mHint = hint;
        mSelected = "";
        mTitle = title;
    }

    public EditTextTitleParamData(String title, String hint, String defaultData) {
        super(ViewType.EDIT_TEXT_WITH_TITLE);
        mHint = hint;
        mSelected = defaultData;
        mTitle = title;
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

    public String getTitle() {
        return mTitle;
    }
}
