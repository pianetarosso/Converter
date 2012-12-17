package it.converter;

import it.converter.classes.R;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

public class EditorM extends Activity {
	
	private Context context;
	private String[] gruppi;
	private ArrayList<GroupMeasures> misure;
	private ArrayList<String> misure_personali_s;
	private ArrayList<String> misure_s;
	private ArrayList<Integer> misure_personali;
	
	// posizione dei menu
	private int group_menu_position = -1;
	private int group_edit_position = -1;
	
	// gruppo selezionato
	private GroupMeasures selected;
	private int selected_s = -1;
	private Button show_hide, edit, delete, select_group;
	
	// valore per chiamare intent
	private final int EDITOR = 4;

	////////////////////////////////////////////////////////////////////////////////////////////
	// ONCREATE
	
	 @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.editorm);
	        
	        context = this;     
	        
	        // recupero i nomi dei gruppi
	        gruppi = this.getIntent().getStringArrayExtra("gruppi");
	        // recupero la lista dei gruppi
	        misure = this.getIntent().getParcelableArrayListExtra("misure");
	        
	        // inizializzo le misure personali
	        misure_personali_s = new ArrayList<String>();
	        misure_s = new ArrayList<String>();
	        misure_personali = new ArrayList<Integer>();
	        
	        bindButtons();
	 }

	 ////////////////////////////////////////////////////////////////////////////////////////////
	 // BOTTONI
	 
	 // carico i bottoni
	 private void bindButtons() {
		 
		 select_group = (Button)this.findViewById(R.id.select_type);
		 show_hide = (Button)this.findViewById(R.id.show_hide);
		 edit = (Button)this.findViewById(R.id.edit_personal);
		 delete = (Button)this.findViewById(R.id.delete_personal);
		 
		 setGroupListener(select_group);
	 }
	 
	 ///////////////////////////////////////////////////////////////////////////////////////////
	 // BOTTONE DEL GRUPPO
	 
	 private void setGroupListener(final Button classi) {
	    	
		 // imposto l'adapter
		 final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				 android.R.layout.simple_spinner_dropdown_item, gruppi);
	    	
		 classi.setOnClickListener(new OnClickListener () {
			 
			 @Override
			 public void onClick(View v) {
					
				 new AlertDialog.Builder(context)
				 .setTitle(context.getText(R.string.select_type))
				 .setSingleChoiceItems(adapter, group_menu_position, new DialogInterface.OnClickListener() {

					 @Override
					 public void onClick(DialogInterface dialog, int which) {
						
						 group_menu_position = which;
						 classi.setText(gruppi[which]);	
						 // imposto la variabile del gruppo selezionato
						 selected_s = which;
						 
						 setPerasonalMeasures();
						
						 dialog.dismiss();
					 }
				 }).create().show();
			 }			
		 });	
	 }
	 
	 
	 // imposto l'array delle misure personali e abilito (in caso) i pulsanti EDIT E DELETE
	 private void setPerasonalMeasures() {
		 
		 selected = misure.get(selected_s);
		 misure_personali.clear();
		 misure_personali_s.clear();
		 
		 // scansiono il gruppo per trovare le misure personali dell'utente (MISURE_PERSONALI)
		 // e per popolare l'array  di stringhe MISURE_S
		 for (int i=0; i< selected.getMeasures().size(); i++) {
			 Measures m = selected.getMeasure(i);
			 misure_s.add(m.getId());
			 if (m.isPersonale()) {
				 misure_personali.add(i);
				 misure_personali_s.add(m.getId());
			 }
		 }
		 
		 // se esistono misure personali, abilito il tasto EDIT
		 if (misure_personali.size() > 0) {
			 edit.setEnabled(true);
			 delete.setEnabled(true);
			 setEditListener(edit);
			 setDeleteListener(delete);
		 }
		 else {
			 edit.setEnabled(false);
			 delete.setEnabled(false);
		 }
		 		
		 // abilito il tasto per visualizzare o meno le misure
		 show_hide.setEnabled(true);
		 setShowHideListener(show_hide);
	 
	 }
	
	 ////////////////////////////////////////////////////////////////////////////////////////////
	 // BOTTONE PER IMPOSTARE LA VISIBILITÀ DELLE MISURE
	 
	 private void setShowHideListener(Button button) {
	    	
		 button.setOnClickListener(new OnClickListener () {
			 
			 @Override
			 public void onClick(View v) {
				 // costruisco l'array della visibilità
				 final boolean[] show = new boolean[selected.getList().length];
				 for(int i=0; i < selected.getList().length; i++) 
					 show[i] = selected.getMeasure(i).isVisibile();
				 
				 new AlertDialog.Builder(context)
				 .setTitle(context.getText(R.string.select_type))
				 // dò in pasto al dialog l'array dei nomi delle misure, insieme all'array della visibilità
				 .setMultiChoiceItems(selected.getList(), show, new OnMultiChoiceClickListener() {

					@Override
					public void onClick(DialogInterface arg0, int arg1,
							boolean arg2) {
						show[arg1] = arg2;
					}				 
				 })
				 // imposto il bottone per il salvataggio
				 .setPositiveButton(R.string.salva, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// modifico i valori di visibilità
						for(int i=0; i<show.length; i++) 
							selected.getMeasure(i).setVisibile(show[i]);
						// salvo il file sulla SD
						if (selected.write(context)) {
							Toast.makeText(context, R.string.salvataggio_ok, Toast.LENGTH_SHORT).show();
							selected.buildList(context);
							// sostituisco il file nel gruppo e restituisco il tutto al Main
							setResultOk(selected);
						}
						else
							Toast.makeText(context, R.string.salvataggio_errore, Toast.LENGTH_SHORT).show();
						
						dialog.dismiss();
					} 
				 }).create().show();
			 }
		 });
	 }
	 
	 // questa funzione sostituisce nell'array dei gruppi il valore trovato
	 private void setResultOk(GroupMeasures s) {
			
		 if (selected_s>=0) {
			 misure.remove(selected_s);
			 s.buildList(context);
			 misure.add(selected_s, s);
			 // imposto l'intent
			 setIntent();
			 // resetto i vari campi e reimposto bottoni e ascoltatori
			 setPerasonalMeasures();
		 }
	 }
	 
	 
	 //////////////////////////////////////////////////////////////////////////////////////////////
	 // BOTTONE PER MODIFICARE I VALORI CON L'EDITOR  
	 
	 
	 private void setEditListener(Button button) {
		 // costruisco l'array della visibilità
		 String[] mp = new String[misure_personali_s.size()];
		 for(int i=0; i< misure_personali_s.size(); i++) 
			 mp[i] = misure_personali_s.get(i);
		 
		 // costruisco il solito adapter
		 final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				 android.R.layout.simple_spinner_dropdown_item, mp);
	    	
		 button.setOnClickListener(new OnClickListener () {
			 
			 @Override
			 public void onClick(View v) {
					
				 new AlertDialog.Builder(context)
				 .setTitle(context.getText(R.string.select_edit))
				 .setSingleChoiceItems(adapter, group_edit_position, new DialogInterface.OnClickListener() {

					 @Override
					 public void onClick(DialogInterface dialog, int which) {
						 
						 group_edit_position = which;
						 
						 Intent i = new Intent(EditorM.this, AddMeasure.class);
						 // in questo caso sto accedendo all'editor
						 i.putExtra("editor", true);
						 // inserisco l'oggetto GroupMeasures
						 i.putExtra("misura", misure.get(group_menu_position)); 
						 // inserisco l'oggetto misura selezionata
						 i.putExtra("personale", misure.get(group_menu_position).getMeasure(misure_personali.get(which)));
					     setResult(Activity.RESULT_OK, i);
					     dialog.dismiss();
					     startActivityForResult(i,EDITOR);
						 }
					 
				 }).create().show();
			 }			
		 });	
	 }
	 
	 ///////////////////////////////////////////////////////////////////////////////////////////////////
	 // BOTTONE PER ELIMINARE UN'UNITÀ PERSONALE
	 
	 
	 private void setDeleteListener(Button button) {
		 // costruisco l'array della visibilità
		 String[] mp = new String[misure_personali_s.size()];
		 for(int i=0; i< misure_personali_s.size(); i++) 
			 mp[i] = misure_personali_s.get(i);
		 
		 // costruisco il solito adapter
		 final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				 android.R.layout.simple_spinner_dropdown_item, mp);
	    	
		 button.setOnClickListener(new OnClickListener () {
			 
			 @Override
			 public void onClick(View v) {
					
				 new AlertDialog.Builder(context)
				 .setTitle(context.getText(R.string.select_delete))
				 .setSingleChoiceItems(adapter, group_edit_position, new DialogInterface.OnClickListener() {

					 @Override
					 public void onClick(final DialogInterface sdialog, final int swhich) {
						 
						 Measures m = misure.get(group_menu_position).getMeasure(misure_personali.get(swhich));
						 String message = context.getString(R.string.are_you_sure) + m.toString(context);
						 
						 new AlertDialog.Builder(context)
						 .setTitle(R.string.delete)
						 .setMessage(message)
						 .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						 })
						
						 .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

							 @Override
							 public void onClick(DialogInterface dialog,
									 int which) {
								
								GroupMeasures gm = misure.get(group_menu_position);
								gm.removeMisura(misure_personali.get(swhich), context);
								
								// salvo il file sulla SD
								if (gm.write(context)) {
									Toast.makeText(context, R.string.cancellazione_ok, Toast.LENGTH_SHORT).show();
									gm.buildList(context);
									// sostituisco il file nel gruppo e restituisco il tutto al Main
									setResultOk(gm);
								}
								else
									Toast.makeText(context, R.string.cancellazione_errore, Toast.LENGTH_SHORT).show();
								
								dialog.dismiss();
								sdialog.dismiss();
							}
						 }).create().show();
					 }		
				 }).create().show();	
			 }
		 });
	 }
	 
	 ///////////////////////////////////////////////////////////////////////////////////////////////////
	 // PREPARAZIONE DELL'INTENT PER LA TERMINAZIONE DELL'ACTIVITY
	 
	 private void setIntent() {
		 Intent i = new Intent();
		 i.putExtra("misure", misure);
		 setResult(Activity.RESULT_OK, i);
	 }
	 
	 ///////////////////////////////////////////////////////////////////////////////////////////////////
	 // RICEZIONE DEL VALORE DALL'INTENT EDITOR
	
	// funzione per intercettare il valore restituito da StartActivityForResult
	 	protected void onActivityResult(int requestCode, int resultCode,
	            Intent data) {
	 		
	 		GroupMeasures g = null;
	 		
	 		// nel caso il valore sia restituito dall'editor
	        if ((requestCode == EDITOR) && (resultCode == RESULT_OK)) 
	        	// recupero l'array delle misure
	        	g = data.getParcelableExtra("misura");	

	        // se g NON è nullo
	        if (g!= null) {
	        	// reimposto misure
	        	for(int i=0; i<misure.size(); i++) {
	        		if(misure.get(i).getId()==g.getId()) {
	        			misure.remove(i);
	        			misure.add(i, g);
	        			break;
	        		}
	        	}
	        	
	        	// resetto i vari campi e reimposto bottoni e ascoltatori
	            this.bindButtons();
	            this.setPerasonalMeasures();
	            setIntent();
	        }
	    }	
}
