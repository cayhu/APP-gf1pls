import { useEffect, useState, useMemo } from 'react';
import { getAllFromRealtimeDB, setRealtimeDB, REALTIME_NODES } from '../utils/realtimeDB';
import { parseRealtimeDateTime } from '../utils/dateUtils';
import { format } from 'date-fns';
import { vi } from 'date-fns/locale';
import { Plus, Trash2, Layers, Calendar, CalendarCheck, RefreshCw } from 'lucide-react';
import toast from 'react-hot-toast';
import FilterPanel from '../components/FilterPanel';
import StatusBadge from '../components/StatusBadge';
import SortPanel from '../components/SortPanel';

/**
 * Trang quản lý bàn (Web Admin)
 * 
 * MỤC ĐÍCH:
 * - Quản lý danh sách các bàn trong hệ thống
 * - Hiển thị trạng thái CHÍNH XÁC của bàn dựa trên:
 *   1. Ban.trangThai - Trạng thái hiện tại (có khách đang ngồi)
 *   2. DatBan - Các đặt bàn đã duyệt và chưa sử dụng
 * 
 * LOGIC TRẠNG THÁI:
 * - 0 (CON_TRONG): Bàn trống, chưa có khách và chưa có đặt bàn
 * - 1 (CO_KHACH): Bàn đang có khách ngồi (từ Ban.trangThai)
 * - 2 (DA_DAT): Bàn đã được đặt (từ DatBan đã duyệt, chưa sử dụng)
 * 
 * KIỂM TRA TỪ DATBAN:
 * - Chỉ tính các đặt bàn đã duyệt (trangThai = 1)
 * - Chỉ tính các đặt bàn chưa sử dụng (ngayGioSuDung >= hiện tại)
 * - Kiểm tra theo maBan để xác định bàn nào đã được đặt
 */
