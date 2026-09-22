# Ubah Default Tab Jelajah (Daftar Konten/Feed)

Mengubah nilai bawaan (default) pada pengaturan aplikasi agar tab **"Daftar Konten"** (yang merupakan terjemahan dari **Feed/Terbaru** dalam Bahasa Indonesia) menjadi tab pertama saat membuka menu **Jelajah** (Browse).

## Perubahan yang Diusulkan

### UI & Domain

#### [MODIFY] [UiPreferences.kt](file:///D:/Android-Dev/Proyek/rout-komik/app/src/main/java/eu/kanade/domain/ui/UiPreferences.kt)

Mengubah nilai default `feedTabInFront` (kunci preferensi: `latest_tab_position`) dari `false` menjadi `true`. Hal ini akan membuat tab Feed (Daftar Konten) berada di posisi paling depan (index 0) secara default.

## Rencana Verifikasi

### Manual Verification
1. Menghapus data aplikasi atau melakukan instalasi baru.
2. Membuka menu **Jelajah**.
3. Memastikan tab **"Daftar Konten"** (Feed) adalah tab yang pertama muncul dan aktif.
4. Membuka **Setelan > Jelajah** dan memastikan opsi **"Posisi tab Daftar Konten"** dalam keadaan aktif (ON) secara default.

### Build Verification
- Menjalankan `./gradlew spotlessApply` untuk memastikan format kode benar.
- Menjalankan `./gradlew :app:compileDebugKotlin` untuk memastikan kode dapat dikompilasi.
