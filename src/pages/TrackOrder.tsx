import { useContext, useEffect, useMemo, useState } from 'react';
import { Navigate, useParams } from 'react-router-dom';
import { ShopContext } from '../context/ShopContext';
import Title from '../components/Title';
import { apiRequest } from '../lib/api';
import type { ApiResponse, OrderDetail, OrderStatus, Product } from '../types/shop';
import { formatCurrency } from '../lib/format';

const trackingSteps: Array<{ status: OrderStatus; label: string }> = [
    { status: 'UNPAID', label: 'Chờ thanh toán' },
    { status: 'PAID', label: 'Đã thanh toán' },
    { status: 'PENDING', label: 'Đang xử lý' },
    { status: 'CONFIRMED', label: 'Đã xác nhận' },
    { status: 'SHIPPING', label: 'Đang giao' },
    { status: 'DELIVERED', label: 'Đã giao' },
    { status: 'COMPLETED', label: 'Hoàn tất' },
];

const statusLabel: Record<string, string> = {
    UNPAID: 'Chưa thanh toán',
    PAID: 'Đã thanh toán',
    PENDING: 'Đang xử lý',
    CANCELLED: 'Đã hủy',
    CONFIRMED: 'Đã xác nhận',
    SHIPPING: 'Đang giao',
    DELIVERED: 'Đã giao',
    COMPLETED: 'Hoàn tất',
    RETURNED: 'Đã trả hàng',
};

const getStepState = (stepIndex: number, activeStepIndex: number) => {
    if (stepIndex < activeStepIndex) return 'done';
    if (stepIndex === activeStepIndex) return 'current';
    return 'next';
};

const formatDate = (date?: string) =>
    date ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(date)) : 'Không có';

