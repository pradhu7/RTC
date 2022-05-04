package apixio.cardinalsystem.api.model;

import java.util.List;

public class EventResponse {
    private int code;
    private List<String> types;
    private List<String> event_ids;
    private String message;

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public List<String> getEvent_ids() {
        return event_ids;
    }

    public void setEvent_ids(List<String> event_ids) {
        this.event_ids = event_ids;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
