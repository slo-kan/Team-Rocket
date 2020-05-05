package com.teamrocket.app.model;

import java.util.Objects;

public class Bird {

    public static final int SIZE_SMALL = 0;
    public static final int SIZE_MEDIUM = 1;
    public static final int SIZE_LARGE = 2;

    private String name;
    private String imagePath;
    private String family;
    private String color;
    private int size;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Bird)) {
            return false;
        }
        Bird bird = (Bird) o;
        return getName().toLowerCase().equals(bird.getName().toLowerCase())
                && getFamily().toLowerCase().equals(bird.getFamily().toLowerCase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase(), family.toLowerCase());
    }
}
