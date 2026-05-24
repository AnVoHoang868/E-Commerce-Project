# Admin Management API Flows

> Scope: phan tich cac API Admin lien quan den quan ly don hang, voucher va receivers trong `backend-e`.
>
> Base URL local: `http://localhost:8080`
>
> Source chinh:
> - `modules/order/controllers/admin/*`
> - `modules/order/services/impl/*`
> - `modules/voucher/controllers/admin/*`
> - `modules/voucher/services/impl/*`
> - `modules/users/controllers/admin/AdminReceiverController.java`

## 1. Xac thuc va response chung

Tat ca endpoint Admin nam duoi `/v1/api/admin/**` va bi chan boi Spring Security:

```http
Authorization: Bearer <admin_access_token>
```

Token phai co role `ADMIN` vi config dang dung:

```java
.requestMatchers("/v1/api/admin/**").hasRole("ADMIN")
```

Neu test tu Swagger, dang nhap admin truoc qua:

```http
POST /v1/api/public/auth/login
X-Device-Id: test-device-001
Content-Type: application/json
```

```json
{
  "username": "admin",
  "password": "<admin-password>"
}
```

Response chung cua backend:

```json
{
  "success": true,
  "message": "message",
  "data": {}
}
```

Response phan trang dung `PageRes<T>`:

```json
{
  "success": true,
  "message": "All orders",
  "data": {
    "items": [],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "hasNext": true
  }
}
```

## 2. Quan ly don hang

### 2.1 Model va trang thai

Trang thai don hang hien co:

```text
UNPAID, PAID, PENDING, CANCELLED, CONFIRMED, SHIPPING, DELIVERED, COMPLETED, RETURNED
```

Khi tao don tu user:

- `paymentType = ONLINE` -> trang thai ban dau `UNPAID`
- `paymentType = PAYMENT_UPON_DELIVER` -> trang thai ban dau `PENDING`

Luon chuyen trang thai hop le trong ham tong quat `adminUpdateOrderStatus`:

```text
UNPAID    -> PAID
PAID      -> PENDING
PENDING   -> CONFIRMED | CANCELLED
CONFIRMED -> SHIPPING
SHIPPING  -> DELIVERED | RETURNED
DELIVERED -> RETURNED
COMPLETED -> terminal
CANCELLED -> terminal
RETURNED  -> terminal
```

Khuyen nghi FE Admin nen dung endpoint update status tong quat de tranh cac endpoint shortcut dang co logic chua nhat quan.

### 2.2 Lay danh sach don hang

```http
GET /v1/api/admin/order/get/all?page=0&size=20
Authorization: Bearer <admin_access_token>
```

Controller: `AdminGetOrderController.getAllOrders`

Response `data.items[]` la `OrderSummaryRes`:

```json
{
  "orderCode": "ORD-...",
  "status": "PENDING",
  "finalPrice": 250000,
  "totalItems": 2,
  "createdAt": "2026-05-24T03:00:00Z"
}
```

Ghi chu:

- Repository dang `ORDER BY o.createdAt DESC`, nen sort tu query string co the khong anh huong nhu Swagger mo ta.
- FE dung `orderCode` de vao trang chi tiet hoac cap nhat trang thai.

### 2.3 Loc don hang theo trang thai

```http
GET /v1/api/admin/order/get/by-status?status=CONFIRMED&page=0&size=20
Authorization: Bearer <admin_access_token>
```

Controller: `AdminGetOrderController.getOrdersByStatus`

`status` la enum `OrderStatus`.

### 2.4 Xem chi tiet don hang

```http
GET /v1/api/admin/order/get/{orderCode}
Authorization: Bearer <admin_access_token>
```

Controller: `AdminGetOrderController.getOrderDetail`

Response `data` la `OrderRes`:

