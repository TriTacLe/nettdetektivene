package ntnu.no.backend.service;

import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.sidequest.SideQuestStateDTO;
import ntnu.no.backend.mapper.UserSideQuestMapper;
import ntnu.no.backend.model.Achievement;
import ntnu.no.backend.model.SideQuestStatus;
import ntnu.no.backend.model.UserAchievementId;
import ntnu.no.backend.model.UserSideQuest;
import ntnu.no.backend.model.UserSideQuestId;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.repository.AchievementRepository;
import ntnu.no.backend.repository.UserAchievementRepository;
import ntnu.no.backend.repository.UserRepository;
import ntnu.no.backend.repository.UserSideQuestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Per-user state for branching side quests on the game map. Pairs with
 * {@code user_achievement} to award the per-quest medal on completion.
 *
 * <p>Stage advancement is monotonic; status is terminal once it leaves
 * {@code ACTIVE}.</p>
 */
@Service
@RequiredArgsConstructor
public class SideQuestService {

  /** Side-quest key whose completion is mirrored by the legacy
   * {@code caught_hacker} secret achievement, used for backfill. */
  private static final String HACKER_QUEST_KEY = "hacker";
  private static final String HACKER_ACHIEVEMENT_REF = "caught_hacker";
  /** Stage value to surface for the synthetic backfilled hacker row.
   *  Matches the final stage of the hacker arc on the frontend. */
  private static final int HACKER_COMPLETED_STAGE = 1;

  private final UserSideQuestRepository sideQuestRepository;
  private final UserRepository userRepository;
  private final AchievementRepository achievementRepository;
  private final UserAchievementRepository userAchievementRepository;

  /**
   * Returns every persisted side-quest row for the user, plus a synthetic
   * COMPLETED row for {@code hacker} when the user already owns the
   * {@code caught_hacker} achievement from before this system existed.
   */
  @Transactional(readOnly = true)
  public List<SideQuestStateDTO> getMine(UUID userId) {
    List<UserSideQuest> rows = sideQuestRepository.findByIdUserId(userId);
    List<SideQuestStateDTO> out = new ArrayList<>(rows.size() + 1);
    boolean hasHacker = false;
    for (UserSideQuest row : rows) {
      if (HACKER_QUEST_KEY.equals(row.getId().getQuestKey())) hasHacker = true;
      out.add(UserSideQuestMapper.toDTO(row));
    }
    if (!hasHacker && ownsHackerAchievement(userId)) {
      out.add(new SideQuestStateDTO(HACKER_QUEST_KEY, HACKER_COMPLETED_STAGE, SideQuestStatus.COMPLETED));
    }
    return out;
  }

  private boolean ownsHackerAchievement(UUID userId) {
    return achievementRepository
        .findByTriggerTypeAndTriggerRef("secret", HACKER_ACHIEVEMENT_REF)
        .map(Achievement::getId)
        .map(achId -> userAchievementRepository.existsById(new UserAchievementId(userId, achId)))
        .orElse(false);
  }

  /**
   * Advances or finalizes a side-quest row. Inserts on first call; otherwise
   * updates in place. Stage is monotonic — a lower stage is ignored. Once
   * status is COMPLETED or ABANDONED, further updates are rejected.
   */
  @Transactional
  public SideQuestStateDTO upsert(UUID userId, String questKey, int stage, SideQuestStatus status) {
    if (stage < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stage must be >= 0");
    }
    SideQuestStatus targetStatus = status != null ? status : SideQuestStatus.ACTIVE;
    UserSideQuestId id = new UserSideQuestId(userId, questKey);
    UserSideQuest row = sideQuestRepository.findById(id).orElse(null);
    if (row == null) {
      User user = userRepository.findById(userId)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
      row = UserSideQuest.builder()
          .id(id)
          .user(user)
          .stage(stage)
          .status(targetStatus)
          .build();
      return UserSideQuestMapper.toDTO(sideQuestRepository.save(row));
    }
    if (row.getStatus() != SideQuestStatus.ACTIVE) {
      // Terminal: silently no-op so callers don't have to special-case races.
      return UserSideQuestMapper.toDTO(row);
    }
    if (stage > row.getStage()) {
      row.setStage(stage);
    }
    row.setStatus(targetStatus);
    return UserSideQuestMapper.toDTO(sideQuestRepository.save(row));
  }
}
