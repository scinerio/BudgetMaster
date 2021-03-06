package com.budgetmaster.budgetmaster;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/****************************************************************************************/
/*
/* FILE NAME: Database
/*
/* DESCRIPTION: The homepage of the app that allows users to add incomes and
/#   expenses.  It also is will create and hold the sqllite database
 */
/*
/* REFERENCE:
/*
/* DATE         BY             CHANGE REF         DESCRIPTION
/* ========   =============     ===========         =============
/* 11/17/2016  Grant Hardy          DB1                 DB started cluttering, so I will make its own class
/* 11/18/2016  Grant Hardy          DB2                 Started to add functions createDB, constructor
/*                                                 Also added in all basic add and get functions
/* 11/25/2016  Grant Hardy          DB3                 Fixed the db crashing issue by updating the table names
/* 11/26/2016  Grant Hardy          DB4                 Created income as its own default category and made it to way the default categories
/*                                                  implement their owns budgets.
/*11/27/2016   Grant Hardy          DB5            Implemented the methods getCategoryNames, getTransNames, getTransPrices, and getTransDate
/*
/****************************************************************************************/

public class Database {
    
    private SQLiteDatabase budgetDB;

    /**
     * Contrustor class for the Budget db
     * @param db is an SQLiteDatabase that is what local budgetDB initializes to
     */
    public Database(SQLiteDatabase db)
    {
        budgetDB = db;
    }
    /**
     * Calling this function opens or creates the database with the tables Budget, Category
     * Trans, SQ
     * It also checks to see if the tables have been populated,
     * if not that it populates them with the default tables
     */
    public void createTables()
    {
            try {

                // Create Tables  Budget, Category, Transaction, Security Question with the allotted fields.
                budgetDB.execSQL("CREATE TABLE IF NOT EXISTS Budget " + "(budgetID integer primary key, name varchar(30), netMoney double);");
                budgetDB.execSQL("CREATE TABLE IF NOT EXISTS Category " + "(catID integer primary key, name varchar(30), type varchar(10), maxAmount double, curAmountSpent double, budgetID integer, foreign key(budgetID) references Budget(budgetID));");
                budgetDB.execSQL("CREATE TABLE IF NOT EXISTS Trans " + "(tranID integer primary key, price double, name varchar, type varchar(10), date varchar(200), description varchar(50), recurring boolean, budgetID integer, catID integer, foreign key(budgetID) references Budget(budgetID), foreign key(catID) references Category(catID));");

                Cursor cursor = budgetDB.rawQuery("select count(*) from Budget;", null);
                cursor.moveToFirst();
                int icount = cursor.getInt(0);
                //If no budgets, populate the beginning master budget
                if (icount == 0) {
                    this.addBudget("masterbudget"); //Master Budget id should always be 1.
                }

                //Get number of categories
                String count = "select count(*) from Category;";
                cursor = budgetDB.rawQuery(count, null);
                cursor.moveToFirst();
                icount = cursor.getInt(0);

                //If the table contains no categories(ie it just got created,
                //Then populate the default tables
                if (icount == 0) {
                    addCategory("gas", "expense", 100.00);
                    addCategory("rent", "expense", 600.00);
                    addCategory("utilities", "expense", 200.00);
                    addCategory("food", "expense", 300.00);
                    addCategory("income", "income", 300.00);
                    addCategory("miscellaneous", "expense", 100.00);

                }


                cursor.close();
            }
            catch(Exception e)
            {
                Log.e("BudgetDatabase ERROR", "Error Creating Tables");
            }


    }

    /**
     * Add a budget to the table
     * @param budName name of new Budget
     */
    private void addBudget(String budName)
    {
        budName = budName.toLowerCase();
        budName = "\""+budName+"\"";
        budgetDB.execSQL("insert into Budget (name, netMoney) Values ("+budName+", "+ 0.0 +");");
    }

