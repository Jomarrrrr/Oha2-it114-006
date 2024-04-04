package Project.Common;

public class FlipPayload extends Payload {

    private String result; // Store 'heads' or 'tails'

    public FlipPayload(String result) {
        setPayloadType(PayloadType.FLIP);
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return super.toString() + ", Result: " + getResult();
    }
}