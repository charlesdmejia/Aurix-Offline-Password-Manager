package model;

import java.io.Serializable;

public class VaultMeta implements Serializable {
    private byte[] salt;

    public VaultMeta(byte[] salt) {
        this.salt = salt;
    }

    public byte[] getSalt() {
        return salt;
    }
}                