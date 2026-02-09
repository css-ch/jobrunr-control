Use MUST use the [arc42.md](../docs/arc42.md).
Use context7 mcp for more information for libraries (version in arc42).

# Documentation

* Only document public APIs.
* Use English for documentation

# Logging

* Always use JBoss Logging: `org.jboss.logging.Logger`
* Logger constant MUST be uppercase: `private static final Logger LOG = Logger.getLogger(ClassName.class);`
* Use formatted logging methods: `LOG.infof()`, `LOG.warnf()`, `LOG.debugf()`, `LOG.errorf()`
* Include exception as first parameter when logging errors: `LOG.errorf(exception, "Message: %s", context)`
