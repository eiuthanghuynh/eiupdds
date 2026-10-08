# BACKEND & CLOUD/AI SPRINT BACKLOG — THẮNG

> **Chỉ dẫn cho AI Agent:**
> Khi người dùng nhắc đến cụm từ **"Tôi là Thắng"**, **"Thắng đây"**, hoặc khai báo vai trò là **Thắng**:
> 1. Tự động nhận diện vai trò: **Backend Lead & AI Cloud Engineer** (Spring Boot, PostgreSQL, Docker, Roboflow Proxy, Storage).
> 2. Đọc file này để xác định Sprint hiện tại và các task chưa hoàn thành (`- [ ]`).
> 3. Tuyệt đối không can thiệp vào logic giao diện Mobile hay Web Admin trừ khi được yêu cầu API Contract.
> 4. Khi implement xong mỗi task, tự động cập nhật trạng thái thành `- [x]`.

---

## Thông Tin Thành Viên & Tech Stack
* **Thành viên:** Thắng
* **Vai trò:** Backend Developer & Data/AI Engineer
* **Công nghệ cốt lõi:** Java 17+, Spring Boot 3.x, Spring Data JPA, PostgreSQL, Docker / Docker Compose, MinIO / Cloudinary, RESTful API (OpenAPI 3 / Swagger).
* **Workspace dự kiến:** `server/` hoặc `backend/`

---

## Sprint 1: Khởi Tạo Nền Tảng & Đặc Tả API Contract (Tuần 1 - 2)
**Mục tiêu Sprint:** Dựng khung dịch vụ Spring Boot, Database và kết nối thành công với Roboflow API.

- [ ] **Task 1.1: Khởi tạo Spring Boot Project & Docker Compose** (4h)
  - Khởi tạo project Spring Boot 3.x (Dependencies: Spring Web, Spring Data JPA, PostgreSQL Driver, Lombok, Validation).
  - Viết file `docker-compose.yml` khởi chạy PostgreSQL 15+ và MinIO local.
  - *Acceptance Criteria:* Chạy `docker compose up -d` và `mvn spring-boot:run` kết nối DB thành công.

- [ ] **Task 1.2: Thiết kế Database Schema & Entity** (6h)
  - Tạo các JPA Entities: `User`, `Disease` (danh mục bệnh), `Diagnosis` (phiên chẩn đoán), `BoundingBox` (tọa độ nhãn), `Treatment` (phác đồ).
  - Cấu hình quan hệ One-to-Many giữa `Diagnosis` và `BoundingBox`.
  - *Acceptance Criteria:* Hibernate sinh đúng schema tables trong PostgreSQL, có migration script (Flyway hoặc Liquibase nếu cần).

- [x] **Task 1.3: Tích hợp Thư viện Roboflow Inference API** (6h)
  - Pre-trained Model chính thức: `plant-leaf-diseases/10` (mAP@50: 70.7%, Precision: 71.0%, Recall: 65.1%, 30 classes).
  - Direct Model Endpoint: `POST https://detect.roboflow.com/plant-leaf-diseases/10?api_key=${ROBOFLOW_API_KEY}`.
  - Workflow ID tích hợp: `plant-disease-yolo-onfen`, Workspace: `plant-disease-detection-mztfb`.
  - Client script: `ai-pipeline/scripts/roboflow_workflow_client.py` (hỗ trợ `Authorization: Bearer`, exponential backoff retry, decode ảnh base64 ra đĩa an toàn).
  - Smoke test: `ai-pipeline/scripts/test_workflow_smoke.py`.
  - *Acceptance Criteria:* Chạy `python ai-pipeline/scripts/test_workflow_smoke.py` với `ROBOFLOW_API_KEY` pass 100%.

- [ ] **Task 1.4: Đặc tả API Contract qua Swagger / OpenAPI** (4h)
  - Tích hợp `springdoc-openapi-starter-webmvc-ui`.
  - Định nghĩa sẵn response DTOs và mock endpoints cho Huy (Mobile):
    - `POST /api/v1/diagnoses/analyze` (upload ảnh $\rightarrow$ trả Bounding Boxes + mã bệnh).
    - `GET /api/v1/diseases` (danh sách bệnh).
  - *Acceptance Criteria:* Huy có thể truy cập `http://localhost:8080/swagger-ui.html` để lấy mẫu JSON.

---

## Sprint 2: Core MVP — Luồng Chẩn Đoán & Proxy AI (Tuần 3 - 4)
**Mục tiêu Sprint:** Hoàn thiện pipeline nhận diện khép kín từ Mobile gửi lên qua Backend proxy.

- [ ] **Task 2.1: Hoàn thiện Endpoint Chẩn Đoán Cốt Lõi** (8h)
  - Viết controller và service cho `POST /api/v1/diagnoses/analyze`.
  - Validate kích thước file ảnh, định dạng (JPEG, PNG).
  - Forward ảnh sang Roboflow API, xử lý timeout (fallback 10s).
  - *Acceptance Criteria:* Nhận ảnh từ request multipart, trả về JSON chuẩn hóa: `{ diagnosisId, diseaseName, confidence, boxes: [{ x, y, width, height }] }`.

- [ ] **Task 2.2: Tích hợp Cloudinary/MinIO Storage Service** (6h)
  - Viết `StorageService` interface với 2 implementation: `LocalStorageService` và `CloudinaryService`.
  - Tự động đẩy file ảnh gốc lên Storage và sinh URL công khai an toàn.
  - *Acceptance Criteria:* Ảnh upload lên trả về URL truy cập được ngay lập tức.

