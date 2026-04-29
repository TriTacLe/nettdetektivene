package ntnu.no.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ntnu.no.backend.model.*;
import ntnu.no.backend.model.auth.*;
import ntnu.no.backend.model.classroom.ClassroomStudent;
import ntnu.no.backend.model.classroom.ClassroomStudentId;
import ntnu.no.backend.model.enums.NameChangeStatus;
import ntnu.no.backend.model.enums.TaskType;
import ntnu.no.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Demo data seeder activated with the "seed" Spring profile.
 *
 * Run with:
 *   mvn spring-boot:run -Dspring-boot.run.profiles=seed
 *
 * Creates institutions, teachers, classrooms, tasks (all 7 stops),
 * students with varied progress, notebook entries, and achievements.
 * Task attempts go through sp_submit_attempt so the trg_attempt_sync
 * trigger awards XP and unlocks the next task automatically.
 *
 * @author tri
 * @version 0.2
 * @since 0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("seed")
public class DataSeeder implements CommandLineRunner {

  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;
  private final LocalCredentialRepository localCredentialRepository;
  private final AppUserIdentityRepository appUserIdentityRepository;
  private final RoleRepository roleRepository;
  private final InstitutionRepository institutionRepository;
  private final ClassroomRepository classroomRepository;
  private final ClassroomStudentRepository classroomStudentRepository;
  private final ClassroomTaskRepository classroomTaskRepository;
  private final TaskRepository taskRepository;
  private final NotebookEntryRepository notebookEntryRepository;
  private final AchievementRepository achievementRepository;
  private final PasswordEncoder passwordEncoder;
  private final JdbcTemplate jdbcTemplate;

  private final Map<String, User> users = new HashMap<>();
  private final Map<String, Institution> institutions = new HashMap<>();
  private final Map<String, Classroom> classrooms = new HashMap<>();
  private final List<Task> allTasks = new ArrayList<>();
  private Role studentRole;
  private Role instructorRole;

  @Override
  @Transactional
  public void run(String... args) {
    log.info("========================================");
    log.info("Starting Demo Data Seeder");
    log.info("========================================");

    try {
      loadRoles();
      seedInstitutions();
      seedInstructors();
      seedClassrooms();
      seedTasks();
      seedStudents();
      seedEnrollments();
      seedTaskAttempts();
      seedNotebookEntries();
      seedAchievementsIfMissing();
      verifyTaskAssignments();
      verifyEnrollments();
      printSummary();
      log.info("Demo data seeding complete.");
    } catch (Exception e) {
      log.error("Seeding failed: {}", e.getMessage(), e);
      throw e;
    }
  }

  // -----------------------------------------------------------------------
  // Roles
  // -----------------------------------------------------------------------

  private void loadRoles() {
    // Roles are inserted by the Flyway migration with lowercase names.
    studentRole = roleRepository.findByRoleName("student")
        .orElseThrow(() -> new IllegalStateException("Role 'student' not found — run Flyway migrations first"));
    instructorRole = roleRepository.findByRoleName("instructor")
        .orElseThrow(() -> new IllegalStateException("Role 'instructor' not found — run Flyway migrations first"));
    log.info("Roles loaded: student, instructor");
  }

  // -----------------------------------------------------------------------
  // Institutions
  // -----------------------------------------------------------------------

  private void seedInstitutions() {
    String[][] data = {
        {"NTNU Trondheim", "ntnu"},
        {"OsloMet",        "oslomet"},
    };
    for (String[] row : data) {
      Institution inst = institutionRepository.findByInstName(row[0]).orElseGet(() -> {
        log.info("Institution: {}", row[0]);
        return institutionRepository.save(Institution.builder().instName(row[0]).build());
      });
      institutions.put(row[1], inst);
    }
  }

  // -----------------------------------------------------------------------
  // Instructors
  // -----------------------------------------------------------------------

  private void seedInstructors() {
    Object[][] data = {
        {"laerer@ntnu.no",  "Lærer",  "nora",  "ntnu"},
        {"lars@oslomet.no", "Lærer Lars", "lars", "oslomet"},
    };
    for (Object[] row : data) {
      User u = createUser((String) row[0], (String) row[1], List.of(instructorRole));
      users.put((String) row[2], u);
      log.info("Instructor: {} ({})", row[1], row[0]);
    }
  }

  // -----------------------------------------------------------------------
  // Classrooms
  // -----------------------------------------------------------------------

