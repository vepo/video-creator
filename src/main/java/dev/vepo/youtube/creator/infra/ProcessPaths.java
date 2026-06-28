package dev.vepo.youtube.creator.infra;

import java.util.Map;

public final class ProcessPaths {

    static final String SAFE_PATH = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin";

    private ProcessPaths() {
    }

    public static void applySafePath(Map<String, String> environment) {
        environment.put("PATH", SAFE_PATH);
    }
}
