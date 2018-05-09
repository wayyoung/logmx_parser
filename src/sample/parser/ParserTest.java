package sample.parser;

import de.undercouch.actson.JsonEvent;
import de.undercouch.actson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;

public class ParserTest {
    public static void main(String[] args)throws Exception{
        // JSON text to parse
//        byte[] json = "{\"name\":\"Elvis\"}".getBytes(StandardCharsets.UTF_8);

        BufferedReader bfr=new BufferedReader(new FileReader(new File("e:/2018-04-01.json")));
        JsonParser parser = new JsonParser(StandardCharsets.UTF_8);


        int event; // event returned by the parser
        boolean fEOF=false;
        int pos = 0; // position in the input JSON text
        String line=null;
        do {
            // feed the parser until it returns a new event
            byte[] buf=null;
            while ((event = parser.nextEvent()) == JsonEvent.NEED_MORE_INPUT) {
                line=null;
                if(pos==0){
                    if((line=bfr.readLine())==null) {
                        if(!fEOF) {
                            fEOF = true;
                            parser.getFeeder().done();
                            continue;
                        }
                    }else {
                        buf = line.getBytes(StandardCharsets.UTF_8);
                    }
                }

                // provide the parser with more input
                pos += parser.getFeeder().feed(buf, pos, buf.length - pos);

                // indicate end of input to the parser
                //System.out.println("left="+(buf.length - pos));
                if (pos == buf.length) {
                    //parser.getFeeder().done();
                    pos=0;
                    buf=null;
                }
            }

            // handle event
            System.out.println("JSON event: " + event);


            if (event == JsonEvent.ERROR) {
                System.out.println("ERROR LINE:"+line);
                throw new IllegalStateException("Syntax error in JSON text");


            }
        } while (event != JsonEvent.EOF);
    }
}
