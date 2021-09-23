package com.strivve;

import java.io.IOException;

class CardsavrEncryptionException extends IOException {

    private static final long serialVersionUID = -3307146660680085641L;

    public CardsavrEncryptionException(String message, Throwable e) {
        super(message, e);
    }

}