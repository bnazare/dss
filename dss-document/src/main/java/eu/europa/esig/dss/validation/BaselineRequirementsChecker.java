package eu.europa.esig.dss.validation;

import eu.europa.esig.dss.model.Digest;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.spi.x509.CandidatesForSigningCertificate;
import eu.europa.esig.dss.spi.x509.CertificateValidity;
import eu.europa.esig.dss.spi.x509.ListCertificateSource;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.timestamp.TimestampToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Checks conformance of a signature to the requested baseline format
 *
 * @param <AS> {@code DefaultAdvancedSignature}
 *
 */
public abstract class BaselineRequirementsChecker<AS extends DefaultAdvancedSignature> {

    private static final Logger LOG = LoggerFactory.getLogger(BaselineRequirementsChecker.class);

    /** The signature object */
    protected final AS signature;

    /**
     * The offline copy of a CertificateVerifier
     */
    private final CertificateVerifier offlineCertificateVerifier;

    /**
     * Default constructor
     *
     * @param signature {@link DefaultAdvancedSignature} to validate
     * @param offlineCertificateVerifier {@link CertificateVerifier} offline copy of a used CertificateVerifier
     */
    public BaselineRequirementsChecker(final AS signature, final CertificateVerifier offlineCertificateVerifier) {
        this.signature = signature;
        this.offlineCertificateVerifier = offlineCertificateVerifier;
    }

    /**
     * Checks if the signature has a corresponding BASELINE-B profile
     *
     * @return TRUE if the signature has a BASELINE-B profile, FALSE otherwise
     */
    public abstract boolean hasBaselineBProfile();

    /**
     * Checks if the signature has a corresponding BASELINE-T profile
     *
     * @return TRUE if the signature has a BASELINE-T profile, FALSE otherwise
     */
    public abstract boolean hasBaselineTProfile();

    /**
     * Checks if the signature has a corresponding BASELINE-LT profile
     *
     * @return TRUE if the signature has a BASELINE-LT profile, FALSE otherwise
     */
    public abstract boolean hasBaselineLTProfile();

    /**
     * Checks if the signature has a corresponding BASELINE-LTA profile
     *
     * @return TRUE if the signature has a BASELINE-LTA profile, FALSE otherwise
     */
    public abstract boolean hasBaselineLTAProfile();

