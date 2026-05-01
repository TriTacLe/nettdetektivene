package ntnu.no.backend.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ntnu.no.backend.model.*;
import ntnu.no.backend.model.auth.*;
import ntnu.no.backend.model.classroom.ClassroomStudent;
import ntnu.no.backend.model.classroom.ClassroomStudentId;
import ntnu.no.backend.model.enums.MysteryStatus;
import ntnu.no.backend.model.enums.NameChangeStatus;
import ntnu.no.backend.model.enums.TaskType;
import ntnu.no.backend.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Demo data seeder activated with the "seed" Spring profile.
 *
 * Run with:
 * mvn spring-boot:run -Dspring-boot.run.profiles=seed
 *
 * Creates institutions, teachers, classrooms, tasks
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
@Profile({"seed", "dev"})
public class DataSeeder implements CommandLineRunner {

  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;
  private final LocalCredentialRepository localCredentialRepository;
  private final AppUserIdentityRepository appUserIdentityRepository;
  private final RoleRepository roleRepository;
  private final InstitutionRepository institutionRepository;
  private final ClassroomRepository classroomRepository;
  private final ClassroomStudentRepository classroomStudentRepository;
  private final ClassroomInstructorRepository classroomInstructorRepository;
  private final ClassroomTaskRepository classroomTaskRepository;
  private final TaskRepository taskRepository;
  private final NotebookEntryRepository notebookEntryRepository;
  private final AchievementRepository achievementRepository;
  private final MysterySubmissionRepository mysterySubmissionRepository;
  private final PasswordEncoder passwordEncoder;
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Value("${app.uploads.root:uploads}")
  private String uploadsRoot;

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
      seedAchievementsIfMissing();
      seedTaskAttempts();
      seedNotebookEntries();
      seedMysterySubmissions();
      verifyTaskAssignments();
      verifyInstructorAssignments();
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
        { "Flatåsen skole", "flataasen" },
        { "Byåsen skole", "byaasen" },
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
        { "laerer@flataasen.no", "Nora", "nora", "flataasen" },
        { "laerer@byaasen.no", "Lars", "lars", "byaasen" },
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
        { "5A", "Lær å være trygg på nettet!", "nora", "flataasen", "class1" },
        { "6B", "Hvem kan du stole på på internett?", "nora", "flataasen", "class2" },
        { "7A", "Bli en nettdetektiv og løs mysterier!", "lars", "byaasen", "class3" },
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

  /** Spec for one NEWS task — a fake/real article pair. */
  private record NewsTaskSpec(
      String title, String stop, int difficulty, String hint,
      String fakeImage, String realImage) {
  }

  /** Spec for one PHOTO task — three alternatives keyed by their kind. */
  private record PhotoTaskSpec(
      String title, String stop, int difficulty, String hint,
      String realImage, String aiImage, String manipulatedImage) {
  }

