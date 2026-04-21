package ntnu.no.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.institution.InstitutionRequestDTO;
import ntnu.no.backend.dto.institution.InstitutionResponseDTO;
import ntnu.no.backend.service.InstitutionService;
import ntnu.no.backend.shared.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for institution endpoints.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@RestController
@RequestMapping("/api/v1/institutions")
@RequiredArgsConstructor
@Tag(name = "Institutions", description = "Endpoints for institutions")
public class InstitutionController {

  private final InstitutionService institutionService;

  /**
   * Returns all active institutions.
   *
   * @return 200 OK with list of institutions
   */
  @GetMapping
  @Operation(summary = "Get all institutions")
  public ResponseEntity<ApiResponse<List<InstitutionResponseDTO>>> getAllInstitutions() {
    return ResponseEntity.ok(ApiResponse.ok(institutionService.getAllInstitutions()));
  }

  /**
   * Returns a single institution by ID.
   *
   * @param id the institution ID
   * @return 200 OK with the institution
   */
  @GetMapping("/{id}")
  @Operation(summary = "Get institution by ID")
  public ResponseEntity<ApiResponse<InstitutionResponseDTO>> getInstitutionById(@PathVariable UUID id) {
    return ResponseEntity.ok(ApiResponse.ok(institutionService.getInstitutionById(id)));
  }

  /**
   * Creates a new institution. Only accessible by system admins.
   *
   * @param dto the request body
   * @return 201 Created with the created institution
   */
  @PostMapping
  @Operation(summary = "Create a new institution")
  @PreAuthorize("hasAuthority('sys_admin')")
  public ResponseEntity<ApiResponse<InstitutionResponseDTO>> createInstitution(
      @RequestBody @Valid InstitutionRequestDTO dto) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(institutionService.createInstitution(dto)));
  }

  /**
   * Updates an existing institution. Only accessible by system admins.
   *
   * @param id  the institution ID
   * @param dto the request body with updated values
   * @return 200 OK with the updated institution
   */
  @PutMapping("/{id}")
  @Operation(summary = "Update an institution")
  @PreAuthorize("hasAuthority('sys_admin')")
  public ResponseEntity<ApiResponse<InstitutionResponseDTO>> updateInstitution(
      @PathVariable UUID id,
      @RequestBody @Valid InstitutionRequestDTO dto) {
    return ResponseEntity.ok(ApiResponse.ok(institutionService.updateInstitution(id, dto)));
  }

  /**
   * Soft-deletes an institution by ID. Only accessible by system admins.
   *
   * @param id the institution ID
   * @return 204 No Content
   */
  @DeleteMapping("/{id}")
  @Operation(summary = "Delete an institution")
  @PreAuthorize("hasAuthority('sys_admin')")
  public ResponseEntity<Void> deleteInstitution(@PathVariable UUID id) {
    institutionService.deleteInstitution(id);
    return ResponseEntity.noContent().build();
  }
}
