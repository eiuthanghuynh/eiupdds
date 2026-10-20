# AGENTS.MD — DỰ ÁN EIU Plant Disease Detection System (EIU PDDS)

> **Dành cho mọi AI Coding Agent:**  
> Đọc kỹ file này để nắm bắt toàn bộ ngữ cảnh hệ thống, kiến trúc luồng dữ liệu, phân vai thành viên và các nguyên tắc phát triển trước khi bắt tay vào sinh mã nguồn.

---

## 1. Tổng Quan Dự Án & Chiến Lược 3 Phase
* **Tên chính thức dự án:** **EIU Plant Disease Detection System** (Tên viết tắt: **EIU PDDS**).
* **Quy ước tên gọi (BẮT BUỘC):** **TUYỆT ĐỐI KHÔNG** sử dụng các tên gọi cũ như "CropPulse" hay "PhytoGuard". Mọi package, repository, API, tài liệu, commit, comment và giao diện bắt buộc dùng định danh **EIU Plant Disease Detection System** hoặc **EIU PDDS**.
* **Tính chất kỹ thuật:** Đồ án tốt nghiệp Kỹ thuật Phần mềm (Software Engineering) tập trung vào phát triển hệ thống phân tán tích hợp thị giác máy tính ứng dụng (Applied Computer Vision).
* **Lộ trình tiến hóa 3 giai đoạn (Agile/Iterative Roadmap):**
  * **Phase 1 (Giai đoạn hiện tại - 10 tuần):** Xây dựng hệ thống End-to-End MVP hoàn chỉnh. Sử dụng Cloud Inference API có sẵn (Roboflow Serverless); tập trung vào luồng dữ liệu khép kín, backend proxy, quản lý lịch sử, cấu trúc API và trải nghiệm Mobile Client.
  * **Phase 2 (Tương lai):** Nghiên cứu nhúng mô hình AI trực tiếp vào thiết bị di động (On-device / Edge AI với TFLite hoặc ONNX) để tối ưu độ trễ và loại bỏ phụ thuộc API bên ngoài.
  * **Phase 3 (Tương lai):** Hiện thực hóa kiến trúc Offline-First chuyên sâu (Local DB, Sync Engine, Outbox Pattern, Exponential Backoff, bản đồ dịch tễ).

---

## 2. Phạm Vi & Ràng Buộc Kỹ Thuật Phase 1 (Core Constraints)
Agent khi tiếp nhận công việc **bắt buộc tuân thủ nghiêm ngặt** các giới hạn sau để tránh vỡ tiến độ:
1. **Không nhúng AI on-device trong Phase 1:** Không cài runtime nén mô hình (TFLite/ONNX) lên thiết bị di động lúc này.
2. **Không làm Offline-First phức tạp:** Ứng dụng yêu cầu có kết nối mạng (Internet Required) để thực hiện chẩn đoán. Chưa làm cơ chế đồng bộ ngoại tuyến phức tạp (Sync Engine / Outbox Pattern).
3. **Không huấn luyện mô hình từ đầu (Zero Custom Training):** Sử dụng Model Object Detection pre-trained chuẩn trên Roboflow: `plant-leaf-diseases/10` (mAP@50: 70.7%, Precision: 71.0%, Recall: 65.1%, 30 classes lá và bệnh) hoặc qua Workflow `plant-disease-yolo-onfen`.
4. **Luồng dữ liệu khép kín qua Backend Proxy (Bắt buộc):**
   * Mobile chụp ảnh $\rightarrow$ gửi Multipart lên Core Backend.
   * Core Backend gọi Roboflow Serverless API (Header: `Authorization: Bearer <ROBOFLOW_API_KEY>`).
   * Core Backend lưu ảnh vào Storage (MinIO/Cloudinary), lưu lịch sử vào PostgreSQL.
   * Core Backend trả về: Tọa độ Bounding Box, nhãn bệnh, độ tin cậy và phác đồ điều trị.
   * Mobile render Bounding Box đè lên ảnh chụp và hiển thị kết quả.

---

## 3. Cấu Trúc Monorepo Chuẩn Enterprise

