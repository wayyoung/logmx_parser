package sample.parser;

import com.lightysoft.logmx.business.ParsedEntry;
import com.lightysoft.logmx.mgr.LogFileParser;
import de.undercouch.actson.JsonEvent;
import de.undercouch.actson.JsonParser;

import java.io.PipedReader;
import java.io.PipedWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SumoJsonParser  extends LogFileParser {


    JsonParser parser = null;
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss:SSS");

    /** Current parsed log entry */
    private ParsedEntry entry = null;

    /** Mutex to avoid that multiple threads use the same Date formatter at the same time */
    private final Object DATE_FORMATTER_MUTEX = new Object();

    /** Pattern for entry begin */
    private final static Pattern ENTRY_BEGIN_PATTERN = Pattern
            .compile("^\\d{2}/\\d{2}/\\d{4}, \\d{2}:\\d{2}:\\d{2} \\(T0\\+(\\d+)ms\\).*$");

    /** Buffer for Entry message (improves performance for multi-lines entries)  */
    private StringBuilder entryMsgBuffer = null;

    /** Key of user-defined field "timestamp" */
    private static final String EXTRA_TIMESTAMP_FIELD_KEY = "Timestamp";

    /** User-defined fields names (here, only one) */
    private static final List<String> EXTRA_FIELDS_KEYS = Arrays
            .asList(EXTRA_TIMESTAMP_FIELD_KEY);


    public SumoJsonParser()throws Exception{
        parser = new JsonParser(StandardCharsets.UTF_8);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    /**
     * Returns the name of this parser
     * @see com.lightysoft.logmx.mgr.LogFileParser#getParserName()
     */
    @Override
    public String getParserName() {
        return "Sumo Parser";
    }

    /**
     * Returns the supported file type for this parser
     * @see com.lightysoft.logmx.mgr.LogFileParser#getSupportedFileType()
     */
    @Override
    public String getSupportedFileType() {
        return "LogMX sample log files";
    }

    private Pattern dmp = Pattern.compile("(\\d{2}-\\d{2})");
    Calendar cal=Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    static SimpleDateFormat df = new SimpleDateFormat("MM-dd");


    /**
     * Process the new line of text read from file
     * @see com.lightysoft.logmx.mgr.LogFileParser#parseLine(java.lang.String)
     */
    String fieldName=null;
    @Override
    protected void parseLine(String line) throws Exception {
        int event=JsonEvent.NEED_MORE_INPUT; // event returned by the parser
        int pos = 0; // position in the input JSON text
        byte[] buf=null;

        if(line!=null) {
            buf=line.getBytes(StandardCharsets.UTF_8);
        }else{
            parser.getFeeder().done();
        }

        while (event != JsonEvent.EOF) {
            // feed the parser until it returns a new event
            while((event=parser.nextEvent())==JsonEvent.NEED_MORE_INPUT) {
                if(buf!=null) {
                    if(pos==buf.length)return;
                    pos += parser.getFeeder().feed(buf, pos, buf.length - pos);
                }else{
                    //we have something wrong here, return;
                    return;
                }
            }
            if(event==JsonEvent.START_OBJECT){
                entry = createNewEntry();
                entry.setLevel("TRACE");
            }else if(event==JsonEvent.END_OBJECT) {
                Matcher m = dmp.matcher(entry.getMessage());
                if (m.find()) {

                    Date mtime = getAbsoluteEntryDate(entry);
                    String str = df.format(mtime);
                    if (!m.group(0).equals(str)) {
                        cal.setTime(mtime);
                        cal.set(Calendar.MONTH, Integer.parseInt(str.substring(0, 2)));
                        cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(str.substring(3, 5)));
                        mtime = cal.getTime();
                        entry.setDate(dateFormat.format(mtime));
                    }

                    addEntry(entry);
                }
            }else if(event==JsonEvent.FIELD_NAME) {
                fieldName=parser.getCurrentString();
            }else if(event==JsonEvent.VALUE_STRING){
                if(fieldName.equals("_raw")){
                    entry.setMessage(parser.getCurrentString());
                }
                if(fieldName.equals("_messagetime")){
                    synchronized (DATE_FORMATTER_MUTEX) {
                        entry.setDate(dateFormat.format(new Date(Long.parseLong(parser.getCurrentString()))));
                    }
                }
                if(fieldName.equals("_sourcecategory")){
                    entry.setEmitter(parser.getCurrentString());
                }
                fieldName=null;
            }else if(event==JsonEvent.VALUE_INT){

            }else if(event==JsonEvent.VALUE_FALSE){

            }else if(event==JsonEvent.VALUE_NULL){

            }
        }
//        parser.getFeeder().
    }

    /**
     * Returns the ordered list of user-defined fields to display (given by their key), for each entry.
     * @see com.lightysoft.logmx.mgr.LogFileParser#getUserDefinedFields()
     */
    @Override
    public List<String> getUserDefinedFields() {
        return EXTRA_FIELDS_KEYS;
    }

    /**
     * Returns a relative Date for the given entry (if entry's ExtraInfo contains "1265",
     * it means "T0 + 1265 ms", so simply return "new Date(1265)")
     * @see com.lightysoft.logmx.mgr.LogFileParser#getRelativeEntryDate(com.lightysoft.logmx.business.ParsedEntry)
     */
    @Override
    public Date getRelativeEntryDate(ParsedEntry pEntry) throws Exception {
//        final String strTimeStamp = pEntry.getUserDefinedFields().get(EXTRA_TIMESTAMP_FIELD_KEY)
//                .toString();
//        return new Date(Integer.parseInt(strTimeStamp));
        return Calendar.getInstance().getTime();
    }

    /**
     * Returns the absolute Date for the given entry
     * @see com.lightysoft.logmx.mgr.LogFileParser#getAbsoluteEntryDate(com.lightysoft.logmx.business.ParsedEntry)
     */
    @Override
    public Date getAbsoluteEntryDate(ParsedEntry pEntry) throws Exception {
        synchronized (DATE_FORMATTER_MUTEX) { // Java date formatter is not thread-safe
            return dateFormat.parse(pEntry.getDate()); // (the right-part "T0+..." will be ignored by the formatter)
        }
    }

    /**
     * Send to LogMX the current parsed log entry
     * @throws Exception
     */
    private void recordPreviousEntryIfExists() throws Exception {
        if (entry != null) {
            entry.setMessage(entryMsgBuffer.toString());
            addEntry(entry);
        }
    }

    /**
     * Send to LogMX the current parsed log entry, then create a new one
     * @throws Exception
     */
    private void prepareNewEntry() throws Exception {
        recordPreviousEntryIfExists();
        entry = createNewEntry();
        entryMsgBuffer = new StringBuilder(80);
        entry.setUserDefinedFields(new HashMap<String, Object>(1)); // Create an empty Map with only one element allocated
    }
}
