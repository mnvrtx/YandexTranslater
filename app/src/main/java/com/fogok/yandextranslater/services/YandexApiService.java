package com.fogok.yandextranslater.services;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.fogok.yandextranslater.TabSelect;
import com.fogok.yandextranslater.sugarlitesql.HistoryObject;
import com.fogok.yandextranslater.tabs.favorites_and_history.History;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class YandexApiService extends IntentService {

    public static final String ACTION_YANDEXAPISERVICE = "com.fogok.yandextranslater.RESPONSE"; //имя действия для broadcast receiver
    public static final String EXTRA_KEY_RESPONSE = "EXTRA_RESPONSE";     //ключ для данных, передаваемых через broadcast receiver
    public static final String ERROR_POST_REQUEST = "ERROR_POST_REQUEST";   //в случае ошибки будет возвращён этот текст

    public static final String REQUEST_ACTION = "REQUEST_ACTION";    //при запросе через putExtra мы указываем, какое именно действие совершать

    public static final String TARGET_TEXT_STR = "TARGET_TEXT";    //при запросе через putExtra мы указываем текст перевода
    public static final String LANG_STR_DIRECTION = "LANG_DIRECTION";    //при запросе через putExtra мы указываем направление перевода (прим. ru-en)

    private final String KEY_TRNSL = "trnsl.1.1.20170402T115857Z.000ab4f56b6ed1cd.77bcaafd12b4db821db9e91e9b5a2ca7cfb7e9b8";
    private final String KEY_DICT = "dict.1.1.20170403T192637Z.3f4722d577196988.cc32ed79ada11772b986bd2cc9cbfe103fc3a54b";

    private final String TAG = "YandexApiService";


    public enum RequestActions{
        //  возвращаем доступные языки для перевода, возвращаем ответ перевода, возвращаем доступные языки для доп. вариантов перевода, возвращаем доп. вар. перевода, открываем перевод из истории
        GET_LANGUAGES_TO_TRANSLATE_IS_POSSIBLE, GET_TRANSLATE_RESPONSE, GET_LANGUAGES_TO_DICT_IS_POSSIBLE, GET_DICT_RESPONSE, HISTORY_OPEN
    }

    public YandexApiService() {
        super("com.fogok.yandexapi");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        assert intent != null;
        RequestActions requestAction = (RequestActions) intent.getSerializableExtra(REQUEST_ACTION);

        String responseTranslate = "";  //перевод
        String targetText = intent.getStringExtra(TARGET_TEXT_STR);     //текст, который надо перевести
        String langDir = intent.getStringExtra(LANG_STR_DIRECTION);     //направление перевода

        HistoryObject historyObject = isExistInHistoryObjects(targetText, langDir);
        boolean isExistInHistoryObjects = historyObject != null;
        if (isExistInHistoryObjects)
            requestAction = RequestActions.HISTORY_OPEN;

        Log.d(TAG, requestAction.name());

        switch (requestAction){
            case GET_DICT_RESPONSE:
                if (!targetText.equals(""))    //если в строке ничего нет, то ничего не делаем
                    responseTranslate = postRequestGetMoreTranslatedJSON(targetText, langDir);
                break;
            case GET_TRANSLATE_RESPONSE:
                if (!targetText.equals(""))    //если в строке ничего нет, то ничего не делаем
                    responseTranslate = parseJSONTranslatedResponse(postRequestGetTranslatedJSON(targetText, langDir));
                break;
            case GET_LANGUAGES_TO_DICT_IS_POSSIBLE:
                responseTranslate = postRequestGetDictLangsJSON();
                break;
            case GET_LANGUAGES_TO_TRANSLATE_IS_POSSIBLE:
                responseTranslate = postRequestGetLangsJSON();
                break;
        }


        //возвращаем результат через broadcast
        Intent responseIntent = new Intent();
        responseIntent
                .setAction(ACTION_YANDEXAPISERVICE)
                .addCategory(Intent.CATEGORY_DEFAULT)
                .putExtra(REQUEST_ACTION, requestAction);

        if (isExistInHistoryObjects)
            responseIntent.putExtra(EXTRA_KEY_RESPONSE, historyObject);
        else
            responseIntent.putExtra(EXTRA_KEY_RESPONSE, responseTranslate);



        sendBroadcast(responseIntent);
    }


    /**
     * Превращаем ответ JSON (перевод) в читабельный вид
     * @param responseStringData
     * @return Читабельный вид ответа
     */
    private String parseJSONTranslatedResponse(String responseStringData){
        try {
            JSONArray textData = new JSONObject(responseStringData).getJSONArray("text");
            return textData.getString(0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }


    /**
     * Смотрим, есть ли то, что нам нужно перевести в истории, и если есть - возвращаем HistoryObject
     * @param text текст, который мы должны проверить
     * @param langDir направление перевода
     * @return HistoryObject или null
     */
    @Nullable
    private HistoryObject isExistInHistoryObjects(String text, String langDir){
        if (text != null && langDir != null){
            ArrayList<HistoryObject> historyObjects = TabSelect.getHistoryObjects();
            for (HistoryObject historyObject : historyObjects)
                if (historyObject.getLangDirection().toLowerCase().equals(langDir))  //если направление перевода совпадает
                    if (historyObject.getFromLangText().equals(text))  //если совпадает строка, которую мы переводим со строкой, которая уже есть в истории
                        return historyObject;
        }
        return null;
    }


    /**
     * Выполняем POST запрос на сервер яндекса
     * @param targetText Текст, который необходимо перевести.
     * @param twoLangs Направление перевода. Прим. ru-en
     * @return JSON с dicResult (доп. вар. перевода и т.д.)
     */
    private String postRequestGetMoreTranslatedJSON(String targetText, String twoLangs){
        targetText = Uri.encode(targetText);
        String key = "key=" + KEY_DICT;
        String lang = "lang=" + twoLangs;
        String text = "text=" + targetText;
        String ui = "ui=ru";
        String params = key + "&" + lang + "&" + text + "&" + ui;
        return postRequestJSON("https://dictionary.yandex.net/api/v1/dicservice.json/lookup?", params);
    }

    /**
     * Выполняем POST запрос на сервер яндекса
     * @return JSON со всеми парами языков, на которые можно делать несколько вариантов перевода
     */
    private String postRequestGetDictLangsJSON(){
        String params = "key=" + KEY_DICT;
        return postRequestJSON("https://dictionary.yandex.net/api/v1/dicservice.json/getLangs?", params);
    }

    /**
     * Выполняем POST запрос на сервер яндекса
     * @param targetText Текст, который необходимо перевести.
     * @param twoLangs Направление перевода. Прим. ru-en
     * @return JSON с переводом
     */
    private String postRequestGetTranslatedJSON(String targetText, String twoLangs){
        targetText = Uri.encode(targetText);
        String params = "format=plain" + "&options=0" + "&text=" + targetText + "&lang=" + twoLangs + "&key=" + KEY_TRNSL;
        return postRequestJSON("https://translate.yandex.net/api/v1.5/tr.json/translate?", params);
    }

    /**
     * Выполняем POST запрос на сервер яндекса
     * @return JSON со всеми возможными языками, на который можно перевести текст
     */
    private String postRequestGetLangsJSON(){
        String params = "ui=ru" + "&key=" + KEY_TRNSL;
        return postRequestJSON("https://translate.yandex.net/api/v1.5/tr.json/getLangs?", params);
    }

    /**
     * Выполняем POST запрос на сервер яндекса
     * @param urlString адрес запроса
     * @param params Параметры для POST запроса
     * @return  JSON c ответом
     */
    private String postRequestJSON(String urlString, String params){
        InputStream is = null;  //сюда запишем данные
        HttpURLConnection conn = null;
        int errCode = -1;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();    //открываем подключение
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            conn.setRequestProperty("Content-Length", "" + Integer.toString(params.getBytes().length));
            conn.getOutputStream().write(params.getBytes("UTF-8"));     //отправляем параметры для POST запроса

            conn.connect();
            errCode = conn.getResponseCode();

            is = conn.getInputStream();

            String response = convertInputStreamToString(is);

            conn.disconnect();  //закрываем подключение

            return response;   //записываем ответ в тип string
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, errCode + "");
            Log.d(TAG, params + "");

        } finally {
            try {
                if (is != null)
                    is.close();         //если во время подключения произошла ошибка, то закрываем inputStream
            } catch (Exception ex) {}

            try {
                if (conn != null)
                    conn.disconnect();  //если во время подключения произошла ошибка, то обрываем его
            }catch (Exception e) {}
        }
        return ERROR_POST_REQUEST;
    }

    /**
     * Конвертируем InputStream в String
     */
    private String convertInputStreamToString(InputStream inputStream){
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        try {
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString("UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ERROR_POST_REQUEST;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
