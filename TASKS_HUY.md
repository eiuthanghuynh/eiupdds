# CLIENT & FRONTEND SPRINT BACKLOG — HUY

> **Chỉ dẫn cho AI Agent:**
> Khi người dùng nhắc đến cụm từ **"Tôi là Huy"**, **"Huy đây"**, hoặc khai báo vai trò là **Huy**:
> 1. Tự động nhận diện vai trò: **Client Lead & Frontend Developer** (React Native - Expo Dev Client, Bounding Box Canvas, Next.js Web Admin, UI/UX).
> 2. Đọc file này để xác định Sprint hiện tại và các task chưa hoàn thành (`- [ ]`).
> 3. Tuyệt đối không can thiệp vào logic xử lý dữ liệu Backend Java/Spring Boot trừ khi đọc hiểu API Contract.
> 4. Khi implement xong mỗi task, tự động cập nhật trạng thái thành `- [x]`.

---

## Thông Tin Thành Viên & Tech Stack
* **Thành viên:** Huy
* **Vai trò:** Mobile App & Web Frontend Developer
* **Công nghệ cốt lõi:** React Native, Expo Dev Client (`expo-dev-client`), React Navigation, TypeScript, TailwindCSS / NativeWind, Next.js (Web Admin), React Native SVG / Canvas.
* **Workspace dự kiến:** `mobile/` (React Native) và `web-admin/` (Next.js)

---

## Sprint 1: Khởi Tạo Ứng Dụng Mobile & Giao Diện Chụp Ảnh (Tuần 1 - 2)
**Mục tiêu Sprint:** Dựng app Mobile với Expo Dev Client, điều hướng màn hình và mở được Camera chụp ảnh.

- [ ] **Task 1.1: Khởi tạo Project Mobile với Expo Dev Client** (4h)
  - Khởi tạo project React Native với Expo TypeScript template.
  - Cài đặt `expo-dev-client` và cấu hình build APK debug nội bộ (chuẩn bị sẵn đường cho Phase 2 On-device AI).
  - Cài đặt thư viện giao diện NativeWind / Tailwind hoặc StyleSheet chuẩn hóa theme màu nông nghiệp (xanh lá cây/trắng).
  - *Acceptance Criteria:* Chạy `npx expo start` và cài đặt được app lên thiết bị thật/giả lập qua `expo-dev-client`.

- [ ] **Task 1.2: Thiết lập Luồng Điều Hướng (React Navigation)** (4h)
  - Cài đặt `@react-navigation/native` và `@react-navigation/bottom-tabs` (hoặc stack).
  - Dựng 3 tab chính: Chẩn đoán (Home/Scanner), Lịch sử (History), Cài đặt/Hỗ trợ (Settings).
  - *Acceptance Criteria:* Chuyển tab mượt mà, giữ trạng thái hiển thị chuẩn.

- [ ] **Task 1.3: Màn Hình Chụp Ảnh Lá Cây (Camera & Gallery)** (8h)
  - Cài đặt `expo-camera` và `expo-image-picker`.
  - Xin quyền truy cập Camera và Thư viện ảnh (xử lý từ chối quyền người dùng).
  - Thiết kế khung ngắm định vị lá cây (Overlay Viewfinder) giúp nông dân canh đúng vùng bệnh.
  - Cho phép xem trước (Preview) ảnh đã chụp trước khi xác nhận gửi đi chẩn đoán.
  - *Acceptance Criteria:* Chụp được ảnh từ camera hoặc chọn ảnh từ thư viện, hiển thị rõ ràng trên màn hình preview.

- [ ] **Task 1.4: Tạo API Service Client & Mock Data** (4h)
  - Cài đặt `axios` hoặc `fetch` client bọc trong service `ApiClient`.
  - Đọc tài liệu Swagger của Thắng (`/api/v1/diagnoses/analyze`) và tạo mock response để code giao diện kết quả.
  - *Acceptance Criteria:* Nút "Chẩn đoán" gọi mock data và nhảy sang màn hình kết quả mẫu.

---

