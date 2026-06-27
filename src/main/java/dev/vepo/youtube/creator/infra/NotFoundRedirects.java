package dev.vepo.youtube.creator.infra;

final class NotFoundRedirects {

    private NotFoundRedirects() {
    }

    static boolean shouldRedirectBrowserRequest(String method, String path) {
        return "GET".equalsIgnoreCase(method) && !shouldNotRedirect(path);
    }

    static boolean shouldNotRedirect(String path) {
        if (path == null || path.isBlank()) {
            return true;
        }
        var normalized = path.startsWith("/") ? path : "/" + path;
        if (normalized.startsWith("/api/") || normalized.startsWith("/download/")) {
            return true;
        }
        if (normalized.startsWith("/css/") || normalized.startsWith("/javascript/")
                || normalized.startsWith("/brand/") || normalized.startsWith("/icons/")) {
            return true;
        }
        return hasStaticExtension(normalized);
    }

    private static boolean hasStaticExtension(String path) {
        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot == path.length() - 1) {
            return false;
        }
        var extension = path.substring(dot + 1).toLowerCase();
        return switch (extension) {
            case "css", "js", "svg", "png", "jpg", "jpeg", "gif", "webp", "ico", "woff", "woff2", "ttf" -> true;
            default -> false;
        };
    }
}