  private void seedClassrooms() {
    Object[][] data = {
        {"5A - Digital Sikkerhet", "Introduksjon til digital sikkerhet", "nora", "ntnu", "class1"},
        {"6B - Nettvett",          "Lær om nettvett og personvern",       "nora", "ntnu", "class2"},
        {"7C - Cybersecurity",     "Grunnleggende cybersecurity",          "lars", "oslomet", "class3"},
    };
    for (Object[] row : data) {
      Classroom c = classroomRepository.findByTitleAndDeletedAtIsNull((String) row[0]).orElseGet(() -> {
        Classroom created = classroomRepository.save(Classroom.builder()
            .title((String) row[0])
            .classDesc((String) row[1])
            .createdBy(users.get(row[2]))
            .institution(institutions.get(row[3]))
            .joinCode(randomCode())
            .isGameEnabled(true)
            .build());
        log.info("Classroom: {} (join code: {})", row[0], created.getJoinCode());
        return created;
      });
      classrooms.put((String) row[4], c);
    }
  }

  // -----------------------------------------------------------------------
  // Tasks  (3 per stop, all 7 stops)
  // -----------------------------------------------------------------------

  private void seedTasks() {
    // {title, type, stop, difficulty, ownerKey}
    Object[][] taskDefs = {
        // nyhetskvartalet
        {"Gjenkjenn falske nyheter",       TaskType.NEWS,        "nyhetskvartalet", 1, "nora"},
        {"Kildesjekk av nettsider",        TaskType.NEWS,        "nyhetskvartalet", 2, "nora"},
        {"Kjennetegn på clickbait",        TaskType.NEWS,        "nyhetskvartalet", 2, "nora"},
        // fotografen
        {"Spor bildemanipulasjon",         TaskType.PHOTO,       "fotografen",      2, "nora"},
        {"Personvern i bilder",            TaskType.PHOTO,       "fotografen",      2, "nora"},
        {"Metadata i foto",                TaskType.PHOTO,       "fotografen",      3, "nora"},
        // postkontoret
        {"Gjenkjenn phishing-e-post",      TaskType.EMAIL,       "postkontoret",    1, "nora"},
        {"Farlig e-postvedlegg",           TaskType.EMAIL,       "postkontoret",    2, "nora"},
        {"Spoofed avsenderadresse",        TaskType.EMAIL,       "postkontoret",    3, "nora"},
        // passordbanken
        {"Lag et sterkt passord",          TaskType.PASSWORD,    "passordbanken",   1, "nora"},
        {"Passordgjenbruk og risiko",      TaskType.PASSWORD,    "passordbanken",   2, "nora"},
        {"To-faktor autentisering",        TaskType.PASSWORD,    "passordbanken",   3, "nora"},
        // markedsplassen
        {"Trygg netthandel",               TaskType.MARKETPLACE, "markedsplassen",  1, "lars"},
        {"Gjenkjenn falske annonser",      TaskType.MARKETPLACE, "markedsplassen",  2, "lars"},
        {"Unngå svindel på finn.no",       TaskType.MARKETPLACE, "markedsplassen",  3, "lars"},
        // sosialen
        {"Personvern på sosiale medier",   TaskType.SOCIAL,      "sosialen",        1, "lars"},
        {"Digitalt fotavtrykk",            TaskType.SOCIAL,      "sosialen",        2, "lars"},
        {"Nettvett og digital mobbing",    TaskType.SOCIAL,      "sosialen",        3, "lars"},
        // datasenteret
        {"Kryptering og sikker lagring",   TaskType.BOSS,        "datasenteret",    4, "nora"},
        {"Sikkerhetskopiering",            TaskType.BOSS,        "datasenteret",    4, "nora"},
        {"Digital Sikkerhetsmester",       TaskType.BOSS,        "datasenteret",    5, "nora"},
    };

Classroom class1 = classrooms.get("class1");
    Classroom class2 = classrooms.get("class2");
    Classroom class3 = classrooms.get("class3");
    short order = 0;
    for (Object[] row : taskDefs) {
      Task task = taskRepository.findByTitleAndDeletedAtIsNull((String) row[0]).orElseGet(() -> {
        log.info("Task: {} @ {}", row[0], row[2]);
        return taskRepository.save(Task.builder()
            .title((String) row[0])
            .taskType((TaskType) row[1])
            .stopLocation((String) row[2])
            .difficulty((Integer) row[3])
            .content("{\"description\": \"" + row[0] + "\", \"difficulty\": " + row[3] + "}")
            .owner(users.get(row[4]))
            .isActive(true)
            .build());
      });

      allTasks.add(task);

      ClassroomTaskId id1 = new ClassroomTaskId(class1.getId(), task.getId());
      if (!classroomTaskRepository.existsById(id1)) {
        classroomTaskRepository.save(ClassroomTask.builder()
            .id(id1).classroom(class1).task(task).sortOrder(order).isActive(true).build());
      }
      ClassroomTaskId id2 = new ClassroomTaskId(class2.getId(), task.getId());
      if (!classroomTaskRepository.existsById(id2)) {
        classroomTaskRepository.save(ClassroomTask.builder()
            .id(id2).classroom(class2).task(task).sortOrder(order).isActive(true).build());
      }
      ClassroomTaskId id3 = new ClassroomTaskId(class3.getId(), task.getId());
      if (!classroomTaskRepository.existsById(id3)) {
        classroomTaskRepository.save(ClassroomTask.builder()
            .id(id3).classroom(class3).task(task).sortOrder(order).isActive(true).build());
      }
      order++;
    }
  }

