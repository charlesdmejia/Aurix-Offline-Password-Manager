# Aurix 

A secure, offline desktop password manager built with **Java** and **JavaFX**, featuring AES encryption, brute-force protection, password breach detection, and an animated dark-themed UI.


## Screenshots
<img width="1919" height="1030" alt="1" src="https://github.com/user-attachments/assets/261f2f35-29e8-4ca7-8846-934a4a5e4204" />
<img width="1919" height="1029" alt="2" src="https://github.com/user-attachments/assets/ca06d275-335b-4f4f-9236-43947c955175" />
<img width="1919" height="1031" alt="4" src="https://github.com/user-attachments/assets/c14d9bcd-fbed-480a-a81b-2e9ee88918ff" />
<img width="1919" height="1029" alt="5" src="https://github.com/user-attachments/assets/b853480c-6fb2-4f26-9dd2-7a07252e79db" />

---

## Features

- **AES-256 Encrypted Vault** — All passwords are encrypted at rest
- **PBKDF2 Key Derivation** — Master password is never stored directly
- **Brute-Force Protection** — Automatic lockout after repeated failed attempts
- **One-Time Recovery Key** — Generated on vault creation for account recovery
- **Breach Detection** — Warns you if a password is commonly known/compromised
- **Password History** — Prevents reuse of previously used passwords
- **Search & Filter** — Search entries by website, username, or category
- **Password Generator** — Customizable random password generator
- **Auto-Clear Clipboard** — Optionally clears clipboard after a set time
- **Ephemeral Reveal** — Double-click to reveal a password for 10 seconds (requires re-authentication)
- **Animated Dark UI** — Particle background, smooth hover animations

---

## Project Structure

```
Password Manager/
├── src/
│   ├── gui/
│   │   ├── MainApp.java             # Main UI and application entry point
│   │   └── styles.css               # JavaFX stylesheet
│   ├── core/
│   │   ├── BreachChecker.java       # Common password breach detection
│   │   ├── AccessLimiter.java       # Login attempt limiter
│   │   ├── AesGcmCipher.java        # AES-GCM encryption/decryption
│   │   ├── AppLogger.java           # App-wide logging
│   │   ├── Key Derivation.java      # PBKDF2 hashing and AES key generation
│   │   ├── VaultSearch.java         # Vault search logic
│   │   ├── VaultConfig.java         # Security constants and configuration
│   │   ├── SecurityInfo.java        # Security explanation text
│   │   ├── VaultRepo.java           # Encrypted file I/O
│   │   └── IntegrityVerifier.java   # HMAC integrity verification
│   └── model/
│       ├── PasswordEntry.java       # Single password record
│       ├── Vault.java               # Collection of entries + hash history
│       └── VaultMeta.java           # Salt and vault metadata
├── .gitignore
├── LICENSE
├── README.md
└── common_passwords.txt
```

---

## Getting Started

### Prerequisites

- Java 17 or higher
- JavaFX 17 or higher
- Any Java IDE (IntelliJ IDEA, Eclipse, VS Code with Java extensions)

### Running the App

1. **Clone the repository**
   ```bash
   git clone https://github.com/YOUR_USERNAME/aurix.git
   cd aurix
   ```

2. **Add JavaFX to your module path.** Download JavaFX SDK and configure your IDE or run:
   ```bash
   java --module-path /path/to/javafx-sdk/lib \
        --add-modules javafx.controls,javafx.fxml \
        -cp out gui.MainApp
   ```

3. **Compile and run** from your IDE by setting the JavaFX SDK as a library and running `MainApp.java` as the main class.

---

## Security Design

| Layer | Technology |
|---|---|
| Encryption | AES-256 (GCM mode) |
| Key Derivation | PBKDF2WithHmacSHA256 |
| Salt | Randomly generated per vault |
| Password Hashing | SHA-256 with salt (history check) |
| Breach Detection | Local blocklist of common passwords |
| Brute Force | Exponential backoff lockout |

Your master password is **never stored**. The vault file can only be decrypted with the correct password. If lost, access can be restored only with the one-time recovery key shown at vault creation.

---

## Contributors

This is a joint academic project developed collaboratively by:

| Name | Role |
|---|---|
| Baniqued, Shauri Mae     | UI Design & Animation |
| Brioso, Jasmine Myeisha  | Security Engineering & Data Architecture |
| Diamante III, Benjamin   | UI Design & Animation |
| Mejia, Charles Daniel    | Security Engineering & Data Architecture |
| Mayuga, Samantha Nicole  | UI Design |

---

## License
This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.
