package deb.easyaccess.archival.model;

public class TradeDetails {

    private String refId;

    private TradeBase tradeBase;

    private ErrorLog errorLog;

    private TradeLeg tradeLeg;

    private TradeBaseStage tradeBaseStage;

    public TradeDetails() {
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    public TradeBase getTradeBase() {
        return tradeBase;
    }

    public void setTradeBase(TradeBase tradeBase) {
        this.tradeBase = tradeBase;
    }

    public ErrorLog getErrorLog() {
        return errorLog;
    }

    public void setErrorLog(ErrorLog errorLog) {
        this.errorLog = errorLog;
    }

    public TradeLeg getTradeLeg() {
        return tradeLeg;
    }

    public void setTradeLeg(TradeLeg tradeLeg) {
        this.tradeLeg = tradeLeg;
    }

    public TradeBaseStage getTradeBaseStage() {
        return tradeBaseStage;
    }

    public void setTradeBaseStage(TradeBaseStage tradeBaseStage) {
        this.tradeBaseStage = tradeBaseStage;
    }
}
