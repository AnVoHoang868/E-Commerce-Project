import { useContext, useEffect, useMemo, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { ShopContext } from '../context/ShopContext';
import Title from '../components/Title';
import { apiRequest } from '../lib/api';
import { assets } from '../assets/assets';
import type { ApiResponse, OrderDetail, OrderStatus, OrderSummary, PageResponse, PaymentQr, Product } from '../types/shop';
import { formatCurrency } from '../lib/format';

type OrderGroup = 'ALL' | 'UNPAID' | 'PROCESSING' | 'SHIPPING' | 'DELIVERED' | 'CANCELLED' | 'RETURNED';

type OrderListItem = OrderSummary & {
    paymentType?: 'PAYMENT_UPON_DELIVER' | 'ONLINE';
    paymentUrl?: string;
    bankTransferQr?: PaymentQr;
    previewProductCode?: string;
};

const orderGroups: Array<{ key: OrderGroup; label: string; statuses: OrderStatus[] | null }> = [
    { key: 'ALL', label: 'Tất cả', statuses: null },
    { key: 'UNPAID', label: 'Chờ thanh toán', statuses: ['UNPAID'] },
    { key: 'PROCESSING', label: 'Đang xử lý', statuses: ['PAID', 'PENDING', 'CONFIRMED'] },
    { key: 'SHIPPING', label: 'Đang giao', statuses: ['SHIPPING'] },
    { key: 'DELIVERED', label: 'Đã giao', statuses: ['DELIVERED', 'COMPLETED'] },
    { key: 'CANCELLED', label: 'Đã hủy', statuses: ['CANCELLED'] },
    { key: 'RETURNED', label: 'Hoàn trả', statuses: ['RETURNED'] },
];

const statusLabel: Record<OrderStatus, string> = {
    UNPAID: 'Chờ thanh toán',
    PAID: 'Đã thanh toán',
    PENDING: 'Chờ xác nhận',
    CANCELLED: 'Đã hủy',
    CONFIRMED: 'Đã xác nhận',
    SHIPPING: 'Đang giao hàng',
    DELIVERED: 'Đã giao hàng',
    COMPLETED: 'Hoàn tất',
    RETURNED: 'Đã hoàn trả',
};

const paymentLabel: Record<string, string> = {
    ONLINE: 'Thanh toán online',
    PAYMENT_UPON_DELIVER: 'Thanh toán khi nhận hàng',
};

const statusTone: Record<OrderStatus, string> = {
    UNPAID: 'bg-amber-50 text-amber-700 border-amber-100',
    PAID: 'bg-blue-50 text-blue-700 border-blue-100',
    PENDING: 'bg-sky-50 text-sky-700 border-sky-100',
    CONFIRMED: 'bg-indigo-50 text-indigo-700 border-indigo-100',
    SHIPPING: 'bg-cyan-50 text-cyan-700 border-cyan-100',
    DELIVERED: 'bg-green-50 text-green-700 border-green-100',
    COMPLETED: 'bg-emerald-50 text-emerald-700 border-emerald-100',
    CANCELLED: 'bg-red-50 text-red-600 border-red-100',
    RETURNED: 'bg-yellow-50 text-yellow-700 border-yellow-100',
};

const formatDate = (date?: string) =>
    date ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(date)) : 'Không có';

