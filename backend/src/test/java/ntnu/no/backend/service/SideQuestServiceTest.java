package ntnu.no.backend.service;

import ntnu.no.backend.dto.sidequest.SideQuestStateDTO;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SideQuestServiceTest {

  @Mock UserSideQuestRepository sideQuestRepository;
  @Mock UserRepository userRepository;
  @Mock AchievementRepository achievementRepository;
  @Mock UserAchievementRepository userAchievementRepository;

  @InjectMocks SideQuestService service;

  @Test
  void upsert_rejectsUnknownQuestKey() {
    UUID userId = UUID.randomUUID();
    assertThatThrownBy(() -> service.upsert(userId, "totally-bogus", 1, SideQuestStatus.ACTIVE))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Unknown side quest");
    verify(sideQuestRepository, never()).save(any(UserSideQuest.class));
  }

  @Test
  void upsert_inserts_whenNoRow() {
    UUID userId = UUID.randomUUID();
    User user = User.builder().id(userId).build();
    UserSideQuestId id = new UserSideQuestId(userId, "hacker");

    when(sideQuestRepository.findById(id)).thenReturn(Optional.empty());
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(sideQuestRepository.save(any(UserSideQuest.class))).thenAnswer(inv -> inv.getArgument(0));

    SideQuestStateDTO out = service.upsert(userId, "hacker", 1, SideQuestStatus.ACTIVE);

    assertThat(out.key()).isEqualTo("hacker");
    assertThat(out.stage()).isEqualTo(1);
    assertThat(out.status()).isEqualTo(SideQuestStatus.ACTIVE);
  }

  @Test
  void upsert_keepsHigherStage_whenLowerStageWritten() {
    UUID userId = UUID.randomUUID();
    UserSideQuestId id = new UserSideQuestId(userId, "hacker");
    UserSideQuest existing = UserSideQuest.builder()
        .id(id).user(User.builder().id(userId).build())
        .stage(2).status(SideQuestStatus.ACTIVE)
        .build();

    when(sideQuestRepository.findById(id)).thenReturn(Optional.of(existing));
    when(sideQuestRepository.save(any(UserSideQuest.class))).thenAnswer(inv -> inv.getArgument(0));

    SideQuestStateDTO out = service.upsert(userId, "hacker", 1, SideQuestStatus.ACTIVE);

    assertThat(out.stage()).isEqualTo(2);
  }

  @Test
  void upsert_isNoop_whenStatusIsTerminal() {
    UUID userId = UUID.randomUUID();
    UserSideQuestId id = new UserSideQuestId(userId, "hacker");
    UserSideQuest done = UserSideQuest.builder()
        .id(id).user(User.builder().id(userId).build())
        .stage(4).status(SideQuestStatus.COMPLETED)
        .build();

    when(sideQuestRepository.findById(id)).thenReturn(Optional.of(done));

    SideQuestStateDTO out = service.upsert(userId, "hacker", 5, SideQuestStatus.ACTIVE);

    assertThat(out.status()).isEqualTo(SideQuestStatus.COMPLETED);
    assertThat(out.stage()).isEqualTo(4);
    verify(sideQuestRepository, never()).save(any(UserSideQuest.class));
  }

  @Test
  void getMine_backfillsHackerCompleted_whenLegacyAchievementOwned() {
    UUID userId = UUID.randomUUID();
    UUID achievementId = UUID.randomUUID();
    Achievement caughtHacker = Achievement.builder().id(achievementId).build();

    when(sideQuestRepository.findByIdUserId(userId)).thenReturn(List.of());
    when(achievementRepository.findByTriggerTypeAndTriggerRef("secret", "caught_hacker"))
        .thenReturn(Optional.of(caughtHacker));
    when(userAchievementRepository.existsById(new UserAchievementId(userId, achievementId)))
        .thenReturn(true);

    List<SideQuestStateDTO> out = service.getMine(userId);

    assertThat(out).hasSize(1);
    assertThat(out.getFirst().key()).isEqualTo("hacker");
    assertThat(out.getFirst().status()).isEqualTo(SideQuestStatus.COMPLETED);
  }

  @Test
  void getMine_doesNotBackfill_whenAchievementNotOwned() {
    UUID userId = UUID.randomUUID();
    Achievement caughtHacker = Achievement.builder().id(UUID.randomUUID()).build();

    when(sideQuestRepository.findByIdUserId(userId)).thenReturn(List.of());
    when(achievementRepository.findByTriggerTypeAndTriggerRef("secret", "caught_hacker"))
        .thenReturn(Optional.of(caughtHacker));
    when(userAchievementRepository.existsById(any(UserAchievementId.class))).thenReturn(false);

    List<SideQuestStateDTO> out = service.getMine(userId);

    assertThat(out).isEmpty();
  }
}
