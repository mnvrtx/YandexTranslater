package com.fogok.yandextranslater.tabs;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.fogok.yandextranslater.R;
import com.fogok.yandextranslater.services.YandexApiService;
import com.fogok.yandextranslater.utils.MapUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;


public class TranslaterFragment extends Fragment {

    private final String TAG = "TranslateFragmentLog";

    private Animation reversLangBAnimation;     //анимация стрелки смены языка
    private EditText textEdit;  //поле для ввода фразы перевода

    private Intent intentYandexApiService;
    private TextView textOut;
    private ProgressBar translateIndicator;

    private Spinner fromLangSpinner, toLangSpinner;
    private ArrayAdapter<String> fromLangAdapter, toLangAdapter;
    private HashMap<String, String> langsTranslatePossible;
    private ArrayList<String> langsDictPossible = new ArrayList<>();

    private ArrayList<String> spinnerDataLangs = new ArrayList<>();

    private TranslateResponseReceiver translateResponseReceiver;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //регистрируем приёмник ответа от апи переводчика
        translateResponseReceiver = new TranslateResponseReceiver();
        IntentFilter intentFilter = new IntentFilter(
                YandexApiService.ACTION_YANDEXAPISERVICE);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        getContext().registerReceiver(translateResponseReceiver, intentFilter);

        //инициализируем намерение для перевода, используя класс YandexApiService
        intentYandexApiService = new Intent(getContext(), YandexApiService.class);


        //запрашиваем списк доступных языков для перевода и доп. вар. перевода, или же достаём из из сохранённых данных
        if (savedInstanceState == null){
            requestLangsList();
        }else{
            langsTranslatePossible = (HashMap<String, String>) savedInstanceState.getSerializable(langsTranslatePossibleHashMap);
            spinnerDataLangs.addAll(langsTranslatePossible.keySet());    //добавляем в лист(spinnerDataLangs) все языки
            langsDictPossible = savedInstanceState.getStringArrayList(langsDictPossibleList);   //на какие языки можно делать доп. вар. перевода
        }