## Sprint 2: Core MVP — Hiển Thị Bounding Box & Phác Đồ (Tuần 3 - 4)
**Mục tiêu Sprint:** Tải ảnh lên Backend, nhận diện vết bệnh và vẽ khung Bounding Box chuẩn xác đè lên ảnh.

- [ ] **Task 2.1: Module Thuật Toán Scale & Vẽ Bounding Box (Visual Overlay)** (10h)
  - Cài đặt thư viện vẽ vector `react-native-svg`.
  - Viết giải thuật chuyển đổi tọa độ chuẩn hóa `[x_center, y_center, width, height]` sang pixel thực tế theo kích thước màn hình hiển thị (Aspect Ratio Scaling).
  - Vẽ khung chữ nhật bo góc kèm nhãn bệnh và % tin cậy nổi bật ngay trên đốm lá bị bệnh.
  - *Acceptance Criteria:* Bounding Box ôm khít vết bệnh trên ảnh dù xoay ngang hay xoay dọc màn hình.

- [ ] **Task 2.2: Tích Hợp Gửi Ảnh Lên Backend Thắng** (6h)
  - Đóng gói ảnh dưới dạng `FormData` (Multipart) gửi tới endpoint `POST /api/v1/diagnoses/analyze`.
  - Thêm hiệu ứng Skeleton / Loading spinner xoay vòng trong lúc chờ phản hồi (3-5 giây).
  - Lấy tọa độ GPS thiết bị qua `expo-location` đính kèm vào payload gửi đi.
  - *Acceptance Criteria:* Bấm "Gửi", ảnh được đẩy lên Backend và nhận về tọa độ thật từ Roboflow để vẽ lên màn hình.

- [ ] **Task 2.3: Màn Hình Kết Quả Chẩn Đoán Chi Tiết** (6h)
  - Hiển thị tên bệnh (tiếng Việt), độ tin cậy (Confidence score).
  - Nút bấm xem nhanh "Phác đồ điều trị khẩn cấp" (sơ cứu cách ly, thuốc đề xuất).
  - *Acceptance Criteria:* Người dùng nhìn thấy rõ ràng lá cây bị bệnh gì và cần làm gì ngay lập tức.

---

## Sprint 3: Lịch Sử Chẩn Đoán & Khởi Tạo Web Admin Next.js (Tuần 5 - 6)
**Mục tiêu Sprint:** Nông dân xem lại được lịch sử khám bệnh trên Mobile; Khởi tạo Web Admin quản trị cho chuyên gia.

- [ ] **Task 3.1: Màn Hình Lịch Sử Khám Bệnh Trên Mobile** (8h)
  - Viết màn hình danh sách lịch sử chẩn đoán (`FlatList` tối ưu hiệu năng).
  - Hiển thị ảnh thu nhỏ (thumbnail), ngày giờ chụp, tên bệnh và nhãn mức độ nguy hiểm (Đỏ: Nghiêm trọng, Vàng: Trung bình, Xanh: Nhẹ).
  - Bấm vào một dòng để mở xem lại chi tiết hình ảnh kèm khung Bounding Box đã lưu.
  - *Acceptance Criteria:* Lấy dữ liệu từ `GET /api/v1/diagnoses` và hiển thị mượt mà, có tính năng kéo để tải lại (Pull-to-refresh).

- [ ] **Task 3.2: Khởi Tạo Web Admin Next.js (App Router)** (6h)
  - Tạo project Next.js 14+ với TailwindCSS và component library (shadcn/ui hoặc Ant Design).
  - Dựng layout quản trị viên: Sidebar điều hướng, Header hiển thị thông tin tài khoản.
  - *Acceptance Criteria:* Khởi chạy `npm run dev` hiển thị giao diện quản trị hiện đại, chuẩn responsive.

- [ ] **Task 3.3: Trang Web Quản Lý Danh Mục Bệnh Cây** (8h)
  - Tạo trang danh sách bệnh dạng Bảng (Table): Tên bệnh, tên khoa học, loài cây, mức độ nguy hiểm.
  - Form thêm mới / chỉnh sửa thông tin bệnh kèm upload ảnh mẫu.
  - *Acceptance Criteria:* Gọi thành công các API CRUD `/api/v1/diseases` của Thắng.

