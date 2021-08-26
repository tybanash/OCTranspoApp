package algonquin.cst2335.octranspoapp;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * This class represents the first page of the application.
 * The user types in a buss stop number to load the bus routes for this stop number on the recycler view.
 * @author  Nadege Awah
 * @version  1.0
 */
public class MainActivity extends AppCompatActivity {
    /**
     * this holds the bus routes list
     */
    RecyclerView busRoutesList;
    /**
     * variable used as a reference to the Adapter object
     */
    MyBusRouteAdapter adt;
    /**
     * This holds the stop number input from the user
     */
    EditText busRouteEditText;
    /**
     * This string represents the address of the server we will connect to
     */
    private String stringURL;

    String routeNo = null;
    ArrayList<BusRoutes> busRoutes;
    String routeNumber;

    /**
     * This method displays a toast message to welcome the user when the activity title is clicked.
     * This method also displays an  AlertDialog with instructions for how to use the interface
     * @param item The menu item
     * @return true
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.show_activity_title) {
            Context context = getApplicationContext();
            CharSequence text = "Welcome to OCTranspo Bus App";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return true;
        }
        switch (item.getItemId()) {
            case R.id.help_icon:
                AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Tips for use")
                        .setMessage("Enter bus stop number\nClick on Go to see bus routes")
                        .setView(new ProgressBar(MainActivity.this))
                        .show();
        }
        return super.onOptionsItemSelected(item);

    }

    /**
     * This method initialises the toolbar
     * @param menu
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /**
         * Loads the activity_second layout
         */
        setContentView(R.layout.activity_main);


        /**
         * creates a new bus route array list
         */
        busRoutes = new ArrayList<>();
        busRoutesList = findViewById(R.id.myrecycler);

        /**
         * creates a new MyBusRouteAdapter object
         */
        adt = new MyBusRouteAdapter();
        busRoutesList.setAdapter(adt);
        busRoutesList.setLayoutManager(new LinearLayoutManager(this));

