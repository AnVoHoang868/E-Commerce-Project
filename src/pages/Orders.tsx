import { useContext, useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { ShopContext } from '../context/ShopContext'
import Title from '../components/Title'
import { apiRequest } from '../lib/api'
import { assets } from '../assets/assets'
import type { ApiResponse, OrderDetail, OrderSummary, PageResponse, Product } from '../types/shop'
import { toast } from 'react-toastify'
import { formatCurrency } from '../lib/format'

const formatDate = (date?: string) =>
    date ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(date)) : 'Không có';

const Orders = () => {
    const context = useContext(ShopContext);
    const token = context?.token;
    const navigate = context?.navigate;
    const products = (context?.products || []) as Product[];
    const [orders, setOrders] = useState<Array<OrderSummary & { previewProductCode?: string }>>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const cancelOrder = async (orderCode: string) => {
        if (!token) return;

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
        }
    };

    useEffect(() => {
        if (!token) return;

        const getOrders = async () => {
            setLoading(true);
            setError('');

            try {
                const data = await apiRequest<ApiResponse<PageResponse<OrderSummary>>>('/v1/api/user/order/my-orders?page=0&size=20&sort=createdAt,desc', {
                    method: 'GET',
                    token,
                });

                const orderSummaries = data.data?.items || [];
                const ordersWithPreview = await Promise.all(
                    orderSummaries.map(async (order) => {
                        try {
                            const detail = await apiRequest<ApiResponse<OrderDetail>>(`/v1/api/user/order/${encodeURIComponent(order.orderCode)}`, {
                                method: 'GET',
                                token,
                            });

                            return {
                                ...order,
                                previewProductCode: detail.data?.items?.[0]?.productCode,
                            };
                        } catch (error) {
                            console.error(error);
                            return order;
                        }
                    })
                );

                setOrders(ordersWithPreview);
            } catch (error) {
                const message = error instanceof Error ? error.message : 'Không thể tải danh sách đơn hàng';
                setError(message);
            } finally {
                setLoading(false);
            }
        };

        getOrders();
    }, [token]);

    if (!token) {
        return <Navigate to='/login' replace />;
    }

    return (
        <div className='border-t pt-16'>
            <div className='text-2xl mb-6'>
                <Title text1={'ĐƠN'} text2={'HÀNG'} />
            </div>

            {loading ? (
                <p className='py-14 text-center text-gray-500'>Đang tải đơn hàng...</p>
            ) : error ? (
                <p className='py-14 text-center text-red-500'>{error}</p>
            ) : orders.length === 0 ? (
                <div className='py-16 text-center text-gray-500'>
                    <p>Bạn chưa có đơn hàng nào.</p>
                    <button onClick={() => navigate?.('/collection')} className='mt-6 bg-black text-white px-8 py-3 text-sm'>MUA SẮM NGAY</button>
                </div>
            ) : (
                <div className='space-y-4'>
                    {orders.map((order) => {
                        const previewProduct = products.find((product) => product._id === order.previewProductCode);

                        return (
                            <div key={order.orderCode} className='border bg-white p-5 text-gray-700 flex flex-col md:flex-row md:items-center md:justify-between gap-5'>
                                <div className='flex items-start gap-5'>
                                    <img
                                        className='w-20 h-20 object-cover border bg-gray-50'
                                        src={previewProduct?.image[0] || assets.p_img1}
                                        alt={previewProduct?.name || order.orderCode}
                                    />
                                    <div>
                                        <p className='text-xs text-gray-400 uppercase tracking-[0.18em]'>Mã đơn hàng</p>
                                        <p className='mt-1 font-medium text-gray-900'>{order.orderCode}</p>
                                        <p className='mt-1 text-sm text-gray-500'>{previewProduct?.name || 'Sản phẩm trong đơn'}</p>
                                        <div className='flex flex-wrap items-center gap-4 mt-3 text-sm'>
                                            <p>{formatCurrency(order.finalPrice)}</p>
                                            <p>{order.totalItems} sản phẩm</p>
                                            <p>{formatDate(order.createdAt)}</p>
                                        </div>
                                    </div>
                                </div>

                                <div className='flex items-center justify-between md:justify-end gap-3'>
                                    <button
                                        onClick={() => cancelOrder(order.orderCode)}
                                        disabled={order.status !== 'PENDING'}
                                        className='border border-red-500 px-5 py-2 text-sm text-red-600 hover:bg-red-500 hover:text-white transition-all disabled:border-gray-200 disabled:text-gray-400 disabled:hover:bg-white disabled:cursor-not-allowed'
                                    >
                                        Hủy đơn
                                    </button>
                                    <button onClick={() => navigate?.(`/orders/${order.orderCode}`)} className='border border-black px-5 py-2 text-sm hover:bg-black hover:text-white transition-all'>
                                        Theo dõi đơn
                                    </button>
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    )
}

export default Orders