```json
{
  "orderCode": "ORD-...",
  "status": "CONFIRMED",
  "paymentType": "PAYMENT_UPON_DELIVER",
  "voucherCode": "SALE10",
  "reciever": {
    "addr": {
      "country": "VN",
      "province": "Ha Noi",
      "district": "Cau Giay",
      "street": "Tran Thai Tong",
      "detail": "So 1"
    },
    "fName": "An",
    "lName": "Nguyen",
    "phone": "0900000000"
  },
  "note": "Giao gio hanh chinh",
  "items": [
    {
      "productCode": "TSHIRT001",
      "size": "M",
      "quantity": 1,
      "originalPrice": 200000,
      "finalPrice": 180000
    }
  ],
  "totalAmount": 200000,
  "voucherDiscount": 20000,
  "finalPrice": 180000,
  "paymentUrl": null,
  "bankTransferQr": null,
  "createdAt": "2026-05-24T03:00:00Z",
  "updatedAt": "2026-05-24T03:00:00Z"
}
```

Ghi chu: field trong response dang ten `reciever`, khong phai `receiver`.

### 2.5 Cap nhat trang thai don hang

Endpoint nen dung cho Admin UI:

```http
PUT /v1/api/admin/order/update/status
Authorization: Bearer <admin_access_token>
Content-Type: application/json
```

```json
{
  "orderCode": "ORD-...",
  "status": "SHIPPING"
}
```

Controller: `AdminUpdateOrderController.updateStatus`

Backend se validate transition theo bang o muc 2.1. Neu transition sai, response la `409 Conflict`.

### 2.6 Cac endpoint shortcut hien co

```http
PUT /v1/api/admin/order/update/confirm/{code}
PUT /v1/api/admin/order/update/shipping/{code}
PUT /v1/api/admin/order/update/delivered/{code}
PUT /v1/api/admin/order/update/returned/{code}
```

Luu y implementation hien tai:

| Endpoint | Dieu kien hien tai | Trang thai set | Ghi chu |
|---|---:|---:|---|
| `/confirm/{code}` | current = `SHIPPING` | `DELIVERED` | Ten endpoint khong khop logic confirm |
| `/shipping/{code}` | current = `CONFIRMED` | `DELIVERED` | Co ve bug, dang bo qua `SHIPPING` |
| `/delivered/{code}` | current = `SHIPPING` | `DELIVERED` | Hop ly cho buoc giao thanh cong |
| `/returned/{code}` | bat ky trang thai khac `RETURNED` | `RETURNED` | Idempotent neu da `RETURNED` |

Khuyen nghi khi sync FE:

- Dung `PUT /v1/api/admin/order/update/status` cho man hinh quan ly trang thai.
- Neu muon dung button shortcut, nen sua backend truoc de `/shipping/{code}` set `SHIPPING` thay vi `DELIVERED`.

### 2.7 Tao don hang tu Admin

Hien chua co API tao don tu Admin:

- `AdminCreateOrderController` dang la class rong.
- `OrderCreateService.adminCreateOrder(...)` dang return `null`.

Neu FE can chuc nang "Admin tao don ho user", backend can bo sung endpoint va service truoc.

## 3. Quan ly voucher

### 3.1 Model va enum

`DiscountType`:

```text
PERCENT, FIXED
```

`VoucherType`:

```text
NEWBIE, GLOBAL
```

`VoucherStatus`:

```text
ACTIVE, INACTIVE, COMMING_SOON
```

`UserVoucherStatus`:

```text
AVAILABLE, USED, EXPIRED
```

Scheduler voucher chay moi phut:

- Voucher het han -> `INACTIVE`
- Voucher toi thoi diem start -> `ACTIVE`
- User voucher het han -> `EXPIRED`

### 3.2 Tao voucher

```http
POST /v1/api/admin/voucher/create
Authorization: Bearer <admin_access_token>
Content-Type: application/json
```

```json
{
  "code": "SALE10",
  "discountType": "PERCENT",
  "voucherType": "GLOBAL",
  "value": 10,
  "minOrderAmount": 200000,
  "startAt": "2026-05-24T00:00:00Z",
  "endAt": "2026-06-01T00:00:00Z"
}
```

Controller: `AdminVoucherController.createVoucher`

Response `data` la `VoucherRes`:

```json
{
  "discountType": "PERCENT",
  "voucherType": "GLOBAL",
  "status": "ACTIVE",
  "value": 10,
  "minOrderAmount": 200000,
  "endAt": "2026-06-01T00:00:00Z"
}
```

Quy tac hien tai:

