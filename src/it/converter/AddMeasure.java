package it.converter;

import it.converter.classes.R;
import XML.BuildXML;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import expr.Expr;
import expr.Parser;
import expr.SyntaxException;
import expr.Variable;


public class AddMeasure extends Activity {
	
	private Context context;
	// arraylist delle misure
	private GroupMeasures misura;
	// misura selezionata per il riferimento
	private Measures riferimento;
	private int ref_val = 0;
	// valori di controllo per verificare la correttezza del processo
	private boolean correctName = false, correctSymbol=false, 
		correctFormulaTo=false, correctFormulaFrom=false, correctFormulas = false;
	// parametri del nuovo oggetto Misura
	private String nome, simbolo;
	private String[] to_s,from_s;
	// oggetti della view
	private EditText to, from;
	private Button test, save;
	// stringhe temporanee pe
	private String temporary_s;
	private boolean temporary_b;
	// valori per modificare i simboli nelle formule nel caso questi
	// cambino dopo averla scritta
	private String backupSymbol = "";
	private boolean backupSymbol_=false;
	// usata nell'edit, è la misura da modificare
	private Measures oldmeasure;
	// indica se siamo in modalità edit o add
	private boolean isEditor;
	
	// valore di test dato in pasto alla formula
	public static final int TEST_VALUE = 10;
	
	
	///////////////////////////////////////////////////////////////////////////////////////
	// ONCREATE
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_measure);
       
        // gruppo della misura
        misura =  this.getIntent().getParcelableExtra("misura");    
        // valore booleano che mi dice se sono nell'editor o no
        isEditor = this.getIntent().getBooleanExtra("editor", false);
        
        context = this;
        
        // condizione per l'editor
        if (!isEditor)
        	// imposto il testo dell'intro 
        	setIntroText(this.getIntent().getStringExtra("gruppo"));
        else {
        	// recupero la misura selezionata
        	oldmeasure = this.getIntent().getParcelableExtra("personale"); 
        	// modifico l'array delle misure per escludere la mia
        	misura.removeMisura(oldmeasure.getId(), this);
        	// imposto alcuni parametri della view
        	setEditorView();
        }
        
        // imposto gli array per le formule
        // 0 => formula base
        // 1 => formula completa pronta per essere salvata
        // 2 => simbolo
        to_s = new String[3];
        from_s = new String[3];
        
        MeasureList();
        setEditTexts();
        setIOFormula();
        setTestButtonListener();
        setSaveButtonListener();
        
        if(isEditor) {
        	setRiferimento(oldmeasure);
        	setEditor(oldmeasure);
        }
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////
    // METODI PER L'EDITOR
    
    // modifico la view per adattarla all'Editor
    private void setEditorView() {
    	this.findViewById(R.id.intro_add_measure).setVisibility(View.GONE);
    	Button s = (Button)this.findViewById(R.id.button_save);
    	s.setText(R.string.save_edit);
    }
    
    // Imposto altri parametri dell'editor, in particolare carico i valori della misura prescelta
    // nei vari campi
    private void setEditor(Measures m) {
    	EditText name = (EditText)this.findViewById(R.id.add_name);
    	name.setText(m.getId());
    	correctName = true;
    	
    	EditText symbol = (EditText)this.findViewById(R.id.add_symbol);
    	symbol.setText(m.getSymbol());
    	correctSymbol=true;
    			
    	String formula_from = m.getFrom();
    	Log.i("test", "formula_from:"+formula_from+", rif.getfrom:"+riferimento.getFrom());
    	formula_from = formula_from.replace(riferimento.getFrom(), m.getSymbol());
    	from.setText(formula_from);
    	correctFormulaFrom=true;
    	
    	String formula_to = m.getTo();
    	formula_to = formula_to.replace(riferimento.getTo(), riferimento.getSymbol());
    	to.setText(formula_to);
    	correctFormulaTo=true;
    	
    	testSaveButton();
    }
    
    // imposto il valore di riferimento per le misure
    private void setRiferimento(Measures m) {
    	String separator = GroupMeasures.loadSeparator(this);
    	for(int i=0; i<misura.getList().length; i++) { 
    		String t = misura.getList()[i];
    		int end = t.indexOf(separator);
    		t = t.substring(0, end).trim();
			if (m.getUnita_riferimento().equalsIgnoreCase(t)) {
				ref_val = i;
				break;
			}
    	}
    	MeasureList();
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // METODO PER IMPOSTARE IL TESTO QUANDO NON SONO IN MODALITÀ EDITOR 
    
    private void setIntroText(String group) {
    	
    	String output = this.getResources().getString(R.string.intro_add_measure);
    	TextView tv = (TextView)findViewById(R.id.intro_add_measure);
    	tv.setText(output+" \""+group+"\".");
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // BOTTONE PER IMPOSTARE LA MISURA DI RIFERIMENTO
    
    private void MeasureList() {
    	
    	final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
    			android.R.layout.simple_spinner_dropdown_item, misura.getList());
    	
    	final Button setMeasure = (Button)findViewById(R.id.select_riferimento);
    	
    	// imposto il nome della misura sul bottone
    	setMeasure.setText(misura.getList()[ref_val]);
    	// imposto la misura di riferimento in base al valore di riferimento
    	riferimento = misura.getMeasure(ref_val);
    	
    	setMeasure.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				
				new AlertDialog.Builder(context)
				 .setTitle(context.getText(R.string.select_subtype))
				 .setSingleChoiceItems(adapter, ref_val, new DialogInterface.OnClickListener() {
					 
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
						setMeasure.setText(misura.getList()[which]);
						testXformula(riferimento.getSymbol(), misura.getMeasure(which).getSymbol(), true);
						riferimento = misura.getMeasure(which);
						ref_val = which;
						dialog.dismiss();
					}
				 }).create().show();	
			}
    	});
    }
 
    ///////////////////////////////////////////////////////////////////////////////////////////////
    // ABILITO L'I/O
    
    // abilito l'input per le formule
    private void setIOFormula() {
    	to = (EditText)findViewById(R.id.formula_to);
		from = (EditText)findViewById(R.id.formula_from);
    	test = (Button)findViewById(R.id.button_test_to);
    }
   
    // funzione per abilitare i listeners sull'input del NOME e del SIMBOLO
    private void setEditTexts() {
    		
    	enableNSListeners(R.id.add_name, misura.getList(), true);
    	
    	String[] temp = new String[misura.getList().length];
    	for(int i=0; i<misura.getList().length; i++)
    		temp[i] = misura.getMeasure(i).getSymbol();
    	
    	enableNSListeners(R.id.add_symbol, temp, false);
    }
    
    // funzione per abilitare i listeners sulla scrittura (nome e simbolo)
    private void enableNSListeners(int id, final String[] l, final boolean test) {
    	
    	final EditText et = (EditText)findViewById(id);
    	// recupero lo sfondo originale dell'edittext
    	final Drawable originalDrawable = et.getBackground();
    	
    	et.setEnabled(true);
    	
    	et.addTextChangedListener(new TextWatcher() {

    		// terminata la scrittura eseguo verifico che ciò che è stato scritto non sia
    		// già presente in un altra unità di misura
			@Override
			public void afterTextChanged(Editable arg0) {
				temporary_s = et.getText().toString();
				// se è stato inserito qualcosa, abilito il corrispondente booleano
				if (temporary_s.length()>0) {
					temporary_b = true;
					// testo che ciò che è stato inserito non corrisponda a una misura 
					// già presente 
					for(String t: l) 
						temporary_b = temporary_b && (temporary_s.trim().compareToIgnoreCase(t)!=0); 
					// nel caso il valore NON sia presente, cambio lo sfondo in verde, altrimenti
					// lo sfondo diventa rosso
					if (temporary_b) 
						et.setBackgroundResource(R.drawable.edittext_selector_green);
					else
						et.setBackgroundResource(R.drawable.edittext_selector_red);
				}
				else // nel caso non si sia scritto nulla, riporto lo sfondo al suo stato originale
					et.setBackgroundDrawable(originalDrawable);
				
				// la variabile TEST mi dice se sto trattano un NOME o un SIMBOLO
				// TRUE 	=> NOME
				// FALSE 	=> SIMBOLO
				if(test) {
		    		nome = temporary_s;
		        	correctName = temporary_b;
		    	}
		    	else { // nel caso venga modificato il simbolo, lo modifico anche nelle formule (se già scritte)
		    		testXformula(simbolo, temporary_s, false);
		    		simbolo = temporary_s;
		        	correctSymbol = temporary_b;
		    	}
		    	// abilito l'editing delle formule
				enableEditFromula();
				// eseguo il test per  abilitare il pulsante di SAVE
				testSaveButton();
				// resetto le variabili temporanee
		    	temporary_s = "";
		    	temporary_b = false;
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {}  		
    	});    	
    }
    
    // abilito gli edittext per la creazione delle formule
    private void enableEditFromula() {
    	// se il nome e il simbolo sono "corretti", cioé se
    	// non sono duplicati di misure già esistenti abilito i campi
    	final boolean t = correctName && correctSymbol;
    	//abilito i listeners
    	editListeners(to, t, simbolo, riferimento.getSymbol());
    	editListeners(from, t, riferimento.getSymbol(), simbolo);
 
    }
    
    // abilito i listener sugli edittext delle FORMULE
    private void editListeners(final EditText et, final Boolean t, String hint, final String simbolo) {
    	// abilito l'editText
    	et.setEnabled(t);
    
    	// imposto l'hint per guidare l'utente
    	et.setHint(hint+" = ");
    	// abilito i listener sul focus
    	et.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus && t && (et.getText().toString().length()==0)) {
					et.setText(simbolo);
					// posiziono il cursore alla fine del testo
					et.setSelection(et.getText().length());
					testSaveButton();
				}
			}
    	});
    }
    
    
    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    // SOSTITUZIONE DEL VECCHIO SIMBOLO NELLE FORMULE, NEL CASO VENGA SOTITUITO DALL'UTENTE
    // NEI CAMPI RISPETTIVI
    
    
    private void testXformula(String oldsym, String newsym, boolean to_b) {
    	// verifico se il nome e il simbolo sono corretti
    	if (correctName && correctSymbol) {
    		// questo valore mi dice che esiste un backup del simbolo precedentemente
    		// usato nella formula
    		if(backupSymbol_) oldsym = backupSymbol;
    		String temp = to.getText().toString();
    		// se necessario, modifico il testo presente nell'edittext per aggiornare
    		// il simbolo fornito dall'utente nel campo TO
    		if ((temp.length()>0) && (!oldsym.equals(newsym) && to_b && (newsym.length()>0))){
    			temp = temp.replace(oldsym, newsym);
    			to.setText(temp);
    		}    		
    		temp = from.getText().toString();
    		// stesso di sopra per il campo FROM
    		if ((temp.length()>0) && (!oldsym.equals(newsym)) && (!to_b) && (newsym.length()>0)) {
    			temp = temp.replace(oldsym, newsym);
    			from.setText(temp);
    		}
    		// aggiorno il backup del simbolo
    		if (backupSymbol_ = (newsym.length()==0)) 
    			backupSymbol = oldsym;
    	}
    }
    
    
   ////////////////////////////////////////////////////////////////////////////////////////////////////////
   // PULSANTE DI TEST

    
    // abilito il listener sul pulsante di test
    private void setTestButtonListener() {
    	
    	test.setOnClickListener(new OnClickListener() {
    		
			@Override
			public void onClick(View arg0) {
				
				// popolo i contenitori delle formule
				from_s[0] = from.getText().toString().replace(simbolo, "xx"); //sanitarizzo la variabile
				from_s[2] = simbolo;
				from_s[1] = from_s[0].replace("xx", riferimento.getFrom());
				
				to_s[0] = to.getText().toString().replace(riferimento.getSymbol(), "xx"); //sanitarizzo la variabile
				to_s[2] = riferimento.getSymbol();
				to_s[1] = to_s[0].replace("xx", riferimento.getTo());
				
				double from_v = 0, to_v;
				Expr expr_f, expr_t = null;
				String output = "";
				
				// testo la correttezza della formula FROM
				if(correctFormulaFrom = ((from_s[0].length()>0) && (from_s[0].contains("xx")))) {
					
					try {
						expr_f = Parser.parse(from_s[0]); 
						Variable x_f = Variable.make("xx");
						x_f.setValue(TEST_VALUE);
						from_v = expr_f.value();
						
						output = context.getResources().getString(R.string.test_formula);
						output = output.replace("xx", from_s[2] + " = " + TEST_VALUE )+" "+from_v+"\n";
						
						correctFormulaFrom = true;
						
					} catch (SyntaxException e) {
						output = e.explain();
						int initial = output.indexOf("as far as \"");
						output = output.substring(initial+11);
						int fin = output.indexOf("\"");
						output = output.substring(0, fin);
						output = context.getResources().getString(R.string.test_formula_err)
								.replace("xx", output)+"\n";
						
						correctFormulaFrom = false;
					}
				} 
				// testo la correttezza della formula to
				else if ((!from_s[0].contains(from_s[2])) && (from_s[0].length()>0))
					
					output+= context.getResources().getString(R.string.delete_x_from_first).replace("x", from_s[2]);
				
				if(correctFormulaTo = ((to_s[0].length()>0) && (to_s[0].contains("xx")))) {
					
					try {
						expr_t = Parser.parse(to_s[0]); 
						Variable x_t = Variable.make("xx");
						x_t.setValue(TEST_VALUE);
						to_v = expr_t.value();
						
						output+= context.getResources().getString(R.string.test_formula);
						output = output.replace("xx", to_s[2] + " = " + TEST_VALUE )+" "+to_v+"\n";
						
						correctFormulaTo = true;
						
					} catch (SyntaxException e) {
						output = e.explain();
						int initial = output.indexOf("as far as \"");
						output = output.substring(initial+11);
						int fin = output.indexOf("\"");
						output = output.substring(0, fin);
						output = context.getResources().getString(R.string.test_formula_err)
								.replace("xx", output)+"\n";
						
						correctFormulaTo = false;
					}
				}
				// verifico se le formule sono una l'inverso dell'altra
				else if ((!to_s[0].contains(to_s[2])) && (to_s[0].length()>0))
					output+= context.getResources().getString(R.string.delete_x_from_second).replace("x", to_s[2]);
				
				if (correctFormulaTo && correctFormulaFrom) {
					
					Variable x_t = Variable.make("xx");
					x_t.setValue(from_v);
					double test_value = expr_t.value();
					
					correctFormulas = (Math.abs(test_value-TEST_VALUE) <= 0.001);
					
					if (correctFormulas) 
						output+= context.getResources().getString(R.string.formule_corrette);
					else
						output+= context.getResources().getString(R.string.formule_scorrette);
					
				}
				
				testSaveButton();
				Toast.makeText(context, output, Toast.LENGTH_LONG).show();
			}			
    	});
    }
    
    // funzione che testa se è possibile abilitare il pulsante di salvataggio e di test
    private void testSaveButton() {
    	
    	boolean simboli = false;
    	
    	try {
    		simboli = (from_s[2] == simbolo) && (to_s[2] == riferimento.getSymbol());
    	} catch (NullPointerException npe){};
    	
    	// abilito (in caso) il bottone di test
    	test.setEnabled(correctName && correctSymbol && (from.getText().length()>0) &&
        			(to.getText().length()>0));
    	
    	// abilito (in caso) il bottone di salvataggio
    	save.setEnabled(correctFormulas &&  simboli);
	}
    
    
    ///////////////////////////////////////////////////////////////////////////////////////////////
    // SALVATAGGIO
    
    
    private void setSaveButtonListener() {
    	save = (Button)findViewById(R.id.button_save);
    	
    	save.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// costruisco un nuovo oggetto MEASURES
				Measures m = new Measures();
		    	m.setId(nome);
		    	m.setSymbol(simbolo);
		    	m.setFrom(from_s[1]);
		    	m.setTo(to_s[1]);
		    	m.setPersonale(true);
		    	m.setUnita_riferimento(riferimento.getId());
		    	// la aggiungo al gruppo 
		    	misura.addMeasure(m, context);
		    	
		    	Log.i("test", "Misura aggiunta:"+m.toString());
		    	
		    	// salvo la misura sulla SD
		    	BuildXML bxml = new BuildXML(misura, context);
		    	if (bxml.writeXml()) {
		    		Toast.makeText(context, R.string.salvataggio_ok, Toast.LENGTH_LONG);
		    		
		    		// restituisco il valore con l'intent
		    		Intent i = new Intent();
		    		i.putExtra("misura", misura);
		    		setResult(Activity.RESULT_OK, i);
		    		// termino l'anbaradan
		    		finish();
		    	}
		    	else
		    		Toast.makeText(context, R.string.salvataggio_errore, Toast.LENGTH_LONG);	
			}  		
    	});    	
    }
}
