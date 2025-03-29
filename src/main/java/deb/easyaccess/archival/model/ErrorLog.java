package deb.easyaccess.archival.model;

import java.sql.Timestamp;

public class ErrorLog {

    private long errorId;

    private TradeBase tradeBase;

    private String errorCode;

    private String errorDesc;

    private Timestamp addDate;

    private int errorStatus;

    public ErrorLog() {
    }

    public long getErrorId() {
        return errorId;
    }

    public void setErrorId(long errorId) {
        this.errorId = errorId;
    }

    public TradeBase getTradeBase() {
        return tradeBase;
    }

    public void setTradeBase(TradeBase tradeBase) {
        this.tradeBase = tradeBase;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDesc() {
        return errorDesc;
    }

    public void setErrorDesc(String errorDesc) {
        this.errorDesc = errorDesc;
    }

    public Timestamp getAddDate() {
        return addDate;
    }

    public void setAddDate(Timestamp addDate) {
        this.addDate = addDate;
    }

    public int getErrorStatus() {
        return errorStatus;
    }

    public void setErrorStatus(int errorStatus) {
        this.errorStatus = errorStatus;
    }
}
