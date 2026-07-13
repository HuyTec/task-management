package com.taskmanagement.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
// TODO: import @PreAuthorize nếu cần (nhớ lại: getAllTask cần @PreAuthorize("hasRole('ADMIN')")
// — Expense có cần một endpoint admin-only tương tự không, hay Expense hoàn toàn không có
// khái niệm "xem tất cả" vì nó gắn chặt với privacy hơn cả Task?)
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import java.util.List;

import com.taskmanagement.service.expense.ExpenseService;
// TODO: import DTO cần thiết (Response, ExpenseResponse, CreateExpenseRequest, UpdateExpenseRequest)

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    // Câu hỏi trước khi viết endpoint đầu tiên:
    // Expense có cần GET /api/expenses (admin xem tất cả) giống Task không?
    // Hay vì Expense nhạy cảm hơn (thông tin chi tiêu cá nhân), bạn quyết định
    // KHÔNG có endpoint admin-only nào cho Expense cả — chỉ có /me?
    // Đây là quyết định kiến trúc bạn cần tự chốt trước khi viết route này.

    // TODO: GET /me — lấy expense của chính mình (giống getMyTasks())

    // TODO: GET /{id} — xem chi tiết 1 expense (ownership check trong Service)

    // TODO: POST — tạo expense mới

    // TODO: PATCH /{id} — cập nhật expense

    // TODO: DELETE /{id} — xóa expense

    // Gợi ý format 1 method mẫu (bạn tự áp dụng cho các method còn lại,
    // đối chiếu với TaskController bạn đã viết trước đó):
    //
    // @GetMapping("/{id}")
    // public ResponseEntity<Response<ExpenseResponse>> getExpenseById(@PathVariable @Positive Long id) {
    //     return ResponseEntity.ok(expenseService.getExpenseById(id));
    // }
}