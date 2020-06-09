package com.teamrocket.app.model;

import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity
public class BirdSighting {

    @PrimaryKey(autoGenerate = true)
    public long sightingId;

    @Embedded
    private Bird bird;
    @Embedded
    private Location location;
    private long time;
    private String userId;
    private String notes;

    public BirdSighting(Bird bird, Location location, long time, String notes) {
        this.bird = bird;
        this.location = location;
        this.time = time;
        this.notes = notes;
    }

    public Bird getBird() {
        return bird;
    }

    public void setBird(Bird bird) {
        this.bird = bird;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BirdSighting)) {
            return false;
        }

        BirdSighting birdSight = (BirdSighting) o;
        return getBird().equals(birdSight.getBird())
                && getLocation().equals(birdSight.getLocation())
                && getTime() == birdSight.getTime();
    }

    @Override
    public int hashCode() {
        return Objects.hash(bird, location, time);
    }

    public static class Location {
        private double lat;
        private double lon;

        public Location(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLon() {
            return lon;
        }

        public void setLon(double lon) {
            this.lon = lon;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Location)) {
                return false;
            }

            Location loc = (Location) o;
            return getLat() == loc.getLat() && getLon() == loc.getLon();
        }

        @Override
        public int hashCode() {
            return Objects.hash(lat, lon);
        }
    }
}
