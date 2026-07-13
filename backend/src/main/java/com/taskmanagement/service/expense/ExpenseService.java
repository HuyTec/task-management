package com.taskmanagement.service.expense;

import java.util.List;

import org.springframework.stereotype.Service;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.expense.ExpenseResponse;
import com.taskmanagement.dto.task.CreateTaskRequest;
import com.taskmanagement.dto.task.TaskResponse;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.mapper.ExpenseMapper;
import com.taskmanagement.model.Expense;
import com.taskmanagement.model.TaskStatus;
import com.taskmanagement.model.User;
import com.taskmanagement.repository.ExpenseRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.CustomUserDetails;
import com.taskmanagement.utils.SecurityUtils;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseMapper expenseMapper;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    public ExpenseService(ExpenseRepository expenseRepository, ExpenseMapper expenseMapper, SecurityUtils securityUtils, UserRepository userRepository) {
       this.expenseMapper=expenseMapper;
       this.expenseRepository = expenseRepository;
       this.securityUtils = securityUtils;
       this.userRepository = userRepository;
    }

    private Expense ensureExpenseAvailable(Long userId, Long id) {
        return expenseRepository.findByIdAndUserId(id, userId).orElseThrow(() -> new ResourceNotFoundException("Expense not found!"));

    /**
     * Tạo expense mới.
     * Câu hỏi cần trả lời trước khi viết:
     * - request có field taskId (nullable) không? Nếu có, taskId != null thì cần load Task
     *   và set vào expense — nhưng KHÔNG dùng ensureTaskAvailable() (đó là ownership check
     *   để USER thao tác trên Task của họ; ở đây bạn chỉ cần biết Task có tồn tại không,
     *   và nó có thuộc về user đang tạo expense không — suy nghĩ xem có nên tái dùng
     *   ensureTaskAvailable() hay viết check riêng).
     * - amount là kiểu gì trong DTO? (nhớ lại: đã ghi nhận Double là code smell cho tiền tệ,
     *   nhưng project đang dùng Double xuyên suốt — giữ nhất quán hay đổi sang BigDecimal?
     *   Đây là quyết định bạn cần tự chốt, không phải tôi.)
     */
    public Response<ExpenseResponse> createExpense(CreateExpenseRequest request) {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();

        Expense expense = expenseMapper.toExpense(request);
        
        User user = userRepository.findById(currentUser.getId()).orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        expense.setUser(user);
        expenseRepository.save(expense);

        ExpenseResponse response = expenseMapper.toExpenseResponse(expense);
        return Response.success(response, "Expense created successfully!");
    }

    /**
     * Lấy expense của chính user hiện tại.
     * Câu hỏi: có cần phân trang (pagination) ngay bây giờ không, hay danh sách đơn giản
     * List<ExpenseResponse> là đủ ở giai đoạn này? (gợi ý: YAGNI — bạn tự quyết)
     */
    public Response<List<ExpenseResponse>> getMyExpenses() {
        // TODO
        return null;
    }

    /**
     * Xem chi tiết 1 expense — dùng ensureExpenseAvailable() ở trên.
     */
    public Response<ExpenseResponse> getExpenseById(Long id) {
        // TODO
        return null;
    }

    /**
     * Cập nhật expense — nhớ lại pattern updateTask(): chỉ set field nào != null trong request
     * (partial update), dùng ensureExpenseAvailable() để lấy entity trước.
     */
    public Response<ExpenseResponse> updateExpense(Long id /* TODO: UpdateExpenseRequest request */) {
        // TODO
        return null;
    }

    /**
     * Xóa expense — dùng ensureExpenseAvailable() trước khi xóa.
     * Câu hỏi: nếu expense này đang gắn với 1 Task, xóa expense có ảnh hưởng gì đến
     * cache "total" của Task đó không? (liên quan đến phần Redis cache sắp làm —
     * chỉ cần NHỚ, chưa cần xử lý ngay)
     */
    public Response<Void> deleteExpense(Long id) {
        // TODO
        return null;
    }
}
}