const Orders = () => {
    const context = useContext(ShopContext);
    const token = context?.token;
    const navigate = context?.navigate;
    const products = (context?.products || []) as Product[];
    const [orders, setOrders] = useState<OrderListItem[]>([]);
    const [activeGroup, setActiveGroup] = useState<OrderGroup>('ALL');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [actionOrderCode, setActionOrderCode] = useState('');

    const refreshOrders = async () => {
        if (!token) return;

        setLoading(true);
        setError('');

        try {
            const data = await apiRequest<ApiResponse<PageResponse<OrderSummary>>>('/v1/api/user/order/my-orders?page=0&size=50&sort=createdAt,desc', {
                method: 'GET',
                token,
            });

            const orderSummaries = data.data?.items || [];
            const ordersWithDetails = await Promise.all(
                orderSummaries.map(async (order) => {
                    try {
                        const detail = await apiRequest<ApiResponse<OrderDetail>>(`/v1/api/user/order/${encodeURIComponent(order.orderCode)}`, {
                            method: 'GET',
                            token,
                        });

                        return {
                            ...order,
                            paymentType: detail.data?.paymentType,
                            paymentUrl: detail.data?.paymentUrl,
                            bankTransferQr: detail.data?.bankTransferQr,
                            previewProductCode: detail.data?.items?.[0]?.productCode,
                        };
                    } catch (error) {
                        console.error(error);
                        return order;
                    }
                })
            );

            setOrders(ordersWithDetails);
        } catch (error) {
            const message = error instanceof Error ? error.message : 'Không thể tải danh sách đơn hàng';
            setError(message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        refreshOrders();
    }, [token]);

    const filteredOrders = useMemo(() => {
        const group = orderGroups.find((item) => item.key === activeGroup);
        if (!group?.statuses) return orders;
        return orders.filter((order) => group.statuses?.includes(order.status));
    }, [activeGroup, orders]);

    const groupCount = (group: OrderGroup) => {
        const groupConfig = orderGroups.find((item) => item.key === group);
        if (!groupConfig?.statuses) return orders.length;
        return orders.filter((order) => groupConfig.statuses?.includes(order.status)).length;
    };

    const cancelOrder = async (orderCode: string) => {
        if (!token) return;

        setActionOrderCode(orderCode);
        try {
            await apiRequest(`/v1/api/user/order/cancel/${encodeURIComponent(orderCode)}`, {
                method: 'PUT',
                token,
            });

            setOrders((currentOrders) =>
                currentOrders.map((order) =>
                    order.orderCode === orderCode ? { ...order, status: 'CANCELLED' } : order
                )
            );
            toast.success('Đã hủy đơn hàng');
        } catch (error) {
            const message = error instanceof Error ? error.message : 'Không thể hủy đơn hàng';
            toast.error(message);
        } finally {
            setActionOrderCode('');
        }
    };

    const repayOrder = async (order: OrderListItem) => {
        if (!token) return;

        setActionOrderCode(order.orderCode);
        try {
            if (order.paymentUrl) {
                window.location.href = order.paymentUrl;
                return;
            }

            const response = await apiRequest<ApiResponse<{ paymentUrl?: string; bankTransferQr?: PaymentQr }>>(`/v1/api/user/payment/vnpay/${encodeURIComponent(order.orderCode)}`, {
                method: 'POST',
                token,
            });

            if (response.data?.paymentUrl) {
                window.location.href = response.data.paymentUrl;
                return;
            }

            toast.info('Backend chưa trả link thanh toán cho đơn hàng này');
        } catch (error) {
            const message = error instanceof Error ? error.message : 'Không thể tạo lại link thanh toán';
            toast.error(message);
        } finally {
            setActionOrderCode('');
        }
    };

    const completeOrder = async (orderCode: string) => {
        if (!token) return;

        setActionOrderCode(orderCode);
        try {
            await apiRequest(`/v1/api/user/order/complete/${encodeURIComponent(orderCode)}`, {
                method: 'PUT',
                token,
            });

            setOrders((currentOrders) =>
                currentOrders.map((order) =>
                    order.orderCode === orderCode ? { ...order, status: 'COMPLETED' } : order
                )
            );
            toast.success('Đã xác nhận nhận hàng');
        } catch (error) {
            const message = error instanceof Error ? error.message : 'Không thể xác nhận nhận hàng';
            toast.error(message);
        } finally {
            setActionOrderCode('');
        }
    };

    if (!token) {
        return <Navigate to='/login' replace />;
    }

    return (
        <div className='border-t pt-10 sm:pt-14 min-h-[70vh]'>
            <div className='flex flex-col md:flex-row md:items-end md:justify-between gap-5 mb-6'>
                <div>
                    <div className='text-2xl'>
                        <Title text1='ĐƠN HÀNG' text2='CỦA TÔI' />
                    </div>
                    <p className='mt-2 text-sm text-gray-500'>Lọc, theo dõi trạng thái và xử lý nhanh các đơn hàng của bạn.</p>
                </div>
                <button onClick={refreshOrders} disabled={loading} className='w-fit border border-black px-5 py-2.5 text-sm hover:bg-black hover:text-white transition-all disabled:border-gray-200 disabled:text-gray-400'>
                    {loading ? 'Đang tải...' : 'Tải lại'}
                </button>
            </div>

            <div className='overflow-x-auto border-b border-gray-200'>
                <div className='flex min-w-max gap-2 pb-3'>
                    {orderGroups.map((group) => (
                        <button
                            key={group.key}
                            onClick={() => setActiveGroup(group.key)}
                            className={`px-4 py-2.5 text-sm border transition-all ${
                                activeGroup === group.key ? 'bg-black text-white border-black' : 'bg-white text-gray-600 border-gray-200 hover:border-black'
                            }`}
                        >
                            {group.label}
                            <span className={`ml-2 text-xs ${activeGroup === group.key ? 'text-white/80' : 'text-gray-400'}`}>{groupCount(group.key)}</span>
                        </button>
                    ))}
                </div>
            </div>

            {loading ? (
                <p className='py-14 text-center text-gray-500'>Đang tải đơn hàng...</p>
            ) : error ? (
                <p className='py-14 text-center text-red-500'>{error}</p>
            ) : orders.length === 0 ? (
                <div className='my-10 border bg-white px-6 py-16 text-center text-gray-500'>
                    <p className='text-lg font-medium text-gray-900'>Bạn chưa có đơn hàng nào</p>
                    <p className='mt-2 text-sm'>Khi đặt hàng thành công, đơn hàng sẽ xuất hiện tại đây.</p>
                    <button onClick={() => navigate?.('/collection')} className='mt-6 bg-black text-white px-8 py-3 text-sm'>TIẾP TỤC MUA SẮM</button>
                </div>
            ) : filteredOrders.length === 0 ? (
                <div className='my-10 border bg-white px-6 py-14 text-center text-gray-500'>
                    <p>Không có đơn hàng trong nhóm này.</p>
                </div>
            ) : (
                <div className='mt-6 space-y-4'>
                    {filteredOrders.map((order) => {
                        const previewProduct = products.find((product) => product._id === order.previewProductCode);
                        const busy = actionOrderCode === order.orderCode;

                        return (
                            <article key={order.orderCode} className='border bg-white p-5 shadow-sm'>
                                <div className='flex flex-col lg:flex-row lg:items-start lg:justify-between gap-5'>
                                    <div className='flex items-start gap-4 min-w-0'>
                                        <img
                                            className='w-20 h-20 object-cover border bg-gray-50'
                                            src={previewProduct?.image[0] || assets.p_img1}
                                            alt={previewProduct?.name || order.orderCode}
                                        />
                                        <div className='min-w-0'>
                                            <div className='flex flex-wrap items-center gap-2'>
                                                <p className='font-medium text-gray-900'>{order.orderCode}</p>
                                                <span className={`border px-2.5 py-1 text-xs ${statusTone[order.status]}`}>
                                                    {statusLabel[order.status]}
                                                </span>
                                            </div>
                                            <p className='mt-1 text-sm text-gray-500'>{previewProduct?.name || 'Sản phẩm trong đơn'}</p>
                                            <div className='mt-3 grid sm:grid-cols-2 lg:grid-cols-4 gap-x-6 gap-y-2 text-sm'>
                                                <p><span className='text-gray-400'>Ngày đặt:</span> {formatDate(order.createdAt)}</p>
                                                <p><span className='text-gray-400'>Số lượng:</span> {order.totalItems} sản phẩm</p>
                                                <p><span className='text-gray-400'>Thanh toán:</span> {paymentLabel[order.paymentType || ''] || 'Chưa có'}</p>
                                                <p><span className='text-gray-400'>Tổng:</span> <span className='font-medium text-gray-900'>{formatCurrency(order.finalPrice)}</span></p>
                                            </div>
                                        </div>
                                    </div>

                                    <div className='flex flex-wrap lg:justify-end gap-2'>
                                        {order.status === 'UNPAID' && (
                                            <>
                                                <button onClick={() => repayOrder(order)} disabled={busy} className='bg-black px-5 py-2 text-sm text-white disabled:bg-gray-300'>
                                                    Thanh toán lại
                                                </button>
                                                <button onClick={() => cancelOrder(order.orderCode)} disabled={busy} className='border border-red-500 px-5 py-2 text-sm text-red-600 hover:bg-red-500 hover:text-white disabled:border-gray-200 disabled:text-gray-400 disabled:hover:bg-white'>
                                                    Hủy đơn
                                                </button>
                                            </>
                                        )}
                                        {order.status === 'PENDING' && (
                                            <button onClick={() => cancelOrder(order.orderCode)} disabled={busy} className='border border-red-500 px-5 py-2 text-sm text-red-600 hover:bg-red-500 hover:text-white disabled:border-gray-200 disabled:text-gray-400 disabled:hover:bg-white'>
                                                Hủy đơn
                                            </button>
                                        )}
                                        {order.status === 'DELIVERED' && (
                                            <button onClick={() => completeOrder(order.orderCode)} disabled={busy} className='bg-green-600 px-5 py-2 text-sm text-white hover:bg-green-700 disabled:bg-gray-300'>
                                                Xác nhận đã nhận hàng
                                            </button>
                                        )}
                                        {order.status === 'COMPLETED' && previewProduct && (
                                            <button onClick={() => navigate?.(`/product/${previewProduct._id}`)} className='border border-black px-5 py-2 text-sm hover:bg-black hover:text-white'>
                                                Đánh giá sản phẩm
                                            </button>
                                        )}
                                        <button onClick={() => navigate?.(`/orders/${order.orderCode}`)} className='border border-black px-5 py-2 text-sm hover:bg-black hover:text-white transition-all'>
                                            Xem chi tiết
                                        </button>
                                    </div>
                                </div>
                            </article>
                        );
                    })}
                </div>
            )}
        </div>
    );
};

export default Orders;
