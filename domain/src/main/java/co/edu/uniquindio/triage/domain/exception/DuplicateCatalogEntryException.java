package co.edu.uniquindio.triage.domain.exception;

public class DuplicateCatalogEntryException extends DomainException {

    private final String catalog;
    private final String value;

    public DuplicateCatalogEntryException(String catalog, String value) {
        super("Ya existe un " + catalog + " con nombre '" + value + "'");
        this.catalog = catalog;
        this.value = value;
    }

    public String getCatalog() {
        return catalog;
    }

    public String getValue() {
        return value;
    }
}