  private void seedTasks() {
    // Copy seed images to the tasks upload folder once (same images factory.sh uses).
    // These paths are used as fallback content; factory.sh will PATCH with its own
    // uploaded images if it runs after the seeder.
    String fakeArticle = copySeedImageForTasks("seed-images/news/fake-article-1.jpg");
    String realArticle  = copySeedImageForTasks("seed-images/news/real-article-1.png");
    String realPhoto    = copySeedImageForTasks("seed-images/photos/real-photo-1.jpeg");
    String fakePhoto    = copySeedImageForTasks("seed-images/photos/fake-photo-1.jpg");

    // {title, type, stop, difficulty, ownerKey}
    Object[][] taskDefs = {
        // nyhetskvartalet — titles match factory.sh so seeder skips if factory already ran
        {"Ekte eller falsk nyhet (nivå 1)", TaskType.NEWS,  "nyhetskvartalet", 1, "nora"},
        {"Ekte eller falsk nyhet (nivå 2)", TaskType.NEWS,  "nyhetskvartalet", 2, "nora"},
        {"Ekte eller falsk nyhet (nivå 3)", TaskType.NEWS,  "nyhetskvartalet", 3, "nora"},
        // fotografen
        {"Ekte, AI eller manipulert (nivå 1)", TaskType.PHOTO, "fotografen", 1, "nora"},
        {"Ekte, AI eller manipulert (nivå 2)", TaskType.PHOTO, "fotografen", 2, "nora"},
        {"Ekte, AI eller manipulert (nivå 3)", TaskType.PHOTO, "fotografen", 3, "nora"},
        // datasenteret — 5 tasks matching factory.sh (3 NEWS + 2 PHOTO)
        {"Datatyven sin første løgn",   TaskType.NEWS,  "datasenteret", 4, "nora"},
        {"Avslør den falske rapporten", TaskType.NEWS,  "datasenteret", 5, "nora"},
        {"Manipulert bevismateriale",   TaskType.PHOTO, "datasenteret", 4, "nora"},
        {"Sannhetens øyeblikk",         TaskType.NEWS,  "datasenteret", 5, "nora"},
        {"Endelig konfrontasjon",       TaskType.PHOTO, "datasenteret", 5, "nora"},
    };

    Classroom class1 = classrooms.get("class1");
    Classroom class2 = classrooms.get("class2");
    Classroom class3 = classrooms.get("class3");
    short order = 0;
    for (Object[] row : taskDefs) {
      TaskType type = (TaskType) row[1];
      String stop   = (String) row[2];
      Task task = taskRepository.findByTitleAndDeletedAtIsNull((String) row[0]).orElseGet(() -> {
        log.info("Task: {} @ {}", row[0], stop);
        return taskRepository.save(Task.builder()
            .title((String) row[0])
            .taskType(type)
            .stopLocation(stop)
            .difficulty((Integer) row[3])
            .content(buildTaskContent(type, stop, fakeArticle, realArticle, realPhoto, fakePhoto))
            .owner(users.get(row[4]))
            .isActive(true)
            .build());
      });
    }

    // ----- Nyhetskvartalet (NEWS) — 3 tasks ---------------------------------
    NewsTaskSpec[] newsSpecs = {
        new NewsTaskSpec("Ekte eller falsk nyhet (nivå 1)", "nyhetskvartalet", 1,
            "Sjekk om kilden virker troverdig.", "fakeArticle1.png", "realArticle1.png"),
        new NewsTaskSpec("Overskriften som lokker (nivå 2)", "nyhetskvartalet", 2,
            "Clickbait skriker etter klikk uten å gi svar.", "fakeArticle3.png", "realArticle3.png"),
        new NewsTaskSpec("Kildehenvisning eller ikke (nivå 3)", "nyhetskvartalet", 3,
            "Reelle saker pleier å nevne navngitte kilder.", "fakeArticle5.png", "realArticle5.png"),
    };
    for (NewsTaskSpec spec : newsSpecs) {
      Task t = upsertNewsTask(spec, "laerer");
      attachToAllClassrooms(t, order++);
    }

    // ----- Fotografen (PHOTO) — 3 tasks -------------------------------------
    PhotoTaskSpec[] photoSpecs = {
        new PhotoTaskSpec("Ekte, AI eller manipulert (nivå 1)", "fotografen", 1,
            "Se etter unaturlig perspektiv og rare detaljer.",
            "realPhoto1.png", "aiPhoto1.png", "psPhoto1.png"),
        new PhotoTaskSpec("Manipulert eller bare merkelig? (nivå 2)", "fotografen", 2,
            "Studer tekstur og lys.",
            "realPhoto3.png", "aiPhoto3.png", "psPhoto3.png"),
        new PhotoTaskSpec("Avansert bildeanalyse (nivå 3)", "fotografen", 3,
            "Sammenlign bakgrunn, lys og perspektiv samtidig.",
            "realPhoto6.png", "aiPhoto6.png", "psPhoto6.png"),
    };
    for (PhotoTaskSpec spec : photoSpecs) {
      Task t = upsertPhotoTask(spec, "laerer");
      attachToAllClassrooms(t, order++);
    }

    // ----- Datasenteret — 5 mixed tasks (3 NEWS + 2 PHOTO) ------------------
    // The frontend's TaskView only renders NEWS and PHOTO content shapes;
    // keep the type aligned with the content even at the boss stop so we
    // don't trip "Denne oppgavetypen har ikke noe innhold å vise ennå."
    NewsTaskSpec[] bossNews = {
        new NewsTaskSpec("Datatyven sin første løgn", "datasenteret", 4,
            "Datatyven planter falsk omtale – avslør den.", "fakeArticle1.png", "realArticle1.png"),
        new NewsTaskSpec("Avslør den falske rapporten", "datasenteret", 5,
            "Sammenlign påstander mot pålitelige kilder.", "fakeArticle3.png", "realArticle4.png"),
        new NewsTaskSpec("Sannhetens øyeblikk", "datasenteret", 5,
            "Den sanne saken er ofte minst spennende.", "fakeArticle5.png", "realArticle2.png"),
    };
    for (NewsTaskSpec spec : bossNews) {
      Task t = upsertNewsTask(spec, "laerer");
      attachToAllClassrooms(t, order++);
    }
    PhotoTaskSpec[] bossPhotos = {
        new PhotoTaskSpec("Manipulert bevismateriale", "datasenteret", 4,
            "Datatyven manipulerer bilder – fordel rolle riktig.",
            "realPhoto2.png", "aiPhoto1.png", "psPhoto3.png"),
        new PhotoTaskSpec("Endelig konfrontasjon", "datasenteret", 5,
            "Siste runde: alt du har lært om skygger og artefakter.",
            "realPhoto4.png", "aiPhoto5.png", "psPhoto6.png"),
    };
    for (PhotoTaskSpec spec : bossPhotos) {
      Task t = upsertPhotoTask(spec, "laerer");
      attachToAllClassrooms(t, order++);
    }  
  }

