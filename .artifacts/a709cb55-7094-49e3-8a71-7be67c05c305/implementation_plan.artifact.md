# Mengubah Tampilan Pemilihan Tema Menjadi Horizontal Scroll di Pengaturan

Mengubah UI pemilihan tema di layar Pengaturan Tampilan agar lebih hemat ruang dengan menggunakan *horizontal scroll* (2 baris), sementara tetap mempertahankan tampilan grid 4 kolom di layar *Onboarding*.

## User Review Required

> [!IMPORTANT]
> Perubahan ini hanya akan berdampak pada **Pengaturan > Tampilan**. Layar **Selamat Datang (Onboarding)** tidak akan berubah sesuai permintaan Anda.

## Proposed Changes

### [Component] UI Presentation Widgets

#### [MODIFY] [AppThemePreferenceWidget.kt](file:///D:/Android-Dev/Proyek/rout-komik/app/src/main/java/eu/kanade/presentation/more/settings/widget/AppThemePreferenceWidget.kt)
- Menambahkan parameter `isGrid: Boolean = true` pada `AppThemePreferenceWidget` dan `AppThemesList`.
- Memperbarui `AppThemesList` untuk merender `LazyRow` (scroll horizontal) jika `isGrid = false`.
- Dalam mode horizontal, tema akan dikelompokkan menjadi 2 item per kolom (2 baris vertikal).
- Menyesuaikan lebar item dalam mode horizontal agar proporsional.

### [Component] Settings Screen

#### [MODIFY] [SettingsAppearanceScreen.kt](file:///D:/Android-Dev/Proyek/rout-komik/app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsAppearanceScreen.kt)
- Mengirimkan `isGrid = false` saat memanggil `AppThemePreferenceWidget` agar menggunakan tampilan scroll horizontal baru.

## Verification Plan

### Manual Verification
- Buka aplikasi dan masuk ke **Pengaturan > Tampilan**.
- Pastikan daftar tema sekarang dapat digeser ke samping (horizontal) dan tersusun dalam 2 baris.
- Cek layar **Selamat Datang** (jika bisa diakses kembali via hapus data/onboarding manual) untuk memastikan tampilan tema di sana tetap 4 kolom vertikal.
- Pastikan pemilihan tema tetap berfungsi (tema berubah saat diklik).
