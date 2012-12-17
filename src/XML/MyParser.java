package XML;

import it.converter.GroupMeasures;

import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import android.content.Context;

public class MyParser {
	
	Handler handler;

	    public MyParser(InputStream input, Context context) {
	    	
	        SAXParserFactory factory = SAXParserFactory.newInstance();
	        try {
	            SAXParser parser = factory.newSAXParser();
	            handler = new Handler(context);
	            parser.parse(input, handler);
	            
	        } catch (Exception e) {
	            throw new RuntimeException(e);
	        } 
	    }
	    
	    public GroupMeasures getParsedData() {
	    	return handler.getmeasures();
	    }  
}


