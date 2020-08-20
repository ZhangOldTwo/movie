package com.hw.movie.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hw.movie.R;
import com.hw.movie.listenerinterface.OnItemClickListener;
import com.hw.movie.model.MovieBean;

import java.util.ArrayList;
import java.util.List;

// ① 创建Adapter
public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.VH> {
    private List<MovieBean> mDatas;
    private OnItemClickListener mListener;

    public MovieAdapter(OnItemClickListener mListener) {
        this.mListener = mListener;
        mDatas = new ArrayList<>();
    }

    public void setmDatas(List<MovieBean> data) {
        mDatas.clear();
        mDatas.addAll(data);
    }

    public void addData(MovieBean data) {
        mDatas.add(data);
    }
    public void cleanData() {
        mDatas.clear();
    }

    public MovieBean getItem(int position) {
        return mDatas.get(position);
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        //LayoutInflater.from指定写法
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recycler_movie, parent, false);
        return new VH(v);
    }

    //③ 在Adapter中实现3个方法
    @Override
    public void onBindViewHolder(VH holder, final int position) {
        holder.title.setText(mDatas.get(position).getTitle());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.setOnItemClickListener(v, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    //② 创建ViewHolder
    public static class VH extends RecyclerView.ViewHolder {
        public final TextView title;

        public VH(View v) {
            super(v);
            title = v.findViewById(R.id.text1);
        }
    }
}