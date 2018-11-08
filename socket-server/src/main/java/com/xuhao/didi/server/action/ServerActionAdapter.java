package com.xuhao.didi.server.action;

import android.content.Context;

import com.xuhao.didi.common.common_interfacies.server.IClient;
import com.xuhao.didi.common.common_interfacies.server.IClientPool;
import com.xuhao.didi.common.common_interfacies.server.IServerActionListener;
import com.xuhao.didi.common.common_interfacies.server.IServerShutdown;

public abstract class ServerActionAdapter implements IServerActionListener {
    @Override
    public void onServerListening(Context context, int serverPort) {

    }

    @Override
    public void onClientConnected(Context context, IClient client, int serverPort, IClientPool clientPool) {

    }

    @Override
    public void onClientDisconnected(Context context, IClient client, int serverPort, IClientPool clientPool) {

    }

    @Override
    public void onServerWillBeShutdown(Context context, int serverPort, IServerShutdown shutdown, IClientPool clientPool, Throwable throwable) {

    }

    @Override
    public void onServerAlreadyShutdown(Context context, int serverPort) {

    }
}
