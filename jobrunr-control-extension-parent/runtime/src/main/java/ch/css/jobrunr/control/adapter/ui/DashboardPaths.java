package ch.css.jobrunr.control.adapter.ui;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Resolves the absolute URL prefix at which the JobRunr Control dashboard is mounted.
 * <p>
 * The dashboard lives under Quarkus' non-application root path ({@code /q} by default),
 * so the prefix is {@code quarkus.http.root-path} + {@code quarkus.http.non-application-root-path}
 * + {@code /jobrunr-control}. This mirrors {@code NonApplicationRootPathBuildItem.resolvePath(...)}
 * and is used by Qute templates to emit links and HTMX targets that work regardless of
 * the consumer application's {@code @ApplicationPath}.
 */
@ApplicationScoped
public class DashboardPaths {

    private static final String DASHBOARD_SUBPATH = "/jobrunr-control";

    private final String basePath;

    public DashboardPaths(
            @ConfigProperty(name = "quarkus.http.root-path", defaultValue = "/") String rootPath,
            @ConfigProperty(name = "quarkus.http.non-application-root-path", defaultValue = "q") String nonApplicationRootPath) {
        this.basePath = resolveBasePath(rootPath, nonApplicationRootPath);
    }

    /**
     * @return absolute URL prefix for the dashboard, e.g. {@code /q/jobrunr-control}
     */
    public String basePath() {
        return basePath;
    }

    private static String resolveBasePath(String rootPath, String nonApplicationRootPath) {
        String normalizedRoot = normalizeRoot(rootPath);
        String base;
        if (nonApplicationRootPath.startsWith("/")) {
            base = nonApplicationRootPath;
        } else {
            String prefix = normalizedRoot.equals("/") ? "/" : normalizedRoot + "/";
            base = prefix + nonApplicationRootPath;
        }
        return stripTrailingSlash(base) + DASHBOARD_SUBPATH;
    }

    private static String normalizeRoot(String rootPath) {
        if (rootPath == null || rootPath.isBlank()) {
            return "/";
        }
        String result = rootPath.startsWith("/") ? rootPath : "/" + rootPath;
        return stripTrailingSlash(result);
    }

    private static String stripTrailingSlash(String s) {
        if (s.length() > 1 && s.endsWith("/")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }
}
