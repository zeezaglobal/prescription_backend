package com.zeezaglobal.prescription.PDF;



import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public class CreateSignature implements SignatureInterface {

    private final PrivateKey privateKey;
    private final Certificate[] certificateChain;

    public CreateSignature(KeyStore keystore, char[] pin) throws Exception {
        // Ensure BouncyCastle is registered as a security provider
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        String alias = keystore.aliases().nextElement();
        this.privateKey = (PrivateKey) keystore.getKey(alias, pin);
        this.certificateChain = keystore.getCertificateChain(alias);

        if (privateKey == null) {
            throw new IOException("Could not find private key for alias: " + alias);
        }
    }

    @Override
    public byte[] sign(InputStream content) throws IOException {
        try {
            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
            X509Certificate cert = (X509Certificate) certificateChain[0];

            // Use BouncyCastle provider explicitly for the signer
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(privateKey);

            gen.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(
                            new JcaDigestCalculatorProviderBuilder()
                                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                                    .build()
                    ).build(signer, cert)
            );

            gen.addCertificates(new JcaCertStore(Arrays.asList(certificateChain)));

            // PDFBox passes the stream of the PDF bytes to be signed
            byte[] data = content.readAllBytes();
            CMSTypedData cmsData = new CMSProcessableByteArray(data);

            // Generate the CMS/PKCS7 signature
            // 'false' means the content is NOT encapsulated (Detached signature)
            // This is required for PDF signatures (adbe.pkcs7.detached)
            CMSSignedData signedData = gen.generate(cmsData, false);

            return signedData.getEncoded();
        } catch (Exception e) {
            throw new IOException("Error during CMS signature generation", e);
        }
    }
}