  /** Builds + saves a NEWS task, copying its two seed images into uploads/. */
  private Task upsertNewsTask(NewsTaskSpec spec, String ownerKey) {
    Optional<Task> existing = taskRepository.findByTitleAndDeletedAtIsNull(spec.title());
    if (existing.isPresent()) {
      allTasks.add(existing.get());
      return existing.get();
    }
    String fakeUrl = copyTaskSeedImage("seed-images/news/" + spec.fakeImage());
    String realUrl = copyTaskSeedImage("seed-images/news/" + spec.realImage());
    Map<String, Object> content = new LinkedHashMap<>();
    content.put("question", "Hvilken nyhet er ekte?");
    content.put("hint", spec.hint());
    // Shuffle deterministically per-title so the real article isn't always
    // in slot 1 — the player can't fall into a positional pattern across
    // tasks, but reseeding the same DB still produces the same layout.
    content.put("articles", shuffleStable(List.of(
        Map.of("imagePath", fakeUrl, "isReal", false),
        Map.of("imagePath", realUrl, "isReal", true)), spec.title()));
    Task task = taskRepository.save(Task.builder()
        .title(spec.title())
        .taskType(TaskType.NEWS)
        .stopLocation(spec.stop())
        .difficulty(spec.difficulty())
        .content(toJson(content))
        .owner(users.get(ownerKey))
        .isActive(true)
        .build());
    allTasks.add(task);
    log.info("Task: {} @ {} (NEWS)", spec.title(), spec.stop());
    return task;
  }

  /** Builds + saves a PHOTO task with three alternatives. */
  private Task upsertPhotoTask(PhotoTaskSpec spec, String ownerKey) {
    Optional<Task> existing = taskRepository.findByTitleAndDeletedAtIsNull(spec.title());
    if (existing.isPresent()) {
      allTasks.add(existing.get());
      return existing.get();
    }
    String realUrl = copyTaskSeedImage("seed-images/photos/" + spec.realImage());
    String aiUrl = copyTaskSeedImage("seed-images/photos/" + spec.aiImage());
    String psUrl = copyTaskSeedImage("seed-images/photos/" + spec.manipulatedImage());
    Map<String, Object> content = new LinkedHashMap<>();
    content.put("description", "Klassifiser hvert bilde: ekte, AI-generert eller manipulert.");
    content.put("hint", spec.hint());
    // Shuffle deterministically per-title so the [real, ai, manipulated]
    // order isn't fixed across tasks — players can't memorize the slot.
    content.put("alternatives", shuffleStable(List.of(
        Map.of("imagePath", realUrl, "correctType", "ekte"),
        Map.of("imagePath", aiUrl, "correctType", "ai-generert"),
        Map.of("imagePath", psUrl, "correctType", "manipulert")), spec.title()));
    Task task = taskRepository.save(Task.builder()
        .title(spec.title())
        .taskType(TaskType.PHOTO)
        .stopLocation(spec.stop())
        .difficulty(spec.difficulty())
        .content(toJson(content))
        .owner(users.get(ownerKey))
        .isActive(true)
        .build());
    allTasks.add(task);
    log.info("Task: {} @ {} (PHOTO)", spec.title(), spec.stop());
    return task;
  }

