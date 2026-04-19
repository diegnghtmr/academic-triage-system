package co.edu.uniquindio.triage.infrastructure.idempotency;

public final class OperationScope {

    public static final String AUTH_REGISTER                  = "auth:register";
    public static final String REQUESTS_CREATE                = "requests:create";
    public static final String REQUESTS_CLASSIFY              = "requests:classify";
    public static final String REQUESTS_PRIORITIZE            = "requests:prioritize";
    public static final String REQUESTS_ASSIGN                = "requests:assign";
    public static final String REQUESTS_ATTEND                = "requests:attend";
    public static final String REQUESTS_CLOSE                 = "requests:close";
    public static final String REQUESTS_CANCEL                = "requests:cancel";
    public static final String REQUESTS_REJECT                = "requests:reject";
    public static final String REQUESTS_ADD_INTERNAL_NOTE     = "requests:add-internal-note";
    public static final String CATALOGS_REQUEST_TYPES_CREATE  = "catalogs:request-types:create";
    public static final String CATALOGS_ORIGIN_CHANNELS_CREATE = "catalogs:origin-channels:create";
    public static final String BUSINESS_RULES_CREATE          = "business-rules:create";
    public static final String AI_SUGGEST_CLASSIFICATION      = "ai:suggest-classification";

    private OperationScope() {}
}
