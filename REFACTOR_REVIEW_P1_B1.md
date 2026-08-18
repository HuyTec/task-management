# Review nhanh P1-B1 — Project Lifecycle

## 1. Thông tin batch

- Phase: `Phase 1 — Project Lifecycle`
- Batch: `P1-B1 — ProjectStatus enum + Flyway migration + lifecycle commands`
- Branch: `feature/project-lifecycle`
- Base commit Phase 0: `c4aec89`
- Trạng thái P1-B1: đã triển khai, chưa commit để chờ review
- Baseline trước batch: 49 tests pass
- Sau batch: 84 tests pass, 0 failures, 0 errors

## 2. Mục tiêu

Chuyển Project từ mô hình CRUD có thể chỉnh dữ liệu trực tiếp sang mô hình có
vòng đời được backend kiểm soát.

```text
PLANNING ──activate──> ACTIVE
ACTIVE ──hold───────> ON_HOLD
ON_HOLD ──resume────> ACTIVE
ACTIVE ──complete───> COMPLETED
COMPLETED ─archive──> ARCHIVED
```

Mọi transition phải đi qua command service. Frontend/client không được PATCH
trực tiếp `status`.

## 3. Những điểm kỹ thuật mới

### 3.1 State machine

`ProjectLifecycleService` khai báo tập transition hợp lệ bằng
`Map<ProjectStatus, Set<ProjectStatus>>`.

Backend kiểm tra đồng thời:

1. Trạng thái hiện tại đúng với command.
2. Trạng thái đích nằm trong state machine.
3. Người dùng có role phù hợp.
4. Invariant riêng của transition đã được thỏa mãn.

`activate` và `resume` cùng chuyển đến `ACTIVE`, nhưng không thể dùng thay nhau:

- `activate`: chỉ từ `PLANNING`.
- `resume`: chỉ từ `ON_HOLD`.

### 3.2 Command endpoint

```text
POST /api/projects/{projectId}/activate
POST /api/projects/{projectId}/hold
POST /api/projects/{projectId}/resume
POST /api/projects/{projectId}/complete
POST /api/projects/{projectId}/archive
```

Controller không nhận `ProjectStatus` từ request và không tự sửa entity. Nó chỉ
chuyển command sang `ProjectLifecycleService`.

### 3.3 Enum persistence

Project lưu status bằng:

```java
@Enumerated(EnumType.STRING)
private ProjectStatus status = ProjectStatus.PLANNING;
```

Không dùng `ORDINAL`, vì thay đổi thứ tự enum có thể làm dữ liệu cũ bị hiểu sai.

### 3.4 Migration Expand → Backfill → Contract

Migration V7 thực hiện theo thứ tự:

```text
Thêm cột nullable
→ backfill dữ liệu cũ
→ đặt default PLANNING
→ set NOT NULL
→ thêm check constraint
```

Rule backfill:

- Project có ít nhất một task và tất cả task `DONE` → `COMPLETED`.
- Project rỗng hoặc còn task chưa `DONE` → `ACTIVE`.

Migration mới chỉ được review tĩnh, chưa chạy trên PostgreSQL thật.

**Phải backup database trước khi chạy Flyway V7.**

### 3.5 Completion invariant

`complete()` chỉ thành công khi:

```text
projectTaskCount >= 1
AND incompleteTaskCount == 0
```

Mọi status khác `DONE`, gồm `TODO`, `IN_PROGRESS`, `IN_REVIEW` và
`CHANGES_REQUESTED`, đều chặn hoàn thành Project.

### 3.6 Authorization

| Command | Role được phép |
|---|---|
| activate | OWNER, MANAGER |
| hold | OWNER, MANAGER |
| resume | OWNER, MANAGER |
| complete | OWNER, MANAGER |
| archive | OWNER |

MEMBER và VIEWER không được thay đổi lifecycle.

### 3.7 Hard-delete invariant

`deleteProject()` chỉ cho phép hard-delete khi Project đang `PLANNING`.

Project ở `ACTIVE`, `ON_HOLD`, `COMPLETED` hoặc `ARCHIVED` phải bị từ chối.
Mục tiêu là giữ lại dữ liệu đã phát sinh lịch sử nghiệp vụ.

## 4. File đã thay đổi

| File | Thay đổi |
|---|---|
| `backend/src/main/java/com/taskmanagement/model/ProjectStatus.java` | Enum gồm 5 trạng thái lifecycle |
| `backend/src/main/java/com/taskmanagement/model/Project.java` | Thêm field `status`, mặc định `PLANNING` |
| `backend/src/main/resources/db/migration/V7__add_project_status.sql` | Migration và conditional backfill |
| `backend/src/main/java/com/taskmanagement/service/ProjectLifecycleService.java` | State machine, authorization và completion invariant |
| `backend/src/main/java/com/taskmanagement/controller/ProjectLifecycleController.java` | 5 command endpoints |
| `backend/src/main/java/com/taskmanagement/service/project/ProjectService.java` | Chặn hard-delete ngoài `PLANNING` |
| `backend/src/test/java/com/taskmanagement/service/ProjectLifecycleServiceTest.java` | Test transition, role và complete invariant |
| `backend/src/test/java/com/taskmanagement/controller/ProjectLifecycleControllerTest.java` | Test 5 endpoint delegate đúng command |
| `backend/src/test/java/com/taskmanagement/service/ProjectServiceTest.java` | Test 4 trạng thái không được hard-delete |

