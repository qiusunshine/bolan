package com.hd.tvpro.setting;

import java.util.ArrayList;

public class SettingOption {
    public String mLeftValue;
    public ArrayList<String> mRightList;

    public SettingOption(String name) {
        mRightList = new ArrayList<>();
        mLeftValue = name;
    }
}
