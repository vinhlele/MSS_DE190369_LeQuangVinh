# HƯỚNG DẪN TRIỂN KHAI VÀ NỘP BÀI THI MSS301 PRACTICAL EXAM 2026 SUMMER

**Mã sinh viên:** `de190369`  
**Sinh viên:** Lê Quang Vinh  
**Học phần:** MSS301 - Microservices Architecture  

---

## 1. TỔNG QUAN KIẾN TRÚC HỆ THỐNG

Hệ thống được xây dựng theo kiến trúc Microservices gồm 3 ứng dụng Spring Boot độc lập:

```
[ Client / Browser / Postman ]
               │
               ▼ Port 8080
    ┌──────────────────────┐
    │  de190369HotelGateway│ (Spring Cloud Gateway WebFlux + Spring Security + Swagger Aggregation)
    └──────────┬───────────┘
               │
      ┌────────┴────────────────────────┐
      ▼ /api/rooms/**                   ▼ /api/reservations/**
┌──────────────────────┐         ┌──────────────────────────┐
│  de190369RoomService │         │de190369ReservationService│
│     (Port 8081)      │◄──Feign─┤       (Port 8082)        │
└──────────┬───────────┘         └────────────┬─────────────┘
           │                                  │
           └─────────────────┬────────────────┘
                             ▼
                   [( SQL Server 2016+ )]
                   Database: MSS301_2026_PE
```

1. **`de190369HotelGateway` (Port 8080)**: Cổng giao tiếp duy nhất (Single Entry Point), định tuyến request, xử lý CORS toàn diện, cấu hình Spring Security cho phép truy cập mở không cần đăng nhập, gom Swagger UI từ cả 2 service về 1 trang chung.
2. **`de190369RoomService` (Port 8081)**: Quản lý danh mục phòng nghỉ (bảng `rooms`), kiểm tra trùng số phòng, phân trang và lọc theo loại phòng/trạng thái.
3. **`de190369ReservationService` (Port 8082)**: Quản lý đơn đặt phòng (bảng `reservations`), sử dụng **Spring Cloud OpenFeign** gọi sang RoomService để xác minh phòng tồn tại và đang `AVAILABLE`, kiểm tra sức chứa, tự động tính tổng tiền `pricePerNight × số đêm`.

---

## 2. YÊU CẦU MÔI TRƯỜNG & HẠ TẦNG

- **Java JDK:** 21 trở lên (đã kiểm tra tương thích hoàn hảo trên JDK 22).
- **Maven:** Apache Maven 3.9 trở lên.
- **Database Server:** Microsoft SQL Server 2016 trở lên tại `localhost:1433`.
- **Tài khoản SQL:** Username `sa` / Password `sa`.
- **Database Name:** `MSS301_2026_PE`.

---

## 3. THIẾT LẬP CƠ SỞ DỮ LIỆU (DATABASE SETUP)

Nếu cần khởi tạo hoặc reset lại cơ sở dữ liệu về trạng thái ban đầu của đề thi, thực hiện các bước sau trong SQL Server Management Studio (SSMS) hoặc `sqlcmd`:

```sql
CREATE DATABASE MSS301_2026_PE;
GO
USE MSS301_2026_PE;
GO

-- 1. Chạy file create_rooms_sql.txt
-- 2. Chạy file create_reservations_sql.txt

-- 3. Dữ liệu Seed ban đầu cho bảng rooms:
INSERT INTO dbo.rooms (room_number, room_type, price_per_night, capacity, floor, status) VALUES
 ('101', 'SINGLE', 50.00, 1, 1, 'AVAILABLE'),
 ('102', 'DOUBLE', 80.00, 2, 1, 'AVAILABLE'),
 ('201', 'SUITE', 150.00, 4, 2, 'AVAILABLE'),
 ('202', 'DELUXE', 200.00, 4, 2, 'MAINTENANCE');
GO
```

---

## 4. THỨ TỰ KHỞI ĐỘNG CÁC SERVICE

Để hệ thống hoạt động chính xác và OpenFeign / Swagger Aggregation kết nối thông suốt, khởi động các service theo thứ tự:

1. **Khởi động RoomService (8081) trước:**
   ```powershell
   cd d:\MSS\slot17\de190369RoomService
   mvn spring-boot:run
   ```
2. **Khởi động ReservationService (8082) tiếp theo:**
   ```powershell
   cd d:\MSS\slot17\de190369ReservationService
   mvn spring-boot:run
   ```
3. **Khởi động HotelGateway (8080) cuối cùng:**
   ```powershell
   cd d:\MSS\slot17\de190369HotelGateway
   mvn spring-boot:run
   ```

---

## 5. BẢNG DANH MỤC CỔNG & ĐƯỜNG DẪN SWAGGER UI

| Service | Port | Base API URL | Swagger UI URL |
| :--- | :---: | :--- | :--- |
| **Hotel Gateway** | **8080** | `http://localhost:8080` | 👉 [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) *(Gom cả 2 service)* |
| **Room Service** | **8081** | `http://localhost:8081` | 👉 [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) |
| **Reservation Service** | **8082** | `http://localhost:8082` | 👉 [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html) |