    /**
     * Checks the minimal requirement to satisfy T-profile for AdES signatures
     *
     * @return TRUE if the signature has a T-profile, FALSE otherwise
     */
    protected boolean minimalTRequirement() {
        // SignatureTimeStamp (Cardinality >= 1)
        if (Utils.isCollectionEmpty(signature.getSignatureTimestamps())) {
            LOG.trace("SignatureTimeStamp shall be present for BASELINE-T signature (cardinality >= 1)!");
            return false;
        }
        CertificateToken signingCertificate = signature.getSigningCertificateToken();
        if (signingCertificate != null) {
            for (TimestampToken timestampToken : signature.getSignatureTimestamps()) {
                if (!timestampToken.getCreationDate().before(signingCertificate.getNotAfter())) {
                    LOG.warn("SignatureTimeStamp shall be generated before the signing certificate expiration for BASELINE-T signature!");
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks the minimal requirement to satisfy LT-profile for AdES signatures
     *
     * @return TRUE if the signature has an LT-profile, FALSE otherwise
     */
    public boolean minimalLTRequirement() {
        Objects.requireNonNull(offlineCertificateVerifier, "offlineCertificateVerifier cannot be null!");

        ListCertificateSource certificateSources = getCertificateSourcesExceptLastArchiveTimestamp();
        boolean certificateFound = certificateSources.getNumberOfCertificates() > 0;
        boolean allSelfSigned = certificateFound && certificateSources.isAllSelfSigned();

        boolean emptyCRLs = signature.getCompleteCRLSource().getAllRevocationBinaries().isEmpty();
        boolean emptyOCSPs = signature.getCompleteOCSPSource().getAllRevocationBinaries().isEmpty();
        boolean emptyRevocation = emptyCRLs && emptyOCSPs;

        boolean minimalLTRequirement = !allSelfSigned && !emptyRevocation;
        if (minimalLTRequirement) {
            // check presence of all revocation data
            return isAllRevocationDataPresent(offlineCertificateVerifier);
        }
        return minimalLTRequirement;
    }

    /**
     * Returns a list of certificate sources with an exception of the last archive timestamp if available
     *
     * @return {@link ListCertificateSource}
     */
    protected ListCertificateSource getCertificateSourcesExceptLastArchiveTimestamp() {
        ListCertificateSource certificateSource = new ListCertificateSource(signature.getCertificateSource());
        certificateSource.addAll(signature.getTimestampSource().getTimestampCertificateSourcesExceptLastArchiveTimestamp());
        certificateSource.addAll(signature.getCounterSignaturesCertificateSource());
        return certificateSource;
    }

    private boolean isAllRevocationDataPresent(CertificateVerifier offlineCertificateVerifier) {
        SignatureValidationContext validationContext = new SignatureValidationContext();
        validationContext.initialize(offlineCertificateVerifier);

        validationContext.addDocumentCertificateSource(signature.getCompleteCertificateSource());
        validationContext.addDocumentCRLSource(signature.getCompleteCRLSource());
        validationContext.addDocumentOCSPSource(signature.getCompleteOCSPSource());

        addSignatureForVerification(validationContext, signature);

        validationContext.validate();
        return validationContext.checkAllRequiredRevocationDataPresent();
    }

    private void addSignatureForVerification(ValidationContext validationContext, AdvancedSignature signature) {
        CertificateToken signingCertificate = signature.getSigningCertificateToken();
        if (signingCertificate != null) {
            validationContext.addCertificateTokenForVerification(signingCertificate);
        } else {
            CandidatesForSigningCertificate candidatesForSigningCertificate = signature.getCandidatesForSigningCertificate();
            List<CertificateValidity> certificateValidities = candidatesForSigningCertificate.getCertificateValidityList();
            if (Utils.isCollectionNotEmpty(certificateValidities)) {
                for (CertificateValidity certificateValidity : certificateValidities) {
                    if (certificateValidity.isValid() && certificateValidity.getCertificateToken() != null) {
                        validationContext.addCertificateTokenForVerification(certificateValidity.getCertificateToken());
                    }
                }
            }
        }
        for (TimestampToken timestampToken : signature.getTimestampSource().getAllTimestampsExceptLastArchiveTimestamp()) {
            validationContext.addTimestampTokenForVerification(timestampToken);
        }
        for (AdvancedSignature counterSignature : signature.getCounterSignatures()) {
            addSignatureForVerification(validationContext, counterSignature);
        }
    }

    /**
     * Checks the minimal requirement to satisfy LTA-profile for AdES signatures
     *
     * @return TRUE if the signature has an LTA-profile, FALSE otherwise
     */
    public boolean minimalLTARequirement() {
        if (Utils.isCollectionEmpty(signature.getArchiveTimestamps())) {
            LOG.trace("ArchiveTimeStamp shall be present for BASELINE-LTA signature (cardinality >= 1)!");
            return false;
        }
        return true;
    }

    /**
     * Checks if the given collection of {@code CertificateToken}s contains the signing certificate for the signature
     *
     * @param certificateTokens a collection of {@link CertificateToken}s
     * @return TRUE if the given collection of certificate contains teh signing certificate, FALSE otherwise
     */
    protected boolean containsSigningCertificate(Collection<CertificateToken> certificateTokens) {
        CertificateToken signingCertificate = signature.getSigningCertificateToken();
        for (CertificateToken certificate : certificateTokens) {
            if (certificate.equals(signingCertificate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the signature contains a SignaturePolicyIdentifier
     * containing a hash used to digest the signature policy
     *
     * @return TRUE if the SignaturePolicyIdentifier hash is present, FALSE otherwise
     */
    protected boolean isSignaturePolicyIdentifierHashPresent() {
        SignaturePolicy signaturePolicyIdentifier = signature.getSignaturePolicy();
        if (signaturePolicyIdentifier != null) {
            Digest digest = signaturePolicyIdentifier.getDigest();
            if (digest != null && digest.getAlgorithm() != null) {
                return true;
            }
        }
        return false;
    }

}
