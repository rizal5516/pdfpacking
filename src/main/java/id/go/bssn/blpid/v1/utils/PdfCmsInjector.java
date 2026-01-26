package id.go.bssn.blpid.v1.utils;

import com.itextpdf.signatures.IExternalSignatureContainer;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfString;

import java.io.InputStream;

public class PdfCmsInjector implements IExternalSignatureContainer {
    private final String base64Cms;

    public PdfCmsInjector(String base64Cms) {
        this.base64Cms = base64Cms;
    }

    @Override
    public byte[] sign(InputStream data) {
        return java.util.Base64.getDecoder().decode(base64Cms);
    }

    @Override
    public void modifySigningDictionary(PdfDictionary signDic) {
        signDic.put(PdfName.Filter, PdfName.Adobe_PPKLite);
        signDic.put(PdfName.SubFilter, PdfName.ETSI_CAdES_DETACHED);
        signDic.put(PdfName.M, new PdfString(new java.util.Date().toString()));
    }
}
