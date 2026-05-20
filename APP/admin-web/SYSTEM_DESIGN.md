# ARmy Coffee - System Design Document

## 1. Tổng quan hệ thống

Hệ thống **ARmy Coffee Admin** là trang quản trị web cho ứng dụng bán hàng quán cà phê, kết nối đồng thời với:

- **Firebase Realtime Database** - Dữ liệu chính (users, đơn hàng, sản phẩm...)
- **Firebase Firestore** - Backup/auth
- **Firebase Storage** - Hình ảnh sản phẩm, loại hàng, nhân viên
- **Firebase Auth** - Xác thực admin

---

## 2. Mô hình dữ liệu (Realtime Database)

```
Firebase Realtime Database
│
├── Ban                    (Array, index = maBan)
│   └── [maBan]: { maBan, trangThai, lastModified }
│       - trangThai: 0 = Còn trống, 1 = Có khách
│
├── NguoiDung              (Object, key = maNguoiDung)
│   └── [maNguoiDung]: { maNguoiDung, hoVaTen, email, matKhau, chucVu, gioiTinh, ngaySinh, hinhAnhUrl }
│       - chucVu: "Admin" | "NhanVien" | "KhachHang"
│
├── LoaiHang               (Array, index = maLoai)
│   └── [maLoai]: { maLoai, tenLoai, hinhAnhUrl, hasImage, lastModified }
│
├── HangHoa                (Array, index = maHangHoa)
│   └── [maHangHoa]: { maHangHoa, tenHangHoa, giaTien, maLoai, trangThai, hinhAnhUrl, hasImage, lastModified }
│       - trangThai: 0 = Hết hàng, 1 = Còn hàng
│
├── HoaDon                 (Array, index = maHoaDon)
│   └── [maHoaDon]: { maHoaDon, maBan, maKhachHang, gioVao, gioRa, trangThai, ghiChu, lastModified }
│       - trangThai: 0 = Chưa TT, 1 = Đã TT, 2 = Đã duyệt, 3 = Đã hủy
│
├── HoaDonChiTiet          (Array, index = auto)
│   └── [id]: { maHDCT, maHoaDon, maHangHoa, soLuong, giaTien, ghiChu }
│
├── HoaDonMangVe           (Object, key = maHoaDon)
│   └── [maHoaDon]: { maHoaDon, maKhachHang, gioVao, gioRa, trangThai, ghiChu, lastModified }
│       - trangThai: -1 = Chưa xác nhận, 0 = Đã duyệt, 1 = Chờ duyệt, 2 = Hủy
│
├── DatBan                  (Array, index = maDatBan)
│   └── [maDatBan]: { maDatBan, maBan, maKhachHang, ngayGioDat, ngayGioSuDung, trangThai, ghiChu, lastModified }
│       - trangThai: 0 = Chờ duyệt, 1 = Đã duyệt, -1 = Từ chối, 2 = Đã sử dụng, 3 = Đã hủy
│
└── ThongBao               (Array, index = maThongBao)
    └── [maThongBao]: { maThongBao, noiDung, ngayThongBao, trangThai, lastModified }
```

---

## 3. Sơ đồ ERD

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│   NguoiDung  │       │     Ban      │       │   LoaiHang    │
├──────────────┤       ├──────────────┤       ├──────────────┤
│ maNguoiDung  │       │    maBan     │       │    maLoai     │
│   hoVaTen    │       │  trangThai   │       │   tenLoai     │
│    email     │       └──────┬───────┘       │  hinhAnhUrl   │
│   chucVu     │              │               └───────┬───────┘
│  gioiTinh    │              │                       │
│   ngaySinh   │              │                       │
└──────┬───────┘              │                       │
       │                     │                       │
       │                     ▼                       ▼
       │              ┌──────────────┐       ┌──────────────┐
       │              │   HoaDon     │       │   HangHoa    │
       │              ├──────────────┤       ├──────────────┤
       │              │  maHoaDon    │       │  maHangHoa   │
       └─────────────▶│    maBan     │       │  tenHangHoa  │
                       │ maKhachHang │       │  giaTien     │
                       │   gioVao    │       │   maLoai ◀───┘
                       │   gioRa     │       │  trangThai   │
                       │  trangThai  │       │  hinhAnhUrl  │
                       └──────┬───────┘       └──────────────┘
                              │
                              ▼
                       ┌──────────────┐
                       │HoaDonChiTiet │
                       ├──────────────┤
                       │    maHDCT    │
                       │  maHoaDon    │
                       │  maHangHoa ◀─┤
                       │   soLuong    │
                       │  giaTien    │
                       └──────────────┘
