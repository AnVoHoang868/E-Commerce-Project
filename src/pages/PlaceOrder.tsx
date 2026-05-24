import { useContext, useEffect, useMemo, useState, type FormEvent } from 'react';
import { Navigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { ShopContext } from '../context/ShopContext';
import Title from '../components/Title';
import { assets } from '../assets/assets';
import { apiRequest } from '../lib/api';
import { formatCurrency } from '../lib/format';
import type {
    ApiResponse,
    CartItems,
    CreateOrderPayload,
    CreateOrderResponse,
    Product,
    Receiver,
    ReceiverCreatePayload,
} from '../types/shop';

type PaymentMethod = 'PAYMENT_UPON_DELIVER' | 'ONLINE';
type ReceiverMode = 'select' | 'create';

const emptyReceiverForm: ReceiverCreatePayload = {
    fName: '',
    lName: '',
    phone: '',
    addr: {
        country: 'Việt Nam',
        province: '',
        district: '',
        street: '',
        detail: '',
    },
};

const compactAddress = (receiver?: Receiver) => [
    receiver?.addr?.detail,
    receiver?.addr?.street,
    receiver?.addr?.district,
    receiver?.addr?.province,
    receiver?.addr?.country,
].filter(Boolean).join(', ');

const normalizeText = (value?: string) => (value || '').trim().toLowerCase();

const matchesReceiverPayload = (receiver: Receiver, payload: ReceiverCreatePayload) =>
    normalizeText(receiver.fName) === normalizeText(payload.fName) &&
    normalizeText(receiver.lName) === normalizeText(payload.lName) &&
    normalizeText(receiver.phone) === normalizeText(payload.phone) &&
    normalizeText(receiver.addr?.country) === normalizeText(payload.addr.country) &&
    normalizeText(receiver.addr?.province) === normalizeText(payload.addr.province) &&
    normalizeText(receiver.addr?.district) === normalizeText(payload.addr.district) &&
    normalizeText(receiver.addr?.street) === normalizeText(payload.addr.street) &&
    normalizeText(receiver.addr?.detail) === normalizeText(payload.addr.detail);

const PlaceOrder = () => {
    const context = useContext(ShopContext);
    const token = context?.token as string;
    const navigate = context?.navigate;
    const products = (context?.products || []) as Product[];
    const cartItems = (context?.cartItems || {}) as CartItems;
    const refreshCart = context?.refreshCart as (() => Promise<void>) | undefined;

    const [receivers, setReceivers] = useState<Receiver[]>([]);
    const [receiverMode, setReceiverMode] = useState<ReceiverMode>('select');
    const [selectedReceiverId, setSelectedReceiverId] = useState<number | null>(null);
    const [receiverForm, setReceiverForm] = useState<ReceiverCreatePayload>(emptyReceiverForm);
    const [paymentType, setPaymentType] = useState<PaymentMethod>('PAYMENT_UPON_DELIVER');
    const [note, setNote] = useState('');
    const [loadingReceivers, setLoadingReceivers] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [createdOrder, setCreatedOrder] = useState<CreateOrderResponse | null>(null);

    const orderItems = useMemo(() => {
        return Object.entries(cartItems).flatMap(([productCode, sizes]) =>
            Object.entries(sizes)
                .filter(([, quantity]) => quantity > 0)
                .map(([size, quantity]) => {
                    const product = products.find((item) => item._id === productCode);
                    if (!product) return null;

                    const originalPrice = product.originalPrice ?? product.price;
                    const finalPrice = product.finalPrice ?? product.price;

                    return {
                        productCode,
                        size,
                        quantity,
                        originalPrice,
                        finalPrice,
                        product,
                    };
                })
                .filter(Boolean)
        ) as Array<{
            productCode: string;
            size: string;
            quantity: number;
            originalPrice: number;
            finalPrice: number;
            product: Product;
        }>;
    }, [cartItems, products]);

    const subtotal = orderItems.reduce((total, item) => total + item.finalPrice * item.quantity, 0);
    const selectedReceiver = receivers.find((receiver) => receiver.id === selectedReceiverId);
    const receiverApiMissingId = receivers.length > 0 && receivers.some((receiver) => receiver.id === undefined || receiver.id === null);

    const fetchReceivers = async () => {
        if (!token) return;

        setLoadingReceivers(true);
        try {
            const response = await apiRequest<ApiResponse<Receiver[]>>('/v1/api/user/receiver/get/all', {
                method: 'GET',
                token,
            });

            const nextReceivers = Array.isArray(response.data) ? response.data : [];
            setReceivers(nextReceivers);

            const firstReceiverWithId = nextReceivers.find((receiver) => receiver.id !== undefined && receiver.id !== null);
            if (firstReceiverWithId?.id) {
                setSelectedReceiverId(firstReceiverWithId.id);
                setReceiverMode('select');
            } else {
                setSelectedReceiverId(null);
                setReceiverMode('create');
            }
        } catch (error) {
            console.error(error);
            const message = error instanceof Error ? error.message : 'Không thể tải thông tin giao hàng';
            toast.error(message);
        } finally {
            setLoadingReceivers(false);
        }
    };

    useEffect(() => {
        if (token) {
            fetchReceivers();
        }
    }, [token]);

    const updateReceiverField = (field: keyof ReceiverCreatePayload, value: string) => {
        setReceiverForm((prev) => ({ ...prev, [field]: value }));
    };

    const updateAddressField = (field: keyof ReceiverCreatePayload['addr'], value: string) => {
        setReceiverForm((prev) => ({
            ...prev,
            addr: {
                ...prev.addr,
                [field]: value,
            },
        }));
    };

    const createReceiver = async () => {
        const payload: ReceiverCreatePayload = {
            fName: receiverForm.fName.trim(),
            lName: receiverForm.lName.trim(),
            phone: receiverForm.phone.trim(),
            addr: {
                country: receiverForm.addr.country.trim() || 'Việt Nam',
                province: receiverForm.addr.province.trim(),
                district: receiverForm.addr.district.trim(),
                street: receiverForm.addr.street.trim(),
                detail: receiverForm.addr.detail.trim(),
            },
        };

        if (!payload.fName || !payload.lName || !payload.phone) {
            throw new Error('Vui lòng nhập đầy đủ họ tên và số điện thoại người nhận');
        }

        if (!/^[0-9]{9,10}$/.test(payload.phone)) {
            throw new Error('Số điện thoại chỉ nên gồm 9-10 chữ số');
        }

        if (!payload.addr.province || !payload.addr.district || !payload.addr.street || !payload.addr.detail) {
            throw new Error('Vui lòng nhập đầy đủ địa chỉ giao hàng');
        }

        const response = await apiRequest<ApiResponse<Receiver>>('/v1/api/user/receiver/create', {
            method: 'POST',
            token,
            body: payload,
        });

        const createdReceiver = response.data;
        if (!createdReceiver?.id) {
            const listResponse = await apiRequest<ApiResponse<Receiver[]>>('/v1/api/user/receiver/get/all', {
                method: 'GET',
                token,
            });
            const nextReceivers = Array.isArray(listResponse.data) ? listResponse.data : [];
            const matchedReceiver = nextReceivers.find((receiver) => receiver.id && matchesReceiverPayload(receiver, payload));

            setReceivers(nextReceivers);

            if (matchedReceiver?.id) {
                setSelectedReceiverId(matchedReceiver.id);
                setReceiverMode('select');
                setReceiverForm(emptyReceiverForm);
                return matchedReceiver.id;
            }

            throw new Error('Backend đang chạy chưa trả id của receiver. Hãy rebuild/restart backend để API receiver trả id trước khi tạo order.');
        }

        setReceivers((current) => [createdReceiver, ...current]);
        setSelectedReceiverId(createdReceiver.id);
        setReceiverMode('select');
        setReceiverForm(emptyReceiverForm);

        return createdReceiver.id;
    };

    const resolveReceiverId = async () => {
        if (receiverMode === 'create') {
            return createReceiver();
        }

        if (!selectedReceiverId) {
            throw new Error('Vui lòng chọn thông tin giao hàng');
        }

        return selectedReceiverId;
    };

    const handlePlaceOrder = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        if (orderItems.length === 0) {
            toast.error('Giỏ hàng đang trống');
            return;
        }

        setSubmitting(true);
        setCreatedOrder(null);

        try {
            const recieverid = await resolveReceiverId();
            const payload: CreateOrderPayload = {
                recieverid,
                items: orderItems.map((item) => ({
                    productCode: item.productCode,
                    size: item.size,
                    quantity: item.quantity,
                    originalPrice: item.originalPrice,
                    finalPrice: item.finalPrice,
                })),
                totalPrice: subtotal,
                voucherDiscount: 0,
                finalPrice: subtotal,
                paymentType,
                note: note.trim() || undefined,
            };

            const response = await apiRequest<ApiResponse<CreateOrderResponse>>('/v1/api/user/order/create', {
                method: 'POST',
                token,
                body: payload,
            });

            await refreshCart?.();
            toast.success(paymentType === 'ONLINE' ? 'Đã tạo đơn hàng, vui lòng thanh toán' : 'Đặt hàng thành công');

            if (response.data?.paymentUrl) {
                window.location.href = response.data.paymentUrl;
                return;
            }

            if (response.data?.bankTransferQr?.qrImageUrl) {
                setCreatedOrder(response.data);
                return;
            }

            navigate?.(response.data?.orderCode ? `/orders/${response.data.orderCode}` : '/orders');
        } catch (error) {
            console.error(error);
            const message = error instanceof Error ? error.message : 'Không thể tạo đơn hàng';
            toast.error(message);
        } finally {
            setSubmitting(false);
        }
    };

    if (!token) {
        return <Navigate to='/login' replace />;
    }

    return (
        <form onSubmit={handlePlaceOrder} className='border-t pt-10 sm:pt-14 min-h-[80vh]'>
            <div className='grid gap-10 lg:grid-cols-[1.3fr_0.9fr] items-start'>
                <div className='space-y-6'>
                    <div>
                        <div className='text-2xl'>
                            <Title text1='ĐẶT' text2='HÀNG' />
                        </div>
                        <p className='mt-2 text-sm text-gray-500'>Xác nhận thông tin giao hàng, phương thức thanh toán và sản phẩm trong giỏ.</p>
                    </div>

                    <section className='border bg-white p-5 sm:p-6 shadow-sm'>
                        <div className='flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-gray-100 pb-5'>
                            <div>
                                <p className='text-lg font-medium text-gray-900'>Thông tin giao hàng</p>
                                <p className='mt-1 text-sm text-gray-500'>
                                    {receiverMode === 'create' ? 'Tạo receiver mới để tiếp tục đặt hàng.' : 'Chọn receiver đã lưu cho đơn hàng này.'}
                                </p>
                            </div>
                            <div className='flex gap-2'>
                                {receivers.length > 0 && (
                                    <button
                                        type='button'
                                        onClick={() => setReceiverMode(receiverMode === 'select' ? 'create' : 'select')}
                                        className='border border-gray-300 px-4 py-2 text-xs font-medium tracking-[0.16em] hover:border-black'
                                    >
                                        {receiverMode === 'select' ? 'THÊM MỚI' : 'CHỌN ĐÃ LƯU'}
                                    </button>
                                )}
                                <button
                                    type='button'
                                    onClick={fetchReceivers}
                                    disabled={loadingReceivers}
                                    className='border border-black px-4 py-2 text-xs font-medium tracking-[0.16em] hover:bg-black hover:text-white disabled:border-gray-200 disabled:text-gray-400'
                                >
                                    {loadingReceivers ? 'ĐANG TẢI' : 'TẢI LẠI'}
                                </button>
                            </div>
                        </div>

                        {receiverApiMissingId && (
                            <div className='mt-5 border border-yellow-200 bg-yellow-50 px-4 py-3 text-sm text-yellow-700'>
                                API receiver đang thiếu `id` ở một số bản ghi. Frontend cần `id` để gửi `recieverid` khi tạo order.
                            </div>
                        )}

                        {receiverMode === 'select' && receivers.length > 0 ? (
                            <div className='mt-5 grid gap-4'>
                                {receivers.map((receiver, index) => {
                                    const isSelected = receiver.id === selectedReceiverId;

                                    return (
                                        <button
                                            key={receiver.id ?? `${receiver.phone}-${index}`}
                                            type='button'
                                            disabled={!receiver.id}
                                            onClick={() => receiver.id && setSelectedReceiverId(receiver.id)}
                                            className={`w-full border p-4 text-left transition-all disabled:cursor-not-allowed disabled:opacity-60 ${
                                                isSelected ? 'border-black bg-black text-white shadow-md' : 'border-gray-200 hover:border-black'
                                            }`}
                                        >
                                            <div className='flex items-start justify-between gap-4'>
                                                <div>
                                                    <p className='font-medium'>{[receiver.fName, receiver.lName].filter(Boolean).join(' ') || 'Người nhận'}</p>
                                                    <p className={`mt-1 text-sm ${isSelected ? 'text-white/80' : 'text-gray-500'}`}>{receiver.phone || 'Chưa có số điện thoại'}</p>
                                                    <p className={`mt-2 text-sm leading-6 ${isSelected ? 'text-white/85' : 'text-gray-600'}`}>{compactAddress(receiver) || 'Chưa có địa chỉ'}</p>
                                                    {!receiver.id && <p className='mt-2 text-xs text-red-500'>Thiếu receiver id</p>}
                                                </div>
                                                <span className={`mt-1 h-4 w-4 rounded-full border ${isSelected ? 'border-white bg-white' : 'border-gray-400'}`}></span>
                                            </div>
                                        </button>
                                    );
                                })}
                            </div>
                        ) : (
                            <div className='mt-5 grid gap-4 sm:grid-cols-2'>
                                <input
                                    value={receiverForm.fName}
                                    onChange={(event) => updateReceiverField('fName', event.target.value)}
                                    className='border border-gray-300 px-4 py-3 outline-none focus:border-black'
                                    placeholder='Tên người nhận'
                                />
                                <input
                                    value={receiverForm.lName}
                                    onChange={(event) => updateReceiverField('lName', event.target.value)}
                                    className='border border-gray-300 px-4 py-3 outline-none focus:border-black'
                                    placeholder='Họ người nhận'
                                />
                                <input
                                    value={receiverForm.phone}
                                    onChange={(event) => updateReceiverField('phone', event.target.value)}
                                    className='border border-gray-300 px-4 py-3 outline-none focus:border-black sm:col-span-2'
                                    inputMode='tel'
                                    placeholder='Số điện thoại'
                                />
                                <input
                                    value={receiverForm.addr.province}
                                    onChange={(event) => updateAddressField('province', event.target.value)}
                                    className='border border-gray-300 px-4 py-3 outline-none focus:border-black'
                                    placeholder='Tỉnh / Thành phố'
                                />
                                <input
                                    value={receiverForm.addr.district}
                                    onChange={(event) => updateAddressField('district', event.target.value)}
                                    className='border border-gray-300 px-4 py-3 outline-none focus:border-black'
                                    placeholder='Quận / Huyện'
                                />
                                <input
                                    value={receiverForm.addr.street}
                                    onChange={(event) => updateAddressField('street', event.target.value)}
                                    className='border border-gray-300 px-4 py-3 outline-none focus:border-black sm:col-span-2'
                                    placeholder='Tên đường'
                                />
                                <input
                                    value={receiverForm.addr.detail}
                                    onChange={(event) => updateAddressField('detail', event.target.value)}
                                    className='border border-gray-300 px-4 py-3 outline-none focus:border-black sm:col-span-2'
                                    placeholder='Số nhà, tòa nhà, ghi chú địa chỉ'
                                />
                                <input
                                    value={receiverForm.addr.country}
                                    onChange={(event) => updateAddressField('country', event.target.value)}
                                    className='border border-gray-300 px-4 py-3 outline-none focus:border-black sm:col-span-2'
                                    placeholder='Quốc gia'
                                />
                            </div>
                        )}
                    </section>

                    <section className='border bg-white p-5 sm:p-6 shadow-sm'>
                        <p className='text-lg font-medium text-gray-900'>Ghi chú</p>
                        <textarea
                            value={note}
                            onChange={(event) => setNote(event.target.value)}
                            maxLength={500}
                            className='mt-4 min-h-[110px] w-full border border-gray-300 px-4 py-3 outline-none focus:border-black'
                            placeholder='Ví dụ: giao giờ hành chính, gọi trước khi giao...'
                        />
                    </section>
                </div>

                <div className='space-y-6'>
                    <section className='border bg-white p-5 sm:p-6 shadow-sm'>
                        <Title text1='SẢN PHẨM' text2='ĐẶT MUA' />
                        {orderItems.length === 0 ? (
                            <div className='py-10 text-center text-gray-500'>
                                <p>Giỏ hàng của bạn đang trống.</p>
                                <button type='button' onClick={() => navigate?.('/collection')} className='mt-5 bg-black px-6 py-3 text-sm text-white'>
                                    Mua sắm ngay
                                </button>
                            </div>
                        ) : (
                            <div className='mt-5 divide-y'>
                                {orderItems.map((item) => (
                                    <div key={`${item.productCode}-${item.size}`} className='py-4 flex gap-4'>
                                        <img className='h-20 w-16 border object-cover' src={item.product.image[0]} alt={item.product.name} />
                                        <div className='min-w-0 flex-1'>
                                            <p className='text-sm font-medium text-gray-900'>{item.product.name}</p>
                                            <p className='mt-1 text-xs uppercase tracking-[0.16em] text-gray-400'>Size {item.size}</p>
                                            <div className='mt-2 flex items-center justify-between text-sm'>
                                                <span className='text-gray-500'>SL {item.quantity}</span>
                                                <span className='font-medium'>{formatCurrency(item.finalPrice * item.quantity)}</span>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </section>

                    <section className='border bg-white p-5 sm:p-6 shadow-sm'>
                        <Title text1='PHƯƠNG THỨC' text2='THANH TOÁN' />
                        <div className='mt-5 grid gap-3'>
                            <button
                                type='button'
                                onClick={() => setPaymentType('PAYMENT_UPON_DELIVER')}
                                className={`border p-4 text-left transition-all ${paymentType === 'PAYMENT_UPON_DELIVER' ? 'border-black bg-black text-white' : 'border-gray-200 hover:border-black'}`}
                            >
                                <div className='flex items-center gap-3'>
                                    <img className='h-5 w-auto' src={assets.razorpay_logo} alt='Thanh toán khi nhận hàng' />
                                    <div>
                                        <p className='text-sm font-medium'>Thanh toán khi nhận hàng</p>
                                        <p className={`mt-1 text-xs ${paymentType === 'PAYMENT_UPON_DELIVER' ? 'text-white/80' : 'text-gray-500'}`}>Đơn bắt đầu ở trạng thái PENDING.</p>
                                    </div>
                                </div>
                            </button>
                            <button
                                type='button'
                                onClick={() => setPaymentType('ONLINE')}
                                className={`border p-4 text-left transition-all ${paymentType === 'ONLINE' ? 'border-black bg-black text-white' : 'border-gray-200 hover:border-black'}`}
                            >
                                <div className='flex items-center gap-3'>
                                    <img className='h-5 w-auto bg-white px-1 py-0.5' src={assets.stripe_logo} alt='Thanh toán online' />
                                    <div>
                                        <p className='text-sm font-medium'>Thanh toán online</p>
                                        <p className={`mt-1 text-xs ${paymentType === 'ONLINE' ? 'text-white/80' : 'text-gray-500'}`}>Đơn bắt đầu ở trạng thái UNPAID.</p>
                                    </div>
                                </div>
                            </button>
                        </div>

                        <div className='mt-6 bg-gray-50 px-4 py-4 text-sm'>
                            <div className='flex justify-between text-gray-600'>
                                <span>Tạm tính sản phẩm</span>
                                <span>{formatCurrency(subtotal)}</span>
                            </div>
                            <div className='mt-2 flex justify-between text-gray-600'>
                                <span>Giảm giá</span>
                                <span>{formatCurrency(0)}</span>
                            </div>
                            <div className='mt-3 flex justify-between border-t border-gray-200 pt-3 text-base font-medium text-gray-900'>
                                <span>Tổng tạo đơn</span>
                                <span>{formatCurrency(subtotal)}</span>
                            </div>
                        </div>

                        {selectedReceiver && receiverMode === 'select' && (
                            <div className='mt-5 border border-gray-100 px-4 py-3 text-sm text-gray-600'>
                                <p className='font-medium text-gray-900'>Giao đến</p>
                                <p className='mt-1'>{[selectedReceiver.fName, selectedReceiver.lName].filter(Boolean).join(' ')}</p>
                                <p className='mt-1'>{compactAddress(selectedReceiver)}</p>
                            </div>
                        )}

                        {createdOrder?.bankTransferQr?.qrImageUrl && (
                            <div className='mt-5 border border-green-100 bg-green-50 p-4 text-sm text-green-800'>
                                <p className='font-medium'>Đơn {createdOrder.orderCode} đã được tạo.</p>
                                <img className='mt-3 w-44 border bg-white p-2' src={createdOrder.bankTransferQr.qrImageUrl} alt='QR thanh toán' />
                                <p className='mt-2'>Nội dung chuyển khoản: {createdOrder.bankTransferQr.transferContent}</p>
                            </div>
                        )}

                        <button
                            type='submit'
                            disabled={submitting || orderItems.length === 0}
                            className='mt-6 w-full bg-black px-8 py-3 text-sm font-medium text-white transition-all hover:bg-gray-800 disabled:cursor-not-allowed disabled:bg-gray-300'
                        >
                            {submitting ? 'ĐANG TẠO ĐƠN HÀNG...' : 'XÁC NHẬN ĐẶT HÀNG'}
                        </button>
                    </section>
                </div>
            </div>
        </form>
    );
};

export default PlaceOrder;
