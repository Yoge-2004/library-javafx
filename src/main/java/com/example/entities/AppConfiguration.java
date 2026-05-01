package com.example.entities;

import com.example.storage.AppPaths;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Currency;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class AppConfiguration implements Serializable {
    private static final long serialVersionUID = 3L;
    private static final List<Integer> COMMON_SMTP_PORTS = List.of(25, 465, 587, 2525);

    // ── Library identity
    private String libraryId      = UUID.randomUUID().toString();
    private String libraryName    = "My Library";
    private String branchId       = UUID.randomUUID().toString();
    private String branchName     = "Main Branch";

    // ── Data storage
    private String dataDirectory   = AppPaths.defaultDataDirectory().toString();
    private String exportDirectory = AppPaths.defaultExportDirectory().toString();

    // ── Fine / currency
    private double finePerDay      = 2.00;
    private String currencySymbol  = "$";
    private String currencyCode    = "USD";

    // ── SMTP
    private String  smtpHost;
    private int     smtpPort            = 587;
    private String  smtpUsername;
    private String  smtpPassword;
    private String  fromAddress;
    private boolean smtpAuth            = true;
    private boolean startTlsEnabled     = true;

    // ── UI preferences
    private boolean darkMode            = false;

    // ── Optional database persistence
    private DatabaseConfiguration databaseConfiguration = new DatabaseConfiguration();

    // ── First-run flag
    private boolean initialSetupDone    = false;

    // ── Library chooser support
    private List<String> knownLibraries = new ArrayList<>();
    private List<LibraryIdentity> knownLibraryProfiles = new ArrayList<>();
    private List<String> savedCategories = new ArrayList<>();

    // ════════════════════════════════════════════════════════════════
    // Library identity
    // ════════════════════════════════════════════════════════════════
    public String getLibraryId()               { return libraryId; }
    public String getLibraryName()             { return libraryName; }
    public void   setLibraryName(String v)     { libraryName  = blankOr(v, "My Library"); }

    public String getBranchId()                { return branchId; }
    public String getBranchName()              { return branchName; }
    public void   setBranchName(String v)      { branchName   = blankOr(v, "Main Branch"); }

    public String getCurrentLibraryDisplayName() {
        return getLibraryName() + " - " + getBranchName();
    }

    // ════════════════════════════════════════════════════════════════
    // Data storage
    // ════════════════════════════════════════════════════════════════
    public String getDataDirectory()           { return dataDirectory; }
    public void   setDataDirectory(String v)   {
        dataDirectory = AppPaths.resolveConfiguredDirectory(blankToNull(v), AppPaths.defaultDataDirectory())
                .toString();
    }

    public String getExportDirectory()         { return exportDirectory; }
    public void   setExportDirectory(String v) {
        exportDirectory = AppPaths.resolveConfiguredDirectory(blankToNull(v), AppPaths.defaultExportDirectory())
                .toString();
    }

    // ════════════════════════════════════════════════════════════════
    // Fine / currency
    // ════════════════════════════════════════════════════════════════
    public double getFinePerDay()              { return finePerDay; }
    public void   setFinePerDay(double v)      { finePerDay = Math.max(0.0, v); }

    public String getCurrencySymbol() {
        if (currencySymbol != null && !currencySymbol.isBlank()) {
            return currencySymbol.trim();
        }
        return inferCurrencySymbol(getCurrencyCode());
    }
    public void   setCurrencySymbol(String v)  { currencySymbol  = blankToNull(v); }

    public String getCurrencyCode()            { return currencyCode != null ? currencyCode : "USD"; }
    public void   setCurrencyCode(String v)    { currencyCode    = blankOr(v, "USD"); }

    public String formatAmount(double amount)  { return getCurrencySymbol() + String.format("%,.2f", amount); }

    // ════════════════════════════════════════════════════════════════
    // SMTP
    // ════════════════════════════════════════════════════════════════
    public String  getSmtpHost()               { return smtpHost; }
    public void    setSmtpHost(String v)       { smtpHost       = blankToNull(v); }

    public int     getSmtpPort()               { return smtpPort; }
    public void    setSmtpPort(int v)          { smtpPort       = Math.max(1, v); }

    public String  getSmtpUsername()           { return smtpUsername; }
    public void    setSmtpUsername(String v)   { smtpUsername   = blankToNull(v); }

    public String  getSmtpPassword()           { return smtpPassword; }
    public void    setSmtpPassword(String v)   { smtpPassword   = blankToNull(v); }

    public String  getFromAddress()            { return fromAddress; }
    public void    setFromAddress(String v)    { fromAddress    = blankToNull(v); }

    public boolean isSmtpAuth()                { return smtpAuth; }
    public void    setSmtpAuth(boolean v)      { smtpAuth       = v; }

    public boolean isStartTlsEnabled()         { return startTlsEnabled; }
    public void    setStartTlsEnabled(boolean v){ startTlsEnabled = v; }

    public boolean isEmailConfigured() {
        return smtpHost != null && fromAddress != null
                && (!smtpAuth || (smtpUsername != null && smtpPassword != null));
    }

    public List<Integer> getCommonSmtpPorts() {
        if (COMMON_SMTP_PORTS.contains(smtpPort)) {
            return COMMON_SMTP_PORTS;
        }
        List<Integer> ports = new ArrayList<>(COMMON_SMTP_PORTS);
        ports.add(smtpPort);
        ports.sort(Integer::compareTo);
        return List.copyOf(ports);
    }

    // ════════════════════════════════════════════════════════════════
    // UI
    // ════════════════════════════════════════════════════════════════
    public boolean isDarkMode()                { return darkMode; }
    public void    setDarkMode(boolean v)      { darkMode = v; }
    public void    toggleDarkMode()            { darkMode = !darkMode; }

    public DatabaseConfiguration getDatabaseConfiguration() {
        if (databaseConfiguration == null) {
            databaseConfiguration = new DatabaseConfiguration();
        }
        return databaseConfiguration;
    }

    public void setDatabaseConfiguration(DatabaseConfiguration databaseConfiguration) {
        this.databaseConfiguration = databaseConfiguration != null
                ? databaseConfiguration
                : new DatabaseConfiguration();
    }

    // ════════════════════════════════════════════════════════════════
    // First-run setup
    // ════════════════════════════════════════════════════════════════
    public boolean isInitialSetupDone()        { return initialSetupDone; }
    public void    markSetupDone()             { initialSetupDone = true; }

    public List<String> getKnownLibraries() {
        ensureKnownLibraries();
        return List.copyOf(knownLibraries);
    }

    public void setKnownLibraries(List<String> libraries) {
        knownLibraries = new ArrayList<>();
        knownLibraryProfiles = new ArrayList<>();
        if (libraries != null) {
            for (String library : libraries) {
                if (library != null && !library.isBlank()) {
                    String trimmed = library.trim();
                    knownLibraries.add(trimmed);
                    addKnownLibraryProfile(parseDisplayName(trimmed));
                }
            }
        }
        ensureKnownLibraries();
    }

    public void rememberCurrentLibrary() {
        ensureKnownLibraries();
        LibraryIdentity current = new LibraryIdentity(getLibraryName(), getBranchName());
        knownLibraryProfiles.removeIf(current::matches);
        knownLibraryProfiles.add(0, current);
        syncKnownLibraryDisplays();
    }

    public boolean selectKnownLibrary(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return false;
        }

        ensureKnownLibraries();
        String trimmed = displayName.trim();
        for (LibraryIdentity profile : knownLibraryProfiles) {
            if (profile.matches(trimmed)) {
                setLibraryName(profile.libraryName());
                setBranchName(profile.branchName());
                rememberCurrentLibrary();
                return true;
            }
        }

        LibraryIdentity parsed = parseDisplayName(trimmed);
        if (parsed != null) {
            setLibraryName(parsed.libraryName());
            setBranchName(parsed.branchName());
            rememberCurrentLibrary();
            return true;
        }
        return false;
    }

    public List<String> getSavedCategories() {
        ensureSavedCategories();
        return List.copyOf(savedCategories);
    }

    public void setSavedCategories(List<String> categories) {
        savedCategories = new ArrayList<>();
        if (categories != null) {
            for (String category : categories) {
                addSavedCategory(category);
            }
        }
        ensureSavedCategories();
    }

    public void rememberCategory(String category) {
        addSavedCategory(category);
        ensureSavedCategories();
    }

    public void normalize() {
        if (libraryId == null || libraryId.isBlank()) {
            libraryId = UUID.randomUUID().toString();
        }
        if (branchId == null || branchId.isBlank()) {
            branchId = UUID.randomUUID().toString();
        }
        setLibraryName(libraryName);
        setBranchName(branchName);
        setDataDirectory(dataDirectory);
        setExportDirectory(exportDirectory);
        setCurrencySymbol(currencySymbol);
        setCurrencyCode(currencyCode);
        setFinePerDay(finePerDay);
        setDatabaseConfiguration(databaseConfiguration);
        ensureKnownLibraries();
        ensureSavedCategories();
    }

    // ════════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════════
    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
    private static String blankOr(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v.trim();
    }

    private static String inferCurrencySymbol(String code) {
        try {
            return Currency.getInstance(blankOr(code, "USD")).getSymbol();
        } catch (Exception ignored) {
            return "$";
        }
    }

    private static final List<String> SEED_LIBRARIES = List.of(
            "City Central Library - Main Branch",
            "City Central Library - East Wing",
            "Green Valley Public Library - Main Branch",
            "Green Valley Public Library - North Campus",
            "Sunrise University Library - Academic Block",
            "Sunrise University Library - Research Wing",
            "Westside Community Library - Main Branch",
            "Westside Community Library - Children's Section",
            "Lakewood District Library - Downtown",
            "Lakewood District Library - Suburban Branch",
            "Hilltop School Library - Senior Block",
            "Hilltop School Library - Junior Block",
            "Heritage Archive - History Wing",
            "Tech Park Library - Innovation Hub",
            "Seaside Public Library - Marina Branch"
    );

    private void ensureKnownLibraries() {
        if (knownLibraries == null) {
            knownLibraries = new ArrayList<>();
        }
        if (knownLibraryProfiles == null) {
            knownLibraryProfiles = new ArrayList<>();
        }

        // Seed demo libraries on first run (when list would be empty)
        if (knownLibraries.isEmpty() && knownLibraryProfiles.isEmpty()) {
            knownLibraries.addAll(SEED_LIBRARIES);
        }

        List<LibraryIdentity> mergedProfiles = new ArrayList<>();
        for (LibraryIdentity profile : knownLibraryProfiles) {
            addKnownLibraryProfile(mergedProfiles, profile);
        }
        for (String value : knownLibraries) {
            addKnownLibraryProfile(mergedProfiles, parseDisplayName(value));
        }
        addKnownLibraryProfile(mergedProfiles, new LibraryIdentity(getLibraryName(), getBranchName()));

        knownLibraryProfiles = mergedProfiles;
        syncKnownLibraryDisplays();
    }

    private void ensureSavedCategories() {
        if (savedCategories == null) {
            savedCategories = new ArrayList<>();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String category : savedCategories) {
            if (category != null && !category.isBlank()) {
                unique.add(category.trim());
            }
        }
        savedCategories = new ArrayList<>(unique);
    }

    private void addSavedCategory(String category) {
        if (category == null || category.isBlank()) {
            return;
        }
        if (savedCategories == null) {
            savedCategories = new ArrayList<>();
        }
        String trimmed = category.trim();
        savedCategories.removeIf(existing -> existing.equalsIgnoreCase(trimmed));
        savedCategories.add(trimmed);
    }

    private void syncKnownLibraryDisplays() {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (LibraryIdentity profile : knownLibraryProfiles) {
            if (profile != null) {
                unique.add(profile.displayName());
            }
        }
        unique.add(getCurrentLibraryDisplayName());
        knownLibraries = new ArrayList<>(unique);
    }

    private void addKnownLibraryProfile(LibraryIdentity identity) {
        if (knownLibraryProfiles == null) {
            knownLibraryProfiles = new ArrayList<>();
        }
        addKnownLibraryProfile(knownLibraryProfiles, identity);
    }

    private static void addKnownLibraryProfile(List<LibraryIdentity> profiles, LibraryIdentity identity) {
        if (profiles == null || identity == null) {
            return;
        }
        profiles.removeIf(identity::matches);
        profiles.add(identity);
    }

    private static LibraryIdentity parseDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return null;
        }
        String trimmed = displayName.trim();
        int separator = trimmed.lastIndexOf(" - ");
        if (separator <= 0 || separator >= trimmed.length() - 3) {
            return new LibraryIdentity(trimmed, "Main Branch");
        }
        return new LibraryIdentity(trimmed.substring(0, separator), trimmed.substring(separator + 3));
    }

    private static final class LibraryIdentity implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String libraryName;
        private final String branchName;

        private LibraryIdentity(String libraryName, String branchName) {
            this.libraryName = blankOr(libraryName, "My Library");
            this.branchName = blankOr(branchName, "Main Branch");
        }

        private String libraryName() {
            return libraryName;
        }

        private String branchName() {
            return branchName;
        }

        private String displayName() {
            return libraryName + " - " + branchName;
        }

        private boolean matches(String displayName) {
            return displayName != null && displayName().equalsIgnoreCase(displayName.trim());
        }

        private boolean matches(LibraryIdentity other) {
            return other != null
                    && libraryName.equalsIgnoreCase(other.libraryName)
                    && branchName.equalsIgnoreCase(other.branchName);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof LibraryIdentity that)) {
                return false;
            }
            return libraryName.equalsIgnoreCase(that.libraryName)
                    && branchName.equalsIgnoreCase(that.branchName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(libraryName.toLowerCase(), branchName.toLowerCase());
        }
    }
}