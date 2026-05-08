import { CheckCircle, XCircle, Clock, AlertCircle, DollarSign, Package, Calendar } from 'lucide-react';

/**
 * Component hiển thị status badge đẹp
 * 
 * @param {Object} props
 * @param {String} props.type - Loại status: 'datban', 'hoadon', 'hanghoa', 'ban'
 * @param {Number} props.value - Giá trị status
 * @param {String} props.label - Label tùy chỉnh (optional)
 * @param {String} props.size - Kích thước: 'sm', 'md', 'lg'
 */
const StatusBadge = ({ type, value, label, size = 'md' }) => {
  const getConfig = () => {
    const configs = {
      // Đặt bàn
      datban: {
        0: {
          label: 'Chờ duyệt',
          bg: 'bg-yellow-100',
          text: 'text-yellow-800',
          border: 'border-yellow-300',
          icon: Clock,
        },
        1: {
          label: 'Đã duyệt',
          bg: 'bg-green-100',
          text: 'text-green-800',
          border: 'border-green-300',
          icon: CheckCircle,
        },
        '-1': {
          label: 'Từ chối',
          bg: 'bg-red-100',
          text: 'text-red-800',
          border: 'border-red-300',
          icon: XCircle,
        },
        2: {
          label: 'Đã sử dụng',
          bg: 'bg-blue-100',
          text: 'text-blue-800',
          border: 'border-blue-300',
          icon: CheckCircle,
        },
        3: {
          label: 'Đã hủy',
          bg: 'bg-gray-100',
          text: 'text-gray-800',
          border: 'border-gray-300',
          icon: XCircle,
        },
      },
      // Hóa đơn
      hoadon: {
        0: {
          label: 'Chưa thanh toán',
          bg: 'bg-orange-100',
          text: 'text-orange-800',
          border: 'border-orange-300',
          icon: Clock,
        },
        1: {
          label: 'Đã thanh toán',
          bg: 'bg-green-100',
          text: 'text-green-800',
          border: 'border-green-300',
          icon: DollarSign,
        },
        2: {
          label: 'Đã duyệt',
          bg: 'bg-blue-100',
          text: 'text-blue-800',
          border: 'border-blue-300',
          icon: CheckCircle,
        },
        3: {
          label: 'Đã hủy',
          bg: 'bg-gray-100',
          text: 'text-gray-800',
          border: 'border-gray-300',
          icon: XCircle,
        },
      },
      // Hàng hóa
      hanghoa: {
        0: {
          label: 'Hết hàng',
          bg: 'bg-red-100',
          text: 'text-red-800',
          border: 'border-red-300',
          icon: AlertCircle,
        },
        1: {
          label: 'Còn hàng',
          bg: 'bg-green-100',
          text: 'text-green-800',
          border: 'border-green-300',
          icon: Package,
        },
      },
      // Bàn
      ban: {
        0: {
          label: 'Còn trống',
          bg: 'bg-green-100',
          text: 'text-green-800',
          border: 'border-green-300',
          icon: CheckCircle,
        },
        1: {
          label: 'Đang sử dụng',
          bg: 'bg-blue-100',
          text: 'text-blue-800',
          border: 'border-blue-300',
          icon: Clock,
        },
        2: {
          label: 'Đã đặt',
          bg: 'bg-orange-100',
          text: 'text-orange-800',
          border: 'border-orange-300',
          icon: Calendar,
        },
      },
    };

    const config = configs[type]?.[value?.toString()] || {
      label: label || 'Không xác định',
      bg: 'bg-gray-100',
      text: 'text-gray-800',
      border: 'border-gray-300',
      icon: AlertCircle,
    };

    return config;
  };

  const config = getConfig();
  const Icon = config.icon;

  const sizeClasses = {
    sm: 'px-2 py-1 text-xs',
    md: 'px-3 py-1.5 text-sm',
    lg: 'px-4 py-2 text-base',
  };

  const iconSizes = {
    sm: 'w-3 h-3',
    md: 'w-4 h-4',
    lg: 'w-5 h-5',
  };

  return (
    <span
      className={`
        inline-flex items-center gap-1.5 font-medium rounded-full border
        ${config.bg} ${config.text} ${config.border}
        ${sizeClasses[size]}
      `}
    >
      <Icon className={iconSizes[size]} />
      {label || config.label}
    </span>
  );
};

export default StatusBadge;

