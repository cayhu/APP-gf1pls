package app.edu.app.ui;

import static app.edu.app.ui.SignInActivity._maNguoiDung;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.Calendar;

import app.edu.app.R;
import app.edu.app.adapter.HoaDonChiTietMainAdapter;
import app.edu.app.dao.BanDAO;
import app.edu.app.dao.HangHoaDAO;
import app.edu.app.dao.HoaDonChiTietDAO;
import app.edu.app.dao.HoaDonDAO;
import app.edu.app.dao.NguoiDungDAO;
import app.edu.app.dao.ThongBaoDAO;
import app.edu.app.interfaces.ItemTangGiamSoLuongOnClick;
import app.edu.app.model.Ban;
import app.edu.app.model.HangHoa;
import app.edu.app.model.HoaDon;
import app.edu.app.model.HoaDonChiTiet;
import app.edu.app.model.NguoiDung;
import app.edu.app.model.ThongBao;
import app.edu.app.ui.AISuggestionDialog;
import app.edu.app.utils.MyToast;
import app.edu.app.utils.VNPayHelper;
import app.edu.app.utils.XDate;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Activity quản lý đặt món và thanh toán hóa đơn
 * 
 * ✅ 100% FIREBASE DIRECT MODE
 * - Tất cả dữ liệu đọc (READ) đều load TRỰC TIẾP từ Firebase Realtime Database
 * - Không sử dụng SQLite để đọc dữ liệu
 * - Các thao tác ghi (UPDATE/DELETE) vẫn dùng DAO (sẽ tự sync lên Firebase)
 * 
 * Dữ liệu load từ Firebase:
 * - HoaDon (getByMaBanFromFirebaseDirect)
 * - HoaDonChiTiet (getByMaHoaDonFromFirebaseDirect)
 * - HangHoa (getByMaHangHoaFromFirebaseDirect)
 * - NguoiDung (getByMaNguoiDungFromFirebaseDirect)
 * - Ban (getByMaBanFromFirebaseDirect)
 */
