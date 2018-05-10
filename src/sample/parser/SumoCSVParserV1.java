package sample.parser;

public class SumoCSVParserV1 extends SumoCSVParserV2  {
    public SumoCSVParserV1(){
        ver=1;
    }

    @Override
    public String getParserName() {
        return "SumoCSVParserV1";
    }
}
