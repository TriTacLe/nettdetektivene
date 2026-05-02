package ntnu.no.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Unit tests for {@link TaskImageStorageService}.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
class TaskImageStorageServiceTest {

  private final TaskImageStorageService taskImageStorageService = new TaskImageStorageService("test-uploads");

  @Test
  void replaceUploadReferences_withNullContent_returnsNull() {
    String result = taskImageStorageService.replaceUploadReferences(null, List.of());
    assertNull(result);
  }

  @Test
  void replaceUploadReferences_withEmptyContent_returnsEmpty() {
    String result = taskImageStorageService.replaceUploadReferences("", List.of());
    assertEquals("", result);
  }

  @Test
  void replaceUploadReferences_withBlankContent_returnsBlank() {
    String result = taskImageStorageService.replaceUploadReferences("   ", List.of());
    assertEquals("   ", result);
  }

  @Test
  void replaceUploadReferences_withNoImages_returnsOriginalContent() {
    String content = "Task description with no images";
    String result = taskImageStorageService.replaceUploadReferences(content, List.of());
    assertEquals(content, result);
  }

  @Test
  void replaceUploadReferences_withNullImages_returnsOriginalContent() {
    String content = "Task description";
    String result = taskImageStorageService.replaceUploadReferences(content, null);
    assertEquals(content, result);
  }

  @Test
  void replaceUploadReferences_withEmptyImages_returnsOriginalContent() {
    String content = "Task description";
    String result = taskImageStorageService.replaceUploadReferences(content, List.of());
    assertEquals(content, result);
  }

