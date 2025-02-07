// Modified by SignalFx
package datadog.trace.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Config reads values with the following priority: 1) system properties, 2) environment variables,
 * 3) optional configuration file. It also includes default values to ensure a valid config.
 *
 * <p>
 *
 * <p>System properties are {@link Config#PREFIX}'ed. Environment variables are the same as the
 * system property, but uppercased with '.' -> '_'.
 */
@Slf4j
@ToString(includeFieldNames = true)
public class Config {

  /** Config keys below */
  private static final String PREFIX = "dd.";

  private static final String SIGNALFX_PREFIX = "signalfx.";

  public static final String PROFILING_URL_TEMPLATE = "https://intake.profile.%s/v1/input";

  private static final Pattern ENV_REPLACEMENT = Pattern.compile("[^a-zA-Z0-9_]");

  public static final String CONFIGURATION_FILE = "trace.config";
  public static final String API_KEY = "api-key";
  public static final String API_KEY_FILE = "api-key-file";
  public static final String SITE = "site";
  public static final String SERVICE_NAME = "service.name";
  public static final String TRACE_ENABLED = "tracing.enabled";
  public static final String INTEGRATIONS_ENABLED = "integrations.enabled";
  public static final String WRITER_TYPE = "writer.type";
  public static final String API_TYPE = "api.type";
  public static final String USE_B3_PROPAGATION = "b3.propagation";
  public static final String AGENT_HOST = "agent.host";
  public static final String TRACE_AGENT_PORT = "trace.agent.port";
  public static final String AGENT_PORT_LEGACY = "agent.port";
  public static final String AGENT_PATH = "agent.path";
  public static final String AGENT_USE_HTTPS = "agent.https";
  public static final String ENDPOINT_URL = "endpoint.url";
  public static final String AGENT_UNIX_DOMAIN_SOCKET = "trace.agent.unix.domain.socket";
  public static final String PRIORITY_SAMPLING = "priority.sampling";
  public static final String TRACE_RESOLVER_ENABLED = "trace.resolver.enabled";
  public static final String SERVICE_MAPPING = "service.mapping";
  public static final String ENV = "env";

  private static final String VERSION = "version";
  public static final String TAGS = "tags";
  @Deprecated // Use dd.tags instead
  public static final String GLOBAL_TAGS = "trace.global.tags";
  public static final String SPAN_TAGS = "span.tags";
  public static final String JMX_TAGS = "trace.jmx.tags";
  public static final String TRACE_ANALYTICS_ENABLED = "trace.analytics.enabled";
  public static final String TRACE_ANNOTATIONS = "trace.annotations";
  public static final String ANNOTATED_METHOD_BLACKLIST = "trace.annotated.method.blacklist";
  public static final String TRACE_EXECUTORS_ALL = "trace.executors.all";
  public static final String TRACE_EXECUTORS = "trace.executors";
  public static final String TRACE_METHODS = "trace.methods";
  public static final String TRACE_CLASSES_EXCLUDE = "trace.classes.exclude";
  public static final String TRACE_SAMPLING_SERVICE_RULES = "trace.sampling.service.rules";
  public static final String TRACE_SAMPLING_OPERATION_RULES = "trace.sampling.operation.rules";
  public static final String TRACE_SAMPLE_RATE = "trace.sample.rate";
  public static final String TRACE_RATE_LIMIT = "trace.rate.limit";
  public static final String TRACE_REPORT_HOSTNAME = "trace.report-hostname";
  public static final String HEADER_TAGS = "trace.header.tags";
  public static final String HTTP_SERVER_ERROR_STATUSES = "http.server.error.statuses";
  public static final String HTTP_CLIENT_ERROR_STATUSES = "http.client.error.statuses";
  public static final String HTTP_SERVER_TAG_QUERY_STRING = "http.server.tag.query-string";
  public static final String HTTP_CLIENT_TAG_QUERY_STRING = "http.client.tag.query-string";
  public static final String HTTP_CLIENT_HOST_SPLIT_BY_DOMAIN = "trace.http.client.split-by-domain";
  public static final String DB_CLIENT_HOST_SPLIT_BY_INSTANCE = "trace.db.client.split-by-instance";
  public static final String SPLIT_BY_TAGS = "trace.split-by-tags";
  public static final String SCOPE_DEPTH_LIMIT = "trace.scope.depth.limit";
  public static final String PARTIAL_FLUSH_MIN_SPANS = "trace.partial.flush.min.spans";
  public static final String RUNTIME_CONTEXT_FIELD_INJECTION =
      "trace.runtime.context.field.injection";
  public static final String PROPAGATION_STYLE_EXTRACT = "propagation.style.extract";
  public static final String PROPAGATION_STYLE_INJECT = "propagation.style.inject";

  public static final String KAFKA_ATTEMPT_PROPAGATION =
      "instrumentation.kafka.attempt-propagation";
  public static final String REDIS_CAPTURE_COMMAND_ARGUMENTS =
      "instrumentation.redis.capture-command-arguments";

  public static final String ZIPKIN_GZIP_CONTENT_ENCODING = "zipkin.gzip.content.encoding";

  public static final String JMX_FETCH_ENABLED = "jmxfetch.enabled";
  public static final String JMX_FETCH_CONFIG_DIR = "jmxfetch.config.dir";
  public static final String JMX_FETCH_CONFIG = "jmxfetch.config";
  @Deprecated public static final String JMX_FETCH_METRICS_CONFIGS = "jmxfetch.metrics-configs";
  public static final String JMX_FETCH_CHECK_PERIOD = "jmxfetch.check-period";
  public static final String JMX_FETCH_REFRESH_BEANS_PERIOD = "jmxfetch.refresh-beans-period";
  public static final String JMX_FETCH_STATSD_HOST = "jmxfetch.statsd.host";
  public static final String JMX_FETCH_STATSD_PORT = "jmxfetch.statsd.port";

  public static final String HEALTH_METRICS_ENABLED = "trace.health.metrics.enabled";
  public static final String HEALTH_METRICS_STATSD_HOST = "trace.health.metrics.statsd.host";
  public static final String HEALTH_METRICS_STATSD_PORT = "trace.health.metrics.statsd.port";

  public static final String LOGS_INJECTION_ENABLED = "logs.injection";
  public static final String DB_STATEMENT_MAX_LENGTH = "db.statement.max.length";
  public static final String RECORDED_VALUE_MAX_LENGTH = "recorded.value.max.length";

  public static final String PROFILING_ENABLED = "profiling.enabled";
  @Deprecated // Use dd.site instead
  public static final String PROFILING_URL = "profiling.url";
  @Deprecated // Use dd.api-key instead
  public static final String PROFILING_API_KEY_OLD = "profiling.api-key";
  @Deprecated // Use dd.api-key-file instead
  public static final String PROFILING_API_KEY_FILE_OLD = "profiling.api-key-file";
  @Deprecated // Use dd.api-key instead
  public static final String PROFILING_API_KEY_VERY_OLD = "profiling.apikey";
  @Deprecated // Use dd.api-key-file instead
  public static final String PROFILING_API_KEY_FILE_VERY_OLD = "profiling.apikey.file";
  public static final String PROFILING_TAGS = "profiling.tags";
  public static final String PROFILING_START_DELAY = "profiling.start-delay";
  // DANGEROUS! May lead on sigsegv on JVMs before 14
  // Not intended for production use
  public static final String PROFILING_START_FORCE_FIRST =
      "profiling.experimental.start-force-first";
  public static final String PROFILING_UPLOAD_PERIOD = "profiling.upload.period";
  public static final String PROFILING_TEMPLATE_OVERRIDE_FILE =
      "profiling.jfr-template-override-file";
  public static final String PROFILING_UPLOAD_TIMEOUT = "profiling.upload.timeout";
  public static final String PROFILING_UPLOAD_COMPRESSION = "profiling.upload.compression";
  public static final String PROFILING_PROXY_HOST = "profiling.proxy.host";
  public static final String PROFILING_PROXY_PORT = "profiling.proxy.port";
  public static final String PROFILING_PROXY_USERNAME = "profiling.proxy.username";
  public static final String PROFILING_PROXY_PASSWORD = "profiling.proxy.password";

  public static final String RUNTIME_ID_TAG = "runtime-id";
  public static final String SERVICE = "service";
  public static final String SERVICE_TAG = SERVICE;
  public static final String ENVIRONMENT_TAG = "environment";
  public static final String HOST_TAG = "host";
  public static final String LANGUAGE_TAG_KEY = "language";
  public static final String LANGUAGE_TAG_VALUE = "jvm";

  public static final String MAX_SPANS_PER_TRACE = "max.spans.per.trace";
  public static final Integer DEFAULT_MAX_SPANS_PER_TRACE = 0;

  public static final String MAX_CONTINUATION_DEPTH = "max.continuation.depth";
  public static final Integer DEFAULT_MAX_CONTINUATION_DEPTH = 100;

  public static final String SERVER_TIMING_CONTEXT = "server.timing.context";
  public static final boolean DEFAULT_SERVER_TIMING_CONTEXT = false;

  public static final String DEFAULT_SITE = "datadoghq.com";
  public static final String DEFAULT_SERVICE_NAME = "unnamed-java-service";

  public static final String TRACING_LIBRARY_KEY = "signalfx.tracing.library";
  public static final String TRACING_LIBRARY_VALUE = "java-tracing";
  public static final String TRACING_VERSION_KEY = "signalfx.tracing.version";
  public static final String TRACING_VERSION_VALUE = "0.48.0-sfx10";

  private static final boolean DEFAULT_TRACE_ENABLED = true;
  public static final boolean DEFAULT_INTEGRATIONS_ENABLED = true;
  public static final String DD_AGENT_WRITER_TYPE = "DDAgentWriter";
  public static final String DD_AGENT_API_TYPE = "DD";
  public static final String ZIPKIN_V2_API_TYPE = "ZipkinV2";
  public static final String LOGGING_WRITER_TYPE = "LoggingWriter";
  private static final String DEFAULT_AGENT_WRITER_TYPE = DD_AGENT_WRITER_TYPE;
  public static final String DEFAULT_API_TYPE = ZIPKIN_V2_API_TYPE;

  public static final String DEFAULT_AGENT_ENDPOINT = "http://localhost:9080/v1/trace";

  public static final String DEFAULT_AGENT_UNIX_DOMAIN_SOCKET = null;

  private static final boolean DEFAULT_RUNTIME_CONTEXT_FIELD_INJECTION = true;

  private static final boolean DEFAULT_PRIORITY_SAMPLING_ENABLED = false;
  private static final boolean DEFAULT_TRACE_RESOLVER_ENABLED = true;
  private static final Set<Integer> DEFAULT_HTTP_SERVER_ERROR_STATUSES =
      parseIntegerRangeSet("500-599", "default");
  private static final Set<Integer> DEFAULT_HTTP_CLIENT_ERROR_STATUSES =
      parseIntegerRangeSet("500-599", "default");
  private static final boolean DEFAULT_HTTP_SERVER_TAG_QUERY_STRING = false;
  private static final boolean DEFAULT_HTTP_CLIENT_TAG_QUERY_STRING = false;
  private static final boolean DEFAULT_HTTP_CLIENT_SPLIT_BY_DOMAIN = false;
  private static final boolean DEFAULT_DB_CLIENT_HOST_SPLIT_BY_INSTANCE = false;
  private static final String DEFAULT_SPLIT_BY_TAGS = "";
  private static final int DEFAULT_SCOPE_DEPTH_LIMIT = 100;
  public static final int DEFAULT_PARTIAL_FLUSH_MIN_SPANS = 1000;
  private static final String DEFAULT_PROPAGATION_STYLE_EXTRACT = PropagationStyle.B3.name();
  private static final String DEFAULT_PROPAGATION_STYLE_INJECT = PropagationStyle.B3.name();
  private static final boolean DEFAULT_JMX_FETCH_ENABLED = false;

  public static final boolean DEFAULT_KAFKA_ATTEMPT_PROPAGATION = true;
  public static final boolean DEFAULT_REDIS_CAPTURE_COMMAND_ARGUMENTS = true;

  public static final int DEFAULT_JMX_FETCH_STATSD_PORT = 8125;

  public static final boolean DEFAULT_METRICS_ENABLED = false;
  // No default constants for metrics statsd support -- falls back to jmxfetch values

  public static final boolean DEFAULT_LOGS_INJECTION_ENABLED = false;

  public static final boolean DEFAULT_PROFILING_ENABLED = false;
  public static final int DEFAULT_PROFILING_START_DELAY = 10;
  public static final boolean DEFAULT_PROFILING_START_FORCE_FIRST = false;
  public static final int DEFAULT_PROFILING_UPLOAD_PERIOD = 60; // 1 min
  public static final int DEFAULT_PROFILING_UPLOAD_TIMEOUT = 30; // seconds
  public static final String DEFAULT_PROFILING_UPLOAD_COMPRESSION = "on";
  public static final int DEFAULT_PROFILING_PROXY_PORT = 8080;

  private static final String SPLIT_BY_SPACE_OR_COMMA_REGEX = "[,\\s]+";

  private static final boolean DEFAULT_TRACE_REPORT_HOSTNAME = false;
  private static final String DEFAULT_TRACE_ANNOTATIONS = null;
  private static final String DEFAULT_ANNOTATED_METHOD_BLACKLIST = null;
  private static final boolean DEFAULT_TRACE_EXECUTORS_ALL = false;
  private static final String DEFAULT_TRACE_EXECUTORS = "";
  private static final String DEFAULT_TRACE_METHODS = null;
  public static final boolean DEFAULT_TRACE_ANALYTICS_ENABLED = false;
  public static final float DEFAULT_ANALYTICS_SAMPLE_RATE = 1.0f;
  public static final double DEFAULT_TRACE_RATE_LIMIT = 100;

  public enum PropagationStyle {
    DATADOG,
    B3,
    HAYSTACK
  }

  public static final int DEFAULT_DB_STATEMENT_MAX_LENGTH = 1024;
  public static final int DEFAULT_RECORDED_VALUE_MAX_LENGTH = 12288;

  public static final boolean DEFAULT_ZIPKIN_GZIP_CONTENT_ENCODING = true;

  /** A tag intended for internal use only, hence not added to the public api DDTags class. */
  private static final String INTERNAL_HOST_NAME = "_dd.hostname";

  /** Used for masking sensitive information when doing toString */
  @ToString.Include(name = "apiKey")
  private String profilingApiKeyMasker() {
    return apiKey != null ? "****" : null;
  }

  /** Used for masking sensitive information when doing toString */
  @ToString.Include(name = "profilingProxyPassword")
  private String profilingProxyPasswordMasker() {
    return profilingProxyPassword != null ? "****" : null;
  }

  /**
   * this is a random UUID that gets generated on JVM start up and is attached to every root span
   * and every JMX metric that is sent out.
   */
  @Getter private final String runtimeId;

  /**
   * Note: this has effect only on profiling site. Traces are sent to Datadog agent and are not
   * affected by this setting.
   */
  @Getter private final String apiKey;
  /**
   * Note: this has effect only on profiling site. Traces are sent to Datadog agent and are not
   * affected by this setting.
   */
  @Getter private final String site;

  @Getter private final String serviceName;
  @Getter private final String environmentName;
  @Getter private final boolean traceEnabled;
  @Getter private final boolean integrationsEnabled;
  @Getter private final String writerType;
  @Getter private final String apiType;
  @Getter private final boolean useB3Propagation;
  private final String agentHost;
  private final Integer agentPort;
  private final String agentPath;
  private final Boolean agentUseHTTPS;
  @Getter private final URL endpointUrl;
  @Getter private final String agentUnixDomainSocket;
  @Getter private final boolean prioritySamplingEnabled;
  @Getter private final boolean traceResolverEnabled;
  @Getter private final Map<String, String> serviceMapping;
  private final Map<String, String> tags;
  @Deprecated private final Map<String, String> globalTags;
  private final Map<String, String> spanTags;
  private final Map<String, String> jmxTags;
  @Getter private final List<String> excludedClasses;
  @Getter private final Map<String, String> headerTags;
  @Getter private final Set<Integer> httpServerErrorStatuses;
  @Getter private final Set<Integer> httpClientErrorStatuses;
  @Getter private final boolean httpServerTagQueryString;
  @Getter private final boolean httpClientTagQueryString;
  @Getter private final boolean httpClientSplitByDomain;
  @Getter private final boolean dbClientSplitByInstance;
  @Getter private final Set<String> splitByTags;
  @Getter private final Integer scopeDepthLimit;
  @Getter private final Integer partialFlushMinSpans;
  @Getter private final boolean runtimeContextFieldInjection;
  @Getter private final Set<PropagationStyle> propagationStylesToExtract;
  @Getter private final Set<PropagationStyle> propagationStylesToInject;

  @Getter private final boolean kafkaAttemptPropagation;
  @Getter private final boolean redisCaptureCommandArguments;

  @Getter private final boolean zipkinGZIPContentEncoding;

  @Getter private final boolean jmxFetchEnabled;
  @Getter private final String jmxFetchConfigDir;
  @Getter private final List<String> jmxFetchConfigs;
  @Deprecated @Getter private final List<String> jmxFetchMetricsConfigs;
  @Getter private final Integer jmxFetchCheckPeriod;
  @Getter private final Integer jmxFetchRefreshBeansPeriod;
  @Getter private final String jmxFetchStatsdHost;
  @Getter private final Integer jmxFetchStatsdPort;

  // These values are default-ed to those of jmx fetch values as needed
  @Getter private final boolean healthMetricsEnabled;
  @Getter private final String healthMetricsStatsdHost;
  @Getter private final Integer healthMetricsStatsdPort;

  @Getter private final boolean logsInjectionEnabled;
  @Getter private final boolean reportHostName;

  @Getter private final Integer dbStatementMaxLength;
  @Getter private final Integer recordedValueMaxLength;

  // Read order: System Properties -> Env Variables, [-> default value]
  @Getter private final String traceAnnotations;
  @Getter private final String annotatedMethodBlacklist;

  @Getter private final String traceMethods;

  @Getter private final boolean traceExecutorsAll;
  @Getter private final List<String> traceExecutors;

  @Getter private final boolean traceAnalyticsEnabled;

  @Getter private final Integer maxSpansPerTrace;
  @Getter private final Integer maxContinuationDepth;

  // Feature for RUM that emits the trace context to the browser in a Server-Timing header
  // on the *response* (most other propagation relies on the client to propagate on
  // the *request*).
  @Getter private final boolean emitServerTimingContext;
  @Getter private final Map<String, String> traceSamplingServiceRules;
  @Getter private final Map<String, String> traceSamplingOperationRules;
  @Getter private final Double traceSampleRate;
  @Getter private final Double traceRateLimit;

  @Getter private final boolean profilingEnabled;
  @Deprecated private final String profilingUrl;
  private final Map<String, String> profilingTags;
  @Getter private final int profilingStartDelay;
  @Getter private final boolean profilingStartForceFirst;
  @Getter private final int profilingUploadPeriod;
  @Getter private final String profilingTemplateOverrideFile;
  @Getter private final int profilingUploadTimeout;
  @Getter private final String profilingUploadCompression;
  @Getter private final String profilingProxyHost;
  @Getter private final int profilingProxyPort;
  @Getter private final String profilingProxyUsername;
  @Getter private final String profilingProxyPassword;

  // Values from an optionally provided properties file
  private static Properties propertiesFromConfigFile;

  // Read order: System Properties -> Env Variables, [-> properties file], [-> default value]
  // Visible for testing
  Config() {
    propertiesFromConfigFile = loadConfigurationFile();

    runtimeId = UUID.randomUUID().toString();

    // Note: We do not want APiKey to be loaded from property for security reasons
    // Note: we do not use defined default here
    // FIXME: We should use better authentication mechanism
    final String apiKeyFile = getSettingFromEnvironment(API_KEY_FILE, null);
    //    String tmpApiKey = System.getenv(propertyNameToEnvironmentVariableName(API_KEY));
    String tmpApiKey = getSettingFromEnvironmentVariables(API_KEY);
    if (apiKeyFile != null) {
      try {
        tmpApiKey =
            new String(Files.readAllBytes(Paths.get(apiKeyFile)), StandardCharsets.UTF_8).trim();
      } catch (final IOException e) {
        log.error("Cannot read API key from file {}, skipping", apiKeyFile, e);
      }
    }
    site = getSettingFromEnvironment(SITE, DEFAULT_SITE);
    serviceName =
        getSettingFromEnvironment(
            SERVICE_NAME, getSettingFromEnvironment(SERVICE, DEFAULT_SERVICE_NAME));

    environmentName = getSettingFromEnvironment(ENV, "");

    traceEnabled = getBooleanSettingFromEnvironment(TRACE_ENABLED, DEFAULT_TRACE_ENABLED);
    integrationsEnabled =
        getBooleanSettingFromEnvironment(INTEGRATIONS_ENABLED, DEFAULT_INTEGRATIONS_ENABLED);
    writerType = getSettingFromEnvironment(WRITER_TYPE, DEFAULT_AGENT_WRITER_TYPE);
    apiType = getSettingFromEnvironment(API_TYPE, DEFAULT_API_TYPE);
    useB3Propagation = getBooleanSettingFromEnvironment(USE_B3_PROPAGATION, true);
    agentHost = getSettingFromEnvironment(AGENT_HOST, null);
    agentPort =
        getIntegerSettingFromEnvironment(
            TRACE_AGENT_PORT, getIntegerSettingFromEnvironment(AGENT_PORT_LEGACY, null));
    agentPath = getSettingFromEnvironment(AGENT_PATH, null);
    agentUseHTTPS = getBooleanSettingFromEnvironment(AGENT_USE_HTTPS, null);
    endpointUrl = getURLSettingFromEnvironment(ENDPOINT_URL, DEFAULT_AGENT_ENDPOINT);
    agentUnixDomainSocket =
        getSettingFromEnvironment(AGENT_UNIX_DOMAIN_SOCKET, DEFAULT_AGENT_UNIX_DOMAIN_SOCKET);
    prioritySamplingEnabled =
        getBooleanSettingFromEnvironment(PRIORITY_SAMPLING, DEFAULT_PRIORITY_SAMPLING_ENABLED);
    traceResolverEnabled =
        getBooleanSettingFromEnvironment(TRACE_RESOLVER_ENABLED, DEFAULT_TRACE_RESOLVER_ENABLED);
    serviceMapping = getMapSettingFromEnvironment(SERVICE_MAPPING, null);

    tags = getMapSettingFromEnvironment(TAGS, null);
    globalTags = getMapSettingFromEnvironment(GLOBAL_TAGS, null);
    spanTags = getMapSettingFromEnvironment(SPAN_TAGS, null);
    jmxTags = getMapSettingFromEnvironment(JMX_TAGS, null);

    excludedClasses = getListSettingFromEnvironment(TRACE_CLASSES_EXCLUDE, null);
    headerTags = getMapSettingFromEnvironment(HEADER_TAGS, null);

    httpServerErrorStatuses =
        getIntegerRangeSettingFromEnvironment(
            HTTP_SERVER_ERROR_STATUSES, DEFAULT_HTTP_SERVER_ERROR_STATUSES);

    httpClientErrorStatuses =
        getIntegerRangeSettingFromEnvironment(
            HTTP_CLIENT_ERROR_STATUSES, DEFAULT_HTTP_CLIENT_ERROR_STATUSES);

    httpServerTagQueryString =
        getBooleanSettingFromEnvironment(
            HTTP_SERVER_TAG_QUERY_STRING, DEFAULT_HTTP_SERVER_TAG_QUERY_STRING);

    httpClientTagQueryString =
        getBooleanSettingFromEnvironment(
            HTTP_CLIENT_TAG_QUERY_STRING, DEFAULT_HTTP_CLIENT_TAG_QUERY_STRING);

    httpClientSplitByDomain =
        getBooleanSettingFromEnvironment(
            HTTP_CLIENT_HOST_SPLIT_BY_DOMAIN, DEFAULT_HTTP_CLIENT_SPLIT_BY_DOMAIN);

    dbClientSplitByInstance =
        getBooleanSettingFromEnvironment(
            DB_CLIENT_HOST_SPLIT_BY_INSTANCE, DEFAULT_DB_CLIENT_HOST_SPLIT_BY_INSTANCE);

    splitByTags =
        Collections.unmodifiableSet(
            new LinkedHashSet<>(
                getListSettingFromEnvironment(SPLIT_BY_TAGS, DEFAULT_SPLIT_BY_TAGS)));

    scopeDepthLimit =
        getIntegerSettingFromEnvironment(SCOPE_DEPTH_LIMIT, DEFAULT_SCOPE_DEPTH_LIMIT);

    partialFlushMinSpans =
        getIntegerSettingFromEnvironment(PARTIAL_FLUSH_MIN_SPANS, DEFAULT_PARTIAL_FLUSH_MIN_SPANS);

    runtimeContextFieldInjection =
        getBooleanSettingFromEnvironment(
            RUNTIME_CONTEXT_FIELD_INJECTION, DEFAULT_RUNTIME_CONTEXT_FIELD_INJECTION);

    propagationStylesToExtract =
        getPropagationStyleSetSettingFromEnvironmentOrDefault(
            PROPAGATION_STYLE_EXTRACT, DEFAULT_PROPAGATION_STYLE_EXTRACT);
    propagationStylesToInject =
        getPropagationStyleSetSettingFromEnvironmentOrDefault(
            PROPAGATION_STYLE_INJECT, DEFAULT_PROPAGATION_STYLE_INJECT);

    kafkaAttemptPropagation =
        getBooleanSettingFromEnvironment(
            KAFKA_ATTEMPT_PROPAGATION, DEFAULT_KAFKA_ATTEMPT_PROPAGATION);
    redisCaptureCommandArguments =
        getBooleanSettingFromEnvironment(
            REDIS_CAPTURE_COMMAND_ARGUMENTS, DEFAULT_REDIS_CAPTURE_COMMAND_ARGUMENTS);

    zipkinGZIPContentEncoding =
        getBooleanSettingFromEnvironment(
            ZIPKIN_GZIP_CONTENT_ENCODING, DEFAULT_ZIPKIN_GZIP_CONTENT_ENCODING);

    jmxFetchEnabled =
        getBooleanSettingFromEnvironment(JMX_FETCH_ENABLED, DEFAULT_JMX_FETCH_ENABLED);
    jmxFetchConfigDir = getSettingFromEnvironment(JMX_FETCH_CONFIG_DIR, null);
    jmxFetchConfigs = getListSettingFromEnvironment(JMX_FETCH_CONFIG, null);
    jmxFetchMetricsConfigs = getListSettingFromEnvironment(JMX_FETCH_METRICS_CONFIGS, null);
    jmxFetchCheckPeriod = getIntegerSettingFromEnvironment(JMX_FETCH_CHECK_PERIOD, null);
    jmxFetchRefreshBeansPeriod =
        getIntegerSettingFromEnvironment(JMX_FETCH_REFRESH_BEANS_PERIOD, null);
    jmxFetchStatsdHost = getSettingFromEnvironment(JMX_FETCH_STATSD_HOST, null);
    jmxFetchStatsdPort =
        getIntegerSettingFromEnvironment(JMX_FETCH_STATSD_PORT, DEFAULT_JMX_FETCH_STATSD_PORT);

    // Writer.Builder createMonitor will use the values of the JMX fetch & agent to fill-in defaults
    healthMetricsEnabled =
        getBooleanSettingFromEnvironment(HEALTH_METRICS_ENABLED, DEFAULT_METRICS_ENABLED);
    healthMetricsStatsdHost = getSettingFromEnvironment(HEALTH_METRICS_STATSD_HOST, null);
    healthMetricsStatsdPort = getIntegerSettingFromEnvironment(HEALTH_METRICS_STATSD_PORT, null);

    logsInjectionEnabled =
        getBooleanSettingFromEnvironment(LOGS_INJECTION_ENABLED, DEFAULT_LOGS_INJECTION_ENABLED);
    reportHostName =
        getBooleanSettingFromEnvironment(TRACE_REPORT_HOSTNAME, DEFAULT_TRACE_REPORT_HOSTNAME);

    dbStatementMaxLength =
        getIntegerSettingFromEnvironment(DB_STATEMENT_MAX_LENGTH, DEFAULT_DB_STATEMENT_MAX_LENGTH);

    recordedValueMaxLength =
        getIntegerSettingFromEnvironment(
            RECORDED_VALUE_MAX_LENGTH, DEFAULT_RECORDED_VALUE_MAX_LENGTH);

    traceAnnotations = getSettingFromEnvironment(TRACE_ANNOTATIONS, DEFAULT_TRACE_ANNOTATIONS);
    annotatedMethodBlacklist =
        getSettingFromEnvironment(ANNOTATED_METHOD_BLACKLIST, DEFAULT_ANNOTATED_METHOD_BLACKLIST);

    traceMethods = getSettingFromEnvironment(TRACE_METHODS, DEFAULT_TRACE_METHODS);

    traceExecutorsAll =
        getBooleanSettingFromEnvironment(TRACE_EXECUTORS_ALL, DEFAULT_TRACE_EXECUTORS_ALL);

    traceExecutors = getListSettingFromEnvironment(TRACE_EXECUTORS, DEFAULT_TRACE_EXECUTORS);

    traceAnalyticsEnabled =
        getBooleanSettingFromEnvironment(TRACE_ANALYTICS_ENABLED, DEFAULT_TRACE_ANALYTICS_ENABLED);

    maxSpansPerTrace =
        getIntegerSettingFromEnvironment(MAX_SPANS_PER_TRACE, DEFAULT_MAX_SPANS_PER_TRACE);
    maxContinuationDepth =
        getIntegerSettingFromEnvironment(MAX_CONTINUATION_DEPTH, DEFAULT_MAX_CONTINUATION_DEPTH);
    emitServerTimingContext =
        getBooleanSettingFromEnvironment(SERVER_TIMING_CONTEXT, DEFAULT_SERVER_TIMING_CONTEXT);

    traceSamplingServiceRules = getMapSettingFromEnvironment(TRACE_SAMPLING_SERVICE_RULES, null);
    traceSamplingOperationRules =
        getMapSettingFromEnvironment(TRACE_SAMPLING_OPERATION_RULES, null);
    traceSampleRate = getDoubleSettingFromEnvironment(TRACE_SAMPLE_RATE, null);
    traceRateLimit = getDoubleSettingFromEnvironment(TRACE_RATE_LIMIT, DEFAULT_TRACE_RATE_LIMIT);

    profilingEnabled =
        getBooleanSettingFromEnvironment(PROFILING_ENABLED, DEFAULT_PROFILING_ENABLED);
    profilingUrl = getSettingFromEnvironment(PROFILING_URL, null);

    if (tmpApiKey == null) {
      final String oldProfilingApiKeyFile =
          getSettingFromEnvironment(PROFILING_API_KEY_FILE_OLD, null);
      tmpApiKey = getSettingFromEnvironmentVariables(PROFILING_API_KEY_OLD);
      if (oldProfilingApiKeyFile != null) {
        try {
          tmpApiKey =
              new String(
                      Files.readAllBytes(Paths.get(oldProfilingApiKeyFile)), StandardCharsets.UTF_8)
                  .trim();
        } catch (final IOException e) {
          log.error("Cannot read API key from file {}, skipping", oldProfilingApiKeyFile, e);
        }
      }
    }
    if (tmpApiKey == null) {
      final String veryOldProfilingApiKeyFile =
          getSettingFromEnvironment(PROFILING_API_KEY_FILE_VERY_OLD, null);
      tmpApiKey = getSettingFromEnvironmentVariables(PROFILING_API_KEY_VERY_OLD);
      if (veryOldProfilingApiKeyFile != null) {
        try {
          tmpApiKey =
              new String(
                      Files.readAllBytes(Paths.get(veryOldProfilingApiKeyFile)),
                      StandardCharsets.UTF_8)
                  .trim();
        } catch (final IOException e) {
          log.error("Cannot read API key from file {}, skipping", veryOldProfilingApiKeyFile, e);
        }
      }
    }

    profilingTags = getMapSettingFromEnvironment(PROFILING_TAGS, null);
    profilingStartDelay =
        getIntegerSettingFromEnvironment(PROFILING_START_DELAY, DEFAULT_PROFILING_START_DELAY);
    profilingStartForceFirst =
        getBooleanSettingFromEnvironment(
            PROFILING_START_FORCE_FIRST, DEFAULT_PROFILING_START_FORCE_FIRST);
    profilingUploadPeriod =
        getIntegerSettingFromEnvironment(PROFILING_UPLOAD_PERIOD, DEFAULT_PROFILING_UPLOAD_PERIOD);
    profilingTemplateOverrideFile =
        getSettingFromEnvironment(PROFILING_TEMPLATE_OVERRIDE_FILE, null);
    profilingUploadTimeout =
        getIntegerSettingFromEnvironment(
            PROFILING_UPLOAD_TIMEOUT, DEFAULT_PROFILING_UPLOAD_TIMEOUT);
    profilingUploadCompression =
        getSettingFromEnvironment(
            PROFILING_UPLOAD_COMPRESSION, DEFAULT_PROFILING_UPLOAD_COMPRESSION);
    profilingProxyHost = getSettingFromEnvironment(PROFILING_PROXY_HOST, null);
    profilingProxyPort =
        getIntegerSettingFromEnvironment(PROFILING_PROXY_PORT, DEFAULT_PROFILING_PROXY_PORT);
    profilingProxyUsername = getSettingFromEnvironment(PROFILING_PROXY_USERNAME, null);
    profilingProxyPassword = getSettingFromEnvironment(PROFILING_PROXY_PASSWORD, null);

    // Setting this last because we have a few places where this can come from
    apiKey = tmpApiKey;

    log.debug("New instance: {}", this);
  }

  // Read order: Properties -> Parent
  private Config(final Properties properties, final Config parent) {
    runtimeId = parent.runtimeId;

    apiKey = properties.getProperty(API_KEY, parent.apiKey);
    site = properties.getProperty(SITE, parent.site);
    serviceName = properties.getProperty(SERVICE_NAME, parent.serviceName);
    environmentName = properties.getProperty(ENV, parent.environmentName);

    traceEnabled = getPropertyBooleanValue(properties, TRACE_ENABLED, parent.traceEnabled);
    integrationsEnabled =
        getPropertyBooleanValue(properties, INTEGRATIONS_ENABLED, parent.integrationsEnabled);
    writerType = properties.getProperty(WRITER_TYPE, parent.writerType);
    apiType = properties.getProperty(API_TYPE, parent.apiType);
    useB3Propagation =
        getPropertyBooleanValue(properties, USE_B3_PROPAGATION, parent.useB3Propagation);
    agentHost = properties.getProperty(AGENT_HOST, parent.agentHost);
    agentPort =
        getPropertyIntegerValue(
            properties,
            TRACE_AGENT_PORT,
            getPropertyIntegerValue(properties, AGENT_PORT_LEGACY, parent.agentPort));
    agentPath = properties.getProperty(AGENT_PATH, parent.agentPath);
    agentUseHTTPS = getPropertyBooleanValue(properties, AGENT_USE_HTTPS, parent.agentUseHTTPS);
    endpointUrl = getPropertyURLValue(properties, ENDPOINT_URL, parent.endpointUrl);
    agentUnixDomainSocket =
        properties.getProperty(AGENT_UNIX_DOMAIN_SOCKET, parent.agentUnixDomainSocket);
    prioritySamplingEnabled =
        getPropertyBooleanValue(properties, PRIORITY_SAMPLING, parent.prioritySamplingEnabled);
    traceResolverEnabled =
        getPropertyBooleanValue(properties, TRACE_RESOLVER_ENABLED, parent.traceResolverEnabled);
    serviceMapping = getPropertyMapValue(properties, SERVICE_MAPPING, parent.serviceMapping);

    tags = getPropertyMapValue(properties, TAGS, parent.tags);
    globalTags = getPropertyMapValue(properties, GLOBAL_TAGS, parent.globalTags);
    spanTags = getPropertyMapValue(properties, SPAN_TAGS, parent.spanTags);
    jmxTags = getPropertyMapValue(properties, JMX_TAGS, parent.jmxTags);
    excludedClasses =
        getPropertyListValue(properties, TRACE_CLASSES_EXCLUDE, parent.excludedClasses);
    headerTags = getPropertyMapValue(properties, HEADER_TAGS, parent.headerTags);

    httpServerErrorStatuses =
        getPropertyIntegerRangeValue(
            properties, HTTP_SERVER_ERROR_STATUSES, parent.httpServerErrorStatuses);

    httpClientErrorStatuses =
        getPropertyIntegerRangeValue(
            properties, HTTP_CLIENT_ERROR_STATUSES, parent.httpClientErrorStatuses);

    httpServerTagQueryString =
        getPropertyBooleanValue(
            properties, HTTP_SERVER_TAG_QUERY_STRING, parent.httpServerTagQueryString);

    httpClientTagQueryString =
        getPropertyBooleanValue(
            properties, HTTP_CLIENT_TAG_QUERY_STRING, parent.httpClientTagQueryString);

    httpClientSplitByDomain =
        getPropertyBooleanValue(
            properties, HTTP_CLIENT_HOST_SPLIT_BY_DOMAIN, parent.httpClientSplitByDomain);

    dbClientSplitByInstance =
        getPropertyBooleanValue(
            properties, DB_CLIENT_HOST_SPLIT_BY_INSTANCE, parent.dbClientSplitByInstance);

    splitByTags =
        Collections.unmodifiableSet(
            new LinkedHashSet<>(
                getPropertyListValue(
                    properties, SPLIT_BY_TAGS, new ArrayList<>(parent.splitByTags))));

    scopeDepthLimit =
        getPropertyIntegerValue(properties, SCOPE_DEPTH_LIMIT, parent.scopeDepthLimit);

    partialFlushMinSpans =
        getPropertyIntegerValue(properties, PARTIAL_FLUSH_MIN_SPANS, parent.partialFlushMinSpans);

    runtimeContextFieldInjection =
        getPropertyBooleanValue(
            properties, RUNTIME_CONTEXT_FIELD_INJECTION, parent.runtimeContextFieldInjection);

    final Set<PropagationStyle> parsedPropagationStylesToExtract =
        getPropagationStyleSetFromPropertyValue(properties, PROPAGATION_STYLE_EXTRACT);
    propagationStylesToExtract =
        parsedPropagationStylesToExtract == null
            ? parent.propagationStylesToExtract
            : parsedPropagationStylesToExtract;
    final Set<PropagationStyle> parsedPropagationStylesToInject =
        getPropagationStyleSetFromPropertyValue(properties, PROPAGATION_STYLE_INJECT);
    propagationStylesToInject =
        parsedPropagationStylesToInject == null
            ? parent.propagationStylesToInject
            : parsedPropagationStylesToInject;

    kafkaAttemptPropagation =
        getPropertyBooleanValue(
            properties, KAFKA_ATTEMPT_PROPAGATION, parent.kafkaAttemptPropagation);

    redisCaptureCommandArguments =
        getPropertyBooleanValue(
            properties, REDIS_CAPTURE_COMMAND_ARGUMENTS, parent.redisCaptureCommandArguments);

    zipkinGZIPContentEncoding =
        getPropertyBooleanValue(
            properties, ZIPKIN_GZIP_CONTENT_ENCODING, parent.zipkinGZIPContentEncoding);

    jmxFetchEnabled =
        getPropertyBooleanValue(properties, JMX_FETCH_ENABLED, parent.jmxFetchEnabled);
    jmxFetchConfigDir = properties.getProperty(JMX_FETCH_CONFIG_DIR, parent.jmxFetchConfigDir);
    jmxFetchConfigs = getPropertyListValue(properties, JMX_FETCH_CONFIG, parent.jmxFetchConfigs);
    jmxFetchMetricsConfigs =
        getPropertyListValue(properties, JMX_FETCH_METRICS_CONFIGS, parent.jmxFetchMetricsConfigs);
    jmxFetchCheckPeriod =
        getPropertyIntegerValue(properties, JMX_FETCH_CHECK_PERIOD, parent.jmxFetchCheckPeriod);
    jmxFetchRefreshBeansPeriod =
        getPropertyIntegerValue(
            properties, JMX_FETCH_REFRESH_BEANS_PERIOD, parent.jmxFetchRefreshBeansPeriod);
    jmxFetchStatsdHost = properties.getProperty(JMX_FETCH_STATSD_HOST, parent.jmxFetchStatsdHost);
    jmxFetchStatsdPort =
        getPropertyIntegerValue(properties, JMX_FETCH_STATSD_PORT, parent.jmxFetchStatsdPort);

    healthMetricsEnabled =
        getPropertyBooleanValue(properties, HEALTH_METRICS_ENABLED, DEFAULT_METRICS_ENABLED);
    healthMetricsStatsdHost =
        properties.getProperty(HEALTH_METRICS_STATSD_HOST, parent.healthMetricsStatsdHost);
    healthMetricsStatsdPort =
        getPropertyIntegerValue(
            properties, HEALTH_METRICS_STATSD_PORT, parent.healthMetricsStatsdPort);

    logsInjectionEnabled =
        getBooleanSettingFromEnvironment(LOGS_INJECTION_ENABLED, DEFAULT_LOGS_INJECTION_ENABLED);
    reportHostName =
        getPropertyBooleanValue(properties, TRACE_REPORT_HOSTNAME, parent.reportHostName);

    dbStatementMaxLength =
        getPropertyIntegerValue(properties, DB_STATEMENT_MAX_LENGTH, parent.dbStatementMaxLength);

    recordedValueMaxLength =
        getPropertyIntegerValue(
            properties, RECORDED_VALUE_MAX_LENGTH, parent.recordedValueMaxLength);

    traceAnnotations = properties.getProperty(TRACE_ANNOTATIONS, parent.traceAnnotations);
    annotatedMethodBlacklist =
        properties.getProperty(ANNOTATED_METHOD_BLACKLIST, parent.annotatedMethodBlacklist);

    traceMethods = properties.getProperty(TRACE_METHODS, parent.traceMethods);

    traceExecutorsAll =
        getPropertyBooleanValue(properties, TRACE_EXECUTORS_ALL, parent.traceExecutorsAll);
    traceExecutors = getPropertyListValue(properties, TRACE_EXECUTORS, parent.traceExecutors);

    traceAnalyticsEnabled =
        getPropertyBooleanValue(properties, TRACE_ANALYTICS_ENABLED, parent.traceAnalyticsEnabled);

    maxSpansPerTrace =
        getPropertyIntegerValue(properties, MAX_SPANS_PER_TRACE, DEFAULT_MAX_SPANS_PER_TRACE);
    maxContinuationDepth =
        getPropertyIntegerValue(properties, MAX_CONTINUATION_DEPTH, DEFAULT_MAX_CONTINUATION_DEPTH);
    emitServerTimingContext =
        getPropertyBooleanValue(properties, SERVER_TIMING_CONTEXT, DEFAULT_SERVER_TIMING_CONTEXT);
    traceSamplingServiceRules =
        getPropertyMapValue(
            properties, TRACE_SAMPLING_SERVICE_RULES, parent.traceSamplingServiceRules);
    traceSamplingOperationRules =
        getPropertyMapValue(
            properties, TRACE_SAMPLING_OPERATION_RULES, parent.traceSamplingOperationRules);
    traceSampleRate = getPropertyDoubleValue(properties, TRACE_SAMPLE_RATE, parent.traceSampleRate);
    traceRateLimit = getPropertyDoubleValue(properties, TRACE_RATE_LIMIT, parent.traceRateLimit);

    profilingEnabled =
        getPropertyBooleanValue(properties, PROFILING_ENABLED, parent.profilingEnabled);
    profilingUrl = properties.getProperty(PROFILING_URL, parent.profilingUrl);
    profilingTags = getPropertyMapValue(properties, PROFILING_TAGS, parent.profilingTags);
    profilingStartDelay =
        getPropertyIntegerValue(properties, PROFILING_START_DELAY, parent.profilingStartDelay);
    profilingStartForceFirst =
        getPropertyBooleanValue(
            properties, PROFILING_START_FORCE_FIRST, parent.profilingStartForceFirst);
    profilingUploadPeriod =
        getPropertyIntegerValue(properties, PROFILING_UPLOAD_PERIOD, parent.profilingUploadPeriod);
    profilingTemplateOverrideFile =
        properties.getProperty(
            PROFILING_TEMPLATE_OVERRIDE_FILE, parent.profilingTemplateOverrideFile);
    profilingUploadTimeout =
        getPropertyIntegerValue(
            properties, PROFILING_UPLOAD_TIMEOUT, parent.profilingUploadTimeout);
    profilingUploadCompression =
        properties.getProperty(PROFILING_UPLOAD_COMPRESSION, parent.profilingUploadCompression);
    profilingProxyHost = properties.getProperty(PROFILING_PROXY_HOST, parent.profilingProxyHost);
    profilingProxyPort =
        getPropertyIntegerValue(properties, PROFILING_PROXY_PORT, parent.profilingProxyPort);
    profilingProxyUsername =
        properties.getProperty(PROFILING_PROXY_USERNAME, parent.profilingProxyUsername);
    profilingProxyPassword =
        properties.getProperty(PROFILING_PROXY_PASSWORD, parent.profilingProxyPassword);

    log.debug("New instance: {}", this);
  }

  /** @return A map of tags to be applied only to the local application root span. */
  public Map<String, String> getLocalRootSpanTags() {
    final Map<String, String> runtimeTags = getRuntimeTags();
    final Map<String, String> result = new HashMap<>(runtimeTags);
    result.put(TRACING_LIBRARY_KEY, TRACING_LIBRARY_VALUE);
    result.put(TRACING_VERSION_KEY, TRACING_VERSION_VALUE);

    if (reportHostName) {
      final String hostName = getHostName();
      if (null != hostName && !hostName.isEmpty()) {
        result.put(INTERNAL_HOST_NAME, hostName);
      }
    }

    return Collections.unmodifiableMap(result);
  }

  public String getAgentHost() {
    if (agentHost != null) {
      return agentHost;
    }
    return endpointUrl == null ? null : endpointUrl.getHost();
  }

  public Integer getAgentPort() {
    if (agentPort != null) {
      return agentPort;
    }
    return endpointUrl == null ? null : endpointUrl.getPort();
  }

  public Boolean getAgentUseHTTPS() {
    if (agentUseHTTPS != null) {
      return agentUseHTTPS;
    }
    return endpointUrl == null ? null : "https".equals(endpointUrl.getProtocol());
  }

  public String getAgentPath() {
    if (agentPath != null) {
      return agentPath;
    }
    return endpointUrl == null ? null : endpointUrl.getPath();
  }

  public Map<String, String> getMergedSpanTags() {
    // Do not include runtimeId into span tags: we only want that added to the root span
    final Map<String, String> result = newHashMap(getGlobalTags().size() + spanTags.size());
    result.putAll(getGlobalTags());
    result.putAll(spanTags);

    if (!environmentName.isEmpty()) {
      result.put("environment", environmentName);
    }

    addPropToMapIfDefinedByEnvironment(result, VERSION);

    return Collections.unmodifiableMap(result);
  }

  public Map<String, String> getMergedJmxTags() {
    final Map<String, String> runtimeTags = getRuntimeTags();
    final Map<String, String> result =
        newHashMap(
            getGlobalTags().size() + jmxTags.size() + runtimeTags.size() + 1 /* for serviceName */);
    result.putAll(getGlobalTags());
    result.putAll(jmxTags);
    result.putAll(runtimeTags);
    // service name set here instead of getRuntimeTags because apm already manages the service tag
    // and may chose to override it.
    // Additionally, infra/JMX metrics require `service` rather than APM's `service.name` tag
    result.put(SERVICE_TAG, serviceName);
    return Collections.unmodifiableMap(result);
  }

  public Map<String, String> getMergedProfilingTags() {
    final Map<String, String> runtimeTags = getRuntimeTags();
    final String host = getHostName();
    final Map<String, String> result =
        newHashMap(
            getGlobalTags().size()
                + profilingTags.size()
                + runtimeTags.size()
                + 3 /* for serviceName and host and language */);
    result.put(HOST_TAG, host); // Host goes first to allow to override it
    result.putAll(getGlobalTags());
    result.putAll(profilingTags);
    result.putAll(runtimeTags);
    // service name set here instead of getRuntimeTags because apm already manages the service tag
    // and may chose to override it.
    result.put(SERVICE_TAG, serviceName);
    result.put(LANGUAGE_TAG_KEY, LANGUAGE_TAG_VALUE);
    return Collections.unmodifiableMap(result);
  }

  /**
   * Returns the sample rate for the specified instrumentation or {@link
   * #DEFAULT_ANALYTICS_SAMPLE_RATE} if none specified.
   */
  public float getInstrumentationAnalyticsSampleRate(final String... aliases) {
    for (final String alias : aliases) {
      final Float rate = getFloatSettingFromEnvironment(alias + ".analytics.sample-rate", null);
      if (null != rate) {
        return rate;
      }
    }
    return DEFAULT_ANALYTICS_SAMPLE_RATE;
  }

  /**
   * Provide 'global' tags, i.e. tags set everywhere. We have to support old (dd.trace.global.tags)
   * version of this setting if new (dd.tags) version has not been specified.
   */
  private Map<String, String> getGlobalTags() {
    return tags.isEmpty() ? globalTags : tags;
  }

  /**
   * Return a map of tags required by the datadog backend to link runtime metrics (i.e. jmx) and
   * traces.
   *
   * <p>These tags must be applied to every runtime metrics and placed on the root span of every
   * trace.
   *
   * @return A map of tag-name -> tag-value
   */
  private Map<String, String> getRuntimeTags() {
    // Rather than attempt to strip these on Zipkin encoding, do not use any
    // undesired runtime tags to prevent collisions
    final Map<String, String> result = newHashMap(0);
    return Collections.unmodifiableMap(result);
  }

  public String getFinalProfilingUrl() {
    if (profilingUrl == null) {
      return String.format(PROFILING_URL_TEMPLATE, site);
    } else {
      return profilingUrl;
    }
  }

  public boolean isIntegrationEnabled(
      final SortedSet<String> integrationNames, final boolean defaultEnabled) {
    return integrationEnabled(integrationNames, defaultEnabled);
  }

  /**
   * @param integrationNames
   * @param defaultEnabled
   * @return
   * @deprecated This method should only be used internally. Use the instance getter instead {@link
   *     #isIntegrationEnabled(SortedSet, boolean)}.
   */
  @Deprecated
  private static boolean integrationEnabled(
      final SortedSet<String> integrationNames, final boolean defaultEnabled) {
    // If default is enabled, we want to enable individually,
    // if default is disabled, we want to disable individually.
    boolean anyEnabled = defaultEnabled;
    for (final String name : integrationNames) {
      final boolean configEnabled =
          getBooleanSettingFromEnvironment("integration." + name + ".enabled", defaultEnabled);
      if (defaultEnabled) {
        anyEnabled &= configEnabled;
      } else {
        anyEnabled |= configEnabled;
      }
    }
    return anyEnabled;
  }

  public boolean isJmxFetchIntegrationEnabled(
      final SortedSet<String> integrationNames, final boolean defaultEnabled) {
    return jmxFetchIntegrationEnabled(integrationNames, defaultEnabled);
  }

  public boolean isDecoratorEnabled(final String name) {
    return getBooleanSettingFromEnvironment("trace." + name + ".enabled", true)
        && getBooleanSettingFromEnvironment("trace." + name.toLowerCase() + ".enabled", true);
  }

  /**
   * @param integrationNames
   * @param defaultEnabled
   * @return
   * @deprecated This method should only be used internally. Use the instance getter instead {@link
   *     #isJmxFetchIntegrationEnabled(SortedSet, boolean)}.
   */
  public static boolean jmxFetchIntegrationEnabled(
      final SortedSet<String> integrationNames, final boolean defaultEnabled) {
    // If default is enabled, we want to enable individually,
    // if default is disabled, we want to disable individually.
    boolean anyEnabled = defaultEnabled;
    for (final String name : integrationNames) {
      final boolean configEnabled =
          getBooleanSettingFromEnvironment("jmxfetch." + name + ".enabled", defaultEnabled);
      if (defaultEnabled) {
        anyEnabled &= configEnabled;
      } else {
        anyEnabled |= configEnabled;
      }
    }
    return anyEnabled;
  }

  public boolean isTraceAnalyticsIntegrationEnabled(
      final SortedSet<String> integrationNames, final boolean defaultEnabled) {
    return traceAnalyticsIntegrationEnabled(integrationNames, defaultEnabled);
  }

  /**
   * @param integrationNames
   * @param defaultEnabled
   * @return
   * @deprecated This method should only be used internally. Use the instance getter instead {@link
   *     #isTraceAnalyticsIntegrationEnabled(SortedSet, boolean)}.
   */
  public static boolean traceAnalyticsIntegrationEnabled(
      final SortedSet<String> integrationNames, final boolean defaultEnabled) {
    // If default is enabled, we want to enable individually,
    // if default is disabled, we want to disable individually.
    boolean anyEnabled = defaultEnabled;
    for (final String name : integrationNames) {
      final boolean configEnabled =
          getBooleanSettingFromEnvironment(name + ".analytics.enabled", defaultEnabled);
      if (defaultEnabled) {
        anyEnabled &= configEnabled;
      } else {
        anyEnabled |= configEnabled;
      }
    }
    return anyEnabled;
  }

  /**
   * Helper method that takes the name, adds a "signalfx." prefix then checks for System Properties
   * of that name. If none found, the name is converted to an Environment Variable and used to check
   * the env. If none of the above returns a value, then an optional properties file if checked. If
   * setting is not configured in either location, the process repeats with the "dd." prefix. If no
   * subsequent mtach, <code>defaultValue</code> is returned.
   *
   * @param name
   * @param defaultValue
   * @return
   * @deprecated This method should only be used internally. Use the explicit getter instead.
   */
  public static String getSettingFromEnvironment(final String name, final String defaultValue) {
    String value = getSettingFromEnvironment(SIGNALFX_PREFIX, name, null);
    if (value == null) {
      // Let the original prefix act as a fallback so we support both for testing/migration
      // purposes.
      value = getSettingFromEnvironment(PREFIX, name, null);
    }
    String returned = value == null ? defaultValue : value;
    return returned;
  }

  public static String getSettingFromEnvironment(
      final String prefix, final String name, final String defaultValue) {
    String value;
    final String systemPropertyName = propertyNameToSystemPropertyName(prefix, name);

    // System properties and properties provided from command line have the highest precedence
    value = System.getProperties().getProperty(systemPropertyName);
    if (null != value) {
      return value;
    }

    // If value not provided from system properties, looking at env variables
    value = System.getenv(propertyNameToEnvironmentVariableName(prefix, name));
    if (null != value) {
      return value;
    }

    // If value is not defined yet, we look at properties optionally defined in a properties file
    value = propertiesFromConfigFile.getProperty(systemPropertyName);
    if (null != value) {
      return value;
    }

    return defaultValue;
  }

  public static String getSettingFromEnvironmentVariables(final String name) {
    String value = System.getenv(propertyNameToEnvironmentVariableName(SIGNALFX_PREFIX, name));
    if (null != value) {
      return value;
    }
    return System.getenv(propertyNameToEnvironmentVariableName(PREFIX, name));
  }

  /** @deprecated This method should only be used internally. Use the explicit getter instead. */
  @NonNull
  private static Map<String, String> getMapSettingFromEnvironment(
      final String name, final String defaultValue) {
    return parseMap(
        getSettingFromEnvironment(name, defaultValue), propertyNameToSystemPropertyName(name));
  }

  /**
   * Calls {@link #getSettingFromEnvironment(String, String)} and converts the result to a list by
   * splitting on `,`.
   *
   * @deprecated This method should only be used internally. Use the explicit getter instead.
   */
  @NonNull
  private static List<String> getListSettingFromEnvironment(
      final String name, final String defaultValue) {
    return parseList(getSettingFromEnvironment(name, defaultValue));
  }

  private static URL getURLSettingFromEnvironment(final String name, final String defaultValue) {
    return parseURL(getSettingFromEnvironment(name, defaultValue), name);
  }

  /**
   * Calls {@link #getSettingFromEnvironment(String, String)} and converts the result to a Boolean.
   *
   * @deprecated This method should only be used internally. Use the explicit getter instead.
   */
  public static Boolean getBooleanSettingFromEnvironment(
      final String name, final Boolean defaultValue) {
    return getSettingFromEnvironmentWithLog(name, Boolean.class, defaultValue);
  }

  /**
   * Calls {@link #getSettingFromEnvironment(String, String)} and converts the result to a Float.
   *
   * @deprecated This method should only be used internally. Use the explicit getter instead.
   */
  public static Float getFloatSettingFromEnvironment(final String name, final Float defaultValue) {
    return getSettingFromEnvironmentWithLog(name, Float.class, defaultValue);
  }

  /**
   * Calls {@link #getSettingFromEnvironment(String, String)} and converts the result to a Double.
   *
   * @deprecated This method should only be used internally. Use the explicit getter instead.
   */
  @Deprecated
  private static Double getDoubleSettingFromEnvironment(
      final String name, final Double defaultValue) {
    return getSettingFromEnvironmentWithLog(name, Double.class, defaultValue);
  }

  /**
   * Calls {@link #getSettingFromEnvironment(String, String)} and converts the result to a Integer.
   */
  private static Integer getIntegerSettingFromEnvironment(
      final String name, final Integer defaultValue) {
    return getSettingFromEnvironmentWithLog(name, Integer.class, defaultValue);
  }

  private static <T> T getSettingFromEnvironmentWithLog(
      final String name, Class<T> tClass, final T defaultValue) {
    try {
      return valueOf(getSettingFromEnvironment(name, null), tClass, defaultValue);
    } catch (final NumberFormatException e) {
      log.warn("Invalid configuration for " + name, e);
      return defaultValue;
    }
  }

  /**
   * Calls {@link #getSettingFromEnvironment(String, String)} and converts the result to a set of
   * strings splitting by space or comma.
   */
  private static Set<PropagationStyle> getPropagationStyleSetSettingFromEnvironmentOrDefault(
      final String name, final String defaultValue) {
    final String value = getSettingFromEnvironment(name, defaultValue);
    Set<PropagationStyle> result =
        convertStringSetToPropagationStyleSet(parseStringIntoSetOfNonEmptyStrings(value));

    if (result.isEmpty()) {
      // Treat empty parsing result as no value and use default instead
      result =
          convertStringSetToPropagationStyleSet(parseStringIntoSetOfNonEmptyStrings(defaultValue));
    }

    return result;
  }

  private static Set<Integer> getIntegerRangeSettingFromEnvironment(
      final String name, final Set<Integer> defaultValue) {
    final String value = getSettingFromEnvironment(name, null);
    try {
      return value == null ? defaultValue : parseIntegerRangeSet(value, name);
    } catch (final NumberFormatException e) {
      log.warn("Invalid configuration for " + name, e);
      return defaultValue;
    }
  }

  /**
   * Converts the property name, e.g. 'service.name' into a public environment variable name, e.g.
   * `SIGNALFX_SERVICE_NAME`.
   *
   * @param setting The setting name, e.g. `service.name`
   * @return The public facing environment variable name
   */
  @NonNull
  private static String propertyNameToEnvironmentVariableName(final String setting) {
    return propertyNameToEnvironmentVariableName(SIGNALFX_PREFIX, setting);
  }

  private static String propertyNameToEnvironmentVariableName(
      final String prefix, final String setting) {
    return ENV_REPLACEMENT
        .matcher(propertyNameToSystemPropertyName(prefix, setting).toUpperCase())
        .replaceAll("_");
  }

  /**
   * Converts the property name, e.g. 'service.name' into a public system property name, e.g.
   * `signalfx.service.name`.
   *
   * @param setting The setting name, e.g. `service.name`
   * @return The public facing system property name
   */
  @NonNull
  private static String propertyNameToSystemPropertyName(final String setting) {
    return propertyNameToSystemPropertyName(SIGNALFX_PREFIX, setting);
  }

  private static String propertyNameToSystemPropertyName(
      final String prefix, final String setting) {
    return prefix + setting;
  }

  /**
   * @param value to parse by tClass::valueOf
   * @param tClass should contain static parsing method "T valueOf(String)"
   * @param defaultValue
   * @param <T>
   * @return value == null || value.trim().isEmpty() ? defaultValue : tClass.valueOf(value)
   * @throws NumberFormatException
   */
  private static <T> T valueOf(
      final String value, @NonNull final Class<T> tClass, final T defaultValue) {
    if (value == null || value.trim().isEmpty()) {
      log.debug("valueOf: using defaultValue '{}' for '{}' of '{}' ", defaultValue, value, tClass);
      return defaultValue;
    }
    try {
      return (T)
          MethodHandles.publicLookup()
              .findStatic(tClass, "valueOf", MethodType.methodType(tClass, String.class))
              .invoke(value);
    } catch (NumberFormatException e) {
      throw e;
    } catch (NoSuchMethodException | IllegalAccessException e) {
      log.debug("Can't invoke or access 'valueOf': ", e);
      throw new NumberFormatException(e.toString());
    } catch (Throwable e) {
      log.debug("Can't parse: ", e);
      throw new NumberFormatException(e.toString());
    }
  }

  private static Map<String, String> getPropertyMapValue(
      final Properties properties, final String name, final Map<String, String> defaultValue) {
    final String value = properties.getProperty(name);
    return value == null || value.trim().isEmpty() ? defaultValue : parseMap(value, name);
  }

  private static List<String> getPropertyListValue(
      final Properties properties, final String name, final List<String> defaultValue) {
    final String value = properties.getProperty(name);
    return value == null || value.trim().isEmpty() ? defaultValue : parseList(value);
  }

  private static URL getPropertyURLValue(
      final Properties properties, final String name, final URL defaultValue) {
    final String value = properties.getProperty(name);
    return value == null ? defaultValue : parseURL(value, name);
  }

  private static Boolean getPropertyBooleanValue(
      final Properties properties, final String name, final Boolean defaultValue) {
    return valueOf(properties.getProperty(name), Boolean.class, defaultValue);
  }

  private static Integer getPropertyIntegerValue(
      final Properties properties, final String name, final Integer defaultValue) {
    return valueOf(properties.getProperty(name), Integer.class, defaultValue);
  }

  private static Double getPropertyDoubleValue(
      final Properties properties, final String name, final Double defaultValue) {
    return valueOf(properties.getProperty(name), Double.class, defaultValue);
  }

  private static Set<PropagationStyle> getPropagationStyleSetFromPropertyValue(
      final Properties properties, final String name) {
    final String value = properties.getProperty(name);
    if (value != null) {
      final Set<PropagationStyle> result =
          convertStringSetToPropagationStyleSet(parseStringIntoSetOfNonEmptyStrings(value));
      if (!result.isEmpty()) {
        return result;
      }
    }
    // null means parent value should be used
    return null;
  }

  private static Set<Integer> getPropertyIntegerRangeValue(
      final Properties properties, final String name, final Set<Integer> defaultValue) {
    final String value = properties.getProperty(name);
    try {
      return value == null ? defaultValue : parseIntegerRangeSet(value, name);
    } catch (final NumberFormatException e) {
      log.warn("Invalid configuration for " + name, e);
      return defaultValue;
    }
  }

  @NonNull
  private static Map<String, String> parseMap(final String str, final String settingName) {
    // If we ever want to have default values besides an empty map, this will need to change.
    if (str == null || str.trim().isEmpty()) {
      return Collections.emptyMap();
    }
    if (!str.matches("(([^,:]+:[^,:]*,)*([^,:]+:[^,:]*),?)?")) {
      log.warn(
          "Invalid config for {}: '{}'. Must match 'key1:value1,key2:value2'.", settingName, str);
      return Collections.emptyMap();
    }

    final String[] tokens = str.split(",", -1);
    final Map<String, String> map = newHashMap(tokens.length);

    for (final String token : tokens) {
      final String[] keyValue = token.split(":", -1);
      if (keyValue.length == 2) {
        final String key = keyValue[0].trim();
        final String value = keyValue[1].trim();
        if (value.length() <= 0) {
          log.warn("Ignoring empty value for key '{}' in config for {}", key, settingName);
          continue;
        }
        map.put(key, value);
      }
    }
    return Collections.unmodifiableMap(map);
  }

  @NonNull
  private static Set<Integer> parseIntegerRangeSet(@NonNull String str, final String settingName)
      throws NumberFormatException {
    str = str.replaceAll("\\s", "");
    if (!str.matches("\\d{3}(?:-\\d{3})?(?:,\\d{3}(?:-\\d{3})?)*")) {
      log.warn(
          "Invalid config for {}: '{}'. Must be formatted like '400-403,405,410-499'.",
          settingName,
          str);
      throw new NumberFormatException();
    }

    final String[] tokens = str.split(",", -1);
    final Set<Integer> set = new HashSet<>();

    for (final String token : tokens) {
      final String[] range = token.split("-", -1);
      if (range.length == 1) {
        set.add(Integer.parseInt(range[0]));
      } else if (range.length == 2) {
        final int left = Integer.parseInt(range[0]);
        final int right = Integer.parseInt(range[1]);
        final int min = Math.min(left, right);
        final int max = Math.max(left, right);
        for (int i = min; i <= max; i++) {
          set.add(i);
        }
      }
    }
    return Collections.unmodifiableSet(set);
  }

  @NonNull
  private static Map<String, String> newHashMap(final int size) {
    return new HashMap<>(size + 1, 1f);
  }

  /**
   * @param map
   * @param propName
   * @return true if map was modified
   */
  private static boolean addPropToMapIfDefinedByEnvironment(
      final Map<String, String> map, final String propName) {
    final String val = getSettingFromEnvironment(propName, null);
    if (val != null) {
      return !val.equals(map.put(propertyNameToSystemPropertyName(propName), val));
    }
    return false;
  }

  @NonNull
  private static List<String> parseList(final String str) {
    if (str == null || str.trim().isEmpty()) {
      return Collections.emptyList();
    }

    final String[] tokens = str.split(",", -1);
    // Remove whitespace from each item.
    for (int i = 0; i < tokens.length; i++) {
      tokens[i] = tokens[i].trim();
    }
    return Collections.unmodifiableList(Arrays.asList(tokens));
  }

  private static URL parseURL(final String str, final String settingName) {
    try {
      return new URL(str);
    } catch (MalformedURLException e) {
      log.warn("Malformed URL {} in setting {}: {}", str, settingName, e.getMessage());
      return null;
    }
  }

  @NonNull
  private static Set<String> parseStringIntoSetOfNonEmptyStrings(final String str) {
    // Using LinkedHashSet to preserve original string order
    final Set<String> result = new LinkedHashSet<>();
    // Java returns single value when splitting an empty string. We do not need that value, so
    // we need to throw it out.
    for (final String value : str.split(SPLIT_BY_SPACE_OR_COMMA_REGEX)) {
      if (!value.isEmpty()) {
        result.add(value);
      }
    }
    return Collections.unmodifiableSet(result);
  }

  @NonNull
  private static Set<PropagationStyle> convertStringSetToPropagationStyleSet(
      final Set<String> input) {
    // Using LinkedHashSet to preserve original string order
    final Set<PropagationStyle> result = new LinkedHashSet<>();
    for (final String value : input) {
      try {
        result.add(PropagationStyle.valueOf(value.toUpperCase()));
      } catch (final IllegalArgumentException e) {
        log.debug("Cannot recognize config string value: {}, {}", value, PropagationStyle.class);
      }
    }
    return Collections.unmodifiableSet(result);
  }

  /**
   * Loads the optional configuration properties file into the global {@link Properties} object.
   *
   * @return The {@link Properties} object. the returned instance might be empty of file does not
   *     exist or if it is in a wrong format.
   */
  private static Properties loadConfigurationFile() {
    final Properties properties = new Properties();

    // Reading from system property first and from env after
    String configurationFilePath =
        System.getProperty(propertyNameToSystemPropertyName(SIGNALFX_PREFIX, CONFIGURATION_FILE));
    if (null == configurationFilePath) {
      configurationFilePath =
          System.getenv(propertyNameToEnvironmentVariableName(SIGNALFX_PREFIX, CONFIGURATION_FILE));
    }
    if (null == configurationFilePath) {
      configurationFilePath =
          System.getProperty(propertyNameToSystemPropertyName(PREFIX, CONFIGURATION_FILE));
    }
    if (null == configurationFilePath) {
      configurationFilePath =
          System.getenv(propertyNameToEnvironmentVariableName(PREFIX, CONFIGURATION_FILE));
    }
    if (null == configurationFilePath) {
      return properties;
    }

    // Normalizing tilde (~) paths for unix systems
    configurationFilePath =
        configurationFilePath.replaceFirst("^~", System.getProperty("user.home"));

    // Configuration properties file is optional
    final File configurationFile = new File(configurationFilePath);
    if (!configurationFile.exists()) {
      log.error("Configuration file '{}' not found.", configurationFilePath);
      return properties;
    }

    try (final FileReader fileReader = new FileReader(configurationFile)) {
      properties.load(fileReader);
    } catch (final FileNotFoundException fnf) {
      log.error("Configuration file '{}' not found.", configurationFilePath);
    } catch (final IOException ioe) {
      log.error(
          "Configuration file '{}' cannot be accessed or correctly parsed.", configurationFilePath);
    }

    return properties;
  }

  /** Returns the detected hostname. First tries locally, then using DNS */
  private static String getHostName() {
    String possibleHostname;

    // Try environment variable.  This works in almost all environments
    if (System.getProperty("os.name").startsWith("Windows")) {
      possibleHostname = System.getenv("COMPUTERNAME");
    } else {
      possibleHostname = System.getenv("HOSTNAME");
    }

    if (possibleHostname != null && !possibleHostname.isEmpty()) {
      log.debug("Determined hostname from environment variable");
      return possibleHostname.trim();
    }

    // Try hostname command
    try (final BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(Runtime.getRuntime().exec("hostname").getInputStream()))) {
      possibleHostname = reader.readLine();
    } catch (final Exception ignore) {
      // Ignore.  Hostname command is not always available
    }

    if (possibleHostname != null && !possibleHostname.isEmpty()) {
      log.debug("Determined hostname from hostname command");
      return possibleHostname.trim();
    }

    // From DNS
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (final UnknownHostException e) {
      // If we are not able to detect the hostname we do not throw an exception.
    }

    return null;
  }

  // This has to be placed after all other static fields to give them a chance to initialize
  private static final Config INSTANCE = new Config();

  public static Config get() {
    return INSTANCE;
  }

  public static Config get(final Properties properties) {
    if (properties == null || properties.isEmpty()) {
      return INSTANCE;
    } else {
      return new Config(properties, INSTANCE);
    }
  }
}
