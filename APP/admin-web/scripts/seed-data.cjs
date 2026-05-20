// Seed data cho ARmy Coffee Admin
// Chạy: node seed-data.cjs

const { initializeApp } = require('firebase/app');
const { getDatabase, ref, set } = require('firebase/database');

// Firebase config - cùng project với app
const firebaseConfig = {
  apiKey: "AIzaSyA3IFhIq1CGR4F2rZuW6OL6GUYGTm8RIiU",
  authDomain: "thuoc-3e916.firebaseapp.com",
  databaseURL: "https://thuoc-3e916-default-rtdb.firebaseio.com",
  projectId: "thuoc-3e916",
  storageBucket: "thuoc-3e916.appspot.com",
  messagingSenderId: "163924833754",
  appId: "1:163924833754:web:397c128d3cfe4249c2fce3"
};

// Khởi tạo Firebase
const app = initializeApp(firebaseConfig);
const db = getDatabase(app);

// Format ngày giờ Realtime Database
const formatDate = (date) => {
  const d = new Date(date);
  const day = String(d.getDate()).padStart(2, '0');
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const year = d.getFullYear();
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  const seconds = String(d.getSeconds()).padStart(2, '0');
  return `${day}-${month}-${year} ${hours}:${minutes}:${seconds}`;
};

const now = new Date();

// ==================== DỮ LIỆU MẪU ====================