  // -----------------------------------------------------------------------
  // Students  (one classroom each — unique constraint enforced by DB)
  // -----------------------------------------------------------------------

  private void seedStudents() {
    // {email, displayName, key}
    String[][] data = {
        {"anine@nettdetektivene.no",   "Agent Anine",    "anine"},
        {"nora-s@nettdetektivene.no",  "Detektiv Nora",  "nora-student"},
        {"gustav@nettdetektivene.no",  "Guru Gustav",    "gustav"},
        {"tri@nettdetektivene.no",     "Tri-Tech",       "tri"},
        {"silje@nettdetektivene.no",   "Spion Silje",    "silje"},
        {"vetle@nettdetektivene.no",   "Vekter Vetle",   "vetle"},
        {"regine@nettdetektivene.no",  "Radar-Regine",   "regine"},
        {"segward@nettdetektivene.no", "Sikker-Segward", "segward"},
    };
    for (String[] row : data) {
      User u = createUser(row[0], row[1], List.of(studentRole));
      users.put(row[2], u);
      log.info("Student: {} ({})", row[1], row[0]);
    }
  }

  // -----------------------------------------------------------------------
  // Enrollments — every student in exactly one classroom
  // -----------------------------------------------------------------------

  private void seedEnrollments() {
    Classroom class1 = classrooms.get("class1");
    Classroom class2 = classrooms.get("class2");
    Classroom class3 = classrooms.get("class3");

    // class1 — high/good performers for progress demo
    for (String key : List.of("anine", "nora-student", "gustav", "tri", "silje")) {
      enroll(users.get(key), class1);
    }
    // class2
    for (String key : List.of("vetle", "regine")) {
      enroll(users.get(key), class2);
    }
    // class3
    enroll(users.get("segward"), class3);
  }

  private void enroll(User student, Classroom classroom) {
    ClassroomStudentId id = new ClassroomStudentId(classroom.getId(), student.getId());
    if (classroomStudentRepository.existsById(id)) {
      return;
    }
    ClassroomStudent studentMember = ClassroomStudent.builder()
        .id(id)
        .classroom(classroom)
        .user(student)
        .displayName(student.getDisplayName())
        .isAllowed(true)
        .nameChangeStatus(NameChangeStatus.NONE)
        .build();
    classroomStudentRepository.saveAndFlush(studentMember);
    log.info("Enrolled {} in {}", student.getDisplayName(), classroom.getTitle());
  }

  // -----------------------------------------------------------------------
  // Task attempts — via sp_submit_attempt so trg_attempt_sync fires
  // -----------------------------------------------------------------------

  private void seedTaskAttempts() {
    Classroom class1 = classrooms.get("class1");
    UUID classroomId = class1.getId();

    // Star students: all 21 tasks, score 95-100
    for (String key : List.of("anine", "nora-student", "gustav")) {
      User u = users.get(key);
      if (hasAttempts(u.getId(), classroomId)) continue;
      for (int i = 0; i < allTasks.size(); i++) {
        submitAttempt(u.getId(), allTasks.get(i).getId(), classroomId, 95 + (i % 5));
      }
    }

    // Good students: first 12 tasks, score 80-92
    for (String key : List.of("tri", "silje")) {
      User u = users.get(key);
      if (hasAttempts(u.getId(), classroomId)) continue;
      for (int i = 0; i < 12; i++) {
        submitAttempt(u.getId(), allTasks.get(i).getId(), classroomId, 80 + (i % 13));
      }
    }

    log.info("Task attempts seeded via sp_submit_attempt");
  }

