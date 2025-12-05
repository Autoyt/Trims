package dev.auto.trims.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WeightedPicker<T> {

    private static class Entry<T> {
        final T value;
        final int weight;
        Entry(T value, int weight) {
            this.value = value;
            this.weight = weight;
        }
    }

    private final List<Entry<T>> entries = new ArrayList<>();
    private int totalWeight = 0;

    public void add(T value, int weight) {
        if (weight <= 0) return;
        entries.add(new Entry<>(value, weight));
        totalWeight += weight;
    }

    public T pick() {
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int running = 0;

        for (Entry<T> e : entries) {
            running += e.weight;
            if (roll < running) {
                return e.value;
            }
        }

        return entries.get(entries.size() - 1).value;
    }
}
