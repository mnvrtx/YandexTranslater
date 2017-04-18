package com.fogok.yandextranslater.tabs.favorites_and_history;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.fogok.yandextranslater.R;
import com.fogok.yandextranslater.TabSelect;
import com.fogok.yandextranslater.services.YandexApiService;
import com.fogok.yandextranslater.sugarlitesql.HistoryObject;
import com.fogok.yandextranslater.utils.MatchableRVArrayAdapter;

import java.util.ArrayList;

import static com.fogok.yandextranslater.services.YandexApiService.ACTION_YANDEXAPISERVICE;
import static com.fogok.yandextranslater.services.YandexApiService.EXTRA_KEY_RESPONSE;
import static com.fogok.yandextranslater.services.YandexApiService.REQUEST_ACTION;

/**
 * Created by FOGOK on 14.04.2017 10:14.
 */

public class FahAdapter extends MatchableRVArrayAdapter<HistoryObject, FahAdapter.FahHolder> {
    private Context context = null;
    private boolean isHistory;


    public FahAdapter(Context context, ArrayList<HistoryObject> historyObjects, ArrayList<HistoryObject> favoriteObjects, boolean isHistory) {
        super(context, R.layout.fav_and_hist_item, isHistory ? historyObjects : favoriteObjects);
        this.context = context;
        this.isHistory = isHistory;
    }

    @Override
    protected FahHolder onCreateHolder(View view) {
        return new FahHolder(view);
    }

    @Override
    protected void onBindHolder(HistoryObject item, FahHolder holder, int position) {
        holder.langFrom.setText(item.getFromLangText());
        holder.langTo.setText(item.getToLangText());
        holder.langDirection.setText(item.getLangDirection());
        holder.isFavorite.setColorFilter(ContextCompat.getColor(context, item.isFavorite() ? R.color.colorAccent : R.color.black));
    }

    public class FahHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        TextView langFrom, langTo = null, langDirection = null;
        ImageButton deleteButton = null;
        ImageView isFavorite = null;
        

        public FahHolder(View itemView) {
            super(itemView);
            langFrom = (TextView) itemView.findViewById(R.id.langFrom);
            langTo = (TextView) itemView.findViewById(R.id.langTo);
            langDirection = (TextView) itemView.findViewById(R.id.langDirection);
            isFavorite = (ImageView) itemView.findViewById(R.id.isSaved);
            isFavorite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    HistoryObject historyObject = getItem(getAdapterPosition());

                    if (isHistory){
                        if (historyObject.isFavorite())
                            TabSelect.getFavoriteObjects().remove(historyObject);
                        else
                            TabSelect.getFavoriteObjects().add(historyObject);
                        notifyItemChanged(getAdapterPosition());
                    }else{
                        remove(historyObject);
                        notifyItemRemoved(getAdapterPosition());
                    }

                    historyObject.reversFavorite();
                    historyObject.save();


                }
            });
            deleteButton = (ImageButton) itemView.findViewById(R.id.deleteButton);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                    builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            HistoryObject historyObject = getItem(getAdapterPosition());
                            if (!isHistory)
                                TabSelect.getHistoryObjects().remove(historyObject);

                            remove(historyObject);
                            notifyItemRemoved(getAdapterPosition());
                            historyObject.delete();
                        }
                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int id) {

                        }
                    }).setMessage(R.string.you_sure_delete_element).create().show();
                }
            });
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {    //передаем в translaterFragment HistoryObject, чтобы он его отобразил
            Intent responseIntent = new Intent();
            responseIntent
                    .setAction(ACTION_YANDEXAPISERVICE)
                    .addCategory(Intent.CATEGORY_DEFAULT)
                    .putExtra(REQUEST_ACTION, YandexApiService.RequestActions.HISTORY_OPEN)
                    .putExtra(EXTRA_KEY_RESPONSE, getItem(getAdapterPosition()));


            getContext().sendBroadcast(responseIntent);
        }
    }
}
