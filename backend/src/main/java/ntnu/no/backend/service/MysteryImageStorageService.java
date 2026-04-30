package ntnu.no.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
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

  /** Hard cap on stored mystery images. Spring's multipart limit is the outer guard. */
  private static final long MAX_BYTES = 10L * 1024 * 1024;

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
    if (image.getSize() > MAX_BYTES) {
      throw new IllegalArgumentException("Image exceeds maximum allowed size");
    }

    // Verify the bytes actually decode as an image. Guards against a client
    // labelling arbitrary bytes with image/* in the Content-Type header.
    BufferedImage decoded;
    try (InputStream in = image.getInputStream()) {
      decoded = ImageIO.read(in);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not read uploaded image", e);
    }
    if (decoded == null) {
      throw new IllegalArgumentException("Uploaded file is not a valid image");
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
