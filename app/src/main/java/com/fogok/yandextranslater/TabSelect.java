package com.fogok.yandextranslater;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.widget.ImageView;

import com.fogok.yandextranslater.tabs.FavoritesAndHistoryFragment;
import com.fogok.yandextranslater.tabs.SettingsFragment;
import com.fogok.yandextranslater.tabs.TranslaterFragment;

import java.util.ArrayList;
import java.util.List;

public class TabSelect extends AppCompatActivity {

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab_select);

        // Создаём адаптер, который будет возвращать для viewPager нужный fragment
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        addFragmentsToAdapter();    // Добавляем в адаптер все нужные layout


        // Создаём и настраиваем viewPager, который будет отображать нужный fragment
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // Инициализируем и настраиваем tabLayout
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        createTabIcons();   //добавляем кастомные лайауты с векторными иконками на вкладки
    }

    private void createTabIcons() {
        final ImageView tabOne = (ImageView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        tabOne.setImageResource(R.drawable.ic_onetab);
        tabLayout.getTabAt(0).setCustomView(tabOne);

        final ImageView tabTwo = (ImageView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        tabTwo.setImageResource(R.drawable.ic_twotab);
        tabLayout.getTabAt(1).setCustomView(tabTwo);

        final ImageView tabThree = (ImageView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        tabThree.setImageResource(R.drawable.ic_threetab);
        tabLayout.getTabAt(2).setCustomView(tabThree);
    }

    private void addFragmentsToAdapter(){
        mSectionsPagerAdapter.addFrag(new TranslaterFragment(), "1");
        mSectionsPagerAdapter.addFrag(new FavoritesAndHistoryFragment(), "2");
        mSectionsPagerAdapter.addFrag(new SettingsFragment(), "3");
    }

    class SectionsPagerAdapter extends FragmentStatePagerAdapter {

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
