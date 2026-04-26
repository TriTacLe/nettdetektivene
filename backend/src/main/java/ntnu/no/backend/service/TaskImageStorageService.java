package ntnu.no.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stores uploaded task images and rewrites content placeholders to public URLs.
 */
@Service
public class TaskImageStorageService {

  private static final Pattern UPLOAD_REFERENCE_PATTERN = Pattern.compile("upload://([^\"\\\\]+)");

  private final Path uploadsRoot;

  public TaskImageStorageService(@Value("${app.uploads.root:uploads}") String uploadsRoot) {
    this.uploadsRoot = Paths.get(uploadsRoot).toAbsolutePath().normalize();
  }

  /**
   * Replaces {@code upload://filename.ext} references in task content with stored file URLs.
   *
   * @param content the task content JSON string
   * @param images  uploaded image files
   * @return content with resolved image URLs
   */
  public String replaceUploadReferences(String content, List<MultipartFile> images) {
    if (images == null || images.isEmpty() || content == null || content.isBlank()) {
      return content;
    }

    Map<String, String> storedPathsByOriginalFilename = storeImages(images);
    Matcher matcher = UPLOAD_REFERENCE_PATTERN.matcher(content);
    StringBuffer rewritten = new StringBuffer();

    while (matcher.find()) {
      String referencedFilename = matcher.group(1);
      String storedPath = storedPathsByOriginalFilename.get(referencedFilename);
      if (storedPath == null) {
        throw new IllegalArgumentException("Missing uploaded image for reference: " + referencedFilename);
      }
      matcher.appendReplacement(rewritten, Matcher.quoteReplacement(storedPath));
    }

    matcher.appendTail(rewritten);
    return rewritten.toString();
  }

  private Map<String, String> storeImages(List<MultipartFile> images) {
    String batchId = UUID.randomUUID().toString();
    Map<String, String> storedPathsByOriginalFilename = new HashMap<>();

    for (MultipartFile image : images) {
      if (image == null || image.isEmpty()) {
        continue;
      }

      validateImage(image);

      String originalFilename = StringUtils.cleanPath(image.getOriginalFilename());
      if (originalFilename.isBlank()) {
        throw new IllegalArgumentException("Uploaded image must have a filename");
      }
      if (storedPathsByOriginalFilename.containsKey(originalFilename)) {
        throw new IllegalArgumentException("Duplicate uploaded image filename: " + originalFilename);
      }

      String storedFilename = UUID.randomUUID() + getExtension(originalFilename);
      Path targetDirectory = uploadsRoot.resolve("tasks").resolve(batchId);
      Path targetFile = targetDirectory.resolve(storedFilename).normalize();

      if (!targetFile.startsWith(targetDirectory)) {
        throw new IllegalArgumentException("Invalid uploaded image filename: " + originalFilename);
      }

      try {
        Files.createDirectories(targetDirectory);
        Files.copy(image.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        throw new UncheckedIOException("Failed to store uploaded image: " + originalFilename, e);
      }

      storedPathsByOriginalFilename.put(originalFilename, "/uploads/tasks/" + batchId + "/" + storedFilename);
    }

    return storedPathsByOriginalFilename;
  }

  private void validateImage(MultipartFile image) {
    String contentType = image.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new IllegalArgumentException("Only image uploads are supported");
    }
  }

  private String getExtension(String filename) {
    int extensionStart = filename.lastIndexOf('.');
    if (extensionStart < 0) {
      return "";
    }
    return filename.substring(extensionStart);
  }
}