const seedData = {
  // Người dùng (Admin + Nhân viên)
  NguoiDung: {
    "admin": {
      maNguoiDung: "admin",
      hoVaTen: "Nguyễn Văn Admin",
      email: "admin@gmail.com",
      matKhau: "admin123",
      chucVu: "Admin",
      gioiTinh: "Nam",
      ngaySinh: "1995-01-15",
      hinhAnhUrl: null,
      lastModified: Date.now()
    },
    "nv001": {
      maNguoiDung: "nv001",
      hoVaTen: "Trần Thị Minh",
      email: "minh.nv@gmail.com",
      matKhau: "minh123",
      chucVu: "NhanVien",
      gioiTinh: "Nữ",
      ngaySinh: "1998-06-20",
      hinhAnhUrl: null,
      lastModified: Date.now()
    },
    "nv002": {
      maNguoiDung: "nv002",
      hoVaTen: "Lê Hoàng Nam",
      email: "nam.nv@gmail.com",
      matKhau: "nam123",
      chucVu: "NhanVien",
      gioiTinh: "Nam",
      ngaySinh: "1999-03-10",
      hinhAnhUrl: null,
      lastModified: Date.now()
    },
    "nv003": {
      maNguoiDung: "nv003",
      hoVaTen: "Phạm Thị Hương",
      email: "huong.nv@gmail.com",
      matKhau: "huong123",
      chucVu: "NhanVien",
      gioiTinh: "Nữ",
      ngaySinh: "2000-08-25",
      hinhAnhUrl: null,
      lastModified: Date.now()
    },
    "kh001": {
      maNguoiDung: "kh001",
      hoVaTen: "Hoàng Minh Tuấn",
      email: "tuan.kh@gmail.com",
      matKhau: "tuan123",
      chucVu: "KhachHang",
      gioiTinh: "Nam",
      ngaySinh: "1992-11-05",
      hinhAnhUrl: null,
      lastModified: Date.now()
    },
    "kh002": {
      maNguoiDung: "kh002",
      hoVaTen: "Đỗ Thị Lan",
      email: "lan.kh@gmail.com",
      matKhau: "lan123",
      chucVu: "KhachHang",
      gioiTinh: "Nữ",
      ngaySinh: "1996-04-18",
      hinhAnhUrl: null,
      lastModified: Date.now()
    }
  },

  // Loại hàng
  LoaiHang: [
    null, // index 0 = null
    { maLoai: 1, tenLoai: "Cà phê", hasImage: false, lastModified: Date.now() },
    { maLoai: 2, tenLoai: "Trà", hasImage: false, lastModified: Date.now() },
    { maLoai: 3, tenLoai: "Nước ép", hasImage: false, lastModified: Date.now() },
    { maLoai: 4, tenLoai: "Sinh tố", hasImage: false, lastModified: Date.now() },
    { maLoai: 5, tenLoai: "Sữa chua", hasImage: false, lastModified: Date.now() },
    { maLoai: 6, tenLoai: "Bánh ngọt", hasImage: false, lastModified: Date.now() },
    { maLoai: 7, tenLoai: "Snack", hasImage: false, lastModified: Date.now() },
    { maLoai: 8, tenLoai: "Trà sữa", hasImage: false, lastModified: Date.now() },
  ],

  // Hàng hóa
  HangHoa: [
    null, // index 0 = null
    { maHangHoa: 1, tenHangHoa: "Cà phê đen", giaTien: 25000, maLoai: 1, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 2, tenHangHoa: "Cà phê sữa", giaTien: 30000, maLoai: 1, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 3, tenHangHoa: "Cà phê muối", giaTien: 35000, maLoai: 1, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 4, tenHangHoa: "Cà phê bơ", giaTien: 40000, maLoai: 1, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 5, tenHangHoa: "Espresso", giaTien: 35000, maLoai: 1, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 6, tenHangHoa: "Cappuccino", giaTien: 45000, maLoai: 1, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 7, tenHangHoa: "Latte", giaTien: 45000, maLoai: 1, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 8, tenHangHoa: "Mocha", giaTien: 50000, maLoai: 1, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 9, tenHangHoa: "Trà đen", giaTien: 20000, maLoai: 2, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 10, tenHangHoa: "Trà xanh", giaTien: 22000, maLoai: 2, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 11, tenHangHoa: "Trà gừng", giaTien: 25000, maLoai: 2, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 12, tenHangHoa: "Trà chanh", giaTien: 28000, maLoai: 2, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 13, tenHangHoa: "Trà sữa trân châu", giaTien: 35000, maLoai: 8, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 14, tenHangHoa: "Trà sữa flan", giaTien: 38000, maLoai: 8, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 15, tenHangHoa: "Nước ép cam", giaTien: 35000, maLoai: 3, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 16, tenHangHoa: "Nước ép táo", giaTien: 35000, maLoai: 3, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 17, tenHangHoa: "Nước ép dưa hấu", giaTien: 30000, maLoai: 3, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 18, tenHangHoa: "Sinh tố bơ", giaTien: 40000, maLoai: 4, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 19, tenHangHoa: "Sinh tố dâu", giaTien: 45000, maLoai: 4, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 20, tenHangHoa: "Sinh tố xoài", giaTien: 40000, maLoai: 4, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 21, tenHangHoa: "Sữa chua đánh", giaTien: 30000, maLoai: 5, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 22, tenHangHoa: "Sữa chua trái cây", giaTien: 35000, maLoai: 5, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 23, tenHangHoa: "Bánh tiramisu", giaTien: 45000, maLoai: 6, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 24, tenHangHoa: "Bánh cheese", giaTien: 40000, maLoai: 6, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 25, tenHangHoa: "Croissant", giaTien: 25000, maLoai: 6, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 26, tenHangHoa: "Khoai tây chiên", giaTien: 20000, maLoai: 7, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 27, tenHangHoa: "Gà viên", giaTien: 25000, maLoai: 7, trangThai: 1, hasImage: false, lastModified: Date.now() },
    { maHangHoa: 28, tenHangHoa: "Cà phê kem", giaTien: 45000, maLoai: 1, trangThai: 1, hasImage: false, lastModified: Date.now() },
  ],

  // Bàn (10 bàn)
  Ban: [
    null,
    { maBan: 1, trangThai: 0, lastModified: Date.now() },
    { maBan: 2, trangThai: 0, lastModified: Date.now() },
    { maBan: 3, trangThai: 0, lastModified: Date.now() },
    { maBan: 4, trangThai: 0, lastModified: Date.now() },
    { maBan: 5, trangThai: 0, lastModified: Date.now() },
    { maBan: 6, trangThai: 0, lastModified: Date.now() },
    { maBan: 7, trangThai: 0, lastModified: Date.now() },
    { maBan: 8, trangThai: 0, lastModified: Date.now() },
    { maBan: 9, trangThai: 0, lastModified: Date.now() },
    { maBan: 10, trangThai: 0, lastModified: Date.now() },
  ],
};

// Tạo hóa đơn mẫu trong 30 ngày gần đây
const HoaDon = [null];
const HoaDonChiTiet = [null];
let hdIndex = 1;
let ctIndex = 1;

