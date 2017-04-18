package com.fogok.yandextranslater.tabs.favorites_and_history;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.fogok.yandextranslater.R;
import com.fogok.yandextranslater.TabSelect;
import com.fogok.yandextranslater.utils.Updatable;


public class FavoritesAndHistoryMain extends Fragment implements Updatable {

    protected EditText searchEditText = null;
    protected boolean isHistory;
    private RecyclerView recyclerView = null;
    private FahAdapter fahAdapter = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fahAdapter = new FahAdapter(getContext(), TabSelect.getHistoryObjects(), TabSelect.getFavoriteObjects(), isHistory);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView =
                inflater.inflate(R.layout.favorites_and_history_content, container, false);
        initAllLayoutVars(rootView, container, savedInstanceState);
        return rootView;
    }

    protected void initAllLayoutVars(View v, @Nullable ViewGroup container,  Bundle savedInstanceState){

        recyclerView = (RecyclerView) v.findViewById(R.id.listView);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), OrientationHelper.VERTICAL));
        recyclerView.setAdapter(fahAdapter);

        searchEditText = (EditText) v.findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                fahAdapter.getFilter().filter(editable.toString().toLowerCase());
            }
        });
    }

    @Override
    public void updateState() {
        fahAdapter.refreshAdapter();
    }
}