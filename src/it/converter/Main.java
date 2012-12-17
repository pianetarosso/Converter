package it.converter;

import it.converter.classes.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

import org.acra.sender.ReportSenderException;

import XML.BuildXML;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
 
public class Main extends Activity {
    /** Called when the activity is first created. */
	
	private EditText input, output; 	
	private Button classi, partenza, arrivo;	 
	private ImageButton cancella, scambia, add, copy; 
	private Context context;
	
	private String[] gruppi;	// lista dei gruppi
	private ArrayList<GroupMeasures> misure;	// array di gruppi delle misure 
	
	private Measures partenza_c = null, arrivo_c = null; // oggetti "Measures" selezionati per la conversione
	
	private int partenza_v = -1;	// indicatore del valore scelto dal menu per la misura di partenza
	private int arrivo_v = -1; 	// indicatore del valore scelto dal menu per la misura di arrivo
	private int gruppo_v = -1;	// indicatore del valore scelto dal menu per il gruppo delle misure
	
	private GroupMeasures partenza_measure, arrivo_measure; // gruppi delle misure scelti

	private int GRUPPI;	// numero di gruppi
	
	private ArrayList<int[]> gruppi_ordinati;	// array dei gruppi ordinato in base all'uso
	private boolean contatore_salvato = false;	// se false vuol dire che il gruppo è già stato incrementato
	SharedPreferences sp;
	Editor edit;
	
	private final String TEST_UPDATE = "test_update"; // 
	private final double VERSIONE = 1.0;	// versione del programma
	private boolean update_measures = false;	// booleano per l'update
	
	// valori di riferimento per startIntentForResult
	private int EDITOR = 31;		// valore per chiamare l'editor
	private int ADDMEASURE = 42;	// valore per chiamare addmeasure
	
	// stringhe per recuperare l'ordinamento dalle SP
	public static final String ORDINAMENTO = "Ordinamento_lista";
	private final String CONTATORE = "contatore_uso";
	private final int TEST_RESET_CONTATORE = 10;
	
	public static final String MODI_ORDINAMENTO = "modi_ordinamento";
	
	private final int MAX_MEASURES_SHARING = 5;
	private final String SHARING_SETTINGS = "Sharing settings";
	private final String SHARING_DATE = "data";
	private final String SHARING_COUNT = "conteggio";
	private int count = 0;
	
	/////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        context = this;
        
        GRUPPI = Integer.parseInt(this.getString(R.string.GRUPPI)); // carico il numero di gruppi dalle SP
        		
        load_ordinamento();
	
        bindButtonsAndText();
       
        resetButton();
       
        setGroupListener();
       
        setAddButton();
      
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////
    // GESTIONE ARRAY DELLA LISTA PER LE PREFERENZE
    
    private void load_ordinamento() {
    //	Log.i("T", null); // test ACRA
    	// carico il gruppo di ORDINAMENTO
	    sp = context.getSharedPreferences(ORDINAMENTO, MODE_PRIVATE);
	    edit = sp.edit();
	    // recupero il numero di utilizzi dell'applicazione
	    int numero_utilizzi = sp.getInt(CONTATORE, 1);
	    
	    gruppi_ordinati = new ArrayList<int[]>();
	    
	    // carico i gruppi, in un arraylist, ogni elemento è costituito
	    // da un array di int[]
	    // 0 => indica il gruppo (es. c0)
	    // 1 => valore del contatore
	    for(int i=0; i<GRUPPI; i++) {
	    
	    	int[] value = new int[2];
	    	
	    	value[0] = i;
	    	value[1] = sp.getInt("c"+i, 0);
	    	
	    	gruppi_ordinati.add(value);
	    }
	 
	    // funzione che, in caso ci siano stati più di 10 utilizzi
	    // dell'applicazione, resetto il contatore totale, e normalizzo
	    // i contatori dei singoli gruppi
	    if (numero_utilizzi >= TEST_RESET_CONTATORE) {
	    	for(int i=0; i<GRUPPI; i++) 
	    		edit.putInt("c"+i, (int)gruppi_ordinati.get(i)[1]/numero_utilizzi);
	    		
	    	numero_utilizzi = 0;
	    	}
	    // nel caso non resetti, incremento il contatore totale
	    numero_utilizzi++;
	    edit.putInt(CONTATORE, numero_utilizzi++);
	    edit.commit();
	    
	    // eseguo l'ordinamento della lista basandomi sui contatori
	    Collections.sort(gruppi_ordinati, new Comparator<int[]>() {
			@Override
			public int compare(int[] lhs, int[] rhs) {
				
				String t1 = lhs[1]+"";
				String t2 = rhs[1]+"";
				return t2.compareTo(t1);
			}
	    });
	     // carico le risorse in base all'ordinamento dato 
	    loadMeasures(gruppi_ordinati);
    }
    	
