export type Product = {
    _id: string;
    code?: string;
    name: string;
    description: string;
    price: number;
    originalPrice?: number;
    finalPrice?: number;
    image: string[];
    category: string;
    categoryCode?: string;
    subCategory: string;
    providerCode?: string;
    sizes: string[];
    date: number;
    bestSeller: boolean;
};

export type CartItems = Record<string, Record<string, number>>;

export type ApiResponse<T> = {
    success: boolean;
    message?: string;
    data: T;
};

export type PageResponse<T> = {
    items: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    hasNext: boolean;
};

export type BackendProductSummary = {
    code: string;
    name: string;
    price?: number;
    originalPrice?: number;
    rated?: number;
    imgUrl?: string;
    categoryCode?: string;
};

export type BackendProductDetail = {
    name: string;
    productCode: string;
    description?: string;
    originalPrice?: number;
    finalPrice?: number;
    imgUrl?: string;
    status?: string;
    items?: Array<{
        productCode: string;
        size: string;
        status: string;
        quantity: number;
    }>;
    createdAt?: string;
    category?: {
        name?: string;
        categoryCode?: string;
    };
    provider?: {
        name?: string;
        providerCode?: string;
    };
};

export type BackendCart = {
    items?: Array<{
        productCode: string;
        size: string;
        quantity: number;
    }>;
    totalItems: number;
    totalAmount: number;
};

export type UserProfile = {
    id: string;
    firstName?: string;
    lastName?: string;
    avatar?: string;
    f_name?: string;
    l_name?: string;
    img?: string;
    phone?: string;
};

export type UserProfileUpdatePayload = {
    id: string;
    firstName: string;
    lastName: string;
    avatar?: string;
};

export type OrderStatus =
    | 'UNPAID'
    | 'PAID'
    | 'PENDING'
    | 'CANCELLED'
    | 'CONFIRMED'
    | 'SHIPPING'
    | 'DELIVERED'
    | 'COMPLETED'
    | 'RETURNED';

export type OrderSummary = {
    orderCode: string;
    status: OrderStatus;
    finalPrice: number;
    totalItems: number;
    createdAt: string;
};

export type OrderItem = {
    productCode: string;
    size: string;
    quantity: number;
    originalPrice?: number;
    finalPrice?: number;
};

export type Receiver = {
    id?: number;
    fName?: string;
    lName?: string;
    phone?: string;
    addr?: {
        country?: string;
        province?: string;
        district?: string;
        street?: string;
        detail?: string;
    };
};

export type OrderDetail = {
    orderCode: string;
    status: OrderStatus;
    paymentType?: 'PAYMENT_UPON_DELIVER' | 'ONLINE';
    voucherCode?: string;
    reciever?: Receiver;
    receiver?: Receiver;
    note?: string;
    items?: OrderItem[];
    totalAmount?: number;
    voucherDiscount?: number;
    finalPrice?: number;
    paymentUrl?: string;
    createdAt?: string;
    updatedAt?: string;
};

export type ReceiverCreatePayload = {
    fName: string;
    lName: string;
    phone: string;
    addr: {
        country: string;
        province: string;
        district: string;
        street: string;
        detail: string;
    };
};

export type CreateOrderPayload = {
    recieverid: number;
    items: Array<{
        productCode: string;
        size: string;
        quantity: number;
        originalPrice: number;
        finalPrice?: number;
    }>;
    totalPrice: number;
    voucherCode?: string;
    voucherDiscount?: number;
    finalPrice: number;
    paymentType: 'PAYMENT_UPON_DELIVER' | 'ONLINE';
    note?: string;
};

export type PaymentQr = {
    provider?: string;
    bankId?: string;
    accountNo?: string;
    accountName?: string;
    amount?: number;
    transferContent?: string;
    qrContent?: string;
    qrImageUrl?: string;
};

export type CreateOrderResponse = {
    orderCode: string;
    status: OrderStatus;
    paymentType?: 'PAYMENT_UPON_DELIVER' | 'ONLINE';
    paymentUrl?: string;
    bankTransferQr?: PaymentQr;
};
