package com.bignerdranch.android.photogallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.webkit.WebView;

/**
 * Created by albus on 27-Dec-17.
 */

public class PhotoPageActivity extends SingleFragmentActivity implements PhotoPageFragment.Callbacks {

    private WebView mWebView;

    public static Intent newIntent(Context context, Uri photoPageUri) {
        Intent i = new Intent(context, PhotoPageActivity.class);
        i.setData(photoPageUri);
        return i;
    }

    @Override
    protected Fragment createFragment() {
        return PhotoPageFragment.newInstance(getIntent().getData());
    }


    public void addBrowsingHistory(WebView mWebView){
        this.mWebView = mWebView;

    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        if(this.mWebView.canGoBack()){
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

