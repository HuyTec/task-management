# Review Phase 0 - Workflow, Authorization và Query Safety

## 1. Mục tiêu

Batch refactor này xử lý các bug và điểm yếu đã được liệt kê trong
`task-management-review-summary.docx` trước khi mở rộng hệ thống sang milestone,
project lifecycle, blocker và activity log.

Phạm vi được giữ hẹp:

- bảo vệ tính đúng đắn của task review workflow;
- ngăn self-review;
- authorize project nguồn và project đích trước khi mutate task;
- invalidation cache cho đúng toàn bộ audience;
- loại bỏ việc tái sử dụng và mutate Criteria subquery;
- trả lỗi xác thực/Redis có chủ đích;
- bổ sung regression và integration tests;
- cập nhật tài liệu Expense API và dọn comment/TODO nháp trong phạm vi liên quan.

Batch này không thêm `ProjectStatus`, `Milestone`, voting hoặc `ActivityLog`.
Những feature đó nên bắt đầu sau khi các invariant dưới đây ổn định.

## 2. Các invariant sau refactor

### 2.1 Review round

Khi reviewer yêu cầu sửa:

```text
IN_REVIEW
  -> requestChanges
  -> mọi acceptance criterion được reset về false
  -> CHANGES_REQUESTED
```

Lý do: approval của vòng trước không được áp dụng cho phiên bản công việc đã sửa.
Nếu không reset, lần submit tiếp theo có thể đi thẳng qua `approve()` bằng các giá
trị `satisfied=true` cũ.

### 2.2 Independent review

Reviewer hợp lệ phải đồng thời thỏa mãn:

```text
role thuộc {OWNER, MANAGER}
AND reviewer.projectMemberId != activeAssignment.assignee.projectMemberId
```

Rule này được áp dụng cho cả `approve()` và `requestChanges()`. OWNER/MANAGER vẫn
có thể nhận task trong nhóm nhỏ, nhưng không được tự đánh giá task của mình.

### 2.3 Assignment và criteria contract

- Claim chỉ được release khi task còn `TODO`.
- Khi work đã start, việc đổi assignee thuộc trách nhiệm OWNER/MANAGER.
- OWNER và MANAGER có thể là assignee; VIEWER không thể là assignee.
- Nội dung/thứ tự acceptance criteria chỉ được sửa khi task còn `TODO`.
- Trong `IN_REVIEW`, reviewer chỉ thay đổi trạng thái `satisfied`.

Các TODO nghiệp vụ cũ đã được thay bằng contract rõ ràng và regression tests.

### 2.4 Project transfer authorization

Đổi `projectId` của task cần hai phép kiểm tra độc lập:

```text
có quyền quản lý task trong project nguồn
AND
có OWNER/MANAGER membership trong project đích
```

Việc chỉ có quyền đọc project đích (`MEMBER` hoặc `VIEWER`) không đủ để chuyển
task vào project đó. Authorization project đích được thực hiện trước khi thay đổi
bất kỳ field nào của task.

Task đã có assignment, criteria hoặc review history vẫn không được chuyển/unlink
bằng PATCH thông thường vì các bản ghi workflow tham chiếu thành viên project cũ.

### 2.5 Cache audience

Task project có thể được cache riêng cho nhiều user bằng key chứa `userId`.
Vì vậy update/delete/unlink phải evict cache của:

- người đang thao tác;
- người tạo task;
- mọi thành viên của project cũ;
- mọi thành viên của project mới khi chuyển project.

Chỉ evict cache của current user sẽ để thành viên khác nhìn thấy dữ liệu cũ.

### 2.6 Authentication boundary

`SecurityUtils.getCurrentUser()` không còn cast principal trực tiếp. Method này
từ chối các trường hợp:

- không có Authentication;
- Authentication chưa authenticated;
- principal không phải `CustomUserDetails`.

Các trường hợp trên được chuyển thành HTTP 401 thay vì NPE/ClassCastException và
HTTP 500.

`AuthSessionService.exists()` cũng chuyển lỗi Redis thành
`AuthenticationStoreUnavailableException`. `JwtFilter` bắt exception này tại
filter boundary và fail closed bằng HTTP 503.

## 3. Refactor TaskSpecifications

### Trước đây

Một `membershipQuery` mutable được tạo trước rồi gọi `where()` lần thứ hai trong
nhánh `REVIEW_QUEUE`. Lần gọi sau thay restriction cũ; code chỉ đúng vì nó lặp lại
các điều kiện project/user.

### Hiện tại

Mỗi mục đích tạo subquery riêng:

