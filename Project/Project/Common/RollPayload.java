package Project.Common;

public class RollPayload extends Payload {

    public RollPayload(int numDice, int numSides) {
        setPayloadType(PayloadType.ROLL);
        this.numDice = numDice;
        this.numSides = numSides;
    }

    private int numDice;
    private int numSides;

    public int getNumDice() {
        return numDice;
    }

    public void setNumDice(int numDice) {
        this.numDice = numDice;
    }

    public int getNumSides() {
        return numSides;
    }

    public void setNumSides(int numSides) {
        this.numSides = numSides;
    }

    @Override
    public String toString() {
        return super.toString() + ", Number of Dice: " + getNumDice() + ", Number of Sides: " + getNumSides();
    }
}