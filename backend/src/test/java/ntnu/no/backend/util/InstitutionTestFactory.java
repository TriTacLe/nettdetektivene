package ntnu.no.backend.util;

import ntnu.no.backend.dto.institution.InstitutionRequestDTO;
import ntnu.no.backend.dto.institution.InstitutionResponseDTO;
import ntnu.no.backend.model.Institution;

import java.time.LocalDateTime;
import java.util.UUID;

public class InstitutionTestFactory {

  public static Institution institution() {
    return Institution.builder()
        .instName("NTNU")
        .build();
  }

  public static InstitutionRequestDTO requestDTO() {
    return InstitutionRequestDTO.builder()
        .instName("NTNU")
        .build();
  }

  public static InstitutionResponseDTO responseDTO(UUID id) {
    return new InstitutionResponseDTO(
        id, "NTNU", LocalDateTime.now(), LocalDateTime.now()
    );
  }
}
