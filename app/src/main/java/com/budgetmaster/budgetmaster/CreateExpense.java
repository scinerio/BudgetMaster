package com.budgetmaster.budgetmaster;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.Date;
/*
/* DATE         BY             CHANGE REF         DESCRIPTION
/* ========   =============     ===========         =============
/* 11/17/2016  Jason Williams       CE1                Created expense class
/* 11/18/2016  Jason Williams       CE2               Made it to where expenses are being stored and shown on main screen (Minus the DB)
/* 11/26/2016  Grant Hardy          CE3               Added database functionality to the class so that when expenses are made
/*                                                      they are stored in the database.
 */
/*
/*
/****************************************************************************************/


public class CreateExpense extends AppCompatActivity {
    SQLiteDatabase db = null;
    Database budDB = null;
    Spinner categorySpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Auto-generated code
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_expense);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Attempt to load DB
        try {
            db = this.openOrCreateDatabase("budgetDB", MODE_PRIVATE, null);
        }
        catch(Exception e)
        {
            System.out.println("It got caught....");
            Log.e("BudgetDatabase ERROR", "Error Creating/Loading database");
        }
        budDB = new Database(db);
        budDB.createTables();

        //Create dropdown menu to select created categories
        categorySpinner = (Spinner) findViewById(R.id.category_spinner);

        //Load categories from intent passed by main activity
        String[] categories = loadCategories();
        for (int i = 0; i < categories.length; i++) {
            char[] charArray = categories[i].toCharArray();
            charArray[0] = Character.toUpperCase(charArray[0]);
            categories[i] = new String(charArray);
        }

        //Populate data to the dropdown menu
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    /**
     * Auto-generated code for activity
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Creates menu at top, uses same menu as income
        getMenuInflater().inflate(R.menu.create_expense_menu, menu);
        return true;
    }

    /**
     * Auto-generated code for activity
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_accept) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Saves data inputted in text fields of create_income Activity
     * @param item
     */
    public void saveExpense(MenuItem item) {
        EditText titleView = (EditText) findViewById(R.id.expense_title);
        EditText amountView = (EditText) findViewById(R.id.expense_amount);

        String title = "";
        float amount = 0;
        String category;
        if(titleView.getText().toString().trim().length() == 0 || amountView.getText().toString().trim().length() == 0)
            Toast.makeText(this, "Please fill out every field", Toast.LENGTH_LONG).show();
        else {
            //Get values and add them to an Expense object
            title = titleView.getText().toString().trim();
            amount = Float.valueOf(amountView.getText().toString());
            category = categorySpinner.getSelectedItem().toString();

            Date date = new Date();
            try {
                budDB.addTransaction(title, amount, "expense", date, "", false, category);
            }
            catch(Exception e) {
                Log.e("BudgetDatabase ERROR", "Transaction was not added");
            }


            //Load values passed to this Activity through the intent object
            Intent intent = getIntent();
            Bundle extras = intent.getExtras();

            //Load spendable income, edit it, and commit changes
            float spendableInc = extras.getFloat(MainActivity.SPENDABLE_INCOME);
            spendableInc -= amount;
            SharedPreferences.Editor editor = getSharedPreferences(MainActivity.SPENDABLE_INCOME, MODE_PRIVATE).edit();
            editor.putFloat(MainActivity.SPENDABLE_INCOME, spendableInc);
            editor.commit();
            //Notify user the changes have been made
            Toast.makeText(this, "Expense added", Toast.LENGTH_LONG).show();

            Intent returnHome = new Intent(this, MainActivity.class);
            returnHome.putExtra("verified", true);
            startActivity(returnHome);
            finish();
        }
    }

    private String[] loadCategories() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        return extras.getStringArray("categories");
    }
}