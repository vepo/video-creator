package dev.vepo.youtube.creator.project;

import java.util.HashMap;
import java.util.Map;

public class ClipEffect {
    private String id;
    private String mltService;
    private Map<String, Object> params = new HashMap<>();

    public ClipEffect() {
    }

    public ClipEffect(String id, String mltService) {
        this.id = id;
        this.mltService = mltService;
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