    	/*
    	 * lunghezza 	=> c0 ok
    	 * volume 		=> c1 ok
    	 * peso			=> c2 ok
    	 * temperatura	=> c3 ok
    	 * tempo		=> c4 ok
    	 * memoria		=> c5 ok
    	 * velocità		=> c6 ok
    	 * area			=> c7 ok
    	 * angolo		=> c8 ok (problemi con minuti/secondi d'arco)
    	 * torque		=> c9 ok (controllare)
    	 * carburante	=> c10
    	 * 
    	 * 
    	 * capire come implementare i successivi
    	 * basi numeriche=>c11
    	 * taglie(M)	=> c12
    	 * taglie(F)	=> c13
    	 * 
    	 */
    	
  //////////////////////////////////////////////////////////////////////////////////////////////////////  	
  // FUNZIONE PER L'UPDATE
    
    // funzione che verifico la versione dalle SP per un eventuale update
    private void testUpdate() {
    	
    	// Carico il numero di versione salvata nelle SP
    	SharedPreferences sp = context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE);
    	double test = Double.parseDouble(sp.getString(TEST_UPDATE, "0"));
    	Log.i("test", "versione attuale:"+test);
    	
    	// verifico se la versione attuale è più recente di quella salvata nell SP
    	if(test<VERSIONE) {
    		update_measures = true;	// imposto la variabile per effettuare l'update
    		Editor ed = sp.edit();
    		ed.putString(TEST_UPDATE, VERSIONE+"");	// aggiorno la versione nelle SP
    		ed.commit();
    	}
    }
    
    // verifico sei i file delle misure sulla SD sono aggiornati (in caso di update dell'app)
    private GroupMeasures testUpdate(InputStream is, int i) {
    	GroupMeasures t, tR;
    	// recupero il sorgente della risorsa da R
    	InputStream isR = loadFromResources(i);
    	// carico la stessa risorse da R e dalla SD
    	t = GroupMeasures.load(is, context);
    	tR = GroupMeasures.load(isR, context);
    	// verifico se la versione del file xml sulla SD è precedente a quella di R
    	if (tR.getVersione()>t.getVersione()) 
    		// in questo caso chiamo la funzione update per il file della SD
    		t.update(tR.getVersione(), tR.getMeasures(), context);    		
    	return t;
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////
    // CARICAMENTO DELLE RISORSE
    
    
    // classe che recupera in modo dinamico le resources da R, tramite stringa costruita
    private static int getResId(String variableName, Context context, Class<?> c) {
        try {
            Field idField = c.getDeclaredField(variableName);
            return idField.getInt(idField);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } 
    }
    
    
    // caricamento risorse da R tramite costruzione della risorsa
    private InputStream loadFromResources(int i) {
    	// recupero l'id della risorsa da R dopo aver costruito l'indirizzo nella forma "c"+int
    	int id = getResId("c"+i, this, it.converter.classes.R.raw.class);
    	// recupero l'inpuStream basandosi sull'id
    	InputStream is = this.getApplicationContext().getResources().openRawResource(id);
		Log.i("load", "c"+i+" loaded from R");
		return is;
    }
    
    
    // caricamento delle GroupMeasures da R o SD
    private void loadMeasures(ArrayList<int[]> ordine) {
    	  
    	// verifico se è accaduto un update
    	testUpdate();
    	
    	// costruisco gli oggetti da riempire
    	misure = new ArrayList<GroupMeasures>();
    	gruppi = new String[GRUPPI];

    	// ciclo for di caricamento
    	for (int i=0; i<GRUPPI; i++) {
    		InputStream is;
    		GroupMeasures t;
    		
    		// recupero il numero del gruppo da caricare in base 
    		// all'ordinamento
    		int o = ordine.get(i)[0];
    		
    		// variabile usata nel caso dell'update, server per verificare se è stato
    		// caricato un file dalla SD
    		boolean testUpdate = false;
    		try {
    			// tenta il caricamento del file dalla SD
    			File file = BuildXML.buildPath(context.getString(R.string.app_name), o+".xml");
    			is = new FileInputStream(file);
    			testUpdate = true;
    			Log.i("load", "c"+o+" loaded from SD");
    		} catch (Exception e) {	
    			// nel caso il precedente non riesca, carica il file dalle SP
    			is = loadFromResources(o);
    		}
    		// nel caso ci sia un update, e il file provenga dalla SD
    		if (testUpdate && update_measures) 
    			// verifico se posso aggiornare il file
    			t = testUpdate(is, o); 
    		else
    			// nel caso normale procedo al suo caricamento
    			t = GroupMeasures.load(is, context);
    		// aggiungo il gruppo
    		misure.add(t);
    		// costruisco la lista
    		gruppi[i] = t.getGroup(); 
    	}
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////
    // GESTIONE BOTTONI
    
    // recupero i BUTTONS e gli EDITTEXT
	private void bindButtonsAndText() {
    	
    	input = (EditText)findViewById(R.id.editTextConvert);
    	output = (EditText)findViewById(R.id.editTextResult);
    	
    	classi = (Button)findViewById(R.id.button_classi);
    	partenza = (Button)findViewById(R.id.button_partenza);
    	arrivo = (Button)findViewById(R.id.button_arrivo);
    	
    	cancella = (ImageButton)findViewById(R.id.button_reset);
    	scambia = (ImageButton)findViewById(R.id.button_scambia);
    	
    	add = (ImageButton)findViewById(R.id.button_add);
    	add.setEnabled(false);
    	
    	copy = (ImageButton)findViewById(R.id.button_copy);
    	
    	resetMeasures();
    	setScambiaListener();
    	setCopyListener();
    }

	// resetto il testo dei bottoni, e imposto gli indicatori delle misure scelte a -1
	private void resetMeasures() {
    	partenza.setText(R.string.select_subtype);
    	arrivo.setText(R.string.select_subtype_to);
    	partenza_v = -1;
    	arrivo_v = -1;
    }

	// imposto il bottone per scambiare le unità in gioco
	private void setScambiaListener() {
    	
		// setto il click listener
    	scambia.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// effettuo lo scambio solo se i valori di arrivo e partenza sono stati impostati
				if ((partenza_v > -1) && (arrivo_v > -1)) {
					
					partenza.setText(arrivo_measure.getListaVisibile()[arrivo_v]);
					partenza_c = arrivo_measure.getMeasure(arrivo_v);
					
					arrivo.setText(partenza_measure.getListaVisibile()[partenza_v]);
					arrivo_c = partenza_measure.getMeasure(partenza_v);
					
					int t = partenza_v;
					partenza_v = arrivo_v;
					arrivo_v = t;
					// effettuo nuovamente i calcoli
					calculate();					
				}
			}
    	});
    }
	
	// creo il listener per il bottone per la copia
	private void setCopyListener() {
    	copy.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// uso la copia solo se la lunghezza della stringa è >0
				if (output.getText().toString().length()>0) {
					ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE); 
					clipboard.setText(output.getText().toString());	
					Toast t = Toast.makeText(context, R.string.copy_s, Toast.LENGTH_SHORT);
					t.show();
				}
			}
    	});
    }

	// listener per il bottone ADD per creare una nuova misura
    private void setAddButton() {
    	
    	add.setOnClickListener(new OnClickListener() {
    		
			@Override
			public void onClick(View v) {
				// verifico che l'utente abbia settato un gruppo	
				if (gruppo_v >= 0) {
					Intent i = new Intent(Main.this, AddMeasure.class);
			    	// aggiungo il gruppo della misura
			    	i.putExtra("misura", misure.get(gruppo_v));
			    	// aggiungo la stringa del gruppo
			    	i.putExtra("gruppo", gruppi[gruppo_v]);
			    	// valore che mi indica se chiamo AddMeasure da EditorM o dal Main
			    	i.putExtra("editor", false);
			    	startActivityForResult(i, ADDMEASURE);
				}
			}
    	});
    }
    
    // bottone di reset, cancella i campi degli EDITTEXT input e output
    private void resetButton() {
    	cancella.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				input.setText("");
				output.setText("");
			}
    	});
    }
    
   
    /////////////////////////////////////////////////////////////////////////////////////////////////
    // BOTTONE PER SELEZIONARE IL GRUPPO
    
    private void setGroupListener() {
    	
    	// costruisco l'adapter che mostrerà la lista dei gruppi
    	final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
    	        android.R.layout.simple_spinner_dropdown_item, gruppi);
    	
    	// imposto il clicklistener
    	classi.setOnClickListener(new OnClickListener () {

			@Override
			public void onClick(View v) {
				
				 new AlertDialog.Builder(context)
				 .setTitle(context.getText(R.string.select_type)) // imposto il titolo del DIALOG
				 // imposto il DIALOG come scelta singola, impostando l'elemento 
				 // selezionato GRUPPO_V
				 .setSingleChoiceItems(adapter, gruppo_v, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// imposto il titolo del bottone
						classi.setText(gruppi[which]);
						// cancello i campi del testo
						cancella.performClick();
						// resetto i bottoni e le misure scelte
						resetMeasures();
						// imposto nuovamente i valori che si possono scegliere dai bottoni
						updateMeasuresListeners(which);
						// abilito gli ascoltatori del testo (per il calcolo in tempo reale)
						enableTextListener();
						// imposto il gruppo selezionato
						gruppo_v = which;
						// abilito il pulsante per aggiungere una nuova misura
						add.setEnabled(true);
						contatore_salvato = true;
						dialog.dismiss();
					}
				 }).create().show();
			}			
    	});	
    }
    
    
    /////////////////////////////////////////////////////////////////////////////////////////////////
    // BOTTONE PER SELEZIONARE LE MISURE
    
    // imposto i bottoni delle misure 
    private void updateMeasuresListeners(int which) {
    	
    	// imposto a NULL le Measures  
    	partenza_c = null;
    	arrivo_c = null;
    	
    	// recupero il gruppo delle misure
    	partenza_measure = misure.get(which);
    	
    	// imposto l'adapter per il bottone di "partenza"
    	final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
    			android.R.layout.simple_spinner_dropdown_item, partenza_measure.getListaVisibile());
    	
    	partenza.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				
				new AlertDialog.Builder(context)
				 .setTitle(context.getText(R.string.select_subtype))
				 .setSingleChoiceItems(adapter, partenza_v, new DialogInterface.OnClickListener() {
					 
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
						partenza.setText(partenza_measure.getListaVisibile()[which]);
						// imposto come Measures solo una di quelle "visibili"
						partenza_c = partenza_measure.getVisibleMeasure(which);
						// imposto l'id della misura 
						partenza_v = which;
						// calcolo i nuovi valori
						calculate();
						dialog.dismiss();
					}
				 }).create().show();	
			}
    	});
    
    	// imposto il gruppo di arrivo
    	arrivo_measure = misure.get(which);
    	
    	final ArrayAdapter<String> adapterA = new ArrayAdapter<String>(this,
    			android.R.layout.simple_spinner_dropdown_item, arrivo_measure.getListaVisibile());
    	
    	arrivo.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				
				new AlertDialog.Builder(context)
				 .setTitle(context.getText(R.string.select_subtype))
				 .setSingleChoiceItems(adapterA, arrivo_v, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// imposto come testo un valore "visibile"
						arrivo.setText(arrivo_measure.getListaVisibile()[which]);
						// imposto come Measures una "visibile" 
						arrivo_c = arrivo_measure.getVisibleMeasure(which);
						// imposto l'id della misura
						arrivo_v = which;
						calculate();
						dialog.dismiss();
					}
				 }).create().show();	
			}
    	});	
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // TEXT LISTENERS
    
    // abilito il text listener per il campo di immissione del valore da convertire
    private void enableTextListener() {
    	
    	try {
	    	input.addTextChangedListener(new TextWatcher() {
	
				@Override
				public void afterTextChanged(Editable s) {}
	
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count,
						int after) {}
	
				@Override
				public void onTextChanged(CharSequence s, int start, int before,
						int count) {
					// quando il testo si modifica viene effettuato il calcolo
					calculate();
				}
	    	});
    	} catch (Exception e) {
    		cancella.performClick();
    	}
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////
    // FUNZIONE DI CALCOLO
    
    private void calculate() {
    	// converto il valore da String in int
    	String textI = input.getText().toString();
    	// verifico che siano state selezionate delle unità di misura
    	// e che il la lungezza della stringa immessa sia >0
    	if ((partenza_c!=null) && (arrivo_c!=null) && (textI.length()>0)) {
	    	double in = Double.parseDouble(textI);
	    	double temp = partenza_c.getFrom(in);
	    	double out = arrivo_c.getTo(temp);
	    	output.setText(out+ " " + arrivo_c.getSymbol());
	    	
	    	if (contatore_salvato) {
	    		int[] t = gruppi_ordinati.get(gruppo_v);
	    		int ref = t[0];
	    		int value = t[1];
	    		value++;
	    		t[1] = value;
	    		gruppi_ordinati.remove(gruppo_v);
	    		gruppi_ordinati.add(gruppo_v, t);
	    		edit.putInt("c"+ref, value);
	    		Log.i("ordinamento", "ref:"+ref+", value:"+value);
	    		edit.commit();
	    		contatore_salvato = false;
	    	}
    	}
    }
     
    ///////////////////////////////////////////////////////////////////////////////////////////////
    // MENU
    
    // creo il menu
 	@Override
     public boolean onCreateOptionsMenu(Menu menu) {
         MenuInflater inflater = getMenuInflater();
         inflater.inflate(R.menu.main_menu, menu);
         return true;
     } 
 	
 	// imposto le opzioni di selezione
 	@Override
     public boolean onOptionsItemSelected(MenuItem item) {
         switch (item.getItemId()) {
         // nel caso sia selezionata l'opzione editor
         case R.id.editor_misure: 
        	 Intent i = new Intent(Main.this, EditorM.class);
        	 // aggiungo la lista dei gruppi
        	 i.putExtra("misure", misure);
        	 // aggiungo l'array dei nomi dei gruppi
        	 i.putExtra("gruppi", gruppi);
        	 startActivityForResult(i, EDITOR);
        	
         	break;
         
         case R.id.preferenze:
        	 setPreferences();
        	 break;
      
 	
         case R.id.send_m:
        	 sendPersonalMeasures();
        	 break;
        	 
         }
         return true;
 	}

 	
 	// funzione per gestire le preferenze di ordinamento, 
 	// il valore viene salvato nelle SP per essere poi riutilizzato
 	private void setPreferences() {
 		
 		// carico il numero possibile di metodi di ordinamento
 		int numero_id = getResId(MODI_ORDINAMENTO, this, it.converter.classes.R.string.class);
 		int numero = Integer.parseInt(context.getString(numero_id));
 				
 		String[] tipi_ordinamento = new String[numero];
 		//carico le stringhe con i tipi
 		for(int i=0; i<numero; i++) {
 			int id = getResId("o"+i, this, it.converter.classes.R.string.class);
 			tipi_ordinamento[i] = context.getString(id);	
 			Log.i("ordinamento", "modi:"+tipi_ordinamento[i]);
 		}
 		
 		final int choosed = context.getSharedPreferences(ORDINAMENTO, Context.MODE_PRIVATE).getInt(MODI_ORDINAMENTO, 0);
 		
 		
 		new AlertDialog.Builder(context)
 		.setTitle(R.string.preferenze_o)
 		.setSingleChoiceItems(tipi_ordinamento, choosed, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				context.getSharedPreferences(ORDINAMENTO, Context.MODE_PRIVATE)
				.edit().putInt(MODI_ORDINAMENTO, which).commit();
				// se il valore di ordinamento dell'utente è differente, riordino le liste
				if (choosed != which) {
					for(int i=0; i< misure.size(); i++) {
						GroupMeasures gm = misure.get(i);
						gm.reOrder(context);
						misure.remove(i);
						misure.add(i, gm);
					}
					cancella.performClick();
					// resetto i bottoni e le misure scelte
					resetMeasures();
					// imposto nuovamente i valori che si possono scegliere dai bottoni
					updateMeasuresListeners(gruppo_v);
				}
				dialog.dismiss();
			}
		})
 		.create()
 		.show();
 	}
 	
 	// funzione per inviarmi una mail all'indirizzo eidolon.software@gmail.com
 	// con le proprie misure personali
 	private void sendPersonalMeasures() {
 		
 		
 		final SharedPreferences sp = context.getSharedPreferences(SHARING_SETTINGS, Context.MODE_PRIVATE);
 		
 		// recupero la data dell'inizio del conteggio e il conteggio
 		long old_date = sp.getLong(SHARING_DATE, 0);
 		count = sp.getInt(SHARING_COUNT, 0);
 		
 		Calendar current_time = Calendar.getInstance();
 		Calendar oldtime = Calendar.getInstance();
 		oldtime.setTimeInMillis(old_date);
 		// aggiungo 1 giorno alla vecchia data
 		oldtime.add(Calendar.DAY_OF_MONTH, 1);
 		
 		// abbiamo questi casi:
 		// 1) currentime < oldtime && count < MAX => ok invia + aggiorna count
 		// 2) currentime > oldtime && (count < MAX  || count >= MAX)=> ok invia + aggiorna data + setta count a 1
 		// 3) currentime < oldtime && count >= MAX => nega l'invio
 		
 		final boolean test_oldtime = oldtime.after(current_time);
 		
 		if ((test_oldtime && (count < MAX_MEASURES_SHARING)) || (!test_oldtime)) {
 		
	 		final ArrayList<Measures> pm = new ArrayList<Measures>();
	 		
	 		// costruisco la lista di misure personali
	 		for(GroupMeasures gm: misure) {
	 			for(Measures m: gm.getMeasures()) {
	 				if(m.isPersonale())
	 					pm.add(m);
	 			}
	 		}
	 		
	 		// costruisco l'array di stringhe da mostrare e il booleano (all false)
	 		String[] mp = new String[pm.size()];
	 		final boolean[] show = new boolean[pm.size()];
	 		for(int i=0; i< pm.size(); i++) {
	 			mp[i] = pm.get(i).getId();
	 			show[i] = false;
	 		}
	 		
	 		
	 		new AlertDialog.Builder(context)
	 		.setTitle(context.getText(R.string.select_type_to_send))
	 		// dò in pasto al dialog l'array dei nomi delle misure, insieme all'array della visibilità
	 		.setMultiChoiceItems(mp, show, new OnMultiChoiceClickListener() {
	
	 			@Override
	 			public void onClick(DialogInterface arg0, int arg1,
	 					boolean arg2) {
					show[arg1] = arg2;
				}				 
			 })
			 // imposto il bottone per il salvataggio
			 .setPositiveButton(R.string.next, new DialogInterface.OnClickListener() {
	
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
					
					String output = "";
					// modifico i valori di visibilità
					for(int i=0; i<show.length; i++) {
						if(show[i]) 
							output += pm.get(i).toString() + " \n";
					}
					
					if (output.length()> 0) {
						// Prendiamo dal context il ConnectivityManager
						ConnectivityManager connManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
						// Prendiamo le informazioni della connessione mobile
						NetworkInfo netInfo= connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
						// Prendiamo le informazioni della connessione WiFi
						NetworkInfo wifiInfo= connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
						
						// verifico se è disponibile una connessione a internet
						if ( netInfo.getState() != NetworkInfo.State.CONNECTED && wifiInfo.getState() != NetworkInfo.State.CONNECTED ) 
							Toast.makeText(Main.this, R.string.no_internet, Toast.LENGTH_SHORT).show();
						else {
							GoogleSender gs = new GoogleSender(context);
							try {
								gs.send(pm);
								Toast.makeText(Main.this, R.string.send_ok, Toast.LENGTH_SHORT).show();
								Editor ed = sp.edit();
								// aggiorno i valori per il MAX numero di sharing nella stessa giornata
								if (test_oldtime) {
						 			ed.putInt(SHARING_COUNT,  count+1);Log.i("test", "++:"+(count+1));}
						 		else {
						 			ed.putInt(SHARING_COUNT, 0);
						 			ed.putLong(SHARING_DATE, Calendar.getInstance().getTimeInMillis());
						 		}
								ed.commit();
								
							} catch (ReportSenderException e) {
								e.printStackTrace();
								Toast.makeText(Main.this, R.string.error_sending, Toast.LENGTH_SHORT).show();
							}
						}
						dialog.dismiss();
					}
					else	
						Toast.makeText(Main.this, R.string.error_no_selection, Toast.LENGTH_SHORT).show();
				} 
			 }).create().show();
 		}
 		else 
 			Toast.makeText(Main.this, R.string.error_exceed_sharing, Toast.LENGTH_SHORT).show();
 	}
 
 	// funzione per intercettare il valore restituito da StartActivityForResult
 	protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
 		
 		ArrayList<GroupMeasures> g = null;
 		// nel caso il valore sia restituito dall'editor
        if ((requestCode == EDITOR) && (resultCode == RESULT_OK)) 
        	// recupero l'array delle misure
        	g = data.getParcelableArrayListExtra("misure");	
        // nel caso il valore sia restituito dall'add
        else if ((requestCode == ADDMEASURE) && (resultCode == RESULT_OK)) {
        	// recupero il gruppo
        	GroupMeasures m = data.getParcelableExtra("misura");	
        	if (m!= null) {
        		g = misure;
        		// scansiono la lista per sostituire il gruppo dato con il mio
            	for(int i=0; i<misure.size(); i++)	{
            		if(m.getId()==g.get(i).getId()) {
            			g.remove(i);
            			g.add(i, m);
            			break;
            		}
            	}
        	}
        }

        // se g NON è nullo
        if (g!= null) {
        	// reimposto misure
        	misure = g;
        	// resetto i vari campi e reimposto bottoni e ascoltatori
            this.resetButton();
        	this.setGroupListener();
        	if (gruppo_v >-1) {
        		// cancello i campi del testo
				cancella.performClick();
				// resetto i bottoni e le misure scelte
				resetMeasures();
				// imposto nuovamente i valori che si possono scegliere dai bottoni
				updateMeasuresListeners(gruppo_v);
        	}
        }
    }
}
