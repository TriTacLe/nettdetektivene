package ntnu.no.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.task.TaskRequestDTO;
import ntnu.no.backend.dto.task.TaskResponseDTO;
import ntnu.no.backend.service.TaskService;
import ntnu.no.backend.shared.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for task endpoints.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Endpoints for tasks")
public class TaskController {

  private final TaskService taskService;

  /**
   * Returns all tasks.
   *
   * @return 200 OK with list of tasks
   */
  @GetMapping
  @Operation(summary = "Get all tasks")
  public ResponseEntity<ApiResponse<List<TaskResponseDTO>>> getAllTasks() {
    return ResponseEntity.ok(ApiResponse.ok(taskService.getAllTask()));
  }

  /**
   * Returns a single task by ID.
   *
   * @param id the task ID
   * @return 200 OK with the task
   */
  @GetMapping("/{id}")
  @Operation(summary = "Get task by ID")
  public ResponseEntity<ApiResponse<TaskResponseDTO>> getTask(@PathVariable UUID id) {
    return ResponseEntity.ok(ApiResponse.ok(taskService.getTask(id)));
  }

  /**
   * Creates a new task. Only accessible by instructors and admins.
   *
   * @param dto the request body
   * @return 201 Created with the created task
   */
  @PostMapping
  @Operation(summary = "Create a new task")
  @PreAuthorize("hasAnyRole('instructor', 'inst_admin', 'sys_admin')")
  public ResponseEntity<ApiResponse<TaskResponseDTO>> createTask(@RequestBody @Valid TaskRequestDTO dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(taskService.createTask(dto)));
  }

  /**
   * Updates an existing task. Only accessible by instructors and admins.
   *
   * @param id  the task ID
   * @param dto the request body with updated values
   * @return 200 OK with the updated task
   */
  @PatchMapping("/{id}")
  @Operation(summary = "Update a task")
  @PreAuthorize("hasAnyRole('instructor', 'inst_admin', 'sys_admin')")
  public ResponseEntity<ApiResponse<TaskResponseDTO>> updateTask(
      @PathVariable UUID id,
      @RequestBody @Valid TaskRequestDTO dto) {
    return ResponseEntity.ok(ApiResponse.ok(taskService.updateTask(dto, id)));
  }

  /**
   * Deletes a task by ID. Only accessible by instructors and admins.
   *
   * @param id the task ID
   * @return 204 No Content
   */
  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a task")
  @PreAuthorize("hasAnyRole('instructor', 'inst_admin', 'sys_admin')")
  public ResponseEntity<Void> deleteTask(@PathVariable UUID id) {
    taskService.deleteTask(id);
    return ResponseEntity.noContent().build();
  }
}