- `code` duoc trim va uppercase khi save.
- Neu `startAt` sau thoi diem hien tai -> `COMMING_SOON`.
- Neu `startAt` <= now -> `ACTIVE`.
- Code trung -> `409 Conflict`.

Luu y quan trong:

- DTO cho phep `startAt` null, mapper co default `Instant.now()`, nhung service dang goi `req.startAt().isAfter(...)`. Vi vay FE nen luon gui `startAt` de tranh `500`.
- Response `VoucherRes` hien khong tra `code` va `startAt`; neu Admin UI can hien thi ma voucher sau khi tao, nen them field vao backend response.

### 3.3 Cap nhat voucher

```http
PUT /v1/api/admin/voucher/update?voucher-code=SALE10
Authorization: Bearer <admin_access_token>
Content-Type: application/json
```

```json
{
  "discountType": "PERCENT",
  "voucherType": "GLOBAL",
  "value": 15,
  "minOrderAmount": 300000,
  "startAt": "2026-05-24T00:00:00Z",
  "endAt": "2026-06-10T00:00:00Z"
}
```

Controller: `AdminVoucherController.updateVoucherStatus`

Service update dang la partial update:

- Field null se duoc bo qua.
- Cac field co cap nhat: `discountType`, `voucherType`, `value`, `minOrderAmount`, `startAt`, `endAt`.

Luu y implementation:

- `VoucherUpdateReq` co field `status`, nhung service chua ap dung field nay.
- Service goi `getVoucherByCode`, ham nay chi tra voucher dang `ACTIVE`; voucher `INACTIVE` hoac `COMMING_SOON` se bi xem nhu not found.
- Cap nhat `startAt/endAt` khong tinh lai `status` ngay lap tuc; scheduler moi phut se cap nhat theo thoi gian.

### 3.4 Xem voucher cua user theo user id

```http
GET /v1/api/admin/user-vouchers/get-by-id?uid={userUuid}
Authorization: Bearer <admin_access_token>
```

Controller: `AdminUserVoucherController.getUserVouchersById`

Response `data[]` la `UserVoucherRes`:

```json
[
  {
    "code": "SALE10",
    "type": "PERCENT",
    "value": 10,
    "minOrderAmount": 200000,
    "status": "AVAILABLE",
    "endAt": "2026-06-01T00:00:00Z"
  }
]
```

### 3.5 Xem voucher cua user theo ten

```http
GET /v1/api/admin/user-vouchers/get-by-name?name=nguyen
Authorization: Bearer <admin_access_token>
```

Flow backend:

1. Tim user bang `UserRepository.findByName(name)`.
2. Lay tat ca voucher cua cac user match.
3. Flatten thanh mot danh sach `UserVoucherRes`.

### 3.6 Ghi chu ve ap dung voucher

Admin API hien chi tao/cap nhat voucher va xem user voucher. Chua co endpoint Admin assign voucher thu cong, du `AssignVoucherReq` da ton tai trong source.

Logic ap dung voucher khi user tao order:

- Chi chap nhan voucher `GLOBAL` trong `UserVoucherImpl.getByCode`.
- Neu user chua co row `UserVoucher`, backend tao row `AVAILABLE` khi user ap dung voucher global.
- Voucher `NEWBIE` duoc apply tu dong khi tao user profile.
- Neu user voucher da `USED` hoac `EXPIRED` -> `422`.
- Neu don hang khong dat `minOrderAmount` -> loi.

Can luu y: ham `applyVoucher` hien dang quyet dinh giam theo `value < 100` thay vi doc truc tiep `discountType`; neu `FIXED` co value nho hon 100, logic se xu ly nhu percent.

## 4. Quan ly receivers

Receiver la thong tin nguoi nhan hang gan voi user. Admin co API xem/xoa, chua co API tao/sua receiver tu Admin.

### 4.1 Lay tat ca receivers

```http
GET /v1/api/admin/receiver/get-all?page=0&size=20
Authorization: Bearer <admin_access_token>
```

Controller: `AdminReceiverController.getAll`

Response `data.items[]` la `ReceiverRes`:

