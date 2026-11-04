# Saga Design – Watch Management

> [TODO-4.1] Tài liệu thiết kế Saga cho hệ thống Watch Management (Student: DE190369).

## 1. Participants

| Service | Vai trò | Database | Dữ liệu tham gia Saga |
|---|---|---|---|
| MSWatch | Orchestrator | DBWatch | `Watches`, `SagaStates`, `SagaLogs` |
| MSBrand | Participant | DBBrand | `BrandWatches` |

---

## 2. Thiết kế 3 Saga

### Saga CREATE_WATCH

| # | Local transaction | Service / DB | Compensation | Idempotent? |
|---|---|---|---|---|
| 1 | `CREATE_WATCH` – INSERT Watches | MSWatch / DBWatch | `DELETE_WATCH` (xoá watch vừa thêm) | ✔ (`deleteIfExists`) |
| 2 | `ASSIGN_BRAND` – upsert BrandWatches | MSBrand / DBBrand | `UNASSIGN_BRAND` (gỡ watch khỏi brand) | ✔ |

### Saga UPDATE_WATCH

| # | Local transaction | Service / DB | Compensation | Idempotent? |
|---|---|---|---|---|
| 1 | `UPDATE_WATCH` – ghi dữ liệu mới | MSWatch / DBWatch | `RESTORE_WATCH` (ghi lại snapshot `before`) | ✔ |
| 2 | `ASSIGN_BRAND` – gán sang brand mới | MSBrand / DBBrand | `ASSIGN_BRAND_BACK` (gán lại brand cũ) | ✔ |

### Saga DELETE_WATCH

| # | Local transaction | Service / DB | Compensation | Idempotent? |
|---|---|---|---|---|
| 1 | `UNASSIGN_BRAND` – gỡ watch khỏi brand | MSBrand / DBBrand | `ASSIGN_BRAND_BACK` (gán lại brand cũ) | ✔ |
| 2 | `DELETE_WATCH` – xoá watch | MSWatch / DBWatch | — **PIVOT** (không thể quay lui) | — |

> **Pivot transaction:** Bước `DELETE_WATCH` là bước sau khi commit thì Saga **không thể quay lui** nữa (xoá cứng khoá `IDENTITY` không khôi phục lại đúng `WatchID` cũ). Vì vậy `DELETE_WATCH` được đặt **cuối cùng** làm pivot. Bước có nguy cơ lỗi cao (gọi mạng sang MSBrand) được thực hiện trước khi còn bù trừ được.

---

## 3. State Machine (Máy trạng thái Saga)

```text
PENDING ──► EXECUTING ──────────────────────────────► COMPLETED
                │ (một bước lỗi)
                ▼
             FAILED ──► COMPENSATING ──► COMPENSATED      (đã bù trừ xong, dữ liệu nhất quán)
                              │
                              └─ (compensation lỗi sau 3 lần) dừng ở COMPENSATING → cần xử lý thủ công
```

---

## 4. Luồng thất bại điển hình (CREATE với brand 999)

```text
MSWatch (Orchestrator)                 DBWatch                 MSBrand / DBBrand
   │ [1] CREATE_WATCH ───────────────► INSERT Watches (id=11) ✔ COMMIT
   │ [2] ASSIGN_BRAND ──── PUT /internal/brands/999/watches/11 ──► 404 Brand not found
   │      → FAILED
   │      → COMPENSATING
   │ [C1] DELETE_WATCH ──────────────► DELETE Watches id=11 ✔
   │      → COMPENSATED
   ▼  HTTP 400 {"sagaStatus":"COMPENSATED","failedStep":"ASSIGN_BRAND"}
```

---

## 5. Quyết định thiết kế [TODO-4.2]

1. **Vì sao không dùng `@Transactional` / 2PC cho bài này?**
   - `@Transactional` chỉ quản lý được 1 database cục bộ (DBWatch). Lời gọi HTTP sang MSBrand nằm ngoài transaction đó, nên rollback cục bộ không thể hoàn tác thay đổi bên DBBrand.
   - 2PC (Two-Phase Commit) đòi hỏi lock tài nguyên ở cả hai service trong suốt 2 pha, làm nghẽn hệ thống, coordinator là single point of failure và MSBrand không hỗ trợ chuẩn XA qua REST API.
   - Saga chia giao dịch phân tán thành chuỗi các local transaction độc lập, chấp nhận eventual consistency. Nếu có bước lỗi, các compensating transaction sẽ hoàn tác theo thứ tự ngược (LIFO).

2. **Quy tắc thực thi & Bù trừ:**
   - Orchestrator không có `@Transactional`; mỗi bước là 1 local transaction riêng biệt.
   - Compensation chạy theo thứ tự LIFO (Last-In-First-Out), retry tối đa 3 lần và bắt buộc phải idempotent.
   - Bước xoá cứng (`DELETE_WATCH`) là pivot transaction, bắt buộc đặt cuối cùng trong Saga xoá.
   - Validate nghiệp vụ rẻ (tên > 10 ký tự, giá >= 100, ngày phát hành là hôm nay, danh mục tồn tại) được thực hiện TRƯỚC khi khởi tạo Saga để tránh tạo rác trong `SagaStates`.
