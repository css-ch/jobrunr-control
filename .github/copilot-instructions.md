Use MUST use the [technical-spec.md](../.spec/technical-spec.md).
Use context7 for more information for libraries (version in technical-spec).

# Documentation

* Only document public APIs.
* Use English for documentation

# Logging

* Always use JBoss Logging: `org.jboss.logging.Logger`
* Logger constant MUST be uppercase: `private static final Logger LOG = Logger.getLogger(ClassName.class);`
* Use formatted logging methods: `LOG.infof()`, `LOG.warnf()`, `LOG.debugf()`, `LOG.errorf()`
* Include exception as first parameter when logging errors: `LOG.errorf(exception, "Message: %s", context)`
