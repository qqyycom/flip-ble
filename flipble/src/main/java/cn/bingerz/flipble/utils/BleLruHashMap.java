package cn.bingerz.flipble.utils;


import java.util.LinkedHashMap;

import cn.bingerz.flipble.peripheral.Peripheral;

public class BleLruHashMap<K, V> extends LinkedHashMap<K, V> {

    private final int MAX_SIZE;

    public BleLruHashMap(int saveSize) {
        super((int) Math.ceil(saveSize / 0.75) + 1, 0.75f, true);
        MAX_SIZE = saveSize;
    }

    @Override
    protected boolean removeEldestEntry(Entry eldest) {
        if (size() > MAX_SIZE && eldest.getValue() instanceof Peripheral) {
            ((Peripheral) eldest.getValue()).disconnect();
        }
        return size() > MAX_SIZE;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Entry<K, V> entry : entrySet()) {
            sb.append(String.format("%s:%s ", entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }

}