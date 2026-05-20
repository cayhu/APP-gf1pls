import { useEffect, useState } from 'react';
import { queryRealtimeDB, getAllFromRealtimeDB, REALTIME_NODES } from '../utils/realtimeDB';
import { parseRealtimeDateTime } from '../utils/dateUtils';
import { format, startOfDay, endOfDay, subDays, startOfMonth } from 'date-fns';
import { vi } from 'date-fns/locale';
import { Link } from 'react-router-dom';
import { 
  Users, DollarSign, Table, ShoppingCart, TrendingUp, Clock, 
  CheckCircle, AlertCircle, Calendar, ArrowRight, Package,
  Coffee, FileText
} from 'lucide-react';
import StatusBadge from '../components/StatusBadge';

const Dashboard = () => {
  const [stats, setStats] = useState({
    totalNhanVien: 0,
    totalDoanhThu: 0,
    doanhThuThang: 0,
    totalBan: 0,
    totalHangHoa: 0,
    hoaDonHomNay: 0,
    hoaDonChuaThanhToan: 0,
    hoaDonDaThanhToan: 0,
  });
  const [recentInvoices, setRecentInvoices] = useState([]);
  const [topProducts, setTopProducts] = useState([]);
  const [dailyRevenue, setDailyRevenue] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      const [nhanVienList, banList, hangHoaList, hoaDonList, hoaDonChiTietList] = await Promise.all([
        queryRealtimeDB(REALTIME_NODES.NGUOI_DUNG, (user) => user.chucVu === 'NhanVien', false),
        getAllFromRealtimeDB(REALTIME_NODES.BAN, true),
        getAllFromRealtimeDB(REALTIME_NODES.HANG_HOA, true),
        getAllFromRealtimeDB(REALTIME_NODES.HOA_DON, true),
        getAllFromRealtimeDB(REALTIME_NODES.HOA_DON_CHI_TIET, true)
      ]);

      const today = new Date();
      const startDay = startOfDay(today);
      const endDay = endOfDay(today);
      const startMonth = startOfMonth(today);

      let doanhThuHomNay = 0;
      let doanhThuThang = 0;
      let hoaDonHomNay = 0;
      const hoaDonChuaThanhToan = hoaDonList.filter(hd => hd.trangThai === 0).length;
      const hoaDonDaThanhToan = hoaDonList.filter(hd => hd.trangThai === 1).length;

      const invoicesWithDetails = hoaDonList.map(hoaDon => {
        const chiTiet = hoaDonChiTietList.filter(ct => 
          ct.maHoaDon === hoaDon.maHoaDon || ct.maHoaDon === parseInt(hoaDon.id)
        );
        
        const tongTien = chiTiet.reduce((sum, ct) => 
          sum + (ct.soLuong || 0) * (ct.giaTien || 0), 0
        );

        return { ...hoaDon, chiTiet, tongTien };
      });

      const last7Days = [];
      for (let i = 6; i >= 0; i--) {
        const date = subDays(today, i);
        const dayStart = startOfDay(date);
        const dayEnd = endOfDay(date);
        
        const dayRevenue = invoicesWithDetails
          .filter(hd => {
            const gioVao = parseRealtimeDateTime(hd.gioVao);
            return gioVao && gioVao >= dayStart && gioVao <= dayEnd && hd.trangThai === 1;
          })
          .reduce((sum, hd) => sum + (hd.tongTien || 0), 0);
        
        last7Days.push({
          date: format(date, 'dd/MM'),
          revenue: dayRevenue
        });
      }

      invoicesWithDetails.forEach(hoaDon => {
        const gioVao = parseRealtimeDateTime(hoaDon.gioVao);
        
        if (hoaDon.trangThai === 1) {
          if (gioVao && gioVao >= startDay && gioVao <= endDay) {
            doanhThuHomNay += hoaDon.tongTien || 0;
          }
          if (gioVao && gioVao >= startMonth) {
            doanhThuThang += hoaDon.tongTien || 0;
          }
        }
        
        if (gioVao && gioVao >= startDay && gioVao <= endDay) {
          hoaDonHomNay++;
        }
      });

      const recent = invoicesWithDetails
        .sort((a, b) => {
          const dateA = parseRealtimeDateTime(a.gioVao) || new Date(0);
          const dateB = parseRealtimeDateTime(b.gioVao) || new Date(0);
          return dateB - dateA;
        })
        .slice(0, 5);

      const productSales = {};
      hoaDonChiTietList.forEach(ct => {
        if (!productSales[ct.maHangHoa]) {
          const hangHoa = hangHoaList.find(hh => 
            (hh.maHangHoa || parseInt(hh.id)) === ct.maHangHoa
          );
          productSales[ct.maHangHoa] = {
            name: hangHoa?.tenHangHoa || `Hàng hóa #${ct.maHangHoa}`,
            quantity: 0,
            revenue: 0
          };
        }
        productSales[ct.maHangHoa].quantity += ct.soLuong || 0;
        productSales[ct.maHangHoa].revenue += (ct.soLuong || 0) * (ct.giaTien || 0);
      });

      const topProductsList = Object.entries(productSales)
        .map(([id, data]) => ({ id, ...data }))
        .sort((a, b) => b.revenue - a.revenue)
        .slice(0, 5);

      setStats({
        totalNhanVien: nhanVienList.length,
        totalDoanhThu: doanhThuHomNay,
        doanhThuThang,
        totalBan: banList.filter(b => b).length,
        totalHangHoa: hangHoaList.filter(h => h).length,
        hoaDonHomNay,
        hoaDonChuaThanhToan,
        hoaDonDaThanhToan,
      });
      setRecentInvoices(recent);
      setTopProducts(topProductsList);
      setDailyRevenue(last7Days);
    } catch (error) {
      console.error('Error loading dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN').format(amount) + 'đ';
  };

  const formatDateTime = (dateTimeStr) => {
    if (!dateTimeStr) return '-';
    const date = parseRealtimeDateTime(dateTimeStr);
    if (!date) return '-';
    return format(date, 'dd/MM/yyyy HH:mm', { locale: vi });
  };

  if (loading) {
    return (
      <div className="p-6 lg:p-8 max-w-7xl mx-auto">
        <div className="animate-pulse space-y-6">
          <div className="h-8 bg-gray-200 rounded w-1/4"></div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="h-28 bg-gray-200 rounded-2xl"></div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  const maxRevenue = Math.max(...dailyRevenue.map(d => d.revenue), 1);

  const statCards = [
    { 
      label: 'Doanh thu hôm nay', 
      value: formatCurrency(stats.totalDoanhThu), 
      icon: DollarSign, 
      color: 'from-blue-500 to-blue-600',
      bgColor: 'bg-blue-50 dark:bg-blue-900/20',
      textColor: 'text-blue-600 dark:text-blue-400'
    },
    { 
      label: 'Doanh thu tháng', 
      value: formatCurrency(stats.doanhThuThang), 
      icon: Calendar, 
      color: 'from-green-500 to-green-600',
      bgColor: 'bg-green-50 dark:bg-green-900/20',
      textColor: 'text-green-600 dark:text-green-400'
    },
    { 
      label: 'Hóa đơn hôm nay', 
      value: stats.hoaDonHomNay.toString(), 
      icon: FileText, 
      color: 'from-purple-500 to-purple-600',
      bgColor: 'bg-purple-50 dark:bg-purple-900/20',
      textColor: 'text-purple-600 dark:text-purple-400'
    },
    { 
      label: 'Chờ thanh toán', 
      value: stats.hoaDonChuaThanhToan.toString(), 
      icon: Clock, 
      color: 'from-orange-500 to-orange-600',
      bgColor: 'bg-orange-50 dark:bg-orange-900/20',
      textColor: 'text-orange-600 dark:text-orange-400'
    },
  ];

  const quickStats = [
    { label: 'Nhân viên', value: stats.totalNhanVien, icon: Users, color: 'text-blue-600' },
    { label: 'Bàn', value: stats.totalBan, icon: Table, color: 'text-green-600' },
    { label: 'Hàng hóa', value: stats.totalHangHoa, icon: Package, color: 'text-purple-600' },
    { label: 'Đã thanh toán', value: stats.hoaDonDaThanhToan, icon: CheckCircle, color: 'text-primary-600' },
  ];

  return (
    <div className="p-6 lg:p-8 max-w-7xl mx-auto">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Dashboard</h1>
        <p className="text-gray-500 dark:text-gray-400 mt-1 capitalize">
          {format(new Date(), 'EEEE, dd MMMM yyyy', { locale: vi })}
        </p>
      </div>

      {/* Main Stats */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
        {statCards.map((card, index) => {
          const Icon = card.icon;
          return (
            <div key={index} className={`bg-gradient-to-br ${card.color} rounded-2xl p-5 text-white shadow-lg hover:shadow-xl transition-shadow`}>
              <div className="flex items-center justify-between mb-3">
                <div className="w-12 h-12 bg-white/20 backdrop-blur-sm rounded-xl flex items-center justify-center">
                  <Icon className="w-6 h-6 text-white" />
                </div>
                <TrendingUp className="w-5 h-5 text-white/60" />
              </div>
              <p className="text-white/80 text-sm font-medium">{card.label}</p>
              <p className="text-2xl font-bold mt-1">{card.value}</p>
            </div>
          );
        })}
      </div>

      {/* Secondary Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        {quickStats.map((stat, index) => {
          const Icon = stat.icon;
          return (
            <div key={index} className="bg-white dark:bg-gray-800 rounded-xl p-4 border border-gray-200 dark:border-gray-700 flex items-center gap-4">
              <div className={`w-10 h-10 rounded-lg ${stat.color.replace('text-', 'bg-')}/10 flex items-center justify-center`}>
                <Icon className={`w-5 h-5 ${stat.color}`} />
              </div>
              <div>
                <p className="text-2xl font-bold text-gray-900 dark:text-white">{stat.value}</p>
                <p className="text-sm text-gray-500 dark:text-gray-400">{stat.label}</p>
              </div>
            </div>
          );
        })}
      </div>

      {/* Charts & Lists */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        {/* Revenue Chart */}
        <div className="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-lg font-bold text-gray-900 dark:text-white">Doanh thu 7 ngày</h2>
              <p className="text-sm text-gray-500 dark:text-gray-400">Biểu đồ theo ngày</p>
            </div>
            <div className="w-10 h-10 bg-blue-100 dark:bg-blue-900/30 rounded-lg flex items-center justify-center">
              <TrendingUp className="w-5 h-5 text-blue-600 dark:text-blue-400" />
            </div>
          </div>
          <div className="space-y-3">
            {dailyRevenue.map((day, index) => (
              <div key={index} className="flex items-center gap-3">
                <span className="text-sm text-gray-500 dark:text-gray-400 w-12 font-medium">{day.date}</span>
                <div className="flex-1 bg-gray-100 dark:bg-gray-700 rounded-full h-8 overflow-hidden">
                  <div 
                    className="bg-gradient-to-r from-blue-500 to-blue-600 h-full flex items-center justify-end pr-3 transition-all duration-500 rounded-full"
                    style={{ width: `${Math.max((day.revenue / maxRevenue) * 100, 2)}%` }}
                  >
                    {day.revenue > 0 && (
                      <span className="text-xs text-white font-semibold whitespace-nowrap">
                        {formatCurrency(day.revenue)}
                      </span>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Top Products */}
        <div className="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-lg font-bold text-gray-900 dark:text-white">Top sản phẩm</h2>
              <p className="text-sm text-gray-500 dark:text-gray-400">Bán chạy nhất</p>
            </div>
            <div className="w-10 h-10 bg-purple-100 dark:bg-purple-900/30 rounded-lg flex items-center justify-center">
              <Coffee className="w-5 h-5 text-purple-600 dark:text-purple-400" />
            </div>
          </div>
          <div className="space-y-3">
            {topProducts.length === 0 ? (
              <p className="text-center text-gray-500 dark:text-gray-400 py-8">Chưa có dữ liệu</p>
            ) : (
              topProducts.map((product, index) => (
                <div key={product.id} className="flex items-center gap-3 p-3 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
                  <div className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-sm ${
                    index === 0 ? 'bg-yellow-100 text-yellow-700' :
                    index === 1 ? 'bg-gray-200 text-gray-700' :
                    index === 2 ? 'bg-orange-100 text-orange-700' :
                    'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300'
                  }`}>
                    {index + 1}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="font-semibold text-gray-900 dark:text-white truncate">{product.name}</p>
                    <p className="text-sm text-gray-500 dark:text-gray-400">{product.quantity} phần đã bán</p>
                  </div>
                  <span className="font-bold text-gray-900 dark:text-white">
                    {formatCurrency(product.revenue)}
                  </span>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* Recent Invoices */}
      <div className="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 p-6 mb-8">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-lg font-bold text-gray-900 dark:text-white">Hóa đơn gần đây</h2>
            <p className="text-sm text-gray-500 dark:text-gray-400">5 hóa đơn mới nhất</p>
          </div>
          <Link 
            to="/hoa-don" 
            className="flex items-center gap-1 text-sm text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300 font-medium"
          >
            Xem tất cả
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
        
        {recentInvoices.length === 0 ? (
          <div className="text-center py-12">
            <FileText className="w-12 h-12 mx-auto text-gray-300 dark:text-gray-600 mb-3" />
            <p className="text-gray-500 dark:text-gray-400">Chưa có hóa đơn nào</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-200 dark:border-gray-700">
                  <th className="text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider pb-3">Mã HĐ</th>
                  <th className="text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider pb-3">Bàn</th>
                  <th className="text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider pb-3">Thời gian</th>
                  <th className="text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider pb-3">Trạng thái</th>
                  <th className="text-right text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider pb-3">Tổng tiền</th>
                </tr>
              </thead>
              <tbody>
                {recentInvoices.map((invoice) => (
                  <tr key={invoice.id} className="border-b border-gray-100 dark:border-gray-700/50 last:border-0 hover:bg-gray-50 dark:hover:bg-gray-700/30">
                    <td className="py-4 font-semibold text-gray-900 dark:text-white">#{invoice.maHoaDon || invoice.id}</td>
                    <td className="py-4 text-gray-600 dark:text-gray-300">Bàn {invoice.maBan}</td>
                    <td className="py-4 text-sm text-gray-500 dark:text-gray-400">{formatDateTime(invoice.gioVao)}</td>
                    <td className="py-4">
                      <StatusBadge type="hoadon" value={invoice.trangThai} />
                    </td>
                    <td className="py-4 text-right font-bold text-gray-900 dark:text-white">
                      {formatCurrency(invoice.tongTien || 0)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Quick Actions */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[
          { to: '/hoa-don', icon: FileText, label: 'Hóa đơn', color: 'text-blue-600 bg-blue-50 dark:bg-blue-900/20' },
          { to: '/hang-hoa', icon: Package, label: 'Hàng hóa', color: 'text-green-600 bg-green-50 dark:bg-green-900/20' },
          { to: '/ban', icon: Table, label: 'Bàn', color: 'text-purple-600 bg-purple-50 dark:bg-purple-900/20' },
          { to: '/nhan-vien', icon: Users, label: 'Nhân viên', color: 'text-orange-600 bg-orange-50 dark:bg-orange-900/20' },
        ].map((item, index) => {
          const Icon = item.icon;
          return (
            <Link 
              key={index}
              to={item.to} 
              className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5 hover:shadow-lg hover:scale-105 transition-all group"
            >
              <div className={`w-12 h-12 rounded-xl ${item.color} flex items-center justify-center mb-3 group-hover:scale-110 transition-transform`}>
                <Icon className="w-6 h-6" />
              </div>
              <p className="font-semibold text-gray-900 dark:text-white group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors">
                {item.label}
              </p>
            </Link>
          );
        })}
      </div>
    </div>
  );
};

export default Dashboard;