  /**
   * Copies a classpath seed image to the uploads dir and returns its public URL.
   */
  private String copyTaskSeedImage(String classpathLocation) {
    String filename = Paths.get(classpathLocation).getFileName().toString();
    Path targetDir = Paths.get(uploadsRoot).toAbsolutePath().normalize().resolve("tasks").resolve("seed");
    Path targetFile = targetDir.resolve(filename);
    try {
      Files.createDirectories(targetDir);
      try (InputStream in = new ClassPathResource(classpathLocation).getInputStream()) {
        Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to copy seed task image: " + classpathLocation, e);
    }
    return "/uploads/tasks/seed/" + filename;
  }

  /**
   * Shuffles {@code input} with a {@link Random} seeded by the hash of
   * {@code seed}. Stable across runs (same seed → same order) but every
   * distinct seed gets a different permutation.
   */
  private <T> List<T> shuffleStable(List<T> input, String seed) {
    List<T> copy = new ArrayList<>(input);
    Collections.shuffle(copy, new Random((long) seed.hashCode()));
    return copy;
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize task content", e);
    }
  }

  private void attachToAllClassrooms(Task task, short order) {
    for (Classroom classroom : classrooms.values()) {
      ClassroomTaskId id = new ClassroomTaskId(classroom.getId(), task.getId());
      if (classroomTaskRepository.existsById(id))
        continue;
      classroomTaskRepository.save(ClassroomTask.builder()
          .id(id).classroom(classroom).task(task).sortOrder(order).isActive(true).build());
    }
  }

  // -----------------------------------------------------------------------
  // Students (one classroom each — unique constraint enforced by DB)
  // -----------------------------------------------------------------------

