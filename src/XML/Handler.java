package XML;

import it.converter.GroupMeasures;
import it.converter.Measures;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;


public class Handler  extends DefaultHandler{
    private GroupMeasures gmeasures;
    private Measures currentMeasure;
    private StringBuilder builder;
    private Context context;
    
    
    public Handler(Context context) {
		super();
		this.context = context;
	}

	public GroupMeasures getmeasures(){
    	this.gmeasures.buildList(context);
    	this.gmeasures.loadMisura(context);
    	//Log.i("test",this.gmeasures.getId()+"");
        return this.gmeasures;
    }
    
    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        super.characters(ch, start, length);
        builder.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String name)
            throws SAXException {
        super.endElement(uri, localName, name);
        if (this.currentMeasure != null){ //Log.i("test","builder:"+builder.toString().trim() );
        	 if (localName.equalsIgnoreCase(GroupMeasures.NOME_IT)){
                currentMeasure.setId(builder.toString().trim()); //Log.i("test","nome:"+builder.toString().trim() );
            } else if (localName.equalsIgnoreCase(GroupMeasures.NOME_ENG)){
                currentMeasure.setId_eng(builder.toString().trim());//Log.i("test","nome_eng:"+builder.toString().trim() );
            } else if (localName.equalsIgnoreCase(GroupMeasures.BASE)){
                currentMeasure.setBase(builder.toString().trim());//Log.i("test","base:"+builder.toString().trim() );
            } else if (localName.equalsIgnoreCase(GroupMeasures.SIMBOLO)){
                currentMeasure.setSymbol(builder.toString().trim());//Log.i("test","symb:"+builder.toString().trim() );
            } else if (localName.equalsIgnoreCase(GroupMeasures.FROM)){
                currentMeasure.setFrom(builder.toString().trim());//Log.i("test","from:"+builder.toString().trim() );
            } else if (localName.equalsIgnoreCase(GroupMeasures.TO)){
                currentMeasure.setTo(builder.toString().trim());//Log.i("test","to:"+builder.toString().trim() );
            } else if (localName.equalsIgnoreCase(GroupMeasures.PERSONALE)){
                currentMeasure.setPersonale(builder.toString().trim());//Log.i("test","pers:"+builder.toString().trim() );
            } else if (localName.equalsIgnoreCase(GroupMeasures.VISIBILE)){
                currentMeasure.setVisibile(builder.toString().trim());//Log.i("test","visibile:"+builder.toString().trim() );
            } else if (localName.equalsIgnoreCase(GroupMeasures.URIFERIMENTO)){
                currentMeasure.setUnita_riferimento(builder.toString().trim());
            } else if (localName.equalsIgnoreCase(GroupMeasures.UNITA)){
            	gmeasures.addMeasure(currentMeasure, context);
            }
            builder.setLength(0);    
        }
    }

    @Override
    public void startDocument() throws SAXException {
        super.startDocument();
        gmeasures = new GroupMeasures();
        builder = new StringBuilder();
    }

    @Override
    public void startElement(String uri, String localName, String name,
            Attributes attributes) throws SAXException {
        super.startElement(uri, localName, name, attributes);
        if (localName.equalsIgnoreCase(GroupMeasures.UNITA)){
            this.currentMeasure = new Measures();
        } else if (localName.equalsIgnoreCase(GroupMeasures.GRUPPO)){
        	this.gmeasures.setId(attributes.getValue(GroupMeasures.ID));
        	this.gmeasures.setVersione(attributes.getValue(GroupMeasures.VERSIONE));
        }
        
    }
}