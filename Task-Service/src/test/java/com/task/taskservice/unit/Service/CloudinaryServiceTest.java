package com.task.taskservice.unit.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.Uploader;
import com.cloudinary.Url;
import com.cloudinary.utils.ObjectUtils;
import com.task.taskservice.Service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CloudinaryServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @Mock
    private Url url;

    @InjectMocks
    private CloudinaryService cloudinaryService;

    @BeforeEach
    void setUp() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);

        // Mock the uploader() method to return the mocked Uploader
        when(cloudinary.uploader()).thenReturn(uploader);

        // Mock the url() method to return the mocked Url
        when(cloudinary.url()).thenReturn(url);
    }

    @Test
    void uploadFile_shouldReturnUploadResponse_whenFileIsValid() throws IOException {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        byte[] fileBytes = "test content".getBytes();
        when(file.getBytes()).thenReturn(fileBytes);

        Map<String, Object> uploadResponse = new HashMap<>();
        uploadResponse.put("public_id", "AGILIA/test-file");
        uploadResponse.put("url", "https://res.cloudinary.com/test/test-file");
        uploadResponse.put("resource_type", "raw");

        when(uploader.upload(eq(fileBytes), anyMap())).thenReturn(uploadResponse);

        // Act
        Map result = cloudinaryService.uploadFile(file);

        // Assert
        assertNotNull(result);
        assertEquals("AGILIA/test-file", result.get("public_id"));
        assertEquals("https://res.cloudinary.com/test/test-file", result.get("url"));
        assertEquals("raw", result.get("resource_type"));

        // Verify interactions
        verify(file).getBytes();
        verify(uploader).upload(eq(fileBytes), argThat(map ->
                map.containsKey("folder") && "AGILIA".equals(map.get("folder")) &&
                        map.containsKey("access_mode") && "authenticated".equals(map.get("access_mode")) &&
                        map.containsKey("resource_type") && "raw".equals(map.get("resource_type"))
        ));
    }

    @Test
    void uploadFile_shouldThrowIOException_whenUploadFails() throws IOException {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenReturn("test content".getBytes());
        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new IOException("Upload failed"));

        // Act & Assert
        assertThrows(IOException.class, () -> cloudinaryService.uploadFile(file), "Upload failed");

        // Verify interactions
        verify(file).getBytes();
        verify(uploader).upload(any(byte[].class), anyMap());
    }

    @Test
    void deleteFile_shouldReturnDeleteResponse_whenPublicIdIsValid() throws IOException {
        // Arrange
        String publicId = "AGILIA/test-file";
        Map<String, Object> deleteResponse = new HashMap<>();
        deleteResponse.put("result", "ok");

        when(uploader.destroy(eq(publicId), anyMap())).thenReturn(deleteResponse);

        // Act
        Map result = cloudinaryService.deleteFile(publicId);

        // Assert
        assertNotNull(result);
        assertEquals("ok", result.get("result"));

        // Verify interactions
        verify(uploader).destroy(eq(publicId), eq(ObjectUtils.emptyMap()));
    }

    @Test
    void deleteFile_shouldThrowIOException_whenDeletionFails() throws IOException {
        // Arrange
        String publicId = "AGILIA/test-file";
        when(uploader.destroy(eq(publicId), anyMap())).thenThrow(new IOException("Deletion failed"));

        // Act & Assert
        assertThrows(IOException.class, () -> cloudinaryService.deleteFile(publicId), "Deletion failed");

        // Verify interactions
        verify(uploader).destroy(eq(publicId), eq(ObjectUtils.emptyMap()));
    }

    @Test
    void generateSignedUrl_shouldReturnSignedUrl_whenPublicIdIsValid() {
        // Arrange
        String publicId = "AGILIA/test-file";
        String expectedUrl = "https://res.cloudinary.com/test/signed-url/test-file";

        // Mock Url configuration chain
        when(url.resourceType("raw")).thenReturn(url);
        when(url.secure(true)).thenReturn(url);
        when(url.signed(true)).thenReturn(url);
        when(url.publicId(publicId)).thenReturn(url);
        when(url.generate()).thenReturn(expectedUrl);

        // Act
        String result = cloudinaryService.generateSignedUrl(publicId);

        // Assert
        assertEquals(expectedUrl, result);

        // Verify interactions
        verify(cloudinary).url();
        verify(url).resourceType("raw");
        verify(url).secure(true);
        verify(url).signed(true);
        verify(url).publicId(publicId);
        verify(url).generate();
    }
}