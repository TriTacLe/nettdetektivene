package ntnu.no.backend.mapper;

import ntnu.no.backend.dto.sidequest.SideQuestStateDTO;
import ntnu.no.backend.model.UserSideQuest;

public final class UserSideQuestMapper {

  private UserSideQuestMapper() {}

  public static SideQuestStateDTO toDTO(UserSideQuest entity) {
    if (entity == null) return null;
    return new SideQuestStateDTO(
        entity.getId().getQuestKey(),
        entity.getStage(),
        entity.getStatus()
    );
  }
}
