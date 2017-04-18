package com.fogok.yandextranslater.tabs;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
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
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.fogok.yandextranslater.R;
import com.fogok.yandextranslater.TabSelect;
import com.fogok.yandextranslater.services.YandexApiService;
import com.fogok.yandextranslater.sugarlitesql.HistoryObject;
import com.fogok.yandextranslater.tabs.favorites_and_history.Favorites;
import com.fogok.yandextranslater.utils.MapUtil;
import com.fogok.yandextranslater.utils.Updatable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;


public class TranslaterFragment extends Fragment implements View.OnClickListener, Updatable {

    //region onSaveInstance params
    private final static String textEditText = "textEditText";
    private final static String textOutText = "textOutText";
    private final static String secondTextOutText = "secondTextOutText";
    private final static String spinnerFromIdSelectPos = "spinnerFromIdSelectPos";
    private final static String spinnerToIdSelectPos = "spinnerToIdSelectPos";
    private final static String langsTranslatePossibleHashMap = "langsTranslatePossibleHashMap";
    private final static String langsDictPossibleList = "langsDictPossibleList";
    //endregion

    //region Views
    private Animation reversLangBAnimation = null;     //анимация стрелки смены языка
    private EditText textEdit = null;  //поле для ввода фразы перевода
    private TextView textOut = null, secondTextOut = null;
    private ProgressBar translateIndicator = null;
    private Spinner fromLangSpinner = null, toLangSpinner = null;
    private ImageButton favoriteAddButton;
    //endregion

    private final String TAG = "TranslateFragmentLog";
    private Intent intentYandexApiService = null;
    private ArrayAdapter<String> fromLangAdapter = null, toLangAdapter = null;
    private HashMap<String, String> langsTranslatePossible = null;
    private ArrayList<String> langsDictPossible = new ArrayList<>();
    private ArrayList<String> spinnerDataLangs = new ArrayList<>();
    private TranslateResponseReceiver translateResponseReceiver = null;
    private HistoryObject currentHistoryObject = null;
    private boolean lockFirstActivate;
    private int lastFromSpinnerSelect, lastToSpinnerSelect;
    private String finalStringTranlate;