## 5. Lộ trình review trong 15 phút

### Bước 1 — Migration, 3 phút

Mở `V7__add_project_status.sql` và xác nhận:

- Cột được thêm nullable trước.
- Backfill có kiểm tra tồn tại task.
- Chỉ Project có task và toàn bộ task `DONE` mới thành `COMPLETED`.
- `NOT NULL` được đặt sau backfill.
- Có default `PLANNING` và check constraint.

### Bước 2 — Enum và entity, 1 phút

Kiểm tra:

```java
@Enumerated(EnumType.STRING)
private ProjectStatus status = ProjectStatus.PLANNING;
```

### Bước 3 — State machine, 5 phút

Đọc `ProjectLifecycleService` và tập trung vào:

- `ALLOWED_TRANSITIONS`.
- Source status bắt buộc của từng command.
- OWNER/MANAGER authorization.
- OWNER-only archive.
- Hai phép đếm task trong `complete()`.
- Không `save()` nếu validation thất bại.

### Bước 4 — Controller, 2 phút

Xác nhận controller chỉ delegate và không nhận status tùy ý từ client.

### Bước 5 — Hard-delete, 1 phút

Kiểm tra `ProjectService.deleteProject()` reject mọi status khác `PLANNING`.

### Bước 6 — Test, 3 phút

Không cần đọc toàn bộ Mockito setup. Chỉ đọc tên test và assertion chính:

- 5 transition hợp lệ.
- Toàn bộ transition bất hợp lệ có command tương ứng.
- Không dùng activate/resume thay nhau.
- Project rỗng không thể complete.
- Có task chưa DONE không thể complete.
- MEMBER không transition.
- MANAGER không archive.
- Project ngoài PLANNING không hard-delete.
- 5 endpoint delegate đúng service method.

## 6. Checklist phê duyệt

- [ ] Enum đủ 5 trạng thái và dùng `EnumType.STRING`.
- [ ] Migration theo thứ tự nullable → backfill → `NOT NULL`.
- [ ] Backfill dữ liệu cũ có điều kiện.
- [ ] Project mới mặc định `PLANNING`.
- [ ] Không có endpoint PATCH status.
- [ ] State machine reject transition ngoài sơ đồ.
- [ ] `activate` và `resume` không dùng thay nhau.
- [ ] OWNER/MANAGER được vận hành lifecycle.
- [ ] Chỉ OWNER được archive.
- [ ] `complete()` yêu cầu ít nhất một task và tất cả `DONE`.
- [ ] Project ngoài `PLANNING` không hard-delete.
- [ ] Backend test đạt 84/84.
- [ ] Đã backup database trước khi chạy Flyway V7.

## 7. Giả định và rủi ro

- Transition/delete sai trạng thái dùng `BadRequestException` hiện có và trả
  HTTP 400. Batch không tạo exception 409 mới.
- Chưa có optimistic locking; hai manager transition đồng thời vẫn có nguy cơ
  race condition.
- Chưa chạy migration trên PostgreSQL thật và chưa kiểm tra dữ liệu bất thường
  trong database hiện tại.
- `ProjectResponse` chưa trả status vì DTO/frontend ngoài scope P1-B1.
- TaskService chưa ngăn tạo hoặc sửa task sau khi Project đã `COMPLETED` hoặc
  `ARCHIVED`.
- `updateProject()` vẫn có thể sửa metadata của Project đã hoàn thành/archive.
- `deleteProject()` hiện truy vấn task theo project và user hiện tại. Project
  PLANNING có task của thành viên khác cần được review trong batch riêng.

## 8. Việc chưa làm

- Chưa thêm `TaskStatus.CANCELLED`.
- Task bị bỏ dở vĩnh viễn sẽ chặn `complete()`. Đây là giới hạn đã biết trước,
  không phải bug. Đưa vào backlog Phase 2 hoặc batch riêng thêm `CANCELLED`.
- Chưa expose status qua `ProjectResponse`.
- Chưa sửa frontend.
- Chưa làm `ProjectMode`, Milestone, Activity Log hoặc Voting.
- Chưa enforce Project status trong các Task command.
- Chưa thêm integration test Flyway V7 trên PostgreSQL.
- Chưa chạy migration lên database thật.
- Chưa commit P1-B1 để giữ diff cho bước review.

## 9. Kết quả verification

```text
Tests run: 84
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

`git diff --check` không phát hiện lỗi whitespace. Các cảnh báo còn lại là cảnh
báo LF/CRLF trên Windows, duplicate `org.json.JSONObject` trong test classpath
và Mockito dynamic agent; chúng không làm test thất bại và không được sửa trong
batch này.
