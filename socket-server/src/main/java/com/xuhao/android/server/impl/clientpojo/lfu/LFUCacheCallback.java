package com.xuhao.android.server.impl.clientpojo.lfu;

public interface LFUCacheCallback<K, V> {
    /**
     * when entry was added
     *
     * @param cachedValue the entry which was added
     */
    void onCacheAdd(LFUCacheEntry<K, V> cachedValue);

    /**
     * when entry was removed
     *
     * @param cachedValue the entry which was deleted
     */
    void onCacheRemove(LFUCacheEntry<K, V> cachedValue);

    /**
     * when cache is full,must remove entry
     *
     * @param cachedValue gonna remove Entry
     */
    void onCacheFull(LFUCacheEntry<K, V> cachedValue);

    /**
     * when cache is totally empty
     */
    void onCacheEmpty();

}