---

## 6. QUY TẮC MÃ PHẢN HỒI (STATUS CODE MAPPING)

Tất cả các API đều phản hồi theo cấu trúc thống nhất `ApiResponseDTO`:
```json
{
  "status": 1,
  "message": "Successful",
  "data": { ... },
  "timestamp": "2026-10-30T09:40:53Z"
}
```

| HTTP Status Code | ApiResponseDTO `status` | Ý nghĩa | Ví dụ |
| :---: | :---: | :--- | :--- |
| **200 OK** | **1** | Thao tác truy vấn / cập nhật / xóa mềm thành công. | GET chi tiết phòng, PUT cập nhật. |
| **201 Created** | **1** | Tạo mới tài nguyên thành công. | POST phòng mới, POST đặt phòng mới. |
| **226 IM Used** | **3** | Trùng lặp số phòng (`room_number`). | POST phòng với số phòng đã tồn tại. |
| **400 Bad Request** | **5** | Phòng không ở trạng thái AVAILABLE để đặt. | Đặt phòng đang ở trạng thái `MAINTENANCE`. |
| **404 Not Found** | **4** | Không tìm thấy tài nguyên (`roomId` hoặc `reservationId`). | GET/PUT/DELETE với ID không tồn tại. |
| **406 Not Acceptable** | **2** | Lỗi validation hoặc vi phạm ràng buộc nghiệp vụ. | Sức chứa > 10, ngày checkout <= checkin, vượt quá sức chứa phòng. |
| **500 Internal Error** | **0** | Lỗi hệ thống nội bộ. | Mất kết nối database hoặc lỗi runtime bất ngờ. |

---

## 7. CÔNG CỤ VÀ HƯỚNG DẪN KIỂM THỬ

Hệ sinh thái kiểm thử đã được tích hợp sẵn 2 công cụ tiện lợi:

1. **Postman Collection:** File [`MSS301_Summer2026_Postman_Collection.json`](file:///d:/MSS/slot17/MSS301_Summer2026_Postman_Collection.json)
   - Mở Postman $\rightarrow$ Chọn **Import** $\rightarrow$ Kéo thả file này vào.
   - Chứa đầy đủ các thư mục kiểm thử: Swagger Aggregation, Rooms API, Reservations API với cả trường hợp thành công và các mã lỗi 226, 400, 404, 406.
2. **HTTP Test Client (IntelliJ / VS Code):** File [`api-test.http`](file:///d:/MSS/slot17/api-test.http)
   - Mở trực tiếp trong IntelliJ IDEA và bấm nút xanh Run bên cạnh từng request để test ngay mà không cần rời IDE.

---

## 8. XỬ LÝ SỰ CỐ THƯỜNG GẶP (TROUBLESHOOTING)

1. **Lỗi `Port 8080/8081/8082 was already in use`:**
   - Kiểm tra và tắt các tiến trình Java cũ đang chiếm cổng:
     ```powershell
     Get-NetTCPConnection -LocalPort 8080, 8081, 8082 -ErrorAction SilentlyContinue | Select-Object LocalPort, OwningProcess
     Stop-Process -Id <PID> -Force
     ```
2. **Lỗi `Failed to obtain JDBC Connection`:**
   - Kiểm tra xem SQL Server đã được bật và cổng 1433 đang mở chưa:
     ```powershell
     Test-NetConnection -ComputerName localhost -Port 1433
     ```
   - Đảm bảo tài khoản `sa` với password `sa` đã được kích hoạt chế độ Mixed Mode Authentication (SQL Server and Windows Authentication).
3. **Lỗi Swagger UI trên Gateway báo `Failed to load API definition`:**
   - Đảm bảo cả `de190369RoomService` (8081) và `de190369ReservationService` (8082) đều đang chạy. Trình duyệt client trực tiếp fetch `/v3/api-docs` từ từng port thông qua cấu hình CORS đã thiết lập.

---

## 9. CHECKLIST NỘP BÀI THI LÊN HỆ THỐNG PEA

> [!IMPORTANT]
> Trước khi nén nộp bài, bắt buộc phải dọn dẹp thư mục `target/` để file nén nhỏ hơn giới hạn 40MB của PEA.

1. **Bước 1: Chạy `mvn clean` trên cả 3 dự án:**
   ```powershell
   cd d:\MSS\slot17\de190369RoomService; mvn clean
   cd d:\MSS\slot17\de190369ReservationService; mvn clean
   cd d:\MSS\slot17\de190369HotelGateway; mvn clean
   ```
2. **Bước 2: Nén thư mục mã nguồn:**
   - Chọn cả 3 thư mục `de190369RoomService`, `de190369ReservationService`, `de190369HotelGateway` (hoặc thư mục chứa bài thi).
   - Chuột phải $\rightarrow$ **Send to** $\rightarrow$ **Compressed (zipped) folder**.
3. **Bước 3: Nộp file `.zip` lên phần mềm thi PEA.**
