package com.gigatms.uhf.paramsData;

import static com.gigatms.uhf.paramsData.ParamData.ViewType.BASE;

public class ParamData {
    private ViewType mViewType;

    ParamData(ViewType viewType) {
        mViewType = viewType;
    }

    public ParamData() {
        mViewType = BASE;
    }

    public ViewType getViewDataType() {
        return mViewType;
    }

    public enum ViewType {
        BASE,
        SPINNER,
        TWO_SPINNER,
        CHECKBOX,
        CHECKBOX_LIST,
        SEEK_BAR,
        EDIT_TEXT,


        SPINNER_WITH_TITLE,
        SEEK_BAR_WITH_TITLE,
        TWO_SPINNER_WITH_TITLE,
        EDIT_TEXT_WITH_TITLE,

    }
}