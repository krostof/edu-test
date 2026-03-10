package com.edutest.webserver.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Pomocniczy skrypt do generowania hashów BCrypt dla testowych użytkowników
 *
 * Uruchom: mvn exec:java -Dexec.mainClass="com.edutest.webserver.util.BCryptHashGenerator" -Dexec.classpathScope=compile
 */
public class BCryptHashGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String[] passwords = {"Test123!", "admin123", "teacher123", "student123"};

        System.out.println("=== BCrypt Hash Generator ===\n");

        for (String password : passwords) {
            String hash = encoder.encode(password);
            System.out.println("Hasło: " + password);
            System.out.println("Hash:  " + hash);
            System.out.println("Weryfikacja: " + encoder.matches(password, hash));
            System.out.println("---");
        }

        // Generuj hash dla domyślnego hasła Test123!
        System.out.println("\n=== Hash do użycia w SQL ===");
        String defaultHash = encoder.encode("Test123!");
        System.out.println(defaultHash);
    }
}

