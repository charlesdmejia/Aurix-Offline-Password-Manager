package core;

import model.PasswordEntry;
import model.Vault;
import java.util.ArrayList;
import java.util.List;

public class VaultSearch {

    public List<PasswordEntry> searchByWebsite(Vault vault, String website) {
    	
        List<PasswordEntry> results = new ArrayList<>();

        for (PasswordEntry entry : vault.getEntries()) {

            if (entry.getWebsite().toLowerCase().contains(website.toLowerCase())) {
                results.add(entry);
            }

        }

        return results;
    }

    
    
    public List<PasswordEntry> searchByUsername(Vault vault, String username) {

        List<PasswordEntry> results = new ArrayList<>();

        for (PasswordEntry entry : vault.getEntries()) {

            if (entry.getUsername().toLowerCase().contains(username.toLowerCase())) {
                results.add(entry);
            }

        }

        return results;
    }

    
    
    public List<PasswordEntry> searchByCategory(Vault vault, String category) {

        List<PasswordEntry> results = new ArrayList<>();

        for (PasswordEntry entry : vault.getEntries()) {

            if (entry.getCategory().equalsIgnoreCase(category)) {
                results.add(entry);
            }

        }

        return results;
    }

}