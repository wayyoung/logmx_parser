package sample.parser;

import com.lightysoft.logmx.business.ParsedEntry;
import com.lightysoft.logmx.mgr.LogFileParser;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SumoCSVParserV2 extends LogFileParser {

    public static final String EXTRA_FW_VERSION_FIELD_KEY="fw_version";

    private static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
    private static SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static SimpleDateFormat odf = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    /** Current parsed log entry */
    private ParsedEntry entry = null;

    private Pattern pat = Pattern.compile("(\\d{2,4}-\\d{2}-?\\d{0,2}(?:\\s|T)\\d{2}:\\d{2}:\\d{2}(?:.|:)\\d{3}Z?)\\s+([^\\s]+)\\s+([^\\s]+)\\s+([^\\s]+)\\s+([^\\s]+)\\s*:.*");
    private static final List<String> EXTRA_FIELDS_KEYS = Arrays
            .asList(EXTRA_FW_VERSION_FIELD_KEY);
    protected int ver=2;
    @Override
    protected void parseLine(String line) throws Exception {


        if (line == null) return;
        if(line.startsWith("\"_messagetimems")){
//            if(!line.contains("reformat_date")) ver=2;//old
            return;
        }

        String pre="";
        pre = line.substring(0, line.substring(0, line.substring(0, line.substring(0, line.substring(0, line.substring(0, line.substring(0, line.substring(0, line.substring(0, line.lastIndexOf(',')).lastIndexOf(',')).lastIndexOf(',')).lastIndexOf(',')).lastIndexOf(',')).lastIndexOf(',')).lastIndexOf(',')).lastIndexOf(',')).lastIndexOf(','));

        String[] sufs=line.substring(pre.length()+1).split(",");
        for(int i=0;i<sufs.length;i++){
            sufs[i]=sufs[i].substring(1,sufs[i].length()-1);

        }
        String msg=pre.substring(pre.indexOf(',')+1);
        if(ver==2) {
            msg = msg.substring(msg.indexOf(',') + 1);
            msg = msg.substring(msg.indexOf(',') + 1);
        }
        msg=msg.substring(msg.indexOf((','))+2,msg.length()-1);


        Matcher matcher = pat.matcher(msg);

        entry=createNewEntry();
        entry.setUserDefinedFields(new HashMap<String, Object>(1)); // Create an empty Map with only one element allocated

        entry.setMessage(msg);
        entry.getUserDefinedFields().put(EXTRA_FW_VERSION_FIELD_KEY,sufs[2]);

        if(matcher.matches()){
            String dt=matcher.group(1);
            if(4==dt.indexOf("-")){
                if(dt.contains("T")){
                    entry.setDate(df.format(df2.parse(dt.trim())));
                }else {
                    entry.setDate(dt.trim());
                }

            }else{
                Date _d=odf.parse(dt.trim());
                Calendar now=Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                Calendar c=Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                c.setTime(_d);
                c.set(Calendar.YEAR,now.get(Calendar.YEAR));
                entry.setDate(df.format(c.getTime()));
            }
            entry.setEmitter(matcher.group(5));
        }
        processMessage(entry,msg);
        addEntry(entry);

    }

    @Override
    public List<String> getUserDefinedFields() {
        return EXTRA_FIELDS_KEYS;
    }

    @Override
    public Date getRelativeEntryDate(ParsedEntry parsedEntry) throws Exception {
        return df.parse(parsedEntry.getDate());
    }

    @Override
    public Date getAbsoluteEntryDate(ParsedEntry parsedEntry) throws Exception {
        return df.parse(parsedEntry.getDate());
    }


    @Override
    public String getParserName() {
        return "SumoCSVParserV2";
    }

    @Override
    public String getSupportedFileType() {
        return "LogMX sample log files";
    }

    protected void processMessage(ParsedEntry Entry, String message){
        if(message.contains("proxyDisconnnected")) {

            entry.setLevel("PROXY_DISC");
            return;
        }
        if(message.contains("proxyConnected")){
            entry.setLevel("PROXY_CONN");
            return;
        }
        entry.setLevel("NORMAL");
    }
}
