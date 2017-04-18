package com.fogok.yandextranslater.tabs;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fogok.yandextranslater.R;
import com.fogok.yandextranslater.tabs.favorites_and_history.Favorites;
import com.fogok.yandextranslater.utils.Updatable;


public class SettingsFragment extends Fragment implements Updatable {

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView =
                inflater.inflate(R.layout.settings_f, container, false);
        return rootView;
    }

    @Override
    public void updateState() {

    }
}
