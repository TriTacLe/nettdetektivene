package ntnu.no.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.sidequest.SideQuestStateDTO;
import ntnu.no.backend.dto.sidequest.SideQuestUpdateRequestDTO;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.service.SideQuestService;
import ntnu.no.backend.shared.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/side-quests")
@RequiredArgsConstructor
@Tag(name = "Side quests", description = "Per-user state for branching one-shot side quests on the game map")
public class SideQuestController {

  private final SideQuestService sideQuestService;

  @GetMapping("/me")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<List<SideQuestStateDTO>>> getMine(
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(ApiResponse.ok(sideQuestService.getMine(user.getId())));
  }

  @PostMapping("/me/{questKey}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<SideQuestStateDTO>> upsert(
      @AuthenticationPrincipal User user,
      @PathVariable String questKey,
      @Valid @RequestBody SideQuestUpdateRequestDTO body) {
    return ResponseEntity.ok(ApiResponse.ok(
        sideQuestService.upsert(user.getId(), questKey, body.stage(), body.status())));
  }
}