  @Test
  void replaceUploadReferences_withMissingImageReference_throwsIllegalArgumentException() {
    String content = "Task with upload://missing.png reference";
    MultipartFile mockFile = createMockImageFile("existing.png", "image/png");

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      taskImageStorageService.replaceUploadReferences(content, List.of(mockFile));
    });

    assertTrue(exception.getMessage().contains("Missing uploaded image for reference: missing.png"));
  }

  @Test
  void replaceUploadReferences_withInvalidContentType_throwsIllegalArgumentException() {
    MultipartFile mockFile = createMockFile("test.txt", "text/plain");

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      taskImageStorageService.replaceUploadReferences("content", List.of(mockFile));
    });

    assertTrue(exception.getMessage().contains("Only image uploads are supported"));
  }

  @Test
  void replaceUploadReferences_withNullContentType_throwsIllegalArgumentException() {
    MultipartFile mockFile = createMockFile("test.png", null);

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      taskImageStorageService.replaceUploadReferences("content", List.of(mockFile));
    });

    assertTrue(exception.getMessage().contains("Only image uploads are supported"));
  }

  @Test
  void replaceUploadReferences_withNonImageContentType_throwsIllegalArgumentException() {
    MultipartFile mockFile = createMockFile("test.pdf", "application/pdf");

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      taskImageStorageService.replaceUploadReferences("content", List.of(mockFile));
    });

    assertTrue(exception.getMessage().contains("Only image uploads are supported"));
  }

  @Test
  void replaceUploadReferences_withEmptyFilename_throwsIllegalArgumentException() {
    MultipartFile mockFile = createMockImageFile("", "image/png");

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      taskImageStorageService.replaceUploadReferences("content", List.of(mockFile));
    });

    assertTrue(exception.getMessage().contains("Uploaded image must have a filename"));
  }

  @Test
  void replaceUploadReferences_withBlankFilename_throwsIllegalArgumentException() {
    MultipartFile mockFile = createMockImageFile("   ", "image/png");

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      taskImageStorageService.replaceUploadReferences("content", List.of(mockFile));
    });

    assertTrue(exception.getMessage().contains("Uploaded image must have a filename"));
  }

  @Test
  void replaceUploadReferences_withDuplicateFilenames_throwsIllegalArgumentException() {
    MultipartFile file1 = createMockImageFile("duplicate.png", "image/png");
    MultipartFile file2 = createMockImageFile("duplicate.png", "image/png");

    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      taskImageStorageService.replaceUploadReferences("content", List.of(file1, file2));
    });

    assertTrue(exception.getMessage().contains("Duplicate uploaded image filename: duplicate.png"));
  }

  @Test
  void replaceUploadReferences_withValidImage_replacesReference() {
    String content = "Task with image: upload://test.png";
    MultipartFile mockFile = createMockImageFile("test.png", "image/png");

    String result = taskImageStorageService.replaceUploadReferences(content, List.of(mockFile));

    assertFalse(result.contains("upload://test.png"));
    assertTrue(result.contains("/uploads/tasks/"));
    assertTrue(result.contains(".png"));
  }

  @Test
  void replaceUploadReferences_withMultipleReferences_replacesAll() {
    String content = "Task with \"upload://image1.png\" and \"upload://image2.png\"";
    MultipartFile file1 = createMockImageFile("image1.png", "image/png");
    MultipartFile file2 = createMockImageFile("image2.png", "image/jpg");

    String result = taskImageStorageService.replaceUploadReferences(content, List.of(file1, file2));

    assertFalse(result.contains("upload://"));
    assertTrue(result.contains("/uploads/tasks/"));
  }

  @Test
  void replaceUploadReferences_preservesContentWithoutUploadReferences() {
    String content = "Task description with no references";
    MultipartFile mockFile = createMockImageFile("image.png", "image/png");

    String result = taskImageStorageService.replaceUploadReferences(content, List.of(mockFile));

    assertEquals(content, result);
  }

  @Test
  void replaceUploadReferences_withContentContainingMultipleUploadReferences() {
    String content = "\"upload://first.png\" \"upload://second.png\"";
    MultipartFile file1 = createMockImageFile("first.png", "image/png");
    MultipartFile file2 = createMockImageFile("second.png", "image/jpeg");

    String result = taskImageStorageService.replaceUploadReferences(content, List.of(file1, file2));

    assertFalse(result.contains("upload://first.png"));
    assertFalse(result.contains("upload://second.png"));
    assertTrue(result.contains("/uploads/tasks/"));
  }

  @Test
  void replaceUploadReferences_withSpecialCharactersInFilename() {
    String content = "Image: upload://test-image_2024.png";
    MultipartFile mockFile = createMockImageFile("test-image_2024.png", "image/png");

    String result = taskImageStorageService.replaceUploadReferences(content, List.of(mockFile));

    assertFalse(result.contains("upload://test-image_2024.png"));
    assertTrue(result.contains("/uploads/tasks/"));
  }

  @Test
  void replaceUploadReferences_createsStorageDirectory() throws IOException {
    String content = "Task with image: upload://test.png";
    MultipartFile mockFile = createMockImageFile("test.png", "image/png");

    taskImageStorageService.replaceUploadReferences(content, List.of(mockFile));

    Path uploadsRoot = Paths.get("test-uploads").toAbsolutePath().normalize();
    assertTrue(Files.exists(uploadsRoot.resolve("tasks")));

    // Clean up
    Files.walk(uploadsRoot)
      .sorted((a, b) -> -a.compareTo(b))
      .forEach(path -> {
        try {
          Files.delete(path);
        } catch (IOException e) {
          // Ignore cleanup failures
        }
      });
  }

  private MultipartFile createMockImageFile(String filename, String contentType) {
    return new MultipartFile() {
      @Override
      public String getName() {
        return "file";
      }

      @Override
      public String getOriginalFilename() {
        return filename;
      }

      @Override
      public String getContentType() {
        return contentType;
      }

      @Override
      public boolean isEmpty() {
        return false;
      }

      @Override
      public long getSize() {
        return 1024;
      }

      @Override
      public byte[] getBytes() throws IOException {
        return "test image content".getBytes(StandardCharsets.UTF_8);
      }

      @Override
      public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream("test image content".getBytes(StandardCharsets.UTF_8));
      }

      @Override
      public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
        throw new UnsupportedOperationException();
      }
    };
  }

  private MultipartFile createMockFile(String filename, String contentType) {
    return new MultipartFile() {
      @Override
      public String getName() {
        return "file";
      }

      @Override
      public String getOriginalFilename() {
        return filename;
      }

      @Override
      public String getContentType() {
        return contentType;
      }

      @Override
      public boolean isEmpty() {
        return false;
      }

      @Override
      public long getSize() {
        return 1024;
      }

      @Override
      public byte[] getBytes() throws IOException {
        return "test content".getBytes(StandardCharsets.UTF_8);
      }

      @Override
      public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream("test content".getBytes(StandardCharsets.UTF_8));
      }

      @Override
      public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
        throw new UnsupportedOperationException();
      }
    };
  }
}