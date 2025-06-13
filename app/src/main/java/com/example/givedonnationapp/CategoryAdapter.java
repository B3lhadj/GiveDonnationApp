package com.example.givedonnationapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class CategoryAdapter extends BaseAdapter {
    private Context context;
    private List<DonationCategory> categories;

    public CategoryAdapter(Context context, List<DonationCategory> categories) {
        this.context = context;
        this.categories = categories;
    }

    @Override
    public int getCount() {
        return categories.size();
    }

    @Override
    public Object getItem(int position) {
        return categories.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_category, parent, false);
        }

        DonationCategory category = categories.get(position);

        ImageView icon = convertView.findViewById(R.id.categoryIcon);
        TextView name = convertView.findViewById(R.id.categoryName);

        icon.setImageResource(category.getIconResId());
        name.setText(category.getName());

        return convertView;
    }
}