```text
PROJECT_PlantDiseaseDetection/
├── server/                    # Backend Spring Boot 3.x (Java 21) - Phụ trách: Thắng
│   ├── src/main/java/com/eiu/pdds/
│   │   ├── controller/        # REST Endpoints (/api/v1/diagnoses)
│   │   ├── service/           # Business logic & interfaces
│   │   ├── repository/        # Spring Data JPA
│   │   ├── entity/            # PostgreSQL entities
│   │   ├── dto/               # Request / Response DTOs
│   │   ├── integration/       # RoboflowClient, StorageService
│   │   └── config/            # Swagger, Security, WebClient
│   └── pom.xml / Dockerfile
├── mobile/                    # Mobile App React Native (Expo Dev Client) - Phụ trách: Huy
│   ├── src/
│   │   ├── screens/           # Camera, Result, History
│   │   ├── components/        # BoundingBox Canvas / SVG overlay
│   │   ├── navigation/        # React Navigation
│   │   └── services/          # ApiClient
├── web/                       # Web Dashboard Next.js 14+ (App Router) - Phụ trách: Huy
│   ├── app/                   # Dashboard, Quản lý bệnh, Quản lý phác đồ
│   └── components/            # UI components (shadcn/ui / Tailwind)
├── ai-pipeline/               # Module Computer Vision & Roboflow SDK - Phụ trách: Thắng
│   ├── scripts/               # roboflow_workflow_client.py, test_workflow_smoke.py
│   └── datasets/              # Ảnh mẫu kiểm thử
├── infra/                     # Cấu hình hạ tầng
│   ├── docker-compose.yml     # PostgreSQL 15+, MinIO
│   └── init-scripts/          # seed_diseases.sql
├── .env                       # Chứa bí mật (ROBOFLOW_API_KEY...) - CẤM COMMIT
├── .gitignore
├── TASKS_THANG.md             # Backlog 5 Sprint của Thắng
├── TASKS_HUY.md               # Backlog 5 Sprint của Huy
└── AGENTS.md                  # Tài liệu ngữ cảnh hệ thống cho AI Agent
```

---

## 4. Cơ Chế Nhận Diện Vai Trò Thành Viên (Persona Detection)

Khi người dùng bắt đầu câu lệnh, Agent cần kiểm tra ngay lập tức cụm từ định danh:

