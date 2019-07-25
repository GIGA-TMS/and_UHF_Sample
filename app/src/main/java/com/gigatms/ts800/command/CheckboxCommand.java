package com.gigatms.ts800.command;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static com.gigatms.ts800.CommandRecyclerViewAdapter.TABLE;

public class CheckboxCommand<E extends Enum<E>> extends Command {
    private OnGetValue mOnGetValue;
    private Set<E> mData;
    private Set<E> mSelected;

    public CheckboxCommand(String title, Class<E> enumData) {
        super(title);
        mData = EnumSet.allOf(enumData);
        mSelected = Collections.emptySet();
    }

    public CheckboxCommand(String title, String leftBtnName, String rightBtnName, Class<E> enumData) {
        super(title, leftBtnName, rightBtnName);
        mData = EnumSet.allOf(enumData);
        mSelected = Collections.emptySet();
    }

    public interface OnGetValue {
        void onGetValue(Set values);
    }

    @Override
    public int getViewType() {
        return TABLE;
    }

    public void setOnGetValue(OnGetValue onGetValue) {
        mOnGetValue = onGetValue;
    }

    public Set<E> getSet() {
        return mData;
    }

    public Set<E> getSelected() {
        return mSelected;
    }

    public void setSelected(Set set) {
        Set<E> selected = new HashSet<>();
        for (E e : mData) {
            if (set.contains(e.ordinal())) {
                selected.add(e);
            }
        }
        mSelected = selected;
    }

    public void didGetValue(Set values) {
        mOnGetValue.onGetValue(values);
    }
}
