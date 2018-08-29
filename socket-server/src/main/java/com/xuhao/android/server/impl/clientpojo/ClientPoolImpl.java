package com.xuhao.android.server.impl.clientpojo;

import com.xuhao.android.common.interfacies.client.msg.ISendable;
import com.xuhao.android.common.interfacies.server.IClient;
import com.xuhao.android.common.interfacies.server.IClientPool;
import com.xuhao.android.server.exceptions.CacheException;

public class ClientPoolImpl extends AbsClientPool<String, IClient> implements IClientPool<IClient, String> {

    public ClientPoolImpl(int capacity) {
        super(capacity);
    }

    @Override
    public void cache(IClient client) {
        super.set(client.getUniqueTag(), client);
    }

    @Override
    public IClient findByUniqueTag(String tag) {
        return get(tag);
    }

    public void unCache(IClient iClient) {
        remove(iClient.getUniqueTag());
    }

    public void unCache(String key) {
        remove(key);
    }

    @Override
    public int size() {
        return super.size();
    }

    @Override
    public void sendToAll(final ISendable sendable) {
        echoRun(new Echo<String, IClient>() {
            @Override
            public void onEcho(String key, IClient value) {
                value.send(sendable);
            }
        });
    }

    @Override
    void onCacheFull(String key, IClient lastOne) {
        lastOne.disconnect(new CacheException("cache is full,you need remove"));
        unCache(lastOne);
    }

    @Override
    void onCacheDuplicate(String key, IClient oldOne) {
        oldOne.disconnect(new CacheException("there are cached in this server.it need removed before new cache"));
        unCache(oldOne);
    }

    @Override
    public void onCacheEmpty() {
        //do nothing
    }
}
