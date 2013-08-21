package com.keevosh.linkchecker.dto;

public class FamilyGroupDto {
    private boolean containsError;
    private int count;

    /**
     * @return the count
     */
    public int getCount() {
        return count;
    }

    /**
     * @param count
     *            the count to set
     */
    public void setCount(int count) {
        this.count = count;
    }

    /**
     * @return the containsError
     */
    public boolean isContainsError() {
        return containsError;
    }

    /**
     * @param containsError
     *            the containsError to set
     */
    public void setContainsError(boolean containsError) {
        this.containsError = containsError;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FamilyGroupDto [containsError=");
        builder.append(containsError);
        builder.append(", count=");
        builder.append(count);
        builder.append("]");
        return builder.toString();
    }

}
