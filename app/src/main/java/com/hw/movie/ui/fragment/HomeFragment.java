package com.hw.movie.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.azoft.carousellayoutmanager.CarouselLayoutManager;
import com.azoft.carousellayoutmanager.CarouselZoomPostLayoutListener;
import com.hw.movie.R;
import com.hw.movie.adapter.MovieAdapter;
import com.hw.movie.common.Constants;
import com.hw.movie.event.MessageEvent;
import com.hw.movie.listenerinterface.OnItemClickListener;
import com.hw.movie.model.MovieBean;
import com.hw.movie.tools.HttpUrlConstance;
import com.hw.movie.ui.WebViewActivity;
import com.tencent.mmkv.MMKV;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * @author zps
 */
public class HomeFragment extends Fragment implements OnItemClickListener {

    private RecyclerView mRecyclerView;
    private MovieAdapter movieAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        mRecyclerView = rootView.findViewById(R.id.mRecyclerView);
        initAdapter();
        EventBus.getDefault().register(this);
        return rootView;
    }

    private void initAdapter() {
//        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        final CarouselLayoutManager layoutManager = new CarouselLayoutManager(CarouselLayoutManager.VERTICAL);
        layoutManager.setPostLayoutListener(new CarouselZoomPostLayoutListener());
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
//        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        movieAdapter = new MovieAdapter(this);

        if (MMKV.defaultMMKV().decodeBool(Constants.IS_HAPPY)) {
            for (int i = 0; i < HttpUrlConstance.happy_title.length; i++) {
                MovieBean movieBean = new MovieBean(HttpUrlConstance.happy_title[i], HttpUrlConstance.happy_url[i]);
                movieAdapter.addData(movieBean);
            }
        } else {
            for (int i = 0; i < HttpUrlConstance.title.length; i++) {
                MovieBean movieBean = new MovieBean(HttpUrlConstance.title[i], HttpUrlConstance.url[i]);
                movieAdapter.addData(movieBean);
            }
        }

        mRecyclerView.setAdapter(movieAdapter);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMoonEvent(MessageEvent messageEvent) {
        movieAdapter.cleanData();
        if (messageEvent.getMessage()) {
            for (int i = 0; i < HttpUrlConstance.happy_title.length; i++) {
                MovieBean movieBean = new MovieBean(HttpUrlConstance.happy_title[i], HttpUrlConstance.happy_url[i]);
                movieAdapter.addData(movieBean);
            }
        } else {
            for (int i = 0; i < HttpUrlConstance.title.length; i++) {
                MovieBean movieBean = new MovieBean(HttpUrlConstance.title[i], HttpUrlConstance.url[i]);
                movieAdapter.addData(movieBean);
            }
        }


    }

    @Override
    public void setOnItemClickListener(View v, int position) {
//        jumpActivity(mDatas.get(position).getUrl());
        Intent intent = new Intent(getActivity(), WebViewActivity.class);
        intent.putExtra("URL", movieAdapter.getItem(position).getUrl());
        startActivity(intent);
    }
}
