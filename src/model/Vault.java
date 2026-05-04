package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Vault implements Serializable {

    private List<PasswordEntry> entries;
    private List<byte[]> previousHashes;

    public Vault() {
        entries = new ArrayList<>();
        previousHashes = new ArrayList<>();
    }

    public List<PasswordEntry> getEntries() {
        return entries;
    }
    
    public List<byte[]> getPreviousHashes() {
        return previousHashes;
    }
    
    public void addHash(byte[] hash) {
        previousHashes.add(hash);
    }
}