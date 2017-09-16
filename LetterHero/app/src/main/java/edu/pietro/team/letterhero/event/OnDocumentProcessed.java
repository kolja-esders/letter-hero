package edu.pietro.team.letterhero.event;


import edu.pietro.team.letterhero.MainActivity;
import edu.pietro.team.letterhero.entities.Document;
import edu.pietro.team.letterhero.helper.ProcessingState;
import edu.pietro.team.letterhero.social.MoneyTransfer;

public class OnDocumentProcessed {

    private final Document mDocument;
    private final ProcessingState mAssumedProcessingState;

    public OnDocumentProcessed(Document document, ProcessingState assumedPS) {
        mDocument = document;
        mAssumedProcessingState = assumedPS;

        MainActivity.getCurrentActivity().setCurrentDoc(document);
    }

    public Document getDocument() {
        return mDocument;
    }
    public ProcessingState getAssumedProcessingState() { return mAssumedProcessingState; }

}
