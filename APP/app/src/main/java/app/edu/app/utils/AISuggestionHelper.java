package app.edu.app.utils;

import android.content.Context;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import app.edu.app.dao.HangHoaDAO;
import app.edu.app.dao.HoaDonChiTietDAO;
import app.edu.app.dao.HoaDonDAO;
import app.edu.app.model.HangHoa;
import app.edu.app.model.HoaDon;
import app.edu.app.model.HoaDonChiTiet;
import app.edu.app.service.OpenAIService;

/**
 * Helper class để phân tích dữ liệu hóa đơn và tạo gợi ý bằng AI
 */
public class AISuggestionHelper {
    private static final String TAG = "AISuggestionHelper";
    
    private Context context;
    private HoaDonDAO hoaDonDAO;
    private HoaDonChiTietDAO hoaDonChiTietDAO;
    private HangHoaDAO hangHoaDAO;
    private OpenAIService openAIService;
    
    public AISuggestionHelper(Context context) {
        this.context = context;
        this.hoaDonDAO = new HoaDonDAO(context);
        this.hoaDonChiTietDAO = new HoaDonChiTietDAO(context);
        this.hangHoaDAO = new HangHoaDAO(context);
        this.openAIService = new OpenAIService();
    }
    
    /**
     * Lấy gợi ý đồ uống dựa trên lịch sử đặt hàng của khách hàng
     * @param maKhachHang Mã khách hàng
     * @param callback Callback để nhận kết quả
     */
    public void getDrinkSuggestions(String maKhachHang, SuggestionCallback callback) {
        // Lấy dữ liệu hóa đơn đã thanh toán của khách hàng
        ArrayList<HoaDon> hoaDons = hoaDonDAO.getByMaKhachHang(maKhachHang);
        
        // Lọc chỉ lấy hóa đơn đã thanh toán
        List<HoaDon> paidOrders = new ArrayList<>();
        for (HoaDon hoaDon : hoaDons) {
            if (hoaDon.getTrangThai() == HoaDon.DA_THANH_TOAN) {
                paidOrders.add(hoaDon);
            }
        }
        
        if (paidOrders.isEmpty()) {
            // Nếu chưa có hóa đơn nào, gợi ý dựa trên menu phổ biến
            getGeneralSuggestions(callback);
            return;
        }
        
        // Phân tích dữ liệu và tạo prompt
        String prompt = buildPrompt(paidOrders);
        Log.d(TAG, "Generated prompt: " + prompt);
        
        // Gọi AI service
        openAIService.getSuggestion(prompt, new OpenAIService.AISuggestionCallback() {
            @Override
            public void onSuccess(String suggestion) {
                if (callback != null) {
                    callback.onSuccess(suggestion);
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "AI suggestion error: " + error);
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }
    
    /**
     * Lấy gợi ý chung khi chưa có lịch sử đặt hàng
     */
    private void getGeneralSuggestions(SuggestionCallback callback) {
        // Lấy tất cả đồ uống có sẵn
        ArrayList<HangHoa> allDrinks = hangHoaDAO.getAll();
        
        StringBuilder menuList = new StringBuilder();
        for (HangHoa hangHoa : allDrinks) {
            if (hangHoa.getTrangThai() == HangHoa.STATUS_STILL) {
                menuList.append("- ").append(hangHoa.getTenHangHoa())
                        .append(" (").append(hangHoa.getGiaTien()).append(" VND)\n");
            }
        }
        
        String prompt = "Bạn là một chuyên gia tư vấn đồ uống tại quán cà phê. " +
                "Dựa vào menu sau đây, hãy đưa ra 3-5 gợi ý đồ uống phổ biến và ngon nhất:\n\n" +
                "MENU:\n" + menuList.toString() + "\n\n" +
                "Hãy đưa ra gợi ý một cách thân thiện, ngắn gọn (không quá 200 từ), " +
                "và giải thích tại sao nên chọn những đồ uống này.";
        
        openAIService.getSuggestion(prompt, new OpenAIService.AISuggestionCallback() {
            @Override
            public void onSuccess(String suggestion) {
                if (callback != null) {
                    callback.onSuccess(suggestion);
                }
            }
            
            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }
    
    /**
     * Xây dựng prompt dựa trên lịch sử đặt hàng theo ngày
     */
    private String buildPrompt(List<HoaDon> hoaDons) {
        // Phân tích các đồ uống đã đặt theo ngày trong tuần và thời gian
        Map<String, Integer> drinkFrequency = new HashMap<>();
        Map<String, Integer> drinkTotalQuantity = new HashMap<>();
        Map<String, Map<String, Integer>> drinkByDayOfWeek = new HashMap<>(); // Đồ uống theo ngày trong tuần
        Map<String, Map<String, Integer>> drinkByTimeOfDay = new HashMap<>(); // Đồ uống theo thời gian trong ngày
        
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        // Lấy thông tin ngày hiện tại
        Calendar now = Calendar.getInstance();
        int currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        String currentDayName = getDayName(currentDayOfWeek);
        String currentTimePeriod = getTimePeriod(currentHour);
        
        for (HoaDon hoaDon : hoaDons) {
            if (hoaDon.getGioVao() == null) continue;
            
            calendar.setTime(hoaDon.getGioVao());
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            String dayName = getDayName(dayOfWeek);
            String timePeriod = getTimePeriod(hour);
            
            ArrayList<HoaDonChiTiet> chiTiets = hoaDonChiTietDAO.getByMaHoaDon(
                    String.valueOf(hoaDon.getMaHoaDon()));
            
            for (HoaDonChiTiet chiTiet : chiTiets) {
                HangHoa hangHoa = hangHoaDAO.getByMaHangHoa(
                        String.valueOf(chiTiet.getMaHangHoa()));
                
                if (hangHoa != null) {
                    String tenHangHoa = hangHoa.getTenHangHoa();
                    
                    // Đếm tần suất tổng
                    drinkFrequency.put(tenHangHoa, 
                            drinkFrequency.getOrDefault(tenHangHoa, 0) + 1);
                    
                    // Đếm tổng số lượng
                    drinkTotalQuantity.put(tenHangHoa,
                            drinkTotalQuantity.getOrDefault(tenHangHoa, 0) + chiTiet.getSoLuong());
                    
                    // Đếm theo ngày trong tuần
                    if (!drinkByDayOfWeek.containsKey(tenHangHoa)) {
                        drinkByDayOfWeek.put(tenHangHoa, new HashMap<>());
                    }
                    Map<String, Integer> dayMap = drinkByDayOfWeek.get(tenHangHoa);
                    dayMap.put(dayName, dayMap.getOrDefault(dayName, 0) + 1);
                    
                    // Đếm theo thời gian trong ngày
                    if (!drinkByTimeOfDay.containsKey(tenHangHoa)) {
                        drinkByTimeOfDay.put(tenHangHoa, new HashMap<>());
                    }
                    Map<String, Integer> timeMap = drinkByTimeOfDay.get(tenHangHoa);
                    timeMap.put(timePeriod, timeMap.getOrDefault(timePeriod, 0) + 1);
                }
            }
        }
        
        // Tạo danh sách đồ uống đã đặt với phân tích theo ngày
        StringBuilder orderHistory = new StringBuilder();
        orderHistory.append("LỊCH SỬ ĐẶT HÀNG TỔNG QUAN:\n");
        for (Map.Entry<String, Integer> entry : drinkFrequency.entrySet()) {
            String drinkName = entry.getKey();
            int frequency = entry.getValue();
            int totalQuantity = drinkTotalQuantity.getOrDefault(drinkName, 0);
            orderHistory.append("- ").append(drinkName)
                    .append(": Đã đặt ").append(frequency).append(" lần, ")
                    .append("Tổng số lượng: ").append(totalQuantity).append("\n");
        }
        
        // Phân tích theo ngày trong tuần
        orderHistory.append("\nTHÓI QUEN THEO NGÀY TRONG TUẦN:\n");
        for (Map.Entry<String, Map<String, Integer>> entry : drinkByDayOfWeek.entrySet()) {
            String drinkName = entry.getKey();
            Map<String, Integer> dayMap = entry.getValue();
            orderHistory.append("- ").append(drinkName).append(": ");
            List<String> dayList = new ArrayList<>();
            for (Map.Entry<String, Integer> dayEntry : dayMap.entrySet()) {
                dayList.add(dayEntry.getKey() + " (" + dayEntry.getValue() + " lần)");
            }
            orderHistory.append(String.join(", ", dayList)).append("\n");
        }
        
        // Phân tích theo thời gian trong ngày
        orderHistory.append("\nTHÓI QUEN THEO THỜI GIAN TRONG NGÀY:\n");
        for (Map.Entry<String, Map<String, Integer>> entry : drinkByTimeOfDay.entrySet()) {
            String drinkName = entry.getKey();
            Map<String, Integer> timeMap = entry.getValue();
            orderHistory.append("- ").append(drinkName).append(": ");
            List<String> timeList = new ArrayList<>();
            for (Map.Entry<String, Integer> timeEntry : timeMap.entrySet()) {
                timeList.add(timeEntry.getKey() + " (" + timeEntry.getValue() + " lần)");
            }
            orderHistory.append(String.join(", ", timeList)).append("\n");
        }
        
        // Thông tin ngày hiện tại
        orderHistory.append("\nTHÔNG TIN HIỆN TẠI:\n");
        orderHistory.append("- Ngày hôm nay: ").append(currentDayName).append("\n");
        orderHistory.append("- Thời gian hiện tại: ").append(currentTimePeriod).append("\n");
        
        // Lấy menu hiện tại
        ArrayList<HangHoa> allDrinks = hangHoaDAO.getAll();
        StringBuilder menuList = new StringBuilder();
        menuList.append("\nMENU HIỆN TẠI:\n");
        for (HangHoa hangHoa : allDrinks) {
            if (hangHoa.getTrangThai() == HangHoa.STATUS_STILL) {
                menuList.append("- ").append(hangHoa.getTenHangHoa())
                        .append(" (").append(hangHoa.getGiaTien()).append(" VND)\n");
            }
        }
        
        // Tạo prompt với phân tích theo ngày
        String prompt = "Bạn là một chuyên gia tư vấn đồ uống tại quán cà phê. " +
                "Dựa vào lịch sử đặt hàng theo ngày và menu hiện tại của khách hàng, " +
                "hãy đưa ra 3-5 gợi ý đồ uống phù hợp với ngày hôm nay.\n\n" +
                orderHistory.toString() + "\n" +
                menuList.toString() + "\n\n" +
                "YÊU CẦU:\n" +
                "1. Phân tích sở thích của khách hàng dựa trên lịch sử đặt hàng\n" +
                "2. Ưu tiên gợi ý các đồ uống mà khách hàng thường đặt vào " + currentDayName + " và thời gian " + currentTimePeriod + "\n" +
                "3. Nếu khách hàng chưa có thói quen vào " + currentDayName + ", gợi ý dựa trên thói quen chung hoặc đồ uống phổ biến\n" +
                "4. Gợi ý các đồ uống mới hoặc tương tự từ menu hiện tại\n" +
                "5. Giải thích ngắn gọn tại sao nên chọn những đồ uống này, có thể đề cập đến thói quen đặt hàng\n" +
                "6. Trả lời bằng tiếng Việt, thân thiện và ngắn gọn (không quá 250 từ)\n" +
                "7. Format: Danh sách gợi ý với số thứ tự, mỗi gợi ý có tên đồ uống và lý do\n" +
                "8. QUAN TRỌNG: Tên đồ uống phải CHÍNH XÁC với tên trong menu, đặt trong dấu ngoặc kép \"\"\n" +
                "9. Ví dụ format: 1. \"Cà phê đen\" - Lý do...";
        
        return prompt;
    }
    
    /**
     * Lấy tên ngày trong tuần
     */
    private String getDayName(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY:
                return "Thứ Hai";
            case Calendar.TUESDAY:
                return "Thứ Ba";
            case Calendar.WEDNESDAY:
                return "Thứ Tư";
            case Calendar.THURSDAY:
                return "Thứ Năm";
            case Calendar.FRIDAY:
                return "Thứ Sáu";
            case Calendar.SATURDAY:
                return "Thứ Bảy";
            case Calendar.SUNDAY:
                return "Chủ Nhật";
            default:
                return "Không xác định";
        }
    }
    
    /**
     * Lấy khoảng thời gian trong ngày
     */
    private String getTimePeriod(int hour) {
        if (hour >= 5 && hour < 11) {
            return "Sáng";
        } else if (hour >= 11 && hour < 14) {
            return "Trưa";
        } else if (hour >= 14 && hour < 18) {
            return "Chiều";
        } else if (hour >= 18 && hour < 22) {
            return "Tối";
        } else {
            return "Đêm";
        }
    }
    
    /**
     * Interface callback cho suggestion
     */
    public interface SuggestionCallback {
        void onSuccess(String suggestion);
        void onError(String error);
    }
}

