package com.fogok.yandextranslater.services;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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
        //  возвращаем доступные языки для перевода, возвращаем ответ перевода, возвращаем доступные языки для доп. вариантов перевода, возвращаем доп. вар. перевода
        GET_LANGUAGES_TO_TRANSLATE_IS_POSSIBLE, GET_TRANSLATE_RESPONSE, GET_LANGUAGES_TO_DICT_IS_POSSIBLE, GET_DICT_RESPONSE
    }

    public YandexApiService() {
        super("com.fogok.yandexapi");
        Log.d(TAG, "com.fogok.yandexapi");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG, "translateAction");

        RequestActions requestAction = (RequestActions) intent.getSerializableExtra(REQUEST_ACTION);

        String responseTranslate = "";  //перевод

        Log.d(TAG, requestAction.name());

        switch (requestAction){
            case GET_DICT_RESPONSE:
                if (!intent.getStringExtra(TARGET_TEXT_STR).equals("")) {    //если в строке ничего нет, то ничего не делаем
                    responseTranslate = postRequestGetMoreTranslatedJSON(intent.getStringExtra(TARGET_TEXT_STR), intent.getStringExtra(LANG_STR_DIRECTION));
                }
                break;
            case GET_LANGUAGES_TO_DICT_IS_POSSIBLE:
                responseTranslate = postRequestGetDictLangsJSON();
                break;
            case GET_LANGUAGES_TO_TRANSLATE_IS_POSSIBLE:
                responseTranslate = postRequestGetLangsJSON();
                break;
            case GET_TRANSLATE_RESPONSE:
                if (!intent.getStringExtra(TARGET_TEXT_STR).equals("")){    //если в строке ничего нет, то ничего не делаем
                    try {
                        String responseStringData = postRequestGetTranslatedJSON(intent.getStringExtra(TARGET_TEXT_STR), intent.getStringExtra(LANG_STR_DIRECTION));
                        JSONObject jsonAllResponseData = new JSONObject(responseStringData);
                        JSONArray textData = jsonAllResponseData.getJSONArray("text");
                        responseTranslate = textData.getString(0);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }


        //возвращаем результат через broadcast
        Intent responseIntent = new Intent();
        responseIntent
                .setAction(ACTION_YANDEXAPISERVICE)
                .addCategory(Intent.CATEGORY_DEFAULT)
                .putExtra(REQUEST_ACTION, requestAction)
                .putExtra(EXTRA_KEY_RESPONSE, responseTranslate);


        sendBroadcast(responseIntent);
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
        Log.d(TAG, "onDestroy");
    }
}
