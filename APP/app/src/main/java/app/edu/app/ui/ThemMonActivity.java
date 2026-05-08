package app.edu.app.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

import app.edu.app.R;
import app.edu.app.adapter.LoaiHangAdapter;
import app.edu.app.adapter.ThucUongOderThemAdapter;
import app.edu.app.dao.HoaDonChiTietDAO;
import app.edu.app.interfaces.ItemLoaiHangOnClick;
import app.edu.app.interfaces.ItemOderOnClick;
import app.edu.app.model.HangHoa;
import app.edu.app.model.HoaDonChiTiet;
import app.edu.app.model.LoaiHang;
import app.edu.app.utils.MyToast;
import app.edu.app.utils.SyncUtils;

public class ThemMonActivity extends AppCompatActivity {
    private static final String TAG = "ThemMonActivity";
    
    Toolbar toolbar;
    RecyclerView recyclerViewThucUongOder, recyclerViewLoaiMon;
    TextView tvDanhSachMonTitle;
    HoaDonChiTietDAO hoaDonChiTietDAO;
    ThucUongOderThemAdapter thucUongAdapter;
    LoaiHangAdapter loaiHangAdapter;
    
    // Firebase
    private DatabaseReference databaseReference;
    private DatabaseReference hangHoaRef;
    private DatabaseReference loaiHangRef;
    
    // Data lists
    private ArrayList<HangHoa> allHangHoaList = new ArrayList<>();
    private ArrayList<LoaiHang> allLoaiHangList = new ArrayList<>();
    private ArrayList<HangHoa> filteredHangHoaList = new ArrayList<>();
    
