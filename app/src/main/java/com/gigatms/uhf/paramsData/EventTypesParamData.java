package com.gigatms.uhf.paramsData;

import java.util.HashSet;
import java.util.Set;

import static com.gigatms.uhf.paramsData.ParamData.ViewType.TS100_EVENT_TYPES_PARAM;

public class EventTypesParamData extends ParamData {
    private String[] mFirstChoices;
    private String[] mMiddleChoices;
    private String[] mLastChoices;
    private String mFirstSelect;
    private String mMiddleSelect;
    private Set<String> mLastSelect;
    private OnFirstItemSelected mOnFirstItemSelected;

    public interface OnFirstItemSelected {
        void onFirstItemSelected(String selected);
    }

    public EventTypesParamData(String[] firstChoices, String[] middleChoices, String[] lastChoices) {
        super(TS100_EVENT_TYPES_PARAM);
        mFirstChoices = firstChoices;
        mMiddleChoices = middleChoices;
        mLastChoices = lastChoices;
        mFirstSelect = firstChoices[0];
        if (middleChoices != null && middleChoices.length > 0) {
            mMiddleSelect = middleChoices[0];
        }
        mLastSelect = new HashSet<>();
    }

    public String[] getFirstChoices() {
        return mFirstChoices;
    }

    public String[] getLastChoices() {
        return mLastChoices;
    }

    public void setLastChoices(String[] lastChoices) {
        mLastChoices = lastChoices;
    }

    public String getFirstSelect() {
        return mFirstSelect;
    }

    public void setFirstSelect(String firstSelect) {
        mFirstSelect = firstSelect;
        if (mOnFirstItemSelected != null) {
            mOnFirstItemSelected.onFirstItemSelected(mFirstSelect);
        }
    }

    public String[] getMiddleChoices() {
        return mMiddleChoices;
    }

    public void setMiddleChoices(String[] middleChoices) {
        mMiddleChoices = middleChoices;
    }

    public String getMiddleSelect() {
        return mMiddleSelect;
    }

    public void setMiddleSelect(String middleSelect) {
        mMiddleSelect = middleSelect;
    }

    public Set<String> getLastSelect() {
        return mLastSelect;
    }

    public void setLastSelect(Set<String> lastSelect) {
        mLastSelect = lastSelect;
    }

    public void setOnFirstItemSelected(OnFirstItemSelected onFirstItemSelected) {
        mOnFirstItemSelected = onFirstItemSelected;
    }
}