const TrackOrder = () => {
    const { orderCode } = useParams();
    const context = useContext(ShopContext);
    const token = context?.token;
    const navigate = context?.navigate;
    const products = (context?.products || []) as Product[];
    const [order, setOrder] = useState<OrderDetail | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        if (!token || !orderCode) return;

        const getOrder = async () => {
            setLoading(true);
            setError('');

            try {
                const data = await apiRequest<ApiResponse<OrderDetail>>(`/v1/api/user/order/${encodeURIComponent(orderCode)}`, {
                    method: 'GET',
                    token,
                });

                setOrder(data.data);
            } catch (error) {
                const message = error instanceof Error ? error.message : 'Không thể tải chi tiết đơn hàng';
                setError(message);
            } finally {
                setLoading(false);
            }
        };

        getOrder();
    }, [orderCode, token]);

    const receiver = order?.reciever || order?.receiver;
    const receiverName = [receiver?.fName, receiver?.lName].filter(Boolean).join(' ');
    const address = [receiver?.addr?.detail, receiver?.addr?.street, receiver?.addr?.district, receiver?.addr?.province, receiver?.addr?.country]
        .filter(Boolean)
        .join(', ');

    const activeStepIndex = useMemo(() => {
        if (!order) return -1;
        return trackingSteps.findIndex((step) => step.status === order.status);
    }, [order]);

    if (!token) {
        return <Navigate to='/login' replace />;
    }

    return (
        <div className='border-t pt-10 sm:pt-14 min-h-[70vh]'>
            <div className='flex items-center justify-between gap-4 mb-8'>
                <div className='text-2xl'>
                    <Title text1='THEO DÕI' text2='ĐƠN HÀNG' />
                </div>
                <button onClick={() => navigate?.('/orders')} className='border border-black px-5 py-2 text-sm hover:bg-black hover:text-white transition-all'>
                    Quay lại đơn hàng
                </button>
            </div>

            {loading ? (
                <p className='py-14 text-center text-gray-500'>Đang tải chi tiết đơn hàng...</p>
            ) : error ? (
                <p className='py-14 text-center text-red-500'>{error}</p>
            ) : order ? (
                <div className='grid lg:grid-cols-[1.3fr_0.8fr] gap-8'>
                    <div className='space-y-6'>
                        <div className='border bg-white p-6'>
                            <div className='flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4'>
                                <div>
                                    <p className='text-xs uppercase tracking-[0.18em] text-gray-400'>Mã đơn hàng</p>
                                    <p className='mt-1 text-xl font-medium text-gray-900'>{order.orderCode}</p>
                                    <p className='mt-2 text-sm text-gray-500'>Đặt lúc {formatDate(order.createdAt)}</p>
                                </div>
                                <span className={`w-fit px-3 py-1 text-sm ${order.status === 'CANCELLED' ? 'bg-red-50 text-red-600' : 'bg-green-50 text-green-700'}`}>
                                    {statusLabel[order.status] || order.status}
                                </span>
                            </div>

                            <div className='mt-8'>
                                {order.status === 'CANCELLED' || order.status === 'RETURNED' ? (
                                    <div className={`border p-4 text-sm ${order.status === 'CANCELLED' ? 'border-red-100 bg-red-50 text-red-600' : 'border-yellow-100 bg-yellow-50 text-yellow-700'}`}>
                                        Đơn hàng này {statusLabel[order.status].toLowerCase()}.
                                    </div>
                                ) : (
                                    <div className='grid grid-cols-1 sm:grid-cols-7 gap-4'>
                                        {trackingSteps.map((step, index) => {
                                            const stepState = getStepState(index, activeStepIndex);
                                            const isDone = stepState === 'done';
                                            const isCurrent = stepState === 'current';

                                            return (
                                                <div key={step.status} className='flex sm:flex-col items-center sm:items-start gap-3'>
                                                    <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs border transition-all ${isCurrent
                                                        ? 'bg-green-500 text-white border-green-500 ring-4 ring-green-100'
                                                        : isDone
                                                            ? 'bg-black text-white border-black'
                                                            : 'bg-white text-gray-400 border-gray-300'
                                                        }`}>
                                                        {index + 1}
                                                    </div>
                                                    <div>
                                                        <p className={`text-sm font-medium ${isCurrent ? 'text-green-600' : isDone ? 'text-gray-900' : 'text-gray-400'}`}>{step.label}</p>
                                                        <p className={`text-xs mt-1 ${isCurrent ? 'text-green-600' : 'text-gray-400'}`}>
                                                            {isCurrent ? 'Giai đoạn hiện tại' : isDone ? 'Đã hoàn thành' : 'Đang chờ'}
                                                        </p>
                                                    </div>
                                                </div>
                                            );
                                        })}
                                    </div>
                                )}
                            </div>
                        </div>

                        <div className='border bg-white p-6'>
                            <p className='text-lg font-medium text-gray-900 mb-4'>Sản phẩm</p>
                            <div className='divide-y'>
                                {(order.items || []).map((item) => {
                                    const product = products.find((product) => product._id === item.productCode);

                                    return (
                                        <div key={`${item.productCode}-${item.size}`} className='py-4 flex items-start justify-between gap-4'>
                                            <div className='flex gap-4'>
                                                {product?.image[0] && <img className='w-16 h-16 object-cover border' src={product.image[0]} alt={product.name} />}
                                                <div>
                                                    <p className='font-medium text-gray-900'>{product?.name || item.productCode}</p>
                                                    <p className='text-sm text-gray-500 mt-1'>Kích cỡ {item.size} | Số lượng {item.quantity}</p>
                                                </div>
                                            </div>
                                            <p className='text-sm font-medium text-gray-900'>{formatCurrency(item.finalPrice ?? item.originalPrice)}</p>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                    </div>

                    <div className='space-y-6'>
                        <div className='border bg-white p-6'>
                            <p className='text-lg font-medium text-gray-900'>Giao hàng</p>
                            <div className='mt-4 space-y-3 text-sm text-gray-600'>
                                <p><span className='text-gray-400'>Người nhận:</span> {receiverName || 'Chưa có'}</p>
                                <p><span className='text-gray-400'>Số điện thoại:</span> {receiver?.phone || 'Chưa có'}</p>
                                <p><span className='text-gray-400'>Địa chỉ:</span> {address || 'Chưa có'}</p>
                            </div>
                        </div>

                        <div className='border bg-white p-6'>
                            <p className='text-lg font-medium text-gray-900'>Tổng kết thanh toán</p>
                            <div className='mt-4 space-y-3 text-sm'>
                                <div className='flex justify-between'><span className='text-gray-500'>Thanh toán</span><span>{order.paymentType || 'Không có'}</span></div>
                                <div className='flex justify-between'><span className='text-gray-500'>Tạm tính</span><span>{formatCurrency(order.totalAmount)}</span></div>
                                <div className='flex justify-between'><span className='text-gray-500'>Giảm giá</span><span>{formatCurrency(order.voucherDiscount)}</span></div>
                                <div className='border-t pt-3 flex justify-between font-medium text-base'><span>Tổng cộng</span><span>{formatCurrency(order.finalPrice)}</span></div>
                            </div>
                        </div>
                    </div>
                </div>
            ) : null}
        </div>
    );
};

export default TrackOrder;
