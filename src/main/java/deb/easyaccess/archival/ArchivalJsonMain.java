package deb.easyaccess.archival;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import deb.easyaccess.archival.model.*;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashSet;

public class ArchivalJsonMain {

//    @JsonFilter("tradeBaseFilter")
//    public static class TradeBaseMixIn {}

    @JsonFilter("errorLogFilter")
    public static class ErrorLogMixIn {}

    public static void main(String[] args) {

        TradeBase tradeBase = new TradeBase();
        tradeBase.setRefId("PTP12345678");
        tradeBase.setSourceId("TPW12345678");
        tradeBase.setSource("TPW");
        tradeBase.setTradeDate("2025-04-01");
        tradeBase.setSettleDate("2023-04-30");
        tradeBase.setTradeType("EQ");
        tradeBase.setPrice("100.50");
        tradeBase.setQuantity("10");
        tradeBase.setLastAction("N");

        ErrorLog errorLog = new ErrorLog();
        errorLog.setErrorId(1234L);
        errorLog.setAddDate(new Timestamp(System.currentTimeMillis()));
        errorLog.setErrorCode("RNDM_ERROR");
        errorLog.setErrorDesc("Random Error");
        errorLog.setErrorStatus(3);
        errorLog.setTradeBase(tradeBase);
        tradeBase.setErrorLogSet(new HashSet<>(Arrays.asList(errorLog)));

        TradeBaseStage tradeBaseStage = new TradeBaseStage();
        tradeBaseStage.setRefId("PTP12345678");
        tradeBaseStage.setSourceId("TPW12345678");
        tradeBaseStage.setSource("TPW");
        tradeBaseStage.setTradeDate("2025-04-01");
        tradeBaseStage.setSettleDate("2023-04-30");
        tradeBaseStage.setTradeType("EQ");
        tradeBaseStage.setPrice("100.50");
        tradeBaseStage.setQuantity("10");
        tradeBaseStage.setLastAction("N");
        tradeBaseStage.setVersion("1");

        TradeLeg tradeLeg = new TradeLeg();
        tradeLeg.setRefId("PTP12345678");
        tradeLeg.setLegType("CT");
        tradeLeg.setAddDate(new java.sql.Timestamp(System.currentTimeMillis()));
        tradeLeg.setStatusCode(50);
        tradeLeg.setStatusText("Trade Confirmed");
        tradeLeg.setResponseGenerated("Y");
        tradeLeg.setTradeType("CT");
        tradeLeg.setSecType("EQ");

        TradeDetails tradeDetails = new TradeDetails();
        tradeDetails.setRefId("PTP12345678");
        tradeDetails.setTradeBase(tradeBase);
        tradeDetails.setErrorLog(errorLog);
        tradeDetails.setTradeLeg(tradeLeg);
        tradeDetails.setTradeBaseStage(tradeBaseStage);

        String json = null;
        ObjectMapper objectMapper = new ObjectMapper();
        //objectMapper.addMixIn(TradeBase.class, TradeBaseMixIn.class);
        objectMapper.addMixIn(ErrorLog.class, ErrorLogMixIn.class);
        SimpleFilterProvider filters = new SimpleFilterProvider()
                //.addFilter("tradeBaseFilter", SimpleBeanPropertyFilter.serializeAllExcept("errorLogSet"))
                .addFilter("errorLogFilter", SimpleBeanPropertyFilter.serializeAllExcept("tradeBase"));
        try {
            json = objectMapper.writer(filters).withDefaultPrettyPrinter().writeValueAsString(tradeDetails);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        try (FileWriter fileWriter = new FileWriter("output/tradeDetails.json")) {
            fileWriter.write(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("TradeDetails Json:");
        System.out.println(json);
    }
}
