package com.ttt.session.model;

public enum PlayerSymbol {
    X, O;

    public PlayerSymbol opponent() {
        return this == X ? O : X;
    }
}
