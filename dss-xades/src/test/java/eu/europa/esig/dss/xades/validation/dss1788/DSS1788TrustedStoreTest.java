package eu.europa.esig.dss.xades.validation.dss1788;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import eu.europa.esig.dss.diagnostic.CertificateWrapper;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.FoundCertificatesProxy;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.enumerations.CertificateRefOrigin;
import eu.europa.esig.dss.enumerations.Indication;
import eu.europa.esig.dss.enumerations.SubIndication;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.simplereport.SimpleReport;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.x509.CommonTrustedCertificateSource;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.AdvancedSignature;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.validation.SignatureCertificateSource;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.xades.validation.AbstractXAdESTestValidation;
import eu.europa.esig.validationreport.jaxb.SignatureAttributesType;
import eu.europa.esig.validationreport.jaxb.SignatureValidationReportType;
import eu.europa.esig.validationreport.jaxb.ValidationReportType;

public class DSS1788TrustedStoreTest extends AbstractXAdESTestValidation {

	@Override
	protected DSSDocument getSignedDocument() {
		return new FileDocument("src/test/resources/validation/dss1788/dss1788-noCertProvided.xml");
	}
	
	@Override
	protected SignedDocumentValidator getValidator(DSSDocument signedDocument) {
		SignedDocumentValidator validator = super.getValidator(signedDocument);
		CertificateToken signingCertificateToken = DSSUtils.loadCertificate(new File("src/test/resources/validation/dss1788/signCert.cer"));
		
		CommonCertificateVerifier commonCertificateVerifier = new CommonCertificateVerifier();
		CommonTrustedCertificateSource trustedCertSource = new CommonTrustedCertificateSource();
		trustedCertSource.addCertificate(signingCertificateToken);
		commonCertificateVerifier.setTrustedCertSource(trustedCertSource);
		validator.setCertificateVerifier(commonCertificateVerifier);
		
		return validator;
	}

	@Override
	protected void checkSigningCertificateValue(DiagnosticData diagnosticData) {
		SignatureWrapper signature = diagnosticData.getSignatureById(diagnosticData.getFirstSignatureId());
		
		CertificateWrapper signingCertificate = signature.getSigningCertificate();
		assertNull(signingCertificate);
		byte[] signingCertificatePublicKey = signature.getSigningCertificatePublicKey();
		assertNotNull(signingCertificatePublicKey);
		assertTrue(signature.isAttributePresent());
		assertTrue(signature.isDigestValuePresent());
		assertFalse(signature.isDigestValueMatch());
		assertFalse(signature.isIssuerSerialMatch());
	}
	
	@Override
	protected void verifySimpleReport(SimpleReport simpleReport) {
		super.verifySimpleReport(simpleReport);

		assertEquals(Indication.INDETERMINATE, simpleReport.getIndication(simpleReport.getFirstSignatureId()));
		assertEquals(SubIndication.NO_SIGNING_CERTIFICATE_FOUND, simpleReport.getSubIndication(simpleReport.getFirstSignatureId()));
	}
	
	@Override
	protected void verifySourcesAndDiagnosticData(List<AdvancedSignature> signatures, DiagnosticData diagnosticData) {
		AdvancedSignature advancedSignature = signatures.get(0);
		SignatureCertificateSource certificateSource = advancedSignature.getCertificateSource();
		
		SignatureWrapper signatureWrapper = diagnosticData.getSignatureById(advancedSignature.getId());
		assertNotNull(signatureWrapper);
		FoundCertificatesProxy foundCertificates = signatureWrapper.foundCertificates();
		
		assertEquals(certificateSource.getSigningCertificateRefs().size(), 
				foundCertificates.getOrphanCertificatesByRefOrigin(CertificateRefOrigin.SIGNING_CERTIFICATE).size());
	}
	
	@Override
	protected void checkCertificateChain(DiagnosticData diagnosticData) {
		SignatureWrapper signature = diagnosticData.getSignatureById(diagnosticData.getFirstSignatureId());
		assertTrue(Utils.isCollectionEmpty(signature.getCertificateChain()));
	}
	
	@Override
	protected void checkOrphanTokens(DiagnosticData diagnosticData) {
		assertEquals(1, diagnosticData.getAllOrphanCertificateReferences().size());
		assertEquals(0, diagnosticData.getAllOrphanRevocationReferences().size());
	}
	
	@Override
	protected void verifyETSIValidationReport(ValidationReportType etsiValidationReportJaxb) {
		List<SignatureValidationReportType> reports = etsiValidationReportJaxb.getSignatureValidationReport();
		for (SignatureValidationReportType signatureValidationReport : reports) {
			assertNotNull(signatureValidationReport);
			
			SignatureAttributesType signatureAttributes = signatureValidationReport.getSignatureAttributes();
			validateETSISignatureAttributes(signatureAttributes);
		}
	}

}