package com.fogok.yandextranslater.tabs;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.fogok.yandextranslater.R;
import com.fogok.yandextranslater.TabSelect;
import com.fogok.yandextranslater.sugarlitesql.HistoryObject;
import com.fogok.yandextranslater.tabs.favorites_and_history.Favorites;
import com.fogok.yandextranslater.tabs.favorites_and_history.FavoritesAndHistoryMain;
import com.fogok.yandextranslater.tabs.favorites_and_history.History;

import java.util.ArrayList;

import static com.fogok.yandextranslater.TabSelect.TAG;


public class FavoritesAndHistoryFragment extends Fragment implements View.OnClickListener {

    private static final String TAB_POSITION = "TAB_POSITION";
    private FragmentManager thisFragmentManager;

    private SectionsPagerAdapter mSectionsPagerAdapter = null;
    private ViewPager mViewPager = null;
    private TabLayout tabLayout = null;
    private ImageButton clearButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView =
                inflater.inflate(R.layout.favorites_and_history_f, container, false);
        initAllLayoutVars(rootView, savedInstanceState);
        return rootView;
    }

    private void initAllLayoutVars(View v, Bundle savedInstanceState){

        //
        clearButton = (ImageButton) v.findViewById(R.id.clearButton);
        clearButton.setOnClickListener(this);

        // Создаём адаптер, который будет возвращать для viewPager нужный fragment
        thisFragmentManager = getChildFragmentManager();
        addFragmentsToBundle();    // Добавляем в адаптер все нужные layout
        mSectionsPagerAdapter = new SectionsPagerAdapter(thisFragmentManager);


        // Создаём и настраиваем viewPager, который будет отображать нужный fragment
        mViewPager = (ViewPager) v.findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                ((FavoritesAndHistoryMain)thisFragmentManager.getFragments().get(position)).notifyDataSetChanged();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        // Инициализируем и настраиваем tabLayout
        tabLayout = (TabLayout) v.findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        if (savedInstanceState != null)
            tabLayout.getTabAt(savedInstanceState.getInt(TAB_POSITION)).select();
    }

    public void notifyDataSetChanged(){
        ((FavoritesAndHistoryMain)thisFragmentManager.getFragments().get(tabLayout.getSelectedTabPosition())).notifyDataSetChanged();
    }

    Bundle argumentsBundle = new Bundle();
    private void addFragmentsToBundle(){
//        argumentsBundle.putParcelableArrayList(HISTORY_OBJECTS_KEY, historyObjects);
//        argumentsBundle.putParcelableArrayList(FAVORITE_OBJECTS_KEY, favoriteObjects);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.clearButton:

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                builder.setPositiveButton("Удалить все элементы", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        TabSelect.getHistoryObjects().clear();
                        TabSelect.getFavoriteObjects().clear();
                        HistoryObject.deleteAll(HistoryObject.class);
                        notifyDataSetChanged();
                    }
                }).setNegativeButton("Удалить всё, кроме избранного", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int id) {
                        ArrayList<HistoryObject> historyObjects = TabSelect.getHistoryObjects();
                        for (int i = 0; i < historyObjects.size(); i++) {
                            if (!historyObjects.get(i).isFavorite()){
                                historyObjects.get(i).delete();
                                historyObjects.remove(i);
                                i--;
                            }

                        }
                        notifyDataSetChanged();
                    }
                }).setNeutralButton("Отмена", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                }).setMessage("Удаление элементов").create().show();



                break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(TAB_POSITION, tabLayout.getSelectedTabPosition());
    }


    private class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        private static final int countItemsInTabs = 2;

        public SectionsPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return position == 0 ? History.newInstance(argumentsBundle) : Favorites.newInstance(argumentsBundle);
        }

        @Override
        public int getCount() {
            return countItemsInTabs;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return position == 0 ? "ИСТОРИЯ" : "ИЗБРАННОЕ";
        }

        //        @SuppressWarnings("unchecked")
//        public void pageRefresh(int position) {
//            ((FavoritesAndHistoryMain)mFragmentList.get(position)).notifyDataSetChanged();
//        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
}
