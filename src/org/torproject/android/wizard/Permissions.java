package org.torproject.android.wizard;

import org.sandroproxy.ony.R;
import org.sufficientlysecure.rootcommands.RootCommands;
import org.torproject.android.TorConstants;
import org.torproject.android.service.TorServiceUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class Permissions extends Activity implements TorConstants {

    private Context context;
    
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        context = this;

    }
    
    @Override
    protected void onStart() {
        
        super.onStart();
        setContentView(R.layout.layout_wizard_permissions);
        
        stepFourRoot();
                
    }
    
    @Override
    protected void onResume() {
        super.onResume();
    
        
    }
    
    
    private void stepFourRoot(){
                
        String msg1 = context.getString(R.string.wizard_permissions_root_msg1);
        String msg2 = context.getString(R.string.wizard_permissions_root_msg2);
        
        TextView txtBody1 = ((TextView)findViewById(R.id.WizardTextBody1));
        txtBody1.setText(msg1);
        

        TextView txtBody2 = ((TextView)findViewById(R.id.WizardTextBody2));
        txtBody2.setText(msg2);
        txtBody2.setVisibility(TextView.VISIBLE);
        
        Button grantPermissions = ((Button)findViewById(R.id.grantPermissions));
        grantPermissions.setVisibility(Button.VISIBLE);
        
        Button back = ((Button)findViewById(R.id.btnWizard1));
        Button next = ((Button)findViewById(R.id.btnWizard2));
        next.setEnabled(false);
        
        CheckBox consent = (CheckBox)findViewById(R.id.checkBox);
        consent.setVisibility(CheckBox.VISIBLE);
        
        consent.setOnCheckedChangeListener(new OnCheckedChangeListener (){

            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
            
                
                //this is saying do not use root
                
                SharedPreferences prefs =  TorServiceUtils.getSharedPrefs(getApplicationContext());

                Editor pEdit = prefs.edit();
                
                pEdit.putBoolean(PREF_TRANSPARENT, false);
                pEdit.putBoolean(PREF_TRANSPARENT_ALL, false);                
                pEdit.putBoolean(PREF_HAS_ROOT, false);
                
                pEdit.commit();
                
                /*
                Button next = ((Button)findViewById(R.id.btnWizard2));
                if(isChecked)
                    next.setEnabled(true);
                else
                    next.setEnabled(false);
                */
                
                stepFour();
                
            }
            
        });
        
        
        grantPermissions.setOnClickListener(new View.OnClickListener() {
            
            public void onClick(View v) {
                //Check and Install iptables - TorTransProxy.testOwnerModule(this)
                
                SharedPreferences prefs =  TorServiceUtils.getSharedPrefs(getApplicationContext());
                
                boolean hasRoot = RootCommands.rootAccessGiven();
                Editor pEdit = prefs.edit();
                pEdit.putBoolean(PREF_HAS_ROOT,hasRoot);
                pEdit.commit();
                                
                if (!hasRoot)
                {

                    stepFour();
                    
                }
                else
                {
                    finish();
                    startActivity(new Intent(Permissions.this, ConfigureTransProxy.class));
                }

                
            }
        });
        
        back.setOnClickListener(new View.OnClickListener() {
            
            public void onClick(View v) {
                finish();
                startActivity(new Intent(Permissions.this, LotsaText.class));
            }
        });
        
        
          next.setOnClickListener(new View.OnClickListener() {
         
            
            public void onClick(View v) {
                finish();
                startActivity(new Intent(Permissions.this, TipsAndTricks.class));
            }
        });
        
    }
    
    private void stepFour(){
        
        String title = context.getString(R.string.wizard_permissions_title);
        String msg = context.getString(R.string.wizard_permissions_no_root_msg);
        
        setTitle(title);
        
        TextView txtBody = ((TextView)findViewById(R.id.WizardTextBody1));
        txtBody.setText(msg);
        
        Button btn1 = ((Button)findViewById(R.id.btnWizard1));
        Button btn2 = ((Button)findViewById(R.id.btnWizard2));
        btn2.setEnabled(true);
   
        TextView txtBody2 = ((TextView)findViewById(R.id.WizardTextBody2));
        txtBody2.setVisibility(TextView.GONE);
        
        Button grantPermissions = ((Button)findViewById(R.id.grantPermissions));
        grantPermissions.setVisibility(Button.GONE);
        
        CheckBox consent = (CheckBox)findViewById(R.id.checkBox);
        consent.setVisibility(CheckBox.GONE);
        
        btn1.setOnClickListener(new View.OnClickListener() {
            
            public void onClick(View v) {
                finish();
                startActivity(new Intent(Permissions.this, LotsaText.class));
            }
        });
        
        btn2.setOnClickListener(new View.OnClickListener() {
            
            public void onClick(View v) {
                finish();
                startActivity(new Intent(Permissions.this, TipsAndTricks.class));
            }
        });
    }
        
    //Code to override the back button!
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            finish();
            startActivity(new Intent(getBaseContext(), LotsaText.class));
            return true;
        }
        return false;
    }
    
}