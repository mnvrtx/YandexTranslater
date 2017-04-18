package com.fogok.yandextranslater.tabs.favorites_and_history;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

public class Favorites extends FavoritesAndHistoryMain {

    public static Favorites newInstance() {
        return new Favorites();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        isHistory = false;
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initAllLayoutVars(View v, @Nullable ViewGroup container, Bundle savedInstanceState) {
        super.initAllLayoutVars(v, container, savedInstanceState);
        searchEditText.setHint("Найти в избранном");
    }
}