    /**
     * Add a category to the table
     * @param name name of the new Category ie Gas
     * @param type income or expense
     * @param maxAmount maximum amount the user plans on spending on this category
     */
    public void addCategory(String name, String type, double maxAmount)
    {
        name = name.toLowerCase();
         type = type.toLowerCase();
        type = "\""+type+"\"";

        //When we create a category, we will also create categorical budget that will be a foreign key in category
        this.addBudget(name+"budget");

        int budID = this.getBudgetID(name+"budget");
        name = "\""+name+"\"";
        budgetDB.execSQL("insert into Category (name, type, maxAmount, curAmountSpent, budgetID) Values ("+ name +", " + type + ", "+ maxAmount + ", " + 0.0 + ", " + budID+");");
    }

    /**
     * Add Transaction to the table.
     * In order to get the correct references for the tranaction, we obtain the catID from the
     * category name given to us, the we obtain the budgetID from the catID just found.
     * @param name title of the transaction
     * @param price how much money was spent/obtained
     * @param type income or expense?
     * @param date the date of the transactioon
     * @param description A more detailed description
     * @param recurring Will this transaction recur reguarly?
     * @param catName name of the category that the transaction belongs to
     */
    public void addTransaction(String name,  double price, String type, Date date, String description,  boolean recurring, String catName)
    {
        type = type.toLowerCase();

        //description = description.toLowerCase();
        name = name.toLowerCase();
        name = "\""+name+"\"";
        description = name;
        String dateString = date.toString();
        Date dateConverter;
        try{
            dateConverter = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy").parse(dateString);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
        dateString = new SimpleDateFormat("MMM dd yyyy").format(dateConverter);
        int catID = getCategoryID(catName);
        Cursor cursor = budgetDB.rawQuery("select budgetID from Category where catID = " + catID+ ";", null);
        cursor.moveToFirst();
        int budID = cursor.getInt(0);

        //Check to make sure type is correct
        //Then add the transaction to the trans table
        //Add correspondingly update the Master budget, and the categorical budget it belongs too.
        if((type.equals("expense") || type.equals("income")))
        {
            type = "\""+type+"\"";
            dateString = "\""+dateString+"\"";
            budgetDB.execSQL("insert into Trans (price, name, type, date, description, recurring, budgetID, catID) Values (" + price + ", " + name + ", " + type + ", " + dateString + ", " + description + ", " + 0 + ", " + budID + ", " + catID + ");");
          //  updateBudget(1, type, price);
            updateBudget(catName, type, price);

        }
        cursor.close();
    }



    /**
     * Return the catID of the name used as a parameter
     * @param name of category
     * @return catID
     */
    public int getCategoryID(String name)
    {
        name = name.toLowerCase();
        name = "\""+name+"\"";
        String sqlite = "select catID from Category where name = " + name+";";
        Cursor cursor = budgetDB.rawQuery(sqlite, null);
        cursor.moveToFirst();
        int catID = cursor.getInt(0);
        cursor.close();
        return catID;
    }

    /**
     * Return the budgetID of the name used as a parameter
     * @param name of budget
     * @return budgetid
     */
    public int getBudgetID(String name)
    {
        name = name.toLowerCase();
        name = "\"" +name+"\"";
        Cursor cursor = budgetDB.rawQuery("select budgetID from Budget where name = " + name+";", null);
        cursor.moveToFirst();
        int budID = cursor.getInt(0);
        cursor.close();
        return budID;
    }

    public Category[] getCategories()
    {
        Cursor cursor = budgetDB.rawQuery("select count(*) from Category;", null);
        cursor.moveToFirst();
        int size = cursor.getInt(0);
        cursor = budgetDB.rawQuery("select * from Category;", null);
        int titleColumn = cursor.getColumnIndex("name");
        int maxAmountCol = cursor.getColumnIndex("maxAmount");
        int curAmountCol = cursor.getColumnIndex("curAmountSpent");

        boolean skipIncomeFlag = false;
        cursor.moveToFirst();

        Category[] cat = new Category[size-1];
        int i = 0;
        // Verify that we have results
        if (cursor != null && (cursor.getCount() > 0)) {

            do {
                // Get the results and store them in a Array
                double maxAmount = cursor.getDouble(maxAmountCol);
                String title = cursor.getString(titleColumn);
                double curAmountSpent = cursor.getDouble(curAmountCol);
                if((title.contains("income")))
                {
                    cursor.moveToNext();
                    skipIncomeFlag = true;
                    maxAmount = cursor.getDouble(maxAmountCol);
                    title = cursor.getString(titleColumn);
                    curAmountSpent = cursor.getDouble(curAmountCol);
                }
               // if(!skipIncomeFlag)
             //   {
                    cat[i] = new Category(title, maxAmount, curAmountSpent);
                    i++;
              //  }
                /**
                else
                {
                    cat[i-1] = new Category(title, maxAmount, curAmountSpent);
                    i++;
                }
                 */



                // Keep getting results as long as they exist
            } while (cursor.moveToNext());
        }
        cursor.close();
        return cat;

    }

    public String[] getCategoryNames()
    {
        Cursor cursor = budgetDB.rawQuery("select count(*) from Category;", null);
        cursor.moveToFirst();
        int size = cursor.getInt(0) - 1; //Remove one for the income category that will not be returned
        cursor = budgetDB.rawQuery("select * from Category;", null);
        int titleColumn = cursor.getColumnIndex("name");
        String catTitle;

        cursor.moveToFirst();

        String[] titles = new String[size];
        int i = 0;
        // Verify that we have results
        if (cursor != null && (cursor.getCount() > 0)) {

            do {
                // Get the results and store them in a Array
                catTitle = cursor.getString(titleColumn);
                //We want to hide the income category from our users
                //They should not be able to select an expense in the income category
                //The income category is to manage incomes in the database only
                if(!(catTitle.equals("income")))
                {
                    titles[i] = catTitle;
                    i++;
                }

                // Keep getting results as long as they exist
            } while (cursor.moveToNext());
        }
        cursor.close();
        return titles;

    }

    public String[] getTransNames()
    {
        Cursor cursor = budgetDB.rawQuery("select count(*) from Trans;", null);
        cursor.moveToFirst();
        int size = cursor.getInt(0);
        cursor = budgetDB.rawQuery("select * from Trans;", null);
        int titleColumn = cursor.getColumnIndex("name");


        cursor.moveToFirst();

        String[] names = new String[size];
        int i = 0;
        // Verify that we have results
        if (cursor != null && (cursor.getCount() > 0)) {

            do {
                // Get the results and store them in a Array
                names[i] = cursor.getString(titleColumn);
                i++;

                // Keep getting results as long as they exist
            } while (cursor.moveToNext());
        }
        cursor.close();
        return names;

    }

    public String[] getTransDates()
    {
        Cursor cursor = budgetDB.rawQuery("select count(*) from Trans;", null);
        cursor.moveToFirst();
        int size = cursor.getInt(0);
        cursor = budgetDB.rawQuery("select * from Trans;", null);
        int dateColumn = cursor.getColumnIndex("date");


        cursor.moveToFirst();

        String[] dates = new String[size];
        int i = 0;
        // Verify that we have results
        if (cursor != null && (cursor.getCount() > 0)) {

            do {
                // Get the results and store them in a Array
                dates[i] = cursor.getString(dateColumn);
                i++;

                // Keep getting results as long as they exist
            } while (cursor.moveToNext());
        }
        cursor.close();
        return dates;

    }

    public String[] getTransPrices()
    {
        Cursor cursor = budgetDB.rawQuery("select count(*) from Trans;", null);
        cursor.moveToFirst();
        int size = cursor.getInt(0);
        cursor = budgetDB.rawQuery("select * from Trans;", null);
        int priceColumn = cursor.getColumnIndex("price");


        cursor.moveToFirst();

        String[] prices = new String[size];
        int i = 0;
        // Verify that we have results
        if (cursor != null && (cursor.getCount() > 0)) {

            do {
                // Get the results and store them in a Array
                Double tmp = cursor.getDouble(priceColumn);
                prices[i] = String.format("%.2f", tmp);
                i++;

                // Keep getting results as long as they exist
            } while (cursor.moveToNext());
        }
        cursor.close();
        return prices;
    }

    public Transaction[] getAllTransactions()
    {

        Cursor cursor = budgetDB.rawQuery("select count(*) from Trans;", null);
        cursor.moveToFirst();
        int size = cursor.getInt(0);
        cursor = budgetDB.rawQuery("select * from Trans;", null);
        int priceColumn = cursor.getColumnIndex("price");
        int nameColumn = cursor.getColumnIndex("name");
        int dateColumn = cursor.getColumnIndex("date");
        int catColumn = cursor.getColumnIndex("catID");
        int typeColumn = cursor.getColumnIndex("type");
        String cat = "";

        Cursor catCursor;
        String findCatNameQuery = "select name from Category where catID = ";

        cursor.moveToFirst();

        Transaction[] transactions = new Expense[size];
        int i = 0;

        // Verify that we have results
        if (cursor != null && (cursor.getCount() > 0)) {

            do {
                // Get the results and store them in a Array
                double price = cursor.getDouble(priceColumn);
                String name = cursor.getString(nameColumn);
                String date = cursor.getString(dateColumn);
                int catID = cursor.getInt(catColumn);
                String type = cursor.getString(typeColumn);

                catCursor = budgetDB.rawQuery(findCatNameQuery+catID +";", null);
                catCursor.moveToFirst();
                cat = catCursor.getString(0);


                transactions[i] = new Expense(name, cat, date, price, type);
                i++;

                // Keep getting results as long as they exist
            } while (cursor.moveToNext());
        }
        cursor.close();
        return transactions;

    }

    /**
     * This function gets all the Expenses from Transactions and returns them in a Expenses array.
     *
     * @return an array of all Expenses kept

    public Expense[] getAllExpenses() {
        Cursor cursor = budgetDB.rawQuery("select count(*) from Trans where type = 'expense';", null);
        cursor.moveToFirst();
        int size = cursor.getInt(0);
        cursor = budgetDB.rawQuery("select * from Trans where type = 'expense';", null);
        int priceColumn = cursor.getColumnIndex("price");
        int nameColumn = cursor.getColumnIndex("name");
        int dateColumn = cursor.getColumnIndex("date");
        int catColumn = cursor.getColumnIndex("catID");
        String cat = "";

        Cursor catCursor;
        String findCatNameQuery = "select name from Category where catID = ";

        cursor.moveToFirst();

        Expense[] expenses = new Expense[size];
        int i = 0;

        // Verify that we have results
        if (cursor != null && (cursor.getCount() > 0)) {

            do {
                // Get the results and store them in a Array
                double price = cursor.getDouble(priceColumn);
                String name = cursor.getString(nameColumn);
                String date = cursor.getString(dateColumn);
                int catID = cursor.getInt(catColumn);

                catCursor = budgetDB.rawQuery(findCatNameQuery+catID +";", null);
                catCursor.moveToFirst();
                cat = catCursor.getString(0);
                expenses[i] = new Expense(name, cat, date, price);
                i++;

                // Keep getting results as long as they exist
            } while (cursor.moveToNext());
        }
        cursor.close();
        return expenses;
    }

    /**
     * This function obtains all Transactions that are incomes and returns them in a income array
     * @return an Income array of all Incomes kept
     * */
/**
    public Income[] getAllIncomes() {
        Cursor cursor = budgetDB.rawQuery("select count(*) from Trans where type = 'income';", null);
        cursor.moveToFirst();
        int size = cursor.getInt(0);
        cursor = budgetDB.rawQuery("select * from Trans where type = 'income';", null);
        int priceColumn = cursor.getColumnIndex("price");
        int nameColumn = cursor.getColumnIndex("name");
        int dateColumn = cursor.getColumnIndex("date");
        int catColumn = cursor.getColumnIndex("catID");
        String cat = "";

        Cursor catCursor;
        String findCatNameQuery = "select name from Category where catID = ";

        cursor.moveToFirst();

        Income[] incomes = new Income[size];
        int i = 0;
        // Verify that we have results
        if (cursor != null && (cursor.getCount() > 0)) {

            do {
                // Get the results and store them in an array
                double price = cursor.getDouble(priceColumn);
                String name = cursor.getString(nameColumn);
                String date = cursor.getString(dateColumn);
                int catID = cursor.getInt(catColumn);

                catCursor = budgetDB.rawQuery(findCatNameQuery+catID +";", null);
                catCursor.moveToFirst();
                cat = catCursor.getString(0);


                incomes[i] = new Income(name, cat, date, price);
                i++;

                // Keep getting results as long as they exist
            } while (cursor.moveToNext());
        }
        cursor.close();
        return incomes;
    }
   **/

    /**
     * This method is used to update budgets.  I made this private bc the budget should not just be
     * updated when nothing else happens. The budget should only be updated whenever another action
     * occur(ie adding or removing a transaction), so this function should only be called in those
     * particular functions.
     * This method works by obtaining the netMoney of the budget before the change,
     * modifying the variable depending on the type of transaction,
     * then updating the database with the new netMoney.
     * @param name name of the category updated
     * @param type income or expense?
     * @param price the price of the update
     */
    private void updateBudget(String name, String type, double price)
    {
        type = type.toLowerCase();
        name = name.toLowerCase();
        name = "\"" +name + "\"";
        Cursor cursor = budgetDB.rawQuery("select curAmountSpent from Category where name = "+name+";", null);
        cursor.moveToFirst();
        double netMon = cursor.getDouble(0);
        netMon += price;
        budgetDB.execSQL("update Category set curAmountSpent = "+netMon+" where name = "+name+";");


        cursor.close();
    }




    /**
     * Removes the category from the database.  Will also remove the corresponding categorical budget
     * @param name the name of the category to be deleted
     */
    public void removeCategory(String name)
    {
        name = name.toLowerCase();
        name = "\"" +name + "\"";
        Cursor cursor = budgetDB.rawQuery("select budgetID from Category where name = " + name + ";", null);
        cursor.moveToFirst();
        int budgetID = cursor.getInt(0);

        //We delete the category requested, but we also delete the categorical budget that it refrences;
        budgetDB.execSQL("delete from Category where name = "+name+";");
        budgetDB.execSQL("delete from Budget where budgetID = "+budgetID+";");
        cursor.close();
    }

    /**
     * Removes the transaction specified by the title.
     * It deletes the transaction from the transaction table, and also updates
     * the corresponding budgets as if the transaction never occured
     * @param title the title of the transaction to remove
     */
    public void removeTransaction(String title)
    {
        title = title.toLowerCase();
        title = "\"" +title + "\"";
        Cursor cursor = budgetDB.rawQuery("select budgetID, price, type, catID from Trans where name = "+title+";", null);
        cursor.moveToFirst();
        int priceColumn = cursor.getColumnIndex("price");
        int typeColumn = cursor.getColumnIndex("type");
        int idColumn = cursor.getColumnIndex("budgetID");
        int catIDColumn = cursor.getColumnIndex("catID");

        budgetDB.execSQL("delete from Trans where name = "+title+";");
        double price = cursor.getDouble(priceColumn);
        String type = cursor.getString(typeColumn);
        int budID = cursor.getInt(idColumn);
        int catID = cursor.getInt(catIDColumn);

        cursor = budgetDB.rawQuery("select name from Category where catID = "+catID+";", null);
        cursor.moveToFirst();
        String catName = cursor.getString(0);

        //Expenses and incomes are opposites of each other
        //If we remove an expense, it is the same as adding an income
        // and vice verso.
        //So, once we delete the Transaction we have to update the budget accordingly
        //We must update the master budget and the categorical one
        if(type.equals("expense"))
            type = "income";
        else
            type = "expense";
       // updateBudget(1, type, price);
        updateBudget(catName, type, price);
        cursor.close();
    }

    public double getAmountSpent(String catName)
    {
        catName = catName.toLowerCase();
        catName = "\"" + catName + "\"";
        Cursor cursor = budgetDB.rawQuery("select curAmountSpent from Category where name = "+catName+";", null);
        cursor.moveToFirst();
        return cursor.getDouble(0);

    }

    public double getAmountAlloted(String catName)
    {
        catName = catName.toLowerCase();
        catName = "\"" + catName + "\"";
        Cursor cursor = budgetDB.rawQuery("select maxAmount from Category where name = "+catName+";", null);
        cursor.moveToFirst();
        double amountSpent = cursor.getDouble(0);
        return amountSpent;
    }




    /**
     * method to return the database
     * @return the database
     */
    public SQLiteDatabase getDB()
    {
        return budgetDB;
    }


    
    
}





