package com.fogok.yandextranslater.sugarlitesql;

import android.os.Parcel;
import android.os.Parcelable;

import com.fogok.yandextranslater.utils.MatchableRVArrayAdapter;
import com.orm.SugarRecord;

/**
 * Created by FOGOK on 13.04.2017 16:38.
 */

public class HistoryObject extends SugarRecord implements Parcelable, MatchableRVArrayAdapter.Matchable {

    public static String NOFAVORITE = "0", ISFAVORITE = "1";

    private String isFavorite;
    private String fromLangText;
    private String toLangText;
    private String secondOutText;
    private String langDirection;

    public HistoryObject() {
    }

    public HistoryObject(String isFavorite, String fromLangText, String toLangText, String secondOutText, String langDirection) {
        this.isFavorite = isFavorite;
        this.fromLangText = fromLangText;
        this.toLangText = toLangText;
        this.secondOutText = secondOutText;
        this.langDirection = langDirection;
    }

    public void setSecondOutText(String secondOutText) {
        this.secondOutText = secondOutText;
    }

    public boolean isFavorite() {
        return isFavorite.equals(ISFAVORITE);
    }

    public String getFromLangText() {
        return fromLangText;
    }

    public String getToLangText() {
        return toLangText;
    }

    public String getLangDirection() {
        return langDirection;
    }

    public String getSecondOutText() {
        return secondOutText;
    }

    public void reversFavorite(){
        isFavorite = isFavorite.equals(NOFAVORITE) ? ISFAVORITE : NOFAVORITE;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.isFavorite);
        dest.writeString(this.fromLangText);
        dest.writeString(this.toLangText);
        dest.writeString(this.secondOutText);
        dest.writeString(this.langDirection);
    }

    protected HistoryObject(Parcel in) {
        this.isFavorite = in.readString();
        this.fromLangText = in.readString();
        this.toLangText = in.readString();
        this.secondOutText = in.readString();
        this.langDirection = in.readString();
    }

    public static final Parcelable.Creator<HistoryObject> CREATOR = new Parcelable.Creator<HistoryObject>() {
        @Override
        public HistoryObject createFromParcel(Parcel source) {
            return new HistoryObject(source);
        }

        @Override
        public HistoryObject[] newArray(int size) {
            return new HistoryObject[size];
        }
    };

    @Override
    public boolean matches(String lowerCasePrefix) {
        return fromLangText.toLowerCase().contains(lowerCasePrefix) || toLangText.toLowerCase().contains(lowerCasePrefix);
    }
}
