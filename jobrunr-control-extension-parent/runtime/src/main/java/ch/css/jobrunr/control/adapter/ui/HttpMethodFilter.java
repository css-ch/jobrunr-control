package ch.css.jobrunr.control.adapter.ui;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.function.Consumer;

/**
 * Named {@link Consumer} applied via {@code routeFunction} when registering a
 * {@link io.quarkus.vertx.http.deployment.RouteBuildItem}.
 * <p>
 * It restricts the route to a specific HTTP method and attaches a {@link BodyHandler}, because
 * the global Quarkus {@code BodyHandler} does not always parse bodies for routes mounted under
 * the non-application root path — without it {@code ctx.request().getFormAttribute(...)} and
 * {@code ctx.body().buffer()} are empty for {@code application/x-www-form-urlencoded} POSTs.
 * <p>
 * File-upload handling is disabled ({@code BodyHandler.create(false)}): the UI only accepts
 * {@code application/x-www-form-urlencoded} submissions, and leaving uploads enabled makes
 * Vert.x create its default {@code file-uploads/} directory eagerly on every request — which
 * breaks deployments on read-only root filesystems.
 * <p>
 * A named class (not a lambda in the deployment module) is required because the consumer is
 * stored in a build item and reconstructed at runtime by Quarkus' bytecode recorder; a lambda
 * from the deployment module would not be on the runtime classpath. The method is held as a
 * {@link String} (rather than {@link HttpMethod}) because Quarkus' recorder expects a bean-style
 * default constructor plus getter/setter per field, and Vert.x 4's {@link HttpMethod} is not a
 * directly-recordable value type.
 */
public class HttpMethodFilter implements Consumer<Route> {

    public static final HttpMethodFilter GET = new HttpMethodFilter("GET");
    public static final HttpMethodFilter POST = new HttpMethodFilter("POST");
    public static final HttpMethodFilter PUT = new HttpMethodFilter("PUT");
    public static final HttpMethodFilter DELETE = new HttpMethodFilter("DELETE");

    private String method;

    public HttpMethodFilter() {
    }

    public HttpMethodFilter(String method) {
        this.method = method;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public void accept(Route route) {
        HttpMethod httpMethod = HttpMethod.valueOf(method);
        route.method(httpMethod);
        if (httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT || httpMethod == HttpMethod.PATCH) {
            route.handler(BodyHandler.create(false));
        }
    }
}
