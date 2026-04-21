package ntnu.no.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.institution.InstitutionResponseDTO;
import ntnu.no.backend.service.InstitutionService;
import ntnu.no.backend.shared.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
   * Returns all institutions.
   *
   * @return 200 OK with list of institutions
   */
  @GetMapping
  @Operation(summary = "Get all institutions")
  public ResponseEntity<ApiResponse<List<InstitutionResponseDTO>>> getAllInstitutions() {
    return ResponseEntity.ok(ApiResponse.ok(institutionService.getAllInstitutions()));
  }
}
