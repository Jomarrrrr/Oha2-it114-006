package Project.Common;

import java.io.Serializable;

public class Payload implements Serializable {


    private long clientId;
    private PayloadType payloadType;
    private String message;

    // Constructor that takes a PayloadType parameter
    public Payload(PayloadType payloadType) {
        this.payloadType = payloadType;
    }

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public PayloadType getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(PayloadType payloadType) {
        this.payloadType = payloadType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return String.format("Type[%s], Message[%s], ClientId[%s]", getPayloadType().toString(),
                getMessage(), getClientId());
    }
}
