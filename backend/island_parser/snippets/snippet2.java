import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;

public class PDDocumentRepresentation extends OutputRepresentation {

    private PDDocument document = new PDDocument();

    public PDDocumentRepresentation(PDDocument document) {
        super(MediaType.APPLICATION_PDF);
        this.document = document;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        try {
            document.save(outputStream);
            document.close();
        } catch (COSVisitorException e) {
            throw new IOException(e);
        }
    }
}