```

---

## 4. Cấu trúc mã nguồn

```
admin-web/src/
├── components/
│   ├── Layout.jsx              # Layout chính (sidebar + main)
│   ├── Modal.jsx               # Component modal chung
│   ├── NhanVienForm.jsx        # Form thêm/sửa nhân viên
│   ├── ProtectedRoute.jsx       # Bảo vệ route bằng auth
│   ├── FilterPanel.jsx         # Panel lọc dữ liệu
│   ├── SortPanel.jsx           # Panel sắp xếp
│   ├── StatusBadge.jsx         # Badge trạng thái
│   ├── ExportPanel.jsx         # Xuất dữ liệu ra CSV
│   ├── AdvancedSearchPanel.jsx # Tìm kiếm nâng cao
│   ├── ImageDisplay.jsx        # Hiển thị hình ảnh từ Firebase Storage
│   └── index.js
│
├── contexts/
│   ├── AuthContext.jsx         # Auth provider (login/logout)
│   └── ThemeContext.jsx         # Dark mode provider
│
├── pages/
│   ├── Login.jsx               # Trang đăng nhập
│   ├── Dashboard.jsx            # Trang tổng quan (stats, chart)
│   ├── QuanLyNhanVien.jsx      # CRUD nhân viên
│   ├── QuanLyDoanhThu.jsx       # Biểu đồ doanh thu (Recharts)
│   ├── QuanLyBan.jsx            # Quản lý bàn + trạng thái
│   ├── QuanLyLoaiHang.jsx       # CRUD loại hàng + ảnh
│   ├── QuanLyHangHoa.jsx        # CRUD hàng hóa + ảnh
│   ├── QuanLyHoaDon.jsx         # Danh sách hóa đơn + chi tiết
│   ├── DuyetHoaDon.jsx         # Duyệt hóa đơn mang về
│   └── DuyetDatBan.jsx         # Duyệt đặt bàn
│
├── config/
│   └── firebase.js             # Firebase config & exports
│
├── utils/
│   ├── realtimeDB.js            # CRUD helpers Realtime DB
│   ├── dateUtils.js            # Parse/format ngày giờ
│   ├── imageUtils.js           # Lấy URL ảnh từ Storage
│   └── checkFirestore.js       # Kiểm tra Firestore
│
├── App.jsx                     # Router + providers
├── main.jsx                    # Entry point + SW registration
└── index.css                  # Global styles + dark mode
```

---

## 5. Tính năng từng trang

| Trang | CRUD | Tìm kiếm | Lọc | Sắp xếp | Xuất | Khác |
|-------|------|-----------|-----|----------|------|------|
| Dashboard | - | - | - | - | - | Chart 7 ngày, top sản phẩm |
| Nhân viên | ✓ | ✓ | - | - | - | Ảnh avatar |
| Doanh thu | - | - | ✓ (tháng/năm) | - | - | Chart Recharts, so sánh năm |
| Bàn | ✓ | - | ✓ | ✓ | - | Trạng thái realtime, đặt bàn |
| Loại hàng | ✓ | - | - | - | - | Upload ảnh |
| Hàng hóa | ✓ | ✓ | ✓ | ✓ | - | Ảnh, thống kê, filter |
| Hóa đơn | - | ✓ | ✓ | ✓ | ✓ CSV | Chi tiết, in hóa đơn |
| Duyệt HĐ | ✓ (duyệt/từ chối) | - | - | - | - | Tạo hóa đơn tự động |
| Duyệt đặt bàn | ✓ (duyệt/từ chối) | ✓ | ✓ | ✓ | - | Gửi thông báo |

---

## 6. Authentication Flow

```
User login (email/username + password)
        │
        ▼
AuthContext.login()
        │
        ├──▶ Firebase Realtime DB → NguoiDung/{id}
        │       (Tìm theo document ID hoặc email)
        │       (Kiểm tra chucVu === 'Admin')
        │       (Kiểm tra matKhau)
        │
        ├──▶ Firebase Firestore → NguoiDung (fallback)
        │
        └──▶ Lưu vào sessionStorage
                │
                ▼
        ProtectedRoute check
                │
                ├── OK  → Cho phép truy cập
                └── ERR → Redirect /login
```

---

## 7. Data Flow

```
Firebase Realtime Database
        │
        ├── getAllFromRealtimeDB() ──▶ Trang quản lý
        │   (Ban, LoaiHang, HangHoa, HoaDon...)
        │
        ├── queryRealtimeDB() ────────▶ Dashboard, NhanVien
        │   (Lọc theo điều kiện)
        │
        ├── setRealtimeDB() ──────────▶ Tạo/Cập nhật
        │
        ├── updateArrayItem() ────────▶ CRUD array nodes
        │
        └── deleteArrayItem() ────────▶ Xóa (set = null)

Firebase Storage
        │
        ├── uploadBytes() ─────────────▶ Upload ảnh
        │   Path: nguoidung/{maNguoiDung}.jpg
        │        loaihang/{maLoai}.jpg
        │        hanghoa/{maHangHoa}.jpg
        │
        └── getDownloadURL() ──────────▶ Hiển thị ảnh
```

---

## 8. Security Rules

### Firestore Rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null
        && get(/databases/$(database)/documents/NguoiDung/$(request.auth.uid)).data.chucVu == 'Admin';
    }
  }
}
```

### Storage Rules
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null
        && firestore.get(/databases/(default)/documents/NguoiDung/$(request.auth.uid)).data.chucVu == 'Admin';
    }
  }
}
```

### Realtime Database Rules
```json
{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null && root.child('NguoiDung').child(auth.uid).child('chucVu').val() === 'Admin'"
  }
}
```
