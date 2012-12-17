package it.converter;

import it.converter.classes.R;

import java.io.InputStream;
import java.util.ArrayList;

import XML.BuildXML;
import XML.MyParser;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

public class GroupMeasures implements Parcelable {

	public static final String GRUPPO = "gruppo";
	public static final String ID = "id";
	public static final String VERSIONE = "versione";
	public static final String UNITA = "unita";
	public static final String NOME_IT = "nome_it";
	public static final String NOME_ENG = "nome_eng";
	public static final String SIMBOLO = "simbolo";
	public static final String BASE = "base";
	public static final String TO = "to";
	public static final String FROM = "from";
	public static final String PERSONALE = "personale";
	public static final String VISIBILE = "visibile";
	public static final String URIFERIMENTO = "unita_riferimento";
	
	private ArrayList<Measures> misure;
	private String[] lista, lista_show;
	private int id;
	private double versione = 1.0;
	private String group;

	public GroupMeasures() {
		misure = new ArrayList<Measures>();
	}
	
	public static GroupMeasures load(InputStream input, Context context) {
		
		MyParser parser= new MyParser(input, context); 
		return parser.getParsedData();  	
	}
	
	public void update(double versione, ArrayList<Measures> newM, Context context) {
		
		for (Measures m : misure) {
			if (m.isPersonale()) 
				newM.add(m);
		}
		
		loadMisura(context);
		this.versione = versione;
		misure = BuildXML.ordina(newM, context);
		
		BuildXML b = new BuildXML(this, context);
		b.writeXml();
	}
	
	public void removeMisura(Measures m, Context context) {
		misure.remove(m);
		buildList(context);
	}
	
	public void removeMisura(String id, Context context) {
		for(Measures m: misure) {
			if (m.getId().equals(id)) {
				misure.remove(m);
				break;
			}
		}
		buildList(context);
	}
	
	public void removeMisura(int i, Context context) {
		misure.remove(i);
		buildList(context);
	}
	
	public static String loadSeparator(Context context) {
		return context.getString(R.string.separator).replace("\"", "");
	}
	
	public void buildList(Context context) {
		
		int show = 0;
		lista = new String[misure.size()];
    	for(int i=0; i<misure.size(); i++) {
    		lista[i] = misure.get(i).getId() + loadSeparator(context) + misure.get(i).getSymbol();
    		if (misure.get(i).isVisibile()) show++;
    	}
    	lista_show = new String[show];
    	int i = 0;
    	for(int k =0; k < misure.size(); k++) {
    		Measures m = misure.get(k);
    		if (m.isVisibile()) { 
    			lista_show[i] = lista[k];
    			i++;
    		}
    	}
	}
	
	public Measures getVisibleMeasure(int w) {
		int t = -1, i = -1;
		Measures output = null;
		do {
			i++;
			if (misure.get(i).isVisibile()) {
				output = misure.get(i);
				t++;
			}
			
		} while ((t<w) && (i< misure.size()));
		return output;
	}
	
	public String[] getListaVisibile() {		
		return lista_show;
	}
	
	public void reOrder(Context context) {
		misure = BuildXML.ordina(misure, context);
		buildList(context);
	}
	
	public void addMeasure(Measures m, Context context) {
		misure.add(m);
		misure = BuildXML.ordina(misure, context);
		buildList(context);
	}

	public String[] getList() {
		return lista;
	}

	public Measures getMeasure(int i) {
		return misure.get(i);
	}
	
	public ArrayList<Measures> getMeasures() {
		return misure;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(lista.length);
		dest.writeStringArray(lista);
		dest.writeInt(lista_show.length);
		dest.writeStringArray(lista_show);
		
		dest.writeInt(id);
		dest.writeString(group);
		dest.writeDouble(versione);
		
		dest.writeInt(misure.size());
		for(int i=0; i< misure.size(); i++)
			dest.writeParcelable(misure.get(i), i);
	}
	
	public GroupMeasures(Parcel in) {
		int l = in.readInt();
		lista = new String[l];
		in.readStringArray(lista);
		l = in.readInt();
		lista_show = new String[l];
		in.readStringArray(lista_show);
		
		id = in.readInt();
		group = in.readString();
		versione = in.readDouble();
		
		int size = in.readInt();
		misure = new ArrayList<Measures>();
		for(int i=0; i< size; i++)
			misure.add((Measures) in.readParcelable(GroupMeasures.class.getClassLoader()));
		
    }
	
	public boolean write(Context context) {
		BuildXML b = new BuildXML(this, context);
		return b.writeXml();
	}
	
	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public void setId(String id) {
		setId(Integer.parseInt(id));
	}

	public static final Parcelable.Creator<GroupMeasures> CREATOR
	= new Parcelable.Creator<GroupMeasures>() {
		public GroupMeasures createFromParcel(Parcel in) {
			return new GroupMeasures(in);
		}

		public GroupMeasures[] newArray(int size) {
			return new GroupMeasures[size];
		}
	};
	
	public void loadMisura(Context context) {
		
		 switch (id) {
         case 0:   
         	group = context.getString(R.string.lunghezza);
         	break;
         	
         case 1:   
          	group = context.getString(R.string.volume);
          	break;
          	
         case 2:   
          	group = context.getString(R.string.peso);
          	break;
         
         case 3:   
          	group = context.getString(R.string.temperatura);
          	break;
          	
         case 4:   
           	group = context.getString(R.string.tempo);
           	break;
           	
         case 5:   
           	group = context.getString(R.string.memoria);
           	break;
           	
         case 6:   
           	group = context.getString(R.string.velocità);
           	break;
           	
         case 7:   
           	group = context.getString(R.string.area);
           	break;
           	
         case 8:   
           	group = context.getString(R.string.angolo);
           	break;
           	
         case 9:   
           	group = context.getString(R.string.torsione);
           	break;
           	
         case 10:   
           	group = context.getString(R.string.carburante);
           	break;
           	
         case 11:   
           	group = context.getString(R.string.basi);
           	break;
           	
		 }
	}

	public double getVersione() {
		return versione;
	}

	public void setVersione(String versione) {
		try {
			this.versione = Double.parseDouble(versione);
		} catch (NullPointerException npe) {
			this.versione = 1.0;
			}
	}
}
