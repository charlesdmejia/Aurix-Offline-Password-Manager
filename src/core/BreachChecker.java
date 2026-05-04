package core;
 
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
 
public class BreachChecker {
 
    public static boolean isBreached(String password) {
        try (BufferedReader reader =
                new BufferedReader(new FileReader("common_passwords.txt"))) {
 
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals(password)) {
                    return true;
                }
            }
        } catch (IOException e) {
          
            AppLogger.log("BreachChecker error: " + e.getMessage());
        }
        return false;
    }
}