const QuanLyBan = () => {
  const [banList, setBanList] = useState([]);
  const [datBanList, setDatBanList] = useState([]);
  const [filteredList, setFilteredList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedDate, setSelectedDate] = useState(() => {
    // Mặc định là hôm nay (format: yyyy-MM-dd)
    const today = new Date();
    return format(today, 'yyyy-MM-dd');
  });
  const [filters, setFilters] = useState({
    trangThai: ''
  });
  const [sortBy, setSortBy] = useState('maBan:asc');

  useEffect(() => {
    loadData();
    
    // Auto-refresh mỗi 30 giây để cập nhật trạng thái bàn
    const interval = setInterval(() => {
      loadData();
    }, 30000); // 30 giây
    
    return () => clearInterval(interval);
  }, []);

  /**
   * Load all data: Ban và DatBan
   */
  const loadData = async () => {
    setLoading(true);
    try {
      const [banData, datBanData] = await Promise.all([
        getAllFromRealtimeDB(REALTIME_NODES.BAN, true),
        getAllFromRealtimeDB(REALTIME_NODES.DAT_BAN, true)
      ]);
      
      setBanList(banData.filter(b => b !== null));
      setDatBanList(datBanData.filter(db => db !== null));
    } catch (error) {
      console.error('Error loading data:', error);
      toast.error('Lỗi khi tải dữ liệu');
    } finally {
      setLoading(false);
    }
  };

  /**
   * Kiểm tra xem một ngày có nằm trong cùng ngày với ngày được chọn không
   */
  const isSameDate = (date1, date2) => {
    if (!date1 || !date2) return false;
    const d1 = new Date(date1);
    const d2 = new Date(date2);
    return d1.getFullYear() === d2.getFullYear() &&
           d1.getMonth() === d2.getMonth() &&
           d1.getDate() === d2.getDate();
  };

  /**
   * Tính trạng thái bàn dựa trên Ban và DatBan cho ngày được chọn
   * LOGIC KIỂM TRA (theo thứ tự ưu tiên):
   * 1. Kiểm tra Ban.trangThai === 1 → "Có khách" (ưu tiên cao nhất, chỉ khi là hôm nay)
   * 2. Kiểm tra DatBan trong ngày được chọn → "Đã đặt"
   * 3. Còn lại → "Còn trống"
   * 
   * @param {Object} ban - Thông tin bàn từ bảng Ban
   * @returns {Object} - Ban với trạng thái được tính toán chính xác
   */
  const calculateBanStatus = useMemo(() => {
    return (ban) => {
      const maBan = typeof ban.maBan === 'number' ? ban.maBan : parseInt(ban.maBan || ban.id);
      const selectedDateObj = new Date(selectedDate + 'T00:00:00');
      const now = new Date();
      const isToday = isSameDate(selectedDateObj, now);
      
      // Tìm tất cả các đặt bàn cho bàn này trong ngày được chọn
      const datBanInDay = datBanList.filter(db => {
        if (!db || db === null) return false;
        
        // So sánh maBan
        const dbMaBan = typeof db.maBan === 'number' 
          ? db.maBan 
          : (typeof db.maBan === 'string' ? parseInt(db.maBan) : parseInt(db.id));
        
        if (dbMaBan !== maBan) return false;
        
        // Kiểm tra ngày sử dụng
        const ngayGioSuDung = parseRealtimeDateTime(db.ngayGioSuDung);
        if (!ngayGioSuDung) return false;
        
        // Kiểm tra xem có cùng ngày với ngày được chọn không
        return isSameDate(ngayGioSuDung, selectedDateObj);
      });
      
      // 1. KIỂM TRA TRẠNG THÁI HIỆN TẠI TỪ BAN (chỉ khi là hôm nay)
      // Nếu bàn đang có khách ngồi (trangThai = 1) và là hôm nay → Ưu tiên cao nhất
      if (isToday && ban.trangThai === 1) {
        return {
          ...ban,
          trangThai: 1, // CO_KHACH
          trangThaiText: 'Có khách',
          datBanInfo: null,
          datBanInDay: datBanInDay
        };
      }
      
      // 2. KIỂM TRA DATBAN TRONG NGÀY ĐƯỢC CHỌN
      // Tìm đặt bàn đã duyệt (trangThai = 1) trong ngày
      const datBanActive = datBanInDay.find(db => {
        const trangThai = typeof db.trangThai === 'number' 
          ? db.trangThai 
          : parseInt(db.trangThai);
        return trangThai === 1; // Đã duyệt
      });
      
      if (datBanActive) {
        return {
          ...ban,
          trangThai: 2, // DA_DAT
          trangThaiText: 'Đã đặt',
          datBanInfo: datBanActive,
          datBanInDay: datBanInDay
        };
      }
      
      // 3. BÀN TRỐNG (không có khách và không có đặt bàn trong ngày)
      return {
        ...ban,
        trangThai: 0, // CON_TRONG
        trangThaiText: 'Còn trống',
        datBanInfo: null,
        datBanInDay: datBanInDay
      };
    };
  }, [datBanList, selectedDate]);

  /**
   * Enrich banList với trạng thái từ DatBan
   */
  const enrichedBanList = useMemo(() => {
    return banList.map(ban => calculateBanStatus(ban));
  }, [banList, calculateBanStatus]);

  // Filter và Sort
  useEffect(() => {
    let result = [...enrichedBanList];

    // Apply filters
    if (filters.trangThai !== '') {
      result = result.filter(item => item.trangThai === parseInt(filters.trangThai));
    }

    // Apply sort
    const [field, direction] = sortBy.split(':');
    result.sort((a, b) => {
      let aValue, bValue;
      
      switch (field) {
        case 'maBan':
          aValue = a.maBan || parseInt(a.id) || 0;
          bValue = b.maBan || parseInt(b.id) || 0;
          break;
        case 'trangThai':
          aValue = a.trangThai || 0;
          bValue = b.trangThai || 0;
          break;
        default:
          aValue = a.maBan || 0;
          bValue = b.maBan || 0;
      }

      if (direction === 'asc') {
        return aValue > bValue ? 1 : -1;
      } else {
        return aValue < bValue ? 1 : -1;
      }
    });

    setFilteredList(result);
  }, [enrichedBanList, filters, sortBy]);

  const handleFilterChange = (key, value) => {
    setFilters(prev => ({
      ...prev,
      [key]: value
    }));
  };

  const handleResetFilters = () => {
    setFilters({
      trangThai: ''
    });
  };

  const handleAddBan = async () => {
    if (!window.confirm('Bạn có chắc muốn thêm bàn mới?')) {
      return;
    }

    try {
      const maxBan = banList.reduce((max, ban) => {
        const num = ban.maBan || parseInt(ban.id) || 0;
        return num > max ? num : max;
      }, 0);

      const newBanNumber = maxBan + 1;
      const now = Date.now();
      
      const allBan = await getAllFromRealtimeDB(REALTIME_NODES.BAN, true);
      const banArray = [];
      banArray[0] = null;
      
      allBan.forEach(ban => {
        const index = ban.maBan || parseInt(ban.id);
        if (index > 0) {
          banArray[index] = {
            maBan: index,
            trangThai: ban.trangThai || 0,
            lastModified: ban.lastModified || now
          };
        }
      });
      
      banArray[newBanNumber] = {
        maBan: newBanNumber,
        trangThai: 0,
        lastModified: now
      };
      
      await setRealtimeDB(REALTIME_NODES.BAN, banArray);
      
      toast.success('Thêm bàn mới thành công');
      loadData();
    } catch (error) {
      console.error('Error adding ban:', error);
      toast.error('Lỗi khi thêm bàn');
    }
  };

  const handleDeleteBan = async (ban) => {
    if (!window.confirm(`Bạn có chắc muốn xóa bàn ${ban.maBan || ban.id}?`)) {
      return;
    }

    try {
      const allBan = await getAllFromRealtimeDB(REALTIME_NODES.BAN, true);
      const banArray = [];
      banArray[0] = null;
      
      allBan.forEach(b => {
        const index = b.maBan || parseInt(b.id);
        if (index > 0 && index !== (ban.maBan || parseInt(ban.id))) {
          banArray[index] = {
            maBan: index,
            trangThai: b.trangThai || 0,
            lastModified: b.lastModified || Date.now()
          };
        }
      });
      
      await setRealtimeDB(REALTIME_NODES.BAN, banArray);
      
      toast.success('Xóa bàn thành công');
      loadData();
    } catch (error) {
      console.error('Error deleting ban:', error);
      toast.error('Lỗi khi xóa bàn');
    }
  };

  // Statistics với trạng thái chính xác
  const statistics = useMemo(() => {
    const total = enrichedBanList.length;
    const conTrong = enrichedBanList.filter(b => b.trangThai === 0).length;
    const coKhach = enrichedBanList.filter(b => b.trangThai === 1).length;
    const daDat = enrichedBanList.filter(b => b.trangThai === 2).length;
    
    return { total, conTrong, coKhach, daDat };
  }, [enrichedBanList]);

  // Filter options với trạng thái mới
  const filterOptions = [
    {
      key: 'trangThai',
      label: 'Trạng thái',
      type: 'select',
      value: filters.trangThai,
      options: [
        { value: '0', label: 'Còn trống' },
        { value: '1', label: 'Có khách' },
        { value: '2', label: 'Đã đặt' }
      ]
    }
  ];

  const sortOptions = [
    { value: 'maBan', label: 'Số bàn' },
    { value: 'trangThai', label: 'Trạng thái' }
  ];

  // Format selectedDate để hiển thị
  const selectedDateDisplay = selectedDate 
    ? format(new Date(selectedDate + 'T00:00:00'), 'dd/MM/yyyy', { locale: vi })
    : '';

  const handleDateChange = (e) => {
    setSelectedDate(e.target.value);
  };

  const handleToday = () => {
    const today = format(new Date(), 'yyyy-MM-dd');
    setSelectedDate(today);
  };

  const isTodaySelected = selectedDate === format(new Date(), 'yyyy-MM-dd');

  return (
    <div className="p-8">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Quản lý Bàn</h1>
          <p className="text-gray-600 mt-2">Quản lý trạng thái các bàn (kiểm tra từ DatBan)</p>
        </div>
        <div className="flex items-center gap-2">
          <button 
            onClick={loadData} 
            disabled={loading}
            className="btn-secondary flex items-center space-x-2 hover:scale-105 transition-transform disabled:opacity-50 disabled:cursor-not-allowed"
            title="Làm mới dữ liệu"
          >
            <RefreshCw className={`w-5 h-5 ${loading ? 'animate-spin' : ''}`} />
            <span>Làm mới</span>
          </button>
          <button onClick={handleAddBan} className="btn-primary flex items-center space-x-2 hover:scale-105 transition-transform">
            <Plus className="w-5 h-5" />
            <span>Thêm bàn</span>
          </button>
        </div>
      </div>

      {/* Date Picker */}
      <div className="mb-6 bg-white p-4 rounded-lg shadow-sm border border-gray-200">
        <div className="flex items-center gap-4 flex-wrap">
          <div className="flex items-center gap-2">
            <CalendarCheck className="w-5 h-5 text-gray-600" />
            <label className="text-sm font-medium text-gray-700">Xem trạng thái ngày:</label>
          </div>
          <input
            type="date"
            value={selectedDate}
            onChange={handleDateChange}
            className="input border-gray-300 focus:border-primary-500 focus:ring-primary-500"
          />
          <button
            onClick={handleToday}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              isTodaySelected
                ? 'bg-primary-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            Hôm nay
          </button>
          {selectedDateDisplay && (
            <div className="text-sm text-gray-600">
              Đang xem: <span className="font-semibold text-gray-900">{selectedDateDisplay}</span>
            </div>
          )}
        </div>
      </div>

      {/* Filters */}
      <FilterPanel
        filters={filterOptions}
        onFilterChange={handleFilterChange}
        onReset={handleResetFilters}
      />

      {/* Sort */}
      <div className="mb-4">
        <SortPanel
          options={sortOptions}
          value={sortBy}
          onChange={setSortBy}
        />
      </div>

      {/* Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
        <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200">
          <div className="text-sm text-gray-600 flex items-center gap-1">
            <Layers className="w-4 h-4" />
            Tổng số bàn
          </div>
          <div className="text-2xl font-bold text-gray-900">{statistics.total}</div>
        </div>
        <div className="bg-green-50 p-4 rounded-lg shadow-sm border border-green-200">
          <div className="text-sm text-green-800">Còn trống</div>
          <div className="text-2xl font-bold text-green-900">
            {statistics.conTrong}
          </div>
        </div>
        <div className="bg-blue-50 p-4 rounded-lg shadow-sm border border-blue-200">
          <div className="text-sm text-blue-800">Có khách</div>
          <div className="text-2xl font-bold text-blue-900">
            {statistics.coKhach}
          </div>
        </div>
        <div className="bg-orange-50 p-4 rounded-lg shadow-sm border border-orange-200">
          <div className="text-sm text-orange-800 flex items-center gap-1">
            <Calendar className="w-4 h-4" />
            Đã đặt
          </div>
          <div className="text-2xl font-bold text-orange-900">
            {statistics.daDat}
          </div>
        </div>
      </div>

      {loading ? (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
          {[1, 2, 3, 4, 5, 6, 7, 8].map((i) => (
            <div key={i} className="card animate-pulse">
              <div className="h-32 bg-gray-200 rounded"></div>
            </div>
          ))}
        </div>
      ) : filteredList.length === 0 ? (
        <div className="card text-center py-12">
          <Layers className="w-16 h-16 mx-auto text-gray-400 mb-4" />
          <p className="text-gray-500 text-lg">Không tìm thấy bàn nào</p>
        </div>
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
          {filteredList.map((ban) => {
            const isCoKhach = ban.trangThai === 1;
            const isDaDat = ban.trangThai === 2;
            const isConTrong = ban.trangThai === 0;
            
            return (
              <div
                key={ban.id || ban.maBan}
                className={`card cursor-pointer transition-all hover:shadow-lg hover:scale-105 ${
                  isCoKhach ? 'border-2 border-blue-300 bg-blue-50' :
                  isDaDat ? 'border-2 border-orange-300 bg-orange-50' :
                  'border-2 border-green-300 bg-green-50'
                }`}
              >
                <div className="text-center">
                  <div className="text-4xl font-bold text-gray-900 mb-3">
                    Bàn {ban.maBan || ban.id}
                  </div>
                  
                  {/* Status Badge */}
                  <div className="mb-4 flex justify-center">
                    {isDaDat ? (
                      <div className="flex flex-col items-center gap-2">
                        <StatusBadge type="ban" value={2} />
                        {ban.datBanInfo && (
                          <div className="text-xs text-orange-700 mt-1 text-center">
                            <div className="flex items-center justify-center gap-1">
                              <Calendar className="w-3 h-3" />
                              <span>
                                {(() => {
                                  const ngaySuDung = parseRealtimeDateTime(ban.datBanInfo.ngayGioSuDung);
                                  if (ngaySuDung) {
                                    return format(ngaySuDung, 'HH:mm', { locale: vi });
                                  }
                                  return 'Đã đặt';
                                })()}
                              </span>
                            </div>
                          </div>
                        )}
                      </div>
                    ) : (
                      <StatusBadge type="ban" value={ban.trangThai} />
                    )}
                  </div>

                  {/* Danh sách đặt bàn trong ngày */}
                  {ban.datBanInDay && ban.datBanInDay.length > 0 && (
                    <div className="mt-3 pt-3 border-t border-gray-200">
                      <div className="text-xs font-medium text-gray-600 mb-2 flex items-center gap-1">
                        <Calendar className="w-3 h-3" />
                        <span>Đặt bàn ({ban.datBanInDay.length})</span>
                      </div>
                      <div className="space-y-1 max-h-32 overflow-y-auto">
                        {ban.datBanInDay.map((db, idx) => {
                          const ngaySuDung = parseRealtimeDateTime(db.ngayGioSuDung);
                          const trangThai = typeof db.trangThai === 'number' 
                            ? db.trangThai 
                            : parseInt(db.trangThai);
                          
                          return (
                            <div
                              key={db.maDatBan || db.id || idx}
                              className={`text-xs p-2 rounded flex items-center justify-between ${
                                trangThai === 1
                                  ? 'bg-orange-100 text-orange-800 border border-orange-300'
                                  : trangThai === 0
                                  ? 'bg-yellow-100 text-yellow-800 border border-yellow-300'
                                  : 'bg-gray-100 text-gray-600 border border-gray-300'
                              }`}
                            >
                              <span className="font-medium">
                                {ngaySuDung ? format(ngaySuDung, 'HH:mm', { locale: vi }) : '--:--'}
                              </span>
                              <span className={`text-xs px-1.5 py-0.5 rounded ${
                                trangThai === 1
                                  ? 'bg-orange-200 text-orange-900'
                                  : trangThai === 0
                                  ? 'bg-yellow-200 text-yellow-900'
                                  : 'bg-gray-200 text-gray-700'
                              }`}>
                                {trangThai === 1 ? '✓' : 
                                 trangThai === 0 ? '⏳' : 
                                 '✗'}
                              </span>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  )}
                  
                  {/* Hiển thị khi không có đặt bàn trong ngày (chỉ khi không phải hôm nay) */}
                  {(!ban.datBanInDay || ban.datBanInDay.length === 0) && !isTodaySelected && ban.trangThai === 0 && (
                    <div className="mt-3 pt-3 border-t border-gray-200">
                      <div className="text-xs text-gray-400 text-center italic">
                        Không có đặt bàn trong ngày này
                      </div>
                    </div>
                  )}
                  
                  <div className="mt-4">
                    <button
                      onClick={() => handleDeleteBan(ban)}
                      className="btn-danger text-sm flex items-center space-x-1 mx-auto hover:scale-105 transition-transform"
                    >
                      <Trash2 className="w-4 h-4" />
                      <span>Xóa</span>
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default QuanLyBan;
