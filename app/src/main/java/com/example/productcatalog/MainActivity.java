package com.example.productcatalog;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText editTextName;
    private EditText editTextPrice;
    private Button buttonAddProduct;
    private ListView listViewProducts;

    private final List<Product> products = new ArrayList<>();
    private ProductList adapter;

    // Firebase:
    private DatabaseReference productsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        editTextName = findViewById(R.id.editTextName);
        editTextPrice = findViewById(R.id.editTextPrice);
        buttonAddProduct = findViewById(R.id.addButton);
        listViewProducts = findViewById(R.id.listViewProducts);


        adapter = new ProductList(this, products);
        listViewProducts.setAdapter(adapter);


        FirebaseDatabase db = FirebaseDatabase.getInstance();

        productsRef = db.getReference("products");


        buttonAddProduct.setOnClickListener(v -> addProduct());


        listViewProducts.setOnItemLongClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            Product p = products.get(position);
            showUpdateDeleteDialog(p.getId(), p.getProductName());
            return true;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        productsRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) {
                products.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Product p = child.getValue(Product.class);
                    if (p != null) products.add(p);
                }
                adapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(DatabaseError error) {
                Toast.makeText(MainActivity.this, "Load failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void addProduct() {
        String name = editTextName.getText().toString().trim();
        String priceStr = editTextPrice.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(priceStr)) {
            Toast.makeText(this, "Name & price required", Toast.LENGTH_LONG).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Price must be a number", Toast.LENGTH_LONG).show();
            return;
        }

        String id = productsRef.push().getKey();
        if (id == null) {
            Toast.makeText(this, "Id generation failed", Toast.LENGTH_LONG).show();
            return;
        }

        Product product = new Product(id, name, price);
        productsRef.child(id).setValue(product)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Added", Toast.LENGTH_LONG).show();
                    editTextName.setText("");
                    editTextPrice.setText("");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Add failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void updateProduct(String id, String name, double price) {
        if (TextUtils.isEmpty(id)) {
            Toast.makeText(this, "Invalid id", Toast.LENGTH_LONG).show();
            return;
        }
        Product updated = new Product(id, name, price);
        productsRef.child(id).setValue(updated)
                .addOnSuccessListener(a -> Toast.makeText(this, "Updated", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void deleteProduct(String id) {
        if (TextUtils.isEmpty(id)) {
            Toast.makeText(this, "Invalid id", Toast.LENGTH_LONG).show();
            return;
        }
        productsRef.child(id).removeValue()
                .addOnSuccessListener(a -> Toast.makeText(this, "Deleted", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void showUpdateDeleteDialog(final String productId, String productName) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.update_dialog, null, false);
        EditText etName = dialogView.findViewById(R.id.editTextName);
        EditText etPrice = dialogView.findViewById(R.id.editTextPrice);
        Button btnUpdate = dialogView.findViewById(R.id.buttonUpdateProduct);
        Button btnDelete = dialogView.findViewById(R.id.buttonDeleteProduct);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(productName)
                .setView(dialogView)
                .create();
        dialog.show();

        btnUpdate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(priceStr)) {
                Toast.makeText(this, "Both fields required", Toast.LENGTH_SHORT).show();
                return;
            }
            double price;
            try { price = Double.parseDouble(priceStr); }
            catch (NumberFormatException e) {
                Toast.makeText(this, "Price must be a number", Toast.LENGTH_SHORT).show();
                return;
            }
            updateProduct(productId, name, price);
            dialog.dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            deleteProduct(productId);
            dialog.dismiss();
        });
    }
}