  private void seedStudents() {
    // {email, displayName, key}
    // student1..5 are clean fixtures with NO task progress so QA can play
    // the full game flow as a fresh user. `tester` is the populated
    // counterpart — all tasks completed — used to verify teacher
    // dashboards / leaderboards / level progression.
    String[][] data = {
        { "anine@nettdetektivene.no", "Agent Anine", "anine" },
        { "nora-s@nettdetektivene.no", "Detektiv Nora", "nora" },
        { "gustav@nettdetektivene.no", "Guru Gustav", "gustav" },
        { "tri@nettdetektivene.no", "Tri-Tech", "tri" },
        { "silje@nettdetektivene.no", "Spion Silje", "silje" },
        { "vetle@nettdetektivene.no", "Vekter Vetle", "vetle" },
        { "regine@nettdetektivene.no", "Radar-Regine", "regine" },
        { "segward@nettdetektivene.no", "Sikker-Segward", "segward" },
        { "studentNoClassroomYet@flataasen.no", "Ny Elev", "no-classroom" },
        // Old seeding hasnt removd bnecasue it wouldt hurt to have
        { "student1@ntnu.no", "Student 1", "student1" },
        { "student2@ntnu.no", "Student 2", "student2" },
        { "student3@ntnu.no", "Student 3", "student3" },
        { "student4@ntnu.no", "Student 4", "student4" },
        { "student5@ntnu.no", "Student 5", "student5" },
        { "tester@ntnu.no", "Demo Tester", "tester" },
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
    for (String key : List.of("student1", "student2", "student3", "student4", "student5", "tester",
      "anine", "nora", "gustav", "tri", "silje", "vetle", "regine", "segward")) {
      enroll(users.get(key), class1);
    }
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

    // Star students: all tasks, score 95-100
    for (String key : List.of("anine", "nora", "gustav")) {
      User u = users.get(key);
      if (hasAttempts(u.getId(), classroomId))
        continue;
      for (int i = 0; i < allTasks.size(); i++) {
        submitAttempt(u.getId(), allTasks.get(i).getId(), classroomId, 95 + (i % 5));
      }
      log.info("Task attempts seeded for {} via sp_submit_attempt", u.getUsername());
    }

    // Good students: first 6 tasks, score 80-92
    for (String key : List.of("tri", "silje")) {
      User u = users.get(key);
      if (hasAttempts(u.getId(), classroomId))
        continue;
      for (int i = 0; i < 6; i++) {
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

    // Notebook reflections live alongside task progress, so they all
    // belong to the demo tester. student1..5 stay empty.
    Object[][] entries = {
        // 5A
        { class1, "anine", "nyhetskvartalet",
            "Jeg lærte at falske nyheter ofte har sensasjonelle overskrifter. Sjekker alltid kilden nå!" },
        { class1, "anine", "fotografen",
            "Bildemanipulasjon er overraskende enkelt. Stoler ikke blindt på bilder lenger." },
        { class1, "nora", "fotografen",
            "Overraskende lett å manipulere bilder. Skal være mer kritisk fremover." },
        { class1, "nora", "datasenteret", "Kryptering er viktig for å beskytte data. Imponerende teknologi!" },
        { class1, "gustav", "datasenteret", "Kryptering beskytter dataene mine. Lærte mye om digital sikkerhet!" },
        { class1, "tri", "fotografen", "Metadata i bilder avslører mer enn jeg trodde. Alltid kritisk til bilder nå." },
        { class1, "silje", "nyhetskvartalet", "Falske nyheter ser ekte ut. Sjekker alltid kilden!" },
        // 6B
        { class1, "regine", "nyhetskvartalet", "Lærte å skille mellom ekte og falske nyheter ved å sjekke kilden." },
        { class1, "regine", "fotografen", "Bilder kan manipuleres enkelt. Ser alltid etter metadata nå." },
        { class1, "vetle", "nyhetskvartalet", "Falske nyheter er vanskeligere å oppdage enn jeg trodde." },
        { class1, "vetle", "datasenteret", "Sikkerhetskopiering er viktig — aldri mer miste data!" },
        // test
        { class1, "tester", "nyhetskvartalet",
            "Jeg lærte at falske nyheter ofte har sensasjonelle overskrifter. Sjekker alltid kilden nå!" },
        { class1, "tester", "fotografen", "Overraskende lett å manipulere bilder. Skal være mer kritisk fremover." },
        { class1, "tester", "datasenteret", "Kryptering er viktig for å beskytte data. Imponerende teknologi!" },
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
          .autoNotes("{\"hints_used\": 1, \"time_spent\": 300}")
          .build());
      log.info("Notebook: {} @ {}", student.getDisplayName(), stopLocation);
    }
  }

  // -----------------------------------------------------------------------
  // Mystery submissions — five pending submissions in class1, one per student
  // -----------------------------------------------------------------------

  private void seedMysterySubmissions() {
    Classroom class1 = classrooms.get("class1");
    if (!mysterySubmissionRepository.findByClassroom_Id(class1.getId()).isEmpty()) {
      log.info("Mystery submissions already seeded for class1, skipping");
      return;
    }

    // {submitterKey, sourceImage, alt0, alt1, alt2, correctIndex}
    Object[][] data = {
        {
            "student1",
            "seed-images/news/fakeArticle1.png",
            "Artikkelen er ekte og publisert av en kjent avis",
            "Artikkelen er en falsk nyhet med oppdiktede kilder",
            "Artikkelen er satire og ikke ment alvorlig",
            (byte) 1,
        },
        {
            "student2",
            "seed-images/news/realArticle2.png",
            "Bildet er et AI-generert portrett",
            "Bildet er manipulert i Photoshop",
            "Bildet er et ekte pressefoto",
            (byte) 2,
        },
        {
            "student3",
            "seed-images/photos/aiPhoto1.png",
            "Bildet er ekte og uredigert",
            "Bildet er AI-generert",
            "Bildet er en collage av flere bilder",
            (byte) 1,
        },
        {
            "student4",
            "seed-images/photos/psPhoto4.png",
            "Bildet er manipulert i Photoshop",
            "Bildet er ekte og uredigert",
            "Bildet er AI-generert",
            (byte) 0,
        },
        {
            "student5",
            "seed-images/news/fakeArticle3.png",
            "Saken har troverdige kilder",
            "Saken er bevisst overdrevet for klikk",
            "Saken er hentet fra en parodiside",
            (byte) 1,
        },
    };

    for (Object[] row : data) {
      User submitter = users.get((String) row[0]);
      String storedPath = copySeedImageToUploads((String) row[1]);
      MysterySubmission submission = MysterySubmission.builder()
          .classroom(class1)
          .userSubmittedBy(submitter)
          .imagePath(storedPath)
          .alternative1((String) row[2])
          .alternative2((String) row[3])
          .alternative3((String) row[4])
          .correctIndex((byte) row[5])
          .mysStatus(MysteryStatus.PENDING)
          .build();
      mysterySubmissionRepository.save(submission);
      log.info("Mystery submission: {} (PENDING)", submitter.getDisplayName());
    }
  }

  /**
   * Copies a classpath seed image into the uploads folder and returns its public
   * URL.
   */
  private String copySeedImageToUploads(String classpathLocation) {
    String filename = Paths.get(classpathLocation).getFileName().toString();
    String batchId = "seed";
    Path targetDir = Paths.get(uploadsRoot).toAbsolutePath().normalize().resolve("mysteries").resolve(batchId);
    Path targetFile = targetDir.resolve(filename);
    try {
      Files.createDirectories(targetDir);
      try (InputStream in = new ClassPathResource(classpathLocation).getInputStream()) {
        Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to copy seed mystery image: " + classpathLocation, e);
    }
    return "/uploads/mysteries/" + batchId + "/" + filename;
  }

  private String copySeedImageForTasks(String classpathLocation) {
    String filename = Paths.get(classpathLocation).getFileName().toString();
    Path targetDir = Paths.get(uploadsRoot).toAbsolutePath().normalize().resolve("tasks").resolve("seed");
    try {
      Files.createDirectories(targetDir);
      try (InputStream in = new ClassPathResource(classpathLocation).getInputStream()) {
        Files.copy(in, targetDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to copy seed task image: " + classpathLocation, e);
    }
    return "/uploads/tasks/seed/" + filename;
  }

  private String buildTaskContent(TaskType type, String stop,
      String fakeArticle, String realArticle, String realPhoto, String fakePhoto) {
    boolean isBoss = "datasenteret".equals(stop);
    if (type == TaskType.NEWS) {
      String question = isBoss
          ? "Datatyven har plantet en falsk nyhet. Hvilken er ekte?"
          : "Hvilken av disse nyhetene er ekte?";
      String hint = isBoss
          ? "Sjekk kilden, forfatteren og datoen — de svakeste signalene avslører løgnen."
          : "Se etter kildehenvisninger og datoer i artiklene.";
      return "{\"question\":\"" + question + "\",\"hint\":\"" + hint + "\","
          + "\"articles\":["
          + "{\"imagePath\":\"" + fakeArticle + "\",\"isReal\":false},"
          + "{\"imagePath\":\"" + realArticle + "\",\"isReal\":true}"
          + "]}";
    } else {
      String description = isBoss
          ? "Datatyven har manipulert bevismateriale. Klassifiser hvert bilde riktig."
          : "Hvilken type bilde er dette?";
      String d1 = isBoss ? "Bilde fra et serverrom" : "Bilde av en by ved solnedgang";
      String d2 = isBoss ? "Portrett av en mistenkt" : "Portrett av en person utendørs";
      String d3 = isBoss ? "Bilde fra et åsted" : "Folkemengde på et torg";
      String b2 = isBoss ? "Urealistiske detaljer" : "Urealistiske detaljer i bakgrunnen";
      return "{\"description\":\"" + description + "\",\"alternatives\":["
          + "{\"imagePath\":\"" + realPhoto + "\",\"description\":\"" + d1 + "\","
          + "\"correctType\":\"ekte\","
          + "\"begrunnelser\":[\"Naturlig lysforhold\",\"Konsistente skygger\",\"Ingen synlige artefakter\"]},"
          + "{\"imagePath\":\"" + fakePhoto + "\",\"description\":\"" + d2 + "\","
          + "\"correctType\":\"ai-generert\","
          + "\"begrunnelser\":[\"" + b2 + "\",\"Feil antall fingre\",\"Unaturlig tekstur på huden\"]},"
          + "{\"imagePath\":\"" + fakePhoto + "\",\"description\":\"" + d3 + "\","
          + "\"correctType\":\"manipulert\","
          + "\"begrunnelser\":[\"Inkonsistente skygger\",\"Synlig kloning av personer\",\"Feil perspektiv\"]}"
          + "]}";
    }
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
        { "Første mysterium løst", "Løs ditt første ukens mysterium", "🔍", "mystery_solved", "first" },
        { "Sikkerhetsmester", "Fullfør alle oppgaver i et klasserom", "🏆", "all_tasks_completed", "classroom" },
        { "Plettfri Detektiv", "Fullfør alle oppgaver i et klasserom uten å feile", "🛡️", "perfect_classroom",
            "classroom" },
        { "Aldri Gi Opp", "Feil 10 oppgaveforsøk", "💪", "tasks_failed", "10" },
        { "Bidragsyter", "Send inn ditt første ukens mysterium", "✍️", "mystery_submitted", "first" },
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
    log.info("Instructors  : laerer@flataasen.no, laerer@byaasen.no");
    log.info("Classrooms   : {}", classrooms.size());
    log.info("Tasks        : {} (3 nyhetskvartalet + 3 fotografen + 5 datasenteret)", allTasks.size());
    log.info("Students     : 9 (1 without classroom: studentNoClassroomYet@flataasen.no)");
    log.info("");
    log.info("LOGIN (password: password123)");
    log.info("  Teacher : laerer@flataasen.no");
    log.info("  Student : anine@nettdetektivene.no  (star — all tasks done)");
    log.info("  Student : tri@nettdetektivene.no    (good — partial tasks done)");
    log.info("  Student : segward@nettdetektivene.no (no attempts)");
    log.info("");
    log.info("CLASSROOM JOIN CODES:");
    classrooms.forEach((key, c) -> log.info("  {} : {}", c.getTitle(), c.getJoinCode()));
    log.info("========================================");
  }

  private void verifyInstructorAssignments() {
    log.info("Verifying instructor classroom assignments...");
    List<String> instructorKeys = List.of("nora", "lars");
    for (String key : instructorKeys) {
      User instructor = users.get(key);
      List<ntnu.no.backend.model.classroom.ClassroomInstructor> assignments =
          classroomInstructorRepository.findAllByUser(instructor);
      if (assignments.isEmpty()) {
        log.error("  ERROR: Instructor {} has NO classroom assignments!", instructor.getDisplayName());
      } else {
        log.info("  Instructor {} -> {} classroom(s): {}",
            instructor.getDisplayName(), assignments.size(),
            assignments.stream()
                .map(ci -> ci.getClassroom().getTitle())
                .toList());
      }
    }
    log.info("Instructor assignment verification complete.");
    log.info("");
  }

  private void verifyEnrollments() {
    log.info("");
    log.info("Verifying student enrollments...");
    List<String> studentKeys = List.of("anine", "nora", "gustav", "tri", "silje", "vetle", "regine", "segward",
        "no-classroom");
    // List<String> studentKeys = List.of("student1", "student2", "student3",
    // "student4", "student5", "tester");
    for (String key : studentKeys) {
      User student = users.get(key);
      ClassroomStudent membership = classroomStudentRepository.findByUser(student).orElse(null);
      if (membership == null) {
        if ("no-classroom".equals(key)) {
          log.info("  Student {} -> no classroom (intentional)", student.getDisplayName());
        } else {
          log.error("  ERROR: Student {} has no classroom membership!", key);
        }
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
      List<ClassroomTask> tasks = classroomTaskRepository
          .findAllByClassroom_IdAndIsActiveTrueOrderBySortOrderAsc(classroom.getId());
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
