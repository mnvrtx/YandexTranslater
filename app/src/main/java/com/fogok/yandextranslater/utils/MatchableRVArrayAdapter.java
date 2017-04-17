package com.fogok.yandextranslater.utils;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import java.util.ArrayList;

public abstract class MatchableRVArrayAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> implements Filterable {

    protected ArrayList<T> mFilteringObjects;
    private ArrayList<T> mOriginalObjects;

    private int mResource;

    private Context mContext;

    private ArrayFilter mFilter;

    private LayoutInflater mInflater;

    public MatchableRVArrayAdapter(Context context, int resource, ArrayList<T> objects) {
        init(context, resource, objects);
    }

    public void refreshAdapter(){
        mFilteringObjects = new ArrayList<T>(mOriginalObjects);
        notifyDataSetChanged();
    }

    public void remove(T object) {
        mOriginalObjects.remove(object);

        if (mFilteringObjects.contains(object))
            mFilteringObjects.remove(object);
    }

    private void init(Context context, int resource, ArrayList<T> objects) {
        mContext = context;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResource = resource;
        mOriginalObjects = objects;
        mFilteringObjects = new ArrayList<>(mOriginalObjects);
    }

    public Context getContext() {
        return mContext;
    }

    public T getItem(int position) {
        return mFilteringObjects.get(position);
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(mResource, parent, false);
        return onCreateHolder(view);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        T item = getItem(position);
        onBindHolder(item, holder, position);
    }

    protected abstract VH onCreateHolder(View view);
    protected abstract void onBindHolder(T item, VH holder, int position);

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mFilteringObjects.size();
    }

    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new ArrayFilter();
        }
        return mFilter;
    }

    private class ArrayFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();

            ArrayList<T> values;
            if (prefix == null || prefix.length() == 0) {
                values = new ArrayList<>(mOriginalObjects);
            } else {
                String prefixString = prefix.toString().toLowerCase();
                values = new ArrayList<>();

                for (int i = 0; i < mOriginalObjects.size(); i++) {
                    final T value = mOriginalObjects.get(i);
                    if (matches(value, prefixString)) {
                        values.add(value);
                    }
                }
            }

            results.values = values;
            results.count = values.size();

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            //noinspection unchecked
            mFilteringObjects = new ArrayList<T>((ArrayList<T>) results.values);
            notifyDataSetChanged();
        }
    }

    private boolean matches(T value, String lowerCasePrefix) {
        if (value instanceof Matchable) {
            return ((Matchable) value).matches(lowerCasePrefix);
        }
        return value.toString().toLowerCase().contains(lowerCasePrefix);
    }

    public interface Matchable {
        boolean matches(String lowerCasePrefix);
    }
}