- [ ] **Task 2.3: Viết Unit Test & Integration Test cho Luồng Chẩn Đoán** (6h)
  - Viết test với `@SpringBootTest` và MockMvc.
  - Mock kết quả từ Roboflow để test luồng độc lập không tốn quota.
  - *Acceptance Criteria:* Test pass 100% các case: ảnh hợp lệ, ảnh quá dung lượng, API bên thứ ba lỗi 500.

---

## Sprint 3: Lưu Trữ Lịch Sử, GPS & Danh Mục Phác Đồ (Tuần 5 - 6)
**Mục tiêu Sprint:** Quản lý dữ liệu bệnh, phác đồ điều trị và lưu vết lịch sử chẩn đoán.

- [ ] **Task 3.1: API Lưu Vết Lịch Sử Chẩn Đoán Kèm GPS** (8h)
  - Cập nhật bảng `Diagnosis` nhận thêm `latitude`, `longitude`, `deviceInfo`.
  - Viết endpoint `GET /api/v1/diagnoses` (phân trang, lọc theo thời gian/người dùng).
  - Viết endpoint `GET /api/v1/diagnoses/{id}` (chi tiết lần khám).
  - *Acceptance Criteria:* Truy vấn danh sách lịch sử trả về đầy đủ ảnh, tọa độ và phác đồ sơ cứu.

- [ ] **Task 3.2: CRUD Danh Mục Bệnh & Phác Đồ Điều Trị Cho Web Admin** (8h)
  - Xây dựng các endpoints:
    - `GET /api/v1/diseases`
    - `POST /api/v1/diseases`
    - `PUT /api/v1/diseases/{id}`
    - `DELETE /api/v1/diseases/{id}`
    - `GET /api/v1/diseases/{id}/treatments`
  - *Acceptance Criteria:* Hỗ trợ Web Admin (Huy) thêm, sửa, xóa danh mục bệnh và tài liệu phác đồ.

- [ ] **Task 3.3: Seed Data Mẫu Chuẩn Cho Demo** (4h)
  - Viết script SQL hoặc `DataInitializer` nạp sẵn 10-15 bệnh cây trồng phổ biến (lúa, cà chua, ngô, khoai tây) kèm phác đồ mẫu tiếng Việt.
  - *Acceptance Criteria:* Chạy lại ứng dụng tự động có sẵn dữ liệu mẫu trong DB.

---

## Sprint 4: Bảo Mật, Rate Limiting & Tối Ưu Triển Khai (Tuần 7 - 8)
**Mục tiêu Sprint:** Bảo vệ API, tránh lãng phí quota AI và đóng gói Docker chuẩn hóa.

- [ ] **Task 4.1: Xác Thực JWT & Phân Quyền (Spring Security)** (8h)
  - Cấu hình Spring Security với JWT token.
  - Phân quyền 2 vai trò: `USER` (Mobile app) và `ADMIN` (Web Admin).
  - *Acceptance Criteria:* Endpoints quản lý bệnh yêu cầu quyền ADMIN; endpoint chẩn đoán yêu cầu USER hoặc public token.

- [ ] **Task 4.2: Rate Limiting Chống Spam Upload Ảnh** (5h)
  - Áp dụng Bucket4j hoặc logic kiểm soát tần suất request theo IP/User (tối đa 10 request/phút).
  - *Acceptance Criteria:* Gửi request thứ 11 liên tiếp trả về HTTP 429 Too Many Requests.

- [ ] **Task 4.3: Dockerfile & CI/CD Pipeline (GitHub Actions)** (7h)
  - Viết Dockerfile đa tầng (Multi-stage build) tối ưu dung lượng image Spring Boot (< 200MB).
  - Viết `.github/workflows/backend-ci.yml` tự động build và chạy test khi có commit/PR vào nhánh `main`.
  - *Acceptance Criteria:* Pipeline GitHub Actions chạy pass toàn bộ test case.

---

## Sprint 5: Nghiệm Thu, Tài Liệu Đồ Án & Hỗ Trợ Demo (Tuần 9 - 10)
**Mục tiêu Sprint:** Kiểm thử toàn diện, viết thuyết minh đồ án và chuẩn bị môi trường demo.

- [ ] **Task 5.1: Deploy Server Lên Môi Trường Thực Tế (VPS / Cloud)** (6h)
  - Deploy Docker Compose (Spring Boot + PostgreSQL) lên VPS (DigitalOcean / Oracle Free / EC2).
  - Cấu hình Nginx reverse proxy và chứng chỉ SSL Let's Encrypt (HTTPS bắt buộc để Mobile gọi được).
  - *Acceptance Criteria:* Mobile gọi được API qua domain HTTPS công khai.

- [ ] **Task 5.2: Viết Tài Liệu Kỹ Thuật Đồ Án (Chương Kiến Trúc & CSDL)** (10h)
  - Vẽ sơ đồ kiến trúc phân tán (Component Diagram, Deployment Diagram).
  - Vẽ sơ đồ thực thể liên kết (ERD) và từ điển dữ liệu (Data Dictionary).
  - Thống kê benchmark độ trễ xử lý API chẩn đoán.
  - *Acceptance Criteria:* Hoàn thành nội dung Word nộp cho giảng viên hướng dẫn.

- [ ] **Task 5.3: Chuẩn Bị Kịch Bản Demo Phòng Thi** (4h)
  - Chuẩn bị sẵn tài khoản admin, dữ liệu chẩn đoán mẫu và kịch bản dự phòng khi mất kết nối mạng.
  - *Acceptance Criteria:* Diễn tập kịch bản demo trơn tru cùng Huy.
