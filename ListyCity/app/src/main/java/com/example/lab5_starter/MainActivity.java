package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private ListView cityListView;
    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;

    // Firestore variables
    private FirebaseFirestore db;
    private CollectionReference citiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        // Set views
        Button addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);

        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

        // Snapshot Listener to keep local list in sync with Firestore
        citiesRef.addSnapshotListener((querySnapshots, error) -> {
            if (error != null) {
                Log.e("Firestore", error.toString());
                return;
            }
            if (querySnapshots != null) {
                cityArrayList.clear();
                for (QueryDocumentSnapshot doc : querySnapshots) {
                    City city = doc.toObject(City.class);
                    cityArrayList.add(city);
                }
                cityArrayAdapter.notifyDataSetChanged();
            }
        });

        addCityButton.setOnClickListener(view -> {
            new CityDialogFragment().show(getSupportFragmentManager(), "Add City");
        });

        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(), "City Details");
        });
    }

    @Override
    public void addCity(City city) {
        // Use the city name as the document ID
        citiesRef.document(city.getName())
                .set(city)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "City added"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error adding city", e));
    }

    @Override
    public void updateCity(City city, String newName, String newProvince) {
        // If the name changed, we need to delete the old document and add a new one
        if (!city.getName().equals(newName)) {
            citiesRef.document(city.getName()).delete();
            addCity(new City(newName, newProvince));
        } else {
            // If only province changed, just update the existing document
            HashMap<String, Object> data = new HashMap<>();
            data.put("province", newProvince);
            citiesRef.document(city.getName()).update(data);
        }
    }

    @Override
    public void deleteCity(City city) {
        citiesRef.document(city.getName())
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "City deleted"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error deleting city", e));
    }
}