        /**
         * gets the toolbar
         */
        Toolbar myToolbar = findViewById(R.id.toolbar);
        /**
         * loads the toolbar
         */
        setSupportActionBar(myToolbar);


        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        /**
         * creates a hamburger button that sits in the top left of the Toolbar
         */
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, myToolbar, R.string.open, R.string.close);
        /**
         * make the button and pop-out menu synchronize so that the hamburger button show the open/close state correctly
         */
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.popout_menu);
        /**
         * adds a selection listener to the menu.
         * closes the navigation drawer when a menu item has been selected.
         */
        navigationView.setNavigationItemSelectedListener((item) -> { //adds a selection listener to the menu
            onOptionsItemSelected(item);
            drawer.closeDrawer(GravityCompat.START);
            return false;
        });


        Button goButton = findViewById(R.id.goButton);
        goButton.setOnClickListener((click) -> {

            //sharedprefernces
            busRouteEditText = findViewById(R.id.busRouteEditText);
            String stopNo = busRouteEditText.getText().toString();
            SharedPreferences prefs = getSharedPreferences("MyData", Context.MODE_PRIVATE);
            String busRoute = prefs.getString("RouteName", "");
            busRouteEditText.setText(busRoute);
            SharedPreferences.Editor  editor = prefs.edit();
            editor.putString("RouteName", busRouteEditText.getText().toString() );
            editor.apply();
//Alert Dialog
            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Getting Route Summary")
                    .setMessage("We're getting the bus route details for " + stopNo)
                    .setView(new ProgressBar(MainActivity.this))
                    .show();
            //dialog.hide();


            //when you click on go button start a thread
            Executor newThread = Executors.newSingleThreadExecutor();
            newThread.execute(() -> {
                //This runs in a separate thread



                try{
                    //stringURL = "https://api.octranspo1.com/v2.0/GetNextTripsForStop?appID=223eb5c3&&apiKey=ab27db5b435b8c8819ffb8095328e775&stopNo=3017&routeNo=95&format=xml ";
                    stringURL = "https://api.octranspo1.com/v2.0/GetRouteSummaryForStop?appID=223eb5c3&apiKey=ab27db5b435b8c8819ffb8095328e775&stopNo="
                            + stopNo + "&format=jSON";


                    URL url = new URL(stringURL);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                    String text = (new BufferedReader(
                            new InputStreamReader(in, StandardCharsets.UTF_8)))
                            .lines()
                            .collect(Collectors.joining("\n"));

                    JSONObject theDocument = new JSONObject( text ); //this converts the String to JSON Object.
                    //String stopnumber = theDocument.getString( "StopNo" );
                    JSONObject routeSummary = theDocument.getJSONObject( "GetRouteSummaryForStopResult" );
                    //String stopnumber = routeSummary.getString( "StopNo" );
                    JSONObject routes = routeSummary.getJSONObject("Routes");
//get the JSONArray
                    JSONArray routeArray = routes.getJSONArray("Route");


                    //get the JSON obj at position 0
                    JSONObject position = null;
                    for (int i = 0; i<routeArray.length(); i++) {
                        //BusRoutes thisRoute = new BusRoutes(routeNumber);

                        position = routeArray.getJSONObject(i);

                        routeNumber = position.getString("RouteNo");
                        BusRoutes thisRoute = new BusRoutes(routeNumber, 1);
                        //thisRoute.setRouteNumber(routeNumber);
                        busRoutes.add(thisRoute); //adds to array list

                        routeNumber = position.getString("RouteHeading");
                        thisRoute = new BusRoutes(routeNumber, 2);
                        //thisRoute.setRouteNumber(routeNumber);
                        busRoutes.add(thisRoute); //adds to array list




//                        routeHeading = position.getString("RouteHeading");
//                        BusRoutes thisRoute = new BusRoutes(routeNumber, 1);
//                        //thisRoute.setRouteNumber(routeNumber);
//                        busRoutes.add(thisRoute); //adds to array list

                    }



                    runOnUiThread(( ) -> {
                        adt.notifyItemInserted( busRoutes.size()-1  ); //just insert the new row:

                    });





                }
                catch (IOException | JSONException ioe){
                    Log.e("Connection error:", ioe.getMessage());
                }
                //adt.notifyItemInserted( route.size() - 1 ); //just insert the new row:


                //busRoutesList = findViewById(R.id.myrecycler);




            });



        });


        /**
         * This loads a snack bar when the cancel button is clicked.
         */
        EditText busRouteEditText = findViewById(R.id.busRouteEditText);
        Button cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener((click) -> {
            String routes = busRouteEditText.getText().toString();
            routes = "";
            Snackbar.make(busRouteEditText, "You deleted the route", Snackbar.LENGTH_LONG).show();

        });

    }

    /**
     * private MyBusRouteAdapter class
     * responsible for creating a layout for a row and setting textview in code
     * uses android LayoutInflater class to load a layout from bus_route.xml
     */

    private class MyBusRouteAdapter extends RecyclerView.Adapter<MyRowViews>{
        /**
         *
         * @param parent
         * @param viewType
         * @return loadedRow
         */
        @Override
        public MyRowViews onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = getLayoutInflater();
            int layoutID;
            if(viewType == 1)//send
                layoutID = R.layout.bus_route;
            else //Receive
                layoutID = R.layout.route_heading;
            View loadedRow = inflater.inflate(layoutID, parent, false);
            return new MyRowViews(loadedRow);

        }


        @Override
        public void onBindViewHolder(MyRowViews holder, int position) {
            holder.routeText.setText(busRoutes.get(position).getRouteInfo());
            holder.setPosition(position);

        }

        @Override
        public int getItemCount() {

            return busRoutes.size();
        }


    }

    /**
     * this class represents a row and its called the ViewHolder
     */
    public class MyRowViews extends RecyclerView.ViewHolder{
        TextView routeText;
        int position = -1;

        /**
         * @param itemView  represents the Constraintlayout which is the root of the row
         */

        public MyRowViews(View itemView) {
            super(itemView);

            routeText = itemView.findViewById(R.id.routeInfo);
        }
        public void setPosition(int p) {
            position = p;
        }

    }

    /**
     * private class used to store thr bus routes
     */
    private class BusRoutes {
        String routeInfo;
        int numberOrHeading;

        public BusRoutes(String routeInfo, int numberOrHeading) {
            this.routeInfo = routeInfo;
            this.numberOrHeading = numberOrHeading;
        }

        public String getRouteInfo() {
            return routeInfo;
        }


        public int getNumberOrHeading() {
            return numberOrHeading;
        }
    }
}