for (let day = 30; day >= 0; day--) {
  // 3-8 hóa đơn mỗi ngày
  const ordersPerDay = Math.floor(Math.random() * 6) + 3;

  for (let o = 0; o < ordersPerDay; o++) {
    const orderDate = new Date(now);
    orderDate.setDate(orderDate.getDate() - day);
    orderDate.setHours(8 + Math.floor(Math.random() * 10)); // 8h - 18h
    orderDate.setMinutes(Math.floor(Math.random() * 60));

    const maBan = Math.floor(Math.random() * 10) + 1;
    const maKhachHang = Math.random() > 0.5 ? (Math.random() > 0.5 ? "kh001" : "kh002") : null;

    // 2-5 items mỗi hóa đơn
    const itemCount = Math.floor(Math.random() * 4) + 2;
    const items = [];
    let total = 0;

    for (let i = 0; i < itemCount; i++) {
      const maHangHoa = Math.floor(Math.random() * 27) + 1;
      const hangHoa = seedData.HangHoa[maHangHoa];
      if (!hangHoa) continue;

      const soLuong = Math.floor(Math.random() * 3) + 1;
      const giaTien = hangHoa.giaTien;
      total += soLuong * giaTien;

      HoaDonChiTiet.push({
        maHDCT: ctIndex,
        maHoaDon: hdIndex,
        maHangHoa: maHangHoa,
        soLuong: soLuong,
        giaTien: giaTien,
        ghiChu: ""
      });
      ctIndex++;
    }

    // Trạng thái: 0 = chưa TT, 1 = đã TT
    // Gần đây: mix; xa: hầu hết đã TT
    let trangThai = 1;
    if (day === 0) {
      trangThai = Math.random() > 0.6 ? 1 : 0; // Hôm nay: 60% đã TT
    }

    const gioRa = new Date(orderDate);
    gioRa.setMinutes(gioRa.getMinutes() + 30 + Math.floor(Math.random() * 60));

    HoaDon.push({
      maHoaDon: hdIndex,
      maBan: maBan,
      maKhachHang: maKhachHang,
      gioVao: formatDate(orderDate),
      gioRa: trangThai === 1 ? formatDate(gioRa) : null,
      trangThai: trangThai,
      ghiChu: "",
      lastModified: Date.now()
    });

    hdIndex++;
  }
}

// Đặt bàn mẫu
const DatBan = [
  null,
  {
    maDatBan: 1,
    maBan: 3,
    maKhachHang: "kh001",
    ngayGioDat: formatDate(new Date(now.getTime() - 86400000 * 3)),
    ngayGioSuDung: formatDate(new Date(now.getTime() + 86400000)), // 1 ngày tới
    trangThai: 1, // Đã duyệt
    ghiChu: "Sinh nhật khách hàng, cần trang trí bàn",
    lastModified: Date.now()
  },
  {
    maDatBan: 2,
    maBan: 5,
    maKhachHang: "kh002",
    ngayGioDat: formatDate(new Date(now.getTime() - 86400000)),
    ngayGioSuDung: formatDate(new Date(now.getTime() + 86400000 * 2)),
    trangThai: 1,
    ghiChu: "Buổi họp team",
    lastModified: Date.now()
  },
  {
    maDatBan: 3,
    maBan: 7,
    maKhachHang: "kh001",
    ngayGioDat: formatDate(new Date()),
    ngayGioSuDung: formatDate(new Date(now.getTime() + 86400000 * 3)),
    trangThai: 0, // Chờ duyệt
    ghiChu: "Tiệc cưới nhỏ, 10 người",
    lastModified: Date.now()
  },
  {
    maDatBan: 4,
    maBan: 8,
    maKhachHang: "kh002",
    ngayGioDat: formatDate(new Date()),
    ngayGioSuDung: formatDate(new Date(now.getTime() + 86400000 * 5)),
    trangThai: 0, // Chờ duyệt
    ghiChu: "",
    lastModified: Date.now()
  },
];

// Hóa đơn mang về mẫu
const HoaDonMangVe = {
  "hd001": {
    maHoaDon: "hd001",
    maKhachHang: "kh001",
    gioVao: formatDate(new Date(now.getTime() - 3600000 * 2)),
    gioRa: formatDate(new Date(now.getTime() - 3600000)),
    trangThai: 1, // Chờ duyệt
    ghiChu: "Giao tận nhà",
    lastModified: Date.now()
  },
  "hd002": {
    maHoaDon: "hd002",
    maKhachHang: "kh002",
    gioVao: formatDate(new Date(now.getTime() - 3600000)),
    gioRa: null,
    trangThai: -1, // Chưa xác nhận
    ghiChu: "Mang đi",
    lastModified: Date.now()
  }
};

