package com.fogok.yandextranslater;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageView;

import com.fogok.yandextranslater.sugarlitesql.HistoryObject;
import com.fogok.yandextranslater.tabs.FavoritesAndHistoryFragment;
import com.fogok.yandextranslater.tabs.SettingsFragment;
import com.fogok.yandextranslater.tabs.TranslaterFragment;

import java.util.ArrayList;
import java.util.List;

public class TabSelect extends AppCompatActivity {

    public static final String HISTORY_OBJECTS_KEY = "HISTORY_OBJECTS_KEY";
    public static final String FAVORITE_OBJECTS_KEY = "FAVORITE_OBJECTS_KEY";
    public static final String TAG = "TabSelect";

    private SectionsPagerAdapter mSectionsPagerAdapter = null;
    private ViewPager mViewPager = null;
    private TabLayout tabLayout = null;

    private static ArrayList<HistoryObject> historyObjects = null;    //объекты истории
    private static ArrayList<HistoryObject> favoriteObjects = null;   //ссылки на historyObjects, с параметром isFavorite


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab_select);
        Log.d(TAG, "onCreate");

//        if (savedInstanceState == null){
//
//        }else{
//            historyObjects = savedInstanceState.getParcelableArrayList(HISTORY_OBJECTS_KEY);
//            favoriteObjects = savedInstanceState.getParcelableArrayList(FAVORITE_OBJECTS_KEY);
//        }


        // Создаём адаптер, который будет возвращать для viewPager нужный fragment
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        addFragmentsToAdapter(savedInstanceState);    // Добавляем в адаптер все нужные layout


        // Создаём и настраиваем viewPager, который будет отображать нужный fragment
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Fragment fragment = getSupportFragmentManager().getFragments().get(position);
                if (fragment instanceof FavoritesAndHistoryFragment)
                    ((FavoritesAndHistoryFragment) fragment).notifyDataSetChanged();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        // Инициализируем и настраиваем tabLayout
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tab.getCustomView().setAlpha(1f);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                tab.getCustomView().setAlpha(0.4f);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }


        });

        createTabIcons();   //добавляем кастомные лайауты с векторными иконками на вкладки
    }

    private void createTabIcons() {
        final ImageView tabOne = (ImageView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        tabOne.setImageResource(R.drawable.ic_onetab);
        tabOne.setAlpha(1f);
        tabLayout.getTabAt(0).setCustomView(tabOne);

        final ImageView tabTwo = (ImageView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        tabTwo.setImageResource(R.drawable.ic_twotab);
        tabLayout.getTabAt(1).setCustomView(tabTwo);

        final ImageView tabThree = (ImageView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        tabThree.setImageResource(R.drawable.ic_threetab);
        tabLayout.getTabAt(2).setCustomView(tabThree);
    }

    public TabLayout getTabLayout() {
        return tabLayout;
    }

    private Fragment translaterFragment;
    private Fragment favoritesAndHistoryFragment;
    private Fragment settingsFragment;

    private void addFragmentsToAdapter(Bundle saveInstanceState){
        translaterFragment = new TranslaterFragment();
        favoritesAndHistoryFragment = new FavoritesAndHistoryFragment();
        settingsFragment = new SettingsFragment();

        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(HISTORY_OBJECTS_KEY, historyObjects);
        bundle.putParcelableArrayList(FAVORITE_OBJECTS_KEY, favoriteObjects);

        translaterFragment.setArguments(bundle);
        favoritesAndHistoryFragment.setArguments(bundle);

        mSectionsPagerAdapter.addFrag(translaterFragment, "1");
        mSectionsPagerAdapter.addFrag(favoritesAndHistoryFragment, "2");
        mSectionsPagerAdapter.addFrag(settingsFragment, "3");
    }

    public static ArrayList<HistoryObject> getFavoriteObjects() {
        Log.d("TEST", "getFavoriteObjects");
        if (favoriteObjects == null){
            favoriteObjects = new ArrayList<>();
            //favorite object должен содержать ссылки на history objects, поэтом инициализируем его так
            for (int i = 0; i < historyObjects.size(); i++) {
                if (historyObjects.get(i).isFavorite())
                    favoriteObjects.add(historyObjects.get(i));
            }
        }
        return favoriteObjects;
    }

    public static ArrayList<HistoryObject> getHistoryObjects() {
        Log.d("TEST", "getHistoryObjects");
        if (historyObjects == null){
            historyObjects = new ArrayList<>(HistoryObject.listAll(HistoryObject.class));
        }
        return historyObjects;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("TEST", "onDestroy");
        favoriteObjects = null;
        historyObjects = null;
    }

    private class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        private static final int countItemsInTabs = 3;
        private final List<Fragment> mFragmentList = new ArrayList<>(countItemsInTabs);
        private final List<String> mFragmentTitleList = new ArrayList<>(countItemsInTabs);

        public SectionsPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFrag(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }



}
