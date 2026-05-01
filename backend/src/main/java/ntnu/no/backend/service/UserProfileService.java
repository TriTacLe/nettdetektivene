package ntnu.no.backend.service;                                                                                          
                                                                                                                                     
import jakarta.persistence.EntityNotFoundException;                                                                                
import lombok.RequiredArgsConstructor;                                                                                             
import ntnu.no.backend.dto.profile.UserProfileResponseDTO;                                                                         
import ntnu.no.backend.dto.profile.UserProfileUpdateRequestDTO;                                                                    
import ntnu.no.backend.mapper.UserProfileMapper;                                                                                   
import ntnu.no.backend.model.auth.User;                                                                                            
import ntnu.no.backend.model.auth.UserProfile;                                                                                     
import ntnu.no.backend.model.Notification;                                                                                         
                                                                                                                                     
import ntnu.no.backend.repository.ClassroomInstructorRepository;                                                                   
                                                                                                                                     
import ntnu.no.backend.repository.NotificationRepository;                                                                          
import ntnu.no.backend.model.classroom.ClassroomStudent;                                                                           
import ntnu.no.backend.model.enums.NameChangeStatus;                                                                               
import ntnu.no.backend.repository.ClassroomStudentRepository;                                                                      
import ntnu.no.backend.repository.UserProfileRepository;                                                                           
import ntnu.no.backend.repository.UserRepository;                                                                                  
import ntnu.no.backend.shared.CensoredWords;                                                                                       
import org.springframework.jdbc.core.JdbcTemplate;                                                                                 
import org.springframework.stereotype.Service;                                                                                     
import org.springframework.transaction.annotation.Transactional;                                                                   
                                                                                                                                     
  /**                                                                                                                                
   * Service for user profile management.                                                                                            
   *                                                                                                                                 
   * @author norakirknes                                                                                                             
   * @version 0.2                                                                                                                    
   * @since 0.1                                                                                                                      
   * @see UserProfileRepository                                                                                                      
   */                                                                                                                                
  @Service                                                                                                                           
  @RequiredArgsConstructor                                                                                                           
  public class UserProfileService {                                                                                                  
                                                                                                                                     
    private final UserProfileRepository userProfileRepository;                                                                       
    private final UserRepository userRepository;                                                                                     
    private final ClassroomStudentRepository classroomStudentRepository;                                                             
    private final JdbcTemplate jdbcTemplate;                                                                                         
    private final ClassroomInstructorRepository classroomInstructorRepository;                                                       
                                                                                                                                     
    private final NotificationRepository notificationRepository;                                                                     
                                                                                                                                     
    /**                                                                                                                              
     * Retrieves the profile for the given user.                                                                                     
     *                                                                                                                               
     * @param user the authenticated user                                                                                            
     * @return a {@link UserProfileResponseDTO} containing the profile data                                                          
     * @throws EntityNotFoundException if no profile exists for the given user                                                       
     */                                                                                                                              
    @Transactional(readOnly = true)                                                                                                  
    public UserProfileResponseDTO getByUser(User user) {                                                                             
      UserProfile profile = userProfileRepository.findByUser(user)                                                                   
        .orElseThrow(() -> new EntityNotFoundException("No profile found for user: " + user.getId()));                               
      String nameChangeStatus = classroomStudentRepository.findByUser(user)                                                          
          .map(m -> m.getNameChangeStatus().name())                                                                                  
          .orElse(null);                                                                                                             
      return UserProfileMapper.toDTO(profile, nameChangeStatus);                                                                     
    }                                                                                                                                
                                                                                                                                     
    /**                                                                                                                              
     * Retrieves a profile by FEIDE identifier.                                                                                      
     * Used for public profile lookups where only the feideId is known.                                                              
     * For example for viewing another student's profile from a leaderboard                                                          
     * or classroom context.                                                                                                         
     *                                                                                                                               
     * @param feideId the FEIDE identifier                                                                                           
     * @return a {@link UserProfileResponseDTO} containing the profile data                                                          
     * @throws EntityNotFoundException if no profile exists for the given feideId                                                    
     */                                                                                                                              
    @Transactional(readOnly = true)
    public int getStoryProgress(User user) {
      UserProfile profile = userProfileRepository.findByUser(user)
          .orElseThrow(() -> new EntityNotFoundException("No profile found for user: " + user.getId()));
      return profile.getStoryProgress();
    }

    @Transactional
    public int advanceStoryProgress(User user, int step) {
      UserProfile profile = userProfileRepository.findByUser(user)
          .orElseThrow(() -> new EntityNotFoundException("No profile found for user: " + user.getId()));
      if (step > profile.getStoryProgress()) {
        profile.setStoryProgress(step);
      }
      return profile.getStoryProgress();
    }

    @Transactional(readOnly = true)
    public UserProfileResponseDTO getByFeideId(String feideId) {

      UserProfile profile = userProfileRepository.findByFeideId(feideId)                                                             
        .orElseThrow(() -> new EntityNotFoundException("No profile found for feideId: " + feideId));                                 
      return UserProfileMapper.toDTO(profile);                                                                                       
    }                                                                                                                                
                                                                                                                                     
    /**                                                                                                                              
     * Updates the profile for the given user.                                                                                       
     * If the user is a student in a classroom and a display name change is requested,                                               
     * the change is forwarded as a pending name change request on the classroom                                                     
     * membership instead of updating the profile directly. The profile display name                                                 
     * is only updated when the instructor approves the request.                                                                     
     *                                                                                                                               
     * @param user the authenticated user whose profile should be updated                                                            
     * @param dto the update request containing new display name and avatar config                                                   
     * @return a {@link UserProfileResponseDTO} with the updated profile data                                                        
     * @throws IllegalArgumentException if display name contains inappropriate words                                                 
     * @throws EntityNotFoundException if no profile exists for the given user                                                       
     */                                                                                                                              
    @Transactional(noRollbackFor = IllegalArgumentException.class)                                                                   
      public UserProfileResponseDTO update(User user, UserProfileUpdateRequestDTO dto) {                                             
        UserProfile profile = userProfileRepository.findByUser(user)                                                                 
          .orElseThrow(() -> new EntityNotFoundException("No profile found for user: " + user.getId()));                             
        if (dto.getDisplayName() != null) {
          if (CensoredWords.containsBlockedWord(dto.getDisplayName())) {
            notifyInstructorsOfInappropriateName(user, dto.getDisplayName());
            throw new IllegalArgumentException("Brukernavnet inneholder upassende ord");
          }
          ClassroomStudent membership = classroomStudentRepository.findByUser(user).orElse(null);
          if (membership != null) {                                                                                                                        
            if (membership.getNameChangeStatus() != NameChangeStatus.PENDING) {
              jdbcTemplate.update("CALL sp_request_display_name_change(?, ?)",
                  user.getId().toString(), dto.getDisplayName());
            }                                                                                                                        
          } else {                                                                                                                   
            profile.setDisplayName(dto.getDisplayName());                                                                            
            user.setDisplayName(dto.getDisplayName());                                                                               
            userRepository.save(user);                                                                                               
          }                                                                                                                          
        }                                                                                                                            
        UserProfileMapper.updateEntity(profile, dto);                                                                                
        ClassroomStudent updatedMembership = classroomStudentRepository.findByUser(user).orElse(null);                               
        String nameChangeStatus = updatedMembership != null ? updatedMembership.getNameChangeStatus().name() : null;                 
        return UserProfileMapper.toDTO(profile, nameChangeStatus);                                                                   
      }                                                                                                                              
                                                                                                                                     
                                                                                                                                     
    /**                                                                                                                              
     * Creates a notification for each instructor in the classroom                                                                   
     * of the student requesting the name change to an inappropriate name.                                                           
     *                                                                                                                               
     * @param student the student requesting a name change                                                                           
     * @param requestedName the requested name                                                                                       
     */                                                                                                                              
    private void notifyInstructorsOfInappropriateName(User student, String requestedName) {                                          
      ClassroomStudent membership = classroomStudentRepository.findByUser(student).orElse(null);                                     
      if (membership == null) return;                                                                                                
                                                                                                                                     
      classroomInstructorRepository.findAllByClassroom(membership.getClassroom())                                                    
          .forEach(instructor -> notificationRepository.save(                                                                        
              Notification.builder()                                                                                                 
                  .user(instructor.getUser())                                                                                        
                  .classroom(membership.getClassroom())                                                                              
                  .msg(student.getDisplayName() + " forsøkte å sette brukernavn til \"" + requestedName + "\"")                      
                  .refType("inappropriate_username")                                                                                 
                  .refId(student.getId())                                                                                            
                  .isRead(false)                                                                                                     
                  .build()                                                                                                           
          ));                                                                                                                        
    }                                                                                                                                
                                                                                                                                     
}
