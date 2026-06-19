# SSH + SFTP Client — Neobrutalism

Aplikasi Android native SSH + SFTP Client dengan tema Neobrutalism.
Dibuat dengan Kotlin + Jetpack Compose.

---

## Fitur

- **SSH Terminal** — full terminal dengan scrollback, ANSI stripping, tombol Ctrl/Alt/Tab/Esc/F1-F12
- **SFTP File Manager** — dual pane (lokal | remote), upload/download, rename, delete, chmod
- **Saved Sessions** — enkripsi password dengan Android Keystore + SQLCipher
- **Multi-tab** — maksimal 5 session simultan
- **Neobrutalism UI** — border tebal, shadow tegas, warna solid, zero border-radius

---

## Cara Build (Android Studio)

### Requirements
- Android Studio Hedgehog (2023.1.1) atau lebih baru
- JDK 17
- Android SDK API 34
- Gradle 8.3

### Steps

1. **Buka project**
   ```
   File → Open → pilih folder sshftp ini
   ```

2. **Sync Gradle**
   ```
   File → Sync Project with Gradle Files
   ```
   Tunggu sampai semua dependencies di-download (JSch, SQLCipher, Room, Compose BOM).

3. **Build APK**
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```
   APK output: `app/build/outputs/apk/debug/app-debug.apk`

4. **Install ke device**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```
   Atau langsung Run di Android Studio dengan device/emulator.

---

## Struktur Project

```
app/src/main/java/com/fidzzcodex/sshftp/
├── MainActivity.kt                 # Entry point
├── data/
│   ├── db/AppDatabase.kt          # Room + SQLCipher
│   ├── model/Models.kt            # Data classes
│   └── repository/SessionRepository.kt
├── ssh/
│   ├── SSHManager.kt              # JSch wrapper (SSH + SFTP)
│   └── SSHService.kt              # Foreground service
├── ui/
│   ├── AppViewModel.kt            # Shared state
│   ├── components/NeoComponents.kt # Reusable UI (NeoButton, NeoCard, dll)
│   ├── navigation/AppNavigation.kt
│   ├── screens/
│   │   ├── SessionsScreen.kt
│   │   ├── FileManagerScreen.kt
│   │   ├── TerminalScreen.kt
│   │   └── SettingsDialog.kt
│   └── theme/Theme.kt             # Neobrutalism theme system
└── util/CryptoManager.kt          # Android Keystore AES-256
```

---

## Dependencies Utama

| Library | Versi | Fungsi |
|---|---|---|
| Jetpack Compose BOM | 2023.10.01 | UI framework |
| JSch (mwiede fork) | 0.2.17 | SSH/SFTP |
| Room | 2.6.0 | Database lokal |
| SQLCipher | 4.5.4 | Enkripsi database |
| Security Crypto | 1.1.0-alpha06 | Android Keystore |
| WorkManager | 2.8.1 | Background tasks |

---

## Catatan

- Semua data SSH tersimpan **lokal di device**, tidak dikirim ke server manapun
- Password dienkripsi dengan **Android Keystore AES-256-CBC**
- Database dienkripsi dengan **SQLCipher**
- Minimal SDK: API 24 (Android 7.0)
- Target SDK: API 34 (Android 14)

---

## Package Name

Default: `com.fidzzcodex.sshftp`

Untuk ganti package name:
1. Edit `applicationId` di `app/build.gradle`
2. Refactor package di Android Studio: klik kanan folder → Refactor → Rename
