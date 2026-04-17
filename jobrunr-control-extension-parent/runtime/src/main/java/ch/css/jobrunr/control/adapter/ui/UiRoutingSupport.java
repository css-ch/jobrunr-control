package ch.css.jobrunr.control.adapter.ui;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.logging.Logger;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class UiRoutingSupport {

    private static final Logger LOG = Logger.getLogger(UiRoutingSupport.class);
    private static final String CONTENT_TYPE_HTML = "text/html; charset=utf-8";

    private UiRoutingSupport() {
    }

    /**
     * Runs {@code work} with an active CDI request context and the {@link SecurityIdentity}
     * from the routing context propagated to {@link CurrentIdentityAssociation}.
     * <p>
     * Vert.x blocking handlers do not activate a request context by default, so request-scoped
     * beans like {@link SecurityIdentity} (used by {@code SecurityTemplateExtension} in Qute)
     * would otherwise fail with {@code ContextNotActiveException}.
     */
    public static void withRequestContext(RoutingContext ctx, Runnable work) {
        ManagedContext requestContext = Arc.container().requestContext();
        boolean activatedHere = false;
        if (!requestContext.isActive()) {
            requestContext.activate();
            activatedHere = true;
        }
        try {
            User user = ctx.user();
            if (user instanceof QuarkusHttpUser quarkusUser) {
                CurrentIdentityAssociation cia = Arc.container()
                        .instance(CurrentIdentityAssociation.class).get();
                cia.setIdentity(quarkusUser.getSecurityIdentity());
            }
            work.run();
        } finally {
            if (activatedHere) {
                requestContext.terminate();
            }
        }
    }

    public static boolean requireAnyRole(RoutingContext ctx, String... allowedRoles) {
        SecurityIdentity identity = extractIdentity(ctx);
        if (identity == null) {
            ctx.fail(401);
            return false;
        }
        for (String role : allowedRoles) {
            if (identity.hasRole(role)) {
                return true;
            }
        }
        LOG.warnf("Access denied for principal %s: required one of %s",
                identity.getPrincipal() != null ? identity.getPrincipal().getName() : "?",
                Set.of(allowedRoles));
        ctx.fail(403);
        return false;
    }

    /**
     * Extracts the {@link SecurityIdentity} from the routing context.
     * <p>
     * {@code SecurityIdentity} is a {@code @RequestScoped} CDI bean that is not accessible
     * from a plain Vert.x blocking handler (no active request context). Quarkus attaches the
     * identity to the routing context's {@link User} via {@link QuarkusHttpUser}, so we read
     * it from there.
     */
    private static SecurityIdentity extractIdentity(RoutingContext ctx) {
        User user = ctx.user();
        if (user instanceof QuarkusHttpUser quarkusUser) {
            return quarkusUser.getSecurityIdentity();
        }
        return null;
    }

    public static void renderHtml(RoutingContext ctx, TemplateInstance template) {
        ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_HTML);
        template.createUni().subscribe().with(
                rendered -> ctx.response().end(rendered),
                ctx::fail);
    }

    public static void renderHtml(RoutingContext ctx, String html) {
        ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_HTML);
        ctx.response().end(html);
    }

    public static void sendModalClose(RoutingContext ctx, TemplateInstance tableTemplate) {
        ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_HTML);
        ctx.response().putHeader("HX-Trigger", "closeModal");
        tableTemplate.createUni().subscribe().with(
                rendered -> ctx.response().end(rendered),
                ctx::fail);
    }

    public static void sendFormError(RoutingContext ctx, String errorMessage) {
        String errorHtml = String.format(
                "<div class=\"alert alert-danger alert-dismissible fade show\" role=\"alert\">"
                        + "<i class=\"bi bi-exclamation-triangle-fill\"></i> <strong>Error:</strong> %s"
                        + "<button type=\"button\" class=\"btn-close\" data-bs-dismiss=\"alert\" aria-label=\"Close\"></button>"
                        + "</div>",
                escapeHtml(errorMessage));
        ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_HTML);
        ctx.response().putHeader("HX-Retarget", "#form-alerts");
        ctx.response().putHeader("HX-Reswap", "innerHTML");
        ctx.response().putHeader("HX-Trigger", "scrollToError");
        ctx.response().end(errorHtml);
    }

    public static String queryParam(RoutingContext ctx, String name) {
        return ctx.request().getParam(name);
    }

    public static String queryParam(RoutingContext ctx, String name, String defaultValue) {
        String value = ctx.request().getParam(name);
        return value != null ? value : defaultValue;
    }

    public static int intQueryParam(RoutingContext ctx, String name, int defaultValue) {
        String raw = ctx.request().getParam(name);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static UUID pathUuid(RoutingContext ctx, String name) {
        String raw = ctx.pathParam(name);
        return raw != null ? UUID.fromString(raw) : null;
    }

    public static String formAttr(RoutingContext ctx, String name) {
        String value = ctx.request().getFormAttribute(name);
        if (value != null) {
            return value;
        }
        MultivaluedMap<String, String> parsed = allFormParams(ctx);
        return parsed.getFirst(name);
    }

    public static MultivaluedMap<String, String> allFormParams(RoutingContext ctx) {
        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        MultiMap form = ctx.request().formAttributes();
        if (form != null && !form.isEmpty()) {
            for (String name : form.names()) {
                for (String value : form.getAll(name)) {
                    result.add(name, value);
                }
            }
            return result;
        }
        // Vert.x only populates formAttributes when setExpectMultipart(true) is called before
        // the body is received, and the global BodyHandler does not always run for non-application
        // routes. We therefore read the raw body synchronously (the UI handlers are BLOCKING) and
        // parse the URL-encoded pairs ourselves.
        String contentType = ctx.request().getHeader(HttpHeaders.CONTENT_TYPE);
        if (contentType == null || !contentType.toLowerCase().contains("application/x-www-form-urlencoded")) {
            return result;
        }
        Buffer body = readBodySynchronously(ctx);
        if (body == null || body.length() == 0) {
            return result;
        }
        parseUrlEncoded(body.toString(StandardCharsets.UTF_8), result);
        return result;
    }

    private static Buffer readBodySynchronously(RoutingContext ctx) {
        Buffer body = ctx.body() != null ? ctx.body().buffer() : null;
        if (body != null && body.length() > 0) {
            return body;
        }
        try {
            return ctx.request().body()
                    .toCompletionStage()
                    .toCompletableFuture()
                    .get(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warnf("Interrupted while reading request body");
            return null;
        } catch (ExecutionException | TimeoutException e) {
            LOG.warnf(e, "Failed to read request body");
            return null;
        }
    }

    private static void parseUrlEncoded(String body, MultivaluedMap<String, String> target) {
        for (String pair : body.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String key;
            String value;
            if (eq < 0) {
                key = URLDecoder.decode(pair, StandardCharsets.UTF_8);
                value = "";
            } else {
                key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            }
            target.add(key, value);
        }
    }

    private static String escapeHtml(String in) {
        if (in == null) {
            return "";
        }
        return in.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