  private boolean hasAttempts(UUID userId, UUID classroomId) {
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM task_best_score WHERE user_id = ? AND classroom_id = ?",
        Integer.class, userId.toString(), classroomId.toString());
    return count != null && count > 0;
  }

  private void submitAttempt(UUID userId, UUID taskId, UUID classroomId, int score) {
    UUID attemptId = UUID.randomUUID();
    boolean passed = score >= 80;
    jdbcTemplate.update("CALL sp_submit_attempt(?, ?, ?, ?, ?, ?)",
        attemptId.toString(),
        userId.toString(),
        taskId.toString(),
        classroomId.toString(),
        score,
        passed ? 1 : 0);
  }

  // -----------------------------------------------------------------------
  // Notebook entries
  // -----------------------------------------------------------------------

  private void seedNotebookEntries() {
    Classroom class1 = classrooms.get("class1");
    Classroom class2 = classrooms.get("class2");

    Object[][] entries = {
        // 5A - Digital Sikkerhet
        {class1, "anine",       "nyhetskvartalet", "Jeg lærte at falske nyheter ofte har sensasjonelle overskrifter. Sjekker alltid kilden nå!"},
        {class1, "nora-student","fotografen",      "Overraskende lett å manipulere bilder. Skal være mer kritisk fremover."},
        {class1, "gustav",      "postkontoret",    "Phishing-e-poster ser veldig ekte ut. Sjekker alltid avsenderadressen."},
        {class1, "tri",         "passordbanken",   "Passordgjenbruk er en stor risiko. Bruker passordbehandler nå."},
        {class1, "silje",       "markedsplassen",  "Lærte å gjenkjenne falske annonser og unngå svindel på nettet."},
        {class1, "anine",       "sosialen",        "Digitalt fotavtrykk er større enn jeg trodde. Tenker meg om nå."},
        {class1, "nora-student","datasenteret",    "Kryptering er viktig for å beskytte data. Imponerende teknologi!"},
        // 6B - Nettvett
        {class2, "regine",      "nyhetskvartalet", "Lærte å skille mellom ekte og falske nyheter ved å sjekke kilden."},
        {class2, "regine",      "fotografen",      "Bilder kan manipuleres enkelt. Ser alltid etter metadata nå."},
        {class2, "vetle",       "postkontoret",    "Phishing er vanskeligere å oppdage enn jeg trodde."},
        {class2, "vetle",       "passordbanken",   "Et sterkt passord er første forsvarslinje. Byttet alle passord!"},
    };

    for (Object[] row : entries) {
      Classroom classroom = (Classroom) row[0];
      User student = users.get((String) row[1]);
      String stopLocation = (String) row[2];
      boolean exists = notebookEntryRepository
          .findByClassroom_IdAndUser_Id(classroom.getId(), student.getId())
          .stream().anyMatch(e -> stopLocation.equals(e.getStopLocation()));
      if (exists) continue;
      notebookEntryRepository.save(NotebookEntry.builder()
          .user(student)
          .classroom(classroom)
          .stopLocation(stopLocation)
          .studentReflection((String) row[3])
          .autoNotes("{\"hints_used\": 1, \"time_spent\": 300}")
          .build());
      log.info("Notebook: {} @ {}", student.getDisplayName(), stopLocation);
    }
  }

  // -----------------------------------------------------------------------
  // Achievements
  // -----------------------------------------------------------------------

  private void seedAchievements() {
    // {title, description, icon, triggerType, triggerRef}
    String[][] data = {
        {"Første Oppgave",     "Fullfør din første oppgave",                    "🎯", "task_completed",      "first_task"},
        {"Nyhetsjeger",        "Fullfør alle oppgaver i Nyhetskvartalet",        "📰", "location_completed",  "nyhetskvartalet"},
        {"Fotoproff",          "Fullfør alle oppgaver hos Fotografen",            "📸", "location_completed",  "fotografen"},
        {"Postmester",         "Fullfør alle oppgaver på Postkontoret",           "📬", "location_completed",  "postkontoret"},
        {"Passordvokter",      "Fullfør alle oppgaver i Passordbanken",           "🔐", "location_completed",  "passordbanken"},
        {"Markedsekspert",     "Fullfør alle oppgaver på Markedsplassen",         "🛒", "location_completed",  "markedsplassen"},
        {"Sosial Detektiv",    "Fullfør alle oppgaver på Sosialen",               "🕵️", "location_completed",  "sosialen"},
        {"Datamester",         "Fullfør alle oppgaver i Datasenteret",            "💻", "location_completed",  "datasenteret"},
        {"Ukens Mysterium",    "Fullfør ukens mysterium",                         "🔍", "ukens_mysterium",     ""},
        {"Sikkerhetsmester",   "Fullfør alle oppgaver i et klasserom",            "🏆", "all_tasks_completed", "classroom"},
        {"Perfekt Score",      "Få 100 poeng på en oppgave",                     "💯", "perfect_score",       "100_points"},
    };

    for (String[] row : data) {
      if (achievementRepository.existsByTitle(row[0])) continue;
      achievementRepository.save(Achievement.builder()
          .title(row[0])
          .descAch(row[1])
          .icon(row[2])
          .triggerType(row[3])
          .triggerRef(row[4].isEmpty() ? null : row[4])
          .build());
      log.info("Achievement: {}", row[0]);
    }
  }

  // -----------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------

  private void seedAchievementsIfMissing() {
    log.info("Seeding achievements...");
    seedAchievements();
  }

  private User createUser(String email, String displayName, List<Role> roles) {
    Optional<User> existing = userRepository.findByEmail(email);
    if (existing.isPresent()) {
      return existing.get();
    }

    User user = userRepository.save(User.builder()
        .email(email)
        .displayName(displayName)
        .isActive(true)
        .roles(new ArrayList<>(roles))
        .build());

    localCredentialRepository.save(LocalCredential.builder()
        .user(user)
        .passwordHash(passwordEncoder.encode("password123"))
        .mustChangePw(false)
        .failedAttempts(0)
        .build());

    String feideId = UUID.randomUUID().toString();

    appUserIdentityRepository.save(AppUserIdentity.builder()
        .user(user)
        .providerName("feide")
        .providerUserId(feideId)
        .providerEmail(email)
        .build());

    userProfileRepository.save(UserProfile.builder()
        .user(user)
        .displayName(displayName)
        .feideId(feideId)
        .xp(0)
        .xpLevel(1)
        .build());

    return user;
  }

  private String randomCode() {
    return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }

  private void printSummary() {
    log.info("");
    log.info("========================================");
    log.info("SEED SUMMARY");
    log.info("========================================");
    log.info("Institutions : {}", institutions.size());
    log.info("Instructors  : laerer@ntnu.no, lars@oslomet.no");
    log.info("Classrooms   : {}", classrooms.size());
    log.info("Tasks        : {} (3 per stop, 7 stops)", allTasks.size());
    log.info("Students     : 8");
    log.info("");
    log.info("LOGIN (password: password123)");
    log.info("  Teacher : laerer@ntnu.no");
    log.info("  Student : anine@nettdetektivene.no  (star — all tasks done)");
    log.info("  Student : tri@nettdetektivene.no    (good — 12/21 tasks done)");
    log.info("  Student : segward@nettdetektivene.no (no attempts)");
    log.info("");
    log.info("CLASSROOM JOIN CODES:");
    classrooms.forEach((key, c) -> log.info("  {} : {}", c.getTitle(), c.getJoinCode()));
    log.info("========================================");
  }

  private void verifyEnrollments() {
    log.info("");
    log.info("Verifying student enrollments...");
    List<String> studentKeys = List.of("anine", "nora-student", "gustav", "tri", "silje", "vetle", "regine", "segward");
    for (String key : studentKeys) {
      User student = users.get(key);
      ClassroomStudent membership = classroomStudentRepository.findByUser(student).orElse(null);
      if (membership == null) {
        log.error("  ERROR: Student {} has no classroom membership!", key);
      } else {
        log.info("  Student {} -> Classroom: {}, isAllowed: {}, DisplayName: {}",
                 student.getDisplayName(),
                 membership.getClassroom().getTitle(),
                 membership.isAllowed(),
                 membership.getDisplayName());
      }
    }
    log.info("Enrollment verification complete.");
    log.info("");
  }

  private void verifyTaskAssignments() {
    log.info("");
    log.info("Verifying task assignments...");
    classrooms.forEach((key, classroom) -> {
      List<ClassroomTask> tasks = classroomTaskRepository.findAllByClassroom_IdAndIsActiveTrueOrderBySortOrderAsc(classroom.getId());
      log.info("  Classroom '{}' ({}): {} active tasks assigned", classroom.getTitle(), key, tasks.size());
      if (tasks.isEmpty()) {
        log.warn("    WARNING: No tasks assigned to classroom {}!", classroom.getTitle());
      } else {
        log.info("    Tasks: {}", tasks.stream()
            .map(ct -> String.format("%s (stop: %s, order: %d)",
                ct.getTask().getTitle(), ct.getTask().getStopLocation(), ct.getSortOrder()))
            .toList());
      }
    });
    log.info("Task assignment verification complete.");
    log.info("");
  }
}
