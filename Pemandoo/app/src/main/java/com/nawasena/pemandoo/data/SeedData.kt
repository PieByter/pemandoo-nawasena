package com.nawasena.pemandoo.data

import com.nawasena.pemandoo.BuildConfig

/**
 * Landmark bawaan aplikasi. Data disimpan lokal supaya tur audio tetap bisa
 * digunakan tanpa koneksi internet. Foto-foto berasal dari Wikimedia Commons
 * dan sudah disertakan di dalam aplikasi.
 */
object SeedData {

    private val photoBase = "android.resource://${BuildConfig.APPLICATION_ID}/drawable/"

    val landmarks: List<Landmark> = listOf(
        Landmark(
            id = "tjong-a-fie-museum",
            name = "Museum Tjong A Fie",
            latitude = 3.585744,
            longitude = 98.680204,
            photo = photoBase + "tjong_a_fie",
            overview = "Museum Tjong A Fie adalah rumah tinggal dua lantai yang terletak di Jalan Jenderal Ahmad Yani, Kesawan, Medan. Rumah ini dibangun pada tahun 1900 oleh Tjong A Fie, seorang pedagang Hakka yang sangat berpengaruh di Sumatera Utara. Kini sebagian besar ruangan rumah dibuka untuk umum sebagai museum yang menceritakan kehidupan, bisnis, dan budaya keluarga Tjong A Fie.",
            architecturalFeatures = "Bangunan ini memadukan gaya arsitektur Tionghoa, Eropa, Melayu, dan art deco. Perabotan kayu, ubin lantai, lukisan, serta ornamen pintu dan jendela masih terawat dan memberi gambaran jelas tentang gaya hidup keluarga pedagang pada awal abad ke-20.",
            interestingFacts = "Tjong A Fie memiliki hubungan keluarga dengan Cheong Fatt Tze, pemilik Cheong Fatt Tze Mansion di Penang, Malaysia. Karena itu, pengunjung sering menemukan kemiripan pada gaya bangunan kedua rumah. Di dalam museum juga terdapat koleksi foto lama dan benda pribadi keluarga.",
            historicalSignificance = "Tjong A Fie adalah tokoh penting dalam perkembangan ekonomi dan sosial Medan. Rumah ini menjadi saksi sejarah komunitas Tionghoa di Kota Medan dan telah ditetapkan sebagai salah satu bangunan cagar budaya yang dilindungi.",
            hours = "09.00 - 17.00 WIB setiap hari",
            entryFee = "sekitar Rp35.000 untuk dewasa",
            radiusMeters = 100f
        ),
        Landmark(
            id = "istana-maimun",
            name = "Istana Maimun",
            latitude = 3.575305,
            longitude = 98.683899,
            photo = photoBase + "istana_maimun",
            overview = "Istana Maimun adalah istana Kesultanan Deli yang berdiri di Jalan Brigadir Jenderal Katamso, Medan. Istana ini dibangun pada tahun 1888 atas perintah Sultan Makmun Al Rasyid Perkasa Alamsyah dan sampai sekarang masih menjadi salah satu ikon paling dikenal dari Kota Medan.",
            architecturalFeatures = "Istana Maimun didominasi warna kuning keemasan dengan atap berbentuk kubah dan menara. Bangunan ini memadukan unsur Melayu, Islam, Spanyol, India, dan Italia. Di dalamnya terdapat ruang balairung dengan singgasana, lampu kristal, serta perabotan bergaya Eropa.",
            interestingFacts = "Bangunan istana memiliki lebih dari tiga puluh ruangan, tetapi hanya sebagian yang dibuka untuk wisatawan. Warna kuning yang mendominasi istana melambangkan kebesaran dan kejayaan Kesultanan Deli. Di halaman istana sering diadakan pertunjukan budaya dan foto bersama keluarga kesultanan.",
            historicalSignificance = "Istana Maimun menjadi pusat pemerintahan Kesultanan Deli pada masa kolonial. Sampai hari ini istana masih digunakan untuk upacara adat dan kegiatan budaya, sehingga menjadi living monument yang menghubungkan masa lalu dengan kehidupan masyarakat Medan sekarang.",
            hours = "08.00 - 17.00 WIB",
            entryFee = "sekitar Rp10.000 - Rp20.000",
            radiusMeters = 120f
        ),
        Landmark(
            id = "masjid-raya-al-mashun",
            name = "Masjid Raya Al-Mashun",
            latitude = 3.575087,
            longitude = 98.687065,
            photo = photoBase + "masjid_raya_medan",
            overview = "Masjid Raya Al-Mashun berdiri di Jalan Sisingamangaraja, tidak jauh dari Istana Maimun. Masjid ini dibangun pada tahun 1906 dan menjadi salah satu masjid tertua serta terbesar di Kota Medan. Arsitekturnya yang megah membuat masjid ini menjadi destinasi wisata religi yang penting.",
            architecturalFeatures = "Masjid berbentuk segi delapan dengan satu kubah besar di tengah dan empat kubah kecil di sudut-sudutnya. Interiornya dihiasi marmer, lampu gantung, dan kaligrafi. Sentuhan gaya Timur Tengah, India, dan Eropa terlihat pada lengkungan serta menara masjid.",
            interestingFacts = "Masjid ini dibangun oleh arsitek asal Belanda dengan pengaruh arsitektur Mughal. Pada masa pembangunannya, Sultan Deli menyumbangkan lahan dan dana yang besar. Hingga kini masjid masih aktif digunakan untuk salat berjamaah dan kegiatan keagamaan.",
            historicalSignificance = "Masjid Raya Al-Mashun merupakan bagian dari kompleks Kesultanan Deli dan menjadi simbol toleransi serta perkembangan Islam di Sumatera Utara. Bangunan ini juga tercatat sebagai cagar budaya yang dilindungi pemerintah.",
            hours = "Setiap hari, di luar waktu salat",
            entryFee = "Gratis, berpakaian sopan",
            radiusMeters = 120f
        ),
        Landmark(
            id = "vihara-gunung-timur",
            name = "Vihara Gunung Timur",
            latitude = 3.582318,
            longitude = 98.681471,
            photo = photoBase + "vihara_gunung_timur",
            overview = "Vihara Gunung Timur terletak di Jalan Hang Tuah, Medan, dan merupakan salah satu vihara tertua di kota ini. Vihara ini menjadi pusat ibadah umat Tionghoa sekaligus tempat wisata budaya yang ramai dikunjungi, terutama menjelang Tahun Baru Imlek.",
            architecturalFeatures = "Bangunan vihara didominasi warna merah dan emas khas arsitektur Tionghoa. Atapnya berbentuk melengkung dengan ornamen naga, lentera, dan patung dewa. Di bagian tengah terdapat halaman luas dengan tempat pembakaran dupa serta kolam kecil.",
            interestingFacts = "Vihara Gunung Timur sering menjadi lokasi perayaan Cap Go Meh dan sembahyang bersama. Meskipun berada di tengah kota, suasana di dalam vihara terasa tenang karena dikelilingi tembok dan pepohonan. Pengunjung dapat melihat patung dewa dan ornamen khas Tionghoa yang detail.",
            historicalSignificance = "Vihara ini berdiri sejak awal abad ke-20 dan menjadi saksi perjalanan komunitas Tionghoa di Medan. Keberadaannya menunjukkan keberagaman budaya dan agama yang hidup berdampingan di kota ini.",
            hours = "06.00 - 18.00 WIB",
            entryFee = "Gratis",
            radiusMeters = 100f
        ),
        Landmark(
            id = "graha-annai-velangkanni",
            name = "Graha Maria Annai Velangkanni",
            latitude = 3.545880,
            longitude = 98.611030,
            photo = photoBase + "graha_annai",
            overview = "Graha Maria Annai Velangkanni adalah tempat ziarah Katolik yang terletak di Tanjung Selamat, Medan. Tempat ibadah ini dibangun dengan gaya arsitektur yang unik dan sering disebut sebagai gereja bergaya kuil India. Nama Annai Velangkanni merujuk pada Bunda Maria yang menampakkan diri di Velankanni, India.",
            architecturalFeatures = "Bangunan ini memiliki tujuh tingkat yang melambangkan tujuh sakramen, dengan atap berbentuk kubah dan menara seperti candi. Dindingnya dihiasi relief dan lukisan yang menceritakan kisah Yesus serta perjalanan Bunda Maria. Warna dominan putih dan krem memberi kesan damai.",
            interestingFacts = "Graha Annai Velangkanni menerima pengunjung dari berbagai latar belakang agama. Di halaman terdapat patung Bunda Maria setinggi beberapa meter dan kolam air yang dipercaya membawa berkat. Tempat ini juga menjadi lokasi favorit untuk berfoto karena arsitekturnya yang megah.",
            historicalSignificance = "Gedung ini dibangun oleh Romo James Bharataputra pada awal tahun 2000-an dan menjadi pusat devosi Maria Annai Velangkanni di Indonesia. Kehadirannya memperkaya keragaman religi dan budaya Kota Medan.",
            hours = "08.00 - 18.00 WIB",
            entryFee = "Gratis, donasi sukarela",
            radiusMeters = 150f
        ),
        Landmark(
            id = "kuil-sri-mariamman",
            name = "Kuil Sri Mariamman",
            latitude = 3.584390,
            longitude = 98.679690,
            photo = photoBase + "sri_mariamman",
            overview = "Kuil Sri Mariamman berada di Jalan Zainul Arifin, Medan, dan merupakan kuil Hindu tertua di kota ini. Kuil ini dibangun oleh komunitas Tamil pada akhir abad ke-19 dan masih digunakan untuk ibadah hingga sekarang.",
            architecturalFeatures = "Kuil didominasi warna cerah dengan menara gopuram yang dipenuhi patung dewa dan ornamen bunga. Bagian dalam kuil memiliki ruang utama untuk Dewi Mariamman serta sejumlah ruang kecil untuk dewa lainnya. Bau dupa dan bunga melati menambah suasana khas India Selatan.",
            interestingFacts = "Kuil ini menjadi pusat perayaan Deepavali dan Pongal di Medan. Pengunjung non-Hindu diperbolehkan masuk dengan sopan, namun harus melepas alas kaki. Fotografi di area tertentu biasanya dibatasi selama upacara berlangsung.",
            historicalSignificance = "Kuil Sri Mariamman menjadi bukti sejarah komunitas Tamil di Medan yang sudah ada sejak masa kolonial. Bangunan ini termasuk cagar budaya dan menjadi salah satu destinasi wisata budaya lintas agama.",
            hours = "06.00 - 12.00 dan 17.00 - 20.00 WIB",
            entryFee = "Gratis",
            radiusMeters = 100f
        )
    )
}
