# CI/CD Setup Guide

## GitHub Actions - Auto Deploy lên Firebase Hosting

### Bước 1: Push code lên GitHub

```bash
cd APP/admin-web
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/YOUR_USERNAME/admin-web.git
git push -u origin main
```

### Bước 2: Tạo Firebase Service Account Key

1. Mở [Firebase Console](https://console.firebase.google.com)
2. Chọn project **thuoc-3e916**
3. Vào **Project Settings** → **Service Accounts**
4. Click **Generate new private key**
5. Lưu file JSON lại

### Bước 3: Thêm Secrets vào GitHub

1. Mở repository GitHub
2. Vào **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret** và thêm:

| Secret Name | Value |
|------------|-------|
| `FIREBASE_SERVICE_ACCOUNT` | Copy toàn bộ nội dung file JSON đã tải ở bước 2 |
| `FIREBASE_TOKEN` | Chạy `npx firebase login:ci` để lấy token |

### Bước 4: Lấy Firebase Token (nếu chưa có)

```bash
npm install -g firebase-tools
firebase login:ci
```

Sau đó copy token và thêm vào GitHub Secrets với tên `FIREBASE_TOKEN`.

### Bước 5: Kích hoạt Actions

1. Vào tab **Actions** trong GitHub repository
2. Workflow `firebase-hosting.yml` sẽ tự động xuất hiện
3. Click **Enable** nếu cần

### Kết quả

Mỗi khi push lên branch `main`, code sẽ tự động:
1. Build React app
2. Deploy lên Firebase Hosting

---

## Custom Domain Setup

### Trên Firebase Hosting

1. Mở [Firebase Console](https://console.firebase.google.com)
2. Chọn project **thuoc-3e916**
3. Vào **Hosting** → **Add custom domain**
4. Nhập domain của bạn (ví dụ: `admin.thuoc.vn` hoặc `thuoc.vn`)
5. Firebase sẽ cung cấp các bản ghi DNS

### Trên nhà cung cấp domain

Thêm các bản ghi DNS theo hướng dẫn của Firebase:

| Type | Name | Value |
|------|------|-------|
| TXT | (your domain) | Firebase verification code |
| A | (your domain) | Firebase IP addresses |
| AAAA | (your domain) | Firebase IPv6 addresses |

### Chờ xác minh

- DNS thường mất 5-30 phút để propagate
- Firebase sẽ tự động cấp SSL certificate

### Hoàn tất

Sau khi domain được xác minh:
- HTTPS được bật tự động
- Website có thể truy cập qua domain tùy chỉnh
