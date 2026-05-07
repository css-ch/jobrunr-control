package ch.css.jobrunr.control.domain;

public record EnumValue(String value, String label, int order) implements Comparable<EnumValue> {

    @Override
    public int compareTo(EnumValue other) {
        int compareValue = Integer.compare(this.order, other.order);
        if(compareValue == 0) {
            return this.value.compareTo(other.value);
        }
        return compareValue;
    }
}