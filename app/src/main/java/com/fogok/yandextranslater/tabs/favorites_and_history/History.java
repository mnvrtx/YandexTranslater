package com.fogok.yandextranslater.tabs.favorites_and_history;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

public class History extends FavoritesAndHistoryMain {

    public static History newInstance() {
        return new History();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        isHistory = true;
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initAllLayoutVars(View v, @Nullable ViewGroup container, Bundle savedInstanceState) {
        super.initAllLayoutVars(v, container, savedInstanceState);
        searchEditText.setHint("Найти в истории");
    }
}
