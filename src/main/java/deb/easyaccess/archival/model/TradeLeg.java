package deb.easyaccess.archival.model;

import java.sql.Timestamp;

public class TradeLeg {

    private String refId;

    private String legType;

    private Timestamp addDate;

    private int statusCode;

    private String statusText;

    private String responseGenerated;

    private String tradeType;

    private String secType;

    public TradeLeg() {
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    public String getLegType() {
        return legType;
    }

    public void setLegType(String legType) {
        this.legType = legType;
    }

    public Timestamp getAddDate() {
        return addDate;
    }

    public void setAddDate(Timestamp addDate) {
        this.addDate = addDate;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public String getResponseGenerated() {
        return responseGenerated;
    }

    public void setResponseGenerated(String responseGenerated) {
        this.responseGenerated = responseGenerated;
    }

    public String getTradeType() {
        return tradeType;
    }

    public void setTradeType(String tradeType) {
        this.tradeType = tradeType;
    }

    public String getSecType() {
        return secType;
    }

    public void setSecType(String secType) {
        this.secType = secType;
    }
}
