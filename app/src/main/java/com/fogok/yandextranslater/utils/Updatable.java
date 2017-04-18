package com.fogok.yandextranslater.utils;

/**
 * Created by FOGOK on 18.04.2017 13:25.
 */

/**
 * Вешаем этот интерфейс на все фрагменты, чтобы их можно было обновлять, когда делаем перелистывание
 */

public interface Updatable {
    void updateState();
}
