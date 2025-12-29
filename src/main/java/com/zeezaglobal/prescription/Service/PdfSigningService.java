package com.zeezaglobal.prescription.Service;



import com.zeezaglobal.prescription.PDF.CreateSignature;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSigProperties;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSignDesigner;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.KeyStore;
import java.util.Calendar;

@Service
public class PdfSigningService {

    public void signPdf(InputStream inputPdf, OutputStream outputPdf, InputStream keystoreStream, char[] pin, String doctorName) throws Exception {
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(keystoreStream, pin);

        byte[] pdfBytes = inputPdf.readAllBytes();

        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {

            // 1. Create Signature Metadata
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName(doctorName);
            signature.setLocation("IndigoRx Medical Center");
            signature.setReason("Prescription Verification");
            signature.setSignDate(Calendar.getInstance());

            // 2. Generate signature image dynamically
            byte[] signatureImageBytes = createSignatureImage(doctorName);
            InputStream signatureImageStream = new ByteArrayInputStream(signatureImageBytes);

            // 3. Configure Visible Signature
            PDVisibleSignDesigner visibleSignDesigner = new PDVisibleSignDesigner(
                    new ByteArrayInputStream(pdfBytes),
                    signatureImageStream,
                    1  // Page number
            );

            // Position: bottom-right area of the page
            visibleSignDesigner
                    .xAxis(350)
                    .yAxis(50)
                    .width(200)
                    .height(80)
                    .zoom(0);

            PDVisibleSigProperties visibleSigProperties = new PDVisibleSigProperties();
            visibleSigProperties
                    .signerName(doctorName)
                    .signerLocation("IndigoRx Medical Center")
                    .signatureReason("Prescription Verification")
                    .preferredSize(0)
                    .page(1)
                    .visualSignEnabled(true)
                    .setPdVisibleSignature(visibleSignDesigner)
                    .buildSignature();

            // 4. Apply with SignatureOptions
            SignatureOptions signatureOptions = new SignatureOptions();
            signatureOptions.setVisualSignature(visibleSigProperties.getVisibleSignature());
            signatureOptions.setPage(0);

            document.addSignature(signature, new CreateSignature(keystore, pin), signatureOptions);
            document.saveIncremental(outputPdf);
        }
    }

    /**
     * Creates a professional signature image with doctor's name and verification text
     */
    private byte[] createSignatureImage(String doctorName) throws IOException {
        int width = 300;
        int height = 100;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Enable anti-aliasing for smooth text
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background - light blue/gray professional look
        g2d.setColor(new Color(240, 248, 255));
        g2d.fillRect(0, 0, width, height);

        // Border
        g2d.setColor(new Color(70, 130, 180));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(1, 1, width - 3, height - 3);

        // "DIGITALLY SIGNED" header
        g2d.setColor(new Color(34, 139, 34));
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("✓ DIGITALLY SIGNED", 10, 20);

        // Doctor's name
        g2d.setColor(new Color(25, 25, 112));
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString(doctorName, 10, 45);

        // "IndigoRx Verified" text
        g2d.setColor(new Color(100, 100, 100));
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.drawString("IndigoRx Verified Prescription", 10, 65);

        // Date
        g2d.setFont(new Font("Arial", Font.PLAIN, 9));
        g2d.drawString("Date: " + java.time.LocalDate.now().toString(), 10, 85);

        g2d.dispose();

        // Convert to PNG bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
}