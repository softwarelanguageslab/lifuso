public class MyResource extends ServerResource {
    @Get
    public Representation getPdf() {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage()
        document.addPage(page);

        return new PDDocumentRepresentation(document);
    }
}