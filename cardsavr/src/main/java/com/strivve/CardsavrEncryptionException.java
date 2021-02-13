package com.strivve;

import java.io.IOException;

class CarsavrEncryptionException extends IOException {

    private static final long serialVersionUID = -3307146660680085641L;

    public CarsavrEncryptionException(String message, Throwable e) {
        super(message, e);
    }

}