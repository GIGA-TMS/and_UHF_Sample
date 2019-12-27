package com.gigatms.uhf.paramsData;

import com.gigatms.tools.GLog;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static com.gigatms.uhf.paramsData.ParamData.ViewType.CHECKBOX_LIST;

public class CheckboxListParamData<E extends Enum<E>> extends ParamData {
    private static final String TAG = CheckboxListParamData.class.getSimpleName();
    private Set<E> mData;
    private Set<Integer> mSelectedOrdinal;

    public CheckboxListParamData(Class<E> enumData) {
        super(CHECKBOX_LIST);
        mData = EnumSet.allOf(enumData);
        mSelectedOrdinal = new HashSet<>();
    }

    public CheckboxListParamData(Set<E> enumData) {
        super(CHECKBOX_LIST);
        mData = enumData;
        mSelectedOrdinal = new HashSet<>();
    }

    public Set<E> getDataSet() {
        return mData;
    }

    public void setSelected(Set<E> set) {
        mSelectedOrdinal = new HashSet<>();
        for (E e : set) {
            GLog.d(TAG, e.name() + " ordinal " + e.ordinal());
            mSelectedOrdinal.add(e.ordinal());
        }
    }

    public Set<E> getSelected() {
        Set<E> set = new HashSet<>();
        for (E e : mData) {
            if (mSelectedOrdinal.contains(e.ordinal())) {
                set.add(e);
            }
        }
        return set;
    }

    public Set<Integer> getSelectedOrdinal() {
        return mSelectedOrdinal;
    }

    public boolean isOrdinalSelected(int ordinal) {
        return mSelectedOrdinal.contains(ordinal);
    }

    public void addSelectedOrdinal(int ordinal) {
        mSelectedOrdinal.add(ordinal);
    }

    public void removeSelectedOrdinal(int ordinal) {
        getSelectedOrdinal().remove(ordinal);
    }
}
