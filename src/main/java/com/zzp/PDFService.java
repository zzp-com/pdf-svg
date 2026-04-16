package com.zzp;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;

/**
 * @description: pdf 处理
 * @author: zengzhiping
 * @create: 2026-03-30
 **/
public class PDFService {

    // 高清 DPI 设置 (300 DPI 为印刷级高清)
//    private  int DPI = 300;



//    //背景颜色
//    private  int BLUEPRINT_BG_RED = 173;
//    private  int BLUEPRINT_BG_GREEN = 229;
//    private  int BLUEPRINT_BG_BLUE = 254;
//
//
//    // 目标颜色 #005B82 (深蓝色)
//    private  int targetRed = 0x00;
//    private  int targetGreen = 0x5B;
//    private  int targetBlue = 0x82;


    private final int DPI ;

    //背景颜色
    private final  int BLUEPRINT_BG_RED ;
    private final int BLUEPRINT_BG_GREEN ;
    private final int BLUEPRINT_BG_BLUE ;


    // 目标颜色 #005B82 (深蓝色)
    private final int targetRed ;
    private final  int targetGreen ;
    private final int targetBlue ;

    public PDFService(int dpi, String bgColorHex,String textColorHex) {
        DPI = dpi;

        int bgRgb = Integer.parseInt(bgColorHex.replace("#", ""), 16);
        BLUEPRINT_BG_RED = (bgRgb >> 16) & 0xFF;
        BLUEPRINT_BG_GREEN = (bgRgb >> 8) & 0xFF;
        BLUEPRINT_BG_BLUE = bgRgb & 0xFF;

        int textRgb = Integer.parseInt(textColorHex.replace("#", ""), 16);
        targetRed = (textRgb >> 16) & 0xFF;
        targetGreen = (textRgb >> 8) & 0xFF;
        targetBlue = textRgb & 0xFF;
    }

    public static void main(String[] args) throws Exception {
        String inputPath = args[0];
        String outputPath = args[1];
        String bgColorHex = args[2];    // #FFFFFF
        String textColorHex = args[3];  // #000000
        int dpi = Integer.parseInt(args[4]);  // 300 或 600
        PDFService service = new PDFService(dpi, bgColorHex,textColorHex);
        service.convertPdfToSvg(inputPath, outputPath);
    }