---

## Sprint 4: Hoàn Thiện Web Admin & Xử Lý Ngoại Lệ Mobile (Tuần 7 - 8)
**Mục tiêu Sprint:** Nâng cấp trải nghiệm người dùng, chống crash app khi mất mạng và hoàn thiện Web Admin.

- [ ] **Task 4.1: Xử Lý Ngoại Lệ & Trạng Thái Mất Mạng Trên Mobile** (8h)
  - Cài đặt `@react-native-community/netinfo` kiểm tra kết nối Internet.
  - Hiển thị thông báo thân thiện khi mất mạng: "Vui lòng kết nối Internet để thực hiện chẩn đoán".
  - Bắt lỗi timeout API (> 15 giây) và hiển thị nút "Thử lại".
  - *Acceptance Criteria:* Tắt Wi-Fi/4G app không bị crash màn hình trắng, hiển thị thông báo rõ ràng cho người dùng.

- [ ] **Task 4.2: Trang Web Quản Lý Phác Đồ Điều Trị (Next.js)** (8h)
  - Dựng giao diện chỉnh sửa phác đồ điều trị cho từng loại bệnh (Biện pháp sinh học, Biện pháp hóa học, Cách phòng ngừa).
  - Tích hợp trình soạn thảo văn bản phong phú (Rich Text Editor hoặc Markdown Editor).
  - *Acceptance Criteria:* Chuyên gia có thể nhập và cập nhật phác đồ điều trị hiển thị xuống Mobile.

- [ ] **Task 4.3: Trang Web Thống Kê & Bản Đồ Lịch Sử Khám Bệnh** (6h)
  - Dựng biểu đồ thống kê các loại bệnh phổ biến nhất (dùng Chart.js hoặc Recharts).
  - Tích hợp Leaflet / Mapbox hiển thị vị trí các ca bệnh theo tọa độ GPS nông dân gửi lên.
  - *Acceptance Criteria:* Bản đồ ghim được các vị trí phát hiện bệnh trên cả nước.

---

## Sprint 5: Nghiệm Thu, Tài Liệu Đồ Án & Đóng Gói Demo (Tuần 9 - 10)
**Mục tiêu Sprint:** Hoàn thiện báo cáo, quay video demo chuyên nghiệp và chạy thử toàn hệ thống.

- [ ] **Task 5.1: Build File Cài Đặt Ứng Dụng (APK Debug/Release)** (6h)
  - Cấu hình EAS Build hoặc chạy lệnh `npx expo run:android --variant release` xuất file APK hoàn chỉnh.
  - Cài đặt file APK lên 2 điện thoại Android thật khác nhau để kiểm tra độ tương thích màn hình.
  - *Acceptance Criteria:* File APK cài và mở được trên điện thoại thật không cần cắm dây cáp máy tính.

- [ ] **Task 5.2: Viết Tài Liệu Kỹ Thuật Đồ Án (Chương Thiết Kế UI/UX & Client)** (10h)
  - Chụp ảnh toàn bộ flow ứng dụng Mobile và Web Admin.
  - Vẽ sơ đồ điều hướng màn hình (Screen Flow / Wireflow).
  - Viết tài liệu giải thích thuật toán scale Bounding Box hiển thị.
  - *Acceptance Criteria:* Hoàn thành toàn bộ nội dung chương giao diện nộp cho giảng viên.

- [ ] **Task 5.3: Chuẩn Bị Slide & Quay Video Demo Dự Phòng** (4h)
  - Thiết kế slide thuyết trình bảo vệ đồ án (Tập trung vào vấn đề nông nghiệp, giải pháp kỹ thuật, demo).
  - Quay 1 video Full HD quay lại cảnh cầm điện thoại soi lá cây và màn hình hiển thị kết quả (dùng khi phòng thi mất mạng).
  - *Acceptance Criteria:* Slide và video hoàn chỉnh sẵn sàng cho buổi bảo vệ.
