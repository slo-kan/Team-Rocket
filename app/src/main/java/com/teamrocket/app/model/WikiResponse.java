package com.teamrocket.app.model;

import com.google.gson.annotations.SerializedName;

public class WikiResponse {

    @SerializedName("extract")
    private String description;

    public WikiResponse(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
