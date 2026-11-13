# Pemandoo : Explore The Nusantara Raya
## GEMASTIK XVII Perangkat Lunak
### Meet Our Team NAWASENA : GEMASTIK24-910045816

| No | Name                      | NIM         | Roles          |
|:--:|:--------------------------|:------------|:---------------|
|1   | Vio Aprivia Nugraha       | 211401058   | Team Leader    |
|2   | Pieter Tanoto             | 211401015   | Member         |
|3   | Habel Febrian Sitanggang  | 211401073   | Member         |

### UNIVERSITAS SUMATERA UTARA
### Tahun 2024
### Dosen Pembimbing: Fuzy Yustika Manik S.Kom., M.Kom (198710152019032010)

## Daftar Isi
- [Perkenalan Singkat](#perkenalan-singkat)
- [Versi Terbaru](#versi-terbaru)
- [Fitur](#fitur)
- [Build & Jalankan](#build--jalankan)
- [Catatan Firebase](#catatan-firebase)
- [Perangkat Pengembangan](#perangkat-pengembangan)

## Perkenalan Singkat
Pemandoo adalah aplikasi inovatif berbasis Android yang menghadirkan pengalaman tur audio interaktif bagi wisatawan. Dengan teknologi text-to-speech, Pemandoo memberikan informasi mendalam tentang tempat wisata, memanfaatkan geofence untuk notifikasi lokasi wisata terdekat, dan menampilkan destinasi di sekitar pengguna pada peta.

## Versi Terbaru
**Versi 1.1.0 (versionCode 2)**

Perubahan utama:
- Data tempat wisata sekarang tersimpan **offline-first** di Room, tidak lagi bergantung pada API Heroku yang sudah tidak aktif.
- Enam destinasi bawaan di Medan beserta foto, deskripsi, jam buka, dan harga tiket.
- Daftar rekomendasi terdekat dengan jarak real-time dan pemilihan destinasi.
- Tur audio Bahasa Indonesia dengan tombol putar/jeda dan penanganan narasi panjang.
- CRUD tempat wisata: tambah, edit, dan hapus langsung dari perangkat.
- Notifikasi geofence saat memasuki radius destinasi (termasuk dukungan izin lokasi latar belakang).
- Halaman Panduan dan Tentang Pemandoo, pengaturan bahasa suara, edit profil, dan reset kata sandi.
- Perbaikan crash pada peta, navigasi, ViewModel, kamera, dan pemuatan data.
- Edge-to-edge Android 15 ditangani sehingga konten tidak tertutup status bar/navigation bar.

### Rilis lama
[Unduh APK rilis 2024 (sebelum perbaikan)](https://github.com/PieByter/Nawasena-Pemandoo-Gemastik/releases/download/prelease/pemandoo.apk)

APK terbaru dihasilkan dari source ini melalui langkah build di bawah.

## Fitur
1. **Peta & rekomendasi terdekat** — marker dan radius geofence untuk setiap destinasi.
2. **Tur audio** — Text-to-Speech Bahasa Indonesia (fallback ke bahasa perangkat).
3. **Detail destinasi** — arsitektur, fakta menarik, nilai sejarah, jam buka, dan tiket.
4. **Kelola destinasi** — tambah destinasi baru dengan foto kamera/galeri, edit, dan hapus.
5. **Geofence** — notifikasi “Kamu tiba di …” dan tombol untuk langsung membuka tur.
6. **Panduan & pengaturan** — panduan penggunaan, bahasa suara, profil, dan tentang aplikasi.
7. **Mode tamu** — seluruh fitur inti tetap bisa dipakai tanpa login.

## Build & Jalankan
Prasyarat:
- JDK 17
- Android SDK Platform 35 dan Build-Tools 35.0.0
- Android Studio Ladybug/terbaru atau Gradle 8.9 (wrapper sudah disertakan)

```bash
cd Pemandoo
./gradlew :app:assembleDebug
```

APK debug dihasilkan di:

```
Pemandoo/app/build/outputs/apk/debug/app-debug.apk
```

Untuk memasang ke perangkat/emulator:

```bash
adb install -r Pemandoo/app/build/outputs/apk/debug/app-debug.apk
```

## Catatan Firebase
Fitur login Firebase (email/password dan Google) membutuhkan **SHA-1 keystore build** yang terdaftar pada project Firebase `pemandoo`:

1. Dapatkan SHA-1 debug:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
2. Buka Firebase Console → Project settings → Your apps → Android app `com.nawasena.pemandoo`.
3. Tambahkan SHA-1 tersebut, lalu unduh ulang `google-services.json` bila perlu.

Jika SHA-1 belum terdaftar, Firebase akan memblokir permintaan login dari build tersebut. Aplikasi menampilkan dialog yang jelas dan menawarkan **mode tamu**; peta, tur audio, geofence, dan penyimpanan lokal tetap berfungsi penuh.

## Perangkat Pengembangan
Pemandoo dikembangkan menggunakan:
1. Android Studio
2. Kotlin + Android Jetpack (ViewModel, LiveData, Navigation, Room)
3. Google Maps Platform
4. Google Play Services Location & Geofencing
5. Android Text-to-Speech
6. Firebase Authentication & Cloud Firestore
7. Glide, Material Components, Lottie
8. Figma dan Adobe Illustrator

### Sumber Data & Foto
Data destinasi disusun sebagai konten lokal aplikasi. Foto destinasi berasal dari Wikimedia Commons dan disertakan sebagai aset aplikasi; kredit sumber ditampilkan pada halaman **Tentang Pemandoo**.
