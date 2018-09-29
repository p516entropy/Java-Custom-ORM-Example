package com.lungesoft.custom.orm.entity.meta;

public final class Pair<S, T> {

    private final S first;
    private final T second;

    public Pair(S first, T second) {
        this.first = first;
        this.second = second;
    }

    public static <S, T> Pair<S, T> of(S first, T second) {
        return new Pair<>(first, second);
    }

    public S getFirst() {
        return first;
    }

    public T getSecond() {
        return second;
    }


}