```json
{
  "addr": {
    "country": "VN",
    "province": "Ha Noi",
    "district": "Cau Giay",
    "street": "Tran Thai Tong",
    "detail": "So 1"
  },
  "fName": "An",
  "lName": "Nguyen",
  "phone": "0900000000"
}
```

Luu y quan trong cho FE:

- `ReceiverRes` hien khong co `id`.
- Voi man hinh Admin list/delete/detail, FE can `receiverId`. Backend nen bo sung `id` vao `ReceiverRes`, neu khong FE khong the thao tac chinh xac tu danh sach.

### 4.2 Lay receiver theo id

```http
GET /v1/api/admin/receiver?receiverId=1
Authorization: Bearer <admin_access_token>
```

Controller: `AdminReceiverController.getById`

Neu khong tim thay -> `404 Receiver not found`.

### 4.3 Lay receivers cua mot user

```http
GET /v1/api/admin/receiver/by-user?userId={userUuid}
Authorization: Bearer <admin_access_token>
```

Controller: `AdminReceiverController.getAllByUser`

Response la danh sach `ReceiverRes`.

### 4.4 Xoa receiver theo id

```http
DELETE /v1/api/admin/receiver?receiverId=1
Authorization: Bearer <admin_access_token>
```

Controller: `AdminReceiverController.removeById`

Backend goi `receiverRemoveService.removeById(receiverId)` va xoa truc tiep theo id, khong check ownership.

### 4.5 Xoa tat ca receivers cua user

```http
DELETE /v1/api/admin/receiver/by-user?userId={userUuid}
Authorization: Bearer <admin_access_token>
```

Controller: `AdminReceiverController.removeAllByUser`

Backend xoa tat ca receiver co `user_id = userId`.

## 5. Goi y sync FE Admin

### Don hang

Man hinh danh sach:

1. Goi `GET /v1/api/admin/order/get/all?page=0&size=20`.
2. Neu co filter status, goi `GET /v1/api/admin/order/get/by-status?status=...`.
3. Dung `orderCode` de route den detail.

Man hinh chi tiet:

1. Goi `GET /v1/api/admin/order/get/{orderCode}`.
2. Hien items, receiver, price, payment type, voucher.
3. Khi admin doi trang thai, goi `PUT /v1/api/admin/order/update/status`.
4. Sau khi update thanh cong, refetch detail hoac update local state.

### Voucher

Man hinh tao voucher:

1. FE bat buoc gui `startAt`.
2. Chuan hoa input code phia FE neu muon hien thi giong DB: trim + uppercase.
3. Sau khi tao, response chua co `code`; FE nen dung code vua nhap hoac backend nen bo sung field.

Man hinh cap nhat voucher:

1. Chi cap nhat duoc voucher dang `ACTIVE`.
2. Khong dung field `status` cho den khi backend implement.
3. Neu can list/search all vouchers, backend hien chua co Admin API tuong ung.

### Receivers

Man hinh danh sach:

1. Backend hien tra receiver khong co `id`.
2. Nen sua backend `ReceiverRes` de them `id` truoc khi lam UI xoa/detail.
3. Sau khi co `id`, dung:
   - Detail: `GET /v1/api/admin/receiver?receiverId=...`
   - Delete: `DELETE /v1/api/admin/receiver?receiverId=...`

## 6. Cac gap nen xu ly truoc khi FE phu thuoc nhieu

1. `ReceiverRes` thieu `id`, gay kho cho Admin UI.
2. `VoucherRes` thieu `code` va `startAt`, gay kho cho Admin UI.
3. `CreateVoucherReq.startAt` co the null tren DTO nhung service co the NPE; nen fix service hoac validate `@NotNull`.
4. `VoucherUpdateReq.status` chua duoc apply.
5. Chua co Admin API list all vouchers.
6. Chua co Admin API assign voucher cho users, du co `AssignVoucherReq`.
7. `PUT /v1/api/admin/order/update/shipping/{code}` dang set `DELIVERED`, co ve sai voi ten endpoint.
8. `PUT /v1/api/admin/order/update/confirm/{code}` dang yeu cau current `SHIPPING`, co ve sai voi ten endpoint.
9. Chua co Admin create order API.
10. Logic apply voucher dang dua vao `value < 100`, chua dua vao `discountType`.