        Log.d(TAG, "onCreate");
    }

    private void requestLangsList(){    //запрашиваем списк доступных языков для перевода и доп. вар. перевода
        getContext().startService(intentYandexApiService
                    .putExtra(YandexApiService.REQUEST_ACTION, YandexApiService.RequestActions.GET_LANGUAGES_TO_TRANSLATE_IS_POSSIBLE));
        getContext().startService(intentYandexApiService
                .putExtra(YandexApiService.REQUEST_ACTION, YandexApiService.RequestActions.GET_LANGUAGES_TO_DICT_IS_POSSIBLE));
    }



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view =
                inflater.inflate(R.layout.translater_f, container, false);
        //метод onSaveInstanceState не срабатывает при onDestroyView, будет свой bundle
        initAllLayoutVars(view, savedInstanceState);    //инициализируем все переменные, которые связаны с layout
        Log.d(TAG, "onCreateView");
        return view;
    }
    private boolean lockFirstActivate;
    private int lastFromSpinnerSelect, lastToSpinnerSelect;
    private void initAllLayoutVars(View v, Bundle savedInstanceState){
        //инициализируем анимацию и ставим обработчик события кнопки смены языка
        reversLangBAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.anim_translaterevers);
        (v.findViewById(R.id.btnChangeLang)).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)     //смена языка
            {
                reversLangs(v);
            }
        });


        //инициализируем progressBar, который будет отображать, переводится ли какой-то текст или нет
        translateIndicator = (ProgressBar) v.findViewById(R.id.translateIndicator);

        //инициализируем спиннеры, которые позволяют выбрать язык с которого будет перевод, и язык, на который будет перевод
        fromLangSpinner = (Spinner) v.findViewById(R.id.fromLang);
        toLangSpinner = (Spinner) v.findViewById(R.id.toLang);

        fromLangAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, spinnerDataLangs);
        fromLangSpinner.setAdapter(fromLangAdapter);

        toLangAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, spinnerDataLangs);
        toLangSpinner.setAdapter(toLangAdapter);

        if (savedInstanceState != null){
            fromLangSpinner.setSelection(savedInstanceState.getInt(spinnerFromIdSelectPos));
            lastFromSpinnerSelect = fromLangSpinner.getSelectedItemPosition();
            toLangSpinner.setSelection(savedInstanceState.getInt(spinnerToIdSelectPos));
            lastToSpinnerSelect = toLangSpinner.getSelectedItemPosition();
        }

        fromLangSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (lastFromSpinnerSelect == i)
                    return;
                lastFromSpinnerSelect = i;
                translateAction(); //совершаем перевод
                Log.d(TAG, "spinnerItemSelect");
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        toLangSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (lastToSpinnerSelect == i)
                    return;
                lastToSpinnerSelect = i;
                translateAction(); //совершаем перевод
                Log.d(TAG, "spinnerItemSelect");
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //инициализируем поле для ввода фразы перевода и ставим в метод изменения текста (afterTextChanged) запуск процесса перевода
        textEdit = (EditText) v.findViewById(R.id.editText);
        if (savedInstanceState != null){
            lockFirstActivate = true; // если мы ставим текст в editText, значит блокируем первое срабатывание textWatcher
            textEdit.setText(savedInstanceState.getString(textEditText));
        }

        textEdit.addTextChangedListener(new TextWatcher() {     //ставим обработчик, который отслеживает изменение текста
            private Timer timer = new Timer();  //делаем небольшую задержку, чтобы после написанного символа дать ещё
                                                // время на написание нового, и не делать перевод. Ещё плюсы - экономия трафика и батареи
            private final long afterChangeDelay = 800; // milliseconds
            @Override
            public void afterTextChanged(Editable editable) {
                if (!lockFirstActivate){
                    translateIndicator.setVisibility(View.VISIBLE);     //говорим пользователю, что мы запускаем процесс перевода
                    timer.cancel();
                    timer = new Timer();
                    timer.schedule(
                            new TimerTask() {
                                @Override
                                public void run() {
                                    translateAction();  //запускаем процесс перевода
                                }
                            },
                            afterChangeDelay
                    );
                }else
                    lockFirstActivate = false;
            }
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
        });

        //инициализируем textView, в котором будет перевод
        textOut = (TextView) v.findViewById(R.id.textOut);
        if (savedInstanceState != null)
            textOut.setText(savedInstanceState.getString(textOutText));
    }

    private void reversLangs(View v){
        v.startAnimation(reversLangBAnimation);
        int toSelIndx = toLangSpinner.getSelectedItemPosition();
        lastToSpinnerSelect = fromLangSpinner.getSelectedItemPosition();
        toLangSpinner.setSelection(lastToSpinnerSelect);
        lastFromSpinnerSelect = toSelIndx;
        fromLangSpinner.setSelection(lastFromSpinnerSelect);

        lockFirstActivate = true; // если мы ставим текст в editText, значит блокируем первое срабатывание textWatcher
        textEdit.setText(textOut.getText());
        textOut.setText("");

        translateAction(); //совершаем перевод
    }

    private void translateAction(){
        String fromLang = langsTranslatePossible.get(fromLangSpinner.getSelectedItem().toString());
        String toLang = langsTranslatePossible.get(toLangSpinner.getSelectedItem().toString());
        String finalStringTranlate = fromLang + "-" + toLang;
        if (langsDictPossible.contains(finalStringTranlate)){   //если можем делать доп. вар. перевода, то делаем, иначе делаем просто перевод
            getContext().startService(intentYandexApiService
                    .putExtra(YandexApiService.REQUEST_ACTION, YandexApiService.RequestActions.GET_DICT_RESPONSE)
                    .putExtra(YandexApiService.LANG_STR_DIRECTION, finalStringTranlate)
                    .putExtra(YandexApiService.TARGET_TEXT_STR, textEdit.getText().toString()));  //запускаем процесс перевода и доп. вар. перевода
        }else{
            getContext().startService(intentYandexApiService
                    .putExtra(YandexApiService.REQUEST_ACTION, YandexApiService.RequestActions.GET_TRANSLATE_RESPONSE)
                    .putExtra(YandexApiService.LANG_STR_DIRECTION, finalStringTranlate)
                    .putExtra(YandexApiService.TARGET_TEXT_STR, textEdit.getText().toString()));  //запускаем процесс перевода
        }
    }



    public class TranslateResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String result = intent
                    .getStringExtra(YandexApiService.EXTRA_KEY_RESPONSE);


            switch ((YandexApiService.RequestActions) intent.getSerializableExtra(YandexApiService.REQUEST_ACTION)){
                case GET_DICT_RESPONSE:
                    if (!result.equals("")){
                        translateIndicator.setVisibility(View.INVISIBLE);    //говорим пользователю, что мы завершили перевод
                        textOut.setText(result);
                    }
                    break;
                case GET_LANGUAGES_TO_DICT_IS_POSSIBLE:
                    //возвращаем доступные языки для доп. вариантов перевода
                    try {
                        JSONArray jsonArray = new JSONArray(result);
                        langsDictPossible = new ArrayList<>(jsonArray.length());
                        for (int i = 0; i < jsonArray.length(); i++) {
                            langsDictPossible.add(jsonArray.get(i).toString());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case GET_LANGUAGES_TO_TRANSLATE_IS_POSSIBLE:
                    //получаем данные доступных языков для перевода
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        jsonObject = jsonObject.getJSONObject("langs");
                        langsTranslatePossible = new HashMap<>(jsonObject.length());
                        for(Iterator<String> iter = jsonObject.keys(); iter.hasNext();) {
                            String key = iter.next();
                            langsTranslatePossible.put(jsonObject.getString(key), key);  ///прим.: key - Русский, val - ru
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                    //сортируем и ставим эти данные в спиннеры

                    //сортировка
                    langsTranslatePossible = (HashMap<String, String>) MapUtil.sort(langsTranslatePossible);
                    spinnerDataLangs.addAll(langsTranslatePossible.keySet());    //добавляем в лист(spinnerDataLangs) все языки

                    int fromIndex = new ArrayList<>(langsTranslatePossible.values()).indexOf("en");   //изначально выбранный язык с которого будет перевод
                    int toIndex = new ArrayList<>(langsTranslatePossible.values()).indexOf("ru");   //изначально выбранный язык на который будет перевод

                    //вставка
                    fromLangAdapter.notifyDataSetChanged();
                    lastFromSpinnerSelect = fromIndex;
                    fromLangSpinner.setSelection(fromIndex);

                    toLangAdapter.notifyDataSetChanged();
                    lastToSpinnerSelect = toIndex;
                    toLangSpinner.setSelection(toIndex);

                    break;
                case GET_TRANSLATE_RESPONSE:
                    translateIndicator.setVisibility(View.INVISIBLE);    //говорим пользователю, что мы завершили перевод
                    textOut.setText(result);
                    break;
            }

            Log.d(TAG, "onReceive: " + result);
        }
    }

    //onSaveInstance params
    private final static String textEditText = "textEditText";
    private final static String textOutText = "textOutText";
    private final static String spinnerFromIdSelectPos = "spinnerFromIdSelectPos";
    private final static String spinnerToIdSelectPos = "spinnerToIdSelectPos";
    private final static String langsTranslatePossibleHashMap = "langsTranslatePossibleHashMap";
    private final static String langsDictPossibleList = "langsDictPossibleList";

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(textEditText, textEdit.getText().toString());
        outState.putString(textOutText, textOut.getText().toString());
        outState.putSerializable(langsTranslatePossibleHashMap, langsTranslatePossible);
        outState.putStringArrayList(langsDictPossibleList, langsDictPossible);
        outState.putInt(spinnerFromIdSelectPos, fromLangSpinner.getSelectedItemPosition());
        outState.putInt(spinnerToIdSelectPos, toLangSpinner.getSelectedItemPosition());
        Log.d(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
    }
    ////


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().unregisterReceiver(translateResponseReceiver);
        Log.d(TAG, "onDestroy");
    }
}