public class OderActivity extends AppCompatActivity {
    public static final String MA_HOA_DON = "maHoaDon";
    HoaDonChiTietDAO hoaDonChiTietDAO;
    HangHoaDAO hangHoaDAO;
    HoaDonDAO hoaDonDAO;
    BanDAO banDAO;
    TextView tvMaBan, tvGioVao, tvThemMon, tvTamTinh, tvHoaDonCuoi,tvnguoi, tvAISuggestion;
    RecyclerView recyclerViewThucUong;
    SwipeRefreshLayout swipeRefresh;
    Button btnThanhToan;
    Toolbar toolbar;
    public static  String maBan = "";  // Reset ở onCreate
    private SharedPreferences sharedPreferences;
    private AISuggestionDialog aiSuggestionDialog;
    private boolean isFirstLoad = true; // ✅ Flag để tracking lần load đầu tiên
    private HoaDonChiTietMainAdapter adapter; // ✅ Lưu reference adapter để update

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        maBan = ""; // Reset static variable mỗi lần mở Activity mới
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oder);
        initToolbar();
        initview();

        hoaDonChiTietDAO = new HoaDonChiTietDAO(this);
        hangHoaDAO = new HangHoaDAO(this);
        hoaDonDAO = new HoaDonDAO(this);
        banDAO = new BanDAO(this);

        sharedPreferences = getSharedPreferences("USER_FILE", MODE_PRIVATE);
        
        // QUAN TRỌNG: Load hóa đơn trực tiếp từ Firebase (không sync về SQLite)
        loadHoaDonFromFirebase();
        tvThemMon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openThemMonActivity();
            }
        });
        btnThanhToan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                thanhToanHoaDon();
            }
        });
    }


    
    /**
     * Load hóa đơn trực tiếp từ Firebase (100% Firebase Direct)
     * Cần thiết khi hóa đơn được tạo từ web admin
     * 
     * LOGIC:
     * - Nếu có ngayGioSuDung từ Intent: Lọc hóa đơn theo ngày (chỉ so sánh phần ngày, không cần giờ chính xác)
     * - Nếu không có ngayGioSuDung: Lấy hóa đơn mới nhất (theo maHoaDon DESC)
     * 
     * ✅ Không sử dụng SQLite - Load trực tiếp từ Firebase Realtime Database
     */
    private void loadHoaDonFromFirebase() {
        Intent intent = getIntent();
        if(maBan.equals("")){
            maBan = intent.getStringExtra("maBan");
        }

        // Kiểm tra xem có maKhachHang trong Intent không (từ QuanLyBanNguoiDungActivity)
        String maKhachHangFromIntent = intent.getStringExtra("maKhachHang");
        String currentMaKhachHang = sharedPreferences.getString("maNguoiDung", "");

        // Kiểm tra vai trò người dùng
        String maNguoiDung = sharedPreferences.getString("maNguoiDung", "");
        NguoiDungDAO nguoiDungDAO = new NguoiDungDAO(OderActivity.this);
        NguoiDung nguoiDung = nguoiDungDAO.getByMaNguoiDung(maNguoiDung);
        boolean isKhachHang = (nguoiDung == null || nguoiDung.getChucVu().equals("KhachHang"));

        // Xác định maKhachHang để lọc
        // - Khách hàng: chỉ xem hóa đơn của mình
        // - Admin/Nhân viên: xem bất kỳ hóa đơn nào của bàn
        String maKhachHang = null;
        if (!isKhachHang) {
            // Admin/Nhân viên: không lọc theo maKhachHang
            maKhachHang = null;
        } else if (maKhachHangFromIntent != null && !maKhachHangFromIntent.isEmpty()) {
            maKhachHang = maKhachHangFromIntent;
        } else if (currentMaKhachHang != null && !currentMaKhachHang.isEmpty()) {
            maKhachHang = currentMaKhachHang;
        }

        // Lấy ngayGioSuDung từ Intent (nếu có) để lọc theo ngày
        String ngayGioSuDung = intent.getStringExtra("ngayGioSuDung");

        // Lấy hóa đơn trực tiếp từ Firebase
        String finalMaKhachHang = maKhachHang;
        hoaDonDAO.getByMaBanFromFirebaseDirect(maBan, maKhachHang, HoaDon.CHUA_THANH_TOAN, ngayGioSuDung,
            new HoaDonDAO.OnHoaDonListener() {
                @Override
                public void onHoaDonReceived(HoaDon hoaDon) {
                    // Lưu hóa đơn vào biến để dùng sau
                    currentHoaDon = hoaDon;
                    
                    Log.d("OderActivity", "Tìm thấy hóa đơn từ Firebase: " + hoaDon.getMaHoaDon() + 
                          " của bàn " + hoaDon.getMaBan() + ", gioVao=" + 
                          app.edu.app.utils.XDate.toStringDateTime(hoaDon.getGioVao()));
                    
                    // Khởi tạo AI Suggestion Dialog và load data
                    String maHoaDon = String.valueOf(hoaDon.getMaHoaDon());
                    aiSuggestionDialog = new AISuggestionDialog(OderActivity.this, maHoaDon);
                    
                    // ✅ FIX: Chỉ load data, fillActivity() sẽ được gọi SAU khi data đã sẵn sàng
                    loadData();
                }

                @Override
                public void onError(Exception e) {
                    Log.w("OderActivity", "Không tìm thấy hóa đơn từ Firebase cho bàn " + maBan +
                          ", maKhachHang=" + finalMaKhachHang + ", ngayGioSuDung=" + ngayGioSuDung, e);

                    if (isKhachHang) {
                        // Khách hàng → báo lỗi và quay về
                        MyToast.error(OderActivity.this, "Bàn này chưa có hóa đơn. Vui lòng tạo hóa đơn mới từ Quản lý bàn.");
                        finish();
                    } else {
                        // Admin/Nhân viên → tự tạo hóa đơn mới
                        Calendar c = Calendar.getInstance();
                        HoaDon hoaDon = new HoaDon();
                        hoaDon.setMaBan(Integer.parseInt(maBan));
                        hoaDon.setGioVao(c.getTime());
                        hoaDon.setGioRa(c.getTime());
                        hoaDon.setTrangThai(HoaDon.CHUA_THANH_TOAN);
                        hoaDon.setMaKhachHang(finalMaKhachHang != null ? finalMaKhachHang : "");
                        hoaDon.setGhiChu("");

                        if (hoaDonDAO.insertHoaDon(hoaDon)) {
                            MyToast.successful(OderActivity.this, "Đã tạo hóa đơn mới cho bàn " + maBan);
                            loadHoaDonFromFirebase();
                        } else {
                            MyToast.error(OderActivity.this, "Lỗi khi tạo hóa đơn");
                            finish();
                        }
                    }
                }
            });
    }
    
    private HoaDon currentHoaDon; // Lưu hóa đơn đã lấy từ Firebase
    private boolean isLoadingData = false; // ✅ Flag để tránh load data duplicate

    private void initToolbar() {
        toolbar = findViewById(R.id.toolbarOder);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    private void initview() {
        tvMaBan = findViewById(R.id.tvMaBan);
        tvGioVao = findViewById(R.id.tvGioVao);
        recyclerViewThucUong = findViewById(R.id.recyclerViewThucUong);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        tvThemMon = findViewById(R.id.tvThemMon);
        btnThanhToan = findViewById(R.id.btnThanhToan);
        tvTamTinh = findViewById(R.id.tvTamTinh);
        tvHoaDonCuoi = findViewById(R.id.tvHoaDonCuoi);
        tvnguoi = findViewById(R.id.tvnguoidat);
        tvAISuggestion = findViewById(R.id.tvAISuggestion);
        
        // Setup SwipeRefreshLayout
        swipeRefresh.setColorSchemeResources(
            R.color.BluePrimary,
            R.color.RedPrimary,
            R.color.GreenPrimary
        );
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d("OderActivity", "🔄 User triggered FORCE refresh - reloading from server...");
                // Reload toàn bộ dữ liệu từ Firebase (force từ server)
                if (currentHoaDon != null) {
                    loadData();
                    fillActivity();
                } else {
                    swipeRefresh.setRefreshing(false);
                }
            }
        });
        
        // Setup AI Suggestion click listener (sẽ khởi tạo dialog sau khi hoaDonDAO được init)
        tvAISuggestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Khởi tạo dialog nếu chưa có
                if (aiSuggestionDialog == null) {
                    HoaDon hoaDon = getHoaDon();
                    String maHoaDon = hoaDon != null ? String.valueOf(hoaDon.getMaHoaDon()) : null;
                    aiSuggestionDialog = new AISuggestionDialog(OderActivity.this, maHoaDon);
                }
                aiSuggestionDialog.show();
            }
        });
    }

    private void openThemMonActivity() {
        // Mở màn hình thêm món
        HoaDon hoaDon = getHoaDon();
        
        // Kiểm tra hóa đơn có tồn tại không
        if (hoaDon == null || hoaDon.getMaHoaDon() == 0) {
            MyToast.error(this, "Không có hóa đơn để thêm món. Vui lòng tạo hóa đơn mới từ Quản lý bàn.");
            return;
        }
        
        Intent intent = new Intent(OderActivity.this, ThemMonActivity.class);
        intent.putExtra(MA_HOA_DON, String.valueOf(hoaDon.getMaHoaDon()));
        startActivity(intent);
        finish();
    }

    @SuppressLint("SetTextI18n")
    private void thanhToanHoaDon() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.layout_dialog_thanhtoan);

        TextView tvMahoaDon = dialog.findViewById(R.id.tvMaHoaDon);
        TextView tvMaBan = dialog.findViewById(R.id.tvMaBan);
        TextView tvGioVaoTT = dialog.findViewById(R.id.tvGioVao);
        TextView tvHoaDonCuoi = dialog.findViewById(R.id.tvHoaDonCUoi);
        TextView tvnguoi = dialog.findViewById(R.id.tvnguoidat);
        TextView tvTongTien = dialog.findViewById(R.id.tvTongTien);
        TextView tvCancle= dialog.findViewById(R.id.tvCancle);
        EditText edtGhiChu = dialog.findViewById(R.id.edtGhiChu);
        Button btnPay = dialog.findViewById(R.id.btnPay);
        Button btnPayVNPay = dialog.findViewById(R.id.btnPayVNPay);

        HoaDon hoaDonForDialog = getHoaDon();
        tvMahoaDon.setText("HD0" + hoaDonForDialog.getMaHoaDon());
        tvMaBan.setText("B0" + hoaDonForDialog.getMaBan());
        tvGioVaoTT.setText(XDate.toStringDateTime(hoaDonForDialog.getGioVao()));
        
        // Load tổng tiền từ Firebase
        hoaDonChiTietDAO.getByMaHoaDonFromFirebaseDirect(hoaDonForDialog.getMaHoaDon(), 
            new HoaDonChiTietDAO.OnHoaDonChiTietListListener() {
                @Override
                public void onListReceived(ArrayList<HoaDonChiTiet> listHDCT) {
                    long tongTien = 0;
                    for (HoaDonChiTiet hdct : listHDCT) {
                        tongTien += hdct.getGiaTien();
                    }
                    final long finalTongTien = tongTien;
                    runOnUiThread(() -> {
                        tvTongTien.setText(finalTongTien + "VND");
                        tvHoaDonCuoi.setText(finalTongTien + "VND");
                    });
                }

                @Override
                public void onError(Exception e) {
                    Log.e("OderActivity", "Lỗi load tổng tiền", e);
                    runOnUiThread(() -> {
                        tvTongTien.setText("0VND");
                        tvHoaDonCuoi.setText("0VND");
                    });
                }
            });
        
        // Load thông tin người dùng TRỰC TIẾP từ Firebase
        NguoiDungDAO nguoiDungDAO = new NguoiDungDAO(this);
        String ma = sharedPreferences.getString("maNguoiDung", "");
        nguoiDungDAO.getByMaNguoiDungFromFirebaseDirect(ma, new NguoiDungDAO.OnNguoiDungListener() {
            @Override
            public void onNguoiDungReceived(app.edu.app.model.NguoiDung nguoiDung) {
                runOnUiThread(() -> {
                    tvnguoi.setText(nguoiDung.getHoVaTen());
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e("OderActivity", "Lỗi load người dùng từ Firebase", e);
                runOnUiThread(() -> {
                    tvnguoi.setText("Khách hàng");
                });
            }
        });
        tvCancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        // Thanh toán VNPay
        btnPayVNPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HoaDon hoaDon = getHoaDon();
                String orderId = "HD0" + hoaDon.getMaHoaDon();
                String orderInfo = "Thanh toan hoa don " + orderId;
                String ipAddr = getLocalIpAddress();
                
                // Load tổng tiền từ Firebase trước khi thanh toán
                hoaDonChiTietDAO.getByMaHoaDonFromFirebaseDirect(hoaDon.getMaHoaDon(), 
                    new HoaDonChiTietDAO.OnHoaDonChiTietListListener() {
                        @Override
                        public void onListReceived(ArrayList<HoaDonChiTiet> listHDCT) {
                            // Tính tổng tiền
                            long totalAmount = 0;
                            for (HoaDonChiTiet hdct : listHDCT) {
                                totalAmount += hdct.getGiaTien();
                            }
                            
                            final long finalAmount = totalAmount;
                            runOnUiThread(() -> {
                                // Tạo payment URL
                                String paymentUrl = VNPayHelper.createPaymentUrl(
                                    orderId,
                                    finalAmount,
                                    orderInfo,
                                    ipAddr,
                                    null
                                );
                                
                                if (paymentUrl != null && !paymentUrl.isEmpty()) {
                                    dialog.dismiss();
                                    // Mở VNPay payment activity
                                    Intent vnpayIntent = new Intent(OderActivity.this, VNPayPaymentActivity.class);
                                    vnpayIntent.putExtra("payment_url", paymentUrl);
                                    vnpayIntent.putExtra("order_id", orderId);
                                    vnpayIntent.putExtra("amount", finalAmount);
                                    vnpayIntent.putExtra("hoa_don_id", String.valueOf(hoaDon.getMaHoaDon()));
                                    startActivityForResult(vnpayIntent, 1001);
                                } else {
                                    MyToast.error(OderActivity.this, "Không thể tạo link thanh toán");
                                }
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e("OderActivity", "Lỗi load tổng tiền cho VNPay", e);
                            MyToast.error(OderActivity.this, "Không thể tính tổng tiền");
                        }
                    });
            }
        });

        // Thanh toán tiền mặt
        btnPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Thanh toán hoá đơn
                HoaDon hoaDon = getHoaDon();
                hoaDon.setTrangThai(HoaDon.DA_THANH_TOAN); // cập nhật lại trạng thái đã thanh toán
                Calendar calendar = Calendar.getInstance();
                hoaDon.setGioRa(calendar.getTime());// cập nhật lại giờ ra
                hoaDon.setGhiChu(edtGhiChu.getText().toString());
                _maNguoiDung = sharedPreferences.getString("maNguoiDung", "");
                hoaDon.setMaKhachHang(_maNguoiDung);
                Intent intent = getIntent();
                String maBan = intent.getStringExtra(QuanLyBanActivity.MA_BAN);
                
                // Load thông tin bàn TRỰC TIẾP từ Firebase
                banDAO.getByMaBanFromFirebaseDirect(maBan, new BanDAO.OnBanListener() {
                    @Override
                    public void onBanReceived(Ban ban) {
                ban.setTrangThai(Ban.CON_TRONG); // cập nhật lại trạng thái còn trống
                Intent intent1 = new Intent(OderActivity.this, ChuyenKhoanActivity.class);
                        intent1.putExtra("ban", ban);
                        intent1.putExtra("hoaDon", hoaDon);
                startActivity(intent1);
                finish();
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("OderActivity", "Lỗi load bàn từ Firebase", e);
                        MyToast.error(OderActivity.this, "Không thể load thông tin bàn");
                    }
                });
//                if (banDAO.updateBan(ban) && hoaDonDAO.updateHoaDon(hoaDon)) {
//                    MyToast.successful(OderActivity.this, "Thanh Toán thành công");
//                    MyNotification.getNotification(OderActivity.this, "Thanh toán thành công hoá đơn HD0775098507" + hoaDon.getMaHoaDon());
//                    themThonBaoMoi(hoaDon, calendar);
//                }
//                onBackPressed();
            }
        });

        dialog.show();
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    private void themThonBaoMoi(HoaDon hoaDon, Calendar calendar) {
        // Tạo thông báo thanh toán hoá đơn
        ThongBao thongBao = new ThongBao();
        thongBao.setNoiDung("Thanh toán thành công hoá đơn HD0775098507"+ hoaDon.getMaHoaDon());
        thongBao.setTrangThai(ThongBao.STATUS_CHUA_XEM);
        thongBao.setNgayThongBao(calendar.getTime());
        ThongBaoDAO thongBaoDAO = new ThongBaoDAO(OderActivity.this);
        thongBaoDAO.insertThongBao(thongBao);
    }

    /**
     * Load dữ liệu TRỰC TIẾP từ Firebase (không dùng SQLite)
     * Bao gồm: HoaDon, HoaDonChiTiet, HangHoa, NguoiDung, Ban
     * 
     * ✅ 100% Firebase Direct - Không sử dụng SQLite để đọc dữ liệu
     */
    private void loadData() {
        Log.d("OderActivity", "╔═══════════════════════════════╗");
        Log.d("OderActivity", "║   LOAD DATA FROM FIREBASE     ║");
        Log.d("OderActivity", "╚═══════════════════════════════╝");
        
        HoaDon hoaDon = getHoaDon();
        
        // Kiểm tra hóa đơn có tồn tại không
        if (hoaDon == null || hoaDon.getMaHoaDon() == 0) {
            Log.w("OderActivity", "Không có hóa đơn chưa thanh toán cho bàn " + maBan);
            MyToast.error(this, "Bàn này chưa có hóa đơn. Vui lòng tạo hóa đơn mới từ Quản lý bàn.");
            finish();
            return;
        }
        
        Log.d("OderActivity", "➤ Load dữ liệu TRỰC TIẾP từ Firebase cho hóa đơn: " + hoaDon.getMaHoaDon());
        
        // ✅ REAL-TIME MODE: Bỏ isLoadingData check vì real-time listener sẽ handle
        // Listener chỉ trigger khi có thay đổi thực sự trên Firebase
        Log.d("OderActivity", "📡 Real-time listener sẽ tự động cập nhật khi có thay đổi");
        
        // Load chi tiết hóa đơn TRỰC TIẾP từ Firebase
        hoaDonChiTietDAO.getByMaHoaDonFromFirebaseDirect(hoaDon.getMaHoaDon(), 
            new HoaDonChiTietDAO.OnHoaDonChiTietListListener() {
                @Override
                public void onListReceived(ArrayList<HoaDonChiTiet> listHDCT) {
                    Log.d("OderActivity", "╔═══════════════════════════════════════╗");
                    Log.d("OderActivity", "║  CALLBACK: onListReceived            ║");
                    Log.d("OderActivity", "╚═══════════════════════════════════════╝");
                    Log.d("OderActivity", "✓ Đã load " + listHDCT.size() + " chi tiết hóa đơn từ Firebase");
                    
                    if (listHDCT.isEmpty()) {
                        // Không có món nào, hiển thị RecyclerView rỗng
                        swipeRefresh.setRefreshing(false); // Tắt loading indicator
                        Log.d("OderActivity", "📋 Danh sách trống - hiển thị empty view");
                        setupRecyclerView(new ArrayList<>(), new ArrayList<>());
                        return;
                    }
                    
                    // Load thông tin hàng hóa TRỰC TIẾP từ Firebase cho từng chi tiết
                    // Khởi tạo ArrayList với size cố định để đảm bảo thứ tự đúng
                    final ArrayList<HangHoa> listHangHoa = new ArrayList<>(listHDCT.size());
                    // Điền null vào tất cả các vị trí
                    for (int i = 0; i < listHDCT.size(); i++) {
                        listHangHoa.add(null);
                    }
                    
                    final int[] loadedCount = {0}; // Counter để track khi nào load xong hết
                    final int[] errorCount = {0}; // Counter để đếm lỗi
                    
                    Log.d("OderActivity", "▶ Bắt đầu load " + listHDCT.size() + " HangHoa:");
                    for (int i = 0; i < listHDCT.size(); i++) {
                        Log.d("OderActivity", "  Loop i=" + i + ", maHangHoa=" + listHDCT.get(i).getMaHangHoa());
                    }
                    
                    for (int i = 0; i < listHDCT.size(); i++) {
                        final int index = i;
                        final HoaDonChiTiet hdct = listHDCT.get(i);
                        
                        Log.d("OderActivity", "▶ [" + i + "] Gọi getByMaHangHoaFromFirebaseDirect(" + hdct.getMaHangHoa() + "), index=" + index);
                        
                        hangHoaDAO.getByMaHangHoaFromFirebaseDirect(hdct.getMaHangHoa(), 
                            new HangHoaDAO.OnHangHoaListener() {
                                @Override
                                public void onHangHoaReceived(HangHoa hangHoa) {
                                    Log.d("OderActivity", "▶ Callback nhận HangHoa: " + hangHoa.getTenHangHoa() + 
                                          " (maHangHoa=" + hangHoa.getMaHangHoa() + ")");
                                    Log.d("OderActivity", "  Sẽ set vào listHangHoa[" + index + "]");
                                    Log.d("OderActivity", "  listHangHoa.size() hiện tại: " + listHangHoa.size());
                                    
                                    // ✅ FIX: Thêm vào đúng vị trí thay vì add() cuối danh sách
                                    listHangHoa.set(index, hangHoa);
                                    loadedCount[0]++;
                                    
                                    Log.d("OderActivity", "✓ Load hàng hóa " + loadedCount[0] + "/" + listHDCT.size() + 
                                          " (index=" + index + "): " + hangHoa.getTenHangHoa());
                                    Log.d("OderActivity", "  listHangHoa[" + index + "] = " + listHangHoa.get(index).getTenHangHoa());
                                    
                                    // Khi đã load xong tất cả hàng hóa
                                    if (loadedCount[0] == listHDCT.size()) {
                                        Log.d("OderActivity", "✓ Hoàn tất load " + loadedCount[0] + " hàng hóa từ Firebase (lỗi: " + errorCount[0] + ")");
                                        
                                        // ✅ Lọc bỏ null values trước khi setup
                                        Log.d("OderActivity", "▶ Bắt đầu filter null values...");
                                        Log.d("OderActivity", "  listHangHoa.size() = " + listHangHoa.size());
                                        Log.d("OderActivity", "  listHDCT.size() = " + listHDCT.size());
                                        
                                        ArrayList<HangHoa> filteredHangHoa = new ArrayList<>();
                                        ArrayList<HoaDonChiTiet> filteredHDCT = new ArrayList<>();
                                        
                                        for (int j = 0; j < listHangHoa.size(); j++) {
                                            Log.d("OderActivity", "  Checking index " + j + ": " + 
                                                  (listHangHoa.get(j) != null ? listHangHoa.get(j).getTenHangHoa() : "NULL"));
                                            
                                            if (listHangHoa.get(j) != null) {
                                                filteredHangHoa.add(listHangHoa.get(j));
                                                filteredHDCT.add(listHDCT.get(j));
                                                Log.d("OderActivity", "    ✓ Đã thêm vào filtered list");
                                            } else {
                                                Log.w("OderActivity", "    ⚠ Bỏ qua item null tại index " + j);
                                            }
                                        }
                                        
                                        Log.d("OderActivity", "▶ Sau filter:");
                                        Log.d("OderActivity", "  filteredHangHoa.size() = " + filteredHangHoa.size());
                                        Log.d("OderActivity", "  filteredHDCT.size() = " + filteredHDCT.size());
                                        
                                        if (filteredHangHoa.isEmpty()) {
                                            Log.e("OderActivity", "Không có dữ liệu hợp lệ để hiển thị");
                                            MyToast.error(OderActivity.this, "Không thể load dữ liệu món ăn");
                                            setupRecyclerView(new ArrayList<>(), new ArrayList<>());
                                        } else {
                                            Log.d("OderActivity", "▶ Gọi setupRecyclerView với " + filteredHangHoa.size() + " items");
                                            setupRecyclerView(filteredHangHoa, filteredHDCT);
                                        }
                                        
                                        // ✅ CRITICAL: Luôn gọi fillActivity() và tắt loading SAU KHI setup xong
                                        runOnUiThread(() -> {
                                            fillActivity();
                                            swipeRefresh.setRefreshing(false);
                                            Log.d("OderActivity", "✓ fillActivity() và tắt loading hoàn tất");
                                        });
                                        Log.d("OderActivity", "✓ Load data hoàn tất - UI đã update");
                                    }
                                }

                                @Override
                                public void onError(Exception e) {
                                    Log.e("OderActivity", "Lỗi load hàng hóa " + hdct.getMaHangHoa() + " tại index=" + index, e);
                                    loadedCount[0]++;
                                    errorCount[0]++;
                                    
                                    // listHangHoa[index] vẫn là null
                                    
                                    // Vẫn tiếp tục khi load xong tất cả (kể cả lỗi)
                                    if (loadedCount[0] == listHDCT.size()) {
                                        Log.d("OderActivity", "Hoàn tất load dữ liệu (thành công: " + 
                                              (loadedCount[0] - errorCount[0]) + ", lỗi: " + errorCount[0] + ")");
                                        
                                        // ✅ Lọc bỏ null values (trong error callback)
                                        Log.d("OderActivity", "▶ Filter (có lỗi): listHangHoa.size=" + listHangHoa.size());
                                        ArrayList<HangHoa> filteredHangHoa = new ArrayList<>();
                                        ArrayList<HoaDonChiTiet> filteredHDCT = new ArrayList<>();
                                        
                                        for (int j = 0; j < listHangHoa.size(); j++) {
                                            Log.d("OderActivity", "  Index " + j + ": " + 
                                                  (listHangHoa.get(j) != null ? listHangHoa.get(j).getTenHangHoa() : "NULL"));
                                            
                                            if (listHangHoa.get(j) != null) {
                                                filteredHangHoa.add(listHangHoa.get(j));
                                                filteredHDCT.add(listHDCT.get(j));
                                            } else {
                                                Log.w("OderActivity", "    ⚠ Bỏ qua item null tại index " + j);
                                            }
                                        }
                                        
                                        Log.d("OderActivity", "▶ Filtered size: " + filteredHangHoa.size());
                                        
                                        if (filteredHangHoa.isEmpty()) {
                                            Log.e("OderActivity", "Tất cả items đều lỗi, không có gì để hiển thị");
                                            MyToast.error(OderActivity.this, "Không thể load dữ liệu món ăn");
                                            setupRecyclerView(new ArrayList<>(), new ArrayList<>());
                                        } else {
                                            if (errorCount[0] > 0) {
                                                Log.w("OderActivity", "⚠ " + errorCount[0] + " món không load được");
                                            }
                                            Log.d("OderActivity", "▶ Setup RecyclerView với " + filteredHangHoa.size() + " items");
                                            setupRecyclerView(filteredHangHoa, filteredHDCT);
                                        }
                                        
                                        // ✅ CRITICAL: Luôn gọi fillActivity() và tắt loading SAU KHI setup xong
                                        runOnUiThread(() -> {
                                            fillActivity();
                                            swipeRefresh.setRefreshing(false);
                                            Log.d("OderActivity", "✓ fillActivity() và tắt loading hoàn tất (có errors)");
                                        });
                                        Log.d("OderActivity", "✓ Load data hoàn tất (với một số errors)");
                                    }
                                }
                            });
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e("OderActivity", "❌ Lỗi load chi tiết hóa đơn từ Firebase", e);
                    MyToast.error(OderActivity.this, "Không thể load dữ liệu từ Firebase");
                    swipeRefresh.setRefreshing(false); // Tắt loading indicator
                    Log.d("OderActivity", "⚠ Load error - check connection");
                }
            });
    }
    
    /**
     * Setup RecyclerView với dữ liệu đã load từ Firebase
     * ✅ Nếu adapter đã tồn tại, update data và notify thay vì tạo mới
     */
    private void setupRecyclerView(ArrayList<HangHoa> listHangHoa, ArrayList<HoaDonChiTiet> listHDCT) {
        Log.d("OderActivity", "═══ setupRecyclerView ═══");
        Log.d("OderActivity", "Số món hiển thị: " + listHangHoa.size());
        for (int i = 0; i < listHangHoa.size(); i++) {
            Log.d("OderActivity", "  [" + i + "] " + listHangHoa.get(i).getTenHangHoa() + 
                  " - SL: " + listHDCT.get(i).getSoLuong());
        }
        
        // ✅ Nếu adapter đã tồn tại, update data và notify
        if (adapter != null) {
            Log.d("OderActivity", "🔄 Adapter đã tồn tại - updating data...");
            Log.d("OderActivity", "  Old size: " + adapter.getItemCount());
            Log.d("OderActivity", "  New size: " + listHangHoa.size());
            
            // Update data trong adapter
            adapter.updateData(listHangHoa, listHDCT);
            
            // ✅ CRITICAL: Phải chạy notify trên UI thread
            runOnUiThread(() -> {
                try {
                    // Đảm bảo adapter vẫn được gán vào RecyclerView
                    if (recyclerViewThucUong.getAdapter() != adapter) {
                        recyclerViewThucUong.setAdapter(adapter);
                        Log.d("OderActivity", "⚠ Adapter đã bị mất - reassign lại");
                    }
                    
                    // Notify adapter thay đổi
                    adapter.notifyDataSetChanged();
                    Log.d("OderActivity", "✓ Adapter.notifyDataSetChanged() đã được gọi");
                    
                    // Force RecyclerView refresh
                    recyclerViewThucUong.post(() -> {
                        recyclerViewThucUong.invalidate();
                        recyclerViewThucUong.requestLayout();
                        Log.d("OderActivity", "✓ RecyclerView đã được invalidate và requestLayout");
                    });
                } catch (Exception e) {
                    Log.e("OderActivity", "❌ Lỗi khi update adapter", e);
                }
            });
            
            Log.d("OderActivity", "✓ Adapter data update request đã được gửi");
            return;
        }
        
        // ✅ Nếu adapter chưa tồn tại, tạo mới
        Log.d("OderActivity", "🆕 Tạo adapter mới...");
        
        // Chỉ setup LayoutManager 1 lần
        if (recyclerViewThucUong.getLayoutManager() == null) {
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
            recyclerViewThucUong.setLayoutManager(linearLayoutManager);
        }
        
        adapter = new HoaDonChiTietMainAdapter(this, listHangHoa, listHDCT,
            new ItemTangGiamSoLuongOnClick() {
                @Override
                public void itemOclick(View view, int indext, HoaDonChiTiet hoaDonChiTiet, HangHoa hangHoa) {
                    // ✅ Cập nhật số lượng và giá tiền vào object
                    hoaDonChiTiet.setSoLuong(indext);
                    hoaDonChiTiet.setGiaTien(indext * hangHoa.getGiaTien());
                    
                    Log.d("OderActivity", "Cập nhật món: " + hangHoa.getTenHangHoa() + 
                          ", số lượng mới: " + indext + ", giá: " + hoaDonChiTiet.getGiaTien());
                    
                    // Cập nhật lên Firebase
                    hoaDonChiTietDAO.updateHoaDonChiTiet(hoaDonChiTiet);
                    
                    // Cập nhật tổng tiền
                    fillActivity();
                }

                @Override
                public void itemOclickDeleteHDCT(View view, HoaDonChiTiet hoaDonChiTiet) {
                    // Xoá món
                    AlertDialog.Builder builder = new AlertDialog.Builder(OderActivity.this, R.style.AlertDialogTheme);
                    builder.setMessage("Xoá món này?");
                    builder.setPositiveButton("Xoá", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Xóa từ Firebase (deleteHoaDonChiTiet sẽ tự sync lên Firebase)
                            if(hoaDonChiTietDAO.deleteHoaDonChiTiet(String.valueOf(hoaDonChiTiet.getMaHDCT()))){
                                MyToast.successful(OderActivity.this, "Xoá món thành công");
                                // ✅ REAL-TIME MODE: Không cần gọi loadData() 
                                // Real-time listener sẽ tự động trigger callback khi Firebase update
                                Log.d("OderActivity", "✓ Đã xóa món - real-time listener sẽ tự động update UI");
                                // fillActivity() sẽ được gọi tự động bởi real-time callback
                            }else {
                                MyToast.error(OderActivity.this, "Xoá không thành công");
                            }
                        }
                    });
                    builder.setNegativeButton("Huỷ", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });
        
        recyclerViewThucUong.setAdapter(adapter);
    }

    @SuppressLint("SetTextI18n")
    private void fillActivity() {
        Log.d("OderActivity", "╔═══════════════════════════════╗");
        Log.d("OderActivity", "║      FILL ACTIVITY            ║");
        Log.d("OderActivity", "╚═══════════════════════════════╝");
        
        HoaDon hoaDon = getHoaDon();
        
        // Kiểm tra hóa đơn có tồn tại không
        if (hoaDon == null || hoaDon.getMaHoaDon() == 0) {
            Log.w("OderActivity", "⚠ Hóa đơn null hoặc maHoaDon=0");
            tvMaBan.setText("Bàn BO" + maBan);
            tvGioVao.setText("-");
            tvTamTinh.setText("0VND");
            tvHoaDonCuoi.setText("0VND");
            return;
        }
        
        Log.d("OderActivity", "➤ Load giá tiền cho hóa đơn: " + hoaDon.getMaHoaDon());
        
        // Hiển thị thông tin hóa đơn của bàn này
        tvMaBan.setText("Bàn BO" + hoaDon.getMaBan());
        tvGioVao.setText(XDate.toStringDateTime(hoaDon.getGioVao()));
        
        // Tính tổng tiền TRỰC TIẾP từ Firebase
        hoaDonChiTietDAO.getByMaHoaDonFromFirebaseDirect(hoaDon.getMaHoaDon(), 
            new HoaDonChiTietDAO.OnHoaDonChiTietListListener() {
                @Override
                public void onListReceived(ArrayList<HoaDonChiTiet> listHDCT) {
                    Log.d("OderActivity", "▶ Callback fillActivity: Nhận " + listHDCT.size() + " chi tiết hóa đơn");
                    
                    // Tính tổng tiền từ danh sách chi tiết
                    long tongTien = 0;
                    for (int i = 0; i < listHDCT.size(); i++) {
                        HoaDonChiTiet hdct = listHDCT.get(i);
                        Log.d("OderActivity", "  [" + i + "] MaHH=" + hdct.getMaHangHoa() + 
                              ", SL=" + hdct.getSoLuong() + 
                              ", GiaTien=" + hdct.getGiaTien() + " VND");
                        tongTien += hdct.getGiaTien();
                    }
                    
                    Log.d("OderActivity", "✓ TỔNG TIỀN = " + tongTien + " VND");
                    
                    final long finalTongTien = tongTien;
                    runOnUiThread(() -> {
                        tvTamTinh.setText(finalTongTien + "VND");
                        tvHoaDonCuoi.setText(finalTongTien + "VND");
                        Log.d("OderActivity", "✓ Đã cập nhật UI: tvTamTinh = " + finalTongTien + " VND");
                    });
                }

                @Override
                public void onError(Exception e) {
                    Log.e("OderActivity", "❌ Lỗi tính tổng tiền từ Firebase", e);
                    runOnUiThread(() -> {
                        tvTamTinh.setText("0VND");
                        tvHoaDonCuoi.setText("0VND");
                    });
                }
            });
    }

    /**
     * Lấy hóa đơn chưa thanh toán của bàn hiện tại
     * 
     * LƯU Ý: Hàm này trả về hóa đơn đã được load từ Firebase (currentHoaDon)
     * Nếu chưa load, sẽ trả về null
     */
    private HoaDon getHoaDon() {
        return currentHoaDon;
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // ✅ REAL-TIME MODE: Không cần reload trong onResume
        // Firebase ValueEventListener đã tự động update UI khi data thay đổi
        if (!isFirstLoad) {
            Log.d("OderActivity", "onResume called");
            Log.d("OderActivity", "📡 Real-time listener đã active - data sẽ tự động update");
            // Chỉ cần update giá tiền (fillActivity lightweight)
            // loadData() sẽ tự động được trigger bởi Firebase listener khi có thay đổi
        } else {
            Log.d("OderActivity", "onResume: Lần đầu tiên, đang load data trong onCreate");
            isFirstLoad = false; // Đánh dấu đã load lần đầu
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Dừng TẤT CẢ Firebase listeners khi activity bị destroy
        hoaDonDAO.stopFirebaseDirectListener();
        hoaDonChiTietDAO.stopFirebaseDirectListener();
        hangHoaDAO.stopFirebaseDirectListener();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001) { // VNPay payment result
            if (resultCode == RESULT_OK && data != null) {
                boolean success = data.getBooleanExtra("success", false);
                String message = data.getStringExtra("message");
                String hoaDonId = data.getStringExtra("hoa_don_id");
                
                if (success) {
                    // Thanh toán thành công
                    HoaDon hoaDon = getHoaDon();
                    hoaDon.setTrangThai(HoaDon.DA_THANH_TOAN);
                    Calendar calendar = Calendar.getInstance();
                    hoaDon.setGioRa(calendar.getTime());
                    _maNguoiDung = sharedPreferences.getString("maNguoiDung", "");
                    hoaDon.setMaKhachHang(_maNguoiDung);
                    
                    Intent intent = getIntent();
                    String maBanStr = intent.getStringExtra(QuanLyBanActivity.MA_BAN);
                    
                    // Load thông tin bàn TRỰC TIẾP từ Firebase
                    banDAO.getByMaBanFromFirebaseDirect(maBanStr, new BanDAO.OnBanListener() {
                        @Override
                        public void onBanReceived(Ban ban) {
                    ban.setTrangThai(Ban.CON_TRONG);
                    
                    if (banDAO.updateBan(ban) && hoaDonDAO.updateHoaDon(hoaDon)) {
                        MyToast.successful(OderActivity.this, message);
                        themThonBaoMoi(hoaDon, calendar);
                        
                        // Quay về màn hình quản lý bàn
                        Intent intent1 = new Intent(OderActivity.this, QuanLyBanActivity.class);
                                OderActivity.maBan = "";
                        startActivity(intent1);
                        finish();
                    }
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e("OderActivity", "Lỗi load bàn từ Firebase trong VNPay callback", e);
                            MyToast.error(OderActivity.this, "Không thể load thông tin bàn");
                        }
                    });
                } else {
                    MyToast.error(OderActivity.this, message);
                }
            }
        }
    }

    /**
     * Lấy địa chỉ IP local của thiết bị
     */
    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); 
                 en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); 
                     enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && 
                        inetAddress instanceof java.net.Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            Log.e("IPAddress", "Error getting IP address", e);
        }
        return "127.0.0.1";
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.anim_in_left, R.anim.anim_out_right);
    }

}