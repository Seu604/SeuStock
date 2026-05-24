package com.seu.seustock.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ImageAnalysisServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    private ImageAnalysisService service;
    private Method resizeForAnalysis;

    @BeforeEach
    void setUp() throws Exception {
        org.mockito.Mockito.when(chatClientBuilder.build()).thenReturn(chatClient);
        service = new ImageAnalysisService(chatClientBuilder);

        resizeForAnalysis = ImageAnalysisService.class.getDeclaredMethod(
                "resizeForAnalysis", byte[].class, String.class);
        resizeForAnalysis.setAccessible(true);
    }

    private byte[] createImageBytes(String format, int width, int height) throws Exception {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, format, baos);
        return baos.toByteArray();
    }

    private Object invokeResize(byte[] bytes, String mimeType) throws Exception {
        return resizeForAnalysis.invoke(service, bytes, mimeType);
    }

    private byte[] getBytes(Object resizedImage) throws Exception {
        return (byte[]) resizedImage.getClass().getMethod("bytes").invoke(resizedImage);
    }

    private String getMimeType(Object resizedImage) throws Exception {
        return (String) resizedImage.getClass().getMethod("mimeType").invoke(resizedImage);
    }

    @Test
    void smallPng_convertsToJpeg() throws Exception {
        byte[] png = createImageBytes("png", 100, 100);

        Object result = invokeResize(png, "image/png");

        assertThat(getMimeType(result)).isEqualTo("image/jpeg");
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(getBytes(result)));
        assertThat(decoded).isNotNull();
        assertThat(decoded.getWidth()).isEqualTo(100);
        assertThat(decoded.getHeight()).isEqualTo(100);
    }

    @Test
    void largePng_resizesAndConvertsToJpeg() throws Exception {
        byte[] png = createImageBytes("png", 2000, 1500);

        Object result = invokeResize(png, "image/png");

        assertThat(getMimeType(result)).isEqualTo("image/jpeg");
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(getBytes(result)));
        assertThat(decoded).isNotNull();
        assertThat(decoded.getWidth()).isLessThanOrEqualTo(1024);
        assertThat(decoded.getHeight()).isLessThanOrEqualTo(1024);
        assertThat(Math.max(decoded.getWidth(), decoded.getHeight())).isEqualTo(1024);
    }

    @Test
    void smallGif_convertsToJpeg() throws Exception {
        byte[] gif = createImageBytes("gif", 200, 150);

        Object result = invokeResize(gif, "image/gif");

        assertThat(getMimeType(result)).isEqualTo("image/jpeg");
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(getBytes(result)));
        assertThat(decoded).isNotNull();
        assertThat(decoded.getWidth()).isEqualTo(200);
        assertThat(decoded.getHeight()).isEqualTo(150);
    }

    @Test
    void largeJpeg_resizesAndConvertsToJpeg() throws Exception {
        byte[] jpeg = createImageBytes("jpeg", 1500, 1500);

        Object result = invokeResize(jpeg, "image/jpeg");

        assertThat(getMimeType(result)).isEqualTo("image/jpeg");
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(getBytes(result)));
        assertThat(decoded).isNotNull();
        assertThat(decoded.getWidth()).isLessThanOrEqualTo(1024);
        assertThat(decoded.getHeight()).isLessThanOrEqualTo(1024);
        assertThat(Math.max(decoded.getWidth(), decoded.getHeight())).isEqualTo(1024);
    }

    @Test
    void imageAtExactMaxSide_convertsToJpegWithoutResize() throws Exception {
        byte[] png = createImageBytes("png", 1024, 768);

        Object result = invokeResize(png, "image/png");

        assertThat(getMimeType(result)).isEqualTo("image/jpeg");
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(getBytes(result)));
        assertThat(decoded).isNotNull();
        assertThat(decoded.getWidth()).isEqualTo(1024);
        assertThat(decoded.getHeight()).isEqualTo(768);
    }
}
