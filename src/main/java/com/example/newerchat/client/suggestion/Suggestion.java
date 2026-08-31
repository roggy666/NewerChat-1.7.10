package com.example.newerchat.client.suggestion;

public final class Suggestion {

    public final String insert;
    public final String display;
    public final String description;

    public Suggestion(String insert, String description) {
        this(insert, insert, description);
    }

    public Suggestion(String insert, String display, String description) {
        this.insert = insert;
        this.display = display;
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Suggestion)) {
            return false;
        }
        return insert.equals(((Suggestion) o).insert);
    }

    @Override
    public int hashCode() {
        return insert.hashCode();
    }
}
