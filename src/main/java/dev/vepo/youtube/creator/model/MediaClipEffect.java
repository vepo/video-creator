package dev.vepo.youtube.creator.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaClipEffect {
    private String id;
    private String mltService;
    private Map<String, Object> params = new HashMap<>();

    public MediaClipEffect() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMltService() {
        return mltService;
    }

    public void setMltService(String mltService) {
        this.mltService = mltService;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params != null ? params : new HashMap<>();
    }
}