```text
hasActiveAssignment   -> MY_WORK
hasProjectMembership  -> ALL_ACCESSIBLE
hasReviewAccess       -> REVIEW_QUEUE
```

Không branch nào chia sẻ hoặc ghi đè state của subquery khác. Integration test
chạy query thật trên H2 và kiểm tra:

- `MY_WORK` chỉ gồm personal task và active assigned task;
- `ALL_ACCESSIBLE` gồm personal task và mọi task thuộc project có membership;
- `REVIEW_QUEUE` chỉ gồm task `IN_REVIEW` đối với OWNER/MANAGER;
- MEMBER không nhìn thấy review queue.

H2 test xác minh Criteria/JPA behavior nhanh trong build. Flyway và PostgreSQL
production constraints vẫn cần được kiểm tra riêng khi có migration mới.

## 4. Đối chiếu các điểm trong tài liệu review

| Điểm | Kết quả |
|---|---|
| #1 Reset criteria | Đã sửa và có regression test |
| #2 Mutable membership subquery | Đã tách thành ba subquery độc lập và có integration test |
| #3 SecurityUtils cast thô | Đã guard principal và map lỗi thành 401 |
| #4 Unlink/IDOR audit | Source authorization giữ nguyên; target project yêu cầu OWNER/MANAGER |
| #5 Self-review | Đã chặn cho approve và request changes |
| #6 Release claim sau start | Đã chốt: không cho phép; có test |
| #7 OWNER/MANAGER làm assignee | Đã chốt: cho phép, VIEWER bị cấm |
| #8 Sửa criteria sau TODO | Đã chốt: freeze content/order |
| #9 README Expense | Đã cập nhật endpoint thực tế |
| #10 Comment nháp | Đã dọn trong auth/task scope |
| #11 Test coverage | Đã mở rộng workflow, authorization, Redis, SecurityUtils và specifications |

Ngoài danh sách gốc, cache invalidation đa thành viên và quyền trên project đích
cũng đã được sửa vì chúng nằm trên cùng mutation flow.

## 5. Test matrix quan trọng

### Workflow

- MEMBER claim task `TODO` thành công.
- Không thể có assignment active thứ hai.
- VIEWER không thể được assign.
- Assignee có thể submit review.
- `requestChanges()` reset toàn bộ criteria.
- Assignee không thể tự approve.
- Assignee không thể tự request changes.
- Claim không thể release sau khi work đã start.

### Authorization và cache

- MEMBER của project đích không thể chuyển task vào project đó.
- OWNER/MANAGER có thể tạo và quản lý project task.
- Update project task evict cache của mọi project member.
- Read authorization vẫn chạy trước cache lookup.

### Session và authentication

- Redis rotate trả `true` khi compare-and-set thành công.
- Stale refresh token trả `false`.
- Redis outage ở create/rotate/exists fail closed.
- Missing hoặc unexpected principal bị từ chối sạch.

## 6. Cách tự review code

Đối với mỗi command service, review theo thứ tự sau:

1. Identity có lấy từ Security Context thay vì request payload không?
2. Resource có được load theo identifier chính xác không?
3. Authorization có chạy trước cache và trước mutation không?
4. State transition có kiểm tra trạng thái nguồn không?
5. Các entity liên quan có được cập nhật trong cùng transaction không?
6. Cache audience nào đã từng đọc resource này?
7. Có regression test cho cả đường thành công và đường bị từ chối không?

Đối với query authorization:

1. Viết tập kết quả mong muốn cho từng role/workspace.
2. Không tái sử dụng Criteria object mutable giữa các branch.
3. Chạy integration test với dữ liệu positive và negative.
4. Kiểm tra generated SQL khi query có subquery hoặc join quyền truy cập.

## 7. Lệnh xác minh

PowerShell:

```powershell
cd backend
./mvnw.cmd test
cd ../frontend
npm.cmd run lint
npm.cmd run build
cd ..
git diff --check
```

Nếu Maven Wrapper gặp lỗi trong môi trường sandbox, cần chạy Maven distribution
đã cache với `MAVEN_USER_HOME` trỏ về thư mục `.m2` của người dùng. Đây là giới
hạn môi trường kiểm thử, không phải lỗi production code.

## 8. Bước đổi mới tiếp theo

Sau khi review và commit Phase 0, slice tiếp theo nên chỉ thêm explicit
`ProjectStatus`:

```text
PLANNING -> ACTIVE -> COMPLETED -> ARCHIVED
              |
              -> ON_HOLD
```

Không nên thêm đồng thời ProjectStatus, Milestone, voting và ActivityLog trong
một migration. Mỗi slice cần migration riêng, API contract riêng và test riêng.