    public static TranslaterFragment newInstance() {
        return new TranslaterFragment();
    }

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
            //noinspection unchecked
            langsTranslatePossible = (HashMap<String, String>) savedInstanceState.getSerializable(langsTranslatePossibleHashMap);
            spinnerDataLangs.addAll(langsTranslatePossible.keySet());    //добавляем в лист(spinnerDataLangs) все языки
            langsDictPossible = savedInstanceState.getStringArrayList(langsDictPossibleList);   //на какие языки можно делать доп. вар. перевода
        }

        Log.d(TAG, "onCreate");
    }

    /**
     * Запрашиваем список доступных языков для перевода и доп. вар. перевода
     */
    private void requestLangsList(){
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
        initAllLayoutVars(view, savedInstanceState);    //инициализируем все переменные, которые связаны с layout
        Log.d(TAG, "onCreateView");
        return view;
    }

    private void initAllLayoutVars(View v, Bundle savedInstanceState){
        //инициализируем анимацию и ставим обработчик события кнопки смены языка
        reversLangBAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.anim_translaterevers);
        (v.findViewById(R.id.btnChangeLang)).setOnClickListener(this);

        //инициализируем progressBar, который будет отображать, переводится ли какой-то текст или нет
        translateIndicator = (ProgressBar) v.findViewById(R.id.translateIndicator);

        //инициализируем спиннеры, которые позволяют выбрать язык с которого будет перевод, и язык, на который будет перевод
        fromLangSpinner = (Spinner) v.findViewById(R.id.fromLang);
        toLangSpinner = (Spinner) v.findViewById(R.id.toLang);

        //адаптеры к ним
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

        //ставим обработчик событий для кнопки очистки editText, а так же инициализируем кнопку "добавить в избранное"
        (v.findViewById(R.id.clearEditText)).setOnClickListener(this);
        favoriteAddButton = (ImageButton) v.findViewById(R.id.favoriteAddButton);
        favoriteAddButton.setOnClickListener(this);

        //инициализируем поле для ввода фразы перевода и ставим в метод изменения текста (afterTextChanged) запуск процесса перевода
        textEdit = (EditText) v.findViewById(R.id.editText);
        if (savedInstanceState != null){
            lockFirstActivate = true; // если мы ставим текст в editText, значит блокируем первое срабатывание textWatcher
            textEdit.setText(savedInstanceState.getString(textEditText));
        }
        textEdit.addTextChangedListener(new TextWatcher() {     //ставим обработчик, который отслеживает изменение текста
                                                // время на написание нового, и не делать перевод. Ещё плюсы - экономия трафика и батареи
            private final long afterChangeDelay = 800; // milliseconds
            private Timer timer = new Timer();  //делаем небольшую задержку, чтобы после написанного символа дать ещё

            @Override
            public void afterTextChanged(Editable editable) {
                favoriteAddButton.setVisibility(View.INVISIBLE);
                textOut.setText("");
                secondTextOut.setText("");
                if (!lockFirstActivate && !textEdit.getText().toString().replace(" ", "").equals("")){
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
                }else{
                    translateIndicator.setVisibility(View.INVISIBLE);     //говорим пользователю, что мы не можем перевести, т.к. либо строка пустая, либо мы уже совершили перевод
                    timer.cancel();
                    lockFirstActivate = false;
                }
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


        //инициализируем textView с доп. вар. перевода в HTML формате
        secondTextOut = (TextView) v.findViewById(R.id.secondTextOut);
        if (savedInstanceState != null)
            secondTextOut.setText(savedInstanceState.getCharSequence(secondTextOutText));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btnChangeLang:    //Меняем направление перевода на обратное. Было ru-en => стало en-ru
                view.startAnimation(reversLangBAnimation);
                int toSelIndx = toLangSpinner.getSelectedItemPosition();

                //переменные lastToSpinnerSelect и lastFromSpinnerSelect нужны для того, чтобы лишний раз не сработал перевод
                lastToSpinnerSelect = fromLangSpinner.getSelectedItemPosition();
                toLangSpinner.setSelection(lastToSpinnerSelect);

                lastFromSpinnerSelect = toSelIndx;
                fromLangSpinner.setSelection(lastFromSpinnerSelect);

                lockFirstActivate = true; // если мы ставим текст в editText, значит блокируем первое срабатывание textWatcher
                textEdit.setText(textOut.getText());
                textOut.setText("");

                translateAction(); //совершаем перевод
                break;
            case R.id.clearEditText:
                textEdit.setText("");
                break;
            case R.id.favoriteAddButton:    //Добавляем/убираем объект из избранного
                currentHistoryObject.reversFavorite();
                currentHistoryObject.save();
                refreshFavoriteAddButton();
                break;
        }
    }

    /**
     * Обновляем состояние кнопки добавления в избранное
     */
    private void refreshFavoriteAddButton(){
        if (currentHistoryObject != null){
            favoriteAddButton.setColorFilter(ContextCompat.getColor(getContext(), currentHistoryObject.isFavorite() ? R.color.colorAccent : R.color.black));
            favoriteAddButton.setVisibility(View.VISIBLE);
        }else
            favoriteAddButton.setVisibility(View.INVISIBLE);
    }

    /**
     * Запускаем сервис, который сделает перевод
     */
    private void translateAction(){
        String fromLang = langsTranslatePossible.get(fromLangSpinner.getSelectedItem().toString());
        String toLang = langsTranslatePossible.get(toLangSpinner.getSelectedItem().toString());
        finalStringTranlate = fromLang + "-" + toLang;
        getContext().startService(intentYandexApiService
                .putExtra(YandexApiService.REQUEST_ACTION, YandexApiService.RequestActions.GET_TRANSLATE_RESPONSE)
                .putExtra(YandexApiService.LANG_STR_DIRECTION, finalStringTranlate)
                .putExtra(YandexApiService.TARGET_TEXT_STR, textEdit.getText().toString()));  //запускаем процесс перевода

        if (langsDictPossible.contains(finalStringTranlate)) {   //если можем делать доп. вар. перевода, то делаем, иначе делаем просто перевод
            getContext().startService(intentYandexApiService
                    .putExtra(YandexApiService.REQUEST_ACTION, YandexApiService.RequestActions.GET_DICT_RESPONSE)
                    .putExtra(YandexApiService.LANG_STR_DIRECTION, finalStringTranlate)
                    .putExtra(YandexApiService.TARGET_TEXT_STR, textEdit.getText().toString()));  //запускаем процесс перевода и доп. вар. перевода
        }

    }


    /**
     * Переводим HTML в spanned
     */
    private Spanned getSpannedFromHtml(String htmlData){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
            return Html.fromHtml(htmlData,Html.FROM_HTML_MODE_LEGACY);
        return Html.fromHtml(htmlData);
    }

    /**
     * Конвертируем ответ от словаря в приемлемый вид
     * @param dicResult ответ от словаря в JSON
     * @return HTML текст
     */
    private String convertDicResultToNormal(JSONObject dicResult){
        StringBuilder builder = new StringBuilder();
        try {

            builder.append("<b>").append(dicResult.getString("text")).append("</b>");

            if (!dicResult.getString("pos").equals("")) {     //если есть часть речи, выделяем и добавляем её
                builder.append("<br>");
                builder.append("<i>").append("<font color=\"grey\">");
                builder.append(dicResult.getString("pos"));
                builder.append("</font>").append("</i>");
            }

            JSONArray tr_Array = dicResult.getJSONArray("tr");
            for (int i = 0; i < tr_Array.length(); i++) {   //проходимся по всем объектам tr, и отображаем каждый
                JSONObject currentJObject = tr_Array.getJSONObject(i);

                JSONArray syn_Array = currentJObject.optJSONArray("syn");
                JSONArray mean_Array = currentJObject.optJSONArray("mean");
                builder.append("<br>");
                builder.append("<font color=\"grey\">").append(i + 1).append(") ").append("</font>");

                builder.append("<font color=\"blue\">");
                builder.append(currentJObject.getString("text"));
                if (syn_Array != null){
                    builder.append(", ");

                    for (int i2 = 0; i2 < syn_Array.length(); i2++) {
                        builder.append(syn_Array.getJSONObject(i2).getString("text"));
                        if (i2 != syn_Array.length() - 1)
                            builder.append(", ");
                    }
                }
                builder.append("</font>");

                if (mean_Array != null){
                    builder.append("<br>");
                    builder.append("<font color=\"maroon\">");
                    builder.append("(");
                    for (int i2 = 0; i2 < mean_Array.length(); i2++) {
                        builder.append(mean_Array.getJSONObject(i2).getString("text"));
                        if (i2 != mean_Array.length() - 1)
                            builder.append(", ");
                    }
                    builder.append(")");
                    builder.append("</font>");
                }
            }




        } catch (JSONException e) {
            e.printStackTrace();
        }


        return builder.toString();
    }

    /**
     * Для нормального отображения вложенных спанов в textView приходится буквально выворачивать маркировку наизнанку. Взято отсюда: https://habrahabr.ru/post/166351/
     */
    private Spannable revertSpanned(Spanned stext) {
        Object[] spans = stext.getSpans(0, stext.length(), Object.class);
        Spannable ret = Spannable.Factory.getInstance().newSpannable(stext.toString());
        if (spans != null && spans.length > 0) {
            for(int i = spans.length - 1; i >= 0; --i) {
                ret.setSpan(spans[i], stext.getSpanStart(spans[i]), stext.getSpanEnd(spans[i]), stext.getSpanFlags(spans[i]));
            }
        }
        return ret;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(textEditText, textEdit.getText().toString());
        outState.putString(textOutText, textOut.getText().toString());
        outState.putCharSequence(secondTextOutText, secondTextOut.getText());
        outState.putSerializable(langsTranslatePossibleHashMap, langsTranslatePossible);
        outState.putStringArrayList(langsDictPossibleList, langsDictPossible);
        outState.putInt(spinnerFromIdSelectPos, fromLangSpinner.getSelectedItemPosition());
        outState.putInt(spinnerToIdSelectPos, toLangSpinner.getSelectedItemPosition());
        Log.d(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
    }

    @Override
    public void updateState() {
        refreshFavoriteAddButton();
    }


    /**
     * Тут все манипуляции с данными
     */
    public class TranslateResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            YandexApiService.RequestActions requestAction = (YandexApiService.RequestActions) intent.getSerializableExtra(YandexApiService.REQUEST_ACTION);
            String result = "";
            //во всех requestAction, кроме HISTORY_OPEN, EXTRA_KEY_RESPONSE является String. В случае HISTORY_OPEN он является HistoryObject
            if (requestAction != YandexApiService.RequestActions.HISTORY_OPEN)
                result = intent
                        .getStringExtra(YandexApiService.EXTRA_KEY_RESPONSE);


            switch (requestAction) {
                case HISTORY_OPEN:
                    //region Этот блок сработает в двух случаях - при открытии элемента истории, или же если нужное слово уже есть в истории
                    HistoryObject openedHistoryObject = intent.getParcelableExtra(YandexApiService.EXTRA_KEY_RESPONSE);
                    lockFirstActivate = true; // если мы ставим текст в editText, значит блокируем первое срабатывание textWatcher
                    textEdit.setText(openedHistoryObject.getFromLangText());
                    textEdit.setSelection(textEdit.getText().length());
                    textOut.setText(openedHistoryObject.getToLangText());
                    secondTextOut.setText(getSpannedFromHtml(openedHistoryObject.getSecondOutText()));
                    String[] langDirections = openedHistoryObject.getLangDirection().split("-");
                    int fromIndex = new ArrayList<>(langsTranslatePossible.values()).indexOf(langDirections[0]);   //изначально выбранный язык с которого будет перевод
                    int toIndex = new ArrayList<>(langsTranslatePossible.values()).indexOf(langDirections[1]);   //изначально выбранный язык на который будет перевод

                    lastFromSpinnerSelect = fromIndex;
                    fromLangSpinner.setSelection(fromIndex);

                    lastToSpinnerSelect = toIndex;
                    toLangSpinner.setSelection(toIndex);

                    currentHistoryObject = openedHistoryObject;
                    refreshFavoriteAddButton();

                    ((TabSelect) getActivity()).getTabLayout().getTabAt(0).select();
                    //endregion
                    break;
                case GET_DICT_RESPONSE:
                    //region Если словарные данные получены с интернета - сработает этот блок, иначе сработает блок HISTORY_OPEN.
                    if (!result.equals("")) {
                        try {
                            JSONArray dictArticles = new JSONObject(result).getJSONArray("def");
                            if (dictArticles.length() != 0) {   //если мы получили хотя бы одну словарную статью
                                String htmlData = convertDicResultToNormal(dictArticles.getJSONObject(0));

                                secondTextOut.setText(revertSpanned(getSpannedFromHtml(htmlData)));

                                currentHistoryObject
                                        .setSecondOutText(htmlData);
                                currentHistoryObject.save();

                            } else
                                secondTextOut.setText("");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    //endregion
                    break;
                case GET_LANGUAGES_TO_DICT_IS_POSSIBLE:
                    //region Возвращаем доступные языки для доп. вариантов перевода
                    try {
                        JSONArray jsonArray = new JSONArray(result);
                        langsDictPossible = new ArrayList<>(jsonArray.length());
                        for (int i = 0; i < jsonArray.length(); i++) {
                            langsDictPossible.add(jsonArray.get(i).toString());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    //endregion
                    break;
                case GET_LANGUAGES_TO_TRANSLATE_IS_POSSIBLE:
                    //region Получаем данные доступных языков для перевода
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        jsonObject = jsonObject.getJSONObject("langs");
                        langsTranslatePossible = new HashMap<>(jsonObject.length());
                        for (Iterator<String> iter = jsonObject.keys(); iter.hasNext(); ) {
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

                    int ffromIndex = new ArrayList<>(langsTranslatePossible.values()).indexOf("en");   //изначально выбранный язык с которого будет перевод
                    int ttoIndex = new ArrayList<>(langsTranslatePossible.values()).indexOf("ru");   //изначально выбранный язык на который будет перевод

                    //вставка
                    fromLangAdapter.notifyDataSetChanged();
                    lastFromSpinnerSelect = ffromIndex;
                    fromLangSpinner.setSelection(ffromIndex);

                    toLangAdapter.notifyDataSetChanged();
                    lastToSpinnerSelect = ttoIndex;
                    toLangSpinner.setSelection(ttoIndex);
                    //endregion
                    break;
                case GET_TRANSLATE_RESPONSE:
                    //region Если перевод получен с интернета - сработает этот блок, иначе сработает блок HISTORY_OPEN.
                    translateIndicator.setVisibility(View.INVISIBLE);    //говорим пользователю, что мы завершили перевод
                    textOut.setText(result.trim());     //trim - обрезаем лишние пробелы
                    secondTextOut.setText("");  ///обновляем доп. вар. перевода


                    HistoryObject historyObject = new HistoryObject(
                            "0",
                            textEdit.getText().toString(),
                            textOut.getText().toString(),
                            "",
                            finalStringTranlate.toUpperCase()
                    );

                    historyObject.save();
                    TabSelect.getHistoryObjects().add(historyObject);

                    currentHistoryObject = historyObject;
                    refreshFavoriteAddButton();
                    //endregion
                    break;
            }

            Log.d(TAG, "onReceive: " + result);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().unregisterReceiver(translateResponseReceiver);
        Log.d(TAG, "onDestroy");
    }
}
