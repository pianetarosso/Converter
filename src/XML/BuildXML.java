package XML;

import it.converter.AddMeasure;
import it.converter.GroupMeasures;
import it.converter.Main;
import it.converter.Measures;
import it.converter.classes.R;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.os.Environment;
import android.util.Xml;

public class BuildXML {
	
	private static final String FILE_EXTENSION = ".xml";
	private StringWriter writer;
	private Context context;
	private GroupMeasures gm;
	
	
	public BuildXML (GroupMeasures gm, Context context) {
		
		this.context = context;
		this.gm = gm;
		createXml();
	}
		
	
	private void createXml() {
		
		XmlSerializer serializer = Xml.newSerializer();
	    writer = new StringWriter();
	    
	    try {
	    	
	    	serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
	    	
	        serializer.setOutput(writer);
	        
	        serializer.startDocument("UTF-8", true); // intestazione
	         
	        serializer.startTag("", GroupMeasures.GRUPPO);	// gruppo che racchiude le misure
	        serializer.attribute("", GroupMeasures.ID, gm.getId()+"");
	        serializer.attribute("", GroupMeasures.VERSIONE, gm.getVersione()+"");
	        
	        for (Measures m: ordina(gm.getMeasures(), context)){
	        	
	            serializer.startTag("", GroupMeasures.UNITA);
	            
	            	addTag(serializer, GroupMeasures.NOME_IT, m.getId());
	            	addTag(serializer, GroupMeasures.NOME_ENG, m.getId_eng());
	            	addTag(serializer, GroupMeasures.SIMBOLO, m.getSymbol());
	            	addTag(serializer, GroupMeasures.BASE, m.isBase());
	            	addTag(serializer, GroupMeasures.URIFERIMENTO, m.getUnita_riferimento());
	            	addTag(serializer, GroupMeasures.TO, m.getTo());
	            	addTag(serializer, GroupMeasures.FROM, m.getFrom());
	            	addTag(serializer, GroupMeasures.PERSONALE, m.isPersonale());
	            	addTag(serializer, GroupMeasures.VISIBILE, m.isVisibile());
		            
		        serializer.endTag("", GroupMeasures.UNITA);
	        }
	        
	        serializer.endTag("", GroupMeasures.GRUPPO);
	        serializer.flush();
	        serializer.endDocument();

	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    } 
	}
	
	private void addTag(XmlSerializer s, String tag, String value) throws Exception {
		s.startTag("", tag);
        s.text(value);
        s.endTag("", tag);
	}
	
/*	private void addTag(XmlSerializer s, String tag, int value) throws Exception {
		addTag(s, tag, value+"");
	}
*/	
	private void addTag(XmlSerializer s, String tag, boolean value) throws Exception {
		String v = "false";
		if (value) v = "true";
		addTag(s, tag, v);
	}
	
	public boolean writeXml() {		

		File file = buildPath(context.getString(R.string.app_name), gm.getId() + FILE_EXTENSION);
		
		try {
			file.createNewFile();
			PrintWriter pw = new PrintWriter(file);
			pw.write(writer.toString());
			pw.close();
			return true;
		} catch (IOException ioe) {
			ioe.printStackTrace(); 
			return false;
			}		
	}
	
	public static ArrayList<Measures> ordina(ArrayList<Measures> misure, Context context) {
		
		int type = context.getSharedPreferences(Main.ORDINAMENTO, Context.MODE_PRIVATE).getInt(Main.MODI_ORDINAMENTO, 0);
		
		switch (type) {
		
		case 0: // ordinamento alfabetico crescente
			Collections.sort(misure, new Comparator<Measures>() {
		        @Override
		        public int compare(Measures s1, Measures s2) {
		            return s1.getId().compareToIgnoreCase(s2.getId());
		        }
		    });
			return misure;
			
		case 1: // ordinamento alfabetico decrescente
			Collections.sort(misure, new Comparator<Measures>() {
		        @Override
		        public int compare(Measures s1, Measures s2) {
		            return s2.getId().compareToIgnoreCase(s1.getId());
		        }
		    });
			return misure;
		
		case 2: // ordinamento numerico crescente
			Collections.sort(misure, new Comparator<Measures>() {
		        @Override
		        public int compare(Measures s1, Measures s2) {
		        	int testValue = AddMeasure.TEST_VALUE;
		        	double r1 = s1.getFrom(testValue); 
		        	double r2 = s2.getFrom(testValue); 
		        	
		        	return (int)Math.signum(r1-r2);
		        }
		    });
			return misure;
			
		case 3: // ordinamento numerico decrescente
			Collections.sort(misure, new Comparator<Measures>() {
		        @Override
		        public int compare(Measures s1, Measures s2) {
		        	int testValue = AddMeasure.TEST_VALUE;
		        	double r1 = s1.getFrom(testValue); 
		        	double r2 = s2.getFrom(testValue); 
		        	
		        	return (int)Math.signum(r2-r1);
		        }
		    });
		}
		return misure;
	}
		
	 
	public static File buildPath(String home, String output) {
		File externalStorage = Environment.getExternalStorageDirectory();
		String path = externalStorage.getAbsolutePath();	
		
		if (!path.endsWith("/")) path += "/";
		
		path += home;
		
		File t = new File(path);
		t.mkdirs();
	
		path += "/" + output;
		
		return new File(path);
	}
}
