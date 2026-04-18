package com.example.entities;

import java.io.Serializable;
import java.util.UUID;

public final class AppConfiguration implements Serializable {
    private static final long serialVersionUID = 2L;

    // ── Library identity
    private String libraryId      = UUID.randomUUID().toString();
    private String libraryName    = "My Library";
    private String branchId       = UUID.randomUUID().toString();
    private String branchName     = "Main Branch";

    // ── Data storage
    private String dataDirectory   = "data";
    private String exportDirectory = "exports";

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

    // ── First-run flag
    private boolean initialSetupDone    = false;

    // ════════════════════════════════════════════════════════════════
    // Library identity
    // ════════════════════════════════════════════════════════════════
    public String getLibraryId()               { return libraryId; }
    public String getLibraryName()             { return libraryName; }
    public void   setLibraryName(String v)     { libraryName  = blankOr(v, "My Library"); }

    public String getBranchId()                { return branchId; }
    public String getBranchName()              { return branchName; }
    public void   setBranchName(String v)      { branchName   = blankOr(v, "Main Branch"); }

    // ════════════════════════════════════════════════════════════════
    // Data storage
    // ════════════════════════════════════════════════════════════════
    public String getDataDirectory()           { return dataDirectory; }
    public void   setDataDirectory(String v)   { dataDirectory  = blankOr(v, "data"); }

    public String getExportDirectory()         { return exportDirectory; }
    public void   setExportDirectory(String v) { exportDirectory = blankOr(v, "exports"); }

    // ════════════════════════════════════════════════════════════════
    // Fine / currency
    // ════════════════════════════════════════════════════════════════
    public double getFinePerDay()              { return finePerDay; }
    public void   setFinePerDay(double v)      { finePerDay = Math.max(0.0, v); }

    public String getCurrencySymbol()          { return currencySymbol != null ? currencySymbol : "$"; }
    public void   setCurrencySymbol(String v)  { currencySymbol  = blankOr(v, "$"); }

    public String getCurrencyCode()            { return currencyCode != null ? currencyCode : "USD"; }
    public void   setCurrencyCode(String v)    { currencyCode    = blankOr(v, "USD"); }

    public String formatAmount(double amount)  { return getCurrencySymbol() + String.format("%.2f", amount); }

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
                && (!smtpAuth || smtpUsername != null);
    }

    // ════════════════════════════════════════════════════════════════
    // UI
    // ════════════════════════════════════════════════════════════════
    public boolean isDarkMode()                { return darkMode; }
    public void    setDarkMode(boolean v)      { darkMode = v; }
    public void    toggleDarkMode()            { darkMode = !darkMode; }

    // ════════════════════════════════════════════════════════════════
    // First-run setup
    // ════════════════════════════════════════════════════════════════
    public boolean isInitialSetupDone()        { return initialSetupDone; }
    public void    markSetupDone()             { initialSetupDone = true; }

    // ════════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════════
    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
    private static String blankOr(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v.trim();
    }
}