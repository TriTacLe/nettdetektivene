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
import java.util.UUID;

/**
 * Stores the single image attached to a mystery submission.
 *
 * <p>Mirrors {@link TaskImageStorageService} but simplified for the
 * single-file case. The returned path is the public URL (e.g.
 * {@code /uploads/mysteries/{batchId}/{uuid}.jpg}) served by the existing
 * static-resource handler at {@code /uploads/**}.</p>
 */
@Service
public class MysteryImageStorageService {

  private final Path uploadsRoot;

  public MysteryImageStorageService(@Value("${app.uploads.root:uploads}") String uploadsRoot) {
    this.uploadsRoot = Paths.get(uploadsRoot).toAbsolutePath().normalize();
  }

  /**
   * Stores the image and returns its public URL.
   *
   * @param image the multipart image to persist
   * @return the public path under {@code /uploads/mysteries/...}
   */
  public String storeImage(MultipartFile image) {
    if (image == null || image.isEmpty()) {
      throw new IllegalArgumentException("Mystery image is required");
    }
    validateImage(image);

    String originalFilename = StringUtils.cleanPath(image.getOriginalFilename() == null ? "" : image.getOriginalFilename());
    if (originalFilename.isBlank()) {
      throw new IllegalArgumentException("Uploaded image must have a filename");
    }

    String batchId = UUID.randomUUID().toString();
    String storedFilename = UUID.randomUUID() + getExtension(originalFilename);
    Path targetDirectory = uploadsRoot.resolve("mysteries").resolve(batchId);
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

    return "/uploads/mysteries/" + batchId + "/" + storedFilename;
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