    /**
     * 将 PDF 文件转换为 SVG
     *
     * @param pdfPath PDF 文件路径
     * @param svgPath 输出 SVG 文件路径
     * @throws Exception 转换异常
     */
    private void convertPdfToSvg(String pdfPath, String svgPath) throws Exception {
        File pdfFile = new File(pdfPath);
        if (!pdfFile.exists()) {
            throw new FileNotFoundException("PDF 文件不存在：" + pdfPath);
        }

        try (PDDocument document = PDDocument.load(pdfFile)) {
            int totalPages = document.getNumberOfPages();
            System.out.println("PDF 总页数：" + totalPages);

            StringBuilder svgContent = new StringBuilder();
            svgContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

            PageInfo pageInfo = getPageInfo(document, 0);

            for (int page = 0; page < totalPages; page++) {
                String imageData = processPage(document, page);
                if (page == 0) {
                    svgContent.append(String.format(
                            "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
                                    "width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">\n",
                            pageInfo.width, pageInfo.height, pageInfo.width, pageInfo.height));


                    // 纯色背景，不需要渐变
                    svgContent.append(String.format(
                            "  <rect width=\"100%%\" height=\"100%%\" fill=\"#%02X%02X%02X\"/>\n",
                            BLUEPRINT_BG_RED, BLUEPRINT_BG_GREEN, BLUEPRINT_BG_BLUE));


                    svgContent.append("  <image x=\"0\" y=\"0\" width=\"").append(pageInfo.width)
                            .append("\" height=\"").append(pageInfo.height).append("\" xlink:href=\"")
                            .append(imageData).append("\"/>\n");
                }
            }

            svgContent.append("</svg>");

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(svgPath))) {
                writer.write(svgContent.toString());
            }
        }
    }

    /**
     * 处理单个 PDF 页面，返回 Base64 图像数据（蓝图反色效果）
     */
    private String processPage(PDDocument document, int pageIndex) throws Exception {
        PDFRenderer renderer = new PDFRenderer(document);
        BufferedImage image = renderer.renderImageWithDPI(pageIndex, DPI);

        BufferedImage blueprintImage = createBlueprintEffect(image);
        blueprintImage = cropBlankArea(blueprintImage);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(blueprintImage, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        System.out.println("已处理第 " + (pageIndex + 1) + " 页，尺寸：" + blueprintImage.getWidth() + "x" + blueprintImage.getHeight() + "（蓝图效果，已裁剪）");

        return "data:image/png;base64," + base64Image;
    }

    /**
     * 裁剪掉顶部和左侧的空白区域
     */
    private BufferedImage cropBlankArea(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();

        int topCrop = 0;
        int leftCrop = 0;

        for (int y = 0; y < height; y++) {
            boolean hasContent = false;
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                if (red > 200 && green > 200 && blue > 200) {
                    hasContent = true;
                    break;
                }
            }
            if (hasContent) {
                topCrop = y;
                break;
            }
        }

        for (int x = 0; x < width; x++) {
            boolean hasContent = false;
            for (int y = topCrop; y < height; y++) {
                int rgb = source.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                if (red > 200 && green > 200 && blue > 200) {
                    hasContent = true;
                    break;
                }
            }
            if (hasContent) {
                leftCrop = x;
                break;
            }
        }

        if (topCrop > 0 || leftCrop > 0) {
            int cropWidth = width - leftCrop;
            int cropHeight = height - topCrop;
            return source.getSubimage(leftCrop, topCrop, cropWidth, cropHeight);
        }

        return source;
    }

    private BufferedImage createBlueprintEffect(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();


        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);

                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                int brightness = (int) (red * 0.299 + green * 0.587 + blue * 0.114);

                // 修正：亮度越低（越黑），alpha 越高（越不透明）
                // 亮度范围 0-255，直接作为不透明度
                int alpha = 255 - brightness;

                // 阈值处理：太亮的像素设为完全透明
                if (brightness > 200) {
                    alpha = 0;
                } else {
                    // 增强对比度：让线条更清晰
                    alpha = Math.min(255, alpha + 50);
                }

                result.setRGB(x, y, (alpha << 24) | (targetRed << 16) | (targetGreen << 8) | targetBlue);
            }
        }

        return result;
    }

    /**
     * 获取页面信息
     */
    private PageInfo getPageInfo(PDDocument document, int pageIndex) throws IOException {
        var page = document.getPage(pageIndex);
        float width = page.getMediaBox().getWidth();
        float height = page.getMediaBox().getHeight();

        float scale = DPI / 72.0f;
        int scaledWidth = Math.round(width * scale);
        int scaledHeight = Math.round(height * scale);

        return new PageInfo(scaledWidth, scaledHeight);
    }

    /**
     * 页面信息类
     */
    private static class PageInfo {
        int width;
        int height;

        PageInfo(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

//    public static void main(String[] args) throws Exception {
//
//
//        PDFService service = new PDFService(300, "#FFFFFF", "#000000");
//
//
//
//
////        service.convertPdfToSvg("src/main/java/com/zzp/pdf/1号引水隧道.pdf", "src/main/java/com/zzp/newsvg/1号引水隧道.svg");
////        service.convertPdfToSvg("src/main/java/com/zzp/pdf/2号引水隧道.pdf", "src/main/java/com/zzp/newsvg/2号引水隧道.svg");
////        service.convertPdfToSvg("src/main/java/com/zzp/pdf/交通洞1-3.pdf", "src/main/java/com/zzp/newsvg/交通洞1至3.svg");
////        service.convertPdfToSvg("src/main/java/com/zzp/pdf/交通洞2明路与4-6隧道.pdf", "src/main/java/com/zzp/newsvg/交通洞2明路与4-6隧道.svg");
////
////
////        service.convertPdfToSvg("src/main/java/com/zzp/pdf/上水库加标注.pdf", "src/main/java/com/zzp/newsvg/上水库加标注.svg");
////        service.convertPdfToSvg("src/main/java/com/zzp/pdf/下水库加标注.pdf", "src/main/java/com/zzp/newsvg/下水库加标注.svg");
////
////        service.convertPdfToSvg("src/main/java/com/zzp/pdf/主厂房.pdf", "src/main/java/com/zzp/newsvg/主厂房.svg");
////        service.convertPdfToSvg("src/main/java/com/zzp/pdf/主变洞.pdf", "src/main/java/com/zzp/newsvg/主变洞.svg");
////        service.convertPdfToSvg("src/main/java/com/zzp/pdf/尾闸洞.pdf", "src/main/java/com/zzp/newsvg/尾闸洞.svg");
//    }
}
