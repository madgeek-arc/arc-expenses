package arc.expenses.requests;

import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

public abstract class StageRequest {

    private String comment = "";

    private List<MultipartFile> attachments = new ArrayList<>();

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<MultipartFile> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<MultipartFile> attachments) {
        this.attachments = attachments;
    }
}
