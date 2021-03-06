package com.cybex.provider.websocket.rx;


import android.util.Log;

import com.cybex.provider.graphene.websocket.WebSocketBase;
import com.cybex.provider.websocket.chat.RxChatWebSocket;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * 打印log
 * @param <T>
 */
public class RxWebSocketLogger<T extends WebSocketBase> implements Subscriber<T> {

    private final String TAG;

    public RxWebSocketLogger(String tag) {
        TAG = String.format("%s-%s", RxChatWebSocket.TAG, tag);
    }

    @Override
    public void onComplete() {
        Log.d(TAG, "Complete");
    }

    @Override
    public void onError(Throwable e) {
        Log.e(TAG, "Error");
        Log.e(TAG, e.getMessage());
    }

    @Override
    public void onSubscribe(Subscription s) {
        Log.e(TAG, "Subscribe");
    }

    @Override
    public void onNext(T chat) {
        Log.d(TAG, "Next");
    }
}