    private int selectedLoaiHang = -1; // -1 = Tất cả
    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oder_them_mon);
        
        initToolBar();
        initView();
        initFirebase();
        
        hoaDonChiTietDAO = new HoaDonChiTietDAO(this);
        
        // Show loading
        showLoading();
        
        // Load data from Firebase
        loadDataFromFirebase();
    }
    
    private void initFirebase() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
        hangHoaRef = databaseReference.child("HangHoa");
        loaiHangRef = databaseReference.child("LoaiHang");
    }
    
    private void showLoading() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tải dữ liệu...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }
    
    private void hideLoading() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    /**
     * Load data from Firebase Realtime Database
     */
    private void loadDataFromFirebase() {
        // Load both LoaiHang and HangHoa
        loadLoaiHangFromFirebase();
        loadHangHoaFromFirebase();
    }
    
    /**
     * Load LoaiHang from Firebase
     */
    private void loadLoaiHangFromFirebase() {
        loaiHangRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                allLoaiHangList.clear();
                
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            LoaiHang loaiHang = SyncUtils.convertMapToLoaiHang(map);
                            allLoaiHangList.add(loaiHang);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing LoaiHang: " + e.getMessage());
                    }
                }
                
                Log.d(TAG, "Loaded " + allLoaiHangList.size() + " loại hàng from Firebase");
                setupLoaiHangRecyclerView();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error loading LoaiHang: " + databaseError.getMessage());
                hideLoading();
                Toast.makeText(ThemMonActivity.this, "Lỗi tải loại món", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Load HangHoa from Firebase
     */
    private void loadHangHoaFromFirebase() {
        hangHoaRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                allHangHoaList.clear();
                
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            HangHoa hangHoa = SyncUtils.convertMapToHangHoa(map);
                            
                            // Chỉ thêm món còn hàng (STATUS_STILL)
                            if (hangHoa.getTrangThai() == HangHoa.STATUS_STILL) {
                                allHangHoaList.add(hangHoa);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing HangHoa: " + e.getMessage());
                    }
                }
                
                Log.d(TAG, "Loaded " + allHangHoaList.size() + " hàng hóa from Firebase");
                
                // Initialize filtered list with all items
                filteredHangHoaList = new ArrayList<>(allHangHoaList);
                
                setupHangHoaRecyclerView();
                hideLoading();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error loading HangHoa: " + databaseError.getMessage());
                hideLoading();
                Toast.makeText(ThemMonActivity.this, "Lỗi tải danh sách món", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Setup LoaiHang RecyclerView
     */
    private void setupLoaiHangRecyclerView() {
        LinearLayoutManager loaiMonLayoutManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        recyclerViewLoaiMon.setLayoutManager(loaiMonLayoutManager);
        
        // Add "Tất cả" option at the beginning
        ArrayList<LoaiHang> displayList = new ArrayList<>();
        
        // Create "Tất cả" item
        LoaiHang tatCa = new LoaiHang();
        tatCa.setMaLoai(-1);
        tatCa.setTenLoai("Tất cả");
        displayList.add(tatCa);
        
        // Add all categories
        displayList.addAll(allLoaiHangList);
        
        loaiHangAdapter = new LoaiHangAdapter(displayList, new ItemLoaiHangOnClick() {
            @Override
            public void itemOclick(View view, LoaiHang loaiHang) {
                selectedLoaiHang = loaiHang.getMaLoai();
                
                if (loaiHang.getMaLoai() == -1) {
                    tvDanhSachMonTitle.setText("Tất cả món");
                } else {
                    tvDanhSachMonTitle.setText(loaiHang.getTenLoai());
                }
                
                filterHangHoaByLoai(selectedLoaiHang);
            }
        });
        recyclerViewLoaiMon.setAdapter(loaiHangAdapter);
        
        // Auto select "Tất cả" at start
        loaiHangAdapter.resetSelection();
    }
    
    /**
     * Setup HangHoa RecyclerView
     */
    private void setupHangHoaRecyclerView() {
        LinearLayoutManager hangHoaLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerViewThucUongOder.setLayoutManager(hangHoaLayoutManager);
        
        thucUongAdapter = new ThucUongOderThemAdapter(filteredHangHoaList, new ItemOderOnClick() {
            @Override
            public void itemOclick(View view, HangHoa hangHoa) {
                addHangHoaToHoaDon(hangHoa);
            }
        });
        recyclerViewThucUongOder.setAdapter(thucUongAdapter);
    }
    
    /**
     * Add HangHoa to HoaDon
     */
    private void addHangHoaToHoaDon(HangHoa hangHoa) {
        Intent intent = getIntent();
        String maHoaDon = intent.getStringExtra(MangVeActivity.MA_HOA_DON);
        
        HoaDonChiTiet hoaDonChiTiet = new HoaDonChiTiet();
        hoaDonChiTiet.setMaHoaDon(Integer.parseInt(maHoaDon));
        hoaDonChiTiet.setMaHangHoa(hangHoa.getMaHangHoa());
        hoaDonChiTiet.setSoLuong(1);
        hoaDonChiTiet.setGiaTien(hangHoa.getGiaTien() * hoaDonChiTiet.getSoLuong());
        
        Calendar calendar = Calendar.getInstance();
        hoaDonChiTiet.setNgayXuatHoaDon(calendar.getTime());
        
        if(hoaDonChiTietDAO.insertHoaDonChiTiet(hoaDonChiTiet)){
            MyToast.successful(ThemMonActivity.this, "Thêm thành công " + hangHoa.getTenHangHoa());
        } else {
            MyToast.error(ThemMonActivity.this, "Thêm thất bại");
        }
    }
    
    /**
     * Filter hàng hóa theo loại
     */
    private void filterHangHoaByLoai(int maLoai) {
        filteredHangHoaList.clear();
        
        if (maLoai == -1) {
            // Hiển thị tất cả
            filteredHangHoaList.addAll(allHangHoaList);
            tvDanhSachMonTitle.setText("Tất cả món");
        } else {
            // Filter theo loại
            for (HangHoa hangHoa : allHangHoaList) {
                if (hangHoa.getMaLoai() == maLoai) {
                    filteredHangHoaList.add(hangHoa);
                }
            }
        }
        
        Log.d(TAG, "Filtered " + filteredHangHoaList.size() + " items for loại: " + maLoai);
        
        // Update adapter với data mới
        if (thucUongAdapter != null) {
            thucUongAdapter = new ThucUongOderThemAdapter(filteredHangHoaList, new ItemOderOnClick() {
                @Override
                public void itemOclick(View view, HangHoa hangHoa) {
                    addHangHoaToHoaDon(hangHoa);
                }
            });
            recyclerViewThucUongOder.setAdapter(thucUongAdapter);
        }
    }

    private void initToolBar() {
        toolbar = findViewById(R.id.toolbarThemMon);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ThemMonActivity.this, OderActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void initView() {
        recyclerViewLoaiMon = findViewById(R.id.recyclerViewLoaiMon);
        recyclerViewThucUongOder = findViewById(R.id.recyclerViewThucUongOder);
        tvDanhSachMonTitle = findViewById(R.id.tvDanhSachMonTitle);
        
        // Click title to show all
        tvDanhSachMonTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedLoaiHang = -1;
                tvDanhSachMonTitle.setText("Tất cả món");
                
                // Reset selection in adapter
                if (loaiHangAdapter != null) {
                    loaiHangAdapter.resetSelection();
                }
                
                filterHangHoaByLoai(-1);
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideLoading();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(ThemMonActivity.this, OderActivity.class);
        startActivity(intent);
        finish();
    }
}