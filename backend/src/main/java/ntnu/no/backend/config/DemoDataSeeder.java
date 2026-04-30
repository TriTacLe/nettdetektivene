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
 * mvn spring-boot:run -Dspring-boot.run.profiles=seed
 *
 * Creates institutions, teachers, classrooms, tasks (3 valid stops),
 * students with varied progress, notebook entries, and achievements.
 * Task attempts go through sp_submit_attempt so the trg_attempt_sync
 * trigger awards XP and unlocks the next task automatically.
 *
 * Only seeds tasks for the 3 stop locations that exist in the game map:
 * - nyhetskvartalet (NEWS tasks)
 * - fotografen (PHOTO tasks)
 * - datasenteret (BOSS/FINAL tasks)
 *
 * Matches the structure of factory.sh script.
 *
 * @author tri
 * @version 2.0
 * @since 0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("seed")
public class DemoDataSeeder implements CommandLineRunner {

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
  private final UserAchievementRepository userAchievementRepository;
  private final MysterySubmissionRepository mysterySubmissionRepository;
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
      seedUserAchievements();
      seedMysterySubmissions();
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
        { "NTNU Trondheim", "ntnu" },
        { "OsloMet", "oslomet" },
        { "UiO", "uio" },
        { "NTNU Gjøvik", "ntnu-gjovik" },
        { "BI", "bi" },
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
        { "nora@ntnu.no", "Nora Lilliedahl Kirknes", "nora", "ntnu" },
        { "tri@ntnu.no", "Tri Tac Le", "tri", "ntnu" },
        { "anine@ntnu.no", "Anine Selle Standnes", "anine", "ntnu" },
        { "gustav@ntnu.no", "Gustav Haverstad Skyberg", "gustav", "ntnu" },
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
        { "5A - Datadetektiver", "Lær å være trygg på nettet", "nora", "ntnu", "class1" },
        { "6B - Nettvett", "Slik beskytter du deg selv", "nora", "ntnu", "class2" },
        { "7C - Sikker på nett", "Vær smart på internett", "tri", "ntnu", "class3" },
        { "8D - Bottle FC", "Arsenal - The Gunners", "gustav", "ntnu", "class4" },
        { "9E - Relegation FC", "Tottenham Hotspur Spurs", "anine", "ntnu", "class5" },
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
  // Tasks (3 per stop, all 7 stops)
  // -----------------------------------------------------------------------

  private void seedTasks() {
    // {title, type, stop, difficulty, ownerKey} - matches factory.sh structure
    Object[][] taskDefs = {
        // nyhetskvartalet (NEWS) - 3 tasks levels 1-3
        { "Ekte eller falsk nyhet (nivå 1)", TaskType.NEWS, "nyhetskvartalet", 1, "nora" },
        { "Ekte eller falsk nyhet (nivå 2)", TaskType.NEWS, "nyhetskvartalet", 2, "nora" },
        { "Ekte eller falsk nyhet (nivå 3)", TaskType.NEWS, "nyhetskvartalet", 3, "nora" },
        // fotografen (PHOTO) - 3 tasks levels 1-3
        { "Ekte, AI eller manipulert (nivå 1)", TaskType.PHOTO, "fotografen", 1, "nora" },
        { "Ekte, AI eller manipulert (nivå 2)", TaskType.PHOTO, "fotografen", 2, "nora" },
        { "Ekte, AI eller manipulert (nivå 3)", TaskType.PHOTO, "fotografen", 3, "nora" },
        // datasenteret (FINAL BOSS) - 5 mixed tasks, difficulty 4-5
        { "Datatyven sin første løgn", TaskType.NEWS, "datasenteret", 4, "nora" },
        { "Avslør den falske rapporten", TaskType.NEWS, "datasenteret", 5, "nora" },
        { "Manipulert bevismateriale", TaskType.PHOTO, "datasenteret", 4, "nora" },
        { "Sannhetens øyeblikk", TaskType.NEWS, "datasenteret", 5, "nora" },
        { "Endelig konfrontasjon", TaskType.PHOTO, "datasenteret", 5, "nora" },
    };

    Classroom class1 = classrooms.get("class1");
    Classroom class2 = classrooms.get("class2");
    Classroom class3 = classrooms.get("class3");
    Classroom class4 = classrooms.get("class4");
    Classroom class5 = classrooms.get("class5");
    short order = 0;
    for (Object[] row : taskDefs) {
      Task task = taskRepository.findByTitleAndDeletedAtIsNull((String) row[0]).orElseGet(() -> {
        log.info("Task: {} @ {}", row[0], row[2]);
        return taskRepository.save(Task.builder()
            .title((String) row[0])
            .taskType((TaskType) row[1])
            .stopLocation((String) row[2])
            .difficulty((Integer) row[3])
            .content(buildTaskContent((String) row[0], (TaskType) row[1]))
            .owner(users.get(row[4]))
            .isActive(true)
            .build());
      });

      allTasks.add(task);

      // Assign tasks to all 5 classrooms
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
      ClassroomTaskId id4 = new ClassroomTaskId(class4.getId(), task.getId());
      if (!classroomTaskRepository.existsById(id4)) {
        classroomTaskRepository.save(ClassroomTask.builder()
            .id(id4).classroom(class4).task(task).sortOrder(order).isActive(true).build());
      }
      ClassroomTaskId id5 = new ClassroomTaskId(class5.getId(), task.getId());
      if (!classroomTaskRepository.existsById(id5)) {
        classroomTaskRepository.save(ClassroomTask.builder()
            .id(id5).classroom(class5).task(task).sortOrder(order).isActive(true).build());
      }
      order++;
    }
  }

  // -----------------------------------------------------------------------
  // Students (one classroom each — unique constraint enforced by DB)
  // -----------------------------------------------------------------------

  private void seedStudents() {
    // {email, displayName, key} - funny tech-loaded purpose names for kids app
    String[][] data = {
        // Developers with tech-funny loaded purpose names
        { "siljelund@nettdetektivene.no", "Silje Sjerm-monitoren", "siljelund" },
        { "segward@nettdetektivene.no", "Segward System-sjefen", "segward" },
        { "aninstan@nettdetektivene.no", "Anine App-arkitekten", "aninstan" },
        { "regine@nettdetektivene.no", "Regine Router-ridderen", "regine" },
        { "vetleni@nettdetektivene.no", "Vetle Vits-mesteren", "vetleni" },
        // Instructors as students for testing (tech versions)
        { "tri-student@nettdetektivene.no", "Tri Tech-trollkarlen", "tri-student" },
        { "gustav-student@nettdetektivene.no", "Gustav GPU-guruen", "gustav-student" },
        { "nora-student@nettdetektivene.no", "Nora Nett-bytteren", "nora-student" },
        { "anine-student@nettdetektivene.no", "Anine Algo-angriperen", "anine-student" },
        // Arsenal players - Bottle FC (tech-loaded purpose)
        { "saka@nettdetektivene.no", "Saka Sikkerhets-kjorer", "saka" },
        { "odegaard@nettdetektivene.no", "Odegaard Online-opptreder", "odegaard" },
        { "rice@nettdetektivene.no", "Rice RAM-rederen", "rice" },
        { "saliba@nettdetektivene.no", "Saliba Sentinel-sjefen", "saliba" },
        { "gabriel@nettdetektivene.no", "Gabriel Gigabyte-gardisten", "gabriel" },
        { "partey@nettdetektivene.no", "Partey Port-vakten", "partey" },
        { "martinelli@nettdetektivene.no", "Martinelli Melding-mesteren", "martinelli" },
        // Tottenham players - Relegation FC (Spurs) - tech-loaded purpose
        { "son@nettdetektivene.no", "Son Server-sjefen", "son" },
        { "kane@nettdetektivene.no", "Kane Koden", "kane" },
        { "maddison@nettdetektivene.no", "Maddison Malware-motstander", "maddison" },
        { "romero@nettdetektivene.no", "Romero ROM-rappfoten", "romero" },
        { "porro@nettdetektivene.no", "Porro Proxy-politiet", "porro" },
        { "udogie@nettdetektivene.no", "Udogie USB-borgeren", "udogie" },
        { "bissouma@nettdetektivene.no", "Bissouma Brannmur-beretningen", "bissouma" },
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
    Classroom class4 = classrooms.get("class4");
    Classroom class5 = classrooms.get("class5");

    // class1 — star students and good performers (real developers)
    for (String key : List.of("siljelund", "segward", "aninstan")) {
      enroll(users.get(key), class1);
    }
    // class2 — medium performers (real developers)
    for (String key : List.of("regine", "vetleni")) {
      enroll(users.get(key), class2);
    }
    // class3 — early starters (instructors as students for testing)
    for (String key : List.of("tri-student", "gustav-student", "nora-student", "anine-student")) {
      enroll(users.get(key), class3);
    }
    // class4 - Bottle FC (Arsenal players)
    for (String key : List.of("saka", "odegaard", "rice", "saliba", "gabriel", "partey", "martinelli")) {
      enroll(users.get(key), class4);
    }
    // class5 - Relegation FC (Tottenham players)
    for (String key : List.of("son", "kane", "maddison", "romero", "porro", "udogie", "bissouma")) {
      enroll(users.get(key), class5);
    }
  }

  private void enroll(User student, Classroom classroom) {
    if (student == null || classroom == null)
      return;
    ClassroomStudentId id = new ClassroomStudentId(classroom.getId(), student.getId());
    if (classroomStudentRepository.existsById(id))
      return;
    if (classroomStudentRepository.findByUser(student).isPresent()) {
      log.debug("Student {} already enrolled in a classroom, skipping", student.getDisplayName());
      return;
    }
    classroomStudentRepository.saveAndFlush(ClassroomStudent.builder()
        .id(id)
        .classroom(classroom)
        .user(student)
        .displayName(student.getDisplayName())
        .isAllowed(true)
        .nameChangeStatus(NameChangeStatus.NONE)
        .build());
    log.info("Enrolled {} in {}", student.getDisplayName(), classroom.getTitle());
  }

  private String buildTaskContent(String title, TaskType type) {
    if (type == TaskType.NEWS) {
      return switch (title) {
        case "Hva er sant?" -> "{\"question\":\"Hvilken overskrift er fra en ekte nettavis?\",\"hint\":\"Sjekk nettadressen – ekte norske nyheter har kjente domener som nrk.no eller vg.no.\",\"articles\":[{\"imagePath\":\"https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=500\",\"isReal\":true},{\"imagePath\":\"https://upload.wikimedia.org/wikipedia/commons/d/d9/Example_bank_phishing_email.svg\",\"isReal\":false}]}";
        case "Sjekk kilden" -> "{\"question\":\"Hvilken nyhet kommer fra en pålitelig kilde?\",\"hint\":\"Pålitelige kilder har tydelig forfatternavn, dato og lenke til originalen.\",\"articles\":[{\"imagePath\":\"https://images.unsplash.com/photo-1495020689067-958852a7765e?w=500\",\"isReal\":true},{\"imagePath\":\"https://upload.wikimedia.org/wikipedia/commons/b/bd/Lottoscamimage1.jpg\",\"isReal\":false}]}";
        case "Falske nyheter" -> "{\"question\":\"Klarer du å avsløre den falske nyheten?\",\"hint\":\"Falske nyheter bruker ofte dramatiske ord og mangler tydelig kilde.\",\"articles\":[{\"imagePath\":\"https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=500\",\"isReal\":true},{\"imagePath\":\"https://upload.wikimedia.org/wikipedia/commons/a/a5/GAN_deepfake_white_girl.jpg\",\"isReal\":false}]}";
        default -> "{\"question\":\"Hvilken av disse er ekte?\",\"hint\":\"Se nøye på detaljene.\",\"articles\":[{\"imagePath\":\"https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=500\",\"isReal\":true},{\"imagePath\":\"https://upload.wikimedia.org/wikipedia/commons/d/d9/Example_bank_phishing_email.svg\",\"isReal\":false}]}";
      };
    }
    if (type == TaskType.PHOTO) {
      return switch (title) {
        case "Er bildet ekte?" -> "{\"description\":\"Er disse bildene ekte fotografier eller laget av kunstig intelligens?\",\"hint\":\"AI-bilder har ofte urealistiske detaljer som rare ører, uskarp bakgrunn eller rare hender.\",\"alternatives\":[{\"imagePath\":\"https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400\",\"correctType\":\"ekte\"},{\"imagePath\":\"https://upload.wikimedia.org/wikipedia/commons/a/a5/GAN_deepfake_white_girl.jpg\",\"correctType\":\"ai-generert\"}]}";
        case "Redigerte bilder" -> "{\"description\":\"Hvilke av disse bildene er ekte og hvilke er redigert?\",\"hint\":\"Se etter urealistiske kanter, unaturlig lys eller detaljer som ikke stemmer.\",\"alternatives\":[{\"imagePath\":\"https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=400\",\"correctType\":\"ekte\"},{\"imagePath\":\"https://upload.wikimedia.org/wikipedia/commons/f/f2/Male_and_female_deepfake.jpg\",\"correctType\":\"manipulert\"}]}";
        case "Hvem er på bildet?" -> "{\"description\":\"Er disse personene virkelige eller laget av en datamaskin?\",\"hint\":\"AI-genererte ansikter mangler ofte naturlige asymmetrier og detaljer i ørene.\",\"alternatives\":[{\"imagePath\":\"https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400\",\"correctType\":\"ekte\"},{\"imagePath\":\"https://upload.wikimedia.org/wikipedia/commons/a/a5/GAN_deepfake_white_girl.jpg\",\"correctType\":\"ai-generert\"}]}";
        default -> "{\"description\":\"Er dette bildet ekte?\",\"hint\":\"Se nøye på detaljene.\",\"alternatives\":[{\"imagePath\":\"https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400\",\"correctType\":\"ekte\"}]}";
      };
    }
    return "{\"description\":\"" + title + "\"}";
  }

  // -----------------------------------------------------------------------
  // Task attempts — via sp_submit_attempt so trg_attempt_sync fires
  // -----------------------------------------------------------------------

  private void seedTaskAttempts() {
    Classroom class1 = classrooms.get("class1");
    Classroom class2 = classrooms.get("class2");
    Classroom class3 = classrooms.get("class3");
    Classroom class4 = classrooms.get("class4");
    Classroom class5 = classrooms.get("class5");
    UUID class1Id = class1.getId();
    UUID class2Id = class2.getId();
    UUID class3Id = class3.getId();
    UUID class4Id = class4.getId();
    UUID class5Id = class5.getId();

    // Star developers in class1: all 18 tasks, score 95-100
    for (String key : List.of("siljelund", "segward")) {
      User u = users.get(key);
      if (hasAttempts(u.getId(), class1Id))
        continue;
      for (int i = 0; i < allTasks.size(); i++) {
        submitAttempt(u.getId(), allTasks.get(i).getId(), class1Id, 95 + (i % 5));
      }
    }

    // Good developer in class1: first 12 tasks, score 80-92
    User aninstanDev = users.get("aninstan");
    if (aninstanDev != null && !hasAttempts(aninstanDev.getId(), class1Id)) {
      for (int i = 0; i < 12; i++) {
        submitAttempt(aninstanDev.getId(), allTasks.get(i).getId(), class1Id, 80 + (i % 13));
      }
    }

    // Medium developers in class2: first 8 tasks, score 75-85
    for (String key : List.of("regine", "vetleni")) {
      User u = users.get(key);
      if (hasAttempts(u.getId(), class2Id))
        continue;
      for (int i = 0; i < 8; i++) {
        submitAttempt(u.getId(), allTasks.get(i).getId(), class2Id, 75 + (i % 11));
      }
    }

    // Early starter instructors in class3: first 3 tasks
    for (String key : List.of("tri-student", "gustav-student")) {
      User u = users.get(key);
      if (hasAttempts(u.getId(), class3Id))
        continue;
      for (int i = 0; i < 3; i++) {
        submitAttempt(u.getId(), allTasks.get(i).getId(), class3Id, 85);
      }
    }

    // Arsenal players in class4 (Bottle FC): first 10 tasks
    for (String key : List.of("saka", "odegaard", "rice", "saliba")) {
      User u = users.get(key);
      if (u != null && !hasAttempts(u.getId(), class4Id)) {
        for (int i = 0; i < 10; i++) {
          submitAttempt(u.getId(), allTasks.get(i).getId(), class4Id, 88);
        }
      }
    }

    // Tottenham players in class5 (Relegation FC): first 6 tasks
    for (String key : List.of("son", "kane", "maddison")) {
      User u = users.get(key);
      if (u != null && !hasAttempts(u.getId(), class5Id)) {
        for (int i = 0; i < 6; i++) {
          submitAttempt(u.getId(), allTasks.get(i).getId(), class5Id, 72);
        }
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
    Classroom class3 = classrooms.get("class3");

    Object[][] entries = {
        // class1 - 5A - Datadetektiver (real developers)
        { class1, "siljelund", "nyhetskvartalet",
            "Jeg hoerte at falske nyheter ofte har morsomme overskrifter. Naa sjekker jeg alltid kilden!",
            generateAutoNotes("nyhetskvartalet") },
        { class1, "siljelund", "fotografen", "Det var lett aa endre bilder! Jeg skal vre mer kritisk fremover.",
            generateAutoNotes("fotografen") },
        { class1, "segward", "nyhetskvartalet",
            "Noen e-poster ser veldig ekte ut. Jeg sjekker alltid avsenderadresse naa.",
            generateAutoNotes("nyhetskvartalet") },
        { class1, "segward", "fotografen", "Passord maa vaere sterke! Jeg husker ikke mine bra passord lenger.",
            generateAutoNotes("fotografen") },
        { class1, "aninstan", "nyhetskvartalet", "Noen ting du leser paa nettet er ikke sanne!",
            generateAutoNotes("nyhetskvartalet") },
        { class1, "aninstan", "fotografen", "Bilder kan endres med apper.", generateAutoNotes("fotografen") },
        // class2 - 6B - Nettvett (real developers)
        { class2, "regine", "nyhetskvartalet", "Lærte å skille mellom ekte og falske nyheter ved å sjekke kilden.",
            generateAutoNotes("nyhetskvartalet") },
        { class2, "regine", "fotografen", "Bilder kan manipuleres enkelt. Ser alltid etter metadata nå.",
            generateAutoNotes("fotografen") },
        { class2, "vetleni", "nyhetskvartalet", "Phishing er vanskeligere å oppdage enn jeg trodde.",
            generateAutoNotes("nyhetskvartalet") },
        { class2, "vetleni", "fotografen", "Et sterkt passord er første forsvarslinje. Byttet alle passord!",
            generateAutoNotes("fotografen") },
        // class3 - 7C - Sikker paa nett (instructors as students)
        { class3, "tri-student", "nyhetskvartalet",
            "Falske nyheter sprer seg fort paa sosiale medier. Lærte teknikker for faktasjekk.",
            generateAutoNotes("nyhetskvartalet") },
        { class3, "tri-student", "fotografen",
            "Bildeanalyse er viktigere enn jeg trodde. AI-genererte bilder er技 vanskelige!",
            generateAutoNotes("fotografen") },
        { class3, "gustav-student", "nyhetskvartalet",
            "Phishing-angrep blir stadig mer sofistikerte. Dobbeltsjekk alltid!", generateAutoNotes("nyhetskvartalet") },
    };

    for (Object[] row : entries) {
      Classroom classroom = (Classroom) row[0];
      User student = users.get((String) row[1]);
      String stopLocation = (String) row[2];
      boolean exists = notebookEntryRepository
          .findByClassroom_IdAndUser_Id(classroom.getId(), student.getId())
          .stream().anyMatch(e -> stopLocation.equals(e.getStopLocation()));
      if (exists)
        continue;
      notebookEntryRepository.save(NotebookEntry.builder()
          .user(student)
          .classroom(classroom)
          .stopLocation(stopLocation)
          .studentReflection((String) row[3])
          .autoNotes((String) row[4])
          .build());
      log.info("Notebook: {} @ {}", student.getDisplayName(), stopLocation);
    }
  }

  private String generateAutoNotes(String stopLocation) {
    return switch (stopLocation) {
      case "nyhetskvartalet" ->
        """
            {
              "key_concepts": ["kildehenvisninger", "forfattere", "datoer", "overskrifter"],
              "tips": ["Sjekk alltid hvem som har skrevet artikkelen", "Vær skeptisk til sansomlige nettsteder", "Se etter service panels og om-oss"],
              "resources": ["https://faktisk.no", "https://sok.dagbladet.no/faktiskhet"],
              "hints_used": 2,
              "time_spent": 450
            }
            """;
      case "fotografen" ->
        """
            {
              "image_analysis": ["Se på lyskilder og skygger", "Sjekk for inkonsistenser", "Vær ekstra kritisk til AI-genererte bilder"],
              "indicators": ["For mange fingre", "Unaturlige teksturer", "Fantastisk elementer"],
              "verification": ["Bruk Google Images for reverse bilde-søk", "Sjekk metadata hvis tilgjengelig"],
              "hints_used": 1,
              "time_spent": 380
            }
            """;
      case "datasenteret" -> """
          {
            "encryption": ["End-to-end kryptering", "Sikkerhetskopiering", "Databeskyttelse"],
            "cyber_defense": ["Brannmur", "Intrusion detection", "Responsplaner"],
            "compliance": ["GDPR", "Personvernvern", "Sikkerhetspolicy"],
            "hints_used": 3,
            "time_spent": 560
          }
          """;
      default -> """
          {
            "hints_used": 1,
            "time_spent": 300
          }
          """;
    };
  }

  // -----------------------------------------------------------------------
  // Achievements
  // -----------------------------------------------------------------------

  private void seedAchievements() {
    // {title, description, icon, triggerType, triggerRef}
    String[][] data = {
        { "Første Oppgave", "Fullfør din første oppgave", "🎯", "task_completed", "first_task" },
        { "Nyhetsjeger", "Fullfør alle oppgaver i Nyhetskvartalet", "📰", "location_completed", "nyhetskvartalet" },
        { "Fotoproff", "Fullfør alle oppgaver hos Fotografen", "📸", "location_completed", "fotografen" },
        { "Datamester", "Fullfør alle oppgaver i Datasenteret", "💻", "location_completed", "datasenteret" },
        { "Ukens Mysterium", "Fullfør ukens mysterium", "🔍", "ukens_mysterium", "" },
        { "Sikkerhetsmester", "Fullfør alle oppgaver i et klasserom", "🏆", "all_tasks_completed", "classroom" },
        { "Plettfri Detektiv", "Fullfør alle oppgaver i et klasserom uten å feile", "🛡️", "perfect_classroom",
            "classroom" },
        { "Aldri Gi Opp", "Feil 10 oppgaveforsøk", "💪", "tasks_failed", "10" },
        { "Perfekt Score", "Få 100 poeng på en oppgave", "💯", "perfect_score", "100_points" },
    };

    for (String[] row : data) {
      if (achievementRepository.existsByTitle(row[0]))
        continue;
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

  // -----------------------------------------------------------------------
  // User Achievements (Medaljer)
  // -----------------------------------------------------------------------

  private void seedUserAchievements() {
    log.info("Seeding user achievements for multiple students...");

    // Get all achievements to award
    List<Achievement> achievements = achievementRepository.findAll();
    Classroom class1 = classrooms.get("class1");
    Classroom class2 = classrooms.get("class2");
    Classroom class3 = classrooms.get("class3");

    // Award achievements to siljelund (star developer in class1)
    User siljelund = users.get("siljelund");
    if (siljelund != null) {
      awardAchievements(siljelund, class1, achievements,
          List.of("Første Oppgave", "Nyhetsjeger", "Fotoproff", "Datamester", "Plettfri Detektiv"));
    }

    // Award achievements to segward (excellent developer in class1)
    User segward = users.get("segward");
    if (segward != null) {
      awardAchievements(segward, class1, achievements,
          List.of("Første Oppgave", "Nyhetsjeger", "Fotoproff", "Perfekt Score", "Sikkerhetsmester"));
    }

    // Award achievements to aninstan (good developer in class1)
    User aninstan = users.get("aninstan");
    if (aninstan != null) {
      awardAchievements(aninstan, class1, achievements,
          List.of("Første Oppgave", "Nyhetsjeger", "Aldri Gi Opp"));
    }

    // Award some achievements to class2 developers
    User regine = users.get("regine");
    if (regine != null) {
      awardAchievements(regine, class2, achievements,
          List.of("Første Oppgave", "Nyhetsjeger"));
    }

    User vetleni = users.get("vetleni");
    if (vetleni != null) {
      awardAchievements(vetleni, class2, achievements,
          List.of("Første Oppgave"));
    }

    // Award achievements to class3 instructors-as-students
    User triStudent = users.get("tri-student");
    if (triStudent != null) {
      awardAchievements(triStudent, class3, achievements,
          List.of("Første Oppgave"));
    }

    log.info("User achievements seeding complete");
  }

  private void awardAchievements(User user, Classroom classroom, List<Achievement> achievements,
      List<String> achievementTitles) {
    for (String achievementTitle : achievementTitles) {
      achievements.stream()
          .filter(a -> a.getTitle().equals(achievementTitle))
          .findFirst()
          .ifPresent(achievement -> {
            UserAchievementId id = new UserAchievementId(user.getId(), achievement.getId());
            if (!userAchievementRepository.existsById(id)) {
              userAchievementRepository.save(UserAchievement.builder()
                  .id(id)
                  .user(user)
                  .achievement(achievement)
                  .classroom(classroom)
                  .build());
              log.info("Awarded '{}' to {}", achievementTitle, user.getDisplayName());
            }
          });
    }
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
    log.info("Instructors  : 4 (Nora, Tri, Anine, Gustav)");
    log.info("Classrooms   : {} classrooms with 11 tasks each", classrooms.size());
    log.info("Tasks        : {} tasks (3 stops: nyhetskvartalet, fotografen, datasenteret)", allTasks.size());
    long studentsCount = users.entrySet().stream()
        .filter(e -> !List.of("nora", "tri", "anine", "gustav").contains(e.getKey())).count();
    log.info("Students     : {} students across classrooms", studentsCount);
    log.info("");
    log.info("LOGIN (password: password123)");
    log.info("  Teacher : nora@ntnu.no, tri@ntnu.no");
    log.info("  Student : siljelund@nettdetektivene.no (Silje Sjerm-monitoren — star)");
    log.info("  Student : segward@nettdetektivene.no (Segward System-sjefen — excellent)");
    log.info("  Student : saka@nettdetektivene.no (Saka Sikkerhets-kjorer — Arsenal)");
    log.info("  Student : kane@nettdetektivene.no (Kane Koden — Tottenham)");
    log.info("");
    log.info("TOP DEVELOPERS:");
    User siljelund = users.get("siljelund");
    if (siljelund != null) {
      long siljelundAchievements = userAchievementRepository.findByUserId(siljelund.getId()).size();
      long siljelundNotebooks = notebookEntryRepository
          .findByClassroom_IdAndUser_Id(classrooms.get("class1").getId(), siljelund.getId()).size();
      log.info("  Silje Sjerm-monitoren: {} achievements, {} notebook entries", siljelundAchievements,
          siljelundNotebooks);
    }
    User segward = users.get("segward");
    if (segward != null) {
      long segwardAchievements = userAchievementRepository.findByUserId(segward.getId()).size();
      long segwardNotebooks = notebookEntryRepository
          .findByClassroom_IdAndUser_Id(classrooms.get("class1").getId(), segward.getId()).size();
      log.info("  Segward System-sjefen: {} achievements, {} notebook entries", segwardAchievements, segwardNotebooks);
    }
    log.info("");
    log.info("CLASSROOM DETAILS:");
    log.info("  5A - Datadetektiver: Developers (Silje, Segward, Anine)");
    log.info("  6B - Nettvett: Developers (Regine, Vetle)");
    log.info("  7C - Sakker pa nett: Instructors as students");
    log.info("  8D - Bottle FC: Arsenal players");
    log.info("  9E - Relegation FC: Tottenham players");
    log.info("");
    log.info("CLASSROOM JOIN CODES:");
    classrooms.forEach((key, c) -> log.info("  {} : {}", c.getTitle(), c.getJoinCode()));
    log.info("========================================");
  }

  private void verifyEnrollments() {
    log.info("");
    log.info("Verifying student enrollments...");
    List<String> studentKeys = List.of("siljelund", "segward", "aninstan", "regine", "vetleni",
        "tri-student", "gustav-student", "nora-student", "anine-student",
        "saka", "odegaard", "rice", "saliba", "gabriel", "partey", "martinelli",
        "son", "kane", "maddison", "romero", "porro", "udogie", "bissouma");
    for (String key : studentKeys) {
      User student = users.get(key);
      if (student == null)
        continue;
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

  // -----------------------------------------------------------------------
  // Mystery Weekly Submissions
  // -----------------------------------------------------------------------

  private void seedMysterySubmissions() {
    log.info("Seeding mystery submissions...");

    Classroom class1 = classrooms.get("class1");
    Classroom class2 = classrooms.get("class2");
    Classroom class4 = classrooms.get("class4");
    Classroom class5 = classrooms.get("class5");

    if (class1 == null) {
      log.warn("Classroom class1 not available for mystery seeding");
      return;
    }

    if (!mysterySubmissionRepository.findByClassroom_Id(class1.getId()).isEmpty()) {
      log.info("Mystery submissions already seeded, skipping.");
      return;
    }

    User noraInstructor = users.get("nora");

    // === PAST MYSTERIES (Week 48-52, 2025) ===

    // Week 48, 2025 - siljelund approved & featured
    createMysterySubmissionWithStatus(users.get("siljelund"), class1,
        "Jeg lærte at ikke alle bilder på nettet er ekte! Noen er laget av kunstig intelligens.",
        "https://upload.wikimedia.org/wikipedia/commons/a/a5/GAN_deepfake_white_girl.jpg", 48, 2025, true,
        ntnu.no.backend.model.enums.MysteryStatus.APPROVED, noraInstructor);

    // Week 48, 2025 - segward approved
    createMysterySubmissionWithStatus(users.get("segward"), class1,
        "Funnet en nettside som ser ekte ut, men har falske nyheter!",
        "https://snl.no/falske_nyheter", 48, 2025, false,
        ntnu.no.backend.model.enums.MysteryStatus.APPROVED, noraInstructor);

    // Week 49, 2025 - aninstan rejected
    createMysterySubmissionWithStatus(users.get("aninstan"), class1,
        "Kan mann dele passord med venner?",
        null, 49, 2025, false,
        ntnu.no.backend.model.enums.MysteryStatus.REJECTED, noraInstructor);

    // Week 50, 2025 - siljelund approved & featured
    createMysterySubmissionWithStatus(users.get("siljelund"), class1,
        "Phishing e-poster kan se veldig ekte ut. Sjekk alltid avsender!",
        "https://upload.wikimedia.org/wikipedia/commons/d/d9/Example_bank_phishing_email.svg", 50, 2025, true,
        ntnu.no.backend.model.enums.MysteryStatus.APPROVED, noraInstructor);

    // Week 51, 2025 - segward pending
    createPendingMysterySubmission(users.get("segward"), class1,
        "Er det trygt aa bruke samme passord paa flere sider?",
        null, 51, 2025, false);

    // Week 52, 2025 - aninstan approved
    createMysterySubmissionWithStatus(users.get("aninstan"), class1,
        "Jeg oppdaget en falske butikk paa nettet som solgte dyre varer billig!",
        "https://snl.no/nettsvindel", 52, 2025, false,
        ntnu.no.backend.model.enums.MysteryStatus.APPROVED, noraInstructor);

    // === CURRENT WEEK (Week 1-2, 2026) ===

    // Week 1, 2026 - regine pending
    if (class2 != null && users.get("regine") != null) {
      createPendingMysterySubmission(users.get("regine"), class2,
        "Hvordan vet man om et passord er sterkt nok?",
        null, 1, 2026, false);
    }

    // Week 2, 2026 - Arsenal player pending
    if (class4 != null && users.get("saka") != null) {
      createPendingMysterySubmission(users.get("saka"), class4,
        "Fotballspillere far mye personlig info pa nettet. Hvordan kan vi beskytte oss?",
        null, 2, 2026, false);
    }

    // Week 2, 2026 - Arsenal player approved & featured
    if (class4 != null && users.get("odegaard") != null) {
      User triInstructor = users.get("tri");
      createMysterySubmissionWithStatus(users.get("odegaard"), class4,
        "Det er mange falske profiler som later som aa være kjendiser pa sosiale medier!",
        "https://images.unsplash.com/photo-1665470909901-162912ec16f7?w=800", 2, 2026, true,
        ntnu.no.backend.model.enums.MysteryStatus.APPROVED, triInstructor);
    }

  // Week 2, 2026 - Tottenham player rejected
  if(class5!=null&&users.get("kane")!=null)
  {
    User anineInstructor = users.get("anine");
    createMysterySubmissionWithStatus(users.get("kane"), class5,
        "Er det trygt aa trykke pa alle lenker i e-post?",
        null, 2, 2026, false,
        ntnu.no.backend.model.enums.MysteryStatus.REJECTED, anineInstructor);
  }

  // Week 2, 2026 - Tottenham player pending
  if(class5!=null&&users.get("son")!=null)
  {
      createPendingMysterySubmission(users.get("son"), class5,
        "Jeg fikk en e-post som sa at jeg hadde vunnet i lotto, men jeg ikke spiller lotto!",
        "https://upload.wikimedia.org/wikipedia/commons/b/bd/Lottoscamimage1.jpg", 2, 2026, false);
    }

  log.info("Mystery submissions seeded");
  }

  private void createMysterySubmissionWithStatus(User student, Classroom classroom, String description,
                                                   String attachmentUrl, int weekNumber, int weekYear,
                                                   boolean isFeatured,
                                                   ntnu.no.backend.model.enums.MysteryStatus status,
                                                   User reviewedBy) {
    if (student == null || classroom == null) return;

    ntnu.no.backend.model.MysterySubmission.MysterySubmissionBuilder builder =
        ntnu.no.backend.model.MysterySubmission.builder()
            .classroom(classroom)
            .userSubmittedBy(student)
            .mysDesc(description)
            .attachmentUrl(attachmentUrl)
            .mysStatus(status)
            .isFeatured(isFeatured)
            .weekNumber(weekNumber)
            .weekYear(weekYear);

    if (reviewedBy != null) {
      builder.userReviewedBy(reviewedBy);
      builder.reviewedAt(java.time.LocalDateTime.now().minusWeeks(2));
    }

    mysterySubmissionRepository.save(builder.build());
    log.info("Mystery submission created by {} for week {}-{} (status: {}, featured: {})",
             student.getDisplayName(), weekNumber, weekYear, status, isFeatured);
  }

  private void createPendingMysterySubmission(User student, Classroom classroom, String description,
                                               String attachmentUrl, int weekNumber, int weekYear, boolean isFeatured) {
    if (student == null || classroom == null) return;

    createMysterySubmissionWithStatus(student, classroom, description, attachmentUrl,
                                       weekNumber, weekYear, isFeatured,
                                       ntnu.no.backend.model.enums.MysteryStatus.PENDING, null);
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