### Khi người dùng là Thắng:
* **Từ khóa nhận diện:** `"Tôi là Thắng"`, `"Thắng đây"`, `"Vai trò: Thắng"`
* **Phạm vi trách nhiệm:** Backend (Spring Boot), Database (PostgreSQL), Docker/Infra, Cloud AI Proxy (Roboflow), Storage (MinIO/Cloudinary).
* **Nguồn công việc:** Đọc file [TASKS_THANG.md](file:///e:/PROJECT_PlantDiseaseDetection/TASKS_THANG.md).
* **Hành vi bắt buộc:** Tập trung vào các thư mục `server/`, `ai-pipeline/`, `infra/`. Sau khi hoàn thành một task, tự động đánh dấu `- [x]` vào [TASKS_THANG.md](file:///e:/PROJECT_PlantDiseaseDetection/TASKS_THANG.md).

### Khi người dùng là Huy:
* **Từ khóa nhận diện:** `"Tôi là Huy"`, `"Huy đây"`, `"Vai trò: Huy"`
* **Phạm vi trách nhiệm:** Mobile Client (React Native + Expo Dev Client), Thuật toán vẽ Bounding Box (SVG/Canvas), Web Admin (Next.js 14+), Trải nghiệm người dùng (UI/UX).
* **Nguồn công việc:** Đọc file [TASKS_HUY.md](file:///e:/PROJECT_PlantDiseaseDetection/TASKS_HUY.md).
* **Hành vi bắt buộc:** Tập trung vào các thư mục `mobile/`, `web/`. Sau khi hoàn thành một task, tự động đánh dấu `- [x]` vào [TASKS_HUY.md](file:///e:/PROJECT_PlantDiseaseDetection/TASKS_HUY.md).

---

## 5. Quy Tắc & Rào Cản Kỹ Thuật (Guardrails for Agents)
1. **Bảo mật bí mật:** Tuyệt đối không hardcode API key hoặc commit file `.env`. Đọc `ROBOFLOW_API_KEY` từ biến môi trường.
2. **Không can thiệp chéo mã nguồn:** Agent không được tự ý sửa đổi code của thành viên khác trừ khi đó là hợp đồng giao tiếp chung (API Contract / Swagger DTO).
3. **Phân bổ Sprint rõ ràng:** Mỗi task phải gắn liền với Sprint cụ thể (từ Sprint 1 đến 5). Không nhảy cóc sang tính năng của Sprint sau khi Sprint trước chưa hoàn thành Definition of Done.
4. **Tối ưu phản hồi (ADHD / Action-first):** Bắt đầu phản hồi bằng hành động/lệnh cụ thể có thể chạy được ngay; chia nhỏ bước việc rõ ràng; không nói vòng vo hoặc tóm tắt thừa thãi.
5. **Chuẩn hóa danh xưng & thương hiệu (CẤM NGOẠI LỆ):** Tên hệ thống là **EIU Plant Disease Detection System** (**EIU PDDS**). Tuyệt đối cấm sử dụng các danh xưng cũ "CropPulse", "PhytoGuard" trong mã nguồn, tên package Java (`com.eiu.pdds`), tài liệu, commit hay UI.

---

## 6. Tiến Độ Kỹ Thuật & Kiến Trúc Cơ Sở Dữ Liệu (Database Foundation)

### 6.1. Tiến độ hoàn thành (Sprint 1)
* **Task 1.1:** Khởi tạo Spring Boot 3.3.4 (Java 21 LTS) & Docker Compose (PostgreSQL 16, Redis 7, Pigsty Silo S3/MinIO fork) $\rightarrow$ `[COMPLETED]`
* **Task 1.2:** Thiết kế Database Foundation 8 bảng 3NF, Flyway Migration `V1__init_schema.sql`, JPA Entities, Repositories & Automated Test Suite $\rightarrow$ `[COMPLETED]`
* **Task 1.3:** Tích hợp Roboflow Serverless API Client & Workflow Smoke Test $\rightarrow$ `[COMPLETED]`
* **Task 1.4:** Đặc tả API Contract qua Swagger / OpenAPI, DTOs hệ tọa độ kép, Seed Master Data và Controller Mocks $\rightarrow$ `[COMPLETED]`

### 6.2. Danh mục 8 Bảng Cơ Sở Dữ Liệu & Mục Đích Nghiệp Vụ
1. `users`: Quản lý danh tính người dùng trong hệ sinh thái (Nông dân - `ROLE_USER`, Chuyên gia nông nghiệp - `ROLE_EXPERT`, Quản trị viên - `ROLE_ADMIN`).
2. `plants`: Danh mục cây trồng được hỗ trợ (Lúa, Cà chua, Ngô, Khoai tây...). Lưu mã định danh khoa học, tên phổ thông và biểu tượng hiển thị trên Mobile.
3. `diseases`: Danh mục chuẩn hóa các bệnh hại cây trồng với phân cấp mức độ nguy hại (`LOW`, `MODERATE`, `HIGH`, `CRITICAL`), mô tả triệu chứng học phục vụ tra cứu.
4. `plant_diseases`: Bảng nối quan hệ Many-to-Many giữa cây trồng và bệnh hại; đặc tả triệu chứng cụ thể trên từng loại cây (`specific_symptoms`) và mùa vụ bùng phát nguy cơ cao (`risk_season`).
5. `treatments`: Kho tri thức phác đồ điều trị và phòng ngừa theo từng bệnh/cây. Phân loại theo `BIOLOGICAL` (sinh học), `CHEMICAL` (hóa học), `PREVENTIVE` (phòng ngừa), `CULTURAL` (canh tác); lưu hoạt chất, liều lượng và thời gian cách ly an toàn (`safety_interval_days`).
6. `diagnoses`: Nhật ký các phiên chẩn đoán hình ảnh từ thiết bị di động. Lưu URL ảnh gốc (S3), kích thước ảnh, tọa độ GPS địa lý, thiết bị chụp, nhãn bệnh chiếm ưu thế, thời gian suy luận AI và toàn vẹn telemetry payload.
7. `bounding_boxes`: Tọa độ các hộp bao phát hiện từ mô hình Computer Vision (`x`, `y`, `width`, `height`), độ tin cậy và liên kết tới danh mục bệnh phục vụ Mobile vẽ SVG/Canvas đè lên ảnh.
8. `diagnosis_feedbacks`: Cơ chế phản hồi Human-in-the-loop. Cho phép nông dân đánh giá độ chính xác, đề xuất bệnh đúng, và chuyên gia nông nghiệp thẩm định (`expert_verified`) nhằm thu thập dữ liệu huấn luyện cải tiến mô hình AI (Active Learning).

### 6.3. Lý Do & Thủ Thuật Thiết Kế Nền Tảng (Architectural Decisions)
* **Khóa chính UUID (`gen_random_uuid()`):**
  * Cho phép ứng dụng di động tự sinh khóa chính ngoại tuyến (Offline ID Generation) trong Phase 2 & 3 mà không lo xung đột khi đồng bộ lên máy chủ trung tâm.
  * Ngăn chặn triệt để lỗ hổng vét cạn số thứ tự bản ghi (ID Enumeration Attacks) trên REST API.
  * Đảm bảo tính mở rộng cao (Scalability) khi phân mảnh hoặc gom dữ liệu phân tán.
* **Chiến lược Lưu trữ AI Kép (Dual AI Storage — Relational + JSONB):**
  * Bảng `bounding_boxes` (Relational): Chuẩn hóa quan hệ 1-N giúp truy vấn nhanh, join dữ liệu, lọc bounding box theo ngưỡng tin cậy và trả về định dạng nhẹ cho Mobile render.
  * Cột `raw_ai_response JSONB` kèm `GIN Index`: Lưu giữ toàn bộ payload phản hồi nguyên bản từ Roboflow Serverless/YOLO (active learning metadata, class logits, pipeline timing). Giúp hệ thống hấp thụ các bản nâng cấp mô hình AI trong tương lai mà không làm vỡ cấu trúc bảng vật lý.
* **Xóa mềm Dạng Bia Mộ (Soft Delete Tombstones — `deleted_at TIMESTAMPTZ`):**
  * Không xóa cứng dữ liệu (`hard delete`) để bảo vệ dữ liệu lịch sử dịch tễ và ảnh phục vụ tái huấn luyện mô hình.
  * Đóng vai trò bản ghi bia mộ (Tombstone Record) để Sync Engine (Phase 3) gửi danh sách ID đã xóa về các thiết bị ngoại tuyến trong quá trình đồng bộ vi sai (Differential Sync).
* **Khóa Lạc Quan (Optimistic Concurrency Control — `version BIGINT` + `@Version`):**
  * Ngăn chặn hiện tượng ghi đè mất dữ liệu (Lost Update Anomaly) khi nhiều chuyên gia hoặc tiến trình nền đồng thời cập nhật phản hồi chẩn đoán hoặc biên tập phác đồ điều trị.
* **Đánh Chỉ Mục Tường Minh Cho Khóa Ngoại (Explicit Foreign Key Indexing):**
  * PostgreSQL không tự động tạo index cho Foreign Key. Việc tạo B-Tree Index trên toàn bộ các cột FK giúp triệt tiêu nguy cơ nghẽn bảng (`ShareRowExclusiveLock`) khi bảng cha cập nhật/xóa và tăng tốc đột biến các câu lệnh `JOIN`.
* **Chỉ Mục Phức Hợp & Không Gian Địa Lý (Composite & Geospatial Indexing):**
  * `idx_diagnoses_user_created (user_id, created_at DESC WHERE deleted_at IS NULL)`: Tối ưu tuyệt đối cho endpoint lấy danh sách lịch sử chẩn đoán của nông dân mà không phải quét các bản ghi đã xóa mềm.
  * `idx_diagnoses_geo (latitude, longitude WHERE latitude IS NOT NULL)`: Nền tảng truy vấn bản đồ dịch tễ và cảnh báo dịch bệnh theo bán kính không gian trong Phase 3.

---

## 7. Đặc Tả Hợp Đồng Giao Tiếp API (API Contract Specification)

### 7.1. Danh Mục Endpoints Cốt Lõi (Sprint 1)
1. **`POST /api/v1/diagnoses/analyze` (Multipart Request)**:
   * **Mục đích:** Nhận ảnh lá chụp từ Mobile, thực hiện suy luận Computer Vision, trả về tọa độ Bounding Box và phác đồ điều trị tức thì.
   * **Input (Multipart/form-data):**
     * `image`: File ảnh (bắt buộc, JPEG/PNG/WebP, tối đa 15MB).
     * `plantCode`: Mã gợi ý cây trồng (tùy chọn, ví dụ `POTATO`, `TOMATO`).
     * `latitude`, `longitude`: Tọa độ GPS địa lý (tùy chọn).
     * `deviceInfo`: Chuỗi định danh thiết bị (tùy chọn, ví dụ `iPhone 15 Pro / iOS 17.5`).
   * **Output (JSON):** `DiagnosisAnalyzeResponse` chuẩn hóa bao gồm `dominantDisease`, `boundingBoxes`, `coOccurringDiseases`, `recommendedTreatments`.
2. **`GET /api/v1/diseases` & `GET /api/v1/diseases/{diseaseCode}`**:
   * **Mục đích:** Cung cấp bách khoa toàn thư bệnh hại phục vụ màn hình tra cứu, hiển thị triệu chứng học và danh mục phác đồ điều trị (sinh học, hóa học, canh tác).
3. **`GET /api/v1/plants` & `GET /api/v1/plants/{plantCode}`**:
   * **Mục đích:** Cung cấp danh mục cây trồng được hỗ trợ kèm biểu tượng và mô tả canh tác.
4. **`GET /swagger-ui.html` & `GET /v3/api-docs`**:
   * **Mục đích:** Giao diện Swagger UI tương tác trực tiếp ("Try it out") với đầy đủ Schema Models và ví dụ payload thực tế.

### 7.2. Các Quyết Định Kiến Trúc Hợp Đồng & Lý Do Kỹ Thuật (Contract Design Decisions)
* **Hệ Tọa Độ Kép Cho Bounding Box (`BoundingBoxDto`):**
  * *Tọa độ Pixel gốc:* `(x, y, width, height, xMin, yMin)` giữ nguyên tham chiếu kích thước ảnh gốc ($399 \times 501$).
  * *Tọa độ Chuẩn Hóa Top-Left $[0.0 - 1.0]$:* `(xMinNormalized, yMinNormalized, widthNormalized, heightNormalized)`.
  * *Lý do:* Mobile Client (React Native + SVG/Canvas của Huy) có thể nhân trực tiếp tọa độ chuẩn hóa với kích thước hiển thị trên màn hình (`left = xMinNormalized * screenWidth`, `top = yMinNormalized * screenHeight`) để vẽ khung bao chính xác mà không cần tính toán ma trận tỷ lệ hay bù trừ tâm trên thiết bị di động.
* **Thuật Toán Chọn Bệnh Chiếm Ưu Thế (Max Confidence Dominant Disease):**
  * Khi mô hình Computer Vision phát hiện nhiều hộp bao thuộc nhiều loại bệnh (hiện tượng đồng nhiễm - co-infection), Backend chọn hộp bao có độ tin cậy cao nhất (Max Confidence) làm `dominantDisease`.
  * Trả về danh mục phác đồ điều trị ưu tiên cho bệnh này, đồng thời đính kèm danh sách `coOccurringDiseases` ghi nhận các bệnh phụ cùng xuất hiện trong ảnh (ví dụ: phát hiện chính *Potato Late Blight* $79.9\%$, phát hiện phụ *Potato Early Blight* $52.0\%$).
* **Hệ Thống DTO Bất Biến (Java 21 `record`) & OpenAPI 3.0 Documentation:**
  * Sử dụng `record` đảm bảo tính bất biến (immutability), thread-safe và tối ưu hóa bộ nhớ khi phục vụ hàng loạt request đồng thời trên nền tảng Virtual Threads (Project Loom).
  * Gắn nhãn `@Schema(example = "...")` trên toàn bộ các trường để Swagger UI tự động tạo mẫu dữ liệu kiểm thử, giúp đội Mobile phát triển song song mà không phụ thuộc vào tiến độ triển khai hạ tầng đám mây.
* **Chiến Lược Kết Hợp Flyway Seed & Idempotent Seeder:**
  * Flyway script `V2__seed_master_data.sql`: Nạp sẵn dữ liệu danh mục 4 cây trồng, 5 bệnh hại chuẩn hóa Roboflow và các phác đồ điều trị vào PostgreSQL.
  * Spring Component `MasterDataSeeder`: Đảm bảo môi trường kiểm thử (H2 in-memory DB) hoặc bất kỳ instance mới nào cũng tự động có sẵn dữ liệu chuẩn để chạy unit/integration tests (`ApiContractTests`) mà không bị lỗi thiếu dữ liệu nền tảng.

---

## 8. Kiến Trúc Đóng Gói Container & Môi Trường Triển Khai (Containerization Architecture)

### 8.1. Đóng Gói Đa Tầng (Multi-Stage Build Pattern)
* **Stage 1 (Builder — `eclipse-temurin:21-jdk-alpine`):**
  * Tối ưu hóa bộ nhớ đệm (Layer Caching): Copy `pom.xml` trước, chạy `mvn dependency:go-offline` nhằm lưu cache toàn bộ dependencies mà không cần tải lại mỗi khi sửa code.
  * Cài đặt Maven trực tiếp từ Alpine package (`apk add --no-cache maven curl`), giúp build container ổn định độc lập tuyệt đối với wrapper của host Windows, đồng thời bổ sung script Unix shell chuẩn `server/mvnw` trong kho mã nguồn.
* **Stage 2 (Hardened Runtime — `eclipse-temurin:21-jre-alpine`):**
  * Chỉ sử dụng JRE siêu nhẹ, giảm kích thước image từ hàng gigabyte xuống dưới 200MB.
  * **Bảo mật Non-root (Principle of Least Privilege):** Tạo tài khoản người dùng riêng biệt `pdds:pdds` (UID/GID 1001), ngăn chặn triệt để nguy cơ leo thang đặc quyền container breakout.
  * **Tối ưu hóa JVM trên môi trường Container (Container Ergonomics):** Thiết lập cờ JVM `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom` giúp ứng dụng tận dụng bộ nhớ container linh hoạt mà không bị OOM Killer của Linux ngắt đột ngột.

### 8.2. Điều Phối Toàn Diện Hệ Thống (Docker Compose Orchestration)
* **Khởi chạy đồng bộ một lệnh duy nhất:** Dịch vụ `backend` được cấu hình trực tiếp vào `infra/docker-compose.yml`, kết nối qua mạng cô lập `eiu-pdds-network`.
* **Cơ chế phụ thuộc có điều kiện (Strict Conditional Dependency Chain):**
  * `postgres`: Yêu cầu `condition: service_healthy` (kiểm tra `pg_isready`) đảm bảo PostgreSQL sẵn sàng nhận kết nối trước khi Spring Boot chạy Flyway migration.
  * `redis`: Yêu cầu `condition: service_healthy` (kiểm tra `redis-cli ping`).
  * `minio`: Yêu cầu `condition: service_healthy` (kiểm tra `silo healthcheck ready`).
  * `minio-init`: Yêu cầu `condition: service_completed_successfully` đảm bảo bucket `eiu-pdds-plants` đã được tạo và thiết lập quyền đọc công khai thành công trước khi backend khởi động.
* **Healthcheck & Observability:** Tích hợp kiểm tra sức khỏe tự động qua endpoint Actuator `curl -f http://localhost:8080/actuator/health/liveness`, giúp Docker Compose nhận diện trạng thái sẵn sàng của backend theo thời gian thực.