// Chi tiết hóa đơn mang về
// hd001: 2 cà phê sữa + 1 bánh tiramisu
HoaDonChiTiet.push(
  { maHDCT: ctIndex++, maHoaDon: "hd001", maHangHoa: 2, soLuong: 2, giaTien: 30000, ghiChu: "" },
  { maHDCT: ctIndex++, maHoaDon: "hd001", maHangHoa: 23, soLuong: 1, giaTien: 45000, ghiChu: "" }
);
// hd002: 1 cà phê đen + 1 trà sữa trân châu
HoaDonChiTiet.push(
  { maHDCT: ctIndex++, maHoaDon: "hd002", maHangHoa: 1, soLuong: 1, giaTien: 25000, ghiChu: "" },
  { maHDCT: ctIndex++, maHoaDon: "hd002", maHangHoa: 13, soLuong: 1, giaTien: 35000, ghiChu: "ít đường" }
);

// Thông báo mẫu
const ThongBao = [
  null,
  {
    maThongBao: 1,
    noiDung: "Chào mừng bạn đến với ARmy Coffee Admin!",
    ngayThongBao: new Date().toISOString().split('T')[0],
    trangThai: 0,
    lastModified: Date.now()
  },
  {
    maThongBao: 2,
    noiDung: "Đơn hàng mang về #hd001 đang chờ duyệt",
    ngayThongBao: new Date().toISOString().split('T')[0],
    trangThai: 0,
    lastModified: Date.now()
  },
  {
    maThongBao: 3,
    noiDung: "Có 2 yêu cầu đặt bàn mới cần duyệt",
    ngayThongBao: new Date().toISOString().split('T')[0],
    trangThai: 0,
    lastModified: Date.now()
  }
];

// ==================== GHI DỮ LIỆU ====================

async function seed() {
  console.log("🚀 Bắt đầu seed dữ liệu...\n");

  try {
    console.log("📝 Ghi NguoiDung...");
    await set(ref(db, 'NguoiDung'), seedData.NguoiDung);

    console.log("📝 Ghi LoaiHang...");
    await set(ref(db, 'LoaiHang'), seedData.LoaiHang);

    console.log("📝 Ghi HangHoa...");
    await set(ref(db, 'HangHoa'), seedData.HangHoa);

    console.log("📝 Ghi Ban...");
    await set(ref(db, 'Ban'), seedData.Ban);

    console.log(`📝 Ghi HoaDon (${HoaDon.length - 1} hóa đơn)...`);
    await set(ref(db, 'HoaDon'), HoaDon);

    console.log(`📝 Ghi HoaDonChiTiet (${HoaDonChiTiet.length - 1} chi tiết)...`);
    await set(ref(db, 'HoaDonChiTiet'), HoaDonChiTiet);

    console.log(`📝 Ghi DatBan (${DatBan.length - 1} đặt bàn)...`);
    await set(ref(db, 'DatBan'), DatBan);

    console.log("📝 Ghi HoaDonMangVe...");
    await set(ref(db, 'HoaDonMangVe'), HoaDonMangVe);

    console.log("📝 Ghi ThongBao...");
    await set(ref(db, 'ThongBao'), ThongBao);

    console.log("\n✅ Seed dữ liệu thành công!");
    console.log("\n📊 Tóm tắt dữ liệu:");
    console.log(`   - Admin: admin / admin123`);
    console.log(`   - Nhân viên: 3 người`);
    console.log(`   - Khách hàng: 2 người`);
    console.log(`   - Loại hàng: 8 loại`);
    console.log(`   - Hàng hóa: 28 sản phẩm`);
    console.log(`   - Bàn: 10 bàn`);
    console.log(`   - Hóa đơn: ${HoaDon.length - 1} hóa đơn (30 ngày)`);
    console.log(`   - Đặt bàn: 4 yêu cầu`);
    console.log(`   - Thông báo: 3 thông báo`);

    process.exit(0);
  } catch (error) {
    console.error("❌ Lỗi khi seed dữ liệu:", error);
    process.exit(1);
  }
}

seed();
