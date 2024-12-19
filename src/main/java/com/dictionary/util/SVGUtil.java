package com.dictionary.util;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

public class SVGUtil {
    public static ImageIcon loadSVGIcon(String path, int width, int height, Color color) {
        try (InputStream inputStream = SVGUtil.class.getClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                System.err.println("Could not find SVG file: " + path);
                return null;
            }

            // 读取SVG文件内容
            byte[] bytes = inputStream.readAllBytes();
            String svgContent = new String(bytes, StandardCharsets.UTF_8);

            // 创建SVG文档
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
            Document document = factory.createDocument(null, new StringReader(svgContent));

            // 创建PNG转码器
            PNGTranscoder transcoder = new PNGTranscoder() {
                @Override
                protected void setImageSize(float width, float height) {
                    super.setImageSize(width, height);
                }
            };

            // 设置图像大小
            transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, (float) width);
            transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, (float) height);

            // 转换SVG为PNG
            TranscoderInput input = new TranscoderInput(document);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            TranscoderOutput output = new TranscoderOutput(outputStream);
            transcoder.transcode(input, output);

            // 创建图像
            byte[] imgData = outputStream.toByteArray();
            BufferedImage originalImage = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(imgData));

            // 如果需要改变颜色
            if (color != null) {
                BufferedImage coloredImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = coloredImage.createGraphics();
                g.drawImage(originalImage, 0, 0, null);
                g.setComposite(AlphaComposite.SrcAtop);
                g.setColor(color);
                g.fillRect(0, 0, originalImage.getWidth(), originalImage.getHeight());
                g.dispose();
                return new ImageIcon(coloredImage);
            }

            return new ImageIcon(originalImage);
        } catch (IOException | TranscoderException e) {
            e.printStackTrace();
            return null;
